class Row extends ViewObject {
    constructor(optionPanel) {
        super(optionPanel);
        this.optionPanel = optionPanel;
    }

    componentId() {
        return 'row';
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
            <div id="${options.id}" class="row" >
            </div>
        `);

        if (children) {
            el.append(children);
        }

        return el;
    }

    scriptRuntime(el, options) {}

/* =======================================
 * Builder Component Setting
 * ======================================= */
    renderBuilder(options) {
        let el = `
            <div id="${options.id}" class="component row vb-item vb-container" data-type="${this.componentId()}">
                <span class="vb-builder-tag">Row</span>
                ${super.componentDeleteBtn()}
            </div>
        `;

        return $(el);
    }

    styleBuilder() {
        return `
            .vb-item[data-type="${this.componentId()}"] {
                background-color: #ffffff;
                padding: 8px;
                min-height: 54px;
                height: auto;
                margin: 0 !important;
                border: 1px dashed #cacaca;
                border-radius: 8px;
                transition: border-color .12s ease, background-color .12s ease;
            }
            .vb-item[data-type="${this.componentId()}"]:hover {
                border-color: #9a9a9a;
                background-color: #fafafa;
            }
            .vb-item[data-type="${this.componentId()}"] .vb-builder-tag {
                position: absolute;
                top: 4px;
                left: 4px;
                background: #6b6b6b;
                color: #fff;
                font-size: 9px;
                font-weight: 700;
                letter-spacing: .03em;
                padding: 1px 6px;
                border-radius: 4px;
                opacity: .5;
                transition: opacity .12s ease;
                pointer-events: none;
                z-index: 5;
            }
            .vb-item[data-type="${this.componentId()}"]:hover .vb-builder-tag,
            .vb-item[data-type="${this.componentId()}"].selected .vb-builder-tag {
                opacity: 1;
            }
        `;
    }

    componentContainerConfig($componentEl) {
        return [{
            element: $componentEl
        }]
    }

/* =======================================
 * Option Panel Setting
 * ======================================= */
    optionPanelView($panel, options) {
        $panel.append(this.optionPanel.input('component-id',{label:'컴포넌트명', size:'col-6', enabled:false}));
        $panel.append(this.optionPanel.button('col-add',{label:'내용', size:'col-12', btnLabel:'열 추가',icon:'fas fa-plus'}));
    }

    optionPanelScript($el, options) {
        this.optionPanel.setValue('component-id',this.componentId());
    }

    optionPanelEvent($el, options, componentFactory) {
        this.optionPanel.clickEvent('col-add',(e) => {
          super.addComponentByType(componentFactory, 'col', $el);
        });
    }
}