package com.zhenbin;

public class EchoHandler implements com.zhenbin.Handler
{
    public byte[] process(byte[] msg)
    {
        System.out.println("i am echo handler!");
        return msg;
    }
}
