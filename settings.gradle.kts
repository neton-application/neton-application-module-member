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

// Neton base shell（提供 module-system / module-infra）
// canonicalize：统一用 ../../Neton/ 前缀指向 Neton canonical 工作区（跨工作区一致）
includeBuild("../../Neton/neton-application")

// MEMBER-IDENTITY-ADAPTER(M1): 不再 includeBuild privchat 模块 —— member 脱离 privchat 直接依赖。
// privchat 模式由 module-privchat(反向 dependsOn member)在 M2 提供 adapter。
