package com.ms.metercollector.dashboard;

import com.ms.metercollector.device.DeviceRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DeviceRepository deviceRepository;

    public DashboardController(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("devices", deviceRepository.findAll());
        return "dashboard";
    }
}
