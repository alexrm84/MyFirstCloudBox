package alexrm84.myFirstCloudBox.common;

import javafx.beans.property.*;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.DecimalFormat;

@Data
public class StoredFile {
    private String name;
    private String path;
    private String size;
    private boolean ifFile;

    public StoredFile(){
        this.name = "[..]";
        this.size = "";
    }

    public StoredFile(Path path) throws IOException {
        this.path = path.toString();
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            this.name = path.getName((path.getNameCount()-1)).toString();
            this.ifFile = false;
            this.size = "";
        }else {
            this.name = path.getFileName().toString();
            this.ifFile = true;
            this.size = new String(new DecimalFormat("#0.00").format((Files.size(path) / (1024*1024.2f))) + " Mb");
        }
    }
}
