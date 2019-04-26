package alexrm84.myFirstCloudBox.server;

import alexrm84.myFirstCloudBox.common.SystemMessage;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

public class Server {

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

    public static void checkPath(SystemMessage systemMessage){
        systemMessage.setIsPath(Files.isDirectory(Paths.get(systemMessage.getPathsList().peek())));
        if (systemMessage.isPath()){
            systemMessage.setCurrentServerPath(systemMessage.getPathsList().peek());
            refreshFiles(systemMessage);
        }
    }
}
