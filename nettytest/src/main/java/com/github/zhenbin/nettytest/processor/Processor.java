package com.github.zhenbin.nettytest.processor;

public interface Processor<T> {
    byte[] process(T msg);
}
