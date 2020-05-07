package com.example.businesscodepit.addsnack;

import com.example.businesscodepit.bean.Order;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 描述：
 * <p>
 * 创建时间：2020/03/22
 * 修改时间：
 *
 * @author yaoyong
 **/
public class TestStream {
    @Test
    public void testListStream(){
        List<Order> list = new ArrayList<>();
        list.add(new Order(1L,null,"1",null,null,null));
        list.add(new Order(6L,null,"6",null,null,null));
        list.add(new Order(5L,null,"5",null,null,null));
        list.add(new Order(3L,null,"3",null,null,null));
        list.add(new Order(8L,null,"8",null,null,null));
        list.add(new Order(9L,null,"9",null,null,null));
        List<Order> moreTwoList = list.stream().filter(e  -> e.getId() > 2).collect(Collectors.toList());
        moreTwoList.stream().forEach(e -> System.out.println(e.getId()));
        System.out.println("-------------");
        list.stream().map(e -> e.getId()).forEach(e -> System.out.println(e));
        List<String> wordList = new ArrayList<>();
        wordList.add("hello");
        wordList.add("world");
        //flatMap 可以将多个Stream进行拼接成一个  因此flatMap中返回的数据类型是Stream
        wordList.stream().map(e -> e.split("")).forEach(strStream-> System.out.println(Arrays.toString(strStream)));
        wordList.stream().flatMap(e -> Arrays.stream(e.split(""))).forEach(strStream-> System.out.print(strStream));
    }
}
