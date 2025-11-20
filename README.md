# 故障预测模型
[English](README.en.md) | [中文](README.md)

## 项目简介 / Project Introduction

本项目是一个基于 Java 21 的故障预测模型，旨在通过机器学习方法对系统或设备的故障类型进行预测。项目采用 Gradle 构建，包含安全特征数据管理和训练数据管理模块，适用于工业场景的故障推断与分析。


---

## 主要功能 / Main Features

- 故障类型推断（FaultTypePredictor）
- 训练数据管理（TrainingDataManager）
- 安全特征数据处理（SecureFeatureData）
- 可扩展的配置（application.properties）
- 支持自定义模型与数据集

---

## 技术架构

### 1. 在线学习与多模型集成

本项目采用 **混合模型融合架构**，结合多个机器学习模型的优势，提高故障预测的准确性和鲁棒性：

#### 模型组成
- **HoeffdingTree（霍夫丁树）**: 支持在线/流式增量学习，适合实时数据流处理
- **DL4J 神经网络**: 支持小批次（mini-batch）增量训练，捕捉复杂非线性特征
- **随机森林（RandomForest）**: 离线批量训练模型，提供集成学习的稳定性

#### 投票机制（Voting & Fusion）

多模型的预测结果通过**加权投票**或**概率加权平均**进行融合：

```
融合流程：
1. 每个模型独立预测，输出各故障类型的概率向量
   - HoeffdingTree: [P_类型A, P_类型B, P_类型C, ...]
   - DL4J NN:     [P_类型A, P_类型B, P_类型C, ...]
   - RandomForest: [P_类型A, P_类型B, P_类型C, ...]

2. 加权融合策略（三选一）：
   
   a) 简单平均（等权重）
      最终概率 = (P_HoeffD + P_DL4J + P_RF) / 3
      
   b) 加权平均（基于验证集精度）
      最终概率 = w1*P_HoeffD + w2*P_DL4J + w3*P_RF
      其中 w1 + w2 + w3 = 1，权重由各模型在验证集上的精度决定
      
   c) 多数投票（hard voting）
      每个模型投出最高概率的类别标签，最终取票数最多的类别
      （推荐奇数个模型以避免平票）

3. 输出最终预测
   取融合概率向量中最大值对应的故障类型
```

#### 在线反馈与模型更新

当用户纠正系统预测时，将纠正后的数据反馈给模型进行增量学习：

```
反馈流程：
1. 系统预测: 输入特征向量 → 多模型投票 → 预测故障类型
2. 用户纠正: 用户提供真实故障类型
3. 增量学习:
   - HoeffdingTree: 直接调用 updateClassifier(instance) 进行在线更新
   - DL4J:         收集小批次数据后调用 fit(mini-batch) 增量训练
   - RandomForest: 定期离线重训练整个模型
```

#### 数据格式与模型序列化

为了确保模型的可复现性与版本管理，引入 **TrainingPackage** 格式：

```json
{
  "packageId": "pkg-20250120-001",
  "domain": "optical_cable",
  "featureSpecVersion": "v1.0",
  "meta": {
    "modelVersion": "v1.0",
    "labels": ["故障A", "故障B", "故障C"],
    "featureNames": ["衰减度", "月份", "小时", "是否周末", "栅格ID"],
    "normalizers": {
      "衰减度": {"type": "minmax", "min": 0.0, "max": 100.0},
      "月份": {"type": "category", "values": [1, 2, ..., 12]}
    },
    "createdAt": "2025-01-20T10:30:00Z",
    "notes": "初始训练集"
  },
  "records": [
    {
      "id": "rec-001",
      "timestamp": "2025-01-20T10:30:00Z",
      "features": {"衰减度": 25.5, "月份": 1, "小时": 10, "是否周末": false, "栅格ID": 123},
      "label": "故障A"
    },
    ...
  ]
}
```

**优势**：
- 元数据（labels、featureNames、normalizers）随数据包保存，避免版本不匹配
- 支持跨域、跨版本的模型迁移
- 便于模型序列化与恢复

#### 置信度与动态权重学习（Confidence & Dynamic Weight Learning）

系统采用 **置信度自适应权重机制**，在初期使用等权重或基于验证集的固定权重，然后根据用户反馈动态调整各模型的置信度权重。

##### 1. 初始置信度计算

在预测时，输出基于当前融合概率的置信度得分：

```
置信度定义：
置信度 = max(融合概率向量) - 次大概率

示例：
- 融合概率向量: [0.7, 0.2, 0.1]
- 置信度 = 0.7 - 0.2 = 0.5  ← 高置信度
- 融合概率向量: [0.35, 0.33, 0.32]
- 置信度 = 0.35 - 0.33 = 0.02  ← 低置信度，接近平手
```

##### 2. 用户反馈与动态权重更新

业务侧人工审核后，将反馈结果发送回模型平台。系统根据 **每个模型在该样本上的表现** 动态调整其在后续预测中的权重：

