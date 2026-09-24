package contractor.site.dmq.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller to navigate between the admin pages
 */
@Controller
@RequestMapping("/admin")
public class AdminPagesController {

    /**
     * Serve the admin dashboard (admin.html)
     * @return admin dashboard view
     */
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard() {
        return "forward:/admin.html";
    }

    /**
     * Serve the create admin account page (createAdminAccount.html)
     * @return create admin account view
     */
    @GetMapping("/create-account")
    public String createAccount() {
        return "forward:/createAdminAccount.html";
    }

    /**
     * Redirect to the admin login page
     * @return redirect to login
     */
    @GetMapping("/login")
    public String login() {
        return "redirect:/login.html";
    }
}
