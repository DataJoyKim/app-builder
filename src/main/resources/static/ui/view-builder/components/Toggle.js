class Toggle extends ViewObject {
    constructor(optionPanel) {
        super(optionPanel);
        this.optionPanel = optionPanel;
    }

    componentId() {
        return 'toggle';
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
           toggleLabel:'',
           checkedValue:'Y',
           defaultChecked:false
       };
    }

/* =======================================
 * Element Define
 *
 * 스위치 모양의 on/off 입력. 알맹이는 checkbox라서 값을 읽는 방법도 Checkbox와
 * 같고(checked 여부 + checkedValue), 보이는 모양만 custom-switch로 다르다.
 * 토글은 보통 왼쪽 라벨만으로 충분해서 스위치 옆 문구(toggleLabel)는 기본이 빈 값이다.
 * (문구가 없어도 스위치를 그리는 건 custom-control-label이므로 항상 붙여둔다.)
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

        $el.append(this.itemElement(options));

        return $el;
    }

    itemElement(options) {
        // runtime에는 styleBuilder()가 주입되지 않으므로 수평배치(d-flex)에서
        // 폭이 무너지지 않도록 flex 값은 인라인으로 준다.
        const $item = $(`<div class="custom-control custom-switch"></div>`);
        $item.css({'flex':'1 1 auto', 'min-width':'0'});

        const $inputEl = $(`<input type="checkbox" class="custom-control-input">`);
        $inputEl.attr('id'   , options.id);
        $inputEl.attr('name' , options.id);
        $inputEl.attr('value', options.checkedValue ?? '');
        $inputEl.attr('data-watch', 'true');
        $inputEl.prop('checked' , !!options.defaultChecked);
        $inputEl.prop('disabled', !options.editable);

        const $labelEl = $(`<label class="custom-control-label" for="${options.id}"></label>`);
        $labelEl.css('cursor', options.editable ? 'pointer' : 'default');
        $labelEl.text(options.toggleLabel ?? '');

        $item.append($inputEl);
        $item.append($labelEl);

        return $item;
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
            itemEl:$el.children(".custom-control"),
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

        $panel.append(this.optionPanel.sectionTitle('토글'));
        $panel.append(this.optionPanel.input('toggleLabel',{label:'토글 문구', size:'col-6', placeholder:'미입력시 스위치만 표시'}));
        $panel.append(this.optionPanel.input('checkedValue',{label:'ON 값', size:'col-6'}));
        $panel.append(this.optionPanel.toggle('defaultChecked',{label:'기본 ON', size:'col-12'}));

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
        this.optionPanel.setValue('toggleLabel',options.toggleLabel);
        this.optionPanel.setValue('checkedValue',options.checkedValue);
        this.optionPanel.check('horizontal',options.horizontal);
        this.optionPanel.check('defaultChecked',options.defaultChecked);
        this.optionPanel.check('editable',options.editable);
        this.optionPanel.check('hidden',options.hidden);
    }

    optionPanelEvent($el, options, componentFactory) {
        const {labelEl} = this.getElement($el);

        // id/체크상태/문구가 input과 label의 id·for로 서로 묶여 있어서
        // 부분 수정보다 스위치 한 덩어리를 다시 그리는 편이 단순하다.
        const redrawItem = () => {
            $el.children(".custom-control").replaceWith(this.itemElement(options));
        };

        this.optionPanel.inputEvent('id',(e) => {
            super.changeOptionValue($el, options, 'id', $(e.target).val());
            labelEl.attr('for', options.id);
            redrawItem();
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

        this.optionPanel.inputEvent('toggleLabel',(e) => {
            super.changeOptionValue($el, options, 'toggleLabel', $(e.target).val());
            redrawItem();
        });

        this.optionPanel.inputEvent('checkedValue',(e) => {
            super.changeOptionValue($el, options, 'checkedValue', $(e.target).val());
            redrawItem();
        });

        this.optionPanel.changeEvent('defaultChecked',(e) => {
            super.changeOptionValue($el, options, 'defaultChecked', $(e.target).is(':checked'));
            redrawItem();
        });

        this.optionPanel.changeEvent('hidden',(e) => {
            let value = $(e.target).is(':checked');

            super.changeOptionValue($el, options, 'hidden', value);
            $el.prop('hidden', value);
        });

        this.optionPanel.changeEvent('editable',(e) => {
            super.changeOptionValue($el, options, 'editable', $(e.target).is(':checked'));
            redrawItem();
        });
    }
}
