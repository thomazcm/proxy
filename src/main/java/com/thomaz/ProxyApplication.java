package com.thomaz;

import com.thomaz.config.Crypto;
import com.thomaz.config.PdfCallbackProperties;
import com.thomaz.config.PdfCompressionProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;


@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties({PdfCompressionProperties.class, PdfCallbackProperties.class})
@PropertySource(value = {
        "classpath:env.properties"
}, ignoreResourceNotFound = true)
public class ProxyApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        Crypto.setCipher();
    }

    public static void main(String[] args) {
        SpringApplication.run(ProxyApplication.class, args);
    }

}
