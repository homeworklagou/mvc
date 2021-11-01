package com.lagou.edu.mvcframework.pojo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Handler {
    private Pattern pattern;

    private Object controller;

    private Method method;

    private Map<String, Integer> params;

    public Handler(Pattern pattern, Object controller, Method method) {
        this.pattern = pattern;
        this.controller = controller;
        this.method = method;
        this.params = new HashMap<>();
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Map<String, Integer> getParams() {
        return params;
    }

    public void setParams(Map<String, Integer> params) {
        this.params = params;
    }
}
