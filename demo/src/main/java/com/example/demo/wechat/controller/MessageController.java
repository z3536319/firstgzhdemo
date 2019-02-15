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
        // TODO 接收、处理、响应由微信服务器转发的用户发送给公众帐号的消息
        // 将请求、响应的编码均设置为UTF-8（防止中文乱码）
        String result = "";
        System.out.println("接受请求!");
        try {
            req.setCharacterEncoding("UTF-8");
            resp.setCharacterEncoding("UTF-8");
            Map<String,String> map = MessageUtil.parseXml(req);

            System.out.println("开始构造消息");
            result = MessageUtil.buildXml(map);
            System.out.println(result);

            if(result.equals("")){
                result = "未正确响应";
            }
            resp.getWriter().println(result);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("发生异常："+ e.getMessage());
        }
    }
}
