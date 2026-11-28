class Layout extends ViewObject {
    constructor(optionPanel) {
        super(optionPanel);
        this.optionPanel = optionPanel;
    }

    componentId() {
        return 'layout';
    }

    componentOptions() {
        return {
           id:this.componentId() + super.getComponentIdNumber()
       };
    }

/* =======================================
 * Runtime Component Setting
 * ======================================= */
    renderRuntime(options, children) {
        let el = $(`
           <div class="layout" id="layout" style="min-height: 993px;" >
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
           <div class="component layout wrapper vb-item vb-container" id="${options.id}" style="min-height: 993px;" data-type="${this.componentId()}">
           </div>
        `;

        return $(el);
    }

    styleBuilder() {
        return `
            .vb-item[data-type="${this.componentId()}"] {
                background-color: #ffffff;
                cursor: pointer;
                width: 100%;
                min-height: 90%;
                border: 1px dashed #d9d9d9;
                border-radius: 10px;
                padding-bottom: 50px;
            }
        `;
    }

    componentContainerConfig($componentEl) {
        return [{
            element: $componentEl
        }]
    }

    afterAddComponent(componentFactory, $el, $componentEl) {
        // 초기 로드 시 Row 컴포넌트 셋팅
        super.addComponentByType(componentFactory, 'row', $componentEl);
    }

/* =======================================
 * Option Panel Setting
 * ======================================= */
    optionPanelView($panel, options) {
        $panel.append(this.optionPanel.input('component-id',{label:'컴포넌트명', size:'col-6', enabled:false}));
        $panel.append(this.optionPanel.button('row-add',{label:'Layout 내용', size:'col-12', btnLabel:'행 추가',icon:'fas fa-plus'}));
    }

    optionPanelScript($el, options) {
        this.optionPanel.setValue('component-id',this.componentId());
    }

    optionPanelEvent($el, options, componentFactory) {
        this.optionPanel.clickEvent('row-add',(e) => {
          super.addComponentByType(componentFactory, 'row', $el);
        });
    }
}