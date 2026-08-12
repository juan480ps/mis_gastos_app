package com.uaa.misgastosapp.ui.components

import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.uaa.misgastosapp.data.AdsManager

/**
 * Banner de AdMob reutilizable.
 * Muestra un anuncio de test o un placeholder visible.
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    testAdUnitId: String = "ca-app-pub-3940256099942544/6300978111"
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isAdsReady by AdsManager.isReady.collectAsState()

    // Estado para saber si el ad cargó
    var adLoaded by remember { mutableStateOf(false) }
    // evita pedir el anuncio mas de una vez para el mismo AdView.
    var adRequested by remember { mutableStateOf(false) }

    // Refresh el ad cuando la pantalla vuelve a estar visible
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("AdBanner", "Screen resumed, refreshing ad")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp) // Altura fija siempre
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F5F5))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        // AndroidView que crea UN SOLO AdView y lo reutiliza
        AndroidView(
            factory = { ctx ->
                Log.d("AdBanner", "Creating single AdView instance")
                
                AdView(ctx).apply {
                    // Configurar tamaño
                    setAdSize(AdSize.BANNER)
                    
                    // ID del anuncio de prueba
                    adUnitId = testAdUnitId
                    
                    // Layout params
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    
                    // Listener
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Log.d("AdBanner", "✅ Ad loaded successfully!")
                            adLoaded = true
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.e("AdBanner", "❌ Ad failed: ${error.message} (code ${error.code})")
                            adLoaded = false
                            // se reintenta en la siguiente recomposicion si isAdsReady sigue en true.
                            adRequested = false
                        }
                    }
                }
            },
            update = { adView ->
                // se espera a que MobileAds termine de inicializarse antes de pedir el anuncio;
                // pedirlo antes (como hacia esta funcion originalmente) podia fallar segun que
                // pantalla se abriera primero al iniciar la app.
                if (isAdsReady && !adRequested) {
                    adRequested = true
                    adView.loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier
                .fillMaxSize()
        )
        
        // Placeholder visible mientras carga o si falla
        if (!adLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE8EAF6)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📢 Publicidad",
                    fontSize = 12.sp,
                    color = Color(0xFF3F51B5),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
