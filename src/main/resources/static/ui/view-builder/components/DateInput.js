class DateInput extends ViewObject {
    constructor(optionPanel) {
        super(optionPanel);
        this.optionPanel = optionPanel;
    }

    componentId() {
        return 'date-input';
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
           format:'YYYY-MM-DD',
           placeholder:'',
           useToday:false,
           useIcon:true,
           directInput:false
       };
    }

/* =======================================
 * Format
 * 화면에 표시되는 문자열이자 달력에서 값을 가져올 때 쓰는 포맷(moment 토큰).
 * 시/분/초 토큰이 들어있으면 달력에 시간 선택 영역을 함께 띄운다.
 * ======================================= */
    static FORMATS = [
        'YYYY-MM-DD',
        'YYYY.MM.DD',
        'YYYY/MM/DD',
        'YYYYMMDD',
        'YYYY-MM',
        'YYYY.MM',
        'YYYY-MM-DD HH:mm',
        'YYYY.MM.DD HH:mm',
        'YYYY/MM/DD HH:mm',
        'YYYY-MM-DD HH:mm:ss',
        'YYYY.MM.DD HH:mm:ss',
        'YYYY/MM/DD HH:mm:ss'
    ];

    defaultFormat() {
        return 'YYYY-MM-DD';
    }

    getFormat(options) {
        return options.format || this.defaultFormat();
    }

    useTimePicker(format) {
        return /[Hhms]/.test(format);
    }

    getPlaceholder(options) {
        return (options.placeholder) ? options.placeholder : this.getFormat(options);
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

        // 달력 아이콘을 붙이려면 input-group으로 감싸야 하는데, 이 래퍼가 끼면서
        // 수평배치(d-flex)일 때 폭이 무너지지 않도록 flex 값을 인라인으로 지정한다.
        // (runtime에서는 styleBuilder()가 주입되지 않으므로 CSS에 기대지 않는다.)
        let $groupEl = $(`<div class="input-group input-group-sm"></div>`);
        $groupEl.css({'flex':'1 1 auto', 'width':'auto'});

        let $inputEl = $(`<input type="text" class="form-control form-control-sm rounded-1" id="${options.id}" >`);
        $inputEl.attr('placeholder'  , this.getPlaceholder(options));
        $inputEl.attr('autocomplete' , 'off');
        $inputEl.attr('data-watch'   , 'true');
        $inputEl.prop('spellcheck'   , false);

        // editable=false는 값 자체를 못 바꾸는 상태(달력도 열리지 않음),
        // directInput=false는 키보드 입력만 막고 달력 선택은 허용하는 상태다.
        if(!options.editable || !options.directInput) {
            $inputEl.prop('readOnly', true);
        }

        $groupEl.append($inputEl);
        $groupEl.append(this.iconElement(options));

        $el.append($groupEl);

        return $el;
    }

    iconElement(options) {
        let $iconEl = $(`
            <div class="input-group-append">
                <span class="input-group-text"><i class="far fa-calendar-alt"></i></span>
            </div>
        `);

        $iconEl.css('cursor', options.editable ? 'pointer' : 'default');
        $iconEl.prop('hidden', !options.useIcon);

        return $iconEl;
    }

