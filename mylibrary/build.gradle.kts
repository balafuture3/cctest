plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id ("maven-publish")
}

android {
    namespace = "com.clearcaptions.mylibrary"
    compileSdk = 34

    defaultConfig {
        minSdk = 19
        multiDexEnabled =true
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation(files("libs/commons-codec-1.3.jar"))
    implementation(files("libs/log4j-1.2.15.jar"))
    implementation(files("libs/slf4j-api-1.5.6.jar"))
    implementation(files("libs/slf4j-jdk14-1.5.6.jar"))
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))

    // define any required OkHttp artifacts without version
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.balafuture3"
            artifactId = "mylibrary"
            version = "1.0"
            afterEvaluate {
                artifact(tasks.getByName("bundleReleaseAar"))
            }
        }
    }
}