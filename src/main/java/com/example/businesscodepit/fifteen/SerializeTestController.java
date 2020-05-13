package com.example.businesscodepit.fifteen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/13
 * 修改时间：
 * Spring 提供的 4 种 RedisSerializer(Redis 序列化器)
 *      默认情况下，RedisTemplate 使用 JdkSerializationRedisSerializer，
 *          也就是 JDK 序列化，容易产生 Redis 中保存了乱码的错觉。
 *          通常考虑到易读性，可以设置 Key 的序列化器为 StringRedisSerializer。
 *     但直接使用 RedisSerializer.string()，
 *          相当于使用了 UTF_8 编码的 StringRedisSerializer，
 *          需要注意字符集问题。
 *     如果希望 Value 也是使用 JSON 序列化的话，
 *          可以把 Value 序列化器设置为 Jackson2JsonRedisSerializer。
 *          默认情况下，不会把类型信息保存在 Value 中，
 *          即使我们定义 RedisTemplate 的 Value 泛型为实际类型，
 *          查询出的 Value 也只能是 LinkedHashMap 类型。
 *     如果希望直接获取真实的数据类型，你可以启用 Jackson ObjectMapper
 *          的 activateDefaultTyping 方法，把类型信息一起序列化保存在 Value 中。
 *     如果希望 Value 以 JSON 保存并带上类型信息，更简单的方式是，
 *          直接使用 RedisSerializer.json() 快捷方法来获取序列化器。
 * 反序列化
 *      反序列化默认调用无参构造器,如果需要使用指定的构造器进行反序列化可以使用
 *      @JsonCreator(标注在指定的构造方法上)
 *      @JsonProperty(标注构造方法的参数 用于指定反序列化是将哪个属性反射到该入参)
 * 枚举作为 API 接口参数或返回值的两个大坑
 *      1.第一个坑是，客户端和服务端的枚举定义不一致时，会出异常
 *          服务端返回的枚举在客户端没有定义导致进行反序列化的时候报错
 *              1.spring.jackson.deserialization.read_unknown_enum_values_using_default_value=true
 *              2.在客户端枚举上添加@JsonEnumDefaultValue 标明默认值
 *              3.配置 RestTemplate，来使用 Spring Boot 的 MappingJackson2HttpMessageConverter
 *                @Bean
 *                public RestTemplate restTemplate(MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter) {
 *                    return new RestTemplateBuilder()
 *                    .additionalMessageConverters(mappingJackson2HttpMessageConverter)
 *                    .build();
 *                }
 *      2.枚举序列化反序列化实现自定义的字段非常麻烦，会涉及 Jackson 的 Bug
 *          queryOrdersByStatusList
 *      3.RedisTemplate<String, Long>
 *          redis返回的值反序列化后  如果值在Integer区间内返回的是Integer，超过这个区间返回Long
 * @author yaoyong
 **/
@Slf4j
@RestController
@RequestMapping("/serialize")
public class SerializeTestController {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RedisTemplate<String, User> userRedisTemplate;
    @Autowired
    private RedisTemplate<String, Long> longRedisTemplate;
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 初始化  向redis中插入数据
     * @throws JsonProcessingException
     */
    @PostConstruct
    public void init() throws JsonProcessingException {
        redisTemplate.opsForValue().set("redisTemplate", new User("zhuye", 36));
        stringRedisTemplate.opsForValue().set("stringRedisTemplate", objectMapper.writeValueAsString(new User("zhuye", 36)));
    }

    /**
     * redisTemplate与stringRedisTemplate
     *     默认情况下 RedisTemplate 针对 Key 和 Value 使用了 JDK 序列化
     *     StringRedisTemplate 对于 Key 和 Value，使用的是 String 序列化方式，Key 和 Value 只能是 String
     *     因此以下方式获取redis中的数据都获取不到,因为RedisTemplate会把key使用了 JDK 序列化
     *          StringRedisTemplate会把key进行String 序列化方式 因此获取不到redis中的数据
     *
     *
     */
    @GetMapping("/test")
    public void test() throws JsonProcessingException {
        log.info("redisTemplate get {}", redisTemplate.opsForValue().get("stringRedisTemplate"));
        log.info("stringRedisTemplate get {}", stringRedisTemplate.opsForValue().get("redisTemplate"));

        //使用RedisTemplate获取Value，无需反序列化就可以拿到实际对象，虽然方便，但是Redis中保存的Key和Value不易读
        User userFromRedisTemplate = (User) redisTemplate.opsForValue().get("redisTemplate");
        log.info("redisTemplate get {}", userFromRedisTemplate);

        //使用StringRedisTemplate，虽然Key正常，但是Value存取需要手动序列化成字符串
        User userFromStringRedisTemplate = objectMapper.readValue(stringRedisTemplate.opsForValue().get("stringRedisTemplate"), User.class);
        log.info("stringRedisTemplate get {}", userFromStringRedisTemplate);
    }

