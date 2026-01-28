package com.project.parkminjeproject.domain.audit.controller;

import com.project.parkminjeproject.domain.audit.entity.AuditLog;
import com.project.parkminjeproject.domain.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public String viewLogs(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "50") int size,
                           @RequestParam(required = false) String username,
                           @RequestParam(required = false) String action,
                           Model model) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<AuditLog> logsPage;
        if (username != null && !username.isEmpty()) {
            logsPage = auditLogRepository.findByUsernameOrderByCreatedAtDesc(username, pageRequest);
        } else if (action != null && !action.isEmpty()) {
            logsPage = auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageRequest);
        } else {
            logsPage = auditLogRepository.findAll(pageRequest);
        }

        List<AuditLog> logs = logsPage.getContent();

        // 날짜별로 그룹화
        Map<String, List<AuditLog>> logsByDate = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> {
                            LocalDate date = log.getCreatedAt().toLocalDate();
                            LocalDate today = LocalDate.now();
                            LocalDate yesterday = today.minusDays(1);

                            if (date.equals(today)) {
                                return "오늘";
                            } else if (date.equals(yesterday)) {
                                return "어제";
                            } else {
                                return date.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));
                            }
                        },
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        model.addAttribute("logs", logs);
        model.addAttribute("logsByDate", logsByDate);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logsPage.getTotalPages());
        model.addAttribute("totalItems", logsPage.getTotalElements());
        model.addAttribute("username", username);
        model.addAttribute("action", action);

        return "admin/audit-logs";
    }
}