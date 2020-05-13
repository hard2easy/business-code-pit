package com.example.businesscodepit.fifteen;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/13
 * 修改时间：
 *
 * @author yaoyong
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements Serializable {
    private String name;
    private int age;
    @JsonCreator
    public User(@JsonProperty("name") String name) {
        this.name = name;
    }
}
