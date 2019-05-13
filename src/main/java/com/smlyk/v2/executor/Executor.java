package com.smlyk.v2.executor;

/**
 * @author yekai
 */
public interface Executor {

    <T> T query(String statement, Object[] parameter, Class pojo);
}
