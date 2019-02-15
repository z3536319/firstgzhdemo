package com.example.demo.test1.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @RequestMapping(value = "/hello",method = {RequestMethod.GET})
    public String test1(){
        return "get test success!";
    }

    @RequestMapping(value = "/hello",method = {RequestMethod.POST})
    public String test2(){
        return "post test success!";
    }
}
