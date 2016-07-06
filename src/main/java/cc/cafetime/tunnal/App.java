package cc.cafetime.tunnal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ch.ethz.ssh2.Connection;

/**
 * Hello world!
 *
 */
public class App {
    private static Connection conn;
    private static Map<Integer, String[]> map;
    boolean isAuthenticated;

    static {
        // 加载tunnal.properties文件
        File file = new File("tunnal.properties");
        System.out.println("Load properties file from " + file.getAbsolutePath());
        Properties prop = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            prop.load(in);
            // 从properties文件中读取远程主机配置和端口映射表
            String hostname = prop.getProperty("hostname");
            String username = prop.getProperty("username");
            String password = prop.getProperty("password");
            // 建立主机连接
            conn = new Connection(hostname);
            conn.connect();
            boolean isAuthenticated = conn.authenticateWithPassword(username, password);
            if (isAuthenticated == false)
                throw new IOException("Authentication failed.");
            System.out.println("connect to " + hostname + " linked!");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String portMapStr = prop.getProperty("map");
        map = resolvePortMap(portMapStr);
    }

    public static void main(String[] args) {
        for (Integer num : map.keySet()) {
            String[] ip_ports = map.get(num);
            String remoteIP = ip_ports[1];
            String localPort = ip_ports[0]; // 本地端口
            String remotePort = ip_ports[2];// 远端端口
            try {
                conn.createLocalPortForwarder(Integer.parseInt(localPort), remoteIP, Integer.parseInt(remotePort));
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Local " + localPort + " port forward to " + remoteIP + ":" + remotePort);
        }
        // 将hook线程添加到运行时环境中去
        Runtime.getRuntime().addShutdownHook(new CleanWorkThread());
        Thread.currentThread().suspend();
    }

    static class CleanWorkThread extends Thread {
        @Override
        public void run() {
            System.out.println("connection will be close ....");
            conn.close();
        }
    }

    private static Map<Integer, String[]> resolvePortMap(String portMaps) {
        Map<Integer, String[]> portMap = new HashMap<Integer, String[]>();
        String[] maps = portMaps.split(";");
        Integer i = 0;
        for (String map : maps) {
            String[] parts = map.split(":");
            portMap.put(i++, parts);
        }
        return portMap;
    }

}
