package com.luke.pingclient.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class FileLockLimiter {

    private static final int MAX_REQUESTS_PER_SECOND = 2; // 全局限速 2 RPS
    private static final String LOCK_FILE_PATH = "rate_limit.lock"; // 锁文件路径
    private final File lockFile;

    public FileLockLimiter() throws Exception {
        this.lockFile = new File(LOCK_FILE_PATH);
        if (!lockFile.exists()) {
            lockFile.createNewFile();
        }
    }

    public boolean tryAcquireLock() {
        try (FileChannel channel = new RandomAccessFile(LOCK_FILE_PATH, "rw").getChannel();
             FileLock lock = channel.tryLock()) {
            if (lock != null) {
                // 获取当前时间戳（秒级）
                String currentSecond = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                // 读取文件内容
                ByteBuffer buffer = ByteBuffer.allocate(64); // 假设文件最多64字节
                channel.position(0); // 从文件开头读取
                int bytesRead = channel.read(buffer);
                buffer.flip();

                String fileContent = (bytesRead > 0) ? new String(buffer.array(), 0, bytesRead).trim() : "";
                String[] lines = fileContent.isEmpty() ? new String[0] : fileContent.split("\n");

                String fileSecond = (lines.length > 0) ? lines[0].trim() : "";
                int writeCounter = (lines.length > 1) ? Integer.parseInt(lines[1].trim()) : 0;

                // 判断当前秒内是否超过限速
                if (!fileSecond.isEmpty() && currentSecond.equals(fileSecond)) {
                    if (writeCounter >= MAX_REQUESTS_PER_SECOND) {
                        return false; // 超过限速
                    } else {
                        writeCounter++;
                    }
                } else {
                    writeCounter = 1; // 新的秒内首次访问
                }

                // 更新文件：重置计数器和时间戳
                channel.truncate(0); // 清空文件
                channel.position(0);
                String updatedContent = currentSecond + "\n" + writeCounter;
                channel.write(ByteBuffer.wrap(updatedContent.getBytes()));
                return true; // 请求被允许
            }
        } catch (IOException | NumberFormatException e) {
            log.error("tryAcquireLock exception: " + e.toString());
        }
        log.info("lock acquired by another process, u lose: " + ProcessHandle.current().pid());
        return false;
    }
}

