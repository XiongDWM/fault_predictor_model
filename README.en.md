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

## Technical Architecture

### 1. Online Learning & Multi-Model Ensemble

This project adopts a **hybrid multi-model fusion architecture** that combines the strengths of multiple machine learning models to improve fault prediction accuracy and robustness:

#### Model Components
- **HoeffdingTree**: Supports online/streaming incremental learning, ideal for real-time data streams
- **DL4J Neural Network**: Supports mini-batch incremental training, capturing complex non-linear patterns
- **RandomForest**: Offline batch training model, providing ensemble stability

#### Voting & Fusion Mechanism

Predictions from multiple models are fused using **weighted voting** or **probability-weighted averaging**:

```
Fusion Pipeline:
1. Each model independently predicts, outputting a probability vector for each fault type
   - HoeffdingTree: [P_Type_A, P_Type_B, P_Type_C, ...]
   - DL4J NN:     [P_Type_A, P_Type_B, P_Type_C, ...]
   - RandomForest: [P_Type_A, P_Type_B, P_Type_C, ...]

2. Weighted Fusion Strategy (choose one):
   
   a) Simple Average (equal weights)
      Final Probability = (P_HoeffD + P_DL4J + P_RF) / 3
      
   b) Weighted Average (based on validation set accuracy)
      Final Probability = w1*P_HoeffD + w2*P_DL4J + w3*P_RF
      where w1 + w2 + w3 = 1, weights determined by model accuracy on validation set
      
   c) Hard Voting (majority voting)
      Each model votes for the class with highest probability; final class is the most voted
      (odd number of models recommended to avoid ties)

3. Output final prediction
   Take the fault type corresponding to the maximum probability in fused vector
```

#### Online Feedback & Model Update

When users correct system predictions, feedback is sent back to models for incremental learning:

```
Feedback Pipeline:
1. System Prediction: Feature vector → Multi-model voting → Predicted fault type
2. User Correction: User provides ground-truth fault type
3. Incremental Learning:
   - HoeffdingTree: Call updateClassifier(instance) for online update
   - DL4J:         Accumulate mini-batches and call fit(mini-batch) for incremental training
   - RandomForest: Periodically retrain the entire model offline
```

#### Data Format & Model Serialization

To ensure reproducibility and version management, **TrainingPackage** format is introduced:

```json
{
  "packageId": "pkg-20250120-001",
  "domain": "optical_cable",
  "featureSpecVersion": "v1.0",
  "meta": {
    "modelVersion": "v1.0",
    "labels": ["Fault_A", "Fault_B", "Fault_C"],
    "featureNames": ["attenuation", "month", "hour", "is_weekend", "grid_id"],
    "normalizers": {
      "attenuation": {"type": "minmax", "min": 0.0, "max": 100.0},
      "month": {"type": "category", "values": [1, 2, ..., 12]}
    },
    "createdAt": "2025-01-20T10:30:00Z",
    "notes": "Initial training set"
  },
  "records": [
    {
      "id": "rec-001",
      "timestamp": "2025-01-20T10:30:00Z",
      "features": {"attenuation": 25.5, "month": 1, "hour": 10, "is_weekend": false, "grid_id": 123},
      "label": "Fault_A"
    },
    ...
  ]
}
```

**Advantages**:
- Metadata (labels, featureNames, normalizers) saved with data package, preventing version mismatch
- Supports cross-domain and cross-version model migration
- Facilitates model serialization and recovery

#### Confidence & Dynamic Weight Learning

The system adopts an **adaptive confidence-weighted mechanism** that starts with equal or validation-set-based weights, then dynamically adjusts model weights based on user feedback.

##### 1. Initial Confidence Calculation

At prediction time, output confidence score based on fused probability:

```
Confidence Definition:
Confidence = max(fused probability vector) - second_max(probability)

Example:
- Fused probability vector: [0.7, 0.2, 0.1]
- Confidence = 0.7 - 0.2 = 0.5  ← High confidence
- Fused probability vector: [0.35, 0.33, 0.32]
- Confidence = 0.35 - 0.33 = 0.02  ← Low confidence, near tie
```

##### 2. User Feedback & Dynamic Weight Update

After manual review, business side sends feedback back to model platform. System dynamically adjusts each model's weight in subsequent predictions based on **how well each model performed on this sample**:

