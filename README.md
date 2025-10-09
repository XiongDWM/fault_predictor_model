# 故障预测模型 / Fault Predictor Model

## 项目简介 / Project Introduction

本项目是一个基于 Java 21 的故障预测模型，旨在通过机器学习方法对系统或设备的故障类型进行预测。项目采用 Gradle 构建，包含安全特征数据管理和训练数据管理模块，适用于工业场景的故障预警与分析。

This project is a fault predictor model based on Java 21, designed to predict fault types of systems or devices using machine learning methods. It is built with Gradle and includes modules for secure feature data management and training data management, suitable for fault warning and analysis in industrial scenarios.

---

## 主要功能 / Main Features

- 故障类型预测（FaultTypePredictor）
- 训练数据管理（TrainingDataManager）
- 安全特征数据处理（SecureFeatureData）
- 可扩展的配置（application.properties）
- 支持自定义模型与数据集

- Fault type prediction (`FaultTypePredictor`)
- Training data management (`TrainingDataManager`)
- Secure feature data processing (`SecureFeatureData`)
- Extensible configuration (`application.properties`)
- Supports custom models and datasets

---

## 环境要求 / Environment Requirements

- JDK 21
- Gradle 8.x 及以上版本
- 推荐使用 macOS 或 Linux 环境

- JDK 21
- Gradle 8.x or above
- Recommended on macOS or Linux

---

## 快速开始 / Quick Start

```bash
# 克隆项目 / Clone the project
git clone https://github.com/XiongDWM/fault_predictor_model.git
cd fault_predictor_model

# 构建项目 / Build the project
./gradlew build

# 运行主程序 / Run the main application
./gradlew run
```

配置文件可在 `src/main/resources/application.properties` 中修改。  
Configuration can be modified in `src/main/resources/application.properties`.

---

## 目录结构 / Project Structure

```
src/main/java/com/xiongdwm/faultpredictor/   # 核心 Java 代码 / Core Java source code
src/main/resources/                          # 配置文件与资源 / Configuration files and resources
models/                                      # 模型文件 / Model files
data/                                        # 数据集 / Dataset
build.gradle                                 # Gradle 构建脚本 / Gradle build script
```

---

## 贡献 / Contributing

欢迎提交 Issue 和 Pull Request，完善功能或修复 Bug。  
Feel free to submit issues and pull requests to improve features or fix bugs.

---

## 许可证 / License

本项目采用 MIT 许可证。  
This project is licensed under the MIT License.