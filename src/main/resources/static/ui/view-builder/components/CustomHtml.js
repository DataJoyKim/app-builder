class CustomHtml extends ViewObject {
    constructor(optionPanel) {
        super(optionPanel);
        this.optionPanel = optionPanel;
        this.contentEditor;
    }

    componentId() {
        return 'custom-html';
    }

    componentOptions() {
        return {
           id:this.componentId() + super.getComponentIdNumber(),
           className:'',
           htmlContent:'Html 내용 입력'
       };
    }

/* =======================================
 * Runtime Component Setting
 * ======================================= */
    renderRuntime(options, children) {
        let el =  $(`
            <div id="${options.id}" class="${options.className}">
                ${options.htmlContent}
            </div>
        `);

        return el;
    }

    scriptRuntime(el, options) {}

/* =======================================
 * Builder Component Setting
 * ======================================= */
    renderBuilder(options) {
        let el = `
            <div id="${options.id}" class="component vb-item ${options.className}" data-type="${this.componentId()}">
                ${super.componentDeleteBtn()}
                ${options.htmlContent}
            </div>
        `;

        return $(el);
    }

    styleBuilder() {
        return `
            .vb-item[data-type="${this.componentId()}"] {
                padding: 6px;
                min-height: 34px;
                height: auto;
                margin: 0 !important;
                border-radius: 6px;
                outline: 1px dashed transparent;
                transition: outline-color .12s ease, background-color .12s ease;
            }
            .vb-item[data-type="${this.componentId()}"]:hover {
                outline-color: #cacaca;
                background-color: #fafafa;
            }
        `;
    }

/* =======================================
 * Option Panel Setting
 * ======================================= */
    optionPanelView($panel, options) {
        $panel.append(this.optionPanel.sectionTitle('기본'));
        $panel.append(this.optionPanel.input('component-id',{label:'컴포넌트명', size:'col-6', enabled:false}));
        $panel.append(this.optionPanel.input('id',{label:'ID', size:'col-6'}));
        $panel.append(this.optionPanel.input('class',{label:'class 명', size:'col-12'}));

        $panel.append(this.optionPanel.sectionTitle('HTML 내용'));
        $panel.append($(`
            <div class="form-group col-12 vb-opt-block">
               <label for="custom-html-content">Html 내용</label>
                <textarea id="custom-html-content"></textarea>
            </div>
        `));
    }

    optionPanelScript($el, options) {
        this.optionPanel.setValue('component-id',this.componentId());
        this.optionPanel.setValue('id',options.id);
        this.optionPanel.setValue('class',options.className);

        let textarea = document.getElementById('custom-html-content');
        this.contentEditor = CodeMirror.fromTextArea(textarea, {
            lineNumbers: true,
            lineWrapping: true,
            theme: "darcula",
            mode: 'htmlmixed'
        });
        this.contentEditor.setSize("100%", "500px");
        this.contentEditor.setValue(options.htmlContent);
    }

    optionPanelEvent($el, options, componentFactory) {
        this.optionPanel.inputEvent('id',(e) => {
            super.changeOptionValue($el, options, 'id', $(e.target).val());
        });

        this.optionPanel.inputEvent('class',(e) => {
            super.changeOptionValue($el, options, 'className', $(e.target).val());
        });

        const changeOptionValue = super.changeOptionValue;
        const componentDeleteBtn = super.componentDeleteBtn();
        this.contentEditor.on("change", function(instance, changeObj) {
            const value = instance.getValue();
            changeOptionValue($el, options, 'htmlContent', value);
            $el.html(componentDeleteBtn + value);
        });
    }
}