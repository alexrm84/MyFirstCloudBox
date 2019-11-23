package alexrm84.myFirstCloudBox.common;

import lombok.Data;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.DecimalFormat;

@Data
public class StoredFile implements Serializable {
    private String name;
    private String path;
    private String size;
    private boolean isFile;

    public StoredFile(){
        this.name = "[..]";
        this.size = "";
    }

    public StoredFile(Path path) throws IOException {
        this.path = path.toString();
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            this.name = path.getName((path.getNameCount()-1)).toString();
            this.isFile = false;
            this.size = "";
        }else {
            this.name = path.getFileName().toString();
            this.isFile = true;
            this.size = new String(new DecimalFormat("#0.00").format((Files.size(path) / (1024*1024.2f))) + " Mb");
        }
    }
}
