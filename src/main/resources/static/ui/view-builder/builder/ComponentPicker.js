// 옵션패널의 "컴포넌트 추가" 버튼이 여는 컴포넌트 선택 모달.
// 드래그 앤 드롭이 불편한 환경(터치 기기 등)에서도 선택한 컨테이너 안에 컴포넌트를
// 넣을 수 있게 한다. 목록은 좌측 팔레트와 같은 ComponentPanel.catalog()를 쓰고,
// 넣을 수 있는 타입(allowedIds)만 보여준다.
//
// 공용 ModalPopup(#popup)은 URL을 불러오는 방식이라 여기서는 쓰지 않고, 부트스트랩
// 모달을 body에 한 번만 만들어두고 재사용한다. 모달은 #app-container 밖(body 최상위)에
// 붙으므로 스타일은 vb-theme.css의 #vb-component-picker 스코프에 둔다.
class ComponentPicker {
    constructor() {
        this.modalId = 'vb-component-picker';
        this.onSelect = null;
        this.$modal = this.createModal();
    }

    createModal() {
        $('#' + this.modalId).remove();

        const $modal = $(`
            <div class="modal fade" id="${this.modalId}" tabindex="-1" role="dialog" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title"><i class="fas fa-plus-square"></i> 컴포넌트 추가</h5>
                            <button type="button" class="close" data-dismiss="modal" aria-label="닫기">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body">
                            <div class="vb-picker-search">
                                <input type="text" placeholder="컴포넌트 검색" autocomplete="off">
                                <i class="fas fa-search"></i>
                            </div>
                            <div class="vb-picker-list"></div>
                        </div>
                    </div>
                </div>
            </div>
        `);

        $('body').append($modal);

        $modal.find('.vb-picker-search input').on('input', (e) => this.filter(e.target.value));

        $modal.on('click', '.vb-picker-item', (e) => {
            const type = $(e.currentTarget).data('type');
            const onSelect = this.onSelect;

            this.onSelect = null;
            $modal.modal('hide');

            if (onSelect) {
                onSelect(type);
            }
        });

        // 열린 뒤 검색창에 바로 입력할 수 있게 한다(모바일에서는 키보드가 화면을 가리므로 생략).
        $modal.on('shown.bs.modal', () => {
            if (!window.matchMedia('(max-width: 991.98px)').matches) {
                $modal.find('.vb-picker-search input').trigger('focus');
            }
        });

        $modal.on('hidden.bs.modal', () => {
            this.onSelect = null;
        });

        return $modal;
    }

    open(allowedIds, onSelect) {
        this.onSelect = onSelect;

        this.render(allowedIds || []);
        this.$modal.find('.vb-picker-search input').val('');
        this.$modal.modal('show');
    }

    render(allowedIds) {
        const $list = this.$modal.find('.vb-picker-list').empty();

        for (const category of ComponentPanel.catalog()) {
            const items = category.items.filter(item => allowedIds.includes(item.id));
            if (items.length === 0) {
                continue;
            }

            const $category = $(`
                <div class="vb-picker-category">
                    <div class="vb-picker-category-title"><i class="${category.icon}"></i><span></span></div>
                    <div class="vb-picker-grid"></div>
                </div>
            `);
            $category.find('.vb-picker-category-title span').text(category.name);

            const $grid = $category.find('.vb-picker-grid');
            for (const item of items) {
                const $item = $(`
                    <button type="button" class="vb-picker-item">
                        <i class="${item.icon}"></i>
                        <span class="vb-picker-item-label"></span>
                    </button>
                `);
                $item.attr('data-type', item.id);
                $item.attr('title', item.description || '');
                $item.find('.vb-picker-item-label').text(item.label);
                $grid.append($item);
            }

            $list.append($category);
        }

        if ($list.children().length === 0) {
            $list.append(`<div class="vb-picker-empty">추가할 수 있는 컴포넌트가 없습니다.</div>`);
        }
    }

    filter(keyword) {
        const kw = (keyword || '').trim().toLowerCase();

        this.$modal.find('.vb-picker-category').each(function() {
            const $category = $(this);
            let hasMatch = false;

            $category.find('.vb-picker-item').each(function() {
                const $item = $(this);
                const text = ($item.find('.vb-picker-item-label').text() + ' ' + ($item.attr('title') || '')).toLowerCase();
                const match = !kw || text.includes(kw);

                $item.toggle(match);
                if (match) hasMatch = true;
            });

            $category.toggle(hasMatch);
        });
    }
}
