package com.smlyk.v2.session;

import com.smlyk.v2.annotation.Entity;
import com.smlyk.v2.annotation.Select;
import com.smlyk.v2.binding.MapperRegistry;
import com.smlyk.v2.executor.CachingExecutor;
import com.smlyk.v2.executor.Executor;
import com.smlyk.v2.executor.SimpleExecutor;
import com.smlyk.v2.plugin.Interceptor;
import com.smlyk.v2.plugin.InterceptorChain;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 全局配置类
 * @author yekai
 */
public class Configuration {

    /**
     * SQL映射关系配置，使用注解时不用重复配置
     */
    public static final ResourceBundle sqlMappings;

    /**
     * 全局配置
     */
    public static final ResourceBundle properties;

    /**
     * 维护接口与工厂类关系
     */
    public static final MapperRegistry MAPPER_REGISTRY = new MapperRegistry();

    /**
     * 维护接口方法与SQL关系
     */
    public static final Map<String, String> mappedStatements = new HashMap<>();

    /**
     * 所有的mapper接口
     */
    private List<Class<?>> mapperList = new ArrayList<>();

    /**
     * 类所有文件
     */
    private List<String> classPaths = new ArrayList<>();

    /**
     * 拦截器链
     */
    private InterceptorChain interceptorChain = new InterceptorChain();


    static {
        sqlMappings = ResourceBundle.getBundle("yksql-v2");
        properties = ResourceBundle.getBundle("ykmybatis");
    }


    /**
     * 初始化时解析全局配置文件
     */
    public Configuration(){
        //注意：在properties和注解中重复配置SQL会覆盖

        //1.解析yksql-v2.properties
        sqlMappings.keySet().forEach(key -> {
            Class mapper = null;
            String statement = null;
            String pojoStr = null;
            Class pojo = null;
            //properties中的value用--隔开，第一个是SQL语句
            statement = sqlMappings.getString(key).split("--")[0];
            //properties中的value用--隔开，第二个是需要转换的pojo类型
            pojoStr = sqlMappings.getString(key).split("--")[1];

            //properties中的key是接口类型+方法
            //从接口类型+方法中截取接口类型
            try {
                String mapperStr = key.substring(0, key.lastIndexOf("."));
                mapper = Class.forName(mapperStr);
                pojo = Class.forName(pojoStr);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            MAPPER_REGISTRY.addMapper(mapper, pojo);
            mappedStatements.put(key, statement);
        });

        //2.解析Mapper接口配置，扫描注册
        String mapperPath = properties.getString("mapper.path");
        scanPackage(mapperPath);
        mapperList.stream()
                .forEach(mapper -> parsingClass(mapper));

        //3.解析插件，可配置多个插件
        String pluginPathValue = properties.getString("plugin.path");
        String[] pluginPaths = pluginPathValue.split(",");
        if (null != pluginPaths){
            for (String plugin : pluginPaths){
                //将插件添加到interceptorChain中
                Interceptor interceptor = null;
                try {
                    interceptor = (Interceptor) Class.forName(plugin).newInstance();
                    interceptorChain.addInterceptor(interceptor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 根据statement判断是否存在映射的SQL
     * @param statementName
     * @return
     */
    public boolean hasStatement(String statementName){
        return mappedStatements.containsKey(statementName);
    }

    /**
     * 根据statement ID获取SQL
     * @param id
     * @return
     */
    public String getMappedStatement(String id){
        return mappedStatements.get(id);
    }

    /**
     * 获取mapper代理对象
     * @param clazz
     * @param sqlSession
     * @param <T>
     * @return
     */
    public <T> T getMapper(Class<T> clazz, DefaultSqlSession sqlSession){
        return MAPPER_REGISTRY.getMapper(clazz, sqlSession);
    }

    public Executor newExecutor(){
        Executor executor = null;
        if (properties.getString("cache.enabled").equals("true")){
            executor = new CachingExecutor(new SimpleExecutor());
        }else {
            executor = new SimpleExecutor();
        }

        //目前只拦截了Executor，所有的插件都对Executor进行代理，没有对拦截类和方法签名进行判断
        if (interceptorChain.hasPlugin()){
            return (Executor) interceptorChain.pluginAll(executor);
        }
        return executor;
    }

    /**
     * 解析Mapper接口上配置的注解（SQL语句）
     * @param mapper
     */
    private void parsingClass(Class<?> mapper) {
        //1.解析类上的注解
        //如果有@Entity注解，说明是查询数据库的接口
        if (mapper.isAnnotationPresent(Entity.class)){
            for (Annotation annotation :  mapper.getAnnotations()){
                if (annotation.annotationType().equals(Entity.class)){
                    //注册接口与实体类的映射关系
                    MAPPER_REGISTRY.addMapper(mapper, ((Entity)annotation).value());
                }
            }
        }

        //2.解析方法上的注解
        Method[] methods = mapper.getMethods();
        for (Method method : methods){
            //解析@Select注解的SQL语句
            if (method.isAnnotationPresent(Select.class)){
                for (Annotation annotation : method.getDeclaredAnnotations()){
                    if (annotation.annotationType().equals(Select.class)){
                        //注册接口类型+方法名和SQL语句的映射关系
                        String statement = method.getDeclaringClass().getName() + "." + method.getName();
                        mappedStatements.put(statement, ((Select)annotation).value());
                    }
                }
            }
        }
    }

    /**
     * 根据全局配置文件的Mapper接口路径，扫描所有接口
     * @param mapperPath
     */
    private void scanPackage(String mapperPath) {
        String classPath = this.getClass().getResource("/").getPath();
        mapperPath.replace(".", File.separator);
        String mainPath = classPath + mapperPath;
        doPath(new File(mainPath));
        classPaths.stream()
                .forEach(className-> {
                    className = className.replace(classPath.replace("/","\\").replaceFirst("\\\\",""),"").replace("\\",".").replace(".class","");
                    Class<?> clazz = null;
                    try {
                        clazz = Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    if (clazz.isInterface()){
                        mapperList.add(clazz);
                    }
                });
    }

    /**
     * 获取文件或文件夹下所有的类 .class
     * @param file
     */
    private void doPath(File file) {
        if (file.isDirectory()){
            //文件夹，遍历
            File[] files = file.listFiles();
            for (File f : files) {
                doPath(f);
            }
        }else {
            //文件，直接添加
            if (file.getName().endsWith(".class")){
                classPaths.add(file.getPath());
            }
        }
    }


}
