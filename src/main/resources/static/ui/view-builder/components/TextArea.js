class TextArea extends ViewObject {
    constructor(optionPanel) {
        super(optionPanel);
        this.optionPanel = optionPanel;
    }

    componentId() {
        return 'textarea';
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
           rows:4,
           height:'',
           placeholder:'',
           maxLength:'',
           resize:'vertical'
       };
    }

    optionResize() {
        return `
            <option value="vertical">vertical</option>
            <option value="horizontal">horizontal</option>
            <option value="both">both</option>
            <option value="none">none</option>
        `;
    }

/* =======================================
 * Element Define
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

        let $inputEl = $(`<textarea class="form-control form-control-sm rounded-1" id="${options.id}" ></textarea>`);
        $inputEl.attr('placeholder'  , options.placeholder || '');
        $inputEl.attr('autocomplete' , 'off');
        $inputEl.attr('data-watch'   , 'true');
        $inputEl.prop('spellcheck'   , false);

        this.applyRows($inputEl, options);
        this.applyMaxLength($inputEl, options);
        this.applyResize($inputEl, options);

        if(!options.editable) {
            $inputEl.prop('readOnly', true);
        }

        $el.append($inputEl);

        return $el;
    }

    // height가 지정되면 그 값이 우선이고(rows는 브라우저 기본 높이 계산에만 쓰이므로
    // 명시적인 height가 있으면 의미가 없다), 없으면 rows로 줄 수를 지정한다.
    applyRows($inputEl, options) {
        if(options.rows) {
            $inputEl.attr('rows', options.rows);
        }

        $inputEl.css('height', options.height ? options.height : '');
    }

    applyMaxLength($inputEl, options) {
        if(options.maxLength) {
            $inputEl.attr('maxlength', options.maxLength);
        }
        else {
            $inputEl.removeAttr('maxlength');
        }
    }

    applyResize($inputEl, options) {
        $inputEl.css('resize', options.resize || 'vertical');
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
            inputEl:$el.children("textarea"),
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

        $panel.append(this.optionPanel.sectionTitle('입력영역'));
        $panel.append(this.optionPanel.input('rows',{label:'줄 수(rows)', size:'col-6'}));
        $panel.append(this.optionPanel.input('height',{label:'height', size:'col-6', placeholder:'미입력시 rows 적용'}));
        $panel.append(this.optionPanel.input('placeholder',{label:'placeholder', size:'col-6'}));
        $panel.append(this.optionPanel.input('maxLength',{label:'최대 글자수', size:'col-6'}));
        $panel.append(this.optionPanel.select('resize',{label:'크기조절', size:'col-6', options:this.optionResize()}));

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
        this.optionPanel.setValue('rows',options.rows);
        this.optionPanel.setValue('height',options.height);
        this.optionPanel.setValue('placeholder',options.placeholder);
        this.optionPanel.setValue('maxLength',options.maxLength);
        this.optionPanel.setValue('resize',options.resize);
        this.optionPanel.check('horizontal',options.horizontal);
        this.optionPanel.check('editable',options.editable);
        this.optionPanel.check('hidden',options.hidden);
    }

    optionPanelEvent($el, options, componentFactory) {
        const {inputEl, labelEl} = this.getElement($el);

        this.optionPanel.inputEvent('id',(e) => {
            super.changeOptionValue($el, options, 'id', $(e.target).val());
            inputEl.attr('id', options.id);
            labelEl.attr('for', options.id);
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

        this.optionPanel.inputEvent('rows',(e) => {
            super.changeOptionValue($el, options, 'rows', $(e.target).val());
            this.applyRows(inputEl, options);
        });

        this.optionPanel.inputEvent('height',(e) => {
            super.changeOptionValue($el, options, 'height', $(e.target).val());
            this.applyRows(inputEl, options);
        });

        this.optionPanel.inputEvent('placeholder',(e) => {
            super.changeOptionValue($el, options, 'placeholder', $(e.target).val());
            inputEl.attr('placeholder', options.placeholder);
        });

        this.optionPanel.inputEvent('maxLength',(e) => {
            super.changeOptionValue($el, options, 'maxLength', $(e.target).val());
            this.applyMaxLength(inputEl, options);
        });

        this.optionPanel.changeEvent('resize',(e) => {
            super.changeOptionValue($el, options, 'resize', $(e.target).val());
            this.applyResize(inputEl, options);
        });

        this.optionPanel.changeEvent('hidden',(e) => {
            let value = $(e.target).is(':checked');

            super.changeOptionValue($el, options, 'hidden', value);
            $el.prop('hidden', value);
        });

        this.optionPanel.changeEvent('editable',(e) => {
            let value = $(e.target).is(':checked');

            super.changeOptionValue($el, options, 'editable', value);
            inputEl.prop('readOnly', !value);
        });
    }
}
