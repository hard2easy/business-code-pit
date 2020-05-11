package com.example.businesscodepit.eleven.controller;


import com.example.businesscodepit.eleven.DTO.TestDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 描述：
 * <p>
 * 创建时间：2020/05/11
 * 修改时间：
 *    通过Optional可以区分前台传递的是null还是前台没有传值
 *    如果单单使用String,那么前台传b=null时与前台不给b传值,后台看到的b的值都是空对象
 *    如果使用Optional<String> 那么如果前台传null那么就是Optional.empty
 *          如果前台不传值就是null,通过Optional<String>可以区分
 * @author yaoyong
 **/
@RestController
public class TestController {
    @PostMapping("/testOptional")
    public String test(@RequestBody TestDTO dto){
        System.out.println(dto);
        System.out.println(dto.getB1() == null);
        System.out.println(dto.getB() == null);
        return "OK";
    }
}
