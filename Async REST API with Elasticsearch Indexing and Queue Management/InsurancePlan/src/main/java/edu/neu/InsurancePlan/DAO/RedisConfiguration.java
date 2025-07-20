/**
 * 
 */
package edu.neu.InsurancePlan.DAO;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.JedisPooled;


@Configuration
public class RedisConfiguration {



    private static ConnectionPoolConfig poolConfig;

    public static void configure() {
        if (poolConfig == null) {
            poolConfig = buildPoolConfig();
        }
    }

    public static JedisPooled getResources() {
        if (poolConfig == null)
            configure();
        return new JedisPooled(poolConfig, "localhost", 6379);
    }
    
    
    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration standaloneConf = new RedisStandaloneConfiguration();
        standaloneConf.setHostName("localhost");
        standaloneConf.setPort(6379);
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(standaloneConf);
        return  jedisConnectionFactory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(jedisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setEnableTransactionSupport(true);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }


    private static ConnectionPoolConfig buildPoolConfig() {
        final ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        return poolConfig;
    }
}