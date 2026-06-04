package com.ddd.admin.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Kaptcha 验证码配置
 */
@Configuration
public class KaptchaConfig {

    @Bean
    public DefaultKaptcha defaultKaptcha() {
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        Properties properties = new Properties();

        // 图片宽度
        properties.setProperty("kaptcha.image.width", "130");
        // 图片高度
        properties.setProperty("kaptcha.image.height", "48");
        // 字符集
        properties.setProperty("kaptcha.textproducer.char.string",
                "ABCDEFGHJKLMNPQRSTUVWXYZ23456789");
        // 字符数量
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        // 字符间距
        properties.setProperty("kaptcha.textproducer.char.space", "4");
        // 字体大小
        properties.setProperty("kaptcha.textproducer.font.size", "36");
        // 字体颜色
        properties.setProperty("kaptcha.textproducer.font.color", "0,100,180");
        // 噪音线颜色
        properties.setProperty("kaptcha.noise.color", "180,180,180");
        // 边框
        properties.setProperty("kaptcha.border", "yes");
        properties.setProperty("kaptcha.border.color", "200,200,200");
        // 背景渐变
        properties.setProperty("kaptcha.background.clear.from", "245,250,255");
        properties.setProperty("kaptcha.background.clear.to", "white");
        // 干扰实现类
        properties.setProperty("kaptcha.noise.impl",
                "com.google.code.kaptcha.impl.DefaultNoise");
        // 图片效果（水纹）
        properties.setProperty("kaptcha.obscurificator.impl",
                "com.google.code.kaptcha.impl.WaterRipple");

        Config config = new Config(properties);
        kaptcha.setConfig(config);
        return kaptcha;
    }
}