```
Weight Update Rules (for model i):

Scenario 1: Model predicted correctly (model_i's highest probability matches ground truth)
  weight_i += model's probability for correct class
  
Example:
  - Ground truth: Fault_A
  - Model_HoeffD probabilities: [0.6, 0.3, 0.1] (highest is Fault_A ✓)
  - Update: weight_HoeffD += 0.6

---

Scenario 2: Model predicted incorrectly (model_i's highest probability does not match ground truth)
  weight_i += (1 - model's highest probability)
  
Example:
  - Ground truth: Fault_A
  - Model_DL4J probabilities: [0.2, 0.6, 0.2] (highest is Fault_B ✗)
  - Model's highest probability = 0.6
  - Update: weight_DL4J += (1 - 0.6) = 0.4

---

Scenario 3: Mixed results (some correct, some incorrect)
  Correct models: weight += probability for correct class
  Incorrect models: weight += (1 - model's highest probability)
  
Example:
  - Ground truth: Fault_A
  - HoeffD [0.7, 0.2, 0.1] → Correct → weight += 0.7
  - DL4J  [0.2, 0.5, 0.3] → Incorrect (highest is B) → weight += (1 - 0.5) = 0.5
  - RF    [0.65, 0.25, 0.1] → Correct → weight += 0.65
```

##### 3. Weighted Fusion

In subsequent predictions, use updated weights for weighted averaging:

```
Weighted Fused Probability = Σ(weight_i * model_i_probability_vector) / Σ(weight_i)

Example (after 3 rounds of feedback):
- weight_HoeffD = 2.5   (accumulated from initial 1.0)
- weight_DL4J = 1.8
- weight_RF = 2.2

New sample:
- HoeffD: [0.75, 0.15, 0.1]
- DL4J:  [0.3, 0.5, 0.2]
- RF:    [0.7, 0.2, 0.1]

Weighted Fusion = (2.5*[0.75, 0.15, 0.1] + 1.8*[0.3, 0.5, 0.2] + 2.2*[0.7, 0.2, 0.1]) / (2.5 + 1.8 + 2.2)
                = [2.385, 1.26, 0.55] / 6.5
                ≈ [0.367, 0.194, 0.085]
```

##### 4. Business Flow & Feedback Loop

```
Complete Pipeline:

1. Prediction Phase:
   Input feature vector → Multi-model independent prediction → Weighted fusion → Output final prediction + confidence

2. Business Review Phase:
   System prediction: Fault_A (confidence 0.45)
   Business manual review/correction → Ground truth fault type (approve or correct)

3. Feedback Phase:
   Business returns: {
     "sampleId": "sample-001",
     "predictedType": "Fault_A",
     "groundTruth": "Fault_A" or "Fault_B",
     "feedback": "correct" or "incorrect"
   }

4. Weight Update Phase:
   System receives feedback
   → Retrieve model predictions for this sample
   → Apply weight update rules above
   → Persist updated weights (for subsequent predictions)

5. Continuous Improvement:
   As more feedback samples accumulate, weights adapt
   → High-performing models gain higher weights
   → Poor-performing models get lower weights
   → Overall prediction accuracy continuously improves
```

##### 5. Data Structure & Persistence

```java
// Dynamic weight configuration storage
public class ModelWeightConfig {
    private String configVersion;           // Version for rollback
    private Map<String, Double> modelWeights;  // Model name → current weight
    private int feedbackCount;              // Number of processed feedback samples
    private long lastUpdateTime;            // Last update timestamp
    private List<FeedbackRecord> feedbackHistory;  // Feedback history (optional)
    
    // getters & setters...
}

// Single feedback record
public class FeedbackRecord {
    private String sampleId;
    private String predictedType;
    private String groundTruth;
    private Map<String, Map<String, Double>> modelProbabilities;  // Probabilities from all models
    private long feedbackTime;
    private boolean correct;  // Whether prediction was correct
    
    // getters & setters...
}
```

##### 6. Confidence Threshold & Decision Logic

```java
// Pseudocode example
PredictionResult result = predictor.predict(features);

if (result.confidence > 0.3) {
    // High confidence: Auto-handle
    handleFault(result.faultType);
} else if (result.confidence > 0.1) {
    // Medium confidence: Manual review
    notifyOperator(result);
} else {
    // Low confidence: Need more data or model retraining
    requestMoreData();
}

// Feedback callback
void onUserFeedback(String sampleId, String groundTruth) {
    // Update weight config
    updateModelWeights(sampleId, groundTruth);
    // If weight change exceeds threshold, trigger model retraining
    if (hasSignificantWeightChange()) {
        retrainModels();
    }
}
```

**Return Format**:

```java
public class PredictionReturn {
    private String predictedFaultType;      // Predicted fault type
    private double confidence;              // Confidence score [0.0, 1.0]
    private Map<String, Double> probabilities;  // Weighted fused probabilities for all types
    private Map<String, Map<String, Double>> modelDetails;  // Independent probabilities from each model (for debugging)
    private Map<String, Double> currentWeights;  // Current weights of each model
    private long predictionTime;            // Prediction elapsed time (milliseconds)
    private String modelVersion;            // Model version used
    
    // getters & setters...
}
```

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