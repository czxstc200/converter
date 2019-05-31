package cn.edu.bupt;

import cn.edu.bupt.annotation.Controller;
import cn.edu.bupt.annotation.RequestMapping;

/**
 * @Description: TestController
 * @Author: czx
 * @CreateDate: 2019-05-30 23:40
 * @Version: 1.0
 */
@Controller
public class TestController {

    @RequestMapping("/")
    public void index() {
        System.out.println("index method");
    }

    @RequestMapping("/about")
    public String about(String args) {
        return args;
    }
}
