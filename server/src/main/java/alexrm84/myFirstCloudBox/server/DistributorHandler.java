package alexrm84.myFirstCloudBox.server;


import alexrm84.myFirstCloudBox.common.FileMessage;
import alexrm84.myFirstCloudBox.common.SystemMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class DistributorHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null){
                return;
            }
            if (msg instanceof SystemMessage){
                SystemMessage systemMessage = (SystemMessage)msg;
                systemMessage.setPathsList(Server.refreshFiles(systemMessage.getPathsList().peek()));
                ctx.writeAndFlush(systemMessage);
            }
            if (msg instanceof FileMessage){
                FileMessage fileMessage = (FileMessage)msg;
//                System.out.println();
                Files.write(Paths.get("server_storage/" + fileMessage.getFilename()),fileMessage.getData(), StandardOpenOption.CREATE);
            }
        }finally {
            ReferenceCountUtil.release(msg);
        }
        super.channelRead(ctx, msg);
    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
