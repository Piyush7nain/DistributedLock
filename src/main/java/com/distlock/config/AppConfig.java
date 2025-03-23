package com.distlock.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "lock")
@Getter
@Setter
public class AppConfig {

    private String strategy = "both"; // redis, zookeeper, or both

    public boolean useRedis() {
        return "redis".equals(strategy) || "both".equals(strategy);
    }

    public boolean useZookeeper() {
        return "zookeeper".equals(strategy) || "both".equals(strategy);
    }
}