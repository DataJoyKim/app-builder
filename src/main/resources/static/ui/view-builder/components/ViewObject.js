class ViewObject {
    constructor(optionPanel) {
        this.optionPanel = optionPanel;
        this._componentId = this.componentId();
    }

    componentId() {}

    componentOptions() {}

/* =======================================
 * Runtime Component Setting
 * ======================================= */
    render(initQueue, options, children) {
        let el = this.renderRuntime(options, children);

        const init = () => this.scriptRuntime(el, options);
        initQueue.push(init);

        return el;
    }

    renderRuntime(options, children) {}
    scriptRuntime(el, options) {}

/* =======================================
 * Builder Component Setting
 * ======================================= */
    component(options) {
        this.mountComponentStyle();

        let el = this.renderBuilder(options);

        return el;
    }

    renderBuilder(options) {}
    styleBuilder() {}

    mountComponentStyle() {
        if (!this.styleBuilder) return;

        const con = this.constructor;
        if (con.__componentStyleMounted) return; //중복방지

        const styleEl = document.createElement('style');
        styleEl.textContent = this.styleBuilder();

        document.head.appendChild(styleEl);

        con.__componentStyleMounted = true; //중복방지
    }

    componentDeleteBtn() {
        return `<button class="component-delete-btn">×</button>`;
    }

    addComponent($el, componentFactory) {
        this.plusComponentIdNumber();

        let options = this.componentOptions();

        let $componentEl = this.createComponent(options, componentFactory);
        $el.append($componentEl);

        this.afterAddComponent(componentFactory, $el, $componentEl);
    }

    afterAddComponent(componentFactory, $el, $componentEl) {}

    createComponent(options, componentFactory) {
        let $componentEl = this.component(options);
        this.setOptions($componentEl, options);

        const containerConfig = this.componentContainerConfig($componentEl);
        for(const config of containerConfig) {
            this.container(config.element, config.allowedComponentIds, componentFactory);
        }

        return $componentEl;
    }

    // 컴포넌트가 자식(레이아웃 내부 요소, 카드 헤더/바디 등)을 받을 수 있는 영역을
    // 하나 이상 반환한다. allowedComponentIds가 없으면 모든 타입을 허용한다.
    componentContainerConfig($componentEl) { return []; }

    addComponentByType(componentFactory, type, $el) {
        componentFactory[type].addComponent($el, componentFactory);
    }

    // 팔레트에서 새 컴포넌트를 끌어와 놓는 것과, 캔버스 안에서 기존 컴포넌트를
    // 재정렬/이동하는 것을 SortableJS의 group(pull/put) 메커니즘 하나로 통합 처리한다.
    // (예전에는 같은 엘리먼트에 jQuery UI droppable + sortable을 각각 따로 걸었는데,
    // 그 조합이 중첩된 컨테이너 구조에서 불안정하게 동작하는 원인이었다.)
    container($el, allowedComponentIds, componentFactory) {
        const COMPONENT_PANEL_ID_PREFIX = 'component-';
        const self = this;

        Sortable.create($el[0], {
            group: {
                name: 'vb-components',
                pull: true,
                put: function(to, from, dragEl) {
                    const type = dragEl.dataset.type;
                    if(!type) return false;

                    const componentType = type.startsWith(COMPONENT_PANEL_ID_PREFIX)
                        ? type.replace(COMPONENT_PANEL_ID_PREFIX, '')
                        : type;

                    if(allowedComponentIds && !allowedComponentIds.includes(componentType)) {
                        return false;
                    }

                    return true;
                }
            },
            filter: '.component-delete-btn',
            preventOnFilter: true,
            animation: 150,
            ghostClass: 'drop-hover',
            onAdd: function(evt) {
                const item = evt.item;
                const type = item.dataset.type || '';

                // 팔레트에서 그대로 복제되어 들어온 노드인지 확인(아직 실제 컴포넌트로
                // 생성되지 않은 상태) - 맞다면 복제 노드는 지우고 실제 컴포넌트를 새로 만든다.
                if(type.startsWith(COMPONENT_PANEL_ID_PREFIX)) {
                    const componentType = type.replace(COMPONENT_PANEL_ID_PREFIX, '');
                    item.remove();
                    self.addComponentByType(componentFactory, componentType, $el);
                }
                // 기존 캔버스 컴포넌트를 다른 컨테이너로 옮긴 경우엔 DOM 이동만 일어나고
                // .data('options')는 그대로 유지되므로 별도 처리가 필요 없다.
            }
        });
    }

    plusComponentIdNumber() {
        let numberId = window.componentIdMap[this._componentId];
        window.componentIdMap[this._componentId] = numberId+1;
    }

    getComponentIdNumber() {
        return window.componentIdMap[this._componentId];
    }

    createSortableIds(allowedComponentIds) {
        return allowedComponentIds
                .map(componentId => `.vb-item[data-type="${componentId}"]`)
                .join(",");
    }

    setOptions($target, options) {
        $target.data('options', options);
    }

    getOptions($target) {
        return $target.data('options');
    }

    changeOptionValue($component, options, key, value) {
        options[key] = value;
        this.setOptions($component, options);
    }

    changeSize($target, newSize) {
        $target.removeClass(function(i, cls) {
            return (cls.match(/col-\d+/g) || []).join(' ');
        });
        $target.addClass(newSize);
    }

/* =======================================
 * Option Panel Setting
 * ======================================= */
    initOptionPanel($el, id, componentFactory) {
        let options = this.getOptions($el);

        let $panel = $("#"+id);

        $panel.empty();

        this.optionPanel.init(this._componentId, []);

        this.optionPanelView($panel, options);
        this.childComponentAddView($el, $panel);
        this.optionPanel.groupSections($panel);

        this.optionPanelScript($el, options);

        this.optionPanelEvent($el, options, componentFactory);
        this.childComponentAddEvent($el, componentFactory);
    }

    /* ---------------------------------------
     * 하위 컴포넌트 추가 (드래그 앤 드롭 대신 버튼 + 선택 모달)
     * --------------------------------------- */

    // 이 컴포넌트 안에 넣을 수 있는 팔레트 컴포넌트와, 각각이 들어갈 영역.
    // 드롭 규칙(container()의 put)과 같은 기준으로 componentContainerConfig()의
    // allowedComponentIds를 따르고, 한 타입이 여러 영역에 들어갈 수 있으면 앞 영역이 우선이다.
    // 팔레트에 없는 내부용 컴포넌트(card-body 등)는 각 컴포넌트의 전용 버튼으로 추가하므로 뺀다.
    addableChildComponents($el) {
        const targets = {};

        // ComponentPanel은 빌더에서만 로드된다(런타임 화면에서는 옵션패널 자체가 없다).
        if (typeof ComponentPanel === 'undefined') {
            return targets;
        }

        const containerConfig = this.componentContainerConfig($el) || [];

        for (const category of ComponentPanel.catalog()) {
            for (const item of category.items) {
                const config = containerConfig.find(c =>
                    !c.allowedComponentIds || c.allowedComponentIds.includes(item.id));

                if (config) {
                    targets[item.id] = config.element;
                }
            }
        }

        return targets;
    }

    childComponentAddView($el, $panel) {
        if (Object.keys(this.addableChildComponents($el)).length === 0) {
            return;
        }

        // 다른 필드가 섹션 카드로 묶이는 컴포넌트면 같은 모양의 섹션으로 붙인다.
        if ($panel.children('.vb-opt-section-title').length > 0) {
            $panel.append(this.optionPanel.sectionTitle('하위 컴포넌트'));
        }

        $panel.append(this.optionPanel.button('child-component-add', {
            label: '하위 컴포넌트', size: 'col-12', btnLabel: '컴포넌트 추가', icon: 'fas fa-plus'
        }));
    }

    childComponentAddEvent($el, componentFactory) {
        this.optionPanel.clickEvent('child-component-add', (e) => {
            if (!window.VB || !VB.componentPicker) {
                return;
            }

            // 모달이 떠 있는 동안엔 다른 컴포넌트를 고를 수 없으니, 지금 시점의 영역을 그대로 쓴다.
            const targets = this.addableChildComponents($el);

            VB.componentPicker.open(Object.keys(targets), (type) => {
                const $target = targets[type];
                if (!$target || !$target.length || !componentFactory[type]) {
                    return;
                }

                this.addComponentByType(componentFactory, type, $target);

                // 추가된 컴포넌트가 화면 밖이면 보이는 곳까지 스크롤한다.
                const added = $target.children('.component').last()[0];
                if (added && added.scrollIntoView) {
                    added.scrollIntoView({block: 'nearest', behavior: 'smooth'});
                }
            });
        });
    }

    optionPanelView($panel, options) {}
    optionPanelScript($el, options) {}
    optionPanelEvent($el, options, componentFactory) {}
}