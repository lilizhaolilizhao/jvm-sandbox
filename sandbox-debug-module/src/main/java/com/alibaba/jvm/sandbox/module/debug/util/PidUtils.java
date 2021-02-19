package com.alibaba.jvm.sandbox.module.debug.util;

import java.lang.management.ManagementFactory;

/**
 * pid 工具类
 */
public class PidUtils {
    private static String PID = "-1";

    static {
        // https://stackoverflow.com/a/7690178
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        int index = jvmName.indexOf('@');

        if (index > 0) {
            try {
                PID = Long.toString(Long.parseLong(jvmName.substring(0, index)));
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    public static String currentPid() {
        return PID;
    }
}
