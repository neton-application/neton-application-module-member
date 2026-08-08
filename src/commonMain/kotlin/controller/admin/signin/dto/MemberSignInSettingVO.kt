package controller.admin.signin.dto

import kotlinx.serialization.Serializable

/**
 * 签到的全局设置项：库里的当前值 + 代码里的定义合并后的视图。
 *
 * 字段与 system 的 `SystemSettingVO` 一致，前端可共用一套类型。这里单独定义而不是
 * 复用那个类，是因为两者的**可见范围**不同：系统设置页面向超管开放全部设置项，
 * 这里只开放签到那几项给运营。同一份数据、两个权限边界，各自有各自的出口。
 */
@Serializable
data class MemberSignInSettingVO(
    val category: String,
    val key: String,
    val value: String,
    /** SettingValueType 序号，后台据此选控件 */
    val valueType: Int,
    val name: String,
    val description: String,
    val defaultValue: String,
    /** true = 当前值就是默认值（后台没改过） */
    val isDefault: Boolean,
)

@Serializable
data class UpdateMemberSignInSettingRequest(val key: String, val value: String)
