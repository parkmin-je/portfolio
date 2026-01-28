/**
 * CSRF 토큰 자동 처리 스크립트
 * 모든 관리자 페이지 <head> 태그에 추가하세요
 */

// 1. 쿠키에서 CSRF 토큰 읽기
function getCsrfToken() {
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
        const [name, value] = cookie.trim().split('=');
        if (name === 'XSRF-TOKEN') {
            return decodeURIComponent(value);
        }
    }
    return null;
}

// 2. 페이지 로드 시 CSRF 토큰을 meta 태그에 저장
document.addEventListener('DOMContentLoaded', function() {
    const token = getCsrfToken();
    if (token) {
        // meta 태그가 없으면 생성
        let metaTag = document.querySelector('meta[name="_csrf"]');
        if (!metaTag) {
            metaTag = document.createElement('meta');
            metaTag.setAttribute('name', '_csrf');
            document.head.appendChild(metaTag);
        }
        metaTag.setAttribute('content', token);

        let headerTag = document.querySelector('meta[name="_csrf_header"]');
        if (!headerTag) {
            headerTag = document.createElement('meta');
            headerTag.setAttribute('name', '_csrf_header');
            document.head.appendChild(headerTag);
        }
        headerTag.setAttribute('content', 'X-XSRF-TOKEN');

        console.log('✅ CSRF 토큰 로드됨:', token.substring(0, 20) + '...');
    } else {
        console.warn('⚠️ CSRF 토큰을 찾을 수 없습니다.');
    }
});

// 3. 모든 폼 제출 시 CSRF 토큰 자동 추가
document.addEventListener('submit', function(e) {
    const form = e.target;
    const token = getCsrfToken();

    if (token && form.method.toUpperCase() === 'POST') {
        // hidden input이 없으면 추가
        let csrfInput = form.querySelector('input[name="_csrf"]');
        if (!csrfInput) {
            csrfInput = document.createElement('input');
            csrfInput.type = 'hidden';
            csrfInput.name = '_csrf';
            form.appendChild(csrfInput);
        }
        csrfInput.value = token;
        console.log('✅ 폼에 CSRF 토큰 추가됨');
    }
});

// 4. fetch/axios 요청용 - 자동으로 헤더에 추가
if (window.fetch) {
    const originalFetch = window.fetch;
    window.fetch = function(url, options = {}) {
        const token = getCsrfToken();
        if (token && options.method && options.method.toUpperCase() !== 'GET') {
            options.headers = options.headers || {};
            options.headers['X-XSRF-TOKEN'] = token;
        }
        return originalFetch(url, options);
    };
}

// 5. jQuery AJAX용 (사용 시)
if (window.jQuery) {
    jQuery.ajaxSetup({
        beforeSend: function(xhr, settings) {
            if (settings.type !== 'GET') {
                const token = getCsrfToken();
                if (token) {
                    xhr.setRequestHeader('X-XSRF-TOKEN', token);
                }
            }
        }
    });
}