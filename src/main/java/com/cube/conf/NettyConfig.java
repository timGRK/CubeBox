package com.cube.conf;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/** 启动服务
* @ClassName: NettyConfig 
* @Description: TODO
* @author Wenbo Shen 
* @date Mar 12, 2016 4:46:32 PM 
*  
*/
@Configuration
public class NettyConfig {
    
    @Bean(name = {"serverBootstrap"})
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ServerBootstrap serverBootstrap() {
        return new ServerBootstrap();
    }

    @Bean(name = {"boss"})
    public EventLoopGroup boss() {
        return new NioEventLoopGroup();
    }

    @Bean(name = {"cubeTcpWorker"})
    public EventLoopGroup cubeTcpWorker() {
        return new NioEventLoopGroup();
    }
    
    @Bean(name = {"cubeHttpWorker"})
    public EventLoopGroup cubeHttpWorker() {
        return new NioEventLoopGroup();
    }

}