```
权重更新规则（以模型 i 为例）：

情景 1：该模型预测正确（模型_i 的最高概率对应真实类型）
  权重_i += 该模型在正确类型上的概率
  
示例：
  - 真实故障类型: 故障A
  - 模型_HoeffD 的概率向量: [0.6, 0.3, 0.1]（最高是故障A）
  - 更新：权重_HoeffD += 0.6

---

情景 2：该模型预测错误（模型_i 的最高概率不对应真实类型）
  权重_i += (1 - 该模型的最高概率)
  
示例：
  - 真实故障类型: 故障A
  - 模型_DL4J 的概率向量: [0.2, 0.6, 0.2]（最高是故障B，错误）
  - 该模型最高概率 = 0.6
  - 更新：权重_DL4J += (1 - 0.6) = 0.4

---

情景 3：多模型混合（某些对某些错）
  对的模型：权重 += 在正确类型上的概率
  错的模型：权重 += (1 - 该模型的最高概率)
  
示例：
  - 真实故障类型: 故障A
  - HoeffD [0.7, 0.2, 0.1] → 正确 → 权重 += 0.7
  - DL4J  [0.2, 0.5, 0.3] → 错误（最高是B） → 权重 += (1 - 0.5) = 0.5
  - RF    [0.65, 0.25, 0.1] → 正确 → 权重 += 0.65
```

##### 3. 加权融合

在后续预测中，使用更新后的权重进行加权平均：

```
加权融合概率 = Σ(权重_i * 模型_i的概率向量) / Σ(权重_i)

示例（3 轮反馈后）：
- 权重_HoeffD = 2.5   (从初始 1.0 累积更新)
- 权重_DL4J = 1.8
- 权重_RF = 2.2

新样本：
- HoeffD: [0.75, 0.15, 0.1]
- DL4J:  [0.3, 0.5, 0.2]
- RF:    [0.7, 0.2, 0.1]

加权融合 = (2.5*[0.75, 0.15, 0.1] + 1.8*[0.3, 0.5, 0.2] + 2.2*[0.7, 0.2, 0.1]) / (2.5 + 1.8 + 2.2)
         = [2.385, 1.26, 0.55] / 6.5
         ≈ [0.367, 0.194, 0.085]
```

##### 4. 业务流程与反馈循环

```
完整流程：

1. 预测阶段：
   输入特征向量 → 多模型独立预测 → 加权融合 → 输出最终预测 + 置信度

2. 业务审核阶段：
   系统预测: 故障A (置信度 0.45)
   业务侧人工确认/纠正 → 真实故障类型（可能同意或纠正）

3. 反馈阶段：
   业务侧返回: {
     "sampleId": "sample-001",
     "predictedType": "故障A",
     "groundTruth": "故障A" or "故障B",
     "feedback": "correct" or "incorrect"
   }

4. 权重更新阶段：
   系统接收反馈
   → 查询该样本各模型的预测概率
   → 按上述规则更新权重
   → 持久化权重配置（供后续预测使用）

5. 持续改进：
   随着反馈样本增多，权重自适应调整
   → 表现好的模型权重升高
   → 表现差的模型权重降低
   → 整体预测准确性持续提升
```

##### 5. 数据结构与持久化

```java
// 动态权重存储结构
public class ModelWeightConfig {
    private String configVersion;           // 版本号，便于回滚
    private Map<String, Double> modelWeights;  // 模型名 → 当前权重
    private int feedbackCount;              // 已处理的反馈样本数
    private long lastUpdateTime;            // 最后更新时间
    private List<FeedbackRecord> feedbackHistory;  // 反馈历史（可选）
    
    // getters & setters...
}

// 单次反馈记录
public class FeedbackRecord {
    private String sampleId;
    private String predictedType;
    private String groundTruth;
    private Map<String, Map<String, Double>> modelProbabilities;  // 保存各模型的概率
    private long feedbackTime;
    private boolean correct;  // 预测是否正确
    
    // getters & setters...
}
```

##### 6. 置信度阈值与决策

```java
// 伪代码示例
PredictionResult result = predictor.predict(features);

if (result.confidence > 0.3) {
    // 高置信度：自动处理
    handleFault(result.faultType);
} else if (result.confidence > 0.1) {
    // 中等置信度：人工审核
    notifyOperator(result);
} else {
    // 低置信度：需更多数据或模型训练
    requestMoreData();
}

// 反馈回调
void onUserFeedback(String sampleId, String groundTruth) {
    // 更新权重配置
    updateModelWeights(sampleId, groundTruth);
    // 如果权重变化超过阈值，可触发模型重训练
    if (hasSignificantWeightChange()) {
        retrainModels();
    }
}
```

**返回格式**：

```java
public class PredictionReturn {
    private String predictedFaultType;      // 预测的故障类型
    private double confidence;              // 置信度 [0.0, 1.0]
    private Map<String, Double> probabilities;  // 各类型的加权融合概率
    private Map<String, Map<String, Double>> modelDetails;  // 各模型的独立概率（用于调试）
    private Map<String, Double> currentWeights;  // 当前各模型的权重
    private long predictionTime;            // 预测耗时（毫秒）
    private String modelVersion;            // 使用的模型版本
    
    // getters & setters...
}
```

---

## 环境要求

- JDK 21
- Gradle 8.x 及以上版本
- 推荐使用 macOS 或 Linux 环境

---

## 快速开始 / Quick Start

```bash
# 克隆项目
git clone https://github.com/XiongDWM/fault_predictor_model.git
cd fault_predictor_model

# 构建项目
./gradlew build

# 运行主程序
./gradlew run
```

配置文件可在 `src/main/resources/application.properties` 中修改。  

---

## 目录结构

```
src/main/java/com/xiongdwm/faultpredictor/   # 核心 Java 代码 
src/main/resources/                          # 配置文件与资源
models/                                      # 模型文件 
data/                                        # 数据集 
build.gradle                                 # Gradle 构建脚本
```

---

## 贡献

欢迎提交 Issue 和 Pull Request，完善功能或修复 Bug。  

---

## 许可证

本项目采用 MIT 许可证。  