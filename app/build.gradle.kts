plugins { id("com.android.application") }

android {
    namespace = "com.bilifix.themefix"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bilifix.themefix"
        minSdk = 26
        // Xposed 模块不走 PackageManager 安装流程，无需签名配置
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // Xposed 模块没有资源，只需把 assets 打进包里
    sourceSets["main"].assets.srcDirs("src/main/assets")
}

// 用仓库内自带的 stub 源码生成编译期 jar，避免分发第三方 API 包
val compileXposedStubs = tasks.register<JavaCompile>("compileXposedStubs") {
    source = fileTree("$rootDir/stubs") { include("**/*.java") }
    classpath = files()
    destinationDirectory.set(layout.buildDirectory.dir("stubs/classes"))
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

val xposedStubsJar = tasks.register<Jar>("xposedStubsJar") {
    dependsOn(compileXposedStubs)
    archiveFileName.set("xposed-stubs.jar")
    destinationDirectory.set(layout.buildDirectory.dir("stubs"))
    from(compileXposedStubs.map { it.destinationDirectory })
}

dependencies {
    compileOnly(files(xposedStubsJar.map { it.archiveFile }))
}
