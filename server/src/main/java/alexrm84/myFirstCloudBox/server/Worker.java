package alexrm84.myFirstCloudBox.server;

import alexrm84.myFirstCloudBox.common.Command;
import alexrm84.myFirstCloudBox.common.CryptoUtil;
import alexrm84.myFirstCloudBox.common.FileMessage;
import alexrm84.myFirstCloudBox.common.SystemMessage;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

public class Worker {
    private String rootStorage;
    private String userStorage;
    private SQLHandler sqlHandler;
//    CryptoUtil cryptoUtil;
    private static final Logger logger = LogManager.getLogger(Worker.class);

    public Worker() {
        sqlHandler = new SQLHandler();
        rootStorage = "server_storage\\";
    }

//Авторизация.
    public boolean authorization(ChannelHandlerContext ctx, SystemMessage systemMessage){
        sqlHandler.connect();
        if (sqlHandler.checkLoginAndPassword(systemMessage.getLoginAndPassword()[0], systemMessage.getLoginAndPassword()[1])){
            userStorage = rootStorage + systemMessage.getLoginAndPassword()[0];
            ctx.writeAndFlush(systemMessage.setAuthorization(true).setCurrentServerPath(userStorage));
            ctx.writeAndFlush(systemMessage.setTypeMessage(Command.Refresh)
                    .setPathsList(refreshFiles(userStorage))
                    .setCurrentServerPath(userStorage));
            sqlHandler.disconnect();
            return true;
        }
        ctx.writeAndFlush(systemMessage.setAuthorization(false));
        sqlHandler.disconnect();
        return false;
    }

    public boolean createUser(ChannelHandlerContext ctx, SystemMessage systemMessage){
        sqlHandler.connect();
        if (sqlHandler.createUser(systemMessage.getLoginAndPassword()[0], systemMessage.getLoginAndPassword()[1])){
            userStorage = rootStorage + systemMessage.getLoginAndPassword()[0];
            try {
                Files.createDirectories(Paths.get(userStorage));
            } catch (IOException e) {
                logger.log(Level.ERROR, "Create directory error: ", e);
            }
            ctx.writeAndFlush(systemMessage.setAuthorization(true).setCurrentServerPath(userStorage));
            ctx.writeAndFlush(systemMessage.setTypeMessage(Command.Refresh)
                    .setPathsList(refreshFiles(userStorage))
                    .setCurrentServerPath(userStorage));
            sqlHandler.disconnect();
            return true;
        }
        ctx.writeAndFlush(systemMessage.setAuthorization(false));
        sqlHandler.disconnect();
        return false;
    }

//Создание списка файлов на запрос обновления
    public LinkedList<String> refreshFiles(String path){
        LinkedList<String> filesList = new LinkedList<>();
        try {
            Files.list(Paths.get(path)).forEach(p->filesList.add(p.toString()));
        } catch (IOException e) {
            logger.log(Level.ERROR, "List update error: ", e);
        }
        System.out.println(filesList);
        return filesList;
    }

//формирование списка файлов при навигации по листу
    public void checkPath(ChannelHandlerContext ctx, SystemMessage systemMessage){
        if (Files.isDirectory(Paths.get(systemMessage.getRequestedPath()))){
            ctx.writeAndFlush(systemMessage.setPathsList(refreshFiles(systemMessage.getRequestedPath())).setCurrentServerPath(systemMessage.getRequestedPath()));
        }
    }

//Запись файлов
    public void writeFile(ChannelHandlerContext ctx, FileMessage fileMessage){
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
            logger.log(Level.ERROR, "File writing error:  ", e);
        }
        ctx.writeAndFlush(new SystemMessage().setTypeMessage(Command.Refresh).setPathsList(refreshFiles(fileMessage.getDestinationPath())));
    }

//Отправка файла/каталога с проходом по детереву каталога
    public void sendFiles(ChannelHandlerContext ctx, SystemMessage systemMessage){
        try {
            Files.walk(Paths.get(systemMessage.getRequestedPath())).sorted(Comparator.naturalOrder()).forEach(path -> send(ctx, systemMessage, path));
        } catch (IOException e) {
            logger.log(Level.ERROR, "Error building the list of send files: ", e);
        }
    }

//Непосредственно отправка (путь пересылайемого файла, путь назначения, данные (если есть), файл/каталог)
    private void send(ChannelHandlerContext ctx, SystemMessage systemMessage, Path path){
        byte[] data = new byte[1024*1024];
        FileMessage fileMessage = new FileMessage(path.subpath(Paths.get(systemMessage.getRequestedPath()).getNameCount()-1,path.getNameCount()).toString(), systemMessage.getCurrentClientPath(), data, true);
        if (Files.isDirectory(path)){
            fileMessage.setData(null);
            fileMessage.setFile(false);
            ctx.writeAndFlush(fileMessage);
        }else {
            try {
                FileInputStream fis = new FileInputStream(path.toFile());
                while (fis.available() > 0) {
                    int temp = fis.read(data);
                    if (temp < 1024 * 1024) {
                        fileMessage.setData(Arrays.copyOfRange(data, 0, temp));
                    }
                    ctx.writeAndFlush(fileMessage);
                    fileMessage.setNewFile(false);
                }
                fis.close();
                logger.log(Level.INFO, "File upload: " + path);
            } catch (Exception e) {
                logger.log(Level.ERROR, "File upload error: ", e);
            }
        }
    }

//Удаление файлов
    public void deleteFiles(ChannelHandlerContext ctx, SystemMessage systemMessage) {
        Path path = Paths.get(systemMessage.getRequestedPath());
        try {
            Files.walk(path).sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    logger.log(Level.ERROR, "File delete error: ", e);
                }
            });
        } catch (IOException e) {
            logger.log(Level.ERROR, "Error building the list of delete files: ", e);
        }
        ctx.writeAndFlush(new SystemMessage().setTypeMessage(Command.Refresh).setPathsList(refreshFiles(path.getParent().toString())));
    }
}
