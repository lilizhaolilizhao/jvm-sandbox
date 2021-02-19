//package com.alibaba.jvm.sandbox.module.debug.entity;
//
//import com.google.common.collect.ImmutableMap;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.Map;
//
//@Data
//@AllArgsConstructor
//public class JdbcEvent {
//    private String prefix;
//    private long costMs;
//    private String isSuccess;
//    private String sql;
//    private String cause;
//    private long timestamp;
//    private String datasourceIP; //数据源IP
//    private String processName; //进程映像名称
//
//    public Map<String, Object> toMap() {
//        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
//
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
//        String indexDate = sdf.format(new Date(timestamp));
//
//        builder.put("index_name", "jdbc_event_" + indexDate);
//        builder.put("prefix", prefix);
//        builder.put("cost_ms", costMs);
//        builder.put("is_success", isSuccess);
//        builder.put("sql", sql);
//        builder.put("cause", cause);
//        builder.put("timestamp", timestamp);
//        builder.put("datasource_ip", datasourceIP);
//        builder.put("process_name", processName);
//
//        return builder.build();
//    }
//}
