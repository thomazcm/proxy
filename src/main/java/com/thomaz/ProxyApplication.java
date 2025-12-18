package com.thomaz;

import com.thomaz.config.PdfCompressionProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

import java.util.TimeZone;


@SpringBootApplication
@EnableConfigurationProperties(PdfCompressionProperties.class)
@PropertySource(value = {
        "classpath:env.properties"
}, ignoreResourceNotFound = true)
public class ProxyApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
    }

    public static void main(String[] args) {
        SpringApplication.run(ProxyApplication.class, args);
    }

}
