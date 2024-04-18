package com.yeonfish.multitool.controllers;

import com.yeonfish.multitool.Constant;
import com.yeonfish.multitool.beans.dao.AdminDAO;
import com.yeonfish.multitool.beans.dao.StatusDAO;
import com.yeonfish.multitool.beans.vo.AlimiVO;
import com.yeonfish.multitool.beans.vo.StatusVO;
import com.yeonfish.multitool.devController.logger;
import com.yeonfish.multitool.services.AlimManageService;
import com.yeonfish.multitool.services.StatusManageService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.expression.Calendars;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;


@RestController
@RequestMapping("/")
public class MainController {
    private final logger log = new logger();

    @Autowired
    private AlimManageService alimManageService;

    @Autowired
    private StatusManageService statusManageService;

    @Autowired
    private AdminDAO adminDAO;

    @Autowired
    private StatusDAO statusDAO;

    @RequestMapping("/auth")
    public String auth(@RequestParam("code") String code, HttpServletRequest request, HttpServletResponse response) throws JSONException, IOException {
        JSONObject params = new JSONObject();
        params.put("code", code);
        params.put("client_id", Constant.GoogleOauthClientId);
        params.put("client_secret", Constant.GoogleOauthClientPw);
        params.put("grant_type", "authorization_code");
        params.put("redirect_uri", "https://localhost/auth");
//        params.put("redirect_uri", "https://dimigo.site/auth");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(params.toString(), headers);

        RestTemplate rt = new RestTemplate();

//        log.info(params.toString());

        ResponseEntity<String> exchange = rt.exchange(
                "https://oauth2.googleapis.com/token",
                HttpMethod.POST,
                entity,
                String.class
        );

        String access_token = (new JSONObject(exchange.getBody())).getString("access_token");

        headers = new HttpHeaders();
        headers.add("Authorization", "Bearer"+access_token);
        entity = new HttpEntity<>("", headers);

        rt = new RestTemplate();
        exchange = rt.exchange(
                "https://www.googleapis.com/oauth2/v1/userinfo?alt=json",
                HttpMethod.GET,
                entity,
                String.class
        );

        log.info("Login "+exchange.getBody());

        JSONObject result = new JSONObject(exchange.getBody());

        String sessionId = getSessionId(request.getCookies());
        HttpSession session = request.getSession();
        session.setAttribute(sessionId, result.toString());

        response.sendRedirect("/");
        return "success";
    }

    @RequestMapping("joke")
    public String joke(@RequestParam("flag") String flag, HttpServletRequest request) throws JSONException {
        // db는 lyj.kr 유저 네임은 joke.
        // 이 서버의 개발자는 너무나도 유저 pw를 짓기 귀찮은 관계로 비밀번호를 무언가를 해싱한 값으로 설정해버렸네요.
        // 멍충멍충

        // 아아 그냥 공개 할란다.
        if (flag.equals("FLAG{9e79cb39-436b-412d-8a60-95d2f8b650d9}")) {
            return "success";
        }
        return "fail";
    }

    @RequestMapping(value = "set/status", method = RequestMethod.POST)
    public boolean setStatus(@RequestParam("status") int status, @RequestParam(name = "reason", required = false) String reason, HttpServletRequest request) throws JSONException {

        JSONObject user = getUser(request);
        String uid = user.getString("id");
        StatusVO s_user = new StatusVO(); s_user.setId(uid); s_user.setStatus(status); s_user.setReason(reason);

        log.info(reason);

        return statusManageService.updateStatus(s_user);
    }

    @RequestMapping(value = "set/alim", method = RequestMethod.POST)
    public String saveAlim(@RequestParam("text") String text, HttpServletRequest request, HttpServletResponse response) throws JSONException {
        HttpSession session = request.getSession();
        JSONObject user = getUser(request);

        if (adminDAO.getAdmin(user.getString("id")) == null || adminDAO.getAdmin(user.getString("id")).equals("")){
            response.setStatus(403);
            return "Access Denied";
        }else {
            AlimiVO alim = new AlimiVO();
            Calendars calendars = new Calendars(Locale.KOREAN);
            alim.setDate(calendars.format(calendars.createNow(), "MM월 dd일 hh시 mm분"));
            alim.setText(text);
            boolean isSuccess = alimManageService.uploadAlim(alim);
            if (isSuccess) {
                response.setStatus(200);
                return "Success";
            }else {
                response.setStatus(503);
                return "Server cannot process your request";
            }
        }
    }

//    @RequestMapping("get/dimiOutside")
//    public String getDimiOutSide() {
//
//    }

