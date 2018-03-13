package fi.vm.yti.datamodel.api.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

    @RequestMapping("/api")
    public String index() {
        return "index.html";
    }

    @RequestMapping("/api/swagger-ui")
    public String swaggerIndex() {
        return "/api/swagger-ui/index.html";
    }
}
