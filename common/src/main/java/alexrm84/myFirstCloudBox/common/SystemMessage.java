package alexrm84.myFirstCloudBox.common;

import javafx.collections.ObservableList;
import lombok.Getter;

import java.security.PublicKey;
import java.util.LinkedList;

@Getter
public class SystemMessage extends AbstractMessage {
    private PublicKey publicKeyRSA;
    private byte[] secretKeyAES;
    private boolean connectionIsAlive;
    private Command typeMessage;
    private String requestedPath;
    private String currentServerPath, currentClientPath;
    private LinkedList<StoredFile> pathsList;
    private String[] loginAndPassword;
    private boolean authorization;

    public SystemMessage setSecretKeyAES(byte[] secretKeyAES) {
        this.secretKeyAES = secretKeyAES;
        return this;
    }

    public SystemMessage setPublicKeyRSA(PublicKey publicKeyRSA) {
        this.publicKeyRSA = publicKeyRSA;
        return this;
    }

    public SystemMessage setTypeMessage(Command typeMessage) {
        this.typeMessage = typeMessage;
        return this;
    }

    public SystemMessage setPathsList(LinkedList<StoredFile> pathsList) {
        this.pathsList = pathsList;
        return this;
    }

    public SystemMessage setConnectionIsAlive(boolean connectionIsAlive) {
        this.connectionIsAlive = connectionIsAlive;
        return this;
    }

    public SystemMessage setCurrentServerPath(String currentServerPath) {
        this.currentServerPath = currentServerPath;
        return this;
    }

    public SystemMessage setCurrentClientPath(String currentClientPath) {
        this.currentClientPath = currentClientPath;
        return this;
    }

    public SystemMessage setRequestedPath(String requestedPath) {
        this.requestedPath = requestedPath;
        return this;
    }

    public SystemMessage setLoginAndPassword(String[] loginAndPassword) {
        this.loginAndPassword = loginAndPassword;
        return this;
    }

    public SystemMessage setAuthorization(boolean authorization) {
        this.authorization = authorization;
        return this;
    }
}
