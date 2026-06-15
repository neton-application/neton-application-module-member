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

// 主应用（提供 module-system / module-infra）
includeBuild("../privchat-application")

// MEMBER-IDENTITY-ADAPTER(M1): 不再 includeBuild privchat 模块 —— member 脱离 privchat 直接依赖。
// privchat 模式由 module-privchat(反向 dependsOn member)在 M2 提供 adapter。
