package com.zhenbin;

public class ProtocMsg
{
    public final byte type;
    public final byte[] msg;

    public ProtocMsg(byte type, byte[] msg)
    {
        this.type = type;
        this.msg = msg;
    }
}
