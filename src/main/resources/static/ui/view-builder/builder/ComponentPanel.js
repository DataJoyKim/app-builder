class ComponentPanel {
    constructor() {}

    init(panelId) {
        let $panel = $("#"+panelId);
        this.panelId = panelId;

        $panel.append(this.toolbar());

        let $listWrap = $(`<div class="vb-component-list"></div>`);

        for (const category of ComponentPanel.catalog()) {
            const $categoryEl = this.category(category.name, true, category.icon);
            for (const item of category.items) {
                this.item($categoryEl, item.id, item.label, item.icon, item.description);
            }
            $listWrap.append($categoryEl);
        }

        $panel.append($listWrap);
    }

    // 팔레트에 보이는 컴포넌트 목록. 좌측 팔레트와 옵션패널의 "컴포넌트 추가" 모달
    // (ComponentPicker)이 같은 목록을 쓰도록 한 곳에 둔다.
    static catalog() {
        return [
            {name: '레이아웃', icon: 'fas fa-th-large', items: [
                {id: 'row', label: 'Row', icon: 'fas fa-grip-lines', description: '가로로 나열되는 행(Row) 컨테이너'},
                {id: 'col', label: 'Col', icon: 'fas fa-grip-lines-vertical', description: 'Row 안에서 폭을 나누는 열(Col) 컨테이너'}
            ]},
            {name: '컨테이너', icon: 'fas fa-square', items: [
                {id: 'card', label: 'Card', icon: 'fas fa-window-maximize', description: '제목과 내용 영역을 가진 카드'},
                {id: 'form', label: 'Form', icon: 'fas fa-clipboard-list', description: '입력 요소를 하나로 묶는 폼 영역'}
            ]},
            {name: '데이터 그리드', icon: 'fas fa-table', items: [
                {id: 'sheet', label: 'Sheet', icon: 'fas fa-table', description: '행/열 데이터를 표로 보여주는 그리드'}
                //{id: 'grid', label: 'jsGrid', icon: 'fas fa-table'}
            ]},
            {name: '차트', icon: 'fas fa-chart-bar', items: [
                {id: 'barline-chart', label: 'Bar/Line', icon: 'fas fa-chart-bar', description: '막대/선 그래프 차트'},
                {id: 'pie-chart', label: 'Pie', icon: 'fas fa-chart-pie', description: '원형(파이/도넛) 차트'}
            ]},
            {name: '버튼', icon: 'far fa-hand-pointer', items: [
                {id: 'button', label: 'Button', icon: 'far fa-hand-pointer', description: '클릭 가능한 버튼'}
            ]},
            {name: '폼 입력 요소', icon: 'fas fa-keyboard', items: [
                {id: 'input', label: 'Input', icon: 'fas fa-i-cursor', description: '한 줄 텍스트 입력창'},
                {id: 'select', label: 'Select', icon: 'fas fa-caret-square-down', description: '드롭다운 선택 목록'},
                {id: 'date-input', label: 'Date', icon: 'far fa-calendar-alt', description: '달력에서 고르는 날짜 입력창'},
                {id: 'textarea', label: 'TextArea', icon: 'fas fa-align-left', description: '여러 줄 텍스트 입력창'},
                {id: 'checkbox', label: 'Checkbox', icon: 'far fa-check-square', description: '단일 체크박스'},
                {id: 'radio', label: 'Radio', icon: 'far fa-dot-circle', description: '항목을 직접 지정하는 라디오 버튼 그룹'},
                {id: 'toggle', label: 'Toggle', icon: 'fas fa-toggle-on', description: '스위치 모양의 on/off 입력'}
            ]},
            {name: '기타', icon: 'fas fa-ellipsis-h', items: [
                {id: 'custom-html', label: 'Html', icon: 'fas fa-code', description: '직접 작성하는 HTML 영역'}
            ]}
        ];
    }

    /* ================= 검색 + 전체 펼치기/접기 ================= */

    toolbar() {
        const $toolbar = $(`
            <div class="vb-component-toolbar">
                <div class="vb-component-search">
                    <input type="text" placeholder="컴포넌트 검색" autocomplete="off">
                    <i class="fas fa-search"></i>
                </div>
                <button type="button" class="vb-component-toggle-all" title="전체 접기/펼치기">
                    <i class="fas fa-compress-alt"></i>
                </button>
            </div>
        `);

        $toolbar.find('input').on('input', (e) => this.filter(e.target.value));

        let expanded = true;
        $toolbar.find('.vb-component-toggle-all').on('click', (e) => {
            expanded = !expanded;

            $('#'+this.panelId+' .vb-category').toggleClass('open', expanded);

            $(e.currentTarget).find('i').attr('class', expanded ? 'fas fa-compress-alt' : 'fas fa-expand-alt');
        });

        return $toolbar;
    }

    filter(keyword) {
        const kw = (keyword || '').trim().toLowerCase();
        const $panel = $('#'+this.panelId);

        // 카테고리를 먼저 숨겨버리면 그 안의 component-item은 jQuery :visible 판정상
        // "조상이 숨겨져 있으니 visible 아님"이 되어버려 검색어를 지워도 다시 안 나타나는
        // 문제가 있었다. :visible로 재조회하지 않고 매칭 여부를 직접 계산해서 반영한다.
        $panel.find('.vb-category').each(function() {
            const $category = $(this);
            let hasMatch = false;

            $category.find('.component-item').each(function() {
                const label = $(this).find('label').text().toLowerCase();
                const match = !kw || label.includes(kw);

                $(this).toggle(match);
                if (match) hasMatch = true;
            });

            $category.toggle(hasMatch);

            if (kw && hasMatch) {
                $category.addClass('open');
            }
        });
    }

    /* ================= 카테고리 / 아이템 ================= */

    category(name, open, icon) {
        const $category = $(`
            <div class="vb-category ${open ? 'open' : ''}">
                <div class="vb-category-header" role="button" tabindex="0">
                    <i class="vb-category-chevron fas fa-chevron-right"></i>
                    <i class="vb-category-icon ${icon}"></i>
                    <span>${name}</span>
                </div>
                <div class="vb-category-body">
                    <div class="vb-category-grid"></div>
                </div>
            </div>
        `);

        // 부드러운 펼치기/접기 애니메이션을 위해 네이티브 <details> 대신 클래스 토글 방식을
        // 직접 구현한다 (details의 open/close는 애니메이션 없이 즉시 튀는 느낌이 있었다).
        const toggle = () => $category.toggleClass('open');

        $category.find('.vb-category-header')
            .on('click', toggle)
            .on('keydown', function(e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    toggle();
                }
            });

        // 팔레트 카테고리는 "밖으로 복제해서 꺼내가는 것"만 가능한 소스 전용 리스트다
        // (여기서 순서를 바꾸거나, 캔버스의 컴포넌트를 여기로 되돌려 놓는 건 허용하지 않는다).
        Sortable.create($category.find('.vb-category-grid')[0], {
            group: {name: 'vb-components', pull: 'clone', put: false},
            sort: false,
            animation: 0,
            filter: function() {
                if (!$('#objectCode').val()) {
                    alert('오브젝트 코드를 입력해주세요.');
                    return true;
                }
                return false;
            },
            preventOnFilter: true
        });

        return $category;
    }

    item($category, componentId, label, icon, description) {
        $category.find('.vb-category-grid').append(this.component(componentId, label, icon, description));
    }

    component(componentId, label, icon, description) {
        return $(`
            <div class="component-item" data-type="component-${componentId}" title="${description || ''}">
                <i class="${icon}"></i>
                <label>${label}</label>
            </div>
        `);
    }
}
