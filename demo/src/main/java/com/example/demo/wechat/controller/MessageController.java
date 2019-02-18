package com.example.demo.wechat.controller;

import com.example.demo.util.MessageUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/message")
public class MessageController {

    @RequestMapping("/")
    public void chat(HttpServletRequest req, HttpServletResponse resp){

    }
}
