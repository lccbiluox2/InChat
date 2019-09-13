package com.myself.nettychat;

import com.myself.nettychat.config.NettyConfig;
import com.myself.nettychat.config.NettyTcpConfig;
import com.myself.nettychat.config.TCPServer;
import com.myself.nettychat.config.TextWebSocketFrameHandler;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
//定时任务支持
@EnableScheduling
//注解开启对aspectJ的支持
@EnableAspectJAutoProxy
//Swagger自动生成文档
@EnableSwagger2
public class NettychatApplication {

    private static final Logger logger = LoggerFactory.getLogger(NettychatApplication.class);

	public static void main(String[] args) throws Exception{
		ConfigurableApplicationContext context = SpringApplication.run(NettychatApplication.class, args);
		NettyConfig nettyConfig = context.getBean(NettyConfig.class);
		NettyTcpConfig nettyTcpConfig = context.getBean(NettyTcpConfig.class);
		TCPServer tcpServer = context.getBean(TCPServer.class);


		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
                    logger.info("Web端Netty通信服务端启动成功！端口：8090");
					tcpServer.startWeb();
				}catch (Exception e){
					e.printStackTrace();
				}
			}
		}).start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
                    logger.info("TCP端Netty通信服务端启动成功！端口：8092");
					tcpServer.startTcp();
				}catch (Exception e){
					e.printStackTrace();
				}
			}
		}).start();
	}

}
