package alexrm84.myFirstCloudBox.common;

import java.io.*;

public class Serialization {

//    private ByteArrayOutputStream baos;
//    private ObjectOutputStream oos;
//
//    public Serialization(){
//        try {
//            baos = new ByteArrayOutputStream();
//            oos = new ObjectOutputStream(baos);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    public byte[] serialize(Object obj) throws IOException {
//        try {
//            oos.writeObject(obj);
//            byte[] b = baos.toByteArray();
//            System.out.println("сериализация" + new String(b));
//            return b;
//        }catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    public byte[] serialize(Object obj) throws IOException {
        try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
            try(ObjectOutputStream o = new ObjectOutputStream(b)){
                o.writeObject(obj);
            }
            return b.toByteArray();
        }
    }

    public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try(ByteArrayInputStream b = new ByteArrayInputStream(bytes)){
            try(ObjectInputStream o = new ObjectInputStream(b)){
                return o.readObject();
            }
        }
    }
}
