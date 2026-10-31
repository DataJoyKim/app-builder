// 컴포넌트 랜더링
$(function () {
    window.VB = window.VB || {};
    window.VB.version = '1.0.0';
    console.log(`version ${VB.version}`);

    window.VB.configs = {
        paths:{
            module:'/ui/view-builder',
            actions:'/actions',
            builder:'/builder',
            components:'/components',
            css:'/css',
            data:'/data',
            img:'/img',
            runtime:'/runtime'
        }
    };

    $.when(
        $.getScript(`${VB.configs.paths.module}/ModuleLoader.js`)
    )
    .done(function() {
        window.VB.module = new ModuleLoader(VB.version, VB.configs, true);

        VB.module.load(function(){
            const componentPanel = new ComponentPanel();
            componentPanel.init('component-panel');

            // 옵션패널의 "컴포넌트 추가" 버튼이 여는 컴포넌트 선택 모달 (ViewObject.initOptionPanel)
            window.VB.componentPicker = new ComponentPicker();

            ViewManager.init('layout');

            window.VB.builder = new ViewBuilder('canvas', VB.actionsFactory, VB.componentFactory, VB.dropComponent, VB.viewDataLoader);
            VB.builder.init();
        });
    });
});

// 옵션패널
$(document).on("click", ".vb-item", function(e) {
    e.stopPropagation();

    const $el = $(this);
    const type = $el.data("type");

    // 옵션패널을 여닫는 건 상단바 버튼(view-builder.html toggleOptionPanel)이 맡는다.
    VB.builder.setOptionPanel($el, type);
});

// 컴포넌트 삭제
$(document).on("click", ".component-delete-btn", function(e) {
    e.stopPropagation();

    $(this).closest(".component").remove();

    // 옵션 내용만 비우고 안내문구로 되돌린다 (패널 열림 상태는 유지)
    if(typeof window.resetOptionPanel === 'function') {
        window.resetOptionPanel();
    }
    else {
        VB.builder.clearOptionPanel($("#options"));
    }
});

// 컴포넌트 선택 영역
$(document).on("click", ".component", function(e) {
    e.stopPropagation();

    // 이전 선택 제거
    $(".component").removeClass("selected");
    $(".component-delete-btn").hide();

    // 현재 요소 선택
    $(this).addClass("selected");
    $(this).children(".component-delete-btn").show();
});