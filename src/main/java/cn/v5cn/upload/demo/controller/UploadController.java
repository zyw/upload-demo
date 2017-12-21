package cn.v5cn.upload.demo.controller;

import cn.v5cn.upload.demo.client.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class UploadController {

    @PostMapping("/upload/file")
    public Map<String,String> upload(MultipartFile file, HttpServletRequest request) throws IOException {

        Client client = new Client();
        FormUploader formUploader = new FormUploader(client,"http://localhost:9969/file/server/upload",file.getBytes()
                ,"file"
                ,file.getOriginalFilename());
        /*StreamUploader streamUploader = new StreamUploader(client
                ,"http://localhost:9969/file/server/upload"
                ,file.getInputStream(),null,null,null);
        Response response = streamUploader.upload();*/
                /*client.multipartPost("http://localhost:9969/file/server/upload",
                "file",
                file.getOriginalFilename(),
                file.getBytes());*/
        Response response = formUploader.upload();
        int status = response.statusCode;
        if(response.isOK()) {
            System.out.println("=------------------------OK OK OK ----------------------------------" + status);
        }
        StringMap stringMap = response.jsonToMap();
        stringMap.forEach((key,value) -> {
            System.out.println(key + " : " + value);
        });
        Map<String,String> result = new HashMap<>();
        result.put("message","success");
        result.put("code","200");
        return result;
    }
}
