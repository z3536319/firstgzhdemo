package com.example.demo.util;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UploadMaterialUtil {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;//字符串存储
    @Autowired
    private RedisTemplate redisTemplate;//对象存储

    public void uploadMedia(File file,String type){
        if(file==null || type==null){
            return;
        }
        String access_token = stringRedisTemplate.opsForValue().get("gzh.access_token");
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("https://api.weixin.qq.com/cgi-bin/media/upload");
            RequestConfig config = RequestConfig.custom().setSocketTimeout(30000).setConnectTimeout(20000).build();
            httpPost.setConfig(config);
            FileBody body = new FileBody(file);
            StringBody token = new StringBody(access_token, ContentType.create("text/plain", Consts.UTF_8));
            StringBody typeBody = new StringBody(type, ContentType.create("text/plain", Consts.UTF_8));
            HttpEntity reqEntity = MultipartEntityBuilder.create().addPart("media",body).addPart("access_token",token).addPart("type", typeBody).build();
            httpPost.setEntity(reqEntity);
            //发起请求 并返回请求的响应
            response = httpClient.execute(httpPost);
            // 判断返回状态是否为200
            if (response.getStatusLine().getStatusCode() == 200) {
                // 解析响应，获取数据
                String content = EntityUtils.toString(response.getEntity(), "UTF-8");
                JSONObject object = JSON.parseObject(content);
                if(object.containsKey("media_id")){
                    String media_id = object.getString("media_id");
                    System.out.println(media_id);
                    if(redisTemplate.hasKey("materialMap")){
                        Map<String, List<String>> map = (Map<String, List<String>>) redisTemplate.opsForValue().get("materialMap");
                        List<String> list = map.get(type.toUpperCase());
                        list.add(media_id);
                        redisTemplate.opsForValue().set("materialMap", map);
                    }else{
                        Map<String, List<String>> map = new HashMap<String, List<String>>();
                        map.put("IMAGE",new ArrayList<String>());
                        map.put("VOICE",new ArrayList<String>());
                        map.put("VIDEO",new ArrayList<String>());
                        map.get(type.toUpperCase()).add(media_id);
                        redisTemplate.opsForValue().set("materialMap", map);
                    }
                    System.out.println("素材上传成功！");
                }
            }else{
                System.out.println("素材上传失败！");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("素材上传失败！");
        } finally {
            try {
                if (response != null) {
                    // 关闭资源
                    response.close();
                }
                // 关闭浏览器
                httpClient.close();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
