package com.chinamobile.cmss.dts.dtsresourcessold.service.capacity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

@Slf4j
@Configuration
public class KafkaConsumerServiceImpl implements KafkaConsumerService{
    @Override
    @KafkaListener(topics = "outer-dts")
    public void consumerTopic(String msg) {
        log.info("topic: outer-dts, " + msg);
    }
}
