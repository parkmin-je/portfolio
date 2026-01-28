package com.project.parkminjeproject.controller;

import com.project.parkminjeproject.entity.Portfolio;
import com.project.parkminjeproject.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * SEO용 Sitemap 및 Robots.txt 컨트롤러
 */
@Controller
@RequiredArgsConstructor
public class SeoController {

    private final PortfolioService portfolioService;

    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    @ResponseBody
    public String sitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 메인 페이지
        xml.append("  <url>\n");
        xml.append("    <loc>https://questionmark.kr.com/</loc>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>1.0</priority>\n");
        xml.append("  </url>\n");

        // About 페이지
        xml.append("  <url>\n");
        xml.append("    <loc>https://questionmark.kr.com/about</loc>\n");
        xml.append("    <changefreq>monthly</changefreq>\n");
        xml.append("    <priority>0.8</priority>\n");
        xml.append("  </url>\n");

        // 각 포트폴리오 페이지
        List<Portfolio> portfolios = portfolioService.getPublishedPortfolios();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Portfolio portfolio : portfolios) {
            xml.append("  <url>\n");
            xml.append("    <loc>https://questionmark.kr.com/portfolio/").append(portfolio.getId()).append("</loc>\n");
            if (portfolio.getUpdatedAt() != null) {
                xml.append("    <lastmod>").append(portfolio.getUpdatedAt().format(formatter)).append("</lastmod>\n");
            }
            xml.append("    <changefreq>monthly</changefreq>\n");
            xml.append("    <priority>0.7</priority>\n");
            xml.append("  </url>\n");
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    @GetMapping(value = "/robots.txt", produces = "text/plain")
    @ResponseBody
    public String robots() {
        return "User-agent: *\n" +
                "Allow: /\n" +
                "Disallow: /admin/\n" +
                "Disallow: /login\n" +
                "Disallow: /register\n" +
                "Disallow: /error/\n" +
                "\n" +
                "Sitemap: https://questionmark.kr.com/sitemap.xml";
    }
}