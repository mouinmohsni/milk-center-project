package org.milkcenter.invoicingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(
        basePackages = "org.milkcenter.invoicingservice.client"
)
@EnableScheduling
public class InvoicingServiceApplication {


    public static void main(String[] args) {
        SpringApplication.run(InvoicingServiceApplication.class, args);
    }
}




