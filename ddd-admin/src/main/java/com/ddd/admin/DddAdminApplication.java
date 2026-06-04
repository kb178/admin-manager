package com.ddd.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DddAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(DddAdminApplication.class, args);
        System.out.println("================================================");
        System.out.println(">>> DDD Admin Startup SUCCESS <<<");
        System.out.println("API : http://localhost:8080/api");
        System.out.println("DB  : MySQL 8.0 - ddd");
        System.out.println("================================================");
    }
}
