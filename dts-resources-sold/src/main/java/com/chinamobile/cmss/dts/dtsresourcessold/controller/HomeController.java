package com.chinamobile.cmss.dts.dtsresourcessold.controller;

import com.chinamobile.cmss.dts.dtsresourcessold.service.capacity.CapacityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class HomeController {

    @Autowired
    private CapacityService capacityService;

    //测试
    @GetMapping("/index")
    public String sendData(){
        capacityService.sendDataToKafka();
        return "ok";
    }
}
