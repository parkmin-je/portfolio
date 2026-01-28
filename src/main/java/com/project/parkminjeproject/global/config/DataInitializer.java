package com.project.parkminjeproject.global.config;

import com.project.parkminjeproject.domain.portfolio.entity.Portfolio;
import com.project.parkminjeproject.domain.user.entity.User;
import com.project.parkminjeproject.domain.portfolio.repository.PortfolioRepository;
import com.project.parkminjeproject.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // ✅ 관리자 계정이 없으면 생성
        User admin = null;
        if (userRepository.count() == 0) {
            admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setName("관리자");
            admin.setEmail("admin@example.com");
            admin.setRole("ROLE_ADMIN");
            admin.setEnabled(true);
            admin = userRepository.save(admin);

            System.out.println("✅ 관리자 계정이 생성되었습니다!");
            System.out.println("   아이디: admin");
            System.out.println("   비밀번호: admin123");
        }

        // ✅ 포트폴리오가 없으면 샘플 데이터 생성
        if (portfolioRepository.count() == 0) {
            // 관리자 계정 조회 (없으면 생성했고, 있으면 조회)
            if (admin == null) {
                admin = userRepository.findAll().stream()
                        .filter(u -> "ROLE_ADMIN".equals(u.getRole()))
                        .findFirst()
                        .orElse(null);
            }

            // ✅ 관리자가 있을 때만 샘플 생성
            if (admin != null) {
                createSamplePortfolios(admin);
                System.out.println("✅ 샘플 포트폴리오 3개가 생성되었습니다!");
            }
        }
    }

    private void createSamplePortfolios(User admin) {
        // 샘플 포트폴리오 1
        Portfolio portfolio1 = new Portfolio();
        portfolio1.setUser(admin);
        portfolio1.setTitle("쇼핑몰 웹사이트 제작");
        portfolio1.setCategory("웹 개발");
        portfolio1.setSummary("Spring Boot와 React를 활용한 풀스택 쇼핑몰");
        portfolio1.setDescription("Spring Boot와 React를 활용한 풀스택 쇼핑몰 프로젝트입니다. " +
                "사용자 인증, 상품 관리, 장바구니, 결제 시스템을 구현했습니다. " +
                "RESTful API 설계와 JWT 토큰 기반 인증을 적용했습니다.");
        portfolio1.setImageUrl("https://images.unsplash.com/photo-1563013544-824ae1b704d3?w=800");
        portfolio1.setProjectUrl("https://github.com/yourusername/shopping-mall");
        portfolio1.setTechnologies("Spring Boot,React,MySQL,JWT");
        portfolio1.setDuration("3개월");
        portfolio1.setRole("풀스택 개발자");
        portfolio1.setPublished(true);
        portfolioRepository.save(portfolio1);

        // 샘플 포트폴리오 2
        Portfolio portfolio2 = new Portfolio();
        portfolio2.setUser(admin);
        portfolio2.setTitle("날씨 예보 앱");
        portfolio2.setCategory("앱 개발");
        portfolio2.setSummary("실시간 날씨 정보 제공 모바일 앱");
        portfolio2.setDescription("OpenWeather API를 활용한 실시간 날씨 정보 제공 앱입니다. " +
                "위치 기반 서비스로 현재 위치의 날씨를 자동으로 표시하며, " +
                "5일간의 예보와 시간별 상세 정보를 제공합니다.");
        portfolio2.setImageUrl("https://images.unsplash.com/photo-1592210454359-9043f067919b?w=800");
        portfolio2.setProjectUrl("https://github.com/yourusername/weather-app");
        portfolio2.setTechnologies("React Native,Redux,OpenWeather API");
        portfolio2.setDuration("2개월");
        portfolio2.setRole("모바일 개발자");
        portfolio2.setPublished(true);
        portfolioRepository.save(portfolio2);

        // 샘플 포트폴리오 3
        Portfolio portfolio3 = new Portfolio();
        portfolio3.setUser(admin);
        portfolio3.setTitle("포트폴리오 웹사이트");
        portfolio3.setCategory("디자인");
        portfolio3.setSummary("모던하고 반응형인 개인 포트폴리오 사이트");
        portfolio3.setDescription("개인 포트폴리오를 위한 반응형 웹사이트 디자인입니다. " +
                "모던하고 깔끔한 UI/UX 디자인을 적용했으며, " +
                "모바일, 태블릿, 데스크톱 모든 화면 크기에 최적화되어 있습니다.");
        portfolio3.setImageUrl("https://images.unsplash.com/photo-1467232004584-a241de8bcf5d?w=800");
        portfolio3.setProjectUrl("https://www.example-portfolio.com");
        portfolio3.setTechnologies("HTML,CSS,JavaScript,Figma");
        portfolio3.setDuration("1개월");
        portfolio3.setRole("UI/UX 디자이너");
        portfolio3.setPublished(true);
        portfolioRepository.save(portfolio3);
    }
}