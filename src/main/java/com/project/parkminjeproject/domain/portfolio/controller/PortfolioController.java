package com.project.parkminjeproject.domain.portfolio.controller;

import com.project.parkminjeproject.domain.category.entity.Category;
import com.project.parkminjeproject.domain.portfolio.entity.Portfolio;
import com.project.parkminjeproject.domain.portfolio.service.PortfolioService;
import com.project.parkminjeproject.domain.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String category,
                       Model model) {
        List<Portfolio> portfolios;

        if (search != null && !search.isBlank()) {
            // 검색 기능은 페이징 메서드 사용
            portfolios = portfolioService.searchPortfoliosWithPaging(search, null, 0, 1000)
                    .getContent();
        } else if (category != null && !category.isBlank()) {
            portfolios = portfolioService.getPortfoliosByCategory(category);
        } else {
            portfolios = portfolioService.getAllPortfolios();
        }

        model.addAttribute("portfolios", portfolios);
        model.addAttribute("search", search);
        model.addAttribute("selectedCategory", category);

        return "admin/portfolio-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("portfolio", new Portfolio());
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/portfolio-form";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        Portfolio portfolio = portfolioService.getPortfolioById(id);
        model.addAttribute("portfolio", portfolio);
        return "admin/portfolio-view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Portfolio portfolio = portfolioService.getPortfolioById(id);
        model.addAttribute("portfolio", portfolio);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/portfolio-form";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        portfolioService.deletePortfolio(id);
        return "redirect:/admin/portfolios?deleted=true";
    }

    @PostMapping("/{id}/toggle-publish")
    public String togglePublish(@PathVariable Long id) {
        portfolioService.togglePublished(id);
        return "redirect:/admin/portfolios/{id}";
    }
}