package me.xawei.domain;

/**
 * Created by wei on 2017/7/17.
 */
public class MyResponse {
    private long requestId;
    private long money;
    private int resultcode;

    public void setResultcode(int resultcode) {
        this.resultcode = resultcode;
    }

    public int getResultcode() {
        return resultcode;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public long getRequestId() {
        return requestId;
    }

    public long getMoney() {
        return money;
    }

    public void increMoney(){
        this.money++;
    }
}
