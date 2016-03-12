package com.cube.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.cube.conf.ApplicationConfig;

/**
 * 程序入口
 * @author fck
 */
public class CubeRun {

    private static final Logger LOG = LoggerFactory.getLogger(CubeRun.class);
    
    public static final Object forWait = new Object();

    public static void main(String[] args) throws InterruptedException, IOException {
        // 获取jvm名
        String vm = ManagementFactory.getRuntimeMXBean().getName();
        
        if (StringUtils.isBlank(vm)) {
            LOG.error("can't get pid");
            return;
        }
        FileOutputStream out = null;
        try {
            // 将jvm进程id保存到pid文件
            File pid = new File("pid");
            if (pid.exists()) {
                LOG.error("the pid file is exist at {}", pid.getAbsolutePath());
                return;
            }
            out = new FileOutputStream(pid);
            out.write(vm.split("@")[0].getBytes());
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }

        LOG.info("Cube main starting at {}", vm);
        
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ApplicationConfig.class);

        context.start();
        //防止退出
        synchronized (forWait) {
        	forWait.wait();
		}
        context.close();
    }
}