    @RequestMapping(value = "get/status", method = RequestMethod.GET)
    public int getStatus(HttpServletRequest request) throws JSONException {
        StatusVO user = new StatusVO(); user.setId(getUser(request).getString("id"));

        StatusVO[] result = statusManageService.getStatus(user);
        if (result.length == 0) return 0;
        else return result[0].getStatus();
    }

    @RequestMapping(value = "get/statusList", method = RequestMethod.GET)
    public int getStatusList(HttpServletRequest request) throws JSONException {
        StatusVO user = new StatusVO(); user.setId(getUser(request).getString("id"));

        StatusVO[] result = statusManageService.getStatus(user);
        if (result.length == 0) return 0;
        else return result[0].getStatus();
    }

    @RequestMapping(value = "get/background", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] getImage() throws IOException {
        InputStream in = null;
        if (LocalTime.now(ZoneId.of("Asia/Seoul")).isBefore(LocalTime.of(13, 0)) && LocalTime.now(ZoneId.of("Asia/Seoul")).isAfter(LocalTime.of(4, 0)))
            in = new ClassPathResource("static/image/background_morning.jpg").getInputStream();
        else if (LocalTime.now(ZoneId.of("Asia/Seoul")).isBefore(LocalTime.of(16, 0)) && LocalTime.now(ZoneId.of("Asia/Seoul")).isAfter(LocalTime.of(8, 0)))
            in = new ClassPathResource("static/image/background_evening.jpg").getInputStream();
        else
            in = new ClassPathResource("static/image/background_night.jpg").getInputStream();
        return IOUtils.toByteArray(in);
    }

    @RequestMapping(value = "get/timetable", method = RequestMethod.GET)
    public String timetable(HttpServletRequest request) throws JSONException {
        Calendars calendars = new Calendars(Locale.KOREAN);
        String date = calendars.format(calendars.createNow(), "yyyyMMdd");

        String userClass = getUserClass(getUser(request));

        String url;

        if (userClass.chars().allMatch( Character::isDigit ))
            url = "https://open.neis.go.kr/hub/hisTimetable?SD_SCHUL_CODE=7530560&ATPT_OFCDC_SC_CODE=J10&GRADE="+userClass.charAt(0)+"&CLASS_NM="+userClass.charAt(1)+"&Type=json&TI_FROM_YMD="+date+"&TI_TO_YMD="+date+"&KEY="+Constant.NeisApiKey;
        else
            url = "https://open.neis.go.kr/hub/hisTimetable?SD_SCHUL_CODE=7530560&ATPT_OFCDC_SC_CODE=J10&GRADE=1&CLASS_NM=3&Type=json&TI_FROM_YMD="+date+"&TI_TO_YMD="+date+"&KEY="+Constant.NeisApiKey;

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>("", headers);

        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> exchange = rt.exchange(url, HttpMethod.GET, entity, String.class);

        return new JSONObject(exchange.getBody()).toString();
    }

    @RequestMapping(value = "user", method = RequestMethod.GET)
    public String userInfo(HttpServletRequest request) {
        return (String) request.getSession().getAttribute(getSessionId(request.getCookies()));
    }


    @RequestMapping(value = "robots.txt")
    public String robots() {
        return "User-agent: *\n" +
                "Allow: /";
    }

    @RequestMapping(value = "privacy")
    public String privacy() {
        return "이 웹 어플리케이션은 사용자의 ";
    }

    private String getUserClass(JSONObject user) throws JSONException {
        String userClass = user.getString("name").substring(0, 4);

        if (userClass.chars().allMatch( Character::isDigit ))
            return userClass;
        else
            return "";
    }

    private JSONObject getUser(HttpServletRequest request) throws JSONException {
        return new JSONObject((String) request.getSession().getAttribute(getSessionId(request.getCookies())));
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
