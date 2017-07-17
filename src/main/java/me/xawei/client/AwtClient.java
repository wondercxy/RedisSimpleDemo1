package me.xawei.client;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Created by wei on 2017/7/17.
 */
public class AwtClient extends JFrame implements ActionListener {
    String requestId;
    String money;
    String port;
    public static String randomcaptcha;

    public JLabel requestIdLabel, moneyLabel, portLabel;
    public JTextField requestIdInput, moneyInput, portInput;
    public JButton login;

    public AwtClient(){
        setTitle("send request!");
        setSize(400, 250);
        setLocationRelativeTo(null);
        init();
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
    }

    public void init() {
        setLayout(null);

        requestIdLabel = new JLabel("requestId");
        requestIdLabel.setBounds(90, 30, 60, 40);
        add(requestIdLabel);
        requestIdInput = new JTextField();
        requestIdInput.setBounds(150, 40, 150, 20);
        add(requestIdInput);

        moneyLabel = new JLabel("money");
        moneyLabel.setBounds(90, 70, 60, 40);
        add(moneyLabel);
        moneyInput = new JTextField();
        moneyInput.setBounds(150, 80, 150, 20);
        add(moneyInput);

        portLabel = new JLabel("port");
        portLabel.setBounds(90, 110, 60, 40);
        add(portLabel);
        portInput = new JTextField();
        portInput.setBounds(150, 120, 150, 20);
        add(portInput);

        login = new JButton("发送请求");
        login.setBounds(70, 150, 120, 30);
        login.setMnemonic(KeyEvent.VK_L);
        add(login);

        requestIdInput.addActionListener(this);
        moneyInput.addActionListener(this);
        portInput.addActionListener(this);

        login.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        requestId = requestIdInput.getText();
        money = moneyInput.getText();
        port = portInput.getText();

        long l_requestId = Long.valueOf(requestId);
        long l_money = Long.valueOf(money);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new ReqClient("127.0.0.1", Integer.valueOf(port), new ReqClientHandler(l_requestId, l_money)).start();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        new AwtClient();
    }
}
