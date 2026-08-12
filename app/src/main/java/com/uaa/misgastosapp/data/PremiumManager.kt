package com.uaa.misgastosapp.data

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.android.billingclient.api.*
import com.uaa.misgastosapp.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager para el estado Premium de la app.
 * - Free: muestra ads
 * - Premium ($0.99 USD): sin ads
 *
 * Usa SharedPreferences como caché local y Google Play Billing para verificar compras.
 */
class PremiumManager private constructor(context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "PremiumManager"
        private const val PREFS_NAME = "premium_prefs"
        private const val KEY_IS_PREMIUM = "is_premium"

        // ID del producto en Google Play Console (configurar en Play Console > Monetize > Products)
        const val PRODUCT_PREMIUM = "premium_no_ads"

        // Clave publica de licencia (Base64) de Play Console > Monetization setup > Licensing.
        // Sin esta clave no se puede verificar la firma de una compra localmente, lo que permite
        // que una compra falsificada (JSON armado a mano) pase como valida en un dispositivo rooteado.
        // TODO: completar con la clave real antes de publicar.
        private const val LICENSE_PUBLIC_KEY_BASE64 = ""

        @Volatile
        private var instance: PremiumManager? = null

        fun getInstance(context: Context): PremiumManager {
            return instance ?: synchronized(this) {
                instance ?: PremiumManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // se usa EncryptedSharedPreferences (en vez de SharedPreferences plano) para que el flag de
    // premium no se pueda editar directamente inspeccionando/modificando el XML de prefs con root.
    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    private var billingClient: BillingClient

    private val _isPremium = MutableStateFlow(prefs.getBoolean(KEY_IS_PREMIUM, false))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    init {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
    }

    /**
     * Conecta al BillingClient y verifica compras existentes.
     */
    fun startConnection(onReady: (() -> Unit)? = null) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "BillingClient conectado")
                    checkExistingPurchases()
                    onReady?.invoke()
                } else {
                    Log.e(TAG, "Error conectando BillingClient: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "BillingClient desconectado, reintentando...")
                // Reintentar después de un delay
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    startConnection()
                }, 5000)
            }
        })
    }

    /**
     * Verifica si el usuario ya compró Premium anteriormente.
     * @param onResult opcional: se llama con true/false segun si se encontro una compra valida.
     * La usa tambien restorePurchases() para darle feedback visible al usuario en PremiumScreen.
     */
    private fun checkExistingPurchases(onResult: ((Boolean) -> Unit)? = null) {
        if (!billingClient.isReady) {
            onResult?.invoke(false)
            return
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.SkuType.INAPP)
                .build()
        ) { _, purchases ->
            var found = false
            for (purchase in purchases) {
                if (purchase.products.contains(PRODUCT_PREMIUM) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    isPurchaseSignatureValid(purchase)
                ) {
                    setPremium(true)
                    // Reconocer la compra si no fue reconocida
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                    found = true
                    break
                }
            }
            if (!found) {
                // No se encontró compra válida
                setPremium(false)
            }
            onResult?.invoke(found)
        }
    }

    /**
     * Restaura compras existentes (ej. tras reinstalar o cambiar de dispositivo) y notifica el
     * resultado, para que la UI pueda mostrar feedback explícito en vez de una restauración muda.
     */
    fun restorePurchases(onResult: (found: Boolean) -> Unit) {
        checkExistingPurchases(onResult)
    }

    /**
     * Verifica la firma RSA de la compra contra la clave publica de licencia de Play Console.
     * Esto evita que un flag premium se pueda otorgar con un objeto Purchase falsificado
     * (por ejemplo, inyectado en un dispositivo rooteado o build modificado).
     * Si LICENSE_PUBLIC_KEY_BASE64 no está configurada, se rechaza toda compra (fail-closed):
     * sin la clave no hay forma de verificar nada, y aceptar por defecto dejaría pasar cualquier
     * compra falsificada. Hay que completar la clave real antes de publicar, sino nadie podrá
     * comprar Premium legítimamente tampoco (usar setPremiumForTesting() para probar mientras tanto).
     */
    private fun isPurchaseSignatureValid(purchase: Purchase): Boolean {
        if (LICENSE_PUBLIC_KEY_BASE64.isBlank()) {
            // fail-closed: sin la clave no hay forma de distinguir una compra real de una
            // armada a mano (JSON + firma inventados), asi que se rechaza en vez de aceptarla.
            // esto tambien bloquea compras reales hasta que se configure la clave, a proposito:
            // es preferible que nadie pueda comprar todavia a que cualquiera pueda falsificar Premium.
            Log.w(TAG, "LICENSE_PUBLIC_KEY_BASE64 no configurada: se rechaza la compra por seguridad")
            return false
        }
        return try {
            val publicKey = java.security.KeyFactory.getInstance("RSA")
                .generatePublic(
                    java.security.spec.X509EncodedKeySpec(
                        android.util.Base64.decode(LICENSE_PUBLIC_KEY_BASE64, android.util.Base64.DEFAULT)
                    )
                )
            val signature = java.security.Signature.getInstance("SHA1withRSA")
            signature.initVerify(publicKey)
            signature.update(purchase.originalJson.toByteArray())
            signature.verify(android.util.Base64.decode(purchase.signature, android.util.Base64.DEFAULT))
        } catch (e: Exception) {
            Log.e(TAG, "Firma de compra invalida", e)
            false
        }
    }

    /**
     * Lanza el flujo de compra para Premium.
     */
    fun launchPurchaseFlow(activity: Activity, onResult: (Boolean) -> Unit) {
        if (!billingClient.isReady) {
            Log.e(TAG, "BillingClient no está listo")
            onResult(false)
            return
        }

        // Consultar detalles del producto
        val productDetailsParamsList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PREMIUM)
                .setProductType(BillingClient.SkuType.INAPP)
                .build()
        )

        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(productDetailsParamsList)
                .build()
        ) { billingResult, productDetailsList ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || productDetailsList.isEmpty()) {
                Log.e(TAG, "Error consultando producto: ${billingResult.debugMessage}")
                onResult(false)
                return@queryProductDetailsAsync
            }

            val productDetails = productDetailsList.first()
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                )
                .build()

            billingClient.launchBillingFlow(activity, flowParams)
            // El resultado llega por PurchasesUpdatedListener
        }
    }

    /**
     * Callback cuando cambia el estado de una compra.
     */
    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.products.contains(PRODUCT_PREMIUM)) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        isPurchaseSignatureValid(purchase)
                    ) {
                        setPremium(true)
                        acknowledgePurchase(purchase)
                    }
                }
            }
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Usuario canceló la compra")
        } else {
            Log.e(TAG, "Error en compra: ${result.debugMessage}")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Compra reconocida exitosamente")
            }
        }
    }

    private fun setPremium(value: Boolean) {
        _isPremium.value = value
        prefs.edit().putBoolean(KEY_IS_PREMIUM, value).apply()
        Log.d(TAG, "Premium status: $value")
    }

    /**
     * Fuerza el estado premium para probar la app como si fuera Premium, sin pasar por Billing.
     * No-op fuera de builds de debug: BuildConfig.DEBUG es una constante `false` fija en release
     * (con isMinifyEnabled=true), asi que R8 elimina esta rama como codigo muerto al compilar el
     * APK que se sube a Play Store. El chequeo se repite aca (no solo en la UI que lo llama) para
     * que sea imposible activarlo por error/reflexion incluso si el boton que lo invoca cambiara.
     */
    fun setPremiumForTesting(value: Boolean) {
        if (!BuildConfig.DEBUG) return
        setPremium(value)
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}
