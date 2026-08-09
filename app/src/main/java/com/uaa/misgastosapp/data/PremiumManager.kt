package com.uaa.misgastosapp.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
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

        @Volatile
        private var instance: PremiumManager? = null

        fun getInstance(context: Context): PremiumManager {
            return instance ?: synchronized(this) {
                instance ?: PremiumManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
     */
    private fun checkExistingPurchases() {
        if (!billingClient.isReady) return

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.SkuType.INAPP)
                .build()
        ) { _, purchases ->
            for (purchase in purchases) {
                if (purchase.products.contains(PRODUCT_PREMIUM) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                ) {
                    setPremium(true)
                    // Reconocer la compra si no fue reconocida
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                    return@queryPurchasesAsync
                }
            }
            // No se encontró compra válida
            setPremium(false)
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
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
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
     * Para testing: fuerza el estado premium.
     */
    fun setPremiumForTesting(value: Boolean) {
        setPremium(value)
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}
