package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.Sentry;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.jvm.sandbox.module.debug.util.Express;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static com.alibaba.jvm.sandbox.module.debug.JdbcLoggerModule.MonitorJavaSqlPreparedStatementStep.*;

/**
 * 基于JDBC的SQL日志
 *
 * @author lilizhao
 */
@MetaInfServices(Module.class)
@Information(id = "debug-jdbc-logger", version = "0.0.1", author = "lilizhao")
public class JdbcLoggerModule extends AbstractModule {

    private final Logger smLogger = LoggerFactory.getLogger("DEBUG-JDBC-LOGGER");

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        super.loadCompleted();

        monitorJavaSqlStatement();
        monitorJavaSqlPreparedStatement();
    }

    // 监控java.sql.Statement的所有实现类
    private void monitorJavaSqlStatement() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(Statement.class).includeSubClasses()
                .onBehavior("execute*")
                /**/.withParameterTypes(String.class)
                /**/.withParameterTypes(String.class, int.class)
                /**/.withParameterTypes(String.class, int[].class)
                /**/.withParameterTypes(String.class, String[].class)
                .onWatch(new AdviceListener() {

                    private final String MARK_STATEMENT_EXECUTE = "MARK_STATEMENT_EXECUTE";
                    private final String PREFIX = "STMT";

//                    @Override
//                    protected void beforeLine(Advice advice, int lineNum) {
//                        super.beforeLine(advice, lineNum);
//                    }

                    @Override
                    public void before(Advice advice) {
                        advice.attach(System.currentTimeMillis(), MARK_STATEMENT_EXECUTE);
                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        if (advice.hasMark(MARK_STATEMENT_EXECUTE)) {
                            final long costMs = System.currentTimeMillis() - (Long) advice.attachment();
                            final String sql = advice.getParameterArray()[0].toString();
                            logSql(PREFIX, sql, costMs, true, null);
                        }
                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        if (advice.hasMark(MARK_STATEMENT_EXECUTE)) {
                            final long costMs = System.currentTimeMillis() - (Long) advice.attachment();
                            final String sql = advice.getParameterArray()[0].toString();
                            logSql(PREFIX, sql, costMs, false, advice.getThrowable());
                        }
                    }
                });
    }

    enum MonitorJavaSqlPreparedStatementStep {
        waiting_Connection_prepareStatement,
        waiting_PreparedStatement_execute,
        waiting_PreparedStatement_execute_finish,
    }

    private void monitorJavaSqlPreparedStatement() {

        new EventWatchBuilder(moduleEventWatcher)
                .onClass(Connection.class)
                .includeSubClasses()
                .onBehavior("prepareStatement")
                .onClass(PreparedStatement.class)
                .includeSubClasses()
                .onBehavior("execute*")
                .onWatch(new AdviceListener() {

                    private final String MARK_PREPARED_STATEMENT_EXECUTE = "MARK_PREPARED_STATEMENT_EXECUTE";
                    private final String PREFIX = "PSTMT";


                    private final Sentry<MonitorJavaSqlPreparedStatementStep> sentry
                            = new Sentry<MonitorJavaSqlPreparedStatementStep>(waiting_Connection_prepareStatement);

                    @Override
                    public void before(Advice advice) {
                        // Connection.prepareStatement()
                        Object target = advice.getTarget();
                        String targetClassName = target.getClass().getName();

                        if (target instanceof Connection
                                && sentry.next(waiting_Connection_prepareStatement, waiting_PreparedStatement_execute)) {
                            sentry.attach(advice.getParameterArray()[0].toString());
                        }

                        // PreparedStatement.execute*()
                        if (target instanceof PreparedStatement
                                && sentry.next(waiting_PreparedStatement_execute, waiting_PreparedStatement_execute_finish)) {
                            Bind bind = binding(advice);

                            boolean findFlag = false;

                            //mysql jdbc 驱动获取参数
                            String sql = sentry.attachment();
                            try {
                                byte[][] parameterValues = (byte[][]) Express.ExpressFactory.newExpress(bind).get("target.parameterValues");
                                for (int i = 0; i < parameterValues.length; i++) {
                                    sql.replaceFirst("[?]", new String(parameterValues[i]));
                                }

                                sentry.attach(sql);
                                findFlag = true;
                            } catch (Exception e) {
                            }

                            //mybatis HikariProxy 驱动获取参数
                            if (!findFlag) {
                                try {
                                    byte[][] parameterValues = (byte[][]) Express.ExpressFactory.newExpress(bind).get("target.delegate.parameterValues");
                                    for (int i = 0; i < parameterValues.length; i++) {
                                        sql = sql.replaceFirst("[?]", new String(parameterValues[i]));
                                    }

                                    sentry.attach(sql);
                                    findFlag = true;
                                } catch (Exception e) {
                                }
                            }

//                            //ali Druid 驱动获取参数
//                            try {
//                                if (targetClassName.equals("com.alibaba.druid.pool.DruidPooledPreparedStatement")) {
////                                    int size = (int) Express.ExpressFactory.newExpress(bind).get("target.stmt.parameters.size");
//                                    int size = (int) Express.ExpressFactory.newExpress(bind).get("target.stmt.parameters.size");
//                                    Map map = (Map) Express.ExpressFactory.newExpress(bind).get("target.stmt.parameters");
//
//                                    for (int i = 0; i < size; i++) {
//                                        Object o = map.get(i);
//
//                                        Object value = Express.ExpressFactory.newExpress(o).get("value");
//                                        if (value instanceof String) {
//                                            value = "\"" + value + "\"";
//                                        }
//
//                                        sql = sql.replaceFirst("[?]", value.toString());
//                                    }
//
//                                    sentry.attach(sql);
//                                    findFlag = true;
//                                }
//                            } catch (Exception e) {
//                                smLogger.error("", e);
//                            }

                            if (!findFlag) {
                                smLogger.error("=====================" + targetClassName + "=========================");
                            }

                            advice.attach(System.currentTimeMillis(), MARK_PREPARED_STATEMENT_EXECUTE);
                        }
                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        if (finishing(sentry, advice)) {
                            final long costMs = System.currentTimeMillis() - (Long) advice.attachment();
                            final String sql = sentry.attachment();
                            logSql(PREFIX, sql, costMs, true, null);
                        }
                    }

                    @Override
                    public void afterThrowing(Advice advice) {
                        if (finishing(sentry, advice)) {
                            final long costMs = System.currentTimeMillis() - (Long) advice.attachment();
                            final String sql = sentry.attachment();
                            logSql(PREFIX, sql, costMs, false, advice.getThrowable());
                        }
                    }

                    private boolean finishing(final Sentry<MonitorJavaSqlPreparedStatementStep> sentry,
                                              final Advice advice) {
                        return advice.hasMark(MARK_PREPARED_STATEMENT_EXECUTE)
                                && sentry.next(waiting_PreparedStatement_execute_finish, waiting_Connection_prepareStatement);
                    }

                });
    }

    // SQL日志输出
    private void logSql(final String prefix,
                        final String sql,
                        final long costMs,
                        final boolean isSuccess,
                        final Throwable cause) {
        String isSuccessStr = isSuccess ? "success" : "failed";
        smLogger.info("{};cost:{}ms;{};sql:{}",
                prefix,
                costMs,
                isSuccessStr,
                sql,
                cause
        );

//        String causeStr = "";
//        if (cause != null) {
//            try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
//                cause.printStackTrace(pw);
//
//                causeStr = sw.toString();
//            } catch (Exception e) {
//                smLogger.info("异常转化exception: ", e);
//            }
//        }
//
//        JdbcEvent jdbcEvent = new JdbcEvent(prefix, costMs, isSuccessStr, sql, causeStr, System.currentTimeMillis(),
//                hostIp, currentProcessName);
//        elasticsearchService.sink(jdbcEvent.toMap());
    }

}
