package com.project.parkminjeproject.service;

import com.project.parkminjeproject.entity.Portfolio;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 🆕 엑셀 내보내기 서비스
 * Apache POI를 사용하여 포트폴리오 데이터를 Excel 파일로 내보내기
 */
@Slf4j
@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 포트폴리오 목록을 Excel 파일로 내보내기
     *
     * @param portfolios 포트폴리오 목록
     * @return Excel 파일의 바이트 배열
     * @throws IOException 파일 생성 실패 시
     */
    public byte[] exportPortfoliosToExcel(List<Portfolio> portfolios) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("포트폴리오 목록");

            // 헤더 스타일 생성
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            // 헤더 행 생성
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "번호", "제목", "카테고리", "요약", "공개여부",
                    "조회수", "좋아요", "기술스택", "기간", "역할",
                    "등록일", "수정일"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 데이터 행 생성
            int rowNum = 1;
            for (Portfolio portfolio : portfolios) {
                Row row = sheet.createRow(rowNum++);

                // ID
                row.createCell(0).setCellValue(portfolio.getId());

                // 제목
                row.createCell(1).setCellValue(portfolio.getTitle());

                // 카테고리
                row.createCell(2).setCellValue(portfolio.getCategory());

                // 요약
                String summary = portfolio.getSummary();
                row.createCell(3).setCellValue(
                        summary != null ? summary : portfolio.getDescription());

                // 공개 여부
                row.createCell(4).setCellValue(portfolio.isPublished() ? "공개" : "비공개");

                // 조회수
                Cell viewCountCell = row.createCell(5);
                viewCountCell.setCellValue(portfolio.getViewCount());
                viewCountCell.setCellStyle(numberStyle);

                // 좋아요
                Cell likeCountCell = row.createCell(6);
                likeCountCell.setCellValue(portfolio.getLikeCount());
                likeCountCell.setCellStyle(numberStyle);

                // 기술 스택
                row.createCell(7).setCellValue(
                        portfolio.getTechnologies() != null ? portfolio.getTechnologies() : "");

                // 기간
                row.createCell(8).setCellValue(
                        portfolio.getDuration() != null ? portfolio.getDuration() : "");

                // 역할
                row.createCell(9).setCellValue(
                        portfolio.getRole() != null ? portfolio.getRole() : "");

                // 등록일
                if (portfolio.getCreatedAt() != null) {
                    Cell createdCell = row.createCell(10);
                    createdCell.setCellValue(
                            portfolio.getCreatedAt().format(DATE_FORMATTER));
                    createdCell.setCellStyle(dateStyle);
                }

                // 수정일
                if (portfolio.getUpdatedAt() != null) {
                    Cell updatedCell = row.createCell(11);
                    updatedCell.setCellValue(
                            portfolio.getUpdatedAt().format(DATE_FORMATTER));
                    updatedCell.setCellStyle(dateStyle);
                }
            }

            // 열 너비 자동 조정
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // 최소/최대 너비 설정
                int currentWidth = sheet.getColumnWidth(i);
                if (currentWidth < 2000) {
                    sheet.setColumnWidth(i, 2000);
                } else if (currentWidth > 15000) {
                    sheet.setColumnWidth(i, 15000);
                }
            }

            workbook.write(out);
            log.info("엑셀 내보내기 완료 - 항목 수: {}", portfolios.size());
            return out.toByteArray();

        } catch (IOException e) {
            log.error("엑셀 내보내기 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 헤더 스타일 생성
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 배경색 (파란색)
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 폰트 (흰색, 굵게)
        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        // 정렬
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 테두리
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    /**
     * 날짜 스타일 생성
     */
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * 숫자 스타일 생성
     */
    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    /**
     * 상세 정보 포함 엑셀 내보내기
     */
    public byte[] exportPortfoliosToExcelDetailed(List<Portfolio> portfolios)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("포트폴리오 상세");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);

            // 헤더
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "ID", "제목", "카테고리", "설명", "기술스택",
                    "기간", "역할", "클라이언트", "팀규모",
                    "성과", "도전과제", "비즈니스 임팩트",
                    "공개여부", "조회수", "좋아요", "등록일"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 데이터
            int rowNum = 1;
            for (Portfolio p : portfolios) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getTitle());
                row.createCell(2).setCellValue(p.getCategory());

                Cell descCell = row.createCell(3);
                descCell.setCellValue(p.getDescription() != null ? p.getDescription() : "");
                descCell.setCellStyle(wrapStyle);

                row.createCell(4).setCellValue(p.getTechnologies() != null ? p.getTechnologies() : "");
                row.createCell(5).setCellValue(p.getDuration() != null ? p.getDuration() : "");
                row.createCell(6).setCellValue(p.getRole() != null ? p.getRole() : "");
                row.createCell(7).setCellValue(p.getClient() != null ? p.getClient() : "");
                row.createCell(8).setCellValue(p.getTeamSize() != null ? p.getTeamSize() : "");
                row.createCell(9).setCellValue(p.getAchievements() != null ? p.getAchievements() : "");
                row.createCell(10).setCellValue(p.getChallenges() != null ? p.getChallenges() : "");
                row.createCell(11).setCellValue(p.getImpact() != null ? p.getImpact() : "");
                row.createCell(12).setCellValue(p.isPublished() ? "공개" : "비공개");
                row.createCell(13).setCellValue(p.getViewCount());
                row.createCell(14).setCellValue(p.getLikeCount());

                if (p.getCreatedAt() != null) {
                    row.createCell(15).setCellValue(p.getCreatedAt().format(DATE_FORMATTER));
                }
            }

            // 열 너비 설정
            sheet.setColumnWidth(0, 2000);   // ID
            sheet.setColumnWidth(1, 8000);   // 제목
            sheet.setColumnWidth(2, 3000);   // 카테고리
            sheet.setColumnWidth(3, 15000);  // 설명
            sheet.setColumnWidth(4, 8000);   // 기술스택

            for (int i = 5; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            log.info("상세 엑셀 내보내기 완료 - 항목 수: {}", portfolios.size());
            return out.toByteArray();

        } catch (IOException e) {
            log.error("상세 엑셀 내보내기 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
}