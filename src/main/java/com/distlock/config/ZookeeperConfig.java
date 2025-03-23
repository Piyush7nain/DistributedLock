package com.distlock.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "zookeeper")
@Getter
@Setter
public class ZookeeperConfig {

    private String connectionString = "localhost:2181";
    private int sessionTimeout = 60000;
    private int connectionTimeout = 15000;
    private int retryTimes = 3;
    private int retryInterval = 1000;

    @Getter
    @Setter
    private Lock lock = new Lock();

    public static class Lock {
        private String basePath = "/locks";
        private long waitTime = 30000;

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }

        public long getWaitTime() {
            return waitTime;
        }

        public void setWaitTime(long waitTime) {
            this.waitTime = waitTime;
        }
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    public CuratorFramework curatorClient() {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(retryInterval, retryTimes);
        return CuratorFrameworkFactory.builder()
                .connectString(connectionString)
                .sessionTimeoutMs(sessionTimeout)
                .connectionTimeoutMs(connectionTimeout)
                .retryPolicy(retryPolicy)
                .build();
    }
}