    /**
     * 使用 RedisTemplate 获取 Value 虽然方便，但是 Key 和 Value 不易读；
     * 而使用 StringRedisTemplate 虽然 Key 是普通字符串，但是 Value 存取需要手动序列化成字符串，有没有两全其美的方式呢？
     *
     *      1.userRedisTemplate 获取到的 Value，是 LinkedHashMap 类型的
     */

    @GetMapping("testSelfRedisTemplate")
    public void testSelfRedisTemplate() {
        User user = new User("zhuye", 36);
        userRedisTemplate.opsForValue().set(user.getName(), user);
        Object userFromRedis = userRedisTemplate.opsForValue().get(user.getName());
        log.info("userRedisTemplate get {} {}", userFromRedis, userFromRedis.getClass());
        log.info("stringRedisTemplate get {}", stringRedisTemplate.opsForValue().get(user.getName()));
        //  测试反序列化指定构造器
        User person = new User("zhuye-construct");
        person.setAge(12);
        userRedisTemplate.opsForValue().set(person.getName(), person);
        Object userConstructFromRedis = userRedisTemplate.opsForValue().get(person.getName());
        log.info("userRedisTemplate-construct get {} {}", userConstructFromRedis, userConstructFromRedis.getClass());
        log.info("stringRedisTemplate-construct get {}", stringRedisTemplate.opsForValue().get(person.getName()));
    }

    @GetMapping("testLongRedisTemplate")
    public void testLongRedisTemplate(){
        String key = "testCounter";
        longRedisTemplate.opsForValue().set(key, 1L);
        log.info("{} {}", longRedisTemplate.opsForValue().get(key), longRedisTemplate.opsForValue().get(key) instanceof Long);
        Long l1 = getLongFromRedis(key);
        longRedisTemplate.opsForValue().set(key, Integer.MAX_VALUE + 1L);
        log.info("{} {}", longRedisTemplate.opsForValue().get(key), longRedisTemplate.opsForValue().get(key) instanceof Long);
        Long l2 = getLongFromRedis(key);
        log.info("{} {} {}", l1, l2);
    }

    /**
     * 注意 Jackson JSON 反序列化对额外字段的处理
     *    前提:在configuration中自动注入了一个自定义的ObjectMapper
     *         该自定义的Mapper会把SpringBoot自己创建的ObjectMapper覆盖掉
     *         原ObjectMapper配置了  就算反序列化回收发现不存在的参数也不报错的属性
     *         自定义的ObjectMapper覆盖后会把该属性也覆盖掉
     *    解决办法:
     *      1.自定义的objectMapper也把该参数禁用
     *          objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
     *      2.@JsonIgnoreProperties
     *      3.不要自定义 ObjectMapper 配置参数
     *          spring.jackson.serialization.write_enums_using_index=true
     *      4.  @Bean
     *          public Jackson2ObjectMapperBuilderCustomizer customizer(){
     *              return builder -> builder.featuresToEnable(SerializationFeature.WRITE_ENUMS_USING_INDEX);
     *          }
     */
    @PostMapping("/testJacksonDealExtraProperty")
    public void testJacksonDealExtraProperty(@RequestBody User user) throws JsonProcessingException {
        log.info("color:{}", objectMapper.writeValueAsString(Color.BLUE));
    }

    /**
     * 2.2.5不再自动注入RestTemplate
     */
    @GetMapping("getOrderStatusClient")
    public void getOrderStatusClient() {
        StatusEnumClient result = restTemplate.getForObject("http://localhost:8080/serialize/getOrderStatus", StatusEnumClient.class);
        log.info("result {}", result);
    }

    @GetMapping("getOrderStatus")
    public StatusEnumServer getOrderStatus() {
        return StatusEnumServer.PUT;
    }

    /**
     * 序列化反序列化实现自定义的字段
     *      默认情况下是以枚举类的名字进行序列化反序列化的 CANCELED  PUT
     * 如果需要按照指定字段进行序列化
     *  1.在需要的字段上加上@JsonValue注解
     *  2.如果标注在int类型上 会有问题
     *      序列化走了 status 的值，而反序列化并没有根据 status 来，
     *      还是使用了枚举的 ordinal() 索引值
     *    前端传输[0],此时后台接受到的值是通过索引值获取的,并不是根据status反序列化来的
     *  为了解决2中的问题:
     *      第一种方法
     *          使用JsonCreator 标明强制反序列化调用的构造方法 在其中根据status值进行构造
     *      第二种 自定义反序列化器 EnumDeserializer
     * @param enumServers
     * @return
     */
    @PostMapping("queryOrdersByStatusList")
    public List<StatusEnumServer> queryOrdersByStatus(@RequestBody List<StatusEnumServer> enumServers) {
        enumServers.add(StatusEnumServer.CANCELED);
        return enumServers;
    }

    private Long getLongFromRedis(String key) {
        Object o = longRedisTemplate.opsForValue().get(key);
        if (o instanceof Integer) {
            return ((Integer) o).longValue();
        }
        if (o instanceof Long) {
            return (Long) o;
        }
        return null;
    }
}

enum Color { RED, BLUE}