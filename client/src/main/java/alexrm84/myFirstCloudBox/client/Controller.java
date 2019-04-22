package alexrm84.myFirstCloudBox.client;

import alexrm84.myFirstCloudBox.common.AbstractMessage;
import alexrm84.myFirstCloudBox.common.FileMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import lombok.Data;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;

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

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        hbAuthPanel.setVisible(!authenticated);
        hbAuthPanel.setManaged(!authenticated);
        hbControlPanel.setVisible(authenticated);
        hbControlPanel.setManaged(authenticated);
        if (!authenticated) {
            nickname = "";
        }
        lvClientFilesList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String filename = lvClientFilesList.getSelectionModel().getSelectedItem();
                tfFilename.setText(filename);
                tfFilename.requestFocus();
                tfFilename.selectEnd();
            }
        });
        lvServerFilesList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String filename = lvServerFilesList.getSelectionModel().getSelectedItem();
                tfFilename.setText(filename);
                tfFilename.requestFocus();
                tfFilename.selectEnd();
            }
        });
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
                        Files.write(Paths.get("client_storage/" + fileMessage.getFilename()),fileMessage.getData(), StandardOpenOption.CREATE);
                        refreshLocalFilesList();
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
        refreshLocalFilesList();
    }

    public void refreshLocalFilesList() {
        if (Platform.isFxApplicationThread()) {
            try {
                lvClientFilesList.getItems().clear();
                Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> lvClientFilesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Platform.runLater(() -> {
                try {
                    lvClientFilesList.getItems().clear();
                    Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> lvClientFilesList.getItems().add(o));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
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
        refreshLocalFilesList();
    }
}
