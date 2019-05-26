package alexrm84.myFirstCloudBox.client;

import alexrm84.myFirstCloudBox.common.AbstractMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;

public class Network {
    private static Socket socket;
    private static ObjectEncoderOutputStream outputStream;
    private static ObjectDecoderInputStream inputStream;

    public static void start(){
        try {
            socket = new Socket("127.0.0.1", 9999);
            outputStream = new ObjectEncoderOutputStream(socket.getOutputStream());
            inputStream = new ObjectDecoderInputStream(socket.getInputStream(), 22*1024*1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void stop(){
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean sendMessage(AbstractMessage message){
        try {
            outputStream.writeObject(message);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static AbstractMessage readObject() throws ClassNotFoundException, IOException {
        Object object = inputStream.readObject();
        return (AbstractMessage)object;
    }
}