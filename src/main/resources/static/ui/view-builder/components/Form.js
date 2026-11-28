class Form extends ViewObject {
    constructor(optionPanel) {
        super(optionPanel);
        this.optionPanel = optionPanel;
    }

    componentId() {
        return 'form';
    }

    componentOptions() {
        return {
            id:this.componentId() + super.getComponentIdNumber(),
            dataProvider:''
       };
    }

/* =======================================
 * Runtime Component Setting
 * ======================================= */
    renderRuntime(options, children) {
        let el = $(`
            <form id="${options.id}" class="form" dataProvider="${options.dataProvider}">
            </form>
        `);

        if (children) {
            el.append(children);
        }

        return el;
    }

    scriptRuntime(el, options) {
        // 폼 이벤트 생성
        $("#"+options.id).attr('dataProvider',options.dataProvider)
            .on('clearData', function(){

            })
            .on('getData', function(){
                return null;
            })
            .on('setData', function(e, data){
                VB.globalVariable.setMessage(options.dataProvider, data);
            })
            .on('newData', function(e, data){
            })
            .on('bindData', function(e, data){
                VB.globalVariable.setMessage(options.dataProvider, data);
            });
    }

/* =======================================
 * Builder Component Setting
 * ======================================= */
    renderBuilder(options) {
        let el = `
            <form id="${options.id}" class="component form vb-item vb-container" data-type="${this.componentId()}">
                <span class="vb-builder-tag">Form</span>
                ${super.componentDeleteBtn()}
            </form>
        `;

        return $(el);
    }

    styleBuilder() {
        return `
            .vb-item[data-type="${this.componentId()}"] {
                padding: 14px;
                min-height: 80px;
                height: auto;
                border: 1px solid #e5e7eb;
                background-color: #ffffff;
                border-radius: 10px;
                transition: border-color .12s ease;
            }
            .vb-item[data-type="${this.componentId()}"]:hover {
                border-color: #9a9a9a;
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

    afterAddComponent(componentFactory, $el, $componentEl) {
        super.addComponentByType(componentFactory, 'row', $componentEl);
    }

/* =======================================
 * Option Panel Setting
 * ======================================= */
    optionPanelView($panel, options) {
        $panel.append(this.optionPanel.sectionTitle('기본'));
        $panel.append(this.optionPanel.input('component-id',{label:'컴포넌트명', size:'col-6', enabled:false}));
        $panel.append(this.optionPanel.input('id',{label:'ID', size:'col-6'}));
        $panel.append(this.optionPanel.input('dataProvider',{label:'dataProvider', size:'col-12'}));

        $panel.append(this.optionPanel.sectionTitle('내용'));
        $panel.append(this.optionPanel.button('row-add',{label:'Form 내용', size:'col-12', btnLabel:'행 추가',icon:'fas fa-plus'}));
    }

    optionPanelScript($el, options) {
        this.optionPanel.setValue('component-id',this.componentId());
        this.optionPanel.setValue('id',options.id);
        this.optionPanel.setValue('dataProvider',options.dataProvider);
    }

    optionPanelEvent($el, options, componentFactory) {
        this.optionPanel.inputEvent('id',(e) => {
            super.changeOptionValue($el, options, 'id', $(e.target).val());
        });

        this.optionPanel.inputEvent('dataProvider',(e) => {
            super.changeOptionValue($el, options, 'dataProvider', $(e.target).val());
        });

        this.optionPanel.clickEvent('row-add',(e) => {
            super.addComponentByType(componentFactory, 'row', $el);
        });
    }
}