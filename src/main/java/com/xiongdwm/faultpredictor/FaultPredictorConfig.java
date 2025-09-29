package com.xiongdwm.faultpredictor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FaultPredictorConfig {
    
    @Bean
    public FaultTypePredictor faultTypePredictor() {
        return new FaultTypePredictor();
    }
}