/**
 * 로그아웃. 토큰 쿠키를 지우고(POST /api/auth/logout) 로그인 화면으로 보낸다.
 * returnUrl 을 주면 다시 로그인한 뒤 그 주소로 돌아온다. 없으면 루트(/)로 돌아가 사용자 유형에 맞는 첫 화면으로 간다.
 * loginPath 를 주면 /login 대신 그 로그인 화면으로 보낸다(예: 애플리케이션 로그인 /{applicationId}/login).
 * 콘솔/생성 화면의 탭(iframe) 안에서 불려도 전체 창을 옮긴다.
 *   예) <a href="#" onclick="logout(); return false;">
 */
window.logout = function(returnUrl, loginPath) {
    if(!confirm('로그아웃 하시겠습니까?')) {
        return;
    }

    const loginBase = loginPath || '/login';
    const loginUrl = returnUrl ? loginBase + '?returnUrl=' + encodeURIComponent(returnUrl) : loginBase;

    fetch('/api/auth/logout', {method: 'POST'})
        .catch(function(error) {
            console.error('logout', error);
        })
        .finally(function() {
            window.top.location.href = loginUrl;
        });
};
