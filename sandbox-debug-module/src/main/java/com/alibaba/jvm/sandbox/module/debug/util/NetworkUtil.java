package com.alibaba.jvm.sandbox.module.debug.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 网络工具
 */
public class NetworkUtil {

    /**
     * 获得本机IP地址
     * @return
     */
    public static String getHostIp() {
        String localIP = "127.0.0.1";
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = allNetInterfaces.nextElement();
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    if (ip != null
                            && ip instanceof Inet4Address
                            && !ip.isLoopbackAddress()
                            && ip.getHostAddress().indexOf(":") == -1) {
                        return localIP = ip.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
        }
        return localIP;
    }
}
