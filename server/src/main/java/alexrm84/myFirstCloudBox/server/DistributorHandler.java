package alexrm84.myFirstCloudBox.server;

import alexrm84.myFirstCloudBox.common.Command;
import alexrm84.myFirstCloudBox.common.CryptoUtil;
import alexrm84.myFirstCloudBox.common.FileMessage;
import alexrm84.myFirstCloudBox.common.SystemMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.NoSuchAlgorithmException;

public class DistributorHandler extends ChannelInboundHandlerAdapter {
    private boolean firstRun;
    private boolean authorization;
    Worker worker;
    CryptoUtil cryptoUtil;
    private static final Logger logger = LogManager.getLogger(DistributorHandler.class);

    public DistributorHandler() {
        this.firstRun = true;
        this.authorization = false;
        this.worker = new Worker();
        this.cryptoUtil = new CryptoUtil();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (firstRun){
            cryptoUtil.initRSA();
            ctx.writeAndFlush(new SystemMessage().setTypeMessage(Command.Encryption).setPublicKeyRSA(cryptoUtil.getKeyPairRSA().getPublic()));
            firstRun = false;
        }
        try {
            if (msg == null) {
                return;
            }
            if (authorization) {
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
            }else {
                if (msg instanceof SystemMessage){
                    SystemMessage systemMessage = (SystemMessage) msg;
                    if (systemMessage.getTypeMessage().equals(Command.Authorization)) {
                        authorization = worker.authorization(ctx, systemMessage, cryptoUtil);
                    }
                }
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
