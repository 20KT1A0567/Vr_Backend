package com.vrtechnologies.vrtech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.admin.SpringApplicationAdminJmxAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = SpringApplicationAdminJmxAutoConfiguration.class)
public class VrTechApplication {

    public static void main(String[] args) {
        SpringApplication.run(VrTechApplication.class, args);
    }
}
