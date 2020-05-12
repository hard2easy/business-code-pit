package com.example.businesscodepit.twelve;

import com.example.businesscodepit.twelve.handle.BusinessException;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/12
 * 修改时间：
 *
 * @author yaoyong
 **/
public class Exceptions {
    public static BusinessException ORDEREXISTS = new BusinessException("订单已存在",3001);
    public static BusinessException getException(){
        return new BusinessException("订单已存在",3001);
    }
}
