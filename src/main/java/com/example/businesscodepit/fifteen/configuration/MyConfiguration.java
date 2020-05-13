package com.example.businesscodepit.fifteen.configuration;

import com.example.businesscodepit.fifteen.EnumDeserializer;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/13
 * 修改时间：
 *
 * @author yaoyong
 **/
@Configuration
public class MyConfiguration {
    /**
     * 以下方法自定义redisTemplate 可以按照COntroller案例进行数据读取,但是需要注意获取到的
     * value值是LinkedHashMap类型,如果需要自动格式化为存储时value的类型可以通过如下方式
     *      1.设置Jackson2JsonRedisSerializer,说明将类型信息作为属性写入序列化后的数据中
     *         Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
     *         ObjectMapper objectMapper = new ObjectMapper();
     *         objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
     *         jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
     *      2.使用已有的
     *         redisTemplate.setKeySerializer(RedisSerializer.string());
     *         redisTemplate.setValueSerializer(RedisSerializer.json());
     *         redisTemplate.setHashKeySerializer(RedisSerializer.string());
     *         redisTemplate.setHashValueSerializer(RedisSerializer.json());
     *
     *         其实使用的就是这个序列化器:GenericJackson2JsonRedisSerializer(实现与自己写的类似)
     *      设置之后,value会多一个参数@class 表明value序列化前的类名 以供反序列化
     * @param redisConnectionFactory
     * @param <T>
     * @return
     */
    @Bean
    public <T> RedisTemplate<String, T> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, T> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);

        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(RedisSerializer.json());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        redisTemplate.setHashValueSerializer(RedisSerializer.json());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 设置 如果写入的是枚举类,那么用index的值代替
     *      自定义 ObjectMapper 启用 WRITE_ENUMS_USING_INDEX 序列化功能特性时，
     *      覆盖了 Spring Boot 自动创建的 ObjectMapper；
     *      而这个自动创建的 ObjectMapper 设置过 FAIL_ON_UNKNOWN_PROPERTIES
     *      反序列化特性为 false，以确保出现未知字段时不要抛出异常
     *
     * 测试   枚举作为 API 接口参数或返回值的两个大坑  先关掉
     * @return
     */
//    @Bean
//    public ObjectMapper objectMapper(){
//        ObjectMapper objectMapper=new ObjectMapper();
//        objectMapper.configure(SerializationFeature.WRITE_ENUMS_USING_INDEX,true);
//        return objectMapper;
//    }

    @Bean
     public RestTemplate restTemplate(MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter) {
         return new RestTemplateBuilder()
                        .additionalMessageConverters(mappingJackson2HttpMessageConverter)
                       .build();
     }

    /**
     * 解决枚举反序列化时  如果自定义字段为int类型  导致反序列化时并不是根据字段进行反序列化的
     * 而是按照值取索引得到的枚举问题
     * @return
     */
    @Bean
    public Module enumModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Enum.class, new EnumDeserializer());
        return module;
    }
}
