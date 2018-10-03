import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    public static void main(String[] args) {
        try {
            Socket s = new Socket("127.0.0.1", 12354);

            //构建IO
            OutputStream os = s.getOutputStream();

//            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
            //向服务器端发送一条消息
            ByteBuf byteBuf = Unpooled.buffer();
            byte[] bs = "abcde".getBytes();
            byteBuf.writeByte(1);
            // 如果是little-endian则使用writeIntLE
            byteBuf.writeInt(bs.length);
            byteBuf.writeBytes(bs);
            System.out.println(byteBuf.readableBytes());
            byte[] o = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(o, 0, byteBuf.readableBytes());

            System.out.println(new String(o));
            os.write(o);
            os.flush();

            //读取服务器返回的消息
            InputStream is = s.getInputStream();
            DataInputStream i = new DataInputStream(is);
            byte[] len = new byte[4];
            i.readFully(len);
            ByteBuf readBuf = Unpooled.buffer();
            readBuf.writeBytes(len);
            int lengh = readBuf.getInt(0);
            System.out.println("len:" + lengh);
            byte[] msg = new byte[lengh];
            is.read(msg, 0, lengh);
            System.out.println("服务器：" + new String(msg));
            Thread.sleep(100 * 1000);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
