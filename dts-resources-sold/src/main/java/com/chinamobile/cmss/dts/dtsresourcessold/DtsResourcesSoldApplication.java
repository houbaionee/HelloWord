package com.chinamobile.cmss.dts.dtsresourcessold;

import com.chinamobile.cmss.dts.dtsresourcessold.service.capacity.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DtsResourcesSoldApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DtsResourcesSoldApplication.class, args);

    }

}
