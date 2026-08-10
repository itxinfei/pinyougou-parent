package com.pinyougou.manager.controller;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import entity.Result;
import util.FastDFSClient;

/**
 * 图片上传
 */
@RestController
@RequestMapping("/upload")
public class UploadController {

    private static final Logger logger = Logger.getLogger(UploadController.class);

    @Value("${FILE_SERVER_URL}")
    private String file_server_url;

    @PostMapping("/uploadFile")
    public Result uploadFile(MultipartFile file) {

        // 参数校验
        if (file == null || file.isEmpty()) {
            return new Result(false, "请选择要上传的文件");
        }

        try {
            // 获得文件名:
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                return new Result(false, "文件名不能为空");
            }

            // 获得文件的扩展名:
            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex < 0) {
                return new Result(false, "文件必须包含扩展名");
            }
            String extName = fileName.substring(dotIndex + 1);

            // 创建工具类
            FastDFSClient client = new FastDFSClient("classpath:fastDFS/fdfs_client.conf");

            String path = client.uploadFile(file.getBytes(), extName); // group1/M00/

            String url = file_server_url + ":8888/" + path;//8888查看端口，默认为80,不需要加。

            logger.info("图片服务器地址：" + url);
            return new Result(true, url);
        } catch (Exception e) {
            logger.error("上传图片失败", e);
            return new Result(false, "上传失败！");
        }


    }
}
