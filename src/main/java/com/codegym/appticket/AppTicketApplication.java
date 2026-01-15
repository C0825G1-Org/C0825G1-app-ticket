package com.codegym.appticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AppTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppTicketApplication.class, args);
    }

}
