package com.xiongdwm.faultpredictor.model;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FaultPredictorConfig {
    
    @Bean
    public FaultTypePredictorHoeffding faultTypePredictor() {
        return new FaultTypePredictorHoeffding();
    }
}