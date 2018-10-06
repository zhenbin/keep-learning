package com.github.zhenbin.nettytest;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

public class AdminClient {
    public static void main(String[] args) {
        try {
            Socket s = new Socket("127.0.0.1", 12355);

            //构建IO
            OutputStream os = s.getOutputStream();

//            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
            //向服务器端发送一条消息
            ByteBuf byteBuf = Unpooled.buffer();
            byte[] bs = "hello executors. i am admin.".getBytes();
            byteBuf.writeByte(1);
            // 如果是little-endian则使用writeIntLE
            byteBuf.writeInt(bs.length);
            byteBuf.writeBytes(bs);
            System.out.println(byteBuf.toString(Charset.forName("UTF-8")));

            int offset = byteBuf.arrayOffset() + byteBuf.readerIndex();
            os.write(byteBuf.array(), offset, byteBuf.readableBytes());
            os.flush();

            //读取服务器返回的消息
            InputStream is = s.getInputStream();
            DataInputStream i = new DataInputStream(is);
            for (; ; ) {
                byte[] len = new byte[5];
                i.readFully(len);
                ByteBuf readBuf = Unpooled.buffer();
                readBuf.writeBytes(len);
                int type = readBuf.getByte(0);
                int lengh = readBuf.getInt(1);

                byte[] msg = new byte[lengh];
                is.read(msg, 0, lengh);
                System.out.println("服务器：\n" + new String(msg));

                if (2 == type) {
                    System.out.println("need to response, type: 2");
                    os.write(len);
                    os.write(msg);
                    os.flush();
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
