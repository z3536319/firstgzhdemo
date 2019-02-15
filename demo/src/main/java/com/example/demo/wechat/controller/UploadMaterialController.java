package com.example.demo.wechat.controller;

import com.example.demo.util.UploadMaterialUtil;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/upload")
public class UploadMaterialController {

    @Autowired
    private UploadMaterialUtil uploadMaterialUtil;

    @RequestMapping(value = "/file",method = {RequestMethod.POST})
    public String upload(@RequestParam(required = true) MultipartFile file, String type){
        try {
            if(file!=null && type!=null){
                File convFile = new File(System.getProperty("java.io.tmpdir")+"/"+file.getOriginalFilename());
                file.transferTo(convFile);
                uploadMaterialUtil.uploadMedia(convFile, type);
                convFile.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "上传成功";
    }
}
