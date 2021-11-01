package com.lagou.edu.mvcframework.util;

public class SecurityPersonnel {

    private static final String[] hasSecurityPerson = new String[]{"zhangsan", "lisi", "wangwu"};

    public static boolean hasSecurity(String name) {
        for (int i = 0; i < hasSecurityPerson.length; i++) {
            String securityPerson = hasSecurityPerson[i];
            if (name.equals(securityPerson)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }
}
