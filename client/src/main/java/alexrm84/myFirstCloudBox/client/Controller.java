package alexrm84.myFirstCloudBox.client;

import alexrm84.myFirstCloudBox.common.AbstractMessage;
import alexrm84.myFirstCloudBox.common.FileMessage;
import alexrm84.myFirstCloudBox.common.SystemMessage;
import io.netty.channel.ChannelHandlerContext;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import lombok.Data;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.Arrays;
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
    private String username;
    private String currentClientPath, currentServerPath;
    private String rootClientPath, rootServerPath;
//    private String pathClientStorage, pathServerStorage;

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        hbAuthPanel.setVisible(!authenticated);
        hbAuthPanel.setManaged(!authenticated);
        hbControlPanel.setVisible(authenticated);
        hbControlPanel.setManaged(authenticated);
        if (!authenticated) {
            username = "";
            lvClientFilesList.getItems().clear();
            lvServerFilesList.getItems().clear();
        }else {
            work();
        }
    }

    public void work(){
        lvClientFilesList.getItems().clear();
        refreshClientFilesList(currentClientPath);
        lvServerFilesList.getItems().clear();
        requestRefreshServerFilesList(currentServerPath);
        storageNavigation();
    }

    public void authorization() {
//Добавить получение настроек из БД
//        pathClientStorage = "client_storage\\";
        username = "user1";

        rootClientPath = "client_storage\\" + username;
        currentClientPath = rootClientPath;
        rootServerPath = "server_storage\\" + username;
        currentServerPath = rootServerPath;
        setAuthenticated(true);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthenticated(false);
        Network.start();
        Thread thread = new Thread(()->{
            try {
                while (true){
                    AbstractMessage abstractMessage = Network.readObject();
                    if (abstractMessage instanceof FileMessage){
                        FileMessage fileMessage = (FileMessage)abstractMessage;
                        writeFile(fileMessage);
                    }
                    if (abstractMessage instanceof SystemMessage){
                        SystemMessage systemMessage = (SystemMessage)abstractMessage;
                        switch (systemMessage.getTypeMessage()){
                            case "REFRESH" :
                                refreshServerFilesList(systemMessage.getPathsList());
                                break;
                            case "CheckPath":
                                checkServerPathResult(systemMessage);
                                break;
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

//Навигация по листам.

    public void storageNavigation(){
        lvClientFilesList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                String filename = lvClientFilesList.getSelectionModel().getSelectedItem();
                if (!filename.equals("[..]") && filename!=null){
                    tfFilename.setText(filename);
                    tfFilename.selectEnd();
                }
            }
            if (event.getClickCount() == 2) {
                String filename = lvClientFilesList.getSelectionModel().getSelectedItem();
                if (filename == null){return;}
                if (filename.equals("[..]")){
                    currentClientPath = Paths.get(currentClientPath).getParent().toString();
                    refreshClientFilesList(currentClientPath);
                }
                if (Files.isDirectory(Paths.get(currentClientPath+"\\"+filename),LinkOption.NOFOLLOW_LINKS)){
                    currentClientPath += "\\"+filename;
                    refreshClientFilesList(currentClientPath);
                }

            }
        });

        lvServerFilesList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                String filename = lvServerFilesList.getSelectionModel().getSelectedItem();
                if (!filename.equals("[..]") && filename!=null){
                    tfFilename.setText(filename);
                    tfFilename.selectEnd();
                }
            }
            if (event.getClickCount() == 2) {
                String filename = lvServerFilesList.getSelectionModel().getSelectedItem();
                if (filename == null){return;}
                if (filename.equals("[..]")){
                    currentServerPath = Paths.get(currentServerPath).getParent().toString();
                    requestRefreshServerFilesList(currentServerPath);
                    return;
                }
                checkServerPath(currentServerPath+"\\"+filename);
            }
        });
    }

//Обновление клиентского листа
    private void refreshClientFilesList(String directoryName){
        if (Platform.isFxApplicationThread()) {
            refreshCFL(directoryName);
        }else {
            Platform.runLater(() -> {
                refreshCFL(directoryName);
            });
        }
    }

//Обновление клиентского листа 2
    private void refreshCFL(String directoryName){
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

//Формотирование пути для вывода в листы только последней части
    private String getNameFromPath(Path path){
        return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) ? path.getName((path.getNameCount()-1)).toString() : path.getFileName().toString();
    }

//Запрос обновления серверного листа
    public void requestRefreshServerFilesList(String directoryName){
        SystemMessage systemMessage = new SystemMessage();
        systemMessage.setTypeMessage("REFRESH").setPathsList(new LinkedList<>(Arrays.asList(directoryName)));
        if (Network.sendMessage(systemMessage)){
            System.out.println("Отправлено");
        }else {
            System.out.println("Не отправлено");
        }
    }

//Обновление серверного листа
    public void refreshServerFilesList(LinkedList<String> pathList){
        if (Platform.isFxApplicationThread()) {
            lvServerFilesList.getItems().clear();
            if (!currentServerPath.equals(rootServerPath)){
                lvServerFilesList.getItems().add("[..]");
            }
            pathList.forEach(p->lvServerFilesList.getItems().add(getNameFromPath(Paths.get(p))));
        }else {
            Platform.runLater(() -> {
                lvServerFilesList.getItems().clear();
                if (!currentServerPath.equals(rootServerPath)){
                    lvServerFilesList.getItems().add("[..]");
                }
                pathList.forEach(p->lvServerFilesList.getItems().add(getNameFromPath(Paths.get(p))));
            });
        }
    }

//Навигайия по серверному листу (запрос)
    public void checkServerPath(String directoryName){
        SystemMessage systemMessage = new SystemMessage();
        systemMessage.setTypeMessage("CheckPath").setPathsList(new LinkedList<>(Arrays.asList(directoryName)));
        if (Network.sendMessage(systemMessage)){
            System.out.println("Отправлено");
        }else {
            System.out.println("Не отправлено");
        }
    }

//Навигайия по серверному листу (если папка то входим в нее, если файл ничего не делаем)
    public void checkServerPathResult(SystemMessage systemMessage){
        if (systemMessage.isPath()){
            currentServerPath = systemMessage.getCurrentServerPath();
            refreshServerFilesList(systemMessage.getPathsList());
        }
    }

//Отправка файла/каталога, стартовый метод
    public void sendFiles() {
        Path path = Paths.get(currentClientPath + "\\" + tfFilename.getText());
        if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)){
            send(path);
        }else {
            sendCatalog(path);
        }
    }

