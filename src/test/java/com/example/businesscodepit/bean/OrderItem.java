package com.example.businesscodepit.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 描述：
 * <p>
 * 创建时间：2020/03/24
 * 修改时间：
 *
 * @author yaoyong
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private Long productId;//商品ID
    private String productName;//商品名称
    private Double productPrice;//商品价格
    private Integer productQuantity;//商品数量
}
