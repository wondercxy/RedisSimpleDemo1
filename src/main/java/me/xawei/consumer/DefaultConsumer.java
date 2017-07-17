package me.xawei.consumer;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.util.List;

/**
 * Created by wei on 2017/7/17.
 */
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
