# 故障预测模型
[English](README.en.md) | [中文](README.md)

## 项目简介 / Project Introduction

本项目是一个基于 Java 21 的故障预测模型，旨在通过机器学习方法对系统或设备的故障类型进行预测。项目采用 Gradle 构建，包含安全特征数据管理和训练数据管理模块，适用于工业场景的故障预警与分析。


---

## 主要功能 / Main Features

- 故障类型预测（FaultTypePredictor）
- 训练数据管理（TrainingDataManager）
- 安全特征数据处理（SecureFeatureData）
- 可扩展的配置（application.properties）
- 支持自定义模型与数据集

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