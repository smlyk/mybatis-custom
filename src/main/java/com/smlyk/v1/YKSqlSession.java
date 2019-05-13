package com.smlyk.v1;

/**
 * @author yekai
 */
public class YKSqlSession {

    private YKConfiguration configuration;

    private YKExecutor executor;

    public YKSqlSession(YKConfiguration configuration, YKExecutor executor) {
        this.configuration = configuration;
        this.executor = executor;
    }

    public <T> T getMapper(Class<T> clazz){

        return configuration.getMapper(clazz, this);
    }


    public <T> T selectOne(String statementId, Object paramter){

        String sql = YKConfiguration.sqlMappings.getString(statementId);
        if (null == sql || "".equals(sql)){
            return null;
        }
        return executor.query(sql, paramter);
    }


}
