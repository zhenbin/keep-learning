package com.github.zhenbin.nettytest.dto;

public class ProtocMsg implements Type
{
    public final byte type;
    public final byte[] msg;

    public ProtocMsg(byte type, byte[] msg)
    {
        this.type = type;
        this.msg = msg;
    }

    public byte getType() {
        return type;
    }
}
