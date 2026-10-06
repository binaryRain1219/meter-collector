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
class DashboardControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 대시보드에_등록된_장비_목록이_보인다() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(containsString("MAIN-A")))
                .andExpect(content().string(containsString("공조기 1호")));
    }
}
