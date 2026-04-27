package com.fitness.training.config

/**
 * 应用配置
 * 用于管理服务器地址、功能开关等配置
 */
object AppConfig {
    
    /**
     * 服务器配置
     */
    object Server {
        // 是否启用云端功能
        // 上线初期设置为false，等服务器准备好后改为true
        const val CLOUD_ENABLED = false
        
        // 服务器地址
        // 等你有服务器后，替换为你的域名，例如: "https://api.yourapp.com/"
        const val BASE_URL = "https://api.yourapp.com/"
        
        // 服务器状态提示
        const val SERVER_STATUS_MESSAGE = "云端同步功能即将上线，敬请期待！"
    }
    
    /**
     * AI助手配置
     */
    object AI {
        // 是否启用AI助手
        // DeepSeek API需要密钥，上线初期可以禁用
        const val AI_ENABLED = false
        
        // AI功能提示
        const val AI_STATUS_MESSAGE = "AI助手功能即将上线，敬请期待！"
    }
    
    /**
     * 功能开关
     */
    object Features {
        // 是否显示云端账号相关功能
        const val SHOW_CLOUD_ACCOUNT = false
        
        // 是否显示AI助手入口
        const val SHOW_AI_ASSISTANT = false
    }
}
