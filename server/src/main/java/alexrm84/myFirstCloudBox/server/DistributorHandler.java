package alexrm84.myFirstCloudBox.server;


import alexrm84.myFirstCloudBox.common.SystemMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class DistributorHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null){
                return;
            }
            if (msg instanceof SystemMessage){
                SystemMessage systemMessage = (SystemMessage)msg;

            }
        }finally {
            ReferenceCountUtil.release(msg);
        }
        super.channelRead(ctx, msg);
    }
}
