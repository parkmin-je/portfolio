package com.project.parkminjeproject.global.exception;

/**
 * 파일 업로드 실패 시 발생하는 예외
 */
public class FileUploadException extends RuntimeException {

    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
