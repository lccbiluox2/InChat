package com.myself.nettychat.constont;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author:UncleCatMySelf
 * @Email：zhupeijie_java@126.com
 * @QQ:1341933031
 * @Date:Created in 11:46 2018\8\14 0014
 */
@Component
public class LikeRedisTemplate {

    private static final Logger logger = LoggerFactory.getLogger(LikeRedisTemplate.class);

    private Map<Object,Object> redisMap = new ConcurrentHashMap<>();

    private Map<Object,Object> secondRedisMap = new ConcurrentHashMap<>();

    /**存放链接池实例*/
    private Map<Object,Object> channelRedisMap = new ConcurrentHashMap<>();

    public void save(Object id,Object name){
        redisMap.put(id,name);
        secondRedisMap.put(name,id);
    }

    /**
     * 存储对应的用户名与Netty链接实例
     * @param name 登录用户名
     * @param channel Netty链接实例
     */
    public void saveChannel(Object name,Object channel){
        channelRedisMap.put(name,channel);
    }

    /**
     * 删除存储池实例
     * @param name 登录用户名
     */
    public void deleteChannel(Object name){
        if(name == null){
            return;
        }
        Object result = channelRedisMap.get(name);
        if(result != null) {
            logger.info("channelRedisMap中删除");
            channelRedisMap.remove(name);
        }
    }

    /**
     * 获取存储池中的链接实例
     * @param name 登录用户名
     * @return {@link io.netty.channel.Channel 链接实例}
     */
    public Object getChannel(Object name){
        return channelRedisMap.get(name);
    }

    /**
     * 获取储存池链接数
     * @return 在线数
     */
    public Integer getSize(){
        return channelRedisMap.size();
    }

    /**
     * 获取连接对应用户名称
     * @param id 连接Id
     * @return 用户名称
     */
    public Object getName(Object id){
        return redisMap.get(id);
    }


    public boolean check(Object id,Object name){
        if (secondRedisMap.get(name) == null){
            return true;
        }
        if (id.equals(secondRedisMap.get(name))){
            return true;
        }else{
            return false;
        }
    }

    public void delete(Object id){
        logger.info("删除：{}",id);
        if(id == null){
            return;
        }
        Object result = redisMap.get(id);
        if(result == null){
            return;
        }
        try {
            secondRedisMap.remove(redisMap.get(id));
            redisMap.remove(id);
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    /**
     * 返回在线用户列表信息
     * @return 用户名列表
     */
    public Object getOnline() {
        List<Object> result = new ArrayList<>();
        for (Object key:channelRedisMap.keySet()){
            result.add(key);
        }
        return result;
    }
}
