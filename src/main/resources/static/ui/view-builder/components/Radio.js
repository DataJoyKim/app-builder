class Radio extends ViewObject {
    constructor(optionPanel) {
        super(optionPanel);
        this.optionPanel = optionPanel;
    }

    componentId() {
        return 'radio';
    }

    componentOptions() {
        return {
           id:this.componentId() + super.getComponentIdNumber(),
           size:'col-auto',
           width:'250px',
           label:'Label',
           labelWidth:'120px',
           horizontal:true,
           editable:true,
           hidden:false,
           inline:true,
           defaultValue:'',
           items:[
               {label:'항목1', value:'1'},
               {label:'항목2', value:'2'}
           ]
       };
    }

    // 옵션패널의 항목 편집기가 쓰는 컬럼 정의. 저장되는 items 원소의 키와 같다.
    itemColumns() {
        return [
            {key:'label', placeholder:'라벨'},
            {key:'value', placeholder:'값'}
        ];
    }

/* =======================================
 * Element Define
 *
 * 항목(items)은 옵션패널에서 라벨/값 쌍으로 직접 입력받는다. 디자인 시점에
 * 목록이 확정되므로 빌더와 runtime이 똑같은 결과를 그린다.
 *
 * id 규칙: 그룹 컨테이너가 options.id, 각 input은 options.id-{index},
 * input의 name은 모두 options.id라서 input[name="{id}"]:checked 로 선택값을 읽는다.
 * (name이 같아야 브라우저가 하나만 선택되도록 묶어준다.)
 * ======================================= */
    element(options, isBuilder) {
        let $el = $(`<div class="form-group ${options.size}"></div>`);
        if(isBuilder) {
            $el.addClass('component');
            $el.addClass('vb-item');
            $el.attr('data-type', this.componentId());
            $el.append(super.componentDeleteBtn());
        }

        if(options.width) {
            $el.css('width', options.width);
        }

        if(options.horizontal) {
            $el.addClass('d-flex');
        }

        if(options.hidden) {
            if(!isBuilder) {
                $el.removeClass('d-flex');
                $el.addClass('d-none');
            }
        }

        let $labelEL = $(`<label for="${options.id}">${options.label}</label>`);
        $labelEL.css('margin-right','20px');

        if(options.labelWidth) {
            $labelEL.css('width', options.labelWidth);
        }

        if(options.label) {
            $el.append($labelEL);
        }
        else {
            if(isBuilder) {
                $labelEL.prop('hidden',true);
                $el.append($labelEL);
            }
        }

        // 라디오를 감싸는 그룹 래퍼. runtime에는 styleBuilder()가 주입되지 않으므로
        // 수평배치(d-flex)에서 폭이 무너지지 않도록 flex 값은 인라인으로 준다.
        let $groupEl = $(`<div class="vb-radio-group" id="${options.id}"></div>`);
        $groupEl.css({'flex':'1 1 auto', 'min-width':'0'});

        this.renderItems($groupEl, options);

        $el.append($groupEl);

        return $el;
    }

    renderItems($groupEl, options) {
        $groupEl.empty();

        (options.items ?? []).forEach((item, index) => {
            $groupEl.append(this.itemElement(options, `${options.id}-${index}`, item.value, item.label));
        });
    }

    itemElement(options, itemId, value, text) {
        const $item = $(`<div class="custom-control custom-radio"></div>`);
        if(options.inline) {
            $item.addClass('custom-control-inline');
        }

        const $inputEl = $(`<input type="radio" class="custom-control-input">`);
        $inputEl.attr('id'   , itemId);
        $inputEl.attr('name' , options.id);
        $inputEl.attr('value', value ?? '');
        $inputEl.attr('data-watch', 'true');
        $inputEl.prop('checked' , this.isDefaultChecked(options, value));
        $inputEl.prop('disabled', !options.editable);

        const $labelEl = $(`<label class="custom-control-label" for="${itemId}"></label>`);
        $labelEl.css('cursor', options.editable ? 'pointer' : 'default');
        $labelEl.text(text ?? '');

        $item.append($inputEl);
        $item.append($labelEl);

        return $item;
    }

    // defaultValue가 비어있으면 "초기 선택 없음"이므로, 값이 빈 항목이
    // 의도치 않게 선택되지 않도록 빈 값끼리의 매칭은 제외한다.
    isDefaultChecked(options, value) {
        if(options.defaultValue === '' || options.defaultValue == null) {
            return false;
        }

        return String(options.defaultValue) === String(value ?? '');
    }

/* =======================================
 * Runtime Component Setting
 * ======================================= */
    renderRuntime(options, children) {
        return this.element(options, false);
    }

    scriptRuntime(el, options) {}

/* =======================================
 * Builder Component Setting
 * ======================================= */
    renderBuilder(options) {
        return this.element(options, true);
    }

    styleBuilder() {
        return `
            .vb-item[data-type="${this.componentId()}"] {
                padding: 10px;
                height: auto;
                margin: 0 !important;
                border-radius: 6px;
                transition: background-color .12s ease;
            }
            .vb-item[data-type="${this.componentId()}"]:hover {
                background-color: #fafbfc;
            }
        `;
    }

    getElement($el) {
        return {
            groupEl:$el.children(".vb-radio-group"),
            labelEl:$el.children("label")
        }
    }

/* =======================================
 * Option Panel Setting
 * ======================================= */
    optionPanelView($panel, options) {
        $panel.append(this.optionPanel.sectionTitle('기본'));
        $panel.append(this.optionPanel.input('component-id',{label:'컴포넌트명', size:'col-6', enabled:false}));
        $panel.append(this.optionPanel.input('id',{label:'ID', size:'col-6'}));
        $panel.append(this.optionPanel.select('size',{label:'크기', size:'col-6', options:this.optionPanel.optionSize()}));
        $panel.append(this.optionPanel.input('width',{label:'width', size:'col-6'}));

        $panel.append(this.optionPanel.sectionTitle('라벨'));
        $panel.append(this.optionPanel.input('label',{label:'라벨', size:'col-6'}));
        $panel.append(this.optionPanel.input('labelWidth',{label:'라벨 width', size:'col-6'}));
        $panel.append(this.optionPanel.toggle('horizontal',{label:'수평배치', size:'col-12'}));

        $panel.append(this.optionPanel.sectionTitle('항목'));
        $panel.append(this.optionPanel.itemList('items',{label:'라디오 항목', columns:this.itemColumns()}));
        $panel.append(this.optionPanel.input('defaultValue',{label:'기본 선택 값', size:'col-6', placeholder:'미입력시 선택 없음'}));
        $panel.append(this.optionPanel.toggle('inline',{label:'항목 가로나열', size:'col-12'}));

        $panel.append(this.optionPanel.sectionTitle('동작'));
        $panel.append(this.optionPanel.toggle('editable',{label:'editable', size:'col-12'}));
        $panel.append(this.optionPanel.toggle('hidden',{label:'hidden', size:'col-12'}));
    }

    optionPanelScript($el, options) {
        this.optionPanel.setValue('component-id',this.componentId());
        this.optionPanel.setValue('id',options.id);
        this.optionPanel.setValue('size',options.size);
        this.optionPanel.setValue('width',options.width);
        this.optionPanel.setValue('labelWidth',options.labelWidth);
        this.optionPanel.setValue('label',options.label);
        this.optionPanel.setValue('defaultValue',options.defaultValue);
        this.optionPanel.setItemListValue('items',this.itemColumns(),options.items);
        this.optionPanel.check('horizontal',options.horizontal);
        this.optionPanel.check('inline',options.inline);
        this.optionPanel.check('editable',options.editable);
        this.optionPanel.check('hidden',options.hidden);
    }

    optionPanelEvent($el, options, componentFactory) {
        const {groupEl, labelEl} = this.getElement($el);

        // 항목 구성에 영향을 주는 옵션은 그룹 전체를 다시 그린다
        // (id/name/선택상태가 한꺼번에 바뀌어서 부분 수정이 더 번거롭다).
        const redrawItems = () => {
            groupEl.attr('id', options.id);
            this.renderItems(groupEl, options);
        };

        this.optionPanel.inputEvent('id',(e) => {
            super.changeOptionValue($el, options, 'id', $(e.target).val());
            labelEl.attr('for', options.id);
            redrawItems();
        });

        this.optionPanel.changeEvent('size',(e) => {
            super.changeOptionValue($el, options, 'size', $(e.target).val());
            super.changeSize($el, options.size);
        });

        this.optionPanel.inputEvent('width',(e) => {
            super.changeOptionValue($el, options, 'width', $(e.target).val());
            $el.css('width',options.width);
        });

        this.optionPanel.inputEvent('label',(e) => {
            super.changeOptionValue($el, options, 'label', $(e.target).val());
            labelEl.text(options.label);
            labelEl.prop('hidden', (options.label) ? false : true);
        });

        this.optionPanel.inputEvent('labelWidth',(e) => {
            super.changeOptionValue($el, options, 'labelWidth', $(e.target).val());
            labelEl.css('width',options.labelWidth);
        });

        this.optionPanel.changeEvent('horizontal',(e) => {
            $el.removeClass('d-flex');
            let value = $(e.target).is(':checked');

            super.changeOptionValue($el, options, 'horizontal', value);
            if(value) {
                $el.addClass('d-flex');
            }
        });

        this.optionPanel.itemListEvent('items',this.itemColumns(),(items) => {
            super.changeOptionValue($el, options, 'items', items);
            redrawItems();
        });

        this.optionPanel.inputEvent('defaultValue',(e) => {
            super.changeOptionValue($el, options, 'defaultValue', $(e.target).val());
            redrawItems();
        });

        this.optionPanel.changeEvent('inline',(e) => {
            super.changeOptionValue($el, options, 'inline', $(e.target).is(':checked'));
            redrawItems();
        });

        this.optionPanel.changeEvent('hidden',(e) => {
            let value = $(e.target).is(':checked');

            super.changeOptionValue($el, options, 'hidden', value);
            $el.prop('hidden', value);
        });

        this.optionPanel.changeEvent('editable',(e) => {
            super.changeOptionValue($el, options, 'editable', $(e.target).is(':checked'));
            redrawItems();
        });
    }
}
