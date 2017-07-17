package me.xawei.client;

import com.alibaba.fastjson.JSON;
import me.xawei.domain.Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * Created by wei on 2017/7/17.
 */
@ChannelHandler.Sharable
public class ReqClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private long requestId=1;
    private long money=1;

    public ReqClientHandler(long requestId, long money){
        this.requestId = requestId;
        this.money = money;
    }

    public ReqClientHandler(){}

    /**
     * 到服务器的连接已经建立之后将被调用。
     * 当被通知该 channel 是活动的时候就发送信息
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Message message = new Message();
        message.setRequestId(requestId);
        message.setMoney(money);

        String jsonString = JSON.toJSONString(message);

        ctx.writeAndFlush(Unpooled.copiedBuffer(jsonString, CharsetUtil.UTF_8));
    }


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        System.out.println("Client received:" + byteBuf.toString(CharsetUtil.UTF_8));
    }
}
