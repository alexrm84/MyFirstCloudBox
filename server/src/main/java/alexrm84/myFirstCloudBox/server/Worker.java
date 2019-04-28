package alexrm84.myFirstCloudBox.server;

import alexrm84.myFirstCloudBox.common.FileMessage;
import alexrm84.myFirstCloudBox.common.SystemMessage;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedList;

import static java.nio.file.Files.newDirectoryStream;

public class Worker {
    private ChannelHandlerContext ctx;

    public Worker(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    //Создание списка файлов на запрос обновления
    public void refreshFiles(SystemMessage systemMessage){
        LinkedList<String> filesList = new LinkedList<>();
        try {
            Files.list(Paths.get(systemMessage.getRequestedPath())).forEach(p->filesList.add(p.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(filesList);
        systemMessage.setPathsList(filesList);
    }

    //формирование списка файлов при навигации по листу
    public void checkPath(SystemMessage systemMessage){
        if (Files.isDirectory(Paths.get(systemMessage.getRequestedPath()))){
            systemMessage.setCurrentServerPath(systemMessage.getRequestedPath());
            refreshFiles(systemMessage);
        }
    }

    //Запись файлов
    public void writeFile(FileMessage fileMessage){
        Path path = Paths.get(fileMessage.getDestinationPath() + "\\" + fileMessage.getFilePath());
        try {
            if (Files.isRegularFile(path)) {
                Files.write(path, fileMessage.getData(), StandardOpenOption.CREATE);
            }else {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Отправка файла/каталога с сроходом по детереву каталога
    public void sendFiles(ChannelHandlerContext ctx, String destinationPath, Path path) {
        if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)){
            send(ctx, destinationPath, path);
            return;
        }
        try (DirectoryStream<Path> directoryStream = newDirectoryStream(path)) {
            for (Path p : directoryStream) {
                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)){
                    send(ctx, destinationPath, p);
                }else{
                    sendFiles(ctx, destinationPath, p);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Непосредственно отправка (путь пересылайемого файла, путь относительно текущего каталога, путь назначения)
    private void send(ChannelHandlerContext ctx, String destinationPath, Path path){
        try {
            ctx.writeAndFlush(new FileMessage(path, path.subpath(path.getNameCount()-1,path.getNameCount()).toString(), destinationPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
