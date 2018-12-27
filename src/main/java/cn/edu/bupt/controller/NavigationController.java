package cn.edu.bupt.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @Description: 用于路由的controller
 * @Author: CZX
 * @CreateDate: 2018/11/30 11:12
 * @Version: 1.0
 */
@Controller
@CrossOrigin
public class NavigationController {


    @RequestMapping("/home")
    public String  index() {
        return "template/index";
    }

    @RequestMapping("/single")
    public String  single() {
        return "template/singleVideo";
    }
}
