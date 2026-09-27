class Button extends ViewObject {
    constructor(optionPanel, utils) {
        super(optionPanel);
        this.optionPanel = optionPanel;
        this.utils = utils;
    }

    componentId() {
        return 'button';
    }

    componentOptions() {
        return {
            id:this.componentId() + super.getComponentIdNumber(),
            label:'Button',
            icon:'fas fa-search',
            action:''
       };
    }

/* =======================================
 * Element Define
 * ======================================= */
    element(options, isBuilder) {
        let $el;
        if(isBuilder) {
            $el = $(`<div id="${options.id}" type="button" class="btn btn-default btn-sm" ></div>`);
            $el.addClass('component');
            $el.addClass('vb-item');
            $el.attr('data-type', this.componentId());
            $el.append(super.componentDeleteBtn());
        }
        else {
            $el = $(`<button id="${options.id}" type="button" class="btn btn-default btn-sm" onclick="VB.doAction('${options.action}')"></button>`);
        }

        $el.css('margin-left', '2px');

        let $iconEl = $(`<i class="btn-icon"></i>`);
        $iconEl.addClass(options.icon);
        $el.append($iconEl);

        let $labelEl = $(`<span class="btn-label">${options.label}</span>`);
        $el.append($labelEl);

        return $el;
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
            }
        `;
    }

    getElement($el) {
        return {
            iconEl:$el.children("i"),
            labelEl:$el.children(".btn-label")
        }
    }

/* =======================================
 * Option Panel Setting
 * ======================================= */
    optionPanelView($panel, options) {
        $panel.append(this.optionPanel.sectionTitle('기본'));
        $panel.append(this.optionPanel.input('component-id',{label:'컴포넌트명', size:'col-6', enabled:false}));
        $panel.append(this.optionPanel.input('id',{label:'ID', size:'col-6'}));

        $panel.append(this.optionPanel.sectionTitle('표시'));
        $panel.append(this.optionPanel.input('icon',{label:'아이콘', size:'col-12'}));
        $panel.append(this.optionPanel.input('text',{label:'라벨', size:'col-12'}));

        $panel.append(this.optionPanel.sectionTitle('동작'));
        $panel.append(this.optionPanel.input('action',{label:'Action', size:'col-8'}));
        $panel.append(this.optionPanel.button('action-popup',{size:'col-4', icon:'fas fa-search', btnLabel:'검색'}));
    }

    optionPanelScript($el, options) {
        this.optionPanel.setValue('component-id',this.componentId());
        this.optionPanel.setValue('id',options.id);
        this.optionPanel.setValue('icon',options.icon);
        this.optionPanel.setValue('text',options.label);
        this.optionPanel.setValue('action',options.action);
    }

    optionPanelEvent($el, options, componentFactory) {
        const {iconEl,labelEl} = this.getElement($el);

        this.optionPanel.inputEvent('id',(e) => {
            super.changeOptionValue($el, options, 'id', $(e.target).val());
        });

        this.optionPanel.inputEvent('text',(e) => {
            super.changeOptionValue($el, options, 'label', $(e.target).val());
            labelEl.text(options.label);
        });

        this.optionPanel.inputEvent('icon',(e) => {
            iconEl.removeClass(options.icon);

            super.changeOptionValue($el, options, 'icon', $(e.target).val());

            iconEl.addClass(options.icon);
        });

        this.optionPanel.inputEvent('action',(e) => {
            super.changeOptionValue($el, options, 'action', $(e.target).val());
        });

        const optionPanel = this.optionPanel;
        this.optionPanel.clickEvent('action-popup',(e) => {
            let objectCode = $("#objectCode").val();
            if(!objectCode) {
                alert('오브젝트를 입력해주세요');
                return;
            }

            this.utils.modalPopup.open(`/${APPLICATION_ID}/console/action`,{title:'Action 팝업',messageId:'ACTION_REQUEST'},{objectCode:objectCode});
            this.utils.modalPopup.receiveParam('ACTION_RESULT', function(data){
                if(data.actionName) {
                    optionPanel.setValue(data.actionName);
                }
            });
        });
    }
}