package com.xiongdwm.faultpredictor.model;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.dataset.DataSet;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class FaultTypePredictorNeuro {
    private MultiLayerNetwork model;
    private int inputSize;
    private int outputSize;

    public FaultTypePredictorNeuro(int inputSize, int outputSize) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .list()
            .layer(new DenseLayer.Builder().nIn(inputSize).nOut(32)
                .activation(Activation.RELU).build())
            .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .activation(Activation.SOFTMAX)
                .nIn(32).nOut(outputSize).build())
            .build();
        model = new MultiLayerNetwork(conf);
        model.init();
    }

    // 增量训练
    public void fit(double[][] features, double[][] labels) {
        INDArray featureArr = Nd4j.create(features);
        INDArray labelArr = Nd4j.create(labels);
        DataSet ds = new DataSet(featureArr, labelArr);
        model.fit(ds);
    }

    // 预测概率
    public double[] predict(double[] features) {
        INDArray input = Nd4j.create(features);
        INDArray output = model.output(input);
        return output.toDoubleVector();
    }

    // 保存模型
    public void save(String path) throws Exception {
        model.save(new java.io.File(path));
    }

    // 加载模型
    public void load(String path) throws Exception {
        model = MultiLayerNetwork.load(new java.io.File(path), true);
    }

    public static void main(String[] args) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var dateString="2025-10-20 14:30:00";
        var dateTime = java.time.LocalDateTime.parse(dateString, formatter);
        System.out.println(dateTime.getDayOfWeek().getValue());
    }
}