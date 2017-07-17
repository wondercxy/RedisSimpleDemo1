package me.xawei;

import me.xawei.tcpserver.DisServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Created by wei on 2017/7/17.
 */
@SpringBootApplication
public class RedisDemoEntrance {
    public static void main(String[] args) throws Exception {
        SpringApplication.run(RedisDemoEntrance.class, args);
        int port = 8000;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new DisServer(port).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        System.out.println("another thread start!");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new DisServer(port+1).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
}
