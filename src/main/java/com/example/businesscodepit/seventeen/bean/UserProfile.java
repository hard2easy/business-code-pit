package com.example.businesscodepit.seventeen.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/14
 * 修改时间：
 *
 * @author yaoyong
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile {
    private User user;
    private String location;
}


