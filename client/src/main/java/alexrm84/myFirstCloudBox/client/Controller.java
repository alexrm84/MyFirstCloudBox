package alexrm84.myFirstCloudBox.client;

import alexrm84.myFirstCloudBox.common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Data;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;


@Data
public class Controller implements Initializable {

    @FXML
    TableView tvClient, tvServer;

    @FXML
    TableColumn<StoredFile, String> colNameClient, colNameServer;

    @FXML
    TableColumn<StoredFile, Long> colSizeClient, colSizeServer;

    @FXML
    HBox hbControlPanel, hbListPanel, hbTablePanel;

    @FXML
    VBox vbAuthPanel;

    @FXML
    TextField tfLogin, tfFilename;

    @FXML
    PasswordField pfPassword;

    @FXML
    ListView<String> lvServerFilesList, lvClientFilesList;

    private static final Logger logger = LogManager.getLogger(Controller.class);

    private CryptoUtil cryptoUtil;
    private Serialization serialization;

    private boolean canSend;
    private boolean authenticated;
    private boolean keyExchange;
    private String username;
    private String currentClientPath, currentServerPath;
    private String rootClientPath, rootServerPath;
    private StoredFile currentSelectionObj;
    private SelectedListView selectedList;
    StoredFile parentDirectory;

    private enum SelectedListView {ServerList, ClientList}

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        vbAuthPanel.setVisible(!authenticated);
        vbAuthPanel.setManaged(!authenticated);

//        hbListPanel.setVisible(authenticated);
//        hbListPanel.setManaged(authenticated);
        hbTablePanel.setVisible(authenticated);
        hbTablePanel.setManaged(authenticated);

        hbControlPanel.setVisible(authenticated);
        hbControlPanel.setManaged(authenticated);
        if (authenticated) {
            refreshData();
        }
    }

    public void refreshData(){
//        lvClientFilesList.getItems().clear();
        tvClient.getItems().clear();
        refreshClientFilesList(currentClientPath);
//        lvServerFilesList.getItems().clear();
        tvServer.getItems().clear();
        refreshServerFilesList(currentServerPath);
        storageNavigation();
    }

//Создание нового пользователя
    public void createUser() {
        if (!tfLogin.getText().trim().equals("") && !pfPassword.getText().trim().equals("")){
            System.out.println();
            encryption(new SystemMessage()
                    .setTypeMessage(Command.CreateUser)
                    .setLoginAndPassword(new String[]{tfLogin.getText(), pfPassword.getText()}));
        }else {
            new Alert(Alert.AlertType.CONFIRMATION, "Enter login and password", ButtonType.OK).showAndWait();
        }
    }

//Запрос авторизации.
    public void requestAuthorization(){
        if (!tfLogin.getText().trim().equals("") && !pfPassword.getText().trim().equals("")){
            encryption(new SystemMessage()
                    .setTypeMessage(Command.Authorization)
                    .setLoginAndPassword(new String[]{tfLogin.getText(), pfPassword.getText()}));
        }else {
            new Alert(Alert.AlertType.CONFIRMATION, "Enter login and password", ButtonType.OK).showAndWait();
        }
    }

//Авторизация.
    public void authorization(SystemMessage systemMessage) {
        if (systemMessage.isAuthorization()) {
            username = tfLogin.getText();
            rootClientPath = "client_storage\\" + username;
            if (systemMessage.getTypeMessage().equals(Command.CreateUser)){
                try {
                    Files.createDirectories(Paths.get(rootClientPath));
                } catch (IOException e) {
                    logger.log(Level.ERROR, "Create directory error: ", e);
                }
            }
            currentClientPath = rootClientPath;
            rootServerPath = systemMessage.getCurrentServerPath();
            currentServerPath = rootServerPath;
            setAuthenticated(true);
        }else {
            new Alert(Alert.AlertType.CONFIRMATION, "User not found", ButtonType.OK).showAndWait();
            tfLogin.clear();
            pfPassword.clear();
        }
    }

