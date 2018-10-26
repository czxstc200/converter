package cn.edu.bupt;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by CZX on 2018/10/25.
 */
@Controller
public class NavigationController {

    @RequestMapping("/home")
    public String  index() {
        return "template/index";
    }

    @RequestMapping("/home2")
    public String  index2() {
        return "template/definition";
    }
}
