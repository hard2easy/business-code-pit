package com.example.businesscodepit.nineteen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/18
 * 修改时间：
 *
 * @author yaoyong
 **/
@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
@Slf4j
public class SayBye extends SayService {
    @Override
    public void say() {
        super.say();
        log.info("bye>>" + this);
    }
}
