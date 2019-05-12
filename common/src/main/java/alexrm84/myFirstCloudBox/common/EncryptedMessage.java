package alexrm84.myFirstCloudBox.common;

import lombok.Getter;

@Getter
public class EncryptedMessage extends AbstractMessage {
    private byte[] data;

    public EncryptedMessage(byte[] data) {
        this.data = data;
    }
}
