package com.myself.nettychat.controller;

import com.myself.nettychat.config.TextWebSocketFrameHandler;
import com.myself.nettychat.constont.CookieConstant;
import com.myself.nettychat.constont.H5Constant;
import com.myself.nettychat.dataobject.User;
import com.myself.nettychat.form.LoginForm;
import com.myself.nettychat.repository.UserMsgRepository;
import com.myself.nettychat.service.UserService;
import com.myself.nettychat.store.TokenStore;
import com.myself.nettychat.common.utils.CookieUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @Author:UncleCatMySelf
 * @Email：zhupeijie_java@126.com
 * @QQ:1341933031
 * @Date:Created in 16:01 2018\8\18 0018
 */
@Controller
@RequestMapping("/admin")
public class NcLoginController {

    private static final Logger logger = LoggerFactory.getLogger(NcLoginController.class);

    @Autowired
    private UserService userService;
    @Autowired
    private UserMsgRepository userMsgRepository;

    /**
     * 登录页面
     * @return
     */
//    @GetMapping("/login")
//    public ModelAndView login(Map<String,Object> map){
//        return new ModelAndView(H5Constant.LOGIN);
//    }


    /**
     * 登录页面SUI
     * @return
     */
    @GetMapping("/loginsui")
    public ModelAndView loginSui(Map<String,Object> map){
        return new ModelAndView(H5Constant.LOGIN_SUI);
    }

    /**
     * 注册页面
     * @return
     */
    @GetMapping("/regis")
    public ModelAndView register(){
        return new ModelAndView(H5Constant.LOGIN_SUI);
    }



    /**
     * 执行注册
     * @param form
     * @param bindingResult
     * @param response
     * @param map
     * @return
     */
    @PostMapping("/toRegister")
    public ModelAndView toRegister(@Valid LoginForm form, BindingResult bindingResult , HttpServletResponse response,
                                   Map<String, Object> map){
        if (bindingResult.hasErrors()){
            map.put("msg",bindingResult.getFieldError().getDefaultMessage());
            return new ModelAndView(H5Constant.LOGIN_SUI,map);
        }
        List<User> userList = userService.findAll();
        for (User item:userList){
            if (item.getUserName().equals(form.getFUserName())){
                map.put("msg","用户名已存在，请重新填写唯一用户名");
                return new ModelAndView(H5Constant.LOGIN_SUI,map);
            }
        }
        User user = new User();
        BeanUtils.copyProperties(form,user);
        userService.save(user);
        map.put("userName",user.getUserName());
        map.put("passWord",user.getPassWord());
        return new ModelAndView(H5Constant.LOGIN_SUI,map);
    }

    /**
     * 登录判断
     * @return
     */
    @PostMapping("/toLogin")
    public ModelAndView toLogin(@RequestParam(value = "page",defaultValue = "1") Integer page,
                                @RequestParam(value = "size",defaultValue = "10") Integer size,
                                @Valid LoginForm form, BindingResult bindingResult , HttpServletResponse response,
                                Map<String, Object> map){
        String userName = form.getUserName();
        if(userName == null){
            logger.info("页面没有传递用户信息");
            return new ModelAndView(H5Constant.LOGIN,map);
        }
        User user = userService.findByUserName(userName);
        if(user == null){
            logger.info("该用户未注册");
            return new ModelAndView(H5Constant.LOGIN,map);
        }
        if (!user.getPassWord().equals(form.getPassWord())) {
            logger.info("密码不正确");
            return new ModelAndView(H5Constant.LOGIN,map);
        }
        logger.info("验证通过");
        //登录成功
        String token = UUID.randomUUID().toString();
        //将token信息添加到系统缓存中
        TokenStore.add(token,user.getId());
        //将Token信息添加到Cookie中
        CookieUtil.set(response, CookieConstant.TOKEN,token,CookieConstant.EXPIRE);
        logger.info("缓存到cookie中");

        return new ModelAndView(H5Constant.HOME);

    }

}
