package com.example.businesscodepit.fifteen;


import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import lombok.Getter;

@Getter
enum StatusEnumClient {
    @JsonEnumDefaultValue
    CREATED(1, "已创建"),
    PAID(2, "已支付"),
    DELIVERED(3, "已送到"),
    FINISHED(4, "已完成");

    private final int status;
    private final String desc;

    StatusEnumClient(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }
}
