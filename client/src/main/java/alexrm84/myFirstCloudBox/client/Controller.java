package alexrm84.myFirstCloudBox.client;

import alexrm84.myFirstCloudBox.common.AbstractMessage;
import alexrm84.myFirstCloudBox.common.FileMessage;
import alexrm84.myFirstCloudBox.common.SystemMessage;
import javafx.application.Platform;
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
    private String rootClientPath, rootServerPath;
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
            work();
        }

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthenticated(false);
        rootClientPath = "client_storage";
        currentClientPath = rootClientPath;
        rootServerPath = "server_storage";
        currentServerPath = rootServerPath;
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
                    if (abstractMessage instanceof SystemMessage){
                        SystemMessage systemMessage = (SystemMessage)abstractMessage;
                        if (systemMessage.getTypeMessage().equals("REFRESH")){
                            refreshServerFilesList(systemMessage.getPathsList());
                        }
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
        requestRefreshServerFilesList(currentServerPath);


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
                    currentClientPath = Paths.get(currentClientPath).getParent().toString();
                    refreshClientFilesList(currentClientPath);
                }
                if (Files.isDirectory(Paths.get(currentClientPath+"/"+filename),LinkOption.NOFOLLOW_LINKS)){
                    currentClientPath+="/"+filename;
                    refreshClientFilesList(currentClientPath);
                }

            }
        });
    }

    public void refreshClientFilesList(String directoryName){
        if (Platform.isFxApplicationThread()) {
            refreshCFL(directoryName);
        }else {
            Platform.runLater(() -> {
                refreshCFL(directoryName);
            });
        }
    }

    public void refreshCFL(String directoryName){
        lvClientFilesList.getItems().clear();
        if (!currentClientPath.equals(rootClientPath)){
            lvClientFilesList.getItems().add("[..]");
        }
        try {
            Files.list(Paths.get(directoryName)).forEach(o->lvClientFilesList.getItems().add(getNameFromPath(o)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNameFromPath(Path path){
        return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) ? path.getName((path.getNameCount()-1)).toString() : path.getFileName().toString();
    }

    public void requestRefreshServerFilesList(String directoryName){
        LinkedList<String> linkedList = new LinkedList<>();
        linkedList.add(Paths.get(directoryName).toString());
        SystemMessage systemMessage = new SystemMessage();
        systemMessage.setTypeMessage("REFRESH").setPathsList(linkedList);
        if (Network.sendMessage(systemMessage)){
            System.out.println("отправлено");
        }else {
            System.out.println("Не отправлено");
        }
    }

    public void refreshServerFilesList(LinkedList<String> linkedList){
        if (Platform.isFxApplicationThread()) {
            linkedList.forEach(p->lvServerFilesList.getItems().add(getNameFromPath(Paths.get(p))));
        }else {
            Platform.runLater(() -> {
                linkedList.forEach(p->lvServerFilesList.getItems().add(getNameFromPath(Paths.get(p))));
            });
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

    public void sendFile() {
        if (Files.exists(Paths.get(currentClientPath + "/" + tfFilename.getText()))){
            try {
                FileMessage fileMessage = new FileMessage(Paths.get(currentClientPath + "/" + tfFilename.getText()));
                if (Network.sendMessage(fileMessage)){
                    System.out.println("отправлено");
                }else {
                    System.out.println("Не отправлено");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    public void receiveFile(ActionEvent actionEvent) {
        refreshClientFilesList(currentClientPath);
    }
}
