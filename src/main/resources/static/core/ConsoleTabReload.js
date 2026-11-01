/**
 * 콘솔 탭(AdminLTE IFrame 모드) 새로고침 처리.
 *
 * 콘솔은 바깥 페이지(index.html) 안에 메뉴마다 탭(iframe)을 띄운다. 그냥 두면 F5 / Ctrl+R 이
 * 바깥 페이지를 다시 불러와 열려 있던 탭이 전부 사라진다. 이 스크립트는 두 가지를 한다.
 *
 * 1. F5 / Ctrl+R(Mac: Cmd+R)을 가로채 지금 보고 있는 탭(iframe)만 다시 불러온다.
 *    - 탭 화면(contents/content.html) 안에 포커스가 있으면 그 탭을,
 *      바깥 페이지(사이드바·탭 헤더 등)에 포커스가 있으면 활성 탭을 새로고침한다.
 *    - 탭 안의 모달 팝업(iframe) 안에서 눌러도 팝업이 아니라 그 팝업을 띄운 탭을 새로고침한다.
 *    - Ctrl+Shift+R 은 가로채지 않는다(전체 새로고침이 필요할 때 쓰는 탈출구).
 *
 * 2. 브라우저 새로고침 버튼, Ctrl+Shift+R 처럼 가로챌 수 없는 전체 새로고침이면,
 *    떠나기 직전(pagehide)에 열려 있던 탭 목록을 sessionStorage 에 저장해 뒀다가
 *    다시 열릴 때 같은 탭들을 열고 보던 탭을 활성화한다.
 *    - 새로고침(navigation type = reload)일 때만 복원한다. 로그아웃 후 다시 들어오거나
 *      주소를 새로 입력해 들어온 경우엔 빈 화면으로 시작한다.
 *    - 탭은 저장된 주소와 같은 사이드바(또는 상단 메뉴) 링크를 눌러서 다시 연다.
 *      메뉴에서 사라진 화면(권한 변경 등)은 자연히 건너뛴다.
 *      (AdminLTE의 $(...).IFrame('createTab', ...)은 인자를 넘기지 못해 쓸 수 없다.)
 *    - 저장 키는 페이지 경로별(/{applicationId}/console, 플랫폼 콘솔)로 나눠 서로 섞이지 않는다.
 *    - 탭 안에서 저장하지 않은 입력값은 복원되지 않는다.
 *
 * 바깥 페이지(console/app/index.html, console/platform/index.html)와
 * 탭 화면 공통 틀(console/app|platform/contents/content.html) 양쪽에서 불러오며,
 * 어느 쪽인지는 스스로 판단한다. 바깥 페이지에서는 adminlte.js 뒤에 불러와야 한다.
 */
