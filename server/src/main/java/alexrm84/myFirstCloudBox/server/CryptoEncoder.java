package alexrm84.myFirstCloudBox.server;

import alexrm84.myFirstCloudBox.common.CryptoUtil;
import alexrm84.myFirstCloudBox.common.EncryptedMessage;
import alexrm84.myFirstCloudBox.common.Serialization;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
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

    public CryptoEncoder(CryptoUtil cryptoUtil) {
        this.cryptoUtil = cryptoUtil;
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
                byte[] data = Serialization.serialize(msg);
                data = cryptoUtil.encryptAES(data);
                ctx.writeAndFlush(new EncryptedMessage(data));
            } catch (IOException e) {
                logger.log(Level.ERROR, "Data serialization error: ", e);
            }
        }
    }
}

