package com.xiongdwm.faultpredictor;

import weka.core.Instance;

import java.util.List;
import java.util.Map;

public class SecureFeatureData {
    private List<Double> features;
    private String faultType; // 用于训练时的标签
    private String timestamp;
    
    public SecureFeatureData() {}
    
    public SecureFeatureData(List<Double> features) {
        this.features = features;
    }
    
    public SecureFeatureData(List<Double> features, String faultType) {
        this.features = features;
        this.faultType = faultType;
    }
    
    // Getters and setters
    public List<Double> getFeatures() { return features; }
    public void setFeatures(List<Double> features) { this.features = features; }
    
    public String getFaultType() { return faultType; }
    public void setFaultType(String faultType) { this.faultType = faultType; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    // 转换为Weka Instance
    public Instance toInstance(FaultTypePredictor predictor) {
        double[] values = new double[features.size() + 1]; // +1 for class attribute
        for (int i = 0; i < features.size(); i++) {
            values[i] = features.get(i);
        }
        
        Instance instance = new weka.core.DenseInstance(1.0, values);
        instance.setDataset(predictor.getHeader());
        
        // 如果有标签，则设置类别值
        if (faultType != null && !faultType.isEmpty()) {
            instance.setClassValue(faultType);
        }
        
        return instance;
    }
}