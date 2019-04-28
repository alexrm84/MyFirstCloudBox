package alexrm84.myFirstCloudBox.server;

import alexrm84.myFirstCloudBox.common.FileMessage;
import alexrm84.myFirstCloudBox.common.SystemMessage;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedList;

import static java.nio.file.Files.newDirectoryStream;

public class Server {

//    private static String pathStorage = "server_storage\\";

    public void run() throws Exception {
        EventLoopGroup mainGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(mainGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception{
                            socketChannel.pipeline().addLast(
                                    new ObjectDecoder(100*1024*1024, ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new DistributorHandler()
                            );
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture channelFuture = serverBootstrap.bind(9999).sync();
            channelFuture.channel().closeFuture().sync();
        }finally {
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new Server().run();
    }

//Создание списка файлов на запрос обновления
    public static void refreshFiles(SystemMessage systemMessage){
        LinkedList<String> filesList = new LinkedList<>();
        try {
            Files.list(Paths.get(systemMessage.getPathsList().peek())).forEach(p->filesList.add(p.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(filesList);
        systemMessage.setPathsList(filesList);
    }

//формирование списка файлов при навигации по листу
    public static void checkPath(SystemMessage systemMessage){
        systemMessage.setIsPath(Files.isDirectory(Paths.get(systemMessage.getPathsList().peek())));
        if (systemMessage.isPath()){
            systemMessage.setCurrentServerPath(systemMessage.getPathsList().peek());
            refreshFiles(systemMessage);
        }
    }

//Запись файлов
    public static void writeFile(FileMessage fileMessage){
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
    }

//Отправка файла/каталога с сроходом по детереву каталога
    public static void sendFiles(ChannelHandlerContext ctx, String destinationPath, Path path) {
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
    private static void send(ChannelHandlerContext ctx, String destinationPath, Path path){
        try {
            ctx.writeAndFlush(new FileMessage(path, path.subpath(path.getNameCount()-1,path.getNameCount()).toString(), destinationPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
