package alexrm84.myFirstCloudBox.common;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
public class FileMessage extends AbstractMessage {
    private String filename;
    private Path path;
    private byte[] data;

    public FileMessage(Path path) throws IOException {
        this.filename = path.getFileName().toString();
        this.path = path;
        this.data = Files.readAllBytes(path);
    }
}
