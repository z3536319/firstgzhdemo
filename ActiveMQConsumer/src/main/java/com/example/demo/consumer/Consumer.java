package com.example.demo.consumer;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class Consumer {

    @JmsListener(destination = "queue-1")
    public void receiveQueue(String message){
        System.out.println("queue:"+message);
    }

    @JmsListener(destination = "topic-1")
    public void receiveTopic(String message){
        System.out.println("topic:"+message);
    }
}
