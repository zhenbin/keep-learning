package com.github.zhenbin.nettytest.handler.business;

public class EchoHandler implements Handler
{
    public byte[] process(byte[] msg)
    {
        System.out.println("i am echo handler!");
        return msg;
    }
}