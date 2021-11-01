package com.lagou.edu.demo.controller;

import com.lagou.edu.demo.service.IDemoService;
import com.lagou.edu.mvcframework.annotation.LgAutowired;
import com.lagou.edu.mvcframework.annotation.LgController;
import com.lagou.edu.mvcframework.annotation.LgRequestMapping;
import com.lagou.edu.mvcframework.annotation.Security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@LgController
@LgRequestMapping("/demo")
public class DemoController {

    @LgAutowired
    private IDemoService demoService;

    @LgRequestMapping("/query")
    @Security("zhangsan")
    public String query(HttpServletRequest request, HttpServletResponse response, String username) {
        return demoService.get(username);
    }

    @LgRequestMapping("/handler01")
    @Security({"zhangsan", "lisi"})
    public String handler01(HttpServletRequest request, HttpServletResponse response, String username) throws IOException {
        return demoService.get(username);
    }

    @LgRequestMapping("/handler02")
    @Security("wangwu")
    public String handler02(HttpServletRequest request, HttpServletResponse response, String username) throws IOException {
        return demoService.get(username);
    }
}
