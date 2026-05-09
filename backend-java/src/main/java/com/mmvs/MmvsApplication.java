package com.mmvs;

import com.mmvs.config.AppModeProperties;
import com.mmvs.config.InferenceProperties;
import com.mmvs.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({StorageProperties.class, InferenceProperties.class, AppModeProperties.class})
public class MmvsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MmvsApplication.class, args);
    }
}
