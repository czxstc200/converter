package cn.edu.bupt.server.scanners;

import cn.edu.bupt.server.annotation.Controller;
import cn.edu.bupt.server.annotation.RequestMapping;
import cn.edu.bupt.server.annotation.RequestParam;
import cn.edu.bupt.server.parser.RequestParser;
import io.netty.handler.codec.http.FullHttpRequest;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @Description: Scanner
 * @Author: czx
 * @CreateDate: 2019-05-30 23:19
 * @Version: 1.0
 */
public class Scanner {

    private Scanner(){
    }

    private static final Scanner INSTANCE = new Scanner();

    public static Scanner getInstance(){
        return INSTANCE;
    }

    private Set<Class<?>> controllers;

    public Object invokeMethod(FullHttpRequest request) throws Exception{
        Map<String,String> params = RequestParser.parse(request);
        return invokeMethod(request.uri(),params);
    }

    public Object invokeMethod(String url,Map<String,String> params) throws Exception{
        for (Class<?> cls : getControllers()) {

            // 判断Controller的URL前缀
            String prefix = "";
            if(cls.getAnnotation(RequestMapping.class)!=null) {
                prefix = cls.getAnnotation(RequestMapping.class).value();
                if (!url.startsWith(prefix)) {
                    continue;
                }
            }

            // 获取Controller下的方法
            Method[] methods = cls.getDeclaredMethods();
            for (Method method : methods) {
                // 获取方法的URL
                RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                if (annotation != null) {
                    //找到RequestMapping的注入value值
                    String value = annotation.value();
                    //提取不带参数的URL
                    int endIndex = url.indexOf('?');
                    if(endIndex==-1){
                        endIndex=url.length();
                    }
                    String urlWithoutParam = url.substring(prefix.length(),endIndex);

                    // 判断方法是否符合
                    if (value.equals(urlWithoutParam)) {
                        Parameter[] parameters = method.getParameters();
                        Object[] paramValues = new Object[parameters.length];
                        for(int i = 0;i<paramValues.length;i++){
                            String paramStr = params.get(parameters[i].getAnnotation(RequestParam.class).value());
                            paramValues[i]=typeCast(parameters[i].getType(),paramStr);
                        }
                        return method.invoke(cls.newInstance(), paramValues);
                    }
                }
            }
        }
        throw new Exception("404");
    }

    public Object typeCast(Type type, String paramStr){
        String typeName = type.getTypeName();
        if(typeName.equals("int")||typeName.equals("java.lang.Integer")){
            return Integer.valueOf(paramStr);
        }else if(typeName.equals("boolean")||typeName.equals("java.lang.Boolean")){
            return Boolean.valueOf(paramStr);
        }else if(typeName.equals("float")||typeName.equals("java.lang.Float")){
            return Float.valueOf(paramStr);
        }else if(typeName.equals("long")||typeName.equals("java.lang.Long")){
            return Long.valueOf(paramStr);
        }else if(typeName.equals("double")||typeName.equals("java.lang.Double")){
            return Double.valueOf(paramStr);
        }else if(typeName.equals("char")||typeName.equals("java.lang.Character")){
            return paramStr.charAt(0);
        }else if(typeName.equals("short")||typeName.equals("java.lang.Short")){
            return Short.valueOf(paramStr);
        }else if(typeName.equals("byte")||typeName.equals("java.lang.Byte")){
            return Byte.valueOf(paramStr);
        }else{
            return paramStr;
        }
    }

    private Set<Class<?>> getControllers() throws Exception{
        if (controllers == null) {
            synchronized (Scanner.class){
                if(controllers==null) {
                    Set<Class<?>> localControllers = new HashSet<>();
                    Set<Class<?>> clsList = getClasses("cn.edu.bupt");
                    if (clsList != null && clsList.size() > 0) {
                        for (Class<?> cls : clsList) {
                            if (cls.getAnnotation(Controller.class) != null) {
                                localControllers.add(cls);
                            }
                        }
                    }
                    controllers = localControllers;
                }
            }
        }
        return controllers;
    }

    private Set<Class<?>> getClasses(String packageName) {
        // 第一个class类的集合
        Set<Class<?>> classes = new HashSet<>();
        // 是否循环迭代
        boolean recursive = true;
        // 获取包的名字 并进行替换
        String packageDirName = packageName.replace('.', '/');
        // 定义一个枚举的集合 并进行循环来处理这个目录下的东西
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            // 循环迭代下去
            while (dirs.hasMoreElements()) {
                // 获取下一个元素
                URL url = dirs.nextElement();
                // 得到协议的名称
                String protocol = url.getProtocol();
                // 如果是以文件的形式保存
                if ("file".equals(protocol)) {
                    // 获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中，以下俩种方法都可以
                    //网上的第一种方法，
                    findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
                } else if ("jar".equals(protocol)) {
                    // 如果是jar包文件
                    // 定义一个JarFile
                    JarFile jar;
                    try {
                        // 获取jar
                        jar = ((JarURLConnection) url.openConnection()).getJarFile();
                        // 从此jar包 得到一个枚举类
                        Enumeration<JarEntry> entries = jar.entries();
                        // 同样的进行循环迭代
                        while (entries.hasMoreElements()) {
                            // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            // 如果是以/开头的
                            if (name.charAt(0) == '/') {
                                // 获取后面的字符串
                                name = name.substring(1);
                            }
                            // 如果前半部分和定义的包名相同
                            if (name.startsWith(packageDirName)) {
                                int idx = name.lastIndexOf('/');
                                // 如果以"/"结尾 是一个包
                                if (idx != -1) {
                                    // 获取包名 把"/"替换成"."
                                    packageName = name.substring(0, idx).replace('/', '.');
                                }
                                // 如果可以迭代下去 并且是一个包
                                if ((idx != -1) || recursive) {
                                    // 如果是一个.class文件 而且不是目录
                                    if (name.endsWith(".class") && !entry.isDirectory()) {
                                        // 去掉后面的".class" 获取真正的类名
                                        String className = name.substring(packageName.length() + 1, name.length() - 6);
                                        try {
                                            // 添加到classes
                                            classes.add(Class.forName(packageName + '.' + className));
                                        } catch (ClassNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return classes;
    }


    /**
     * 以文件的形式来获取包下的所有Class
     *
     * @param packageName
     * @param packagePath
     * @param recursive
     * @param classes
     */
    private static void findAndAddClassesInPackageByFile(String packageName,
                                                         String packagePath, final boolean recursive, Set<Class<?>> classes) {
        // 获取此包的目录 建立一个File
        File dir = new File(packagePath);
        // 如果不存在或者不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        // 如果存在 就获取包下的所有文件 包括目录
        File[] dirfiles = dir.listFiles(new FileFilter() {
            // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            public boolean accept(File file) {
                return (recursive && file.isDirectory())
                        || (file.getName().endsWith(".class"));
            }
        });
        // 循环所有文件
        for (File file : dirfiles) {
            // 如果是目录 则继续扫描
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "."
                                + file.getName(), file.getAbsolutePath(), recursive,
                        classes);
            } else {
                // 如果是java类文件 去掉后面的.class 只留下类名
                String className = file.getName().substring(0,file.getName().length() - 6);
                try {
                    classes.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws Exception{
        Scanner scanner = Scanner.getInstance();
        Map<String,String> map = new HashMap<>();
        map.put("aa","hhh");
        map.put("bb","3");
        System.out.println(scanner.invokeMethod("/about",map));
    }
}
