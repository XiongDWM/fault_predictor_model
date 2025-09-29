package com.xiongdwm.faultpredictor;

import weka.classifiers.trees.HoeffdingTree;
import weka.core.*;
import weka.core.SerializationHelper;

import java.util.*;

public class FaultTypePredictor {
    private HoeffdingTree model;
    private Instances header;
    
    // 故障类型列表
    private static final List<String> FAULT_TYPES = Arrays.asList(
        "压坏", "动物啃食", "电腐蚀", "外力破坏", "劣化", "运行缺陷"
    );

    public FaultTypePredictor() {
        initializeModel();
    }

    private void initializeModel() {
        // 定义属性 (11个特征 + 1个类别)
        ArrayList<Attribute> attrs = new ArrayList<>();
        
        // 空间特征 (3)
        attrs.add(new Attribute("grid_id"));
        attrs.add(new Attribute("longitude_norm"));
        attrs.add(new Attribute("latitude_norm"));
        
        // 时间特征 (4)
        attrs.add(new Attribute("month"));
        attrs.add(new Attribute("hour"));
        attrs.add(new Attribute("season"));
        attrs.add(new Attribute("is_weekend"));
        
        // 光缆属性 (4)
        attrs.add(new Attribute("cable_level"));
        attrs.add(new Attribute("length_norm"));
        attrs.add(new Attribute("service_type"));
        attrs.add(new Attribute("owner"));
        attrs.add(new Attribute("loss_norm"));
        
        // 类别标签
        ArrayList<String> classes = new ArrayList<>(FAULT_TYPES);
        attrs.add(new Attribute("fault_type", classes));
        
        header = new Instances("fault_data", attrs, 0);
        header.setClassIndex(header.numAttributes() - 1);
        
        model = new HoeffdingTree();
        try {
            model.buildClassifier(header);
        } catch (Exception e) {
            throw new RuntimeException("模型初始化失败", e);
        }
    }

    // 增量学习
    public void learn(Instance instance) {
        try {
            model.updateClassifier(instance);
        } catch (Exception e) {
            System.err.println("学习失败: " + e.getMessage());
        }
    }

    // 预测概率
    public List<Map<String, Object>> predict(Instance instance) {
        try {
            double[] probs = model.distributionForInstance(instance);
            List<Map<String, Object>> results = new ArrayList<>();
            for (int i = 0; i < probs.length; i++) {
                String type = header.classAttribute().value(i);
                results.add(Map.of("type", type, "probability", probs[i]));
            }
            results.sort((a, b) -> 
                Double.compare((Double)b.get("probability"), (Double)a.get("probability")));
            return results;
        } catch (Exception e) {
            System.err.println("预测失败: " + e.getMessage());
            return List.of();
        }
    }

    // 保存模型
    public void saveModel(String path) {
        try {
            SerializationHelper.write(path, model);
        } catch (Exception e) {
            throw new RuntimeException("模型保存失败", e);
        }
    }

    // 加载模型
    public void loadModel(String path) {
        try {
            model = (HoeffdingTree) SerializationHelper.read(path);
        } catch (Exception e) {
            throw new RuntimeException("模型加载失败", e);
        }
    }

    public Instances getHeader() {
        return header;
    }
}