/* =======================================
 * Runtime Component Setting
 * ======================================= */
    renderRuntime(options, children) {
        return this.element(options, false);
    }

    scriptRuntime(el, options) {
        const {inputEl, iconEl} = this.getElement(el);

        if(!options.editable) {
            return;
        }

        this.createPicker(inputEl, options);

        // readOnly 상태에서는 input 클릭만으로 달력이 열리지 않는 브라우저가 있어
        // 아이콘을 눌러도 열리도록 연결해둔다.
        iconEl.on('click', () => inputEl.trigger('click'));
    }

    createPicker($inputEl, options) {
        if(!$.fn.daterangepicker) {
            console.error('daterangepicker 플러그인이 로드되지 않아 DateInput의 달력을 사용할 수 없습니다.');
            return;
        }

        const format = this.getFormat(options);
        const useTime = this.useTimePicker(format);

        $inputEl.daterangepicker({
            singleDatePicker : true,
            showDropdowns    : true,
            autoUpdateInput  : false, // 초기 렌더링 시 오늘 날짜가 멋대로 채워지는 것을 막는다.
            timePicker       : useTime,
            timePicker24Hour : /H/.test(format),
            timePickerSeconds: /s/.test(format),
            timePickerIncrement: 1,
            drops            : 'auto',
            locale           : this.locale(format)
        });

        $inputEl.on('apply.daterangepicker', function(e, picker) {
            $(this).val(picker.startDate.format(format)).trigger('change');
        });

        $inputEl.on('cancel.daterangepicker', function() {
            $(this).val('').trigger('change');
        });

        if(options.useToday) {
            $inputEl.val(moment().format(format)).trigger('change');
        }
    }

    locale(format) {
        return {
            format      : format,
            applyLabel  : '확인',
            cancelLabel : '취소',
            daysOfWeek  : ['일','월','화','수','목','금','토'],
            monthNames  : ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월'],
            firstDay    : 0
        };
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
                background-color: #fafbfc;
            }
        `;
    }

    getElement($el) {
        return {
            inputEl:$el.find("input"),
            iconEl:$el.find(".input-group-append"),
            labelEl:$el.children("label")
        }
    }

/* =======================================
 * Option Panel Setting
 * ======================================= */
    optionFormat() {
        return DateInput.FORMATS
                .map(format => `<option value="${format}">${format}</option>`)
                .join('');
    }

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

        $panel.append(this.optionPanel.sectionTitle('날짜'));
        $panel.append(this.optionPanel.select('format',{label:'포맷', size:'col-6', options:this.optionFormat()}));
        $panel.append(this.optionPanel.input('placeholder',{label:'placeholder', size:'col-6', placeholder:'미입력시 포맷 표시'}));
        $panel.append(this.optionPanel.toggle('useToday',{label:'오늘 날짜 기본값', size:'col-12'}));
        $panel.append(this.optionPanel.toggle('useIcon',{label:'달력 아이콘 표시', size:'col-12'}));

        $panel.append(this.optionPanel.sectionTitle('동작'));
        $panel.append(this.optionPanel.toggle('editable',{label:'editable', size:'col-12'}));
        $panel.append(this.optionPanel.toggle('directInput',{label:'직접입력 허용', size:'col-12'}));
        $panel.append(this.optionPanel.toggle('hidden',{label:'hidden', size:'col-12'}));
    }

    optionPanelScript($el, options) {
        this.optionPanel.setValue('component-id',this.componentId());
        this.optionPanel.setValue('id',options.id);
        this.optionPanel.setValue('size',options.size);
        this.optionPanel.setValue('width',options.width);
        this.optionPanel.setValue('labelWidth',options.labelWidth);
        this.optionPanel.setValue('label',options.label);
        this.optionPanel.setValue('format',this.getFormat(options));
        this.optionPanel.setValue('placeholder',options.placeholder);
        this.optionPanel.check('horizontal',options.horizontal);
        this.optionPanel.check('useToday',options.useToday);
        this.optionPanel.check('useIcon',options.useIcon);
        this.optionPanel.check('editable',options.editable);
        this.optionPanel.check('directInput',options.directInput);
        this.optionPanel.check('hidden',options.hidden);
    }

    optionPanelEvent($el, options, componentFactory) {
        const {inputEl, iconEl, labelEl} = this.getElement($el);

        const applyReadOnly = () => {
            inputEl.prop('readOnly', !options.editable || !options.directInput);
        };

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

        this.optionPanel.changeEvent('format',(e) => {
            super.changeOptionValue($el, options, 'format', $(e.target).val());
            inputEl.attr('placeholder', this.getPlaceholder(options));
        });

        this.optionPanel.inputEvent('placeholder',(e) => {
            super.changeOptionValue($el, options, 'placeholder', $(e.target).val());
            inputEl.attr('placeholder', this.getPlaceholder(options));
        });

        this.optionPanel.changeEvent('useToday',(e) => {
            super.changeOptionValue($el, options, 'useToday', $(e.target).is(':checked'));
        });

        this.optionPanel.changeEvent('useIcon',(e) => {
            let value = $(e.target).is(':checked');

            super.changeOptionValue($el, options, 'useIcon', value);
            iconEl.prop('hidden', !value);
        });

        this.optionPanel.changeEvent('hidden',(e) => {
            let value = $(e.target).is(':checked');

            super.changeOptionValue($el, options, 'hidden', value);
            $el.prop('hidden', value);
        });

        this.optionPanel.changeEvent('editable',(e) => {
            let value = $(e.target).is(':checked');

            super.changeOptionValue($el, options, 'editable', value);
            iconEl.css('cursor', value ? 'pointer' : 'default');
            applyReadOnly();
        });

        this.optionPanel.changeEvent('directInput',(e) => {
            super.changeOptionValue($el, options, 'directInput', $(e.target).is(':checked'));
            applyReadOnly();
        });
    }
}
