package com.myself.nettychat.config;

import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Description:
 *
 * @author lcc
 * @version 1.0
 * @date 2019-09-09 13:44
 **/
public class MyFilter implements Filter {

    private Logger logger = Logger.getLogger(MyFilter.class);

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
        logger.info("销毁视图");
    }


    @Override
    public void doFilter(ServletRequest srequest, ServletResponse sresponse, FilterChain filterChain)
            throws IOException, ServletException {
        // TODO Auto-generated method stub
        HttpServletRequest request = (HttpServletRequest) srequest;
        logger.info("this is MyFilter,url :"+request.getRequestURI());
        // /welcome
        filterChain.doFilter(srequest, sresponse);
    }


    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub
    }


}