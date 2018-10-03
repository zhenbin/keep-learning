package com.github.zhenbin.nettytest.handler.business;

public interface Handler {
    byte[] process(byte[] msg);
}
