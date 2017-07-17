package me.xawei.domain;

/**
 * Created by wei on 2017/7/17.
 */
public class Message {
    private long requestId;
    private long money;

    public Message(){
        //加上这个空的构造方法，才能用上fastjson
    }

    public Message(long requestId, long money) {
        this.requestId = requestId;
        this.money = money;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public long getMoney() {
        return money;
    }
}
