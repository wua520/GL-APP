# 健录 (FitnessLog) - AI-Powered Fitness Training Assistant

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AI](https://img.shields.io/badge/AI-Multi--Agent-orange.svg)](https://www.deepseek.com/)

中文 | [English](#english-version)

---

## 🎯 为什么需要这个应用？
## 🎯 Why This App?

### 问题：传统健身App的三大痛点
### Problem: Three Major Pain Points of Traditional Fitness Apps

#### 1. 缺乏真正的个性化指导
#### 1. Lack of True Personalization

市面上大多数健身App提供的是固定的训练模板：初学者计划、增肌计划、减脂计划等。但每个人的情况都不同：

*Most fitness apps offer fixed training templates: beginner programs, muscle gain plans, fat loss plans, etc. However, everyone's situation is different:*

- **训练基础不同**：新手和老手的训练容量、恢复能力完全不同
  - *Different training backgrounds: Beginners and advanced lifters have completely different training capacities and recovery abilities*
- **时间安排不同**：有人一周能练5天，有人只能练3天
  - *Different schedules: Some can train 5 days a week, others only 3*
- **身体状况不同**：有人膝盖有伤，有人腰椎间盘突出
  - *Different physical conditions: Some have knee injuries, others have herniated discs*
- **目标不同**：有人想增肌，有人想提高运动表现，有人只是想健康
  - *Different goals: Muscle gain, athletic performance, or just staying healthy*

固定模板无法考虑这些个体差异，导致：新手不知道从哪里开始，老手觉得计划太简单或不适合，有伤病的人不敢练。

*Fixed templates can't account for these individual differences, resulting in: beginners not knowing where to start, advanced users finding plans too simple or unsuitable, and injured individuals afraid to train.*

#### 2. 数据孤立，无法提供整体性建议
#### 2. Isolated Data, Unable to Provide Holistic Advice

大多数App把功能模块割裂开：

*Most apps fragment their features into isolated modules:*

- **训练模块**：只记录你练了什么
  - *Training module: Only records what you trained*
- **饮食模块**：只记录你吃了什么
  - *Diet module: Only records what you ate*
- **身体数据**：只记录你的体重变化
  - *Body data: Only records your weight changes*

但实际上，这三者是紧密关联的：

*In reality, these three are tightly interconnected:*

- 你今天练了大重量深蹲，应该多吃碳水帮助恢复
  - *You did heavy squats today? You should eat more carbs to aid recovery*
- 你最近体重停滞了，可能是训练容量不够，或者饮食摄入不足
  - *Your weight plateaued? It might be insufficient training volume or inadequate nutrition*
- 你的力量进步很快，但体重没变，说明你在"重组身体成分"
  - *Your strength is increasing rapidly but weight unchanged? You're "recomposing" your body*

**没有AI的帮助，用户很难把这些数据串联起来，得出有价值的洞察。**

***Without AI assistance, users struggle to connect these data points and derive valuable insights.***

#### 3. 依赖网络且隐私担忧
#### 3. Network Dependency and Privacy Concerns

许多健身App强制要求注册账号、上传数据到云端：

*Many fitness apps force account registration and cloud data upload:*

- **离线不可用**：没网络就打不开App，更别说记录训练
  - *Offline unavailable: Can't even open the app without internet, let alone log workouts*
- **隐私风险**：你的身体数据、训练习惯都在别人的服务器上
  - *Privacy risks: Your body data and training habits are on someone else's servers*
- **服务依赖**：公司倒闭或停服，你的数据就没了
  - *Service dependency: If the company shuts down, your data is gone*

对于重视隐私的用户来说，这是不可接受的。

*For privacy-conscious users, this is unacceptable.*

### 解决方案：AI + 本地优先架构
### Solution: AI + Local-First Architecture

健录的核心理念是：**用AI解决个性化问题，用本地化保护隐私**。

*FitnessLog's core philosophy: **Use AI to solve personalization, use local-first to protect privacy**.*

#### Multi-Agent AI System

我们不是简单地把ChatGPT接入App，而是构建了一个**专业的Multi-Agent系统**：

*We didn't simply integrate ChatGPT into the app, but built a **professional Multi-Agent system**:*

- **Supervisor Agent** 理解你的需求，判断需要哪个领域的专家
  - *Understands your needs and determines which domain expert is required*
- **Training Agent** 是训练专家，懂渐进超负荷、训练容量、周期化
  - *Training expert who understands progressive overload, training volume, and periodization*
- **Nutrition Agent** 是营养专家，会根据你的目标和训练强度给建议
  - *Nutrition expert who provides advice based on your goals and training intensity*
- **Progress Agent** 是数据分析师，帮你发现训练中的问题和进步
  - *Data analyst who helps you discover training issues and progress*

**为什么这样设计？** 因为健身是个多学科交叉的领域，单一AI容易"似是而非"，多Agent分工协作才能保证专业性。

***Why this design?** Because fitness is a multidisciplinary field. A single AI tends to be "seemingly correct but actually wrong." Multiple specialized agents working together ensure professionalism.*

#### 本地优先 + AI可编辑
#### Local-First + Editable AI

- **数据完全本地存储**：你的所有数据都在手机上，不强制上云
  - *Complete local data storage: All your data stays on your phone, no forced cloud upload*
- **AI生成的内容可预览和编辑**：不是黑盒，你可以调整AI的建议
  - *AI-generated content is previewable and editable: Not a black box, you can adjust AI suggestions*
- **离线依然可用**：基础功能（训练记录、数据统计）完全不需要网络
  - *Offline functionality: Core features (workout logging, statistics) work completely offline*
- **云端同步是可选的**：需要多设备同步或AI助手时才连接服务器
  - *Optional cloud sync: Only connect to server when you need multi-device sync or AI assistant*

这种架构平衡了**AI的智能化**和**本地化的隐私保护**，用户始终掌握主动权。

*This architecture balances **AI intelligence** with **local privacy protection**, keeping users always in control.*

## ✨ 技术亮点
## ✨ Technical Highlights

### 🤖 Multi-Agent AI System

#### 为什么不用单一AI？
#### Why Not a Single AI?

健身是一个高度专业化的领域，涉及运动生理学、营养学、康复医学等多个学科。单一的通用AI（如直接接入ChatGPT）存在严重问题：

*Fitness is a highly specialized field involving exercise physiology, nutrition, rehabilitation medicine, and more. A single general-purpose AI (like directly integrating ChatGPT) has serious issues:*

- **知识不够深入**：对于"渐进超负荷"、"训练容量"、"RPE"等专业概念理解肤浅
  - *Insufficient depth: Shallow understanding of professional concepts like "progressive overload," "training volume," "RPE"*
- **容易产生幻觉**：可能编造不存在的训练方法或营养建议
  - *Prone to hallucinations: May fabricate non-existent training methods or nutritional advice*
- **无法整合数据**：不知道如何关联训练数据、饮食数据、身体数据
  - *Cannot integrate data: Doesn't know how to correlate training, diet, and body data*
- **缺乏安全边界**：可能给出危险的医疗建议
  - *Lacks safety boundaries: May give dangerous medical advice*

#### 我们的Multi-Agent架构
#### Our Multi-Agent Architecture

```
用户请求 → Supervisor Agent（任务分发）
User Request → Supervisor Agent (Task Distribution)
              ↓
    ┌─────────┼─────────┐
    ↓         ↓         ↓
Training  Nutrition  Progress
 Agent      Agent      Agent
    ↓         ↓         ↓
工具调用（读取数据、生成计划、保存结果）
Tool Calls (Read data, Generate plans, Save results)
```

##### **Supervisor Agent - 任务调度中心**
##### **Supervisor Agent - Task Orchestration Center**

职责：理解用户意图，决定调用哪个专业Agent

*Responsibility: Understand user intent and decide which specialized agent to call*

- 用户说"我想增肌" → 调用Training Agent生成训练计划
  - *User says "I want to build muscle" → Call Training Agent to generate training plan*
- 用户说"我今天吃了什么" → 调用Nutrition Agent分析营养摄入
  - *User says "What did I eat today" → Call Nutrition Agent to analyze nutrition intake*
- 用户说"最近进步慢了" → 调用Progress Agent分析训练数据
  - *User says "My progress slowed down" → Call Progress Agent to analyze training data*

技术保障：

*Technical Safeguards:*

- **意图识别**：基于LLM理解自然语言，提取关键信息（目标、频率、限制条件等）
  - *Intent Recognition: LLM-based natural language understanding to extract key information (goals, frequency, constraints, etc.)*
- **上下文管理**：记录对话历史，支持多轮对话
  - *Context Management: Record conversation history, support multi-turn dialogue*
- **安全拦截**：识别医疗相关请求，拒绝给出诊断或治疗建议
  - *Safety Interception: Identify medical-related requests, refuse to give diagnosis or treatment advice*

##### **Training Agent - 训练计划专家**
##### **Training Agent - Training Plan Expert**

职责：生成科学的训练计划，考虑渐进超负荷原则

*Responsibility: Generate scientific training plans considering progressive overload principles*

- 读取用户的历史训练数据（最近练了什么、力量水平如何）
  - *Read user's training history (recent workouts, strength levels)*
- 读取用户的身体数据（身高体重、训练经验）
  - *Read user's body data (height, weight, training experience)*
- 基于运动科学原理生成计划：
  - *Generate plans based on exercise science:*
  - **选择合适的动作**：考虑目标肌群、设备可用性
    - *Select appropriate exercises: Consider target muscle groups and equipment availability*
  - **安排训练容量**：组数、次数、重量的科学配比
    - *Arrange training volume: Scientific ratio of sets, reps, and weight*
  - **考虑恢复时间**：不会让同一肌群连续大重量训练
    - *Consider recovery time: Avoid consecutive heavy training of the same muscle group*

授权工具：

*Authorized Tools:*

- `read_user_profile`：读取用户基本信息 / *Read user basic information*
- `read_training_history`：读取历史训练记录 / *Read training history*
- `read_body_data`：读取身体数据变化 / *Read body data changes*
- `generate_training_plan`：生成训练计划草案 / *Generate training plan draft*
- `save_training_plan_draft`：保存待用户确认的计划 / *Save plan pending user confirmation*

##### **Nutrition Agent - 营养分析专家**
职责：分析饮食摄入，提供营养建议
- 读取用户今天的饮食记录
- 读取用户今天的训练强度
- 计算营养素摄入（蛋白质、碳水、脂肪、热量）
- 基于目标（增肌/减脂/保持）给出建议

专业性保证：
- 蛋白质摄入：增肌期1.6-2.2g/kg体重/天
- 碳水化合物：根据训练强度调整（大重量训练日多吃碳水）
- 脂肪：不低于总热量的20%，保证激素合成

##### **Progress Agent - 数据分析专家**
职责：分析训练进度，发现问题和机会
- 对比不同时期的力量数据（是否进步）
- 分析训练频率和容量（是否过度训练或训练不足）
- 结合体重变化分析身体成分变化
- 识别瓶颈：某个肌群长期没练、某个动作长期没进步

#### 技术保障机制

##### 1. Tool Contract System（工具契约系统）
**问题**：如果Nutrition Agent能调用`delete_all_training_data`怎么办？

**解决方案**：每个Agent只能调用明确授权的工具
```java
@Agent(name = "nutrition")
@AuthorizedTools({
    "read_diet_records",
    "read_training_summary", 
    "generate_diet_advice"
})
public class NutritionAgent {
    // 编译时保证：只能调用授权的工具
    // 运行时检查：调用未授权工具会被拦截
}
```

##### 2. Safety Policy（安全策略）
**问题**：用户问"我膝盖疼怎么办"，AI不能诊断和开药

**解决方案**：多层安全检查
- **医疗关键词检测**：疼痛、受伤、疾病、药物等
- **免责声明**：明确告知"我不是医生，建议咨询专业医师"
- **工具权限限制**：AI无法访问用户的医疗记录

##### 3. Audit Log（审计日志）
**作用**：可追溯性和可调试性
- 记录每次AI调用：输入、输出、调用的工具、执行结果
- 记录用户确认操作：什么时候确认、确认了什么内容
- 记录失败情况：为什么失败、如何恢复

**实际应用**：
- 开发阶段：发现AI的错误模式，改进Prompt
- 生产阶段：用户反馈问题时，可以回溯当时的对话

### 📚 RAG Knowledge Base（检索增强生成）

#### 为什么纯LLM不够？
大语言模型（LLM）虽然强大，但在专业领域存在致命缺陷：
1. **知识截止日期**：模型训练时的数据是有时间限制的，无法获取最新研究
2. **幻觉问题**：在不确定时会"编造"看似合理但实际错误的信息
3. **缺乏引用**：无法说明信息来源，用户无法验证

**在健身领域的风险**：
- AI说"深蹲膝盖不能超过脚尖"（这是过时的观点）
- AI说"增肌需要每天吃10个鸡蛋"（不科学且可能有害）
- AI说"腰疼可以练硬拉"（可能加重伤病）

#### RAG架构：让AI基于可验证的知识回答

```
用户提问 → 向量化查询
              ↓
         知识库检索（相关文档）
              ↓
         LLM生成回答（基于检索到的文档）
              ↓
         返回答案 + 知识来源
```

##### 知识库内容
我们内置了专业的健身知识文档（YAML格式）：

**训练原理**（training-principles-2026.03.yml）
- 渐进超负荷原理：如何科学增加训练强度
- 训练容量：组数、次数、频率的科学配比
- 周期化训练：如何安排不同阶段的训练
- 肌肥大机制：机械张力、代谢压力、肌肉损伤

**营养基础**（nutrition-basics-2026.03.yml）
- 三大营养素：蛋白质、碳水、脂肪的作用和摄入量
- 增肌期营养：热量盈余、蛋白质摄入、碳水时机
- 减脂期营养：热量赤字、保持肌肉、饮食心理
- 补剂指南：肌酸、蛋白粉、BCAA等的作用和必要性

**恢复知识**（recovery-basics-2026.03.yml）
- 睡眠的重要性：生长激素分泌、肌肉修复
- 主动恢复：拉伸、泡沫轴、轻度有氧
- 过度训练识别：如何判断是否需要减量

**产品帮助**（product-help-2026.03.yml）
- 如何使用训练记录功能
- 如何设置训练目标
- 如何查看进度统计

##### 技术实现

**向量化（Embedding）**
使用Ollama本地运行`nomic-embed-text`模型：
- 知识文档被切分成小块（chunk），每块向量化
- 用户问题也被向量化
- 通过向量相似度找到最相关的知识块

**向量存储（Qdrant）**
- 高性能的向量数据库
- 支持过滤和混合搜索
- 可以根据文档类型、日期、标签筛选

**检索策略**
```java
// 1. 向量化用户问题
float[] queryVector = embeddingClient.embed(userQuestion);

// 2. 检索Top-K相关文档
List<KnowledgeChunk> chunks = vectorStore.search(queryVector, topK=3);

// 3. 构建上下文
String context = chunks.stream()
    .map(c -> c.getContent())
    .collect(Collectors.joining("\n\n"));

// 4. LLM生成回答
String prompt = """
基于以下专业知识回答用户问题：
{context}

用户问题：{question}

要求：
1. 只基于提供的知识回答
2. 如果知识中没有相关内容，明确说明
3. 引用知识来源
""";
```

##### 知识库的可扩展性
**YAML格式**便于非技术人员编辑：
```yaml
document_id: training-principles-001
title: "渐进超负荷原理"
category: training
date: 2026-03-01
content: |
  渐进超负荷（Progressive Overload）是力量训练最核心的原则。
  
  含义：要让肌肉持续生长，必须逐步增加训练刺激。
  
  实现方式：
  1. 增加重量（最直接）
  2. 增加组数或次数
  3. 缩短组间休息时间
  4. 改变动作节奏（增加离心时间）
  
  注意：不是每次训练都要增加，建议每2-4周评估一次进步。
citations:
  - "Schoenfeld, B. J. (2010). The mechanisms of muscle hypertrophy"
  - "Kraemer & Ratamess (2004). Fundamentals of resistance training"
```

**用户也可以添加自己的知识**：
- 健身房的器械使用说明
- 教练给的个性化建议
- 自己的训练心得

#### 效果对比

**不使用RAG**：
> 用户：增肌一天要吃多少蛋白质？
> AI：建议每公斤体重2-3克。（过高，可能是幻觉）

**使用RAG**：
> 用户：增肌一天要吃多少蛋白质？
> AI：根据《营养基础》文档，增肌期蛋白质摄入建议为每公斤体重1.6-2.2克。
> 更多并不总是更好，超过2.2g/kg并不会带来额外的肌肉增长。
> 
> 知识来源：nutrition-basics-2026.03.yml

### 🔄 本地写入恢复机制（Local Write Recovery）

#### 问题场景
想象这个场景：
1. 用户跟AI说"给我生成一个增肌计划"
2. AI花了30秒，生成了一个完整的4天训练计划
3. 用户查看后很满意，点击"保存到本地"
4. **网络突然断了**，保存失败
5. 用户返回，AI生成的计划消失了

**这是灾难性的用户体验**：
- 用户浪费了时间（AI生成需要时间）
- 用户失去了内容（可能再也生成不出一模一样的）
- 用户不信任系统（害怕再次丢失）

#### 传统方案的问题

**方案1：重新生成**
- 缺点：浪费时间，且可能生成不同的内容

**方案2：本地暂存**
- 缺点：用户换设备或重装APP，暂存的内容就没了

**方案3：强制网络检查**
- 缺点：没网就不能用AI，体验差

#### 我们的Local Write Recovery方案

##### 核心思想：
1. **AI生成的内容立即可见可编辑**（不立即保存）
2. **用户确认后，才尝试保存到数据库**
3. **保存失败时，自动记录到本地草案**
4. **重新联网时，提醒用户恢复**

##### 技术流程

```
第1步：AI生成内容
┌─────────────┐
│ 用户请求    │
└──────┬──────┘
       ↓
┌─────────────┐
│ AI生成计划  │ (在服务器端完成)
└──────┬──────┘
       ↓
┌─────────────┐
│ 返回给前端  │ (JSON格式，还不是数据库记录)
└──────┬──────┘
       ↓
┌─────────────┐
│ 前端展示预览│ (用户可编辑)
└─────────────┘

第2步：用户确认保存
┌─────────────┐
│ 用户点击确认│
└──────┬──────┘
       ↓
┌─────────────┐
│ 调用保存API │ POST /api/agent/training/save-draft
└──────┬──────┘
       ↓
    成功？
    /    \
   是      否（网络异常）
   ↓       ↓
  保存到   自动保存LocalWriteReference到本地数据库
  服务器   {
           agent_action_id: "xxx",
           data_type: "training_plan",
           local_data: {计划的完整JSON},
           retry_count: 0
           }

第3步：恢复机制
用户下次打开APP或重新联网
       ↓
  检查本地是否有LocalWriteReference
       ↓
    有待恢复数据？
       ↓
  显示恢复提示："您有1个训练计划待保存"
       ↓
  用户点击"重试"
       ↓
  再次调用保存API
       ↓
    成功？
     ↓
  删除LocalWriteReference
```

##### 技术细节

**1. 幂等性保证**
问题：用户点了2次"重试"，会不会保存2个一样的计划？

解决：使用`agent_action_id`作为幂等键
```java
@Transactional
public void saveDraft(TrainingPlanDraft draft) {
    // 检查是否已经保存过
    TrainingPlan existing = planDao.findByAgentActionId(
        draft.getAgentActionId()
    );
    
    if (existing != null) {
        // 已经保存过，返回成功（幂等）
        return;
    }
    
    // 没保存过，执行保存
    planDao.insert(draft.toEntity());
}
```

**2. 数据一致性**
问题：如果保存了一半，数据库崩了怎么办？

解决：使用Spring事务 + agent_action_id关联
```java
@Transactional
public void saveTrainingPlan(TrainingPlanDraft draft) {
    // 1. 保存主记录
    TrainingPlan plan = planDao.insert(draft.getPlan());
    
    // 2. 保存关联的动作
    for (Exercise ex : draft.getExercises()) {
        ex.setPlanId(plan.getId());
        ex.setAgentActionId(draft.getAgentActionId());
        exerciseDao.insert(ex);
    }
    
    // 3. 更新AgentAction状态
    agentActionDao.updateStatus(
        draft.getAgentActionId(), 
        "COMPLETED"
    );
    
    // 事务提交：要么全成功，要么全失败
}
```

**3. 用户体验优化**

**不打扰用户**：
- 保存成功 → 静默完成，不弹窗
- 保存失败 → 自动保存草案，小提示"已保存到本地草案"
- 重新联网 → 在合适时机（打开AI助手页面）提示恢复

**给用户选择权**：
```kotlin
// 恢复提示UI
AlertDialog(
    title = "发现待恢复的训练计划",
    message = "您有1个训练计划因网络问题未保存成功，是否重试？",
    positiveButton = "重试保存",
    negativeButton = "暂不保存",
    neutralButton = "删除草案"
)
```

##### 实际案例

**场景1：地铁里用AI生成计划**
- 用户在地铁里（断网），生成了训练计划
- 点击保存 → 失败，自动保存到本地草案
- 到家连上WiFi，打开APP → 提示恢复
- 点击重试 → 保存成功

**场景2：服务器临时故障**
- 用户生成了饮食建议
- 点击保存 → 服务器返回500错误
- 自动保存到本地草案
- 5分钟后服务器恢复，用户再次打开
- 提示恢复 → 保存成功

**场景3：用户改主意了**
- 用户生成了计划但没立即保存
- 关闭APP
- 第二天打开，发现有草案
- 点击"删除草案"→ 清理掉

#### 技术优势总结

✅ **永不丢失数据**：AI生成的内容总能找回
✅ **离线友好**：网络不好也能用
✅ **幂等性保证**：重试不会重复保存
✅ **用户可控**：可选择恢复、删除或稍后处理

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
