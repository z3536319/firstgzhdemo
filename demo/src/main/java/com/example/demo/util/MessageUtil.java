package com.example.demo.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MessageUtil {

    private static StringRedisTemplate stringRedisTemplate;//字符串存储
    private static RedisTemplate redisTemplate;//对象存储
    private static String ak;

    @Autowired
    public MessageUtil(StringRedisTemplate stringRedisTemplate, RedisTemplate redisTemplate){
        MessageUtil.stringRedisTemplate = stringRedisTemplate;
        MessageUtil.redisTemplate = redisTemplate;
    }

    /**
     * 解析微信发来的请求（XML）
     * @param request
     * @return map
     * @throws Exception
     */
    public static Map<String,String> parseXml(HttpServletRequest request) throws Exception {
        // 将解析结果存储在HashMap中
        Map<String,String> map = new HashMap();
        // 从request中取得输入流
        InputStream inputStream = request.getInputStream();
        // 读取输入流
        SAXReader reader = new SAXReader();
        // 防止XXE
        try {
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Document document = reader.read(inputStream);
        // 得到xml根元素
        Element root = document.getRootElement();
        // 得到根元素的所有子节点
        List<Element> elementList = root.elements();

        // 遍历所有子节点
        for (Element e : elementList) {
            System.out.println(e.getName() + "|" + e.getText());
            map.put(e.getName(), e.getText());
        }

        // 释放资源
        inputStream.close();
        inputStream = null;
        return map;
    }

    /**
     * 根据消息类型 构造返回消息
     */
    public static String buildXml(Map<String,String> map) {
        String result;
        String msgType = map.get("MsgType").toString();
        System.out.println("MsgType:" + msgType);
        Map<String,List<String>> materialMap = (Map<String,List<String>>) redisTemplate.opsForValue().get("materialMap");
        if(msgType.toUpperCase().equals("TEXT")){
            String content = map.get("Content").toString().trim();
            if("音乐".equals(content)){
                result = buildMusicMessage(map);
            }else if("图文".equals(content)){
                result = buildNewsMessage(map);
            }else if(content.trim().startsWith("天气:")){
                String city = content.trim().replace("天气:", "");
                result = buildWeatherMessage(map, city);
            }else{
                result = buildTextMessage(map, "Cherry的小小窝, 请问客官想要点啥?");
            }
        }else if (msgType.toUpperCase().equals("IMAGE")){
            List<String> list = materialMap.get(msgType.toUpperCase());
            result = buildImageMessage(map,list.get(list.size()-1));
        }else if (msgType.toUpperCase().equals("VOICE")){
            List<String> list = materialMap.get(msgType.toUpperCase());
            result = buildVoiceMessage(map,list.get(list.size()-1));
        }else if (msgType.toUpperCase().equals("VIDEO")){
            List<String> list = materialMap.get(msgType.toUpperCase());
            result = buildVideoMessage(map,list.get(list.size()-1));
        }else{
            String fromUserName = map.get("FromUserName");
            // 开发者微信号
            String toUserName = map.get("ToUserName");
            result = String
                    .format(
                            "<xml>" +
                                    "<ToUserName><![CDATA[%s]]></ToUserName>" +
                                    "<FromUserName><![CDATA[%s]]></FromUserName>" +
                                    "<CreateTime>%s</CreateTime>" +
                                    "<MsgType><![CDATA[text]]></MsgType>" +
                                    "<Content><![CDATA[%s]]></Content>" +
                                    "</xml>",
                            fromUserName, toUserName, getUtcTime(),
                            "请回复如下关键词：\n文本\n图片\n语音\n视频\n音乐\n图文");
        }

        return result;
    }

    /**
     * 构造文本消息
     *
     * @param map
     * @param content
     * @return
     */
    private static String buildTextMessage(Map<String,String> map, String content) {
        //发送方帐号
        String fromUserName = map.get("FromUserName");
        // 开发者微信号
        String toUserName = map.get("ToUserName");
        /**
         * 文本消息XML数据格式
         */
        return String.format(
                "<xml>" +
                        "<ToUserName><![CDATA[%s]]></ToUserName>" +
                        "<FromUserName><![CDATA[%s]]></FromUserName>" +
                        "<CreateTime>%s</CreateTime>" +
                        "<MsgType><![CDATA[text]]></MsgType>" +
                        "<Content><![CDATA[%s]]></Content>" + "</xml>",
                fromUserName, toUserName, getUtcTime(), content);
    }

    /**
     *  构建图片消息
     * @param map
     * @param media_id
     * @return
     */
    private static String buildImageMessage(Map<String, String> map, String media_id) {
        String fromUserName = map.get("FromUserName");
        String toUserName = map.get("ToUserName");

        /*返回用户发过来的图片*/
       // String media_id = map.get("MediaId");
        return String.format(
                "<xml>" +
                        "<ToUserName><![CDATA[%s]]></ToUserName>" +
                        "<FromUserName><![CDATA[%s]]></FromUserName>" +
                        "<CreateTime>%s</CreateTime>" +
                        "<MsgType><![CDATA[image]]></MsgType>" +
                        "<Image>" +
                        "   <MediaId><![CDATA[%s]]></MediaId>" +
                        "</Image>" +
                        "</xml>",
                fromUserName,toUserName, getUtcTime(),media_id
        );
    }

    /**
     * 构造语音消息
     * @param map
     * @return
     */
    private static String buildVoiceMessage(Map<String, String> map, String media_id) {
        String fromUserName = map.get("FromUserName");
        String toUserName = map.get("ToUserName");
        /*返回用户发过来的语音*/
        //String media_id = map.get("MediaId");
        return String.format(
                "<xml>" +
                        "<ToUserName><![CDATA[%s]]></ToUserName>" +
                        "<FromUserName><![CDATA[%s]]></FromUserName>" +
                        "<CreateTime>%s</CreateTime>" +
                        "<MsgType><![CDATA[voice]]></MsgType>" +
                        "<Voice>" +
                        "   <MediaId><![CDATA[%s]]></MediaId>" +
                        "</Voice>" +
                        "</xml>",
                fromUserName,toUserName, getUtcTime(),media_id
        );
    }

    /**
     * 回复视频消息
     * @param map
     * @return
     */
    private static String buildVideoMessage(Map<String, String> map, String media_id) {
        String fromUserName = map.get("FromUserName");
        String toUserName = map.get("ToUserName");
        String title = "客官发过来的视频哟~~";
        String description = "客官您呐,现在肯定很开心,对不啦 嘻嘻?";
        /*返回用户发过来的视频*/
        //String media_id = map.get("MediaId");
        return String.format(
                "<xml>" +
                        "<ToUserName><![CDATA[%s]]></ToUserName>" +
                        "<FromUserName><![CDATA[%s]]></FromUserName>" +
                        "<CreateTime>%s</CreateTime>" +
                        "<MsgType><![CDATA[video]]></MsgType>" +
                        "<Video>" +
                        "   <MediaId><![CDATA[%s]]></MediaId>" +
                        "   <Title><![CDATA[%s]]></Title>" +
                        "   <Description><![CDATA[%s]]></Description>" +
                        "</Video>" +
                        "</xml>",
                fromUserName,toUserName, getUtcTime(),media_id,title,description
        );
    }

    /**
     * 回复音乐消息
     * @param map
     * @return
     */
    private static String buildMusicMessage(Map<String, String> map) {
        String fromUserName = map.get("FromUserName");
        String toUserName = map.get("ToUserName");
        String title = "Victory";
        String description = "多听音乐 心情棒棒 嘻嘻?";
        String hqMusicUrl ="http://www.kugou.com/song/frf6h55.html?frombaidu#hash=C5D9872A390CB7ABB971409C9062CAFD&album_id=0";
        return String.format(
                "<xml>" +
                        "<ToUserName><![CDATA[%s]]></ToUserName>" +
                        "<FromUserName><![CDATA[%s]]></FromUserName>" +
                        "<CreateTime>%s</CreateTime>" +
                        "<MsgType><![CDATA[music]]></MsgType>" +
                        "<Music>" +
                        "   <Title><![CDATA[%s]]></Title>" +
                        "   <Description><![CDATA[%s]]></Description>" +
                        "   <MusicUrl>< ![CDATA[%s] ]></MusicUrl>" +  //非必须项 音乐链接
                        "   <HQMusicUrl><![CDATA[%s]]></HQMusicUrl>"+ //非必须项 高质量音乐链接，WIFI环境优先使用该链接播放音乐
                        "</Music>" +
                        "</xml>",
                fromUserName,toUserName, getUtcTime(),title,description,hqMusicUrl,hqMusicUrl
        );
    }

    /**
     * 返回图文消息
     * @param map
     * @return
     */
    private static String buildNewsMessage(Map<String, String> map) {
        String fromUserName = map.get("FromUserName");
        String toUserName = map.get("ToUserName");
        String title1 = "HAP审计的实现和使用";
        String description1 = "由于HAP框架用的是Spring+SpringMVC+Mybatis";
        String picUrl1 ="http://upload-images.jianshu.io/upload_images/7855203-b9e9c9ded8a732a1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240";
        String textUrl1 = "http://blog.csdn.net/a1786223749/article/details/78330890";

        String title2 = "KendoUI之Grid的问题详解";
        String description2 = "kendoLov带出的值出现 null和undefined";
        String picUrl2 ="https://demos.telerik.com/kendo-ui/content/shared/images/theme-builder.png";
        String textUrl2 = "http://blog.csdn.net/a1786223749/article/details/78330908";

        return String.format(
                "<xml>" +
                        "<ToUserName><![CDATA[%s]]></ToUserName>" +
                        "<FromUserName><![CDATA[%s]]></FromUserName>" +
                        "<CreateTime>%s</CreateTime>" +
                        "<MsgType><![CDATA[news]]></MsgType>" +
                        "<ArticleCount>2</ArticleCount>" + //图文消息个数，限制为8条以内
                        "<Articles>" + //多条图文消息信息，默认第一个item为大图,注意，如果图文数超过8，则将会无响应
                        "<item>" +
                        "<Title><![CDATA[%s]]></Title> " +
                        "<Description><![CDATA[%s]]></Description>" +
                        "<PicUrl><![CDATA[%s]]></PicUrl>" + //图片链接，支持JPG、PNG格式，较好的效果为大图360*200，小图200*200
                        "<Url><![CDATA[%s]]></Url>" + //点击图文消息跳转链接
                        "</item>" +
                        "<item>" +
                        "<Title><![CDATA[%s]]></Title>" +
                        "<Description><![CDATA[%s]]></Description>" +
                        "<PicUrl><![CDATA[%s]]]></PicUrl>" +
                        "<Url><![CDATA[%s]]]></Url>" +
                        "</item>" +
                        "</Articles>" +
                        "</xml>"
                ,
                fromUserName,toUserName, getUtcTime(),
                title1,description1,picUrl1,textUrl1,
                title2,description2,picUrl2,textUrl2
        );
    }

    //回复天气信息
    private static String buildWeatherMessage(Map<String, String> map, String city) {
        //发送方帐号
        String fromUserName = map.get("FromUserName");
        // 开发者微信号
        String toUserName = map.get("ToUserName");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(50000).setConnectionRequestTimeout(10000).setSocketTimeout(50000).build();
        // 创建HttpGet请求，相当于在浏览器输入地址
        HttpGet httpGet = new HttpGet("http://api.map.baidu.com/telematics/v3/weather?location="+city+"&output=json&ak="+ak);
        httpGet.setConfig(requestConfig);
        CloseableHttpResponse response = null;
        StringBuffer sb = new StringBuffer();
        try {
            // 执行请求，相当于敲完地址后按下回车。获取响应
            response = httpclient.execute(httpGet);
            // 判断返回状态是否为200
            if (response.getStatusLine().getStatusCode() == 200) {
                // 解析响应，获取数据
                String content = EntityUtils.toString(response.getEntity(), "UTF-8");
                JSONObject object = JSON.parseObject(content);
                String status = object.getString("status");
                if("success".equals(status)){
                    JSONArray array = object.getJSONArray("results");
                    JSONObject object1 = array.getJSONObject(0);
                    sb.append(object1.getString("currentCity")).append("\npm2.5：").append(object1.getString("pm25")).append("\n");
                    JSONArray weatherData = object1.getJSONArray("weather_data");
                    JSONObject object2 = weatherData.getJSONObject(0);
                    sb.append(object2.getString("date")).append("\n今日天气实况：天气：").append(object2.getString("weather"))
                    .append("；气温：").append(object2.getString("temperature")).append("；风向/风力：").append(object2.getString("wind")).append("\n");
                    JSONArray index = object1.getJSONArray("index");
                    JSONObject object3 = null;
                    for (int i=0;i<index.size();i++){
                        object3 = index.getJSONObject(i);
                        sb.append(object3.getString("tipt")).append("：").append(object3.getString("zs")).append("，").append(object3.getString("des")).append("\n");
                    }
                }
            }
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
        /**
         * 文本消息XML数据格式
         */
        if(sb.length()==0){
            sb.append("查询结果为空。");
        }
        return String.format(
                "<xml>" +
                        "<ToUserName><![CDATA[%s]]></ToUserName>" +
                        "<FromUserName><![CDATA[%s]]></FromUserName>" +
                        "<CreateTime>%s</CreateTime>" +
                        "<MsgType><![CDATA[text]]></MsgType>" +
                        "<Content><![CDATA[%s]]></Content>" + "</xml>",
                fromUserName, toUserName, getUtcTime(), sb.toString());
    }

    private static String getUtcTime() {
        Date dt = new Date();// 如果不需要格式,可直接用dt,dt就是当前系统时间
        DateFormat df = new SimpleDateFormat("yyyyMMddhhmm");// 设置显示格式
        String nowTime = df.format(dt);
        long dd = (long) 0;
        try {
            dd = df.parse(nowTime).getTime();
        } catch (Exception e) {

        }
        return String.valueOf(dd);
    }

    @Value("${baidu.weather.ak}")
    public void setAk(String ak){
        MessageUtil.ak = ak;
    }

}
