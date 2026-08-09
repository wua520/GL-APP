# 健录 (FitnessLog) - AI-Powered Fitness Training Assistant

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AI](https://img.shields.io/badge/AI-Multi--Agent-orange.svg)](https://www.deepseek.com/)

[English](#english-version) | 中文

---

## 🎯 为什么需要这个应用？

### 问题：传统健身App的三大痛点

1. **缺乏个性化指导** - 固定模板无法适应个体差异，健身新手不知道如何开始
2. **数据孤立无法联动** - 训练、饮食、身体数据各自独立，无法获得整体性建议
3. **依赖网络且隐私担忧** - 数据上传云端，离线无法使用核心功能

### 解决方案：AI + 本地优先架构

健录采用 **Multi-Agent AI System** 提供专业的训练、饮食和进度分析，同时保证 **数据完全本地化**。AI生成的计划可预览、编辑后再保存，用户始终掌握主动权。

## ✨ 技术亮点

### 🤖 Multi-Agent AI System
**为什么不用单一AI？** 健身涉及训练、营养、恢复等多个专业领域，单一AI容易产生幻觉和不专业的建议。

**我们的方案：**
- **Supervisor Agent**: 统筹调度，理解用户意图，分配任务给专业Agent
- **Training Agent**: 基于运动科学原理生成训练计划，考虑渐进超负荷、训练容量等
- **Nutrition Agent**: 结合用户身体数据和训练强度，提供个性化饮食建议
- **Progress Agent**: 分析历史数据，发现训练瓶颈和进步空间

**技术保障：**
- ✅ **Tool Contract System**: 每个Agent只能调用授权的工具，防止越权操作
- ✅ **Safety Policy**: 医疗建议拦截，防止AI给出危险建议
- ✅ **Audit Log**: 所有AI操作可追溯，便于调试和改进

### 📚 RAG Knowledge Base
**为什么需要RAG？** 纯LLM容易产生幻觉，特别是在健身这种需要专业知识的领域。

**我们的方案：**
- 内置专业健身知识库（训练原理、营养科学、恢复知识）
- 使用向量检索提供相关上下文
- AI回答基于可验证的专业知识，而非随机生成

**技术栈：**
- Qdrant向量数据库 + Ollama本地Embedding
- YAML格式知识文档，支持扩展

### 🔄 本地写入恢复机制
**问题场景：** 用户确认AI生成的训练计划后，因网络问题保存失败，数据丢失。

**我们的方案：**
```
1. AI生成计划 → 展示给用户预览
2. 用户确认 → 调用保存API
3. 网络异常 → 自动保存到本地草案
4. 用户重新联网 → 显示恢复提示，可重试
```

**技术细节：**
- LocalWriteReference机制记录待写入数据
- 幂等性保证：重复提交不会创建重复记录
- agent_action_id关联保证数据一致性

### 🏗️ 架构设计

**Android端：MVVM + Room + Coroutines**
- 完全本地化数据存储，离线可用
- 云端同步为可选功能，非强制

**后端：Spring Boot + MyBatis-Plus**
- RESTful API设计
- JWT认证 + Spring Security
- 幂等性保证和事务一致性

### 🔒 隐私与安全
- ✅ 数据默认完全本地存储
- ✅ AI操作需要用户明确确认
- ✅ 云端同步为可选功能
- ✅ 敏感医疗建议自动拦截

## 📱 应用截图

<p align="center">
  <img src="images/a6c3b0a15747a70c383d0d92c5736b8a.png" width="120" alt="健录图标"/>
</p>

### AI智能助手
<p align="center">
  <img src="images/160df6579cd5636445f7a2146e19cf7b.png" width="250" alt="AI对话"/>
  <img src="images/3e798895fc8dcccdf318868e37806bd4.png" width="250" alt="训练计划生成"/>
  <img src="images/4af7e612784d95316b6588054b5d7775.png" width="250" alt="饮食分析"/>
</p>

### 训练记录与数据统计
<p align="center">
  <img src="images/6a252d9e9dc99cb4692d194ce0b3eec0.png" width="250" alt="训练记录"/>
  <img src="images/79c74e944449ff4838178a72a3ea11f4.png" width="250" alt="数据统计"/>
  <img src="images/d30203c52293ba5c755ee689ef72b748.png" width="250" alt="训练日历"/>
</p>

### 动作库与身体数据
<p align="center">
  <img src="images/f883a44a20f92f11579925f430029cf6.png" width="250" alt="动作库"/>
</p>

## 🚀 核心功能

### AI智能助手系统
与传统的固定模板不同，AI助手能理解你的具体情况：
- 💬 **自然语言交互**: "我想增肌，一周练3天" → AI生成个性化训练计划
- 🎯 **多Agent协作**: Supervisor理解意图 → 专业Agent执行 → 确保建议的专业性
- ✏️ **可编辑草案**: AI生成的计划可预览和修改，不是黑盒
- 🔄 **本地写入恢复**: 网络异常自动保存，防止数据丢失
- 📚 **知识库支持**: 基于专业健身知识，而非随机生成

### 训练记录
完整记录训练过程，为AI分析提供数据基础：
- 📝 详细记录：动作、组数、重量、次数、超级组
- 📊 自动计算：训练量、训练强度、容量统计
- 🏋️ 模板系统：常用训练快速开始
- 📈 AI分析：识别训练瓶颈和进步空间

### 数据统计与分析
不只是展示数据，更提供洞察：
- 📉 可视化图表：训练趋势、肌肉群分析
- 🏆 个人记录追踪：PR突破自动识别
- 🧮 1RM计算器：科学评估力量水平
- 🤖 AI进度分析：发现问题，提出改进建议

### 饮食管理
训练和饮食联动分析：
- 🍎 200+食物数据库：快速记录
- 📊 营养成分自动计算：蛋白质、碳水、脂肪
- 🎯 AI饮食建议：结合训练强度和目标
- 📝 自定义食物：扩展数据库

### 身体数据追踪
为AI提供全面的分析维度：
- ⚖️ 多维度记录：体重、体脂率、围度
- 📈 变化曲线：可视化进步
- 🎯 目标管理：设定目标，追踪进度

## 🛠️ 技术栈

### Android客户端
- **语言**: Kotlin - 现代化、安全的Android开发语言
- **架构**: MVVM - 清晰的职责分离，易于测试和维护
- **数据库**: Room - 类型安全的SQLite抽象层，完全本地化
- **网络**: Retrofit + OkHttp - 可靠的HTTP客户端
- **异步**: Coroutines - 简洁的异步编程
- **图表**: MPAndroidChart - 专业的数据可视化
- **图片**: Glide - 高效的图片加载

### 后端服务器
- **框架**: Spring Boot 3.2.0 - 成熟稳定的企业级框架
- **数据库**: MySQL - 可靠的关系型数据库
- **ORM**: MyBatis-Plus - 灵活的数据访问层
- **认证**: JWT - 无状态的身份验证
- **安全**: Spring Security - 企业级安全框架
- **AI**: DeepSeek LLM - 高性能的中文大模型
- **向量数据库**: Qdrant - 专业的向量检索
- **Embedding**: Ollama - 本地化的向量化服务

### 架构设计理念
**为什么选择本地优先架构？**
1. **隐私保护**: 敏感的身体和训练数据不强制上云
2. **离线可用**: 核心功能不依赖网络
3. **性能优越**: 本地操作响应迅速
4. **成本可控**: 用户可选择是否使用云端服务

**为什么AI操作需要后端？**
1. **算力要求**: LLM推理需要GPU资源
2. **知识库**: RAG检索需要向量数据库
3. **安全控制**: 统一的安全策略和审计日志

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
"" 
