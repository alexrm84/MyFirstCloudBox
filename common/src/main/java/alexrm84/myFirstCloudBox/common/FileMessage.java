package alexrm84.myFirstCloudBox.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMessage extends AbstractMessage {
    private String filename;
    private byte[] data;

    public FileMessage(Path path) throws IOException {
        this.filename = path.getFileName().toString();
        this.data = Files.readAllBytes(path);
    }
}
