package com.github.zhenbin.nettytest.bootstrap;

import com.github.zhenbin.nettytest.server.AdminServer;
import com.github.zhenbin.nettytest.server.MasterServer;

public class Start {
    public static void main(String[] args) throws Exception {
        MasterServer masterServer = new MasterServer(12354);
        Thread thread = new Thread(
                ((Runnable) () -> {
                    try {
                        masterServer.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }));
        thread.start();
        AdminServer adminServer = new AdminServer(12355, masterServer);
        adminServer.start();
    }
}
