class OptionPanel {
    constructor() {}

    init(panelId, optionInfo) {
        this.panelId = panelId;

        let metadata = {};
        for(const info of optionInfo) {
            metadata[info.id] = info;
        }

        this.metadata = metadata;
    }

    getHtml(id) {
        const info = this.metadata[id];
        if(info.type == 'text') {
            return this.input(info.id, {label:info.label, size:info.size});
        }
        else if(info.type == 'code') {
            return this.codeEditor(info.id, {label:info.label, size:info.size});
        }
        else if(info.type == 'sheet') {
            return this.sheet(info.id, {label:info.label, size:info.size});
        }
    }

    setOptionValue(options) {
        for(const id in this.metadata) {
            const info = this.metadata[id];

            if(info.type == 'text') {
                this.setValue(id, options[id]);
            }
            else if(info.type == 'code') {
                this.setCodeEditorValue(id, options[id]);
            }
            else if(info.type == 'sheet') {
                this.setSheetValue(id, options[id]);
            }
        }
    }

    getOptionValue() {
        let optionValue = {};
        for(const id in this.metadata) {
            const info = this.metadata[id];

            if(info.type == 'text') {
                optionValue[id] = this.getValue(id);
            }
            else if(info.type == 'code') {
                optionValue[id] = this.getCodeEditorValue(id);
            }
            else if(info.type == 'sheet') {
                optionValue[id] = this.getSheetValue(id);
            }
        }

        return optionValue;
    }

    elementId(id) {
        return this.panelId + '-' + id;
    }

    getSize($target) {
        return $target.attr("class").split(/\s+/).find(cls => cls.startsWith("col-"));
    }

    optionSize() {
        let html = `<option value="col-auto">col-auto</option>`;

        for(let i=12; i>=1; i--) {
            html += `<option value="col-${i}">col-${i}</option>`;
        }

        return html;
    }

    select(id, option) {
        return $(`
            <div class="form-group ${option.size}">
               <label for="${this.elementId(id)}">${option.label}</label>
               <select type="text" class="form-control rounded-0" id="${this.elementId(id)}">
                   ${option.options}
               </select>
            </div>
        `);
    }

    button(id, option) {
        const formGroupEl = this.formGroup(option);

        // label이 없는 버튼도 옆에 나란히 오는 label 있는 필드와 높이가 어긋나지 않도록
        // 항상 라벨 자리(빈 라벨)를 확보해둔다.
        if(option.label) {
            formGroupEl.append(this.label(id, option));
        }
        else {
            formGroupEl.append(`<label class="vb-opt-label-spacer">&nbsp;</label>`);
        }

        let html = ``;
        html += `<button id="${this.elementId(id)}" type="button" class="btn btn-default btn-sm form-control">`;
        if(option.icon) {
            html += `<i class="${option.icon}"></i>`;
        }
        if(option.btnLabel) {
            html += `<span>${option.btnLabel}</span>`;
        }
        html += `</button>`;

        formGroupEl.append($(html));

        return formGroupEl;
    }

    input(id, option) {
        const formGroupEl = this.formGroup(option);

        formGroupEl.append(this.label(id, option));

        const inputEl = $(`<input type="text" class="form-control form-control-sm rounded-0" id="${this.elementId(id)}">`);
        inputEl.attr('spellcheck',false);
        inputEl.attr('autocomplete','off');
        inputEl.prop('readonly', !(option.enabled ?? true));
        if(option.placeholder) {
            inputEl.attr('placeholder',option.placeholder);
        }

        if(option.value != undefined) {
            inputEl.val(option.value);
        }

        formGroupEl.append(inputEl);

        return formGroupEl;
    }

    codeEditor(id, option) {
        option.size = option.size || 'col-12';

        const formGroupEl = this.formGroup(option);
        formGroupEl.addClass('vb-opt-block');

        formGroupEl.append(this.label(id, option));

        const textareaEl = $(`<textarea id="${id}"  ></textarea>`);

        formGroupEl.append(textareaEl);

        return formGroupEl;
    }

    codeEditorScript(id, width, height) {
        let textarea = document.getElementById(id);

        let codeEditor = CodeMirror.fromTextArea(textarea, {
            lineNumbers: true,
            lineWrapping: true,
            theme: "darcula",
            mode: "text/javascript",
            val: textarea.value
        });

        codeEditor.setSize(width, height);

        window['_codeEditor_'+id] = codeEditor;
    }

