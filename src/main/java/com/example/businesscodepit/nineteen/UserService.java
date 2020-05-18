package com.example.businesscodepit.nineteen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/18
 * 修改时间：
 *
 * @author yaoyong
 **/
@Service
@Slf4j
public class UserService {
    @Transactional
    @Metrics //启用方法监控
    public void createUser() throws Exception{
        log.info("createUser>>>>>>>>>>>>>>");
        throw new Exception("1111 error");
    }
    public int getUserCount(String name) {
        log.info("getUserCount>>>>>>>>>>>>>>");
        return 1;
    }
}
