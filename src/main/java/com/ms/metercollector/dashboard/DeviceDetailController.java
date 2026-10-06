package com.ms.metercollector.dashboard;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

@Controller
public class DeviceDetailController {

    private final DeviceDetailService deviceDetailService;
    private final JsonMapper jsonMapper;

    public DeviceDetailController(DeviceDetailService deviceDetailService, JsonMapper jsonMapper) {
        this.deviceDetailService = deviceDetailService;
        this.jsonMapper = jsonMapper;
    }

    // 예: /devices/AHU-01?date=2026-10-06&unit=1h. date 를 빼면 오늘, unit 을 빼면 15분.
    @GetMapping("/devices/{deviceCode}")
    public String detail(@PathVariable String deviceCode,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @RequestParam(required = false) String unit,
                         Model model) {
        LocalDate today = LocalDate.now(DashboardController.ZONE);
        LocalDate selected = date == null || date.isAfter(today) ? today : date;
        DeviceDetailView view = deviceDetailService.load(deviceCode, selected, StatUnit.fromCode(unit), today)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "등록되지 않은 장비: " + deviceCode));
        model.addAttribute("view", view);
        model.addAttribute("chartJson", jsonMapper.writeValueAsString(view.chart()));
        return "device";
    }
}
