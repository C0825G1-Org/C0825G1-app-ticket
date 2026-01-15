package com.codegym.appticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class AppTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppTicketApplication.class, args);
    }

}
