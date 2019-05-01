package alexrm84.myFirstCloudBox.common;

import lombok.Data;

@Data
public class FileMessage extends AbstractMessage {
    private boolean newFile;
    private String filePath;
    private byte[] data;
    private String destinationPath;

    public FileMessage(String filePath, String destinationPath, byte[] data){
        this.newFile = true;
        this.filePath = filePath;
        this.destinationPath = destinationPath;
        this.data = data;
    }
}
