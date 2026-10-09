package com.medianet.reservation.controller;

import com.medianet.reservation.dto.DashboardAdminResponse;
import com.medianet.reservation.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardAdminResponse getStatistiques() {
        return dashboardService.getStatistiquesAdmin();
    }
}
