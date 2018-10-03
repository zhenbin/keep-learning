package com.github.zhenbin.keeplearning.nettytest.handler.business;

public interface Handler {
    byte[] process(byte[] msg);
}