    sheet(id, option) {
        option.size = option.size || 'col-12';

        const formGroupEl = this.formGroup(option);
        formGroupEl.addClass('vb-opt-block');

        const headerEl = $(`<div class="vb-opt-block-header"></div>`);
        headerEl.append(this.label(id, option));
        headerEl.append($(`<button type="button" class="btn btn-default btn-sm" onclick="_sheet_${id}.addRowData({})"><i class="fas fa-plus"></i><span>입력</span></button>`));
        formGroupEl.append(headerEl);

        const divEl = $(`<div id="${id}"  ></div>`);

        formGroupEl.append(divEl);

        return formGroupEl;
    }

    sheetScript(id, width, height, columns) {
        const sheet = new Sheet(id, width, height,{useSeq:false,useStatus:false,useDelete:true}, columns);
        window['_sheet_'+id] = sheet;

        return sheet;
    }

    /* 라디오 항목(라벨/값)처럼 같은 모양의 행을 여러 개 직접 입력받는 컨트롤.
       columns는 [{key, placeholder}] 형태이고, 행 추가/삭제/입력이 일어날 때마다
       itemListEvent()에 등록한 핸들러로 현재 목록 전체를 넘긴다.
       (Sheet 기반 sheet()는 메타데이터로 동작하는 액션 옵션패널용이라,
        입력 즉시 반영되는 컴포넌트 옵션패널에는 이쪽이 맞다.) */
    itemList(id, option) {
        option.size = option.size || 'col-12';

        const formGroupEl = this.formGroup(option);
        formGroupEl.addClass('vb-opt-block');

        const headerEl = $(`<div class="vb-opt-block-header"></div>`);
        headerEl.append(this.label(id, option));
        headerEl.append($(`<button type="button" class="btn btn-default btn-sm" id="${this.elementId(id)}-add"><i class="fas fa-plus"></i><span>추가</span></button>`));
        formGroupEl.append(headerEl);

        formGroupEl.append($(`<div class="vb-opt-item-list" id="${this.elementId(id)}"></div>`));

        return formGroupEl;
    }

    itemListRow(columns, item) {
        const rowEl = $(`<div class="vb-opt-item-row"></div>`);

        for(const column of columns) {
            const inputEl = $(`<input type="text" class="form-control form-control-sm rounded-0">`);
            inputEl.attr('data-key', column.key);
            inputEl.attr('placeholder', column.placeholder || column.key);
            inputEl.attr('spellcheck', false);
            inputEl.attr('autocomplete', 'off');
            inputEl.val(item?.[column.key] ?? '');

            rowEl.append(inputEl);
        }

        rowEl.append($(`<button type="button" class="btn btn-default btn-sm vb-opt-item-remove" title="삭제"><i class="fas fa-times"></i></button>`));

        return rowEl;
    }

    setItemListValue(id, columns, items) {
        const listEl = $('#'+this.elementId(id));
        listEl.empty();

        for(const item of items ?? []) {
            listEl.append(this.itemListRow(columns, item));
        }
    }

    getItemListValue(id, columns) {
        const items = [];

        $('#'+this.elementId(id)).children('.vb-opt-item-row').each(function() {
            const item = {};

            for(const column of columns) {
                item[column.key] = $(this).find(`input[data-key="${column.key}"]`).val();
            }

            items.push(item);
        });

        return items;
    }

    itemListEvent(id, columns, _handler) {
        const listId = this.elementId(id);
        const notify = () => _handler(this.getItemListValue(id, columns));

        $('#'+listId+'-add').off('click').on('click', () => {
            $('#'+listId).append(this.itemListRow(columns, null));
            notify();
        });

        // 행이 추가/삭제되며 계속 바뀌므로 목록 컨테이너에 위임해서 건다.
        $('#'+listId)
            .off('input.vbItemList click.vbItemList')
            .on('input.vbItemList', 'input', notify)
            .on('click.vbItemList', '.vb-opt-item-remove', function() {
                $(this).closest('.vb-opt-item-row').remove();
                notify();
            });
    }

