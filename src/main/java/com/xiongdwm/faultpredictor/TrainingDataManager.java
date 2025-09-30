package com.xiongdwm.faultpredictor;

import com.fasterxml.jackson.databind.ObjectMapper;
import weka.core.Instance;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class TrainingDataManager {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static void saveTrainingData(List<SecureFeatureData> data, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(gzos)) {
            
            oos.writeInt(data.size());
            for (SecureFeatureData item : data) {
                String json = objectMapper.writeValueAsString(item);
                oos.writeUTF(json);
            }
        }
    }
    
    public static List<SecureFeatureData> loadTrainingData(String filePath) throws IOException {
        List<SecureFeatureData> data = new ArrayList<>();
        
        if (!Files.exists(Paths.get(filePath))) {
            return data; 
        }
        
        try (FileInputStream fis = new FileInputStream(filePath);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             ObjectInputStream ois = new ObjectInputStream(gzis)) {
            
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                String json = ois.readUTF();
                SecureFeatureData item = objectMapper.readValue(json, SecureFeatureData.class);
                data.add(item);
            }
        }
        
        return data;
    }
    
    // 从安全特征数据创建Weka实例
    public static Instance createInstance(SecureFeatureData secureData, FaultTypePredictor predictor) {
        double[] values = new double[secureData.getFeatures().size() + 1]; // +1 for class attribute
        for (int i = 0; i < secureData.getFeatures().size(); i++) {
            values[i] = secureData.getFeatures().get(i);
        }
        
        Instance instance = new weka.core.DenseInstance(1.0, values);
        instance.setDataset(predictor.getHeader());
        
        // 如果有标签，则设置类别值
        if (secureData.getFaultType() != null && !secureData.getFaultType().isEmpty()) {
            instance.setClassValue(secureData.getFaultType());
        }
        
        return instance;
    }
}