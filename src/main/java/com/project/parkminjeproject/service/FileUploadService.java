package com.project.parkminjeproject.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * 파일 업로드 서비스 (보안 강화 버전)
 *
 * 보안 강화 사항:
 * 1. 파일 확장자 화이트리스트 검증
 * 2. MIME 타입 실제 파일 내용 기반 검증 (Apache Tika 사용)
 * 3. 이미지 실제 검증 (ImageIO로 읽기 시도)
 * 4. 파일 크기 제한 (5MB)
 * 5. 이미지 크기 제한 (10000x10000 픽셀)
 * 6. Path Traversal 공격 방지
 * 7. 안전한 파일명 생성 (UUID + 타임스탬프)
 * 8. 업로드 경로를 환경변수로 관리
 */
@Slf4j
@Service
public class FileUploadService {

    // 환경변수에서 업로드 경로 읽기 (기본값: user.home/portfolio-uploads)
    @Value("${file.upload.dir:${user.home}/portfolio-uploads}")
    private String uploadDir;

    // 허용된 확장자 (화이트리스트 방식)
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    // 허용된 MIME 타입
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    // 최대 파일 크기 (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // 최대 이미지 크기 (10000x10000 픽셀)
    private static final int MAX_IMAGE_WIDTH = 10000;
    private static final int MAX_IMAGE_HEIGHT = 10000;

    // Apache Tika (실제 파일 내용 기반 MIME 타입 검증)
    private final Tika tika = new Tika();

    /**
     * 생성자 - 업로드 디렉토리 생성
     */
    public FileUploadService() {
        // 애플리케이션 시작 시 uploadDir이 null일 수 있으므로 PostConstruct에서 처리
    }

