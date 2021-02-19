package com.alibaba.jvm.sandbox.module.debug.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProcessUtils {

    /**
     * 获得当前进程名称
     *
     * @return
     */
    public static String getCurrentProcessName() {
        String currentProcessName = "";

        String jps = "jps";
        File jpsFile = findJps();
        if (jpsFile != null) {
            jps = jpsFile.getAbsolutePath();
        }

        String[] command = new String[]{jps, "-l"};

        List<String> lines = ExecutingCommand.runNative(command);

        int currentPid = Integer.parseInt(PidUtils.currentPid());
        for (String line : lines) {
            String[] strings = line.trim().split("\\s+");
            if (strings.length < 1) {
                continue;
            }
            int pid = Integer.parseInt(strings[0]);
            if (pid == currentPid) {
                currentProcessName = strings[1];
            }
        }

        return currentProcessName;
    }

    private static File findJps() {
        // Try to find jps under java.home and System env JAVA_HOME
        String javaHome = System.getProperty("java.home");
        String[] paths = {"bin/jps", "bin/jps.exe", "../bin/jps", "../bin/jps.exe"};

        List<File> jpsList = new ArrayList<File>();
        for (String path : paths) {
            File jpsFile = new File(javaHome, path);
            if (jpsFile.exists()) {
                jpsList.add(jpsFile);
            }
        }

        if (jpsList.isEmpty()) {
            String javaHomeEnv = System.getenv("JAVA_HOME");
            for (String path : paths) {
                File jpsFile = new File(javaHomeEnv, path);
                if (jpsFile.exists()) {
                    jpsList.add(jpsFile);
                }
            }
        }

        if (jpsList.isEmpty()) {
            return null;
        }

        // find the shortest path, jre path longer than jdk path
        if (jpsList.size() > 1) {
            Collections.sort(jpsList, new Comparator<File>() {
                @Override
                public int compare(File file1, File file2) {
                    try {
                        return file1.getCanonicalPath().length() - file2.getCanonicalPath().length();
                    } catch (IOException e) {
                        // ignore
                    }
                    return -1;
                }
            });
        }
        return jpsList.get(0);
    }
}
