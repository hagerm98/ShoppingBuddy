package com.hager.shoppingbuddy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.hager.shoppingbuddy.dto.RegistrationRequest;

@Controller
public class UIController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/home")
    public String homePage() {
        return "home";
    }

    @GetMapping("/about")
    public String aboutUs() {
        return "about";
    }

    @GetMapping("/contact")
    public String contactUs() {
        return "contact";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        model.addAttribute("currentPage", "signup");
        return "signup";
    }
}