    /**
     * 초기화 - 업로드 디렉토리 생성
     */
    @jakarta.annotation.PostConstruct
    private void init() {
        try {
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (created) {
                    log.info("업로드 디렉토리 생성 완료: {}", uploadDir);
                }
            }
        } catch (Exception e) {
            log.error("업로드 디렉토리 생성 실패: {}", e.getMessage());
        }
    }

    /**
     * 파일 업로드 (보안 강화)
     *
     * @param file 업로드할 파일
     * @return 저장된 파일의 URL (/uploads/파일명)
     * @throws IOException 파일 업로드 실패 시
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // 1. 파일 존재 여부 확인
        if (file == null || file.isEmpty()) {
            throw new IOException("업로드할 파일이 없습니다.");
        }

        log.debug("파일 업로드 시작 - 원본 파일명: {}, 크기: {} bytes",
                file.getOriginalFilename(), file.getSize());

        // 2. 파일 크기 확인
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("파일 크기는 5MB를 초과할 수 없습니다. (현재: " +
                    (file.getSize() / 1024 / 1024) + "MB)");
        }

        // 3. 파일명 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IOException("파일명이 올바르지 않습니다.");
        }

        // 파일명에 위험한 문자 포함 여부 확인
        if (originalFilename.contains("..") || originalFilename.contains("/") ||
                originalFilename.contains("\\")) {
            throw new IOException("잘못된 파일명입니다.");
        }

        // 4. 파일 확장자 검증 (화이트리스트)
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IOException("허용되지 않은 파일 형식입니다. " +
                    "(허용: jpg, jpeg, png, gif, webp)");
        }

        // 5. MIME 타입 검증 (실제 파일 내용 기반)
        String mimeType = tika.detect(file.getInputStream());
        log.debug("실제 MIME 타입: {}", mimeType);

        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IOException("허용되지 않은 파일 타입입니다. (감지된 타입: " + mimeType + ")");
        }

        // 6. 이미지 파일 실제 검증 (ImageIO로 읽기 시도)
        BufferedImage image = null;
        try {
            image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IOException("유효한 이미지 파일이 아닙니다.");
            }

            // 이미지 크기 제한
            if (image.getWidth() > MAX_IMAGE_WIDTH || image.getHeight() > MAX_IMAGE_HEIGHT) {
                throw new IOException(String.format(
                        "이미지 크기가 너무 큽니다. (현재: %dx%d, 최대: %dx%d)",
                        image.getWidth(), image.getHeight(),
                        MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT
                ));
            }

            log.debug("이미지 크기: {}x{}", image.getWidth(), image.getHeight());

        } catch (IOException e) {
            throw new IOException("이미지 파일 검증 실패: " + e.getMessage());
        } finally {
            if (image != null) {
                image.flush();
            }
        }

        // 7. 안전한 파일명 생성
        String safeFilename = generateSafeFilename(originalFilename);
        log.debug("안전한 파일명 생성: {}", safeFilename);

        // 8. 파일 저장
        Path filePath = Paths.get(uploadDir, safeFilename);

        // Path Traversal 공격 방지 - 실제 경로가 업로드 디렉토리 내부인지 확인
        Path normalizedPath = filePath.normalize();
        Path uploadDirPath = Paths.get(uploadDir).normalize();

        if (!normalizedPath.startsWith(uploadDirPath)) {
            throw new IOException("잘못된 파일 경로입니다.");
        }

        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("파일 업로드 성공: {}", safeFilename);
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", e.getMessage());
            throw new IOException("파일 저장 중 오류가 발생했습니다: " + e.getMessage());
        }

        // 9. 웹에서 접근 가능한 URL 반환
        return "/uploads/" + safeFilename;
    }

    /**
     * 안전한 파일명 생성
     * 형식: 타임스탬프_UUID해시.확장자
     *
     * @param originalFilename 원본 파일명
     * @return 안전한 파일명
     */
    private String generateSafeFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString();

        // UUID의 첫 8자만 사용 (가독성)
        String hashPart = uuid.substring(0, 8);

        return timestamp + "_" + hashPart + "." + extension;
    }

    /**
     * 파일 확장자 추출
     *
     * @param filename 파일명
     * @return 확장자 (소문자)
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 파일 삭제 (Path Traversal 방어)
     *
     * @param fileUrl 삭제할 파일의 URL (/uploads/파일명)
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // URL에서 파일명만 추출 (Path Traversal 방지)
            String filename = Paths.get(fileUrl).getFileName().toString();
            Path filePath = Paths.get(uploadDir, filename);

            // 실제 경로가 업로드 디렉토리 내부인지 확인
            Path normalizedPath = filePath.normalize();
            Path uploadDirPath = Paths.get(uploadDir).normalize();

            if (!normalizedPath.startsWith(uploadDirPath)) {
                log.error("잘못된 파일 경로 감지: {}", fileUrl);
                return;
            }

            // 파일 삭제
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("파일 삭제 성공: {}", filename);
            } else {
                log.warn("파일이 존재하지 않음: {}", filename);
            }

        } catch (IOException e) {
            log.error("파일 삭제 실패: {} - {}", fileUrl, e.getMessage());
        }
    }

    /**
     * 이미지 파일 검증 (간단한 버전)
     *
     * @param file 검증할 파일
     * @return 이미지 파일이면 true
     */
    public boolean isImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        try {
            // 1. Content-Type 확인
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return false;
            }

            // 2. MIME 타입 확인 (실제 파일 내용 기반)
            String mimeType = tika.detect(file.getInputStream());
            if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
                return false;
            }

            // 3. 파일 확장자 확인
            String filename = file.getOriginalFilename();
            if (filename == null) {
                return false;
            }

            String extension = getFileExtension(filename);
            return ALLOWED_EXTENSIONS.contains(extension);

        } catch (IOException e) {
            log.error("파일 검증 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 업로드 디렉토리 정보 조회 (디버깅용)
     *
     * @return 업로드 디렉토리 경로
     */
    public String getUploadDirectory() {
        return uploadDir;
    }

    /**
     * 파일 크기를 읽기 쉬운 형식으로 변환
     *
     * @param size 바이트 크기
     * @return 읽기 쉬운 형식 (예: 1.5 MB)
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
    }
}
