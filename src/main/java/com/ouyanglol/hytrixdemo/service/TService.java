package com.ouyanglol.hytrixdemo.service;

import org.springframework.stereotype.Service;

/**
 * @author ouyangduning
 * @date 2020/11/24 11:23
 */
@Service
public class TService {
//    @HystrixCommand(commandProperties = {
//            @HystrixProperty(name = "circuitBreaker.enabled", value = "true"),
//            // 至少有3个请求才进行熔断错误比率计算
//            /**
//             * 设置在一个滚动窗口中，打开断路器的最少请求数。
//             比如：如果值是20，在一个窗口内（比如10秒），收到19个请求，即使这19个请求都失败了，断路器也不会打开。
//             */
//            @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "3"),
//            //当出错率超过50%后熔断器启动
//            @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "50"),
//            // 熔断器工作时间，超过这个时间，先放一个请求进去，成功的话就关闭熔断，失败就再等一段时间
//            @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "5000"),
//            // 统计滚动的时间窗口
//            @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "10000")
//    },
//            fallbackMethod = "fail1"
//    )
    public String t() throws InterruptedException {
//        throw new RuntimeException("111111111111111");
//        int a = 1/0;
        return "ts";
    }
    private String fail1() {
        return "tf";
    }
}
