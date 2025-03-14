plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
    id("org.lsposed.lsparanoid")

}

android {
    namespace = "com.harshil258.adplacer"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }


    lsparanoid {
        seed = null
        classFilter = { true }
        includeDependencies = false
        variantFilter = { true }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.firebase.config.ktx)
    implementation(libs.app.update.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.sdp.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit2.converter.gson)
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    implementation(libs.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.play.services.ads)
    implementation(libs.lottie)
    implementation(libs.androidx.lifecycle.process)

    implementation(libs.facebook)
    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.config)
    implementation(libs.firebase.analytics)

}


afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
            }
        }
    }
}