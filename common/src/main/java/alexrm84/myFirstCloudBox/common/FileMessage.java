package alexrm84.myFirstCloudBox.common;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
public class FileMessage extends AbstractMessage {
    private boolean isFile;
    private String filePath;
    private byte[] data;
    private String filename;
    private String currentDestinationPath;

    public FileMessage(Path path, String filePath, String currentDestinationPath) throws IOException {
        this.filePath = filePath;
        this.currentDestinationPath = currentDestinationPath;
        if (isFile = Files.isRegularFile(path)){
            this.filename = path.getFileName().toString();
            this.data = Files.readAllBytes(path);
        }
    }
}
