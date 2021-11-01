package com.lagou.edu.demo.service.impl;

import com.lagou.edu.demo.service.IDemoService;
import com.lagou.edu.mvcframework.annotation.LgService;

@LgService
public class DemoServiceImpl implements IDemoService {
    @Override
    public String get(String name) {
        System.out.println("service 方法入参：" + name);
        return name;
    }
}
