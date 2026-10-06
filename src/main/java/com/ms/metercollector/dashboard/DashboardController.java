package com.ms.metercollector.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Controller
public class DashboardController {

    static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final DashboardService dashboardService;
    private final JsonMapper jsonMapper;

    public DashboardController(DashboardService dashboardService, JsonMapper jsonMapper) {
        this.dashboardService = dashboardService;
        this.jsonMapper = jsonMapper;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        DashboardView view = dashboardService.load(LocalDateTime.now(ZONE));
        model.addAttribute("view", view);
        model.addAttribute("chartJson", jsonMapper.writeValueAsString(view.chart()));
        return "dashboard";
    }
}
