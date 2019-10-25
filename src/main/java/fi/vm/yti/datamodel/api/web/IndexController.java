package fi.vm.yti.datamodel.api.web;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class IndexController {

    @RequestMapping("/api")
    public String index2() {
        return "/index.html";
    }

    @RequestMapping("/")
    public String index() {
        return "/index.html";
    }

    @RequestMapping(value = "/datamodel/swagger-ui", method = RequestMethod.GET)
    public void method(HttpServletResponse httpServletResponse) {
        httpServletResponse.setHeader("Location", "/swagger-ui/index.html");
        httpServletResponse.setStatus(302);
    }

}
