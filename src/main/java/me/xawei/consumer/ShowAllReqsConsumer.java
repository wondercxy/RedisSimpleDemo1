package me.xawei.consumer;

import redis.clients.jedis.Jedis;

import java.util.List;

/**
 * Created by wei on 2017/7/17.
 */
public class ShowAllReqsConsumer {
    public static void main(String[] args) {
        Jedis jedis = new Jedis("127.0.0.1", 6379);
        List<String> res = jedis.hvals("requests");

        for(String s:res){
            System.out.println(s);
        }
        jedis.close();
    }
}
