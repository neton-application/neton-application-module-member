package port

/**
 * 注册欢迎消息端口:注册成功后给新用户发一条系统欢迎消息。
 * 产品装配层(privchat)bind 实现(经 server system-messages/send-to-user);
 * 未装配(builtin)= null,不发。文案由装配层从产品 conf 读取(品牌化)。
 */
interface MemberWelcomePort {
    suspend fun sendWelcome(userId: Long)
}
