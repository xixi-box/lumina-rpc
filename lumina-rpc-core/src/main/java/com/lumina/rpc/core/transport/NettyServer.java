package com.lumina.rpc.core.transport;

import com.lumina.rpc.core.codec.RpcDecoder;
import com.lumina.rpc.core.codec.RpcEncoder;
import com.lumina.rpc.core.spi.Serializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Netty RPC 服务器
 */
public class NettyServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    // Boss EventLoopGroup，用于处理连接请求
    private final EventLoopGroup bossGroup;

    // Worker EventLoopGroup，用于处理 I/O 操作
    private final EventLoopGroup workerGroup;

    // ServerBootstrap
    private final ServerBootstrap serverBootstrap;

    // 序列化器
    private final Serializer serializer;

    // 请求处理器
    private final RpcRequestHandler requestHandler;

    // 绑定的端口
    private int port;

    // 运行状态
    private volatile boolean running = false;

    public NettyServer(Serializer serializer, RpcRequestHandler requestHandler) {
        this.serializer = serializer;
        this.requestHandler = requestHandler;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.serverBootstrap = new ServerBootstrap();
        initBootstrap();
    }

    /**
     * 初始化 ServerBootstrap
     */
    private void initBootstrap() {
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();

                        // 心跳检测（60秒读超时，30秒写超时）
                        pipeline.addLast(new IdleStateHandler(60, 30, 0, TimeUnit.SECONDS));

                        // 解码器（解决粘包/半包）
                        pipeline.addLast(new RpcDecoder());

                        // 编码器
                        pipeline.addLast(new RpcEncoder(serializer));

                        // 服务器处理器
                        pipeline.addLast(new NettyServerHandler(requestHandler));
                    }
                });
    }

    /**
     * 启动服务器
     *
     * @param port 端口号
     */
    public void start(int port) {
        if (running) {
            logger.warn("Server is already running on port {}", this.port);
            return;
        }

        this.port = port;

        try {
            ChannelFuture future = serverBootstrap.bind(port).sync();
            this.running = true;
            logger.info("Netty RPC server started on port {}", port);

            // 等待服务器关闭
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("Server interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            shutdown();
        }
    }

    /**
     * 异步启动服务器
     *
     * @param port 端口号
     * @return ChannelFuture
     */
    public ChannelFuture startAsync(int port) {
        if (running) {
            logger.warn("Server is already running on port {}", this.port);
            throw new IllegalStateException("Server is already running");
        }

        this.port = port;

        ChannelFuture future = serverBootstrap.bind(port);
        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                this.running = true;
                logger.info("Netty RPC server started on port {}", port);
            } else {
                logger.error("Failed to start server on port {}", port, f.cause());
            }
        });

        return future;
    }

    /**
     * 关闭服务器
     */
    public void shutdown() {
        if (!running) {
            return;
        }

        this.running = false;
        logger.info("Shutting down Netty RPC server...");

        try {
            if (bossGroup != null && !bossGroup.isShutdown()) {
                bossGroup.shutdownGracefully();
            }
            if (workerGroup != null && !workerGroup.isShutdown()) {
                workerGroup.shutdownGracefully();
            }
            logger.info("Netty RPC server shutdown complete");
        } catch (Exception e) {
            logger.error("Error during server shutdown", e);
        }
    }

    /**
     * 获取端口号
     *
     * @return 端口号
     */
    public int getPort() {
        return port;
    }

    /**
     * 是否正在运行
     *
     * @return true 如果正在运行
     */
    public boolean isRunning() {
        return running;
    }
}
