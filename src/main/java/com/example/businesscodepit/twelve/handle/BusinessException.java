package com.example.businesscodepit.twelve.handle;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/11
 * 修改时间：
 *
 * @author yaoyong
 **/
public class BusinessException extends RuntimeException{
    private int code;
    private String msg;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public BusinessException(String message, int code) {
        super(message);
        this.code = code;
        this.msg = message;
    }
}
