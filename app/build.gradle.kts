plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.uaa.misgastosapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.uaa.gastos"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        getByName("release") {

            /*isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )*/

            // Activa la ofuscación y reducción de código con R8
            isMinifyEnabled = true

            // para eliminar recursos (imágenes, layouts) que no se usan.
            isShrinkResources = true

            // Especifica los archivos de reglas de ProGuard/R8
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            getByName("debug") {
                isMinifyEnabled = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.animation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    implementation("androidx.navigation:navigation-compose:2.7.5")

    //room

    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // animacion
    implementation("androidx.compose.animation:animation:1.6.0")

    // iconos
    implementation("androidx.compose.material:material-icons-extended")

    // Vico Charts

    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")


    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.11.0")


    // DataStore Preferences
    implementation ("androidx.datastore:datastore-preferences:1.0.0") // Verifica la última versión

    // EncryptedSharedPreferences (para seguridad del token)
    implementation ("androidx.security:security-crypto:1.1.0-alpha06")


    implementation("androidx.datastore:datastore-preferences:1.0.0")


    implementation ("androidx.core:core-ktx:1.12.0' ")

    // Retrofit y networking
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:okhttp:4.11.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Coroutines support para Retrofit
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Encrypted SharedPreferences para almacenar el token de forma segura
    implementation ("androidx.security:security-crypto:1.1.0-alpha06")


    implementation ("androidx.core:core-splashscreen:1.0.1")
}