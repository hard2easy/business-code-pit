package com.example.businesscodepit.fifteen;


import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
enum StatusEnumServer {
    CANCELED(5, "已取消"),
    PUT(6,"新增");
    @JsonValue
    private final int status;
    private final String desc;

    StatusEnumServer(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }
}