//Получение открытого ключа RSA, Инициализация AES, после авторизации кодирование сообщения.
    private void encryption(AbstractMessage abstractMessage){
        if (keyExchange && abstractMessage instanceof SystemMessage) {
            SystemMessage systemMessage = (SystemMessage)abstractMessage;
            if (systemMessage.getTypeMessage() == null) {
                Network.sendMessage(new SystemMessage().setTypeMessage(Command.PublicKeyRSA));
                logger.log(Level.INFO, "RSA public key request sent");
                return;
            }
            if (systemMessage.getTypeMessage().equals(Command.PublicKeyRSA)) {
                cryptoUtil.setKeyPairRSA(new KeyPair(systemMessage.getPublicKeyRSA(), null));
                logger.log(Level.INFO, "RSA received");
                cryptoUtil.initAES();
                Network.sendMessage(new SystemMessage().setTypeMessage(Command.SecretKeyAES).setSecretKeyAES(cryptoUtil.encryptRSA(cryptoUtil.getSecretKeyAES())));
                logger.log(Level.INFO, "AES sent");
                return;
            }
            if (systemMessage.getTypeMessage().equals(Command.SecretKeyAES)) {
                keyExchange = false;
                logger.log(Level.INFO, "AES server received");
                return;
            }
        }
        if (!keyExchange){
//            try {
                byte[] data = serialization.serialize(abstractMessage);
                data = cryptoUtil.encryptAES(data);
                Network.sendMessage(new EncryptedMessage(data));
//            } catch (IOException e) {
//                logger.log(Level.ERROR, "Data serialization error: ", e);
//            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthenticated(false);
        canSend = true;
        keyExchange = true;
        tvClient.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
        colNameClient.setMaxWidth( 1f * Integer.MAX_VALUE * 85 );
        colSizeClient.setMaxWidth( 1f * Integer.MAX_VALUE * 15 );
        tvServer.setColumnResizePolicy( TableView.CONSTRAINED_RESIZE_POLICY );
        colNameServer.setMaxWidth( 1f * Integer.MAX_VALUE * 85 );
        colSizeServer.setMaxWidth( 1f * Integer.MAX_VALUE * 15 );
        colNameClient.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSizeClient.setCellValueFactory(new PropertyValueFactory<>("size"));
        colNameServer.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSizeServer.setCellValueFactory(new PropertyValueFactory<>("size"));
        parentDirectory = new StoredFile();
        cryptoUtil = new CryptoUtil();
        serialization = new Serialization();
        Network.start();
        encryption(new SystemMessage());
        clientHandler();
    }

    private void clientHandler(){
        Thread thread = new Thread(()->{
            try {
                while (true){
                    AbstractMessage abstractMessage = Network.readObject();
                    if (abstractMessage instanceof EncryptedMessage){
                        EncryptedMessage em = (EncryptedMessage)abstractMessage;
                        byte[] data = em.getData();
                        data = cryptoUtil.decryptAES(data);
                        Object obj = serialization.deserialize(data);
                        if (obj instanceof AbstractMessage){
                            abstractMessage = (AbstractMessage)obj;
                        }
                    }
                    if (abstractMessage instanceof FileMessage){
                        FileMessage fileMessage = (FileMessage)abstractMessage;
                        writeFile(fileMessage);
                    }
                    if (abstractMessage instanceof SystemMessage){
                        SystemMessage systemMessage = (SystemMessage)abstractMessage;
                        switch (systemMessage.getTypeMessage()){
                            case CanSend:
                                canSend = true;
                                break;
                            case SecretKeyAES:
                            case PublicKeyRSA:
                                encryption(systemMessage);
                                break;
                            case CreateUser:
                            case Authorization:
                                authorization(systemMessage);
                                break;
                            case Refresh:
                                refreshServerFilesList(systemMessage.getPathsList());
                                break;
                        }
                    }
                }
            }catch (ClassNotFoundException | IOException e){
                logger.log(Level.ERROR, "Error retrieving data: ", e);
            }finally {
                Network.stop();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

//Навигация по листам.
    public void storageNavigation(){
        tvClient.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                currentSelectionObj = (StoredFile) tvClient.getSelectionModel().getSelectedItem();
                selectedList = SelectedListView.ClientList;
                return;
            }
            if (event.getClickCount() == 2) {
                currentSelectionObj = (StoredFile) tvClient.getSelectionModel().getSelectedItem();
                selectedList = SelectedListView.ClientList;
                if (currentSelectionObj == null){return;}
                if (currentSelectionObj.getName().equals(parentDirectory.getName())){
                    currentClientPath = Paths.get(currentClientPath).getParent().toString();
                    refreshClientFilesList(currentClientPath);
                    return;
                }
                if (!currentSelectionObj.isFile()){
                    currentClientPath = currentSelectionObj.getPath();
                    refreshClientFilesList(currentClientPath);
                }
            }
        });

        tvServer.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                currentSelectionObj = (StoredFile) tvServer.getSelectionModel().getSelectedItem();
                selectedList = SelectedListView.ServerList;
                return;
            }
            if (event.getClickCount() == 2) {
                currentSelectionObj = (StoredFile) tvServer.getSelectionModel().getSelectedItem();
                selectedList = SelectedListView.ServerList;
                if (currentSelectionObj == null){return;}
                if (currentSelectionObj.getName().equals(parentDirectory.getName())){
                    currentServerPath = Paths.get(currentServerPath).getParent().toString();
                    refreshServerFilesList(currentServerPath);
                    return;
                }
                if (!currentSelectionObj.isFile()){
                    currentServerPath = currentSelectionObj.getPath();
                    refreshServerFilesList(currentServerPath);
                }
            }
        });
    }

