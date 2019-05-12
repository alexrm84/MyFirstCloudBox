package alexrm84.myFirstCloudBox.server;

import alexrm84.myFirstCloudBox.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class CryptoDecoder extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LogManager.getLogger(CryptoDecoder.class);
    private CryptoUtil cryptoUtil;

    public CryptoDecoder(CryptoUtil cryptoUtil) {
        this.cryptoUtil = cryptoUtil;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            System.out.println("подключился клиент");
            if (msg == null) {
                return;
            }
            if (msg instanceof SystemMessage){
                SystemMessage systemMessage = (SystemMessage)msg;
                switch (systemMessage.getTypeMessage()){
                    case PublicKeyRSA:
                        System.out.println("получен запрос РСА");
                        cryptoUtil.initRSA();
                        ctx.writeAndFlush(systemMessage.setPublicKeyRSA(cryptoUtil.getKeyPairRSA().getPublic()));
                        break;
                    case SecretKeyAES:
                        System.out.println("получен АЕС");
                        cryptoUtil.decryptRSA(systemMessage.getSecretKeyAES());
                        ctx.writeAndFlush(systemMessage);
                        break;
                }
            }
            if (msg instanceof EncryptedMessage){
                EncryptedMessage em = (EncryptedMessage)msg;
                byte[] data = em.getData();
                data = cryptoUtil.decryptAES(data);
                Object obj = null;
                try {
                    obj = Serialization.deserialize(data);
                } catch (IOException e) {
                    logger.log(Level.ERROR, "Data deserialization error: ", e);
                } catch (ClassNotFoundException e) {
                    logger.log(Level.ERROR, "Data deserialization error: ", e);
                }
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
