package alexrm84.myFirstCloudBox.server;

import alexrm84.myFirstCloudBox.common.FileMessage;
import alexrm84.myFirstCloudBox.common.SystemMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DistributorHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(DistributorHandler.class);
    private boolean authorization;
    Worker worker;

    public DistributorHandler() {
        this.authorization = false;
        this.worker = new Worker();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) {
                return;
            }
            if (authorization) {
                if (msg instanceof SystemMessage){
                    SystemMessage systemMessage = (SystemMessage)msg;
                    switch (systemMessage.getTypeMessage()){
                        case CanSend:
                            worker.setCanSend(true);
                            break;
                        case Refresh:
                            ctx.writeAndFlush(systemMessage.setPathsList(worker.refreshFiles(systemMessage.getRequestedPath())));
                            break;
                        case ReceiveFiles:
                            worker.sendFiles(ctx, systemMessage);
                            break;
                        case DeleteFiles:
                            worker.deleteFiles(ctx, systemMessage);
                            break;
                        case Exit:
                            logger.log(Level.INFO, "User: " + worker.getUserName() + " logout.");
                            ctx.close();
                            break;
                    }
                }
                if (msg instanceof FileMessage){
                    FileMessage fileMessage = (FileMessage)msg;
                    worker.writeFile(ctx, fileMessage);
                }
            }else {
                if (msg instanceof SystemMessage) {
                    SystemMessage systemMessage = (SystemMessage) msg;
                    switch (systemMessage.getTypeMessage()) {
                        case Authorization:
                            authorization = worker.authorization(ctx, systemMessage);
                            break;
                        case CreateUser:
                            authorization = worker.createUser(ctx, systemMessage);
                            break;

                    }
                }
            }
        }finally {
            ReferenceCountUtil.release(msg);
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
