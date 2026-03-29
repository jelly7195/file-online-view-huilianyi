package cn.keking.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 *  页面跳转
 * @author yudian-it
 * @date 2017/12/27
 */
@Controller
public class IndexController {

    @GetMapping( "/index")
    public String go2Index(){
        return "/main/index";
    }

//    @GetMapping( "/record")
    public String go2Record(){
        return "/main/record";
    }

//    @GetMapping( "/sponsor")
    public String go2Sponsor(){
        return "/main/sponsor";
    }

//    @GetMapping( "/integrated")
    public String go2Integrated(){
        return "/main/integrated";
    }


    @GetMapping( "/handleError")
    public String goError(HttpServletRequest request, Model model) {
        // 从 request 中获取 Filter 设置的属性
        String errorMsg = (String) request.getAttribute("errorMsg");
        request.removeAttribute("errorMsg");

        // 添加参数
        model.addAttribute("errorMsg", errorMsg);
        return "error/error";
    }

    @GetMapping( "/")
    public String root() {
        return "/main/index";
    }


}
