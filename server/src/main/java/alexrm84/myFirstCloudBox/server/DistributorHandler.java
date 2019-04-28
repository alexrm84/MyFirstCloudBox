package alexrm84.myFirstCloudBox.server;


import alexrm84.myFirstCloudBox.common.FileMessage;
import alexrm84.myFirstCloudBox.common.SystemMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;

public class DistributorHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Worker worker = new Worker(ctx);
        try {
            if (msg == null){
                return;
            }
            if (msg instanceof SystemMessage){
                SystemMessage systemMessage = (SystemMessage)msg;
                switch (systemMessage.getTypeMessage()){
                    case "REFRESH" :
                        worker.refreshFiles(systemMessage);
                        break;
                    case "CheckPath":
                        worker.checkPath(systemMessage);
                        break;
                    case "ReceiveFiles":
                        worker.sendFiles(ctx, systemMessage.getCurrentClientPath(), Paths.get(systemMessage.getPathsList().peek()));
                        break;
                }
                ctx.writeAndFlush(systemMessage);
            }
            if (msg instanceof FileMessage){
                FileMessage fileMessage = (FileMessage)msg;
                worker.writeFile(fileMessage);
                SystemMessage systemMessage = new SystemMessage();
                systemMessage.setTypeMessage("REFRESH").setPathsList(new LinkedList<>(Arrays.asList(fileMessage.getDestinationPath())));
                worker.refreshFiles(systemMessage);
                ctx.writeAndFlush(systemMessage);
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