//Отправка каталога с сроходом по детереву каталога
    private void sendCatalog(Path path) {
        try (DirectoryStream<Path> directoryStream = newDirectoryStream(path)) {
            for (Path p : directoryStream) {
                if (Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)){
                    send(p);
                }else{
                    send(p);
                    sendCatalog(p);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//Непосредственно отправка (путь пересылайемого файла, путь относительно текущего каталога, путь назначения)
    private void send(Path path){
        try {
            if (Network.sendMessage(new FileMessage(path, path.subpath(Paths.get(currentClientPath).getNameCount(),path.getNameCount()).toString(), currentServerPath))){
                System.out.println("Отправлено: " + path);
            }else {
                System.out.println("Не отправлено: " + path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//Запрос загрузки файсла/каталога с сервера
    public void receiveFiles() {
        SystemMessage systemMessage = new SystemMessage();
        systemMessage.setTypeMessage("ReceiveFiles").setCurrentClientPath(currentClientPath).setPathsList(new LinkedList<>(Arrays.asList(currentServerPath + "\\" + tfFilename.getText())));
        if (Network.sendMessage(systemMessage)){
            System.out.println("Отправлено");
        }else {
            System.out.println("Не отправлено");
        }
    }

//Запись файлов
    public void writeFile(FileMessage fileMessage){
        Path path = Paths.get(fileMessage.getCurrentDestinationPath() + "\\" + fileMessage.getFilePath());
        try {
            if (!fileMessage.isFile()){
                Files.createDirectories(path);
            }else {
                Files.createDirectories(path.getParent());
                Files.write(path, fileMessage.getData(), StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        refreshClientFilesList(currentClientPath);
    }
}
