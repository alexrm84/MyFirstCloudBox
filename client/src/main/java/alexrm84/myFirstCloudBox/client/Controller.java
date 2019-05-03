package alexrm84.myFirstCloudBox.client;

import alexrm84.myFirstCloudBox.common.AbstractMessage;
import alexrm84.myFirstCloudBox.common.Command;
import alexrm84.myFirstCloudBox.common.FileMessage;
import alexrm84.myFirstCloudBox.common.SystemMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Data;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ResourceBundle;


@Data
public class Controller implements Initializable {

    @FXML
    HBox hbControlPanel, hbListPanel;

    @FXML
    VBox vbAuthPanel;

    @FXML
    TextField tfLogin, tfFilename;

    @FXML
    PasswordField pfPassword;

    @FXML
    ListView<String> lvServerFilesList, lvClientFilesList;

    private boolean authenticated;
    private String username;
    private String currentClientPath, currentServerPath;
    private String rootClientPath, rootServerPath;
    private String currentSelectionInListView;
    private SelectedListView selectedListView;

    private enum SelectedListView {ServerList, ClientList}

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        vbAuthPanel.setVisible(!authenticated);
        vbAuthPanel.setManaged(!authenticated);
        hbListPanel.setVisible(authenticated);
        hbListPanel.setManaged(authenticated);
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

    public void requestAuthorization(){
        System.out.println("нажата кнопка");
        System.out.println(tfLogin.getText() + " " + pfPassword.getText());
        if (!tfLogin.getText().equals("") && !pfPassword.getText().equals("")){
            Network.sendMessage(new SystemMessage().setTypeMessage(Command.Authorization).setLoginAndPassword(new String[]{tfLogin.getText(), pfPassword.getText()}));

        }else {
            new Alert(Alert.AlertType.CONFIRMATION, "Enter login and password", ButtonType.OK).showAndWait();
        }
    }

