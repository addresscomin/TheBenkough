plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    id("org.jetbrains.kotlin.plugin.serialization")
}

val benkoughFirebaseAppId = providers.gradleProperty("BENKOUGH_FIREBASE_APP_ID").orElse(providers.environmentVariable("BENKOUGH_FIREBASE_APP_ID")).orElse("").get()
val benkoughFirebaseApiKey = providers.gradleProperty("BENKOUGH_FIREBASE_API_KEY").orElse(providers.environmentVariable("BENKOUGH_FIREBASE_API_KEY")).orElse("").get()
val benkoughFirebaseProjectId = providers.gradleProperty("BENKOUGH_FIREBASE_PROJECT_ID").orElse(providers.environmentVariable("BENKOUGH_FIREBASE_PROJECT_ID")).orElse("").get()
val benkoughFirebaseSenderId = providers.gradleProperty("BENKOUGH_FIREBASE_SENDER_ID").orElse(providers.environmentVariable("BENKOUGH_FIREBASE_SENDER_ID")).orElse("").get()
val configuredAdMobAppId = providers.gradleProperty("BENKOUGH_ADMOB_APP_ID").orElse(providers.environmentVariable("BENKOUGH_ADMOB_APP_ID")).orElse("").get().trim()
val configuredAdMobBannerId = providers.gradleProperty("BENKOUGH_ADMOB_BANNER_ID").orElse(providers.environmentVariable("BENKOUGH_ADMOB_BANNER_ID")).orElse("").get().trim()
// Invalid AdMob application IDs crash MobileAdsInitProvider before MainActivity can open.
// Use Google's official sample IDs unless valid publisher IDs are configured.
val benkoughAdMobAppId = configuredAdMobAppId.takeIf { Regex("^ca-app-pub-\\d{16}~\\d{10}$").matches(it) }
    ?: "ca-app-pub-3940256099942544~3347511713"
val benkoughAdMobBannerId = configuredAdMobBannerId.takeIf { Regex("^ca-app-pub-\\d{16}/\\d{10}$").matches(it) }
    ?: "ca-app-pub-3940256099942544/9214589741"

android {
    namespace = "com.thebenkough.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.thebenkough.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "SUPABASE_URL", "\"https://gqugbuvknmdbmnojpmbr.supabase.co\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"sb_publishable_uhDy1U2vs9vpF2BuvTzbGQ_4U7-N3kx\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"$benkoughFirebaseAppId\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"$benkoughFirebaseApiKey\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"$benkoughFirebaseProjectId\"")
        buildConfigField("String", "FIREBASE_SENDER_ID", "\"$benkoughFirebaseSenderId\"")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$benkoughAdMobBannerId\"")
        manifestPlaceholders["ADMOB_APP_ID"] = benkoughAdMobAppId
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.icons)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.functions)
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation(libs.ktor.client.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation("com.google.android.gms:play-services-ads:25.5.0")

    debugImplementation(libs.androidx.compose.ui.tooling)
}
