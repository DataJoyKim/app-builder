class Select extends ViewObject {
    constructor(optionPanel) {
        super(optionPanel);
        this.optionPanel = optionPanel;
    }

    componentId() {
        return 'select';
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
            codeName:'',
            useFirstItem: true,
            firstItemLabel: '',
            firstItemValue:''
       };
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

        let $inputEl = $(`<select class="form-control form-control-sm rounded-1 form-select" id="${options.id}" >`);

        if(!options.editable) {
            $inputEl.prop('readOnly', true);
        }

        $el.append($inputEl);

        return $el;
    }

/* =======================================
 * Runtime Component Setting
 * ======================================= */
    renderRuntime(options, children) {
        return this.element(options, false);
    }

    scriptRuntime(el, options) {
        if(options.codeName) {
            let optionHtml = '';
            if(options.useFirstItem) {
                optionHtml += `<option value="${options.firstItemValue}">${options.firstItemLabel}</option>`;
            }

            const codeData = VB.globalVariable.getCode()[options.codeName];
            if(codeData) {
                for(const code of codeData) {
                    optionHtml += `<option value="${code.code}">${code.name}</option>`;
                }
            }

            $("#"+options.id).append(optionHtml);
        }
    }

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
                background-color: #fafafa;
            }
        `;
    }

    getElement($el) {
        return {
            inputEl:$el.children("select"),
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

        $panel.append(this.optionPanel.sectionTitle('동작'));
        $panel.append(this.optionPanel.toggle('editable',{label:'editable', size:'col-12'}));
        $panel.append(this.optionPanel.toggle('hidden',{label:'hidden', size:'col-12'}));

        $panel.append(this.optionPanel.sectionTitle('코드 연동'));
        $panel.append(this.optionPanel.input('codeName',{label:'코드명', size:'col-12'}));
        $panel.append(this.optionPanel.toggle('useFirstItem',{label:'첫번째항목 사용', size:'col-12'}));
        $panel.append(this.optionPanel.input('firstItemLabel',{label:'첫번째항목 라벨', size:'col-6'}));
        $panel.append(this.optionPanel.input('firstItemValue',{label:'첫번째항목 값', size:'col-6'}));
    }

    optionPanelScript($el, options) {
        this.optionPanel.setValue('component-id',this.componentId());
        this.optionPanel.setValue('id',options.id);
        this.optionPanel.setValue('size',options.size);
        this.optionPanel.setValue('width',options.width);
        this.optionPanel.setValue('labelWidth',options.labelWidth);
        this.optionPanel.setValue('label',options.label);
        this.optionPanel.setValue('codeName',options.codeName);
        this.optionPanel.setValue('firstItemLabel',options.firstItemLabel);
        this.optionPanel.setValue('firstItemValue',options.firstItemValue);
        this.optionPanel.check('horizontal',options.horizontal);
        this.optionPanel.check('editable',options.editable);
        this.optionPanel.check('hidden',options.hidden);
        this.optionPanel.check('useFirstItem',options.useFirstItem);
    }

    optionPanelEvent($el, options, componentFactory) {
        const {inputEl, labelEl} = this.getElement($el);

        this.optionPanel.inputEvent('id',(e) => {
            super.changeOptionValue($el, options, 'id', $(e.target).val());
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

        this.optionPanel.changeEvent('useFirstItem',(e) => {
            super.changeOptionValue($el, options, 'useFirstItem', $(e.target).is(':checked'));
        });

        this.optionPanel.changeEvent('codeName',(e) => {
            super.changeOptionValue($el, options, 'codeName', $(e.target).val());
        });

        this.optionPanel.changeEvent('firstItemLabel',(e) => {
            super.changeOptionValue($el, options, 'firstItemLabel', $(e.target).val());
        });

        this.optionPanel.changeEvent('firstItemValue',(e) => {
            super.changeOptionValue($el, options, 'firstItemValue', $(e.target).val());
        });
    }
}