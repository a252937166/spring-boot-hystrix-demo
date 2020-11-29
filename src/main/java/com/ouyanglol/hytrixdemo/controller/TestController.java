package com.ouyanglol.hytrixdemo.controller;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.ouyanglol.hytrixdemo.service.TService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ouyangduning
 * @date 2020/11/24 11:15
 */
@RestController
public class TestController {

    @Autowired
    private TService tService;

    @GetMapping("/t")
    @HystrixCommand(commandProperties = {
//    @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds",value = "5000")
    },fallbackMethod = "fail1"
    )
    public String t() throws InterruptedException {

        Thread.sleep(5000);
        return tService.t();
    }
    private String fail1() {
        System.out.println("fail1");
        return "fail1";
    }
}
