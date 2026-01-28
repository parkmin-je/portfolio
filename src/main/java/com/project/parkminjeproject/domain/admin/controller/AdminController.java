// AdminController.java
package com.project.parkminjeproject.domain.admin.controller;

import com.project.parkminjeproject.domain.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PortfolioService portfolioService;

    // 관리자 대시보드
    @GetMapping
    public String dashboard(Model model) {
        // 전체 통계
        Map<String, Object> stats = portfolioService.getDashboardStats();

        model.addAttribute("totalPortfolios", stats.get("totalPortfolios"));
        model.addAttribute("publishedPortfolios", stats.get("publishedPortfolios"));
        model.addAttribute("unpublishedPortfolios", stats.get("unpublishedPortfolios"));
        model.addAttribute("categoryStats", stats.get("categoryStats"));
        model.addAttribute("recentPortfolios", stats.get("recentPortfolios"));

        return "admin/dashboard";
    }
}