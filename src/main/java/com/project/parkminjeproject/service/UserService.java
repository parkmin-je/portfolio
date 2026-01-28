package com.project.parkminjeproject.service;

import com.project.parkminjeproject.dto.ProfileUpdateDto;
import com.project.parkminjeproject.entity.User;
import com.project.parkminjeproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ========================================
    // 사용자 조회 메서드
    // ========================================

    /**
     * ✅ username으로 사용자 조회 (Optional 반환)
     */
    public Optional<User> findByUsername(String username) {
        log.debug("사용자 조회 시도 - username: {}", username);
        Optional<User> user = userRepository.findByUsername(username);

        if (user.isEmpty()) {
            log.warn("⚠️ 사용자를 찾을 수 없음 - username: {}", username);
        } else {
            log.debug("✅ 사용자 찾음 - userId: {}, username: {}, role: {}",
                    user.get().getId(), user.get().getUsername(), user.get().getRole());
        }

        return user;
    }

    /**
     * ✅ ID로 사용자 조회 (Optional 반환)
     */
    public Optional<User> findById(Long id) {
        log.debug("사용자 조회 - userId: {}", id);
        return userRepository.findById(id);
    }

    /**
     * ✅ 사용자가 존재하는지 확인 (username)
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * ✅ 사용자가 존재하는지 확인 (ID)
     */
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    /**
     * username으로 사용자 조회 (예외 발생)
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("❌ 사용자를 찾을 수 없음 - username: {}", username);
                    return new RuntimeException("사용자를 찾을 수 없습니다.");
                });
    }

    /**
     * ✅ ID로 사용자 조회 (예외 발생) - AdminUserController용
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ 사용자를 찾을 수 없음 - userId: {}", id);
                    return new RuntimeException("사용자를 찾을 수 없습니다.");
                });
    }

    /**
     * ✅ 모든 사용자 조회 - AdminUserController용
     */
    public List<User> getAllUsers() {
        log.debug("모든 사용자 조회");
        return userRepository.findAll();
    }

    // ========================================
    // 사용자 등록/수정/삭제 메서드
    // ========================================

    /**
     * 회원가입
     */
    @Transactional
    public User registerUser(User user) {
        log.info("========================================");
        log.info("회원가입 시도 - username: {}", user.getUsername());

        // 아이디 중복 체크
        if (userRepository.existsByUsername(user.getUsername())) {
            log.error("❌ 중복된 아이디 - username: {}", user.getUsername());
            throw new RuntimeException("이미 존재하는 아이디입니다.");
        }

        // 비밀번호 암호화
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        log.debug("비밀번호 암호화 완료");

        // 기본값 설정
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("ROLE_USER");
        }
        user.setEnabled(true);
        log.debug("기본값 설정 완료 - role: {}, enabled: {}", user.getRole(), user.isEnabled());

        User savedUser = userRepository.save(user);
        log.info("✅ 회원가입 완료 - userId: {}, username: {}, role: {}",
                savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
        log.info("========================================");

        return savedUser;
    }

    /**
     * 아이디 중복 확인
     */
    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * ✅ 사용자 권한 변경 - AdminUserController용
     */
    @Transactional
    public void updateUserRole(Long userId, String newRole) {
        log.info("사용자 권한 변경 시도 - userId: {}, newRole: {}", userId, newRole);

        User user = getUserById(userId);
        String oldRole = user.getRole();

        user.setRole(newRole);
        userRepository.save(user);

        log.info("✅ 권한 변경 완료 - userId: {}, {} → {}", userId, oldRole, newRole);
    }

    /**
     * ✅ 사용자 활성화 상태 변경 - AdminUserController용
     */
    @Transactional
    public void updateUserEnabled(Long userId, boolean enabled) {
        log.info("사용자 활성화 상태 변경 시도 - userId: {}, enabled: {}", userId, enabled);

        User user = getUserById(userId);
        user.setEnabled(enabled);
        userRepository.save(user);

        log.info("✅ 활성화 상태 변경 완료 - userId: {}, enabled: {}", userId, enabled);
    }

    /**
     * ✅ 사용자 삭제 - AdminUserController용
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.info("사용자 삭제 시도 - userId: {}", userId);

        User user = getUserById(userId);
        String username = user.getUsername();

        userRepository.deleteById(userId);

        log.info("✅ 사용자 삭제 완료 - userId: {}, username: {}", userId, username);
    }

    // ========================================
    // 프로필 관리 메서드
    // ========================================

    /**
     * 프로필 수정
     */
    @Transactional
    public void updateProfile(String username, ProfileUpdateDto dto) {
        log.info("프로필 수정 시도 - username: {}", username);

        User user = getUserByUsername(username);

        if (dto.getName() != null && !dto.getName().isEmpty()) {
            user.setName(dto.getName());
            log.debug("이름 변경: {}", dto.getName());
        }

        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            user.setEmail(dto.getEmail());
            log.debug("이메일 변경: {}", dto.getEmail());
        }

        userRepository.save(user);
        log.info("✅ 프로필 수정 완료 - username: {}", username);
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        log.info("비밀번호 변경 시도 - username: {}", username);

        User user = getUserByUsername(username);

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.error("❌ 현재 비밀번호 불일치 - username: {}", username);
            throw new RuntimeException("현재 비밀번호가 올바르지 않습니다.");
        }

        // 새 비밀번호 암호화 및 저장
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("✅ 비밀번호 변경 완료 - username: {}", username);
    }
}