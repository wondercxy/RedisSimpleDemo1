# 使用Redis实现简单请求队列缓存


## 架构概述
![][1]

1. 多个客户端通过套接字连接TCP服务器发送请求；请求格式形如：{"requestId":100, "money":123}
2. 有多台TCP服务器，通过负载均衡器（这里手动模拟）转发客户端请求到不同的服务器；
3. TCP服务器接收到请求后，通过Jedis客户端与Redis连接，使用setnx命令以requestId的值作为身份表示，保存第一个请求;
4. 多个消费者(Consumer)通过自定义的方式消费Redis中缓存的请求。这里实现的逻辑是消费者不断轮询，取出Redis中缓存的请求，在控制台打印后删除。

## TCP服务器的实现
主要使用netty框架，主要代码如下：
程序清单1: DisServer.java
```java
public class DisServer {
    private int port;

    public DisServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        new DisServer(8000).start();
    }

    public void start() {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup(20);
        final EventExecutorGroup executorGroup = new DefaultEventExecutorGroup(2);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.config().setReceiveBufferSize(2048); //set  buf size here
                            socketChannel.pipeline().addLast(executorGroup, new RedisServerHandler());
                        }
                    });
            // 绑定服务器，并等待绑定完成。
            ChannelFuture future = bootstrap.bind().sync();
            // 阻塞直到服务器的Channel关闭。
            future.channel().closeFuture().sync();
        } catch (Exception e){
            e.printStackTrace();
        }
        finally {
//            关闭EventLoopGroup,并且释放所有的资源，包括被创建的线程。
//            group.shutdownGracefully().sync();
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
```

程序清单2: RedisServerHandler.java
```java
@ChannelHandler.Sharable // 标志该Handler可以被多个Handler安全的共享。可以被添加到多个ChannelPipeline
public class RedisServerHandler extends ChannelInboundHandlerAdapter {

    public static byte[] redisKey = "key".getBytes();

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
```
注：POJO对象Message（请求），MyResponse（返回）的具体代码略。

## Jedis客户端连接Redis
新建一个JedisUtil类，配置服务端的连接地址与端口等后，在此类中加入所需的辅助函数，实现与Redis的交互。

主要关键函数有：
```java
public static long hsetnx(String key, String field, String value) {
        Jedis jedis = null;
        long res = -1L;
        try {
            jedis = jedisPool.getResource();
            res = jedis.hsetnx(key, field, value);
        } catch (Exception e) {
            //释放redis对象
            jedis.close();
            e.printStackTrace();
        } finally {
            //返还到连接池
            jedis.close();
        }
        return res;
    }
```

```java
public static List<String> hvals(String key){
        Jedis jedis = null;
        List<String> resList = null;
        try{
            jedis = jedisPool.getResource();
            resList = jedis.hvals(key);
        } catch (Exception e){
            jedis.close();
            e.printStackTrace();
        }
        finally {
            jedis.close();
        }
        return resList;
    }
```

```java
public static void del(byte[] key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.del(key);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //返还到连接池
            jedis.close();
        }
    }
```

## 请求在Redis中的存储方式
Redis以字符串"requests"为键，对应一个hash数据结构，里面存多个filed-value对，filed为requestId的值，value是请求的全文字符串。

例如收到三个请求如下：
{"requestId":100, "money":50}  
{"requestId":101, "money":60}  
{"requestId":100, "money":150}  

则使用shell客户端查看Redis中存储的信息如下：  
![][2]

## 消费者的实现
这里消费者定义的逻辑是，一直轮询（有2秒间隔）Redis中requests对应的值，有则取出然后删除，打印在控制台上。

为保证取值和删键之间没有其他客户端的操作影响，使用redis的mutil和exec命令构建事务。

程序清单3: DefaultConsumer.java
```java
public class DefaultConsumer {

    private List<String> vals;

    private void consume(int timeslice) throws InterruptedException {

        while(true){
            Jedis jedis = new Jedis("127.0.0.1", 6379);
            //事务操作开始
            Transaction transaction = jedis.multi();
            Response<List<String>> response =  transaction.hvals("requests");
            transaction.del("requests");
            transaction.exec();
            //事务操作结束
            
            vals = response.get();

            for(String s:vals){
                System.out.println(s);
            }
            Thread.sleep(2000);
        }
    }

    public static void main(String[] args) {
        DefaultConsumer consumer = new DefaultConsumer();
        try {
            consumer.consume(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

## 项目代码

完整可运行的项目代码地址：

https://github.com/xawei/RedisSimpleDemo1




  [1]: http://static.zybuluo.com/csxawei/7cm783n4wzm1tcbhntvm2sz8/3.png
  [2]: http://static.zybuluo.com/csxawei/n3q0i8szezdnyh6cf0i29rpg/4.png
