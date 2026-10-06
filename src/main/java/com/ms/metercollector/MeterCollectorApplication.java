package com.ms.metercollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MeterCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeterCollectorApplication.class, args);
    }

}
