import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Credenciales de firma: se leen de local.properties (no versionado) o variables de entorno
// (para CI), nunca hardcodeadas en este archivo. Si no estan definidas, el build de release
// queda sin firmar como hasta ahora, y se firma manualmente o via Play App Signing.
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        load(FileInputStream(localPropsFile))
    }
}
fun signingProp(key: String): String? = localProperties.getProperty(key) ?: System.getenv(key)

// Config de release (IDs de AdMob, clave de licencia de Billing): se lee de local.properties o
// variables de entorno (para CI), con los valores de desarrollo/test actuales como default. Asi
// el build sigue funcionando igual que antes sin tocar nada, pero para publicar alcanza con pegar
// los valores reales en local.properties en vez de editar este archivo.
fun stringProp(key: String, default: String): String = localProperties.getProperty(key) ?: System.getenv(key) ?: default

android {
    namespace = "com.uaa.misgastosapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.uaa.gastos"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "ADMOB_APP_ID", "\"${stringProp("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")}\"")
        buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"${stringProp("ADMOB_BANNER_AD_UNIT_ID", "ca-app-pub-3940256099942544/6300978111")}\"")
        // Clave publica de licencia (Base64) de Play Console > Monetization setup > Licensing.
        // Vacia por defecto: sin ella, PremiumManager rechaza toda compra por seguridad (fail-closed).
        buildConfigField("String", "LICENSE_PUBLIC_KEY_BASE64", "\"${stringProp("LICENSE_PUBLIC_KEY_BASE64", "")}\"")

        manifestPlaceholders["adMobAppId"] = stringProp("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
    }

    signingConfigs {
        create("release") {
            val keystorePath = signingProp("MIS_GASTOS_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = signingProp("MIS_GASTOS_KEYSTORE_PASSWORD")
                keyAlias = signingProp("MIS_GASTOS_KEY_ALIAS")
                keyPassword = signingProp("MIS_GASTOS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingProp("MIS_GASTOS_KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        getByName("debug") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // sin esto, cualquier llamada a un metodo de android.jar (Log.d/Log.e, etc.) en un
            // test unitario puro (sin Robolectric) lanza "Method ... not mocked".
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation)
    implementation("androidx.compose.animation:animation:1.6.0")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // Room (with KSP)
    implementation("androidx.room:room-runtime:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")

    // SQLCipher: cifra el archivo de la base de datos Room en disco (datos financieros en reposo).
    implementation("net.zetetic:sqlcipher-android:4.17.0@aar")
    implementation("androidx.sqlite:sqlite:2.6.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Security & Storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Google Sign-In (Credential Manager)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // WorkManager (Background Processing)
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    // Biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Google Play Billing (In-App Purchases)
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    // AdMob
    implementation("com.google.android.gms:play-services-ads:24.4.0")

    // Testing
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.14.2")
    testImplementation("app.cash.turbine:turbine:1.2.0")
    
    // Android Testing (E2E)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