(function() {
    const TAB_CONTENT_SELECTOR = '.content-wrapper.iframe-mode .tab-content';
    const TAB_NAV_SELECTOR = '.content-wrapper.iframe-mode .navbar-nav';
    // adminlte.js IFrame 이 탭을 여는 메뉴 링크 (SELECTOR_SIDEBAR_MENU_ITEM / HEADER_MENU_ITEM / HEADER_DROPDOWN_ITEM)
    const MENU_LINK_SELECTOR = '.main-sidebar .nav-item > a.nav-link, .main-header .nav-item a.nav-link, .main-header a.dropdown-item';
    const STORAGE_KEY = 'console-tabs:' + window.location.pathname;

    function isReloadKey(e) {
        if (e.shiftKey || e.altKey) {
            return false;
        }
        if (e.key === 'F5') {
            return !e.ctrlKey && !e.metaKey;
        }
        return (e.ctrlKey || e.metaKey) && (e.key === 'r' || e.key === 'R');
    }

    function hasTabLayout(win) {
        try {
            return !!win.document.querySelector(TAB_CONTENT_SELECTOR);
        }
        catch (e) {
            return false; // 다른 출처의 창이면 접근할 수 없다
        }
    }

    /* ================= 탭 화면(iframe) 쪽 ================= */

    // 이 창에서 바깥 페이지 바로 아래의 창(= 탭 iframe)까지 거슬러 올라간다.
    // 탭 안의 모달 팝업(iframe) 안이어도 그 팝업을 띄운 탭을 찾는다.
    function findTabWindow() {
        let win = window;
        try {
            while (win.parent !== win && win.parent !== window.top) {
                win = win.parent;
            }
        }
        catch (e) {
            return null;
        }

        if (win === window.top || !hasTabLayout(window.top)) {
            return null; // 탭 구조 밖에서 단독으로 열린 화면이면 브라우저 기본 동작에 맡긴다
        }

        return win;
    }

    function initInTab() {
        document.addEventListener('keydown', function(e) {
            if (!isReloadKey(e)) {
                return;
            }

            const tabWindow = findTabWindow();
            if (!tabWindow) {
                return;
            }

            e.preventDefault();
            tabWindow.location.reload();
        }, true);
    }

    /* ================= 바깥 페이지(탭 레이아웃) 쪽 ================= */

    function activeTabFrame() {
        return document.querySelector(TAB_CONTENT_SELECTOR + ' > .tab-pane.active iframe');
    }

    function initTabLayout() {
        document.addEventListener('keydown', function(e) {
            if (!isReloadKey(e)) {
                return;
            }

            const frame = activeTabFrame();
            if (!frame || !frame.contentWindow) {
                return; // 열린 탭이 없으면 그냥 전체 새로고침해도 잃을 게 없다
            }

            e.preventDefault();
            frame.contentWindow.location.reload();
        }, true);

        window.addEventListener('pagehide', saveTabs);
        window.addEventListener('load', function() {
            // adminlte.js 의 IFrame 플러그인도 window load 에서 초기화되므로 그 뒤에 실행한다
            setTimeout(restoreTabs, 0);
        });
    }

    function saveTabs() {
        try {
            const panes = document.querySelectorAll(TAB_CONTENT_SELECTOR + ' > .tab-pane');
            const tabs = [];
            let active = null;

            panes.forEach(function(pane) {
                const frame = pane.querySelector('iframe');
                const src = frame && frame.getAttribute('src');
                if (!src) {
                    return;
                }

                tabs.push(src);
                if (pane.classList.contains('active')) {
                    active = src;
                }
            });

            sessionStorage.setItem(STORAGE_KEY, JSON.stringify({tabs: tabs, active: active}));
        }
        catch (e) {
            // sessionStorage 를 쓸 수 없는 환경이면 복원만 포기한다
        }
    }

    function isReloadNavigation() {
        try {
            const entries = performance.getEntriesByType('navigation');
            if (entries && entries.length > 0) {
                return entries[0].type === 'reload';
            }
            return performance.navigation && performance.navigation.type === 1; // 구형 브라우저
        }
        catch (e) {
            return false;
        }
    }

    function readSavedTabs() {
        try {
            const state = JSON.parse(sessionStorage.getItem(STORAGE_KEY) || 'null');
            if (state && Array.isArray(state.tabs)) {
                return state;
            }
        }
        catch (e) {
            // 깨진 값이면 무시
        }
        return null;
    }

    function findMenuLink(href) {
        const links = document.querySelectorAll(MENU_LINK_SELECTOR);
        for (const link of links) {
            if (link.getAttribute('href') === href) {
                return link;
            }
        }
        return null;
    }

    function restoreTabs() {
        if (!isReloadNavigation() || typeof window.jQuery === 'undefined') {
            return;
        }

        const state = readSavedTabs();
        if (!state || state.tabs.length === 0) {
            return;
        }

        const $ = window.jQuery;

        // 메뉴 링크 클릭 → adminlte IFrame 이 탭을 연다(이미 열린 주소면 그 탭으로 전환만 한다)
        for (const href of state.tabs) {
            const link = findMenuLink(href);
            if (link) {
                $(link).trigger('click');
            }
        }

        // 마지막으로 열린 탭이 아니라, 새로고침 전에 보던 탭을 다시 활성화한다
        if (state.active) {
            const panes = document.querySelectorAll(TAB_CONTENT_SELECTOR + ' > .tab-pane');
            for (const pane of panes) {
                const frame = pane.querySelector('iframe');
                if (frame && frame.getAttribute('src') === state.active) {
                    const navLink = document.querySelector(TAB_NAV_SELECTOR + ' a.nav-link[aria-controls="' + pane.id + '"]');
                    if (navLink) {
                        $(navLink).trigger('click');
                    }
                    break;
                }
            }
        }
    }

    /* ================= 시작 ================= */

    if (window === window.top) {
        if (hasTabLayout(window) || document.querySelector('[data-widget="iframe"]')) {
            initTabLayout();
        }
        // 탭 구조 없이 단독으로 열린 화면이면 아무것도 하지 않는다(F5 는 브라우저 기본 동작)
    }
    else {
        initInTab();
    }
})();
