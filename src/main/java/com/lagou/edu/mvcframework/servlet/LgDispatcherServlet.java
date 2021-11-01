package com.lagou.edu.mvcframework.servlet;

import com.lagou.edu.mvcframework.annotation.*;
import com.lagou.edu.mvcframework.pojo.Handler;
import com.lagou.edu.mvcframework.util.SecurityPersonnel;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LgDispatcherServlet extends HttpServlet {


    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    private List<Handler> handlerMapping = new ArrayList<>();


    @Override
    public void init(ServletConfig config) throws ServletException {
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");

        //加载配置文件 spring.properties
        doLoadConfig(contextConfigLocation);
        //扫描相关的类，扫描注解
        doScan(properties.getProperty("scanPackage"));
        //初始化bean对象（实现ioc容器，基于注解）
        doInstance();
        //实现依赖注入
        doAutoWired();

        //构造一个HandlerMapping处理器映射器，将配置好的url和method建立映射关系
        initHandlerMapping();
        System.out.println("lagou mvc 初始化完成。。。");
        //等待请求进入，处理请求

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //获取处理器
        Handler handler = getHandler(req);
        if (Objects.isNull(handler)) {
            resp.getWriter().write("404 not found");
            return;
        }
        Object controller = handler.getController();
        if (controller.getClass().isAnnotationPresent(Security.class)) {
            String username = req.getParameter("username");
            Security annotation = controller.getClass().getAnnotation(Security.class);
            String[] controllerSecurity = annotation.value();
            if (!hasSecurity(controllerSecurity, username)) {
                resp.getWriter().write("401 do not have Authorization");
                return;
            }
        }
        Method method = handler.getMethod();
        if (method.isAnnotationPresent(Security.class)) {
            String username = req.getParameter("username");
            Security annotation = method.getAnnotation(Security.class);
            String[] securityPerson = annotation.value();
            if (!hasSecurity(securityPerson, username)) {
                resp.getWriter().write("401 do not have Authorization");
                return;
            }
        }
        //设置参数
        Object[] paras = new Object[handler.getParams().size()];

        Map<String, String[]> parameterMap = req.getParameterMap();
        //设置其他参数
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String[] value = entry.getValue();
            String join = StringUtils.join(value);
            String paramName = entry.getKey();
            if (handler.getParams().containsKey(paramName)) {
                Integer index = handler.getParams().get(paramName);
                paras[index] = join;
            }
        }
        //设置公共参数
        if (handler.getParams().containsKey(HttpServletRequest.class.getSimpleName())) {
            Integer reqIndex = handler.getParams().get(HttpServletRequest.class.getSimpleName());
            paras[reqIndex] = req;
        }

        if (handler.getParams().containsKey(HttpServletResponse.class.getSimpleName())) {
            Integer resIndex = handler.getParams().get(HttpServletResponse.class.getSimpleName());
            paras[resIndex] = resp;
        }

        try {
            handler.getMethod().invoke(handler.getController(), paras);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    private boolean hasSecurity(String[] securityPerson, String name) {
        if (ArrayUtils.isEmpty(securityPerson)) return Boolean.TRUE;
        for (int i = 0; i < securityPerson.length; i++) {
            String securityName = securityPerson[i];
            if (name.equals(securityName)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /*获取Handler*/
    private Handler getHandler(HttpServletRequest request) {
        if (handlerMapping.isEmpty()) return null;
        String uri = request.getRequestURI();
        for (int i = 0; i < handlerMapping.size(); i++) {
            Handler handler = handlerMapping.get(i);
            Matcher matcher = handler.getPattern().matcher(uri);
            if (matcher.matches()) return handler;
        }
        return null;
    }

    //构造一个HandlerMapping处理器映射器，将配置好的url和method建立映射关系
    private void initHandlerMapping() {
        if (ioc.isEmpty()) return;

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> aClass = entry.getValue().getClass();
            if (aClass.isAnnotationPresent(LgController.class)) {
                String baseUrl = StringUtils.EMPTY;
                if (aClass.isAnnotationPresent(LgRequestMapping.class)) {
                    LgRequestMapping annotation = aClass.getAnnotation(LgRequestMapping.class);
                    baseUrl = annotation.value();
                }

                //
                Method[] methods = aClass.getMethods();
                for (int i = 0; i < methods.length; i++) {
                    Method method = methods[i];
                    if (method.isAnnotationPresent(LgRequestMapping.class)) {
                        LgRequestMapping annotation = method.getAnnotation(LgRequestMapping.class);
                        String url = baseUrl + annotation.value();
                        if (StringUtils.isNotEmpty(url)) {
                            Handler handler = new Handler(Pattern.compile(url), entry.getValue(), method);

//                            参数记录
                            Parameter[] parameters = method.getParameters();
                            if (ArrayUtils.isNotEmpty(parameters)) {
                                for (int j = 0; j < parameters.length; j++) {
                                    Parameter parameter = parameters[j];
                                    if (parameter.getType() == HttpServletRequest.class || parameter.getType() == HttpServletResponse.class) {
                                        handler.getParams().put(parameter.getType().getSimpleName(), j);
                                    } else {
                                        handler.getParams().put(parameter.getName(), j);
                                    }
                                }
                            }
                            handlerMapping.add(handler);
                        }
                    }
                }
            }
        }

    }

    //实现依赖注入
    private void doAutoWired() {
        if (ioc.isEmpty()) return;

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();
            for (int i = 0; i < declaredFields.length; i++) {
                Field declaredField = declaredFields[i];
                if (!declaredField.isAnnotationPresent(LgAutowired.class)) {
                    continue;
                }
                LgAutowired annotation = declaredField.getAnnotation(LgAutowired.class);
                String beanName = annotation.value();
                if (StringUtils.isEmpty(beanName)) {
                    //没有配置具体的beanId,那就根据当前字段类型注入（接口注入）IDemoService
                    beanName = declaredField.getType().getName();
                }
                declaredField.setAccessible(true);
                try {
                    declaredField.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    //初始化bean对象（实现ioc容器，基于注解）
    private void doInstance() {
        if (classNames.isEmpty()) return;
        try {
            for (int i = 0; i < classNames.size(); i++) {
                String className = classNames.get(i);
                Class<?> aClass = Class.forName(className);
                if (aClass.isAnnotationPresent(LgController.class)) {
                    String simpleName = aClass.getSimpleName();//DemoController
                    ioc.put(lowerFirst(simpleName), aClass.newInstance());
                } else if (aClass.isAnnotationPresent(LgService.class)) {
                    LgService annotation = aClass.getAnnotation(LgService.class);
                    String iocName = annotation.value();

                    if (StringUtils.isNotBlank(iocName.trim())) {
                        ioc.put(iocName, aClass.newInstance());
                    } else {
                        ioc.put(lowerFirst(aClass.getSimpleName()), aClass.newInstance());
                    }

                    //再以接口名来初始化ioc容器，问题：这里为什么要用新增的实例

                    Class<?>[] interfaces = aClass.getInterfaces();
                    for (int j = 0; j < interfaces.length; j++) {
                        Class<?> anInterface = interfaces[j];
                        //以接口的全限定类名作为id放入ioc容器
                        ioc.put(anInterface.getName(), aClass.newInstance());
                    }
                } else {
                    continue;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    private String lowerFirst(String str) {
        char[] chars = str.toCharArray();
        if (chars[0] >= 'A' && chars[0] <= 'Z') {
            chars[0] += 32;
        }
        return String.valueOf(chars);
    }

    //扫描相关的类，扫描注解
    private void doScan(String scanPackage) {
        System.out.println("scanPackage is ：" + scanPackage);
        String scanPackagePath = Thread.currentThread().getContextClassLoader().getResource("").getPath()
                + scanPackage.replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        File pack = new File(scanPackagePath);
        File[] files = pack.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                doScan(scanPackage + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = scanPackage + "." + file.getName().replaceAll(".class", "");
                classNames.add(className);
            }
        }
    }

    //加载配置文件 spring.properties
    private void doLoadConfig(String contextConfigLocation) {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
