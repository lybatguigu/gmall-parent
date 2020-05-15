package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import org.apache.commons.io.FilenameUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author Liyanbao
 * @create 2020-04-20 0:25
 */
@RestController
@RequestMapping("admin/product/")
public class FileUploadController {

    //http://api.gmall.com/admin/product/fileUpload
    // 文件上传完毕以后返回一个文件的地址
    // 配置服务器的ip地址  在配置文件dev中,实现了软编码
    @Value("${fileServer.url}")
    private String fileUrl; //http://192.168.200.128:8080/

    /**
     *
     * @param file 用户点击的图片文件
     * @return
     */
    @RequestMapping("fileUpload")
    public Result fileUpload(MultipartFile file) throws IOException, MyException {
        //获取resourse目录下的tracker.conf
        String configFile = this.getClass().getResource("/tracker.conf").getFile();
        //声明图片返回的路径
        String path = null;
        if (configFile != null) {
            //初始化文件
            ClientGlobal.init(configFile);
            //文件上传需要 tracker 和 storage
            TrackerClient trackerClient = new TrackerClient();
            //获取trackerServer
            TrackerServer trackerServer = trackerClient.getConnection();
            //获取storageClient
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);
            //上传文件
            //第一个参数：上传文件 字节数组，第二个参数：文件的后缀名 ，第三个参数：数组 null
            path = storageClient1.upload_appender_file1(file.getBytes(), FilenameUtils.getExtension(file.getOriginalFilename()), null);
            //上传完成之后要获取到文件上传的路径
            System.out.println("图片路径:" + fileUrl + path);
        }
        return Result.ok(fileUrl + path);
    }
}
