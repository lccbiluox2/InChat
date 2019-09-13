package com.myself.nettychat.config;

import com.myself.nettychat.constont.H5Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;


/**
 * 这个类主要做的事直接访问html页面 不经过controller
 */
@Configuration
public class MvcConfiguration extends WebMvcConfigurerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MvcConfiguration.class);

    /**
     * 添加直接跳转页面的配置 不需要经过controller
     *
     * @param registry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        //跳转到admin登录页面
        logger.info("静态目录，可以直接访问");
        registry.addViewController("/susu/admin/toLogin").setViewName(H5Constant.LOGIN_SUI);
    }
    /**
     * 这里是自定义映射自己的静态资源目录的
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 这里是自定义映射自己的静态资源目录的  注释的这一行访问http://192.168.0.7:8080/upload/product/logo.jpg 相当于访问/static/img/目录下的logo.jpg图片
        registry.addResourceHandler("/susu/image/**").addResourceLocations("classpath:/static/image/");

         // 下面是映射wepapp/upload/produce 映射成静态资源目录
        registry.addResourceHandler("/upload/rent/**").addResourceLocations("/upload/rent/");
        registry.addResourceHandler("/upload/news/**").addResourceLocations("/upload/news/");
        logger.info("静态资源目录配置");
        super.addResourceHandlers(registry);
    }

}  
