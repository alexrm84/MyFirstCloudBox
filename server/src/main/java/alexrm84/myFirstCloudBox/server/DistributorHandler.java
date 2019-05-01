package alexrm84.myFirstCloudBox.server;

import alexrm84.myFirstCloudBox.common.FileMessage;
import alexrm84.myFirstCloudBox.common.SystemMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

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
                    case Refresh:
                        ctx.writeAndFlush(systemMessage.setPathsList(worker.refreshFiles(systemMessage.getRequestedPath())));
                        break;
                    case CheckPath:
                        worker.checkPath(ctx, systemMessage);
                        break;
                    case ReceiveFiles:
                        worker.sendFiles(ctx, systemMessage);
                        break;
                    case DeleteFiles:
                        worker.deleteFiles(ctx, systemMessage);
                        break;
                }
            }
            if (msg instanceof FileMessage){
                FileMessage fileMessage = (FileMessage)msg;
                worker.writeFile(ctx, fileMessage);
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
