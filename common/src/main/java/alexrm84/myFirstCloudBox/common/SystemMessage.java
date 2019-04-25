package alexrm84.myFirstCloudBox.common;

import lombok.Getter;

import java.nio.file.Path;
import java.util.LinkedList;

@Getter
public class SystemMessage extends AbstractMessage {
    private String typeMessage;
    private LinkedList<String> pathsList;
    private boolean connectionIsAlive;

    public SystemMessage setTypeMessage(String typeMessage) {
        this.typeMessage = typeMessage;
        return this;
    }

    public SystemMessage setPathsList(LinkedList<String> pathsList) {
        this.pathsList = pathsList;
        return this;
    }

    public SystemMessage setConnectionIsAlive(boolean connectionIsAlive) {
        this.connectionIsAlive = connectionIsAlive;
        return this;
    }
}
