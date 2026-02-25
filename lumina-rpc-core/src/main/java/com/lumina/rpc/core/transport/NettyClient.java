package com.lumina.rpc.core.transport;

import com.lumina.rpc.core.codec.RpcDecoder;
import com.lumina.rpc.core.codec.RpcEncoder;
import com.lumina.rpc.core.protocol.RpcMessage;
import com.lumina.rpc.core.spi.Serializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Netty RPC 客户端
 */
public class NettyClient {

    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

    // EventLoopGroup
    private final EventLoopGroup eventLoopGroup;

    // Bootstrap
    private final Bootstrap bootstrap;

    // 序列化器
    private final Serializer serializer;

    // 当前连接的 Channel
    private volatile Channel channel;

    // 连接的服务器地址
    private volatile InetSocketAddress serverAddress;

    // 连接状态
    private volatile boolean connected = false;

    public NettyClient(Serializer serializer) {
        this.serializer = serializer;
        this.eventLoopGroup = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        initBootstrap();
    }

    /**
     * 初始化 Bootstrap
     */
    private void initBootstrap() {
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();

                        // 心跳检测
                        pipeline.addLast(new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS));

                        // 解码器（解决粘包/半包）
                        pipeline.addLast(new RpcDecoder());

                        // 编码器
                        pipeline.addLast(new RpcEncoder(serializer));

                        // 客户端处理器
                        pipeline.addLast(new NettyClientHandler());
                    }
                });
    }

    /**
     * 连接到服务器
     *
     * @param address 服务器地址
     * @return 连接成功的 Channel
     */
    public synchronized Channel connect(InetSocketAddress address) {
        if (connected && channel != null && channel.isActive()) {
            if (address.equals(serverAddress)) {
                logger.debug("Already connected to {}", address);
                return channel;
            }
            // 连接到不同服务器，先关闭当前连接
            close();
        }

        this.serverAddress = address;

        try {
            CompletableFuture<Channel> future = new CompletableFuture<>();

            bootstrap.connect(address).addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    this.channel = channelFuture.channel();
                    this.connected = true;
                    logger.info("Connected to RPC server: {}", address);
                    future.complete(channel);
                } else {
                    this.connected = false;
                    logger.error("Failed to connect to RPC server: {}", address, channelFuture.cause());
                    future.completeExceptionally(channelFuture.cause());
                }
            });

            return future.get(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            this.connected = false;
            logger.error("Exception while connecting to {}", address, e);
            throw new RuntimeException("Failed to connect to RPC server", e);
        }
    }

    /**
     * 发送 RPC 消息
     *
     * @param message RPC 消息
     */
    public void sendMessage(RpcMessage message) {
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("Not connected to RPC server");
        }

        channel.writeAndFlush(message).addListener(future -> {
            if (!future.isSuccess()) {
                logger.error("Failed to send RPC message", future.cause());
            }
        });
    }

    /**
     * 获取当前 Channel
     *
     * @return Channel
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * 是否已连接
     *
     * @return true 如果已连接
     */
    public boolean isConnected() {
        return connected && channel != null && channel.isActive();
    }

    /**
     * 关闭客户端
     */
    public void close() {
        connected = false;
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (eventLoopGroup != null && !eventLoopGroup.isShutdown()) {
            eventLoopGroup.shutdownGracefully();
        }
        logger.info("Netty RPC client closed");
    }
}
