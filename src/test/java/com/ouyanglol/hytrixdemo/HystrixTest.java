package com.ouyanglol.hytrixdemo;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author ouyangduning
 * @date 2020/11/25 15:12
 */
@Slf4j
public class HystrixTest extends HystrixDemoApplicationTests {

    @Autowired
    private HystrixTestBean hystrixTestBean;

    @Test
    public void t() throws InterruptedException {
        log.info(hystrixTestBean.normal());
        log.info(hystrixTestBean.normal2());
        log.info(hystrixTestBean.timeOut());
        log.info(hystrixTestBean.timeOut2());
        log.info(hystrixTestBean.error());

    }
}