    toggle(id, option) {
        const formGroupEl = this.formGroup(option);
        formGroupEl.addClass('vb-opt-toggle-row');

        formGroupEl.append(this.label(id, option));

        const toggleEl = $(`
                <div class="custom-control custom-switch">
                    <input type="checkbox" class="custom-control-input" id="${this.elementId(id)}">
                    <label for="${this.elementId(id)}" class="custom-control-label" style="cursor: pointer;"></label>
                </div>
            `)

        formGroupEl.append(toggleEl);

        return formGroupEl;
    }

    /* 여러 필드를 묶어서 구분해주는 섹션 제목. formGroup을 거치지 않는 순수
       구분선이라 col-* 클래스가 없어도 되며, #options가 있어야 항상 한 줄
       전체를 차지하도록 CSS(vb-theme.css)에서 flex:0 0 100%로 고정한다. */
    sectionTitle(text) {
        return $(`<div class="vb-opt-section-title">${text}</div>`);
    }

    // optionPanelView()가 채워놓은 평평한 목록(섹션 제목 표시 + 필드들이 나란히 나열된 형태)을
    // 컴포넌트 팔레트와 같은 톤의 접을 수 있는 카드 섹션으로 다시 묶는다. 각 컴포넌트 파일의
    // optionPanelView()는 그대로 sectionTitle()/필드를 순서대로 append하기만 하면 되고,
    // 실제 카드 구조로 바꾸는 건 여기서 한 번에 처리한다.
    groupSections($panel) {
        const $children = $panel.children().toArray();
        if ($children.length === 0) return;

        const hasSectionTitle = $children.some(el => el.classList.contains('vb-opt-section-title'));
        if (!hasSectionTitle) return;

        $panel.empty();

        let $currentGrid = null;

        for (const el of $children) {
            const $el = $(el);

            if ($el.hasClass('vb-opt-section-title')) {
                const $section = this.sectionCard($el.text());
                $panel.append($section);
                $currentGrid = $section.find('.vb-opt-section-grid');
            }
            else if ($currentGrid) {
                $currentGrid.append($el);
            }
            else {
                // 섹션 제목보다 먼저 나온 필드는(있다면) 그냥 패널에 바로 붙인다.
                $panel.append($el);
            }
        }
    }

    sectionCard(title) {
        const $section = $(`
            <div class="vb-opt-section open">
                <div class="vb-opt-section-header" role="button" tabindex="0">
                    <i class="vb-opt-section-chevron fas fa-chevron-right"></i>
                    <span>${title}</span>
                </div>
                <div class="vb-opt-section-body">
                    <div class="vb-opt-section-grid"></div>
                </div>
            </div>
        `);

        const toggle = () => $section.toggleClass('open');

        $section.find('.vb-opt-section-header')
            .on('click', toggle)
            .on('keydown', function(e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    toggle();
                }
            });

        return $section;
    }

    formGroup(option) {
        return $(`<div class="form-group ${option.size || ''}"></div>`);
    }

    label(id, option) {
        return $(`<label for="${this.elementId(id)}">${option.label}</label>`);
    }

    row() {
        return $(`<div class="row"></div>`);
    }

    clickEvent(id, _handler) {
        $(document).off("click", "#"+this.elementId(id)).on("click", "#"+this.elementId(id), _handler);
    }

    inputEvent(id, _handler) {
        $("#"+this.elementId(id)).off("input").on("input",_handler);
    }

    changeEvent(id, _handler) {
        $("#"+this.elementId(id)).off("change").on("change",_handler);
    }

    setValue(id,value) {
        $('#'+this.elementId(id)).val(value);
    }

    getValue(id) {
        return $('#'+this.elementId(id)).val();
    }

    getCodeEditorValue(id) {
        return window['_codeEditor_'+id].getValue();
    }

    setCodeEditorValue(id,value) {
        window['_codeEditor_'+id].setValue(value);
    }

    getSheetValue(id) {
         let columns = window['_sheet_'+id].getSheetData();

         return columns.map(({ _status, _delete, _seq, _dnd, ...rest }) => rest);
    }

    setSheetValue(id,value) {
        if(value) {
            for(let v of value) {
                v._status = "C";
            }
        }
        else {
            value = [];
        }

        window['_sheet_'+id].setSheetData(value);
    }

    check(id,value) {
        $('#'+this.elementId(id)).prop('checked',value);
    }
}