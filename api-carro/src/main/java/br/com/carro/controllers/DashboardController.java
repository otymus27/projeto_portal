package br.com.carro.controllers;

import br.com.carro.entities.Dashboard.DashboardMetricsDTO;
import br.com.carro.services.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetricsDTO> getDashboardMetrics() {
        DashboardMetricsDTO metrics = dashboardService.getMetrics();
        return ResponseEntity.ok(metrics);
    }
}