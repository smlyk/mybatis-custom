package com.smlyk.v1;

import java.lang.reflect.Proxy;
import java.util.ResourceBundle;

/**
 * @author yekai
 */
public class YKConfiguration {


    public static final ResourceBundle sqlMappings;

    static {
        sqlMappings = ResourceBundle.getBundle("yksql");
    }

    public <T> T getMapper(Class<T> clazz, YKSqlSession sqlSession) {

        return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{clazz}, new YKMapperProxy(sqlSession));
    }
}
