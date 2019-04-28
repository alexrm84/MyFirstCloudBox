package alexrm84.myFirstCloudBox.server;

import alexrm84.myFirstCloudBox.common.Command;
import alexrm84.myFirstCloudBox.common.FileMessage;
import alexrm84.myFirstCloudBox.common.SystemMessage;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.LinkedList;

import static java.nio.file.Files.newDirectoryStream;

public class Worker {
    private ChannelHandlerContext ctx;

    public Worker(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    //Создание списка файлов на запрос обновления
    public LinkedList<String> refreshFiles(String path){
        LinkedList<String> filesList = new LinkedList<>();
        try {
            Files.list(Paths.get(path)).forEach(p->filesList.add(p.toString()));
        } catch (IOException e) {
            e.printStackTrace();
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
            if (Files.isRegularFile(path)) {
                Files.write(path, fileMessage.getData(), StandardOpenOption.CREATE);
            }else {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ctx.writeAndFlush(new SystemMessage().setTypeMessage(Command.Refresh).setPathsList(refreshFiles(fileMessage.getDestinationPath())));
    }

//Отправка файла/каталога с сроходом по детереву каталога
    public void sendFiles(ChannelHandlerContext ctx, SystemMessage systemMessage){
        try {
            Files.walk(Paths.get(systemMessage.getRequestedPath())).sorted(Comparator.naturalOrder()).forEach(path -> send(ctx, systemMessage, path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//Непосредственно отправка (путь пересылайемого файла, путь относительно текущего каталога, путь назначения)
    private void send(ChannelHandlerContext ctx, SystemMessage systemMessage, Path path){
        try {
            ctx.writeAndFlush(new FileMessage(path, path.subpath(Paths.get(systemMessage.getRequestedPath()).getNameCount()-1,path.getNameCount()).toString(), systemMessage.getCurrentClientPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteFiles(ChannelHandlerContext ctx, SystemMessage systemMessage) {
        Path path = Paths.get(systemMessage.getRequestedPath());
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
        ctx.writeAndFlush(new SystemMessage().setTypeMessage(Command.Refresh).setPathsList(refreshFiles(path.getParent().toString())));
    }
}
