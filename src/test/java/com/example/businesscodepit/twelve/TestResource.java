package com.example.businesscodepit.twelve;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/12
 * 修改时间：
 *
 * @author yaoyong
 **/

public class TestResource implements AutoCloseable {
    public void read() throws Exception{
        throw new Exception("read error");
    }
    @Override
    public void close() throws Exception {
        throw new Exception("close error");
    }
}
