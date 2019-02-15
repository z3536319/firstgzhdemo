package com.example.demo.scheduled.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Calendar;
import java.util.Date;

@Component
public class AccessTokenTask {

    @Value("${wx.gzh.appid}")
    private String appId;
    @Value("${wx.gzh.appsecret}")
    private String secret;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;//字符串存储
    @Autowired
    private RedisTemplate redisTemplate;//对象存储

    @Scheduled(initialDelay=1000, fixedDelay=7200*1000)
    private void getAccessToken(){
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.MINUTE, 110);
        StringBuffer sb = new StringBuffer();
        sb.append("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=").append(appId).append("&secret=").append(secret);
        // 创建Httpclient对象,相当于打开了浏览器
        CloseableHttpClient httpclient = HttpClients.createDefault();
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(50000).setConnectionRequestTimeout(10000).setSocketTimeout(50000).build();

        // 创建HttpGet请求，相当于在浏览器输入地址
        HttpGet httpGet = new HttpGet(sb.toString());
        httpGet.setConfig(requestConfig);

        CloseableHttpResponse response = null;
        while (new Date().getTime()<c.getTime().getTime()){
            try {
                // 执行请求，相当于敲完地址后按下回车。获取响应
                response = httpclient.execute(httpGet);
                // 判断返回状态是否为200
                if (response.getStatusLine().getStatusCode() == 200) {
                    // 解析响应，获取数据
                    String content = EntityUtils.toString(response.getEntity(), "UTF-8");
                    JSONObject object = JSON.parseObject(content);
                    if(object.containsKey("access_token")){
                        String accessToken = object.getString("access_token");
                        System.out.println(accessToken);
                        this.stringRedisTemplate.opsForValue().set("gzh.access_token", accessToken);
                        break;
                    }
                }
                Thread.sleep(5000);
            } catch (Exception e){
                e.printStackTrace();
            } finally {
                try {
                    if (response != null) {
                        // 关闭资源
                        response.close();
                    }
                    // 关闭浏览器
                    httpclient.close();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
