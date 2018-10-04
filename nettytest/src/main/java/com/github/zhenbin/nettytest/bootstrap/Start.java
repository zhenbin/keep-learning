package com.github.zhenbin.nettytest.bootstrap;

import com.github.zhenbin.nettytest.server.MasterServer;

public class Start {
    public static void main(String[] args) throws Exception {
        new MasterServer(12354).start();
    }
}
