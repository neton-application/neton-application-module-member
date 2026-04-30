pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

rootProject.name = "module-member"

// 框架
includeBuild("../neton")

// 主应用（提供 module-system）
includeBuild("../privchat-application")

// PrivChat IM 接入模块（提供 :client / :hook 接口）
includeBuild("../neton-application-module-privchat")
