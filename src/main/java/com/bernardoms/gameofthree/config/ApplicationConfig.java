package com.bernardoms.gameofthree.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties
@Configuration
public class ApplicationConfig {

    @Value("rangeNumber")
    private String rangeNumber;

    public int getRangeNumber() {
        return Integer.parseInt(rangeNumber);
    }

    public void setRangeNumber(String rangeNumber) {
        this.rangeNumber = rangeNumber;
    }
}
