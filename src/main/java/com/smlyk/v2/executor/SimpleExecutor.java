package com.smlyk.v2.executor;

/**
 * @author yekai
 */
public class SimpleExecutor implements Executor{
    @Override
    public <T> T query(String statement, Object[] parameter, Class pojo) {

        StatementHandler statementHandler = new StatementHandler();
        return statementHandler.query(statement, parameter, pojo);
    }
}
