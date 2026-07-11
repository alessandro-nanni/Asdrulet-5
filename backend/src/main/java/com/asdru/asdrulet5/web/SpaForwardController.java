package com.asdru.asdrulet5.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards direct browser navigations to client-side React Router routes
 * (e.g. a QR code opening /join/ABC123 in a fresh tab) to index.html so
 * the SPA can take over routing instead of getting a 404 from Spring.
 * Add new route prefixes here as the frontend gains more top-level routes.
 */
@Controller
public class SpaForwardController {

    @RequestMapping({"/party/**", "/join/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
