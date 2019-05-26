package alexrm84.myFirstCloudBox.server;

import alexrm84.myFirstCloudBox.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CryptoDecoder extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LogManager.getLogger(CryptoDecoder.class);
    private CryptoUtil cryptoUtil;
    private Serialization serialization;

    public CryptoDecoder(CryptoUtil cryptoUtil, Serialization serialization) {
        this.cryptoUtil = cryptoUtil;
        this.serialization = serialization;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) {
                return;
            }
            if (msg instanceof SystemMessage){
                SystemMessage systemMessage = (SystemMessage)msg;
                switch (systemMessage.getTypeMessage()){
                    case PublicKeyRSA:
                        logger.log(Level.INFO, "RSA public key request received");
                        cryptoUtil.initRSA();
                        ctx.writeAndFlush(systemMessage.setPublicKeyRSA(cryptoUtil.getKeyPairRSA().getPublic()));
                        break;
                    case SecretKeyAES:
                        logger.log(Level.INFO, "AES received");
                        cryptoUtil.decryptRSA(systemMessage.getSecretKeyAES());
                        ctx.writeAndFlush(systemMessage);
                        break;
                }
            }
            if (msg instanceof EncryptedMessage){
                EncryptedMessage em = (EncryptedMessage)msg;
                byte[] data = em.getData();
                data = cryptoUtil.decryptAES(data);
                Object obj = serialization.deserialize(data);
                ctx.fireChannelRead(obj);
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
