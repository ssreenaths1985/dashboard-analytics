package com.tarento.analytics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
@EnableRedisRepositories
public class RedisConfig {
	
	@Value("${redis.service.fullpath}")
	private String redisServiceFullPath;
	
	@Value("${redis.service.host}")
	private String redisServiceHost;
	
	@Value("${redis.service.port}")
	private int redisPort;
	
	@Value("${redis.service.databaseNumber}")
	private int redisDatabaseNumber;

    @Bean
    public JedisConnectionFactory connectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(redisServiceHost);
        configuration.setPort(redisPort);
        configuration.setDatabase(redisDatabaseNumber);
        return new JedisConnectionFactory(configuration);
    }

    @Bean
    public RedisTemplate<String, String> template() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        //template.setHashKeySerializer(new JdkSerializationRedisSerializer());
        //template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory factory){
      RedisTemplate redisTemplate = new RedisTemplate();
      redisTemplate.setConnectionFactory(factory);

      //First, we solve the problem of key serialization
      StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
      redisTemplate.setKeySerializer(stringRedisSerializer);

      //Solve the serialization problem of value
      Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
      redisTemplate.setValueSerializer(stringRedisSerializer);
      redisTemplate.setHashKeySerializer(new StringRedisSerializer());
      redisTemplate.setHashValueSerializer(new StringRedisSerializer());
      return redisTemplate;
    }

}
