package me.xawei.tcpserver;

import com.alibaba.fastjson.JSON;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import me.xawei.domain.Message;
import me.xawei.domain.MyResponse;
import me.xawei.tools.JedisUtil;


/**
 * Created by wei on 2017/7/17.
 */
@ChannelHandler.Sharable // 标志该Handler可以被多个Handler安全的共享。可以被添加到多个ChannelPipeline
public class RedisServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 通道读到数据时
     * */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        String reqStr = buf.toString(CharsetUtil.UTF_8);
        System.out.println("Server received(RedisHandler):" + reqStr);
        Message message = JSON.parseObject(reqStr, Message.class);

        String requestIdStr = String.valueOf(message.getRequestId());


        //先创建respone POJO对象
        MyResponse ares = new MyResponse();
        ares.setRequestId(message.getRequestId());
        ares.setMoney(message.getMoney());


        /**
         * 基于Redis的单线程架构，改成以下实现应该即可防止多客户端并发出现错误
         * */
        long res = JedisUtil.hsetnx("requests", requestIdStr, reqStr);
        //不存在，第一次请求成功
        if(res > 0){
            System.out.println("first arrival of requestId: "+requestIdStr);
            ares.setResultcode(1);
            ares.increMoney();
        } else {
            //设置resultcode为0， 表示非第一次请求，失败
            ares.setResultcode(0);
        }
        ctx.write(Unpooled.copiedBuffer(JSON.toJSONString(ares).getBytes()));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // 消息flush到远程节点并且关闭该channel。冲刷所有待审消息到远程节点。关闭通道后，操作完成
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE);
    }

    @Override //打印异常堆栈跟踪
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close(); //关闭通道
    }
}
