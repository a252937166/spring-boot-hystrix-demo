package com.ouyanglol.hytrixdemo;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.springframework.stereotype.Component;

/**
 * @author ouyangduning
 * @date 2020/11/25 15:12
 */
@Component
public class HystrixTestBean {
    @HystrixCommand(commandProperties = {}, fallbackMethod = "fail1")
    public String normal() {
        return "ok";
    }

    @HystrixCommand(commandProperties = {}, fallbackMethod = "fail1")
    public String timeOut() throws InterruptedException {
        Thread.sleep(5000);
        return "ok";
    }

    @HystrixCommand(commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000")
    }, fallbackMethod = "fail1")
    public String normal2() throws InterruptedException {
        Thread.sleep(4000);
        return "ok";
    }

    @HystrixCommand(commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "1000")
    }, fallbackMethod = "fail1")
    public String timeOut2() throws InterruptedException {
        Thread.sleep(1100);
        return "ok";
    }

    @HystrixCommand(commandProperties = {}, fallbackMethod = "fail1")
    public String error() throws InterruptedException {
        int a = 1/0;
        return "ok";
    }

    private String fail1() {
        System.out.println("fail1");
        return "fail1";
    }
}
