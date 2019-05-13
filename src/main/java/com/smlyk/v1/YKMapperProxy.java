package com.smlyk.v1;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author yekai
 */
public class YKMapperProxy implements InvocationHandler{

    private YKSqlSession sqlSession;

    public YKMapperProxy(YKSqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String mapperInterface = method.getDeclaringClass().getName();
        String methodName = method.getName();
        String statementId = mapperInterface + "." + methodName;

        return sqlSession.selectOne(statementId, args[0]);
    }
}
