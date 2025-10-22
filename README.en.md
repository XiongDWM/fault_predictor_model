# Fault Predictor Model
[English](README.en.md) | [中文](README.md)

## Project Introduction


This project is a fault predictor model based on Java 21, designed to predict fault types of systems or devices using machine learning methods. It is built with Gradle and includes modules for secure feature data management and training data management, suitable for fault warning and analysis in industrial scenarios.

---

## Main Features

- Fault type prediction (`FaultTypePredictor`)
- Training data management (`TrainingDataManager`)
- Secure feature data processing (`SecureFeatureData`)
- Extensible configuration (`application.properties`)
- Supports custom models and datasets

---

## Environment Requirements

- JDK 21
- Gradle 8.x or above
- Recommended on macOS or Linux

---

## Quick Start

```bash
# Clone the project
git clone https://github.com/XiongDWM/fault_predictor_model.git
cd fault_predictor_model

# Build the project
./gradlew build

# Run the main application
./gradlew run
```

Configuration can be modified in `src/main/resources/application.properties`.

---

## Project Structure

```
src/main/java/com/xiongdwm/faultpredictor/   # Core Java source code
src/main/resources/                          # Configuration files and resources
models/                                      # Model files
data/                                        # Dataset
build.gradle                                 # Gradle build script
```

---

## Contributing

Feel free to submit issues and pull requests to improve features or fix bugs.

---

## License
 
This project is licensed under the MIT License.