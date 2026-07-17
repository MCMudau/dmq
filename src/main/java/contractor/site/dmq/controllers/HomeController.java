package contractor.site.dmq.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to serve the landing page and static resources
 */
@Controller
public class HomeController {

    /**
     * Serve the main landing page (index.html)
     * @return index view name
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    /**
     * Redirect to root path
     * @return redirect to index
     */
    @GetMapping("/index")
    public String home() {
        return "redirect:/";
    }
}