//Обновление клиентского листа
    private void refreshClientFilesList(String directoryName){
        updateGUI(()->{
            Path path = Paths.get(directoryName);
            tvClient.getItems().clear();
            if (!currentClientPath.equals(rootClientPath)){
                tvClient.getItems().add(parentDirectory);
            }
            try {
                Files.list(path).forEach(o -> {
                    try {
                        tvClient.getItems().add(new StoredFile(o));
                    } catch (IOException e) {
                        logger.log(Level.ERROR, "Create storedFile error: ", e);
                    }
                });
            } catch (IOException e) {
                logger.log(Level.ERROR, "List update error: ", e);
            }
        });
    }

//Запрос обновления серверного листа
    private void refreshServerFilesList(String directoryName){
        encryption(new SystemMessage().setTypeMessage(Command.Refresh).setRequestedPath(directoryName));
    }

//Обновление серверного листа
    private void refreshServerFilesList(LinkedList<StoredFile> pathList){
        updateGUI(()->{
            tvServer.getItems().clear();
            ObservableList<StoredFile> observableList = FXCollections.observableArrayList(pathList);
            if (!currentServerPath.equals(rootServerPath)){
                observableList.add(0, parentDirectory);
            }
            tvServer.setItems(observableList);
        });
    }

//Отправка файла/каталога, стартовый метод
    public void sendFiles(){
        Path path = Paths.get(currentSelectionObj.getPath());
        try {
            Files.walk(path).sorted(Comparator.naturalOrder()).forEach(p -> send(p));
        } catch (IOException e) {
            logger.log(Level.ERROR, "Error building the list of send files: ", e);
        }
    }

//Непосредственно отправка (путь пересылайемого файла, путь назначения, данные (если есть), файл/каталог)
    private void send(Path path){
        byte[] data = new byte[10 *1024*1024];
        FileMessage fileMessage = new FileMessage(path.subpath(Paths.get(currentClientPath).getNameCount(),path.getNameCount()).toString(), currentServerPath, data, true);
        if (Files.isDirectory(path)){
            encryption(new FileMessage(path.subpath(Paths.get(currentClientPath).getNameCount(),path.getNameCount()).toString(), currentServerPath, null, false));
        }else {
            try {
                FileInputStream fis = new FileInputStream(path.toFile());
                while (fis.available() > 0) {
                    int temp = fis.read(data);
                    if (temp <10 * 1024 * 1024) {
                        fileMessage.setData(Arrays.copyOfRange(data, 0, temp));
                    }
                    while (!canSend){
                        TimeUnit.MILLISECONDS.sleep(100);
                    }
                    encryption(fileMessage);
                    canSend = false;
                    fileMessage.setNewFile(false);
                }
                fis.close();
            } catch (Exception e) {
                logger.log(Level.ERROR, "File upload error: ", e);
            }
        }
    }

//Запрос загрузки файсла/каталога с сервера
    public void receiveFiles() {
        encryption(new SystemMessage()
                .setTypeMessage(Command.ReceiveFiles)
                .setCurrentClientPath(currentClientPath)
                .setRequestedPath(currentSelectionObj.getPath()));
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
            Network.sendMessage(new SystemMessage().setTypeMessage(Command.CanSend));
        } catch (IOException e) {
            logger.log(Level.ERROR, "File writing error: ", e);
        }
        refreshClientFilesList(currentClientPath);
    }

//Удаление файлов
    public void deleteFiles() {
        if (selectedList.equals(SelectedListView.ClientList)){
            Path path = Paths.get(currentSelectionObj.getPath());
            try {
                Files.walk(path).sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                        logger.log(Level.INFO, "File delete: " + p);
                    } catch (IOException e) {
                        logger.log(Level.ERROR, "File delete error: ", e);
                    }
                });
            } catch (IOException e) {
                logger.log(Level.ERROR, "Error building the list of delete files: ", e);
            }
            refreshClientFilesList(currentClientPath);
        }else {
            encryption(new SystemMessage()
                    .setTypeMessage(Command.DeleteFiles)
                    .setRequestedPath(currentSelectionObj.getPath()));
        }
    }

    public void exit() {
        encryption(new SystemMessage().setTypeMessage(Command.Exit));
        Network.stop();
    }



    private void updateGUI(Runnable r){
        if (Platform.isFxApplicationThread()){
            r.run();
        }else {
            Platform.runLater(r);
        }
    }
}
