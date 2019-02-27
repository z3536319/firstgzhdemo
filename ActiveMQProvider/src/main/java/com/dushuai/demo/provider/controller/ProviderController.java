package com.dushuai.demo.provider.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.jms.Destination;

@RestController
@RequestMapping("/test")
public class ProviderController {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Resource(name = "queue1")
    private Destination queue1;

    @Resource(name = "topic1")
    private Destination topic1;

    @RequestMapping("/queue")
    public String test1(String message){
        jmsTemplate.convertAndSend(queue1, message);
        return "ok";
    }

    @RequestMapping("/topic")
    public String test2(String message){
        jmsTemplate.convertAndSend(topic1, message);
        return "ok";
    }

}
