package com.womai.hystrixDemo.controller;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * Created by cofco on 2019/3/20.
 */

@RestController
@RequestMapping("/demo")
public class DemoController {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = "fetch", method = RequestMethod.GET)
    @HystrixCommand(
            fallbackMethod = "fetchFallBack",
            threadPoolProperties = {//10个核心线程池,超过20个的队列外的请求被拒绝; 当一切都是正常的时候，线程池一般仅会有1到2个线程激活来提供服务
                    @HystrixProperty(name = "coreSize", value = "10"),
                    @HystrixProperty(name = "maxQueueSize", value = "100"),
                    @HystrixProperty(name = "queueSizeRejectionThreshold", value = "20"),

            },
            commandProperties = {
                    //命令执行超时时间
                @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "10000"),
                    //若干10s一个窗口内失败三次, 则达到触发熔断的最少请求量
                @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "2"),
                    //断路30s后尝试执行, 默认为5s
                @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "50000")
            }
    )
    public String fetchUrlData() {

        BufferedReader in = null;
        StringBuffer result = null;
        try {

//            CookieManager manager = new CookieManager();
//            CookieHandler.setDefault(manager);

            URL url = new URL("http://m.womai.com");
            URLConnection conn = url.openConnection();

            // 设置请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            conn.setRequestProperty("Cookie", "cityCode=31000");

            conn.connect();

            Map<String, List<String>> headers = conn.getHeaderFields();

            for (String headerKey : headers.keySet()) {
                logger.info(headerKey, "-->", headers.get(headerKey));
            }

            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            result = new StringBuffer();
            String line;
            while((line = in.readLine())!=null){
                result.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(in!=null){
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result==null?null:result.toString();
    }

    private String fetchFallBack(){
        logger.error("===================== 执行降级策略");
        return "===================== 执行降级策略";
    }

}
