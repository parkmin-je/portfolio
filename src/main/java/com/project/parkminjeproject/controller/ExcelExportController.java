package com.project.parkminjeproject.controller;

import com.project.parkminjeproject.audit.AuditLogService;
import com.project.parkminjeproject.entity.Portfolio;
import com.project.parkminjeproject.service.ExcelExportService;
import com.project.parkminjeproject.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 🆕 엑셀 내보내기 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/admin/export")
@RequiredArgsConstructor
public class ExcelExportController {

    private final PortfolioService portfolioService;
    private final ExcelExportService excelExportService;
    private final AuditLogService auditLogService;

    /**
     * 포트폴리오 목록 엑셀 다운로드 (기본)
     */
    @GetMapping("/portfolios/excel")
    public ResponseEntity<byte[]> exportPortfoliosToExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // 포트폴리오 조회 (검색/필터 적용)
            List<Portfolio> portfolios;
            if (search != null && !search.isEmpty()) {
                portfolios = portfolioService.getAllPortfolios().stream()
                        .filter(p -> p.getTitle().toLowerCase()
                                .contains(search.toLowerCase()))
                        .toList();
            } else if (category != null && !category.isEmpty()) {
                portfolios = portfolioService.getPortfoliosByCategory(category);
            } else {
                portfolios = portfolioService.getAllPortfolios();
            }

            // 엑셀 파일 생성
            byte[] excelData = excelExportService.exportPortfoliosToExcel(portfolios);

            // 파일명 생성 (한글 인코딩 처리)
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "portfolios_" + timestamp + ".xlsx";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            // 감사 로그 기록
            auditLogService.log(
                    "EXPORT",
                    "Portfolio",
                    null,
                    "포트폴리오 엑셀 내보내기 (" + portfolios.size() + "개)"
            );

            log.info("엑셀 내보내기 - 사용자: {}, 항목 수: {}",
                    userDetails.getUsername(), portfolios.size());

            // HTTP 응답 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", encodedFilename);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("엑셀 내보내기 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 포트폴리오 목록 엑셀 다운로드 (상세)
     */
    @GetMapping("/portfolios/excel/detailed")
    public ResponseEntity<byte[]> exportPortfoliosToExcelDetailed(
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // 전체 포트폴리오 조회
            List<Portfolio> portfolios = portfolioService.getAllPortfolios();

            // 상세 엑셀 파일 생성
            byte[] excelData = excelExportService.exportPortfoliosToExcelDetailed(portfolios);

            // 파일명 생성
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "portfolios_detailed_" + timestamp + ".xlsx";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            // 감사 로그 기록
            auditLogService.log(
                    "EXPORT_DETAILED",
                    "Portfolio",
                    null,
                    "포트폴리오 상세 엑셀 내보내기 (" + portfolios.size() + "개)"
            );

            log.info("상세 엑셀 내보내기 - 사용자: {}, 항목 수: {}",
                    userDetails.getUsername(), portfolios.size());

            // HTTP 응답 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", encodedFilename);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("상세 엑셀 내보내기 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 공개된 포트폴리오만 엑셀 다운로드
     */
    @GetMapping("/portfolios/excel/published")
    public ResponseEntity<byte[]> exportPublishedPortfoliosToExcel(
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // 공개된 포트폴리오만 조회
            List<Portfolio> portfolios = portfolioService.getPublishedPortfolios();

            // 엑셀 파일 생성
            byte[] excelData = excelExportService.exportPortfoliosToExcel(portfolios);

            // 파일명 생성
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "portfolios_published_" + timestamp + ".xlsx";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            // 감사 로그 기록
            auditLogService.log(
                    "EXPORT_PUBLISHED",
                    "Portfolio",
                    null,
                    "공개 포트폴리오 엑셀 내보내기 (" + portfolios.size() + "개)"
            );

            log.info("공개 포트폴리오 엑셀 내보내기 - 사용자: {}, 항목 수: {}",
                    userDetails.getUsername(), portfolios.size());

            // HTTP 응답 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", encodedFilename);

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("공개 포트폴리오 엑셀 내보내기 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}