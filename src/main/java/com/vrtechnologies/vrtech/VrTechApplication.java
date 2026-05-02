package com.vrtechnologies.vrtech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.admin.SpringApplicationAdminJmxAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = SpringApplicationAdminJmxAutoConfiguration.class)
public class VrTechApplication {

    public static void main(String[] args) {
        SpringApplication.run(VrTechApplication.class, args);
    }
}
