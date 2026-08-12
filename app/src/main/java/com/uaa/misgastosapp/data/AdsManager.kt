package com.uaa.misgastosapp.data

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MobileAds.initialize() es asincrono (recibe un callback). Si un AdView llama a loadAd() antes
 * de que ese callback dispare, la carga puede fallar. Esto explicaba que el banner funcionara en
 * algunas pantallas y no en otras: dependia de que esa pantalla se abriera antes o despues de que
 * terminara la inicializacion (la primera pantalla que se muestra al abrir la app es la mas
 * propensa a perder la carrera). AdBanner ahora espera a isReady antes de pedir un anuncio.
 */
object AdsManager {
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    fun initialize(context: Context) {
        // Declara explicitamente que la app no esta dirigida a niños ni a audiencias que
        // requieran consentimiento parental, para que AdMob sirva anuncios acordes (relevante
        // para la Data Safety Section de Play Console: sin esto queda sin declarar).
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
                .build()
        )
        MobileAds.initialize(context) {
            _isReady.value = true
        }
    }
}
