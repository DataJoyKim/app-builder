/**
 * 콘솔 공통 도움말 툴팁 (core/css/help-tooltip.css).
 *
 *   <span class="help-icon" tabindex="0" role="button" aria-label="도움말">?<span class="help-content">설명</span></span>
 *
 * .help-icon 에 마우스를 올리거나 포커스하면 안의 .help-content 를 툴팁으로 띄운다 (짧은 글은 data-help 속성도 된다).
 * body 에 위임해 두므로 나중에 그려지는 화면(팝업, 드로어)의 아이콘도 따로 초기화할 필요가 없고,
 * 툴팁이 body 에 붙어서 overflow:hidden 인 패널 안에서도 잘리지 않는다.
 * 설명의 HTML 은 Bootstrap 툴팁의 sanitizer 를 거친다 (code, br, strong 등만 남는다).
 */
(function($) {
    if(!$ || !$.fn || !$.fn.tooltip) {
        return;
    }

    $(function() {
        $('body').tooltip({
            selector: '.help-icon',
            placement: 'auto',
            trigger: 'hover focus',
            container: 'body',
            html: true,
            boundary: 'window',
            template: '<div class="tooltip help-tooltip" role="tooltip"><div class="arrow"></div><div class="tooltip-inner"></div></div>',
            title: function() {
                const $content = $(this).find('.help-content');
                return $content.length ? $content.html() : ($(this).attr('data-help') || '');
            }
        });
    });
})(window.jQuery);
