# 健录 - 专业健身训练记录应用

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot-brightgreen.svg)](https://spring.io/projects/spring-boot)

健录是一款为认真对待健身的训练者打造的专业记录工具。它提供完整的训练日志系统，支持详细记录每个动作的组数、重量、次数和超级组训练，内置95个动作库覆盖全身肌群；配备科学的饮食管理功能，拥有200+食物数据库可自动计算营养成分和每日热量摄入；通过可视化图表展示训练趋势、肌肉群分析和个人记录突破，帮助你追踪体重、体脂率等身体数据变化。

## 应用截图

<p align="center">
  <img src="images/a6c3b0a15747a70c383d0d92c5736b8a.png" width="120" alt="健录图标"/>
</p>

<p align="center">
  <img src="images/160df6579cd5636445f7a2146e19cf7b.png" width="200" alt="训练记录"/>
  <img src="images/3e798895fc8dcccdf318868e37806bd4.png" width="200" alt="数据统计"/>
  <img src="images/4af7e612784d95316b6588054b5d7775.png" width="200" alt="饮食管理"/>
  <img src="images/6a252d9e9dc99cb4692d194ce0b3eec0.png" width="200" alt="身体数据"/>
</p>

<p align="center">
  <img src="images/79c74e944449ff4838178a72a3ea11f4.png" width="200" alt="动作库"/>
  <img src="images/d30203c52293ba5c755ee689ef72b748.png" width="200" alt="训练日历"/>
  <img src="images/f883a44a20f92f11579925f430029cf6.png" width="200" alt="训练详情"/>
</p>

## 功能特性

### 🤖 AI智能助手（核心功能）
- **多领域专业Agent**: 训练、饮食、进度分析三大智能助手
- **自然语言交互**: 用对话方式获取专业建议和计划
- **智能训练计划**: 基于个人数据生成定制训练方案
- **饮食指导**: AI分析饮食记录并提供营养建议
- **可编辑草案**: AI生成的计划可预览、编辑后保存
- **本地写入恢复**: 网络异常时自动保存，支持手动重试
- **RAG知识库**: 基于专业健身知识提供准确建议

### 训练记录
- 详细记录每次训练的动作、组数、重量、次数
- 支持超级组训练记录
- 自动计算训练量和训练时长
- 训练模板功能，快速开始常用训练
- AI生成的训练计划可直接保存到本地

### 数据统计
- 可视化图表展示训练趋势
- 肌肉群训练分析
- 个人记录（PR）追踪
- 1RM计算器
- AI智能分析训练进度和瓶颈

### 饮食管理
- 200+食物数据库
- 营养成分自动计算
- 每日热量和营养素追踪
- 自定义食物添加
- AI饮食建议和营养分析

### 身体数据
- 体重、体脂率、围度等多维度记录
- 身体数据变化曲线
- 目标设定和进度追踪

### 动作库
- 95个训练动作，涵盖各大肌群
- 按肌肉群分类查找
- 收藏常用动作

### 训练日历
- 日历视图查看训练历史
- 训练频率统计
- 休息日提醒

## 技术栈

### Android客户端
- **语言**: Kotlin
- **架构**: MVVM
- **数据库**: Room
- **网络**: Retrofit + OkHttp
- **异步**: Coroutines
- **图表**: MPAndroidChart
- **图片加载**: Glide
- **依赖注入**: 手动依赖注入

### 后端服务器
- **框架**: Spring Boot 3.2.0
- **数据库**: MySQL
- **ORM**: MyBatis-Plus
- **认证**: JWT
- **安全**: Spring Security
- **AI集成**: DeepSeek LLM
- **知识库**: RAG + Qdrant向量数据库（可选）
- **Agent系统**: 多Agent编排与工具调用

## 项目结构

```
.
├── app/                          # Android应用
│   ├── src/main/
│   │   ├── java/com/fitness/training/
│   │   │   ├── data/            # 数据层
│   │   │   │   ├── entity/      # 实体类
│   │   │   │   ├── dao/         # 数据访问对象
│   │   │   │   └── database/    # 数据库配置
│   │   │   ├── network/         # 网络层
│   │   │   ├── ui/              # UI层
│   │   │   ├── util/            # 工具类
│   │   │   ├── ai/              # AI助手
│   │   │   └── config/          # 配置
│   │   └── res/                 # 资源文件
│   └── build.gradle.kts
├── fitness-server/              # Spring Boot后端
│   ├── src/main/
│   │   ├── java/com/fitness/server/
│   │   │   ├── agent/           # AI Agent系统
│   │   │   ├── knowledge/       # RAG知识库
│   │   │   ├── controller/      # 控制器
│   │   │   ├── service/         # 业务逻辑
│   │   │   ├── entity/          # 实体类
│   │   │   ├── mapper/          # MyBatis映射
│   │   │   └── util/            # 工具类
│   │   └── resources/
│   │       ├── application.yml  # 配置文件（需自行创建）
│   │       ├── schema.sql       # 数据库脚本
│   │       ├── sql/             # 数据库迁移脚本
│   │       └── knowledge/       # 知识库文档
│   └── pom.xml
└── README.md
```

## 快速开始

### Android客户端

#### 环境要求
- Android Studio Arctic Fox或更高版本
- JDK 17
- Android SDK 24+（最低支持Android 7.0）

#### 构建步骤
1. 克隆项目
```bash
git clone https://github.com/wua520/fitness-app.git
cd fitness-app
```

2. 打开Android Studio，导入项目

3. 同步Gradle依赖

4. 运行应用

#### 签名配置（可选）
如果需要生成release版本，创建`keystore.properties`文件：
```properties
storeFile=your-keystore-file.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

### 后端服务器（可选）

**注意**: AI助手功能需要后端服务器支持。应用可以完全离线使用基础功能，后端服务器用于云端同步和AI助手。

#### 环境要求
- JDK 17
- MySQL 8.0+
- Maven 3.6+
- DeepSeek API Key（用于AI功能）
- Qdrant向量数据库（可选，用于RAG知识库）

#### 部署步骤
1. 创建数据库
```bash
mysql -u root -p
CREATE DATABASE fitness_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fitness_db;
source fitness-server/src/main/resources/schema.sql;
source fitness-server/src/main/resources/sql/agent_tables.sql;
source fitness-server/src/main/resources/sql/f0_add_training_plan_agent_action_id.sql;
source fitness-server/src/main/resources/sql/sync_diet_record_agent_fields.sql;
source fitness-server/src/main/resources/sql/agent_migration_local_write.sql;
source fitness-server/src/main/resources/sql/f2_knowledge_base.sql;
```

2. 配置应用
创建`fitness-server/src/main/resources/application-local.yml`：
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fitness_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: your-username
    password: your-password
    driver-class-name: com.mysql.cj.jdbc.Driver

jwt:
  secret: your-jwt-secret-key-at-least-32-characters-long
  expiration: 604800000  # 7天

# AI配置
deepseek:
  api-key: your-deepseek-api-key
  base-url: https://api.deepseek.com

# RAG知识库配置（可选）
knowledge:
  enabled: true
  qdrant:
    host: localhost
    port: 6333
  embedding:
    model: nomic-embed-text
    base-url: http://localhost:11434  # Ollama服务地址
```

3. 启动Qdrant（可选，用于RAG知识库）
```bash
docker run -p 6333:6333 qdrant/qdrant
```

4. 启动Ollama（可选，用于本地embedding）
```bash
# 安装Ollama后运行
ollama pull nomic-embed-text
ollama serve
```

5. 启动服务器
```bash
cd fitness-server
mvn clean package
java -jar target/fitness-server-1.0.0.jar --spring.profiles.active=local
```

4. 修改Android客户端配置
在`app/src/main/java/com/fitness/training/config/AppConfig.kt`中：
```kotlin
object Server {
    const val CLOUD_ENABLED = true
    const val BASE_URL = "http://your-server-ip:8080/"
}
```

## 配置说明

### 功能开关
在`AppConfig.kt`中可以控制功能开关：
- `CLOUD_ENABLED`: 是否启用云端同步
- `AI_ENABLED`: 是否启用AI助手
- `SHOW_CLOUD_ACCOUNT`: 是否显示云端账号功能
- `SHOW_AI_ASSISTANT`: 是否显示AI助手入口

### AI助手配置
要使用AI助手功能，需要：

1. **后端配置**: 在`application-local.yml`中配置DeepSeek API密钥
2. **Android配置**: 确保`AppConfig.kt`中启用AI功能
```kotlin
object Features {
    const val AI_ENABLED = true
    const val SHOW_AI_ASSISTANT = true
}

object Server {
    const val CLOUD_ENABLED = true
    const val BASE_URL = "http://your-server-ip:8080/"
}
```

### RAG知识库（可选）
知识库功能可以让AI助手提供更专业的建议：
- 默认包含训练原理、营养基础、恢复知识等专业文档
- 支持自定义添加知识文档（YAML格式）
- 使用向量检索提供相关上下文

## 应用特色

- **AI智能助手**: 专业的训练、饮食、进度分析Agent系统
- **多Agent协作**: Supervisor统筹调度，专业领域分工明确
- **本地优先**: 完全本地存储，AI生成内容可离线编辑
- **界面简洁**: 采用灰白配色，专业低调
- **数据本地**: 完全本地存储，保护隐私
- **完全免费**: 无广告，无内购
- **离线使用**: 基础功能无需网络即可使用
- **支持深色模式**: 适应不同使用场景
- **安全可控**: AI操作需用户确认，支持本地写入恢复

## 版本信息

- **当前版本**: 1.0
- **最低Android版本**: Android 7.0 (API 24)
- **目标Android版本**: Android 14 (API 34)

## 隐私政策

应用数据完全存储在本地设备，不会上传到任何服务器（除非用户主动启用云端同步功能）。

详细隐私政策：[隐私政策](https://gist.githubusercontent.com/wua520/31ef1480afc1b3bfe1c7b1e1f41d453f/raw)

## 用户协议

[用户协议](https://gist.githubusercontent.com/wua520/4f5e6d27f7c5d165a3c400dc947b0ddf/raw)

## 开发者

- **邮箱**: w303363639@gmail.com

## 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

## 贡献

欢迎提交Issue和Pull Request！

## 致谢

感谢所有开源库的作者和贡献者。

---

**注意**: 本项目仅供学习交流使用。
