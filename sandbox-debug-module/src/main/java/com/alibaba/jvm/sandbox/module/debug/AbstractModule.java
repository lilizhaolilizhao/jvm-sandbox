package com.alibaba.jvm.sandbox.module.debug;

import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.module.debug.service.ElasticsearchService;
import com.alibaba.jvm.sandbox.module.debug.util.NetworkUtil;
import com.alibaba.jvm.sandbox.module.debug.util.ProcessUtils;
import com.alibaba.jvm.sandbox.module.debug.util.ResourceFileUtil;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Properties;

/**
 * 通用抽象实现
 */
public class AbstractModule implements Module, LoadCompleted {
    protected ElasticsearchService elasticsearchService;
    protected String hostIp;
    protected String currentProcessName;
    protected String cfgFile = "classpath:app.properties";

    @Override
    public void loadCompleted() {
        getBaseInfo();
//        initESService();
    }

    /**
     * 获得主机信息,进程信息
     */
    private void getBaseInfo() {
        hostIp = NetworkUtil.getHostIp();
        currentProcessName = ProcessUtils.getCurrentProcessName();
        try {
            String appCfgFile = new File(LoadCompleted.class.getProtectionDomain().getCodeSource().getLocation().getFile())
                    .getParentFile().getParentFile() + File.separator + "sandbox-module" + File.separator + "cfg"
                    + File.separator + "app.properties";
            if (new File(appCfgFile).exists()) {
                cfgFile = appCfgFile;
            }
        } catch (Exception e) {
        }
    }

    private void initESService() {
        Properties props = new Properties();
        try {
            props.load(IOUtils.toInputStream(ResourceFileUtil.load(cfgFile)));

            String esHost = props.getProperty("es.host");
            Integer esPort = Integer.valueOf(props.getProperty("es.port"));
            elasticsearchService = new ElasticsearchService(esHost, esPort);
        } catch (Exception e) {
        }
    }

    protected Bind binding(Advice advice) {
        return new Bind()
                .bind("class", advice.getBehavior().getDeclaringClass())
                .bind("method", advice.getBehavior())
                .bind("params", advice.getParameterArray())
                .bind("target", advice.getTarget())
                .bind("returnObj", advice.getReturnObj())
                .bind("throwExp", advice.getThrowable());
    }

    class Bind extends HashMap<String, Object> {
        Bind bind(final String name, final Object value) {
            put(name, value);
            return this;
        }
    }
}
