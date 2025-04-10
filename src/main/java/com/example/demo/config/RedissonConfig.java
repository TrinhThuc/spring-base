package com.example.demo.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://162.248.102.236:6379")
                .setConnectionMinimumIdleSize(5)
                .setConnectionPoolSize(10)
                .setTimeout(3000);
        return Redisson.create(config);
    }
}

