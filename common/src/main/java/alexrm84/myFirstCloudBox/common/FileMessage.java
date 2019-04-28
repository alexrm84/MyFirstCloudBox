package alexrm84.myFirstCloudBox.common;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
public class FileMessage extends AbstractMessage {
    private String filePath;
    private byte[] data;
    private String filename;
    private String destinationPath;

    public FileMessage(Path path, String filePath, String destinationPath) throws IOException {
        this.filePath = filePath;
        this.destinationPath = destinationPath;
        if (Files.isRegularFile(path)){
            this.filename = path.getFileName().toString();
            this.data = Files.readAllBytes(path);
        }
    }
}
