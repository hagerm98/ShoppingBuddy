package com.hager.shoppingbuddy.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

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
    public String signup() {
        return "signup";
    }

    @GetMapping("/login")
    public String login(Model model,
                        HttpSession session,
                        @RequestParam(value = "confirmationSuccess", required = false)
                        Boolean confirmationSuccess,
                        @RequestParam(value = "error", required = false)
                        String error) {
        if (confirmationSuccess != null && confirmationSuccess) {
            model.addAttribute("confirmationSuccess", true);
        }

        if (error != null) {
            String errorMessage = "Invalid email or password. Please try again.";

            if (session != null) {
                Exception authException = (Exception) session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
                if (authException != null) {
                    String exceptionMessage = authException.getMessage();

                    if (exceptionMessage.contains("Bad credentials")) {
                        errorMessage = "Invalid email or password. Please check your credentials and try again.";
                    } else if (exceptionMessage.contains("User account is locked")) {
                        errorMessage = "Your account has been locked. Please contact support using Contact Us link above.";
                    } else if (exceptionMessage.contains("User is disabled")) {
                        errorMessage = "Your account is disabled. Please contact support using Contact Us link above..";
                    } else if (exceptionMessage.contains("User account has expired")) {
                        errorMessage = "Your account has expired. Please contact support to reactivate your account using Contact Us link above..";
                    } else if (exceptionMessage.contains("Username not found")) {
                        errorMessage = "No account found with this email address. Please check your email or sign up for a new account.";
                    } else {
                        errorMessage = "An unexpected error occurred during login. Please try again later.";
                    }

                    session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
                }
            }

            model.addAttribute("authenticationError", errorMessage);
        }

        return "login";
    }

    @GetMapping("/error-page")
    public String errorPage(Model model,
                            @RequestParam(value = "errorMessage", required = false) String errorMessage,
                            @RequestParam(value = "errorDetails", required = false) String errorDetails) {

        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
        }

        if (errorDetails != null) {
            model.addAttribute("errorDetails", errorDetails);
        }

        return "error-page";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login";
        }

        return "profile";
    }
}
