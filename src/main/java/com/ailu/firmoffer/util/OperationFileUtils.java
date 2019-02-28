package com.ailu.firmoffer.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

/**
 * CalcCenter
 * file:SaveFileUtils
 * <p>  文件操作类
 *
 * @author mr.wang
 * @version 2018年03月08日10:39:53 V1.0
 * @par 版权信息：
 * 2018 copyright 河南艾鹿网络科技有限公司 all rights reserved.
 */
@Slf4j
@Component
public class OperationFileUtils {

    @Value("${file.path}")
    private String exPath;

    public String getExPath() {
        return exPath;
    }

    public boolean writeFile(String directoryName, String fileName, String content) {
        Path path = get(getExPath() + directoryName + fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(content);
        } catch (IOException e) {
            log.error("保存文件失败，错误内容为：", e);
            return false;
        }
        log.debug("保存文件成功，目录为{}", path.toString());
        return true;
    }

    public String readFile(String directoryName, String fileName) {
        Path path = get(getExPath() + directoryName + fileName);
        return readFile(path);
    }

    public String readFile(Path path) {
        try {
            return new String(readAllBytes(path));
            //IO流处理 据说效率会高
            //Files.lines(Paths.get("D:\\jd.txt"), StandardCharsets.UTF_8).forEach(System.out::println);
        } catch (IOException e) {
            log.error("读取文件失败，错误内容为", e);
            return null;
        }
    }

}