    public void authorization(SystemMessage systemMessage) {
        if (systemMessage.isAuthorization()) {
            username = systemMessage.getLoginAndPassword()[0];
            rootClientPath = "client_storage\\" + username;
            currentClientPath = rootClientPath;
            rootServerPath = systemMessage.getCurrentServerPath();
            currentServerPath = rootServerPath;
            setAuthenticated(true);
        }else {
            new Alert(Alert.AlertType.CONFIRMATION, "User not found", ButtonType.OK);
            tfLogin.clear();
            pfPassword.clear();
        }
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
                            case Authorization:
                                authorization(systemMessage);
                                break;
                            case Refresh:
                                refreshServerFilesList(systemMessage.getPathsList());
                                break;
                            case CheckPath:
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
                currentSelectionInListView = filename;
                selectedListView = SelectedListView.ClientList;
            }
            if (event.getClickCount() == 2) {
                String filename = lvClientFilesList.getSelectionModel().getSelectedItem();
                currentSelectionInListView = filename;
                selectedListView = SelectedListView.ClientList;
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
                currentSelectionInListView = filename;
                selectedListView = SelectedListView.ServerList;
            }
            if (event.getClickCount() == 2) {
                String filename = lvServerFilesList.getSelectionModel().getSelectedItem();
                currentSelectionInListView = filename;
                selectedListView = SelectedListView.ServerList;
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
        updateGUI(()->{
            lvClientFilesList.getItems().clear();
            if (!currentClientPath.equals(rootClientPath)){
                lvClientFilesList.getItems().add("[..]");
            }
            try {
                Files.list(Paths.get(directoryName)).forEach(o->lvClientFilesList.getItems().add(getNameFromPath(o)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

//Формотирование пути для вывода в листы только последней части
    private String getNameFromPath(Path path){
        return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) ? path.getName((path.getNameCount()-1)).toString() : path.getFileName().toString();
    }

//Запрос обновления серверного листа
    private void requestRefreshServerFilesList(String directoryName){
        SystemMessage systemMessage = new SystemMessage();
        systemMessage.setTypeMessage(Command.Refresh).setRequestedPath(directoryName);
        if (Network.sendMessage(systemMessage)){
            System.out.println("Отправлено");
        }else {
            System.out.println("Не отправлено");
        }
    }

//Обновление серверного листа
    private void refreshServerFilesList(LinkedList<String> pathList){
        updateGUI(()->{
            lvServerFilesList.getItems().clear();
            if (!currentServerPath.equals(rootServerPath)){
                lvServerFilesList.getItems().add("[..]");
            }
            pathList.forEach(p->lvServerFilesList.getItems().add(getNameFromPath(Paths.get(p))));
        });
    }

//Навигайия по серверному листу (запрос)
    private void checkServerPath(String directoryName){
        SystemMessage systemMessage = new SystemMessage();
        systemMessage.setTypeMessage(Command.CheckPath).setRequestedPath(directoryName);
        if (Network.sendMessage(systemMessage)){
            System.out.println("Отправлено");
        }else {
            System.out.println("Не отправлено");
        }
    }

//Навигайия по серверному листу (если папка то входим в нее, если файл ничего не делаем)
    private void checkServerPathResult(SystemMessage systemMessage){
        if (Files.isDirectory(Paths.get(systemMessage.getRequestedPath()))){
            currentServerPath = systemMessage.getCurrentServerPath();
            refreshServerFilesList(systemMessage.getPathsList());
        }
    }

//Отправка файла/каталога, стартовый метод
    public void sendFiles(){
        Path path = Paths.get(currentClientPath + "\\" + currentSelectionInListView);
        try {
            Files.walk(path).sorted(Comparator.naturalOrder()).forEach(p -> send(p));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//Непосредственно отправка (путь пересылайемого файла, путь назначения, данные (если есть), файл/каталог)
    private void send(Path path){
        byte[] data = new byte[1024*1024];
        FileMessage fileMessage = new FileMessage(path.subpath(Paths.get(currentClientPath).getNameCount(),path.getNameCount()).toString(), currentServerPath, data, true);
        if (Files.isDirectory(path)){
            Network.sendMessage(new FileMessage(path.subpath(Paths.get(currentClientPath).getNameCount(),path.getNameCount()).toString(), currentServerPath, null, false));
        }else {
            try {
                FileInputStream fis = new FileInputStream(path.toFile());
                while (fis.available() > 0) {
                    int temp = fis.read(data);
                    if (temp < 1024 * 1024) {
                        fileMessage.setData(Arrays.copyOfRange(data, 0, temp));
                    }
                    Network.sendMessage(fileMessage);
                    fileMessage.setNewFile(false);
                }
                fis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

//Запрос загрузки файсла/каталога с сервера
    public void receiveFiles() {
        if (Network.sendMessage(new SystemMessage()
                .setTypeMessage(Command.ReceiveFiles)
                .setCurrentClientPath(currentClientPath)
                .setRequestedPath(currentServerPath + "\\" + currentSelectionInListView))){
            System.out.println("Отправлено");
        }else {
            System.out.println("Не отправлено");
        }
    }

//Запись файлов
    private void writeFile(FileMessage fileMessage){
        Path path = Paths.get(fileMessage.getDestinationPath() + "\\" + fileMessage.getFilePath());
        try {
            if (fileMessage.isFile()) {
                if (fileMessage.isNewFile()) {
                    Files.write(path, fileMessage.getData(), StandardOpenOption.CREATE);
                }else {
                    Files.write(path, fileMessage.getData(), StandardOpenOption.APPEND);
                }
            }else {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        refreshClientFilesList(currentClientPath);
    }
//Удаление файлов
    public void deleteFiles() {
        if (selectedListView.equals(SelectedListView.ClientList)){
            Path path = Paths.get(currentClientPath + "\\" + currentSelectionInListView);
            try {
                Files.walk(path).sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            refreshClientFilesList(currentClientPath);
        }else {
            if (Network.sendMessage(new SystemMessage()
                    .setTypeMessage(Command.DeleteFiles)
                    .setRequestedPath(currentServerPath + "\\" + currentSelectionInListView))){
                System.out.println("Отправлено" );
            }else {
                System.out.println("Не отправлено");
            }
        }
    }

    private void updateGUI(Runnable r){
        if (Platform.isFxApplicationThread()){
            r.run();
        }else {
            Platform.runLater(r);
        }
    }
}
