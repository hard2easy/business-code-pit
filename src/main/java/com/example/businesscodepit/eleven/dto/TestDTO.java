package com.example.businesscodepit.eleven.DTO;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;
/**
 * 描述：
 * <p>
 * 创建时间：2020/05/11
 * 修改时间：
 *
 * @author yaoyong
 **/
@Data
public class TestDTO {
    private Long id;
    private Optional<Integer> age;
    private String b;
    private String b1;
    private Optional<String> c;
}
