package alexrm84.myFirstCloudBox.client;

import alexrm84.myFirstCloudBox.common.AbstractMessage;
import alexrm84.myFirstCloudBox.common.FileMessage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.LinkedList;
import java.util.ResourceBundle;

import static java.nio.file.Files.newDirectoryStream;

@Data
public class Controller implements Initializable {

    @FXML
    HBox hbAuthPanel, hbControlPanel;

    @FXML
    TextField tfLogin, pfPassword, tfFilename;

    @FXML
    ListView<String> lvServerFilesList, lvClientFilesList;

    private boolean authenticated;
    private String nickname;
    private String currentClientPath, currentServerPath;

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        hbAuthPanel.setVisible(!authenticated);
        hbAuthPanel.setManaged(!authenticated);
        hbControlPanel.setVisible(authenticated);
        hbControlPanel.setManaged(authenticated);
        if (!authenticated) {
            nickname = "";
            lvClientFilesList.getItems().clear();
            lvServerFilesList.getItems().clear();
        }else {
            refreshClientFilesList(currentClientPath);
            work();
        }

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthenticated(false);
        currentClientPath = "client_storage";
        Network.start();
        Thread thread = new Thread(()->{
            try {
                while (true){
                    AbstractMessage abstractMessage = Network.readObject();
                    if (abstractMessage instanceof FileMessage){
                        FileMessage fileMessage = (FileMessage)abstractMessage;
                        Files.write(Paths.get("client_storage/" + fileMessage.getFilename()),fileMessage.getData(), StandardOpenOption.CREATE);
//                        refreshFilesList("client_storage");
                    }
                }
            }catch (ClassNotFoundException | IOException e){
                e.printStackTrace();
            }finally {
                Network.stop();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void work(){
        lvClientFilesList.getItems().clear();
        refreshClientFilesList(currentClientPath);
        lvClientFilesList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                String filename = lvClientFilesList.getSelectionModel().getSelectedItem();
                tfFilename.setText(filename);
                tfFilename.requestFocus();
                tfFilename.selectEnd();
            }
            if (event.getClickCount() == 2) {
                String filename = lvClientFilesList.getSelectionModel().getSelectedItem();
                if (filename.equals("[..]")){
                    refreshClientFilesList(Paths.get(currentClientPath).getParent().toString());
                }
                if (Files.isDirectory(Paths.get(currentClientPath+"/"+filename),LinkOption.NOFOLLOW_LINKS)){
                    currentClientPath+="/"+filename;
                    refreshClientFilesList(currentClientPath);
                }

            }
        });
    }

//    public void refreshLocalFilesList() {
//        if (Platform.isFxApplicationThread()) {
//            try {
//                lvClientFilesList.getItems().clear();
//                Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> lvClientFilesList.getItems().add(o));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        } else {
//            Platform.runLater(() -> {
//                try {
//                    lvClientFilesList.getItems().clear();
//                    Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> lvClientFilesList.getItems().add(o));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            });
//        }
//    }

    public void refreshClientFilesList(String directoryName){
        lvClientFilesList.getItems().clear();
        if (!currentClientPath.equals("client_storage")){
            lvClientFilesList.getItems().add("[..]");
        }
        try {
            for (Path p:newDirectoryStream(Paths.get(directoryName))) {
                if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)){
                    lvClientFilesList.getItems().add(p.getName(p.getNameCount()-1).toString());
                }else {
                    lvClientFilesList.getItems().add(p.getFileName().toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getDirectoryContentsToCopy(String directoryName, LinkedList<File> files) {
        File directory = new File(directoryName);
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                files.add(file);
            } else if (file.isDirectory()) {
                getDirectoryContentsToCopy(file.getAbsolutePath(), files);
            }
        }
    }


    public void authorization(ActionEvent actionEvent) {
        setAuthenticated(true);
    }

    public void send(ActionEvent actionEvent) {
        try {
            FileMessage fileMessage = new FileMessage(Paths.get("client_storage/" + tfFilename.getText()));
            if (Network.sendMessage(fileMessage)){
                System.out.println("отправлено");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receive(ActionEvent actionEvent) {
        refreshClientFilesList(currentClientPath);
    }
}
