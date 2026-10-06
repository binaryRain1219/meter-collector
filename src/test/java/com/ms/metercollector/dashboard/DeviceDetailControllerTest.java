package com.ms.metercollector.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// 로컬 MySQL(meter-publisher 의 docker compose) 에 장비 시드가 들어가 있어야 한다.
@SpringBootTest
@AutoConfigureMockMvc
class DeviceDetailControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 장비_상세에_그날_15분_구간이_모두_보인다() throws Exception {
        mockMvc.perform(get("/devices/AHU-01").param("date", "2000-01-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("device"))
                .andExpect(content().string(containsString("공조기 1호")))
                .andExpect(content().string(containsString("00:00 ~ 00:15")))
                .andExpect(content().string(containsString("23:45 ~ 00:00")));
    }

    @Test
    void 한시간_단위로_보면_24개_구간이_보인다() throws Exception {
        mockMvc.perform(get("/devices/AHU-01").param("date", "2000-01-01").param("unit", "1h"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("23:00 ~ 00:00")))
                .andExpect(content().string(containsString("최대수요 (kW)")));
    }

    @Test
    void 등록되지_않은_장비는_404() throws Exception {
        mockMvc.perform(get("/devices/NOPE"))
                .andExpect(status().isNotFound());
    }
}
