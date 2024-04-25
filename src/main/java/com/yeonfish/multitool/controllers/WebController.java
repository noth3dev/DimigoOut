package com.yeonfish.multitool.controllers;

import com.yeonfish.multitool.beans.dao.StatusDAO;
import com.yeonfish.multitool.devController.logger;
import com.yeonfish.multitool.services.AlimManageService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class WebController {

    private final logger log = new logger();

    @Autowired
    AlimManageService alimManageService;

    @Autowired
    StatusDAO statusDAO;

    @RequestMapping("/check_log")
    public String logCheck() {
        return "logCheck";
    }

    @RequestMapping("/seat_change")
    public String seatChange() {
        return "seat_change";
    }

    @RequestMapping("/timer")
    public String timer() { return "timer"; }

    @RequestMapping("/")
    public String index(HttpServletRequest request, Model model) throws JSONException {
//        log.info(alimManageService.getAlim());
        model.addAttribute("alim", alimManageService.getAlim());
        return "new";
    }

    private String getSessionId(Cookie[] cookies) {
        String sessionId = "";
        if (cookies != null)
            for (Cookie c : cookies)
                if (c.getName().equals("sessionId")) {
                    sessionId = c.getValue();
                }

        return sessionId;
    }
}
