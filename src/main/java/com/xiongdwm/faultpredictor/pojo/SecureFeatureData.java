package com.xiongdwm.faultpredictor.pojo;

import weka.core.Instance;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.xiongdwm.faultpredictor.model.FaultTypePredictorHoeffding;

public class SecureFeatureData implements Serializable {
    private List<Double> features;
    private String faultType; // 用于训练时的标签
    private String timestamp;
    private String dateFormat;
    
    public SecureFeatureData() {}
    
    public SecureFeatureData(List<Double> features) {
        this.features = features;
    }
    
    public SecureFeatureData(List<Double> features, String faultType) {
        this.features = features;
        this.faultType = faultType;
    }
    
    public List<Double> getFeatures() { return features; }
    public void setFeatures(List<Double> features) { this.features = features; }
    
    public String getFaultType() { return faultType; }
    public void setFaultType(String faultType) { this.faultType = faultType; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public Instance toInstance(FaultTypePredictorHoeffding predictor) {
        double[] values = new double[features.size()+4+1]; // +1 for class attribute
        for (int i = 0; i < features.size(); i++) {
            values[i] = features.get(i);
        }
        
        Instance instance = new weka.core.DenseInstance(1.0, values);
        instance.setDataset(predictor.getHeader());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        var dateTime = java.time.LocalDateTime.parse(timestamp, formatter);

        values[features.size()] = dateTime.getMonthValue(); // month
        values[features.size()+1] = dateTime.getHour(); // hour
        values[features.size()+2] = (dateTime.getMonthValue() - 1) / 3 + 1; // season
        values[features.size()+3] = (dateTime.getDayOfWeek().getValue() >= 6) ? 1.0 : 0.0; // is_weekend



        // 如果有标签，则设置类别值
        if (faultType != null && !faultType.isEmpty()) {
            instance.setClassValue(faultType);
        }
        
        return instance;
    }
}