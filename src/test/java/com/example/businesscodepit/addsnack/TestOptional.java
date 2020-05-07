package com.example.businesscodepit.addsnack;

import com.example.businesscodepit.bean.Product;
import com.example.businesscodepit.function.ThrowingFunction;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;



/**
 * 描述：
 * <p>
 * 创建时间：2020/03/23
 * 修改时间：
 *
 * @author yaoyong
 **/
public class TestOptional {
    @Test
    public void test1() throws Throwable {
        Optional empty = Optional.empty();
        Optional<Integer> a = Optional.of(1);
        int b = a.orElse(1);
        //Supplier<? extends T> other
        int c = a.orElseGet(() -> {
            return 1;
        });
        Optional d = Optional.ofNullable(2);
        //Supplier<? extends X>
        d.orElseThrow(() -> new Exception());
        //Consumer<? super T>
        d.ifPresent(e -> System.out.println("有值"));
        System.out.println(d.isPresent());
        System.out.println(a.get());
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        Predicate<Integer> condition1 = (e) -> e > 1;
        Predicate<Integer> condition2 = (e) -> e < 10;
        Optional a1 = a.map(e -> {
            System.out.println(e);
            return e;
        }).filter((condition1.and(condition2)));
        System.out.println("--------" + (a1.isPresent()?a1.get():"false"));
        int value = list.stream().map(e -> {
            System.out.println(e);
            return e;
        }).flatMapToInt(e -> IntStream.of(e*2)).sum();
        System.out.println(value);
    }

    private Map<Long, Product> cache = new ConcurrentHashMap<>();

    private Product getProductAndCache(Long id) {
        Product product = null;
        //Key存在，返回Value
        if (cache.containsKey(id)) {
            product = cache.get(id);
        } else {
            //不存在，则获取Value
            //需要遍历数据源查询获得Product
            for (Product p : Product.getData()) {
                if (p.getId().equals(id)) {
                    product = p;
                    break;
                }
            }
            //加入ConcurrentHashMap
            if (product != null)
                cache.put(id, product);
        }
        return product;
    }

    @Test
    public void notcoolCache() {
        getProductAndCache(1L);
        getProductAndCache(100L);
        System.out.println(cache);
        assertThat(cache.size(), is(1));
        assertTrue(cache.containsKey(1L));
    }

    /**
     *
     *   if (map.get(key) == null) {
     *     V newValue = mappingFunction.apply(key);
     *   if (newValue != null)
     *     map.put(key, newValue);
     }
     * @param id
     * @return
     */
    private Product getProductAndCacheCool(Long id) {
        //i就是key數值
        return cache.computeIfAbsent(id, i -> //当Key不存在的时候提供一个Function来代表根据Key获取Value的过程
                Product.getData().stream()
                        .filter(p -> {
                            System.out.println("-----" + i);
                            return p.getId().equals(i);
                        }) //过滤
                        .findFirst() //找第一个，得到Optional<Product>
                        .orElse(null)); //如果找不到Product，则使用null
    }

    @Test
    public void coolCache()
    {
        getProductAndCacheCool(1L);
        getProductAndCacheCool(99L);

        System.out.println(cache);
        assertThat(cache.size(), is(1));
        assertTrue(cache.containsKey(1L));
    }

    @Test
    public void filesExample() throws IOException {
        //无限深度，递归遍历文件夹
        try (Stream<Path> pathStream = Files.walk(Paths.get("."))) {
            pathStream.filter(Files::isRegularFile) //只查普通文件
                    .filter(FileSystems.getDefault().getPathMatcher("glob:**/*.java")::matches) //搜索java源码文件
                    .flatMap(ThrowingFunction.unchecked(path ->
                            Files.readAllLines(path).stream() //读取文件内容，转换为Stream<List>
                                    .filter(line -> Pattern.compile("public class").matcher(line).find()) //使用正则过滤带有public class的行
                                    .map(line -> path.getFileName() + " >> " + line))) //把这行文件内容转换为文件名+行
                    .forEach(System.out::println); //打印所有的行
        }
    }
}
