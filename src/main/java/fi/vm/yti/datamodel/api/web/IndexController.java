package fi.vm.yti.datamodel.api.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

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

    @RequestMapping("/datamodel/swagger-ui")
    public String swaggerIndex() {
        return "/datamodel/swagger-ui/index.html";
    }

}
