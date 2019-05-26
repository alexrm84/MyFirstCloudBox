package alexrm84.myFirstCloudBox.common;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.LinkedList;

public class Serialization {

    private Kryo kryo;

    public Serialization(){
        kryo = new Kryo();
        kryo.register(Object.class);
        kryo.register(AbstractMessage.class);
        kryo.register(FileMessage.class);
        kryo.register(SystemMessage.class);
        kryo.register(String[].class);
        kryo.register(String.class);
        kryo.register(boolean.class);
        kryo.register(byte[].class);
        kryo.register(Command.class);
        kryo.register(LinkedList.class);
        kryo.register(StoredFile.class);
    }

    public byte[] serialize(Object obj) {
        Output output = new Output(1, 20*1024*1024);
        kryo.writeClassAndObject(output, obj);
        output.flush();
        output.close();
        return output.getBuffer();
    }

    public Object deserialize(byte[] bytes) {
        Input input = new Input(bytes);
        Object obj = kryo.readClassAndObject(input);
        input.close();
        return obj;
    }
}
