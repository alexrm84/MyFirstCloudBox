package alexrm84.myFirstCloudBox.server;

import alexrm84.myFirstCloudBox.common.CryptoUtil;
import alexrm84.myFirstCloudBox.common.EncryptedMessage;
import alexrm84.myFirstCloudBox.common.Serialization;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class CryptoEncoder extends ChannelOutboundHandlerAdapter {

    private static final Logger logger = LogManager.getLogger(CryptoEncoder.class);
    private boolean keyExchange;
    private CryptoUtil cryptoUtil;
    private Serialization serialization;

    public CryptoEncoder(CryptoUtil cryptoUtil, Serialization serialization) {
        this.cryptoUtil = cryptoUtil;
        this.serialization = serialization;
        this.keyExchange = true;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (keyExchange){
            ctx.writeAndFlush(msg);
            if (cryptoUtil.getSecretKeyAES()!=null){
                keyExchange = false;
            }
        } else {
            try {
                byte[] data = serialization.serialize(msg);
                data = cryptoUtil.encryptAES(data);
                ctx.writeAndFlush(new EncryptedMessage(data));
            } catch (IOException e) {
                logger.log(Level.ERROR, "Data serialization error: ", e);
            }
        }
    }
}

