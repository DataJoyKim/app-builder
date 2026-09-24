class ComponentFactory {
    constructor(optionPanel, utils) {
        this.optionPanel = optionPanel;
        this.utils = utils;
        this._instanceMap = this.instanceMap();
    }

    instanceMap() {
        return {
                "button": new Button(this.optionPanel, this.utils),
                "form": new Form(this.optionPanel),
                "grid": new VbGrid(this.optionPanel, this.utils.grid),
                "sheet": new VbSheet(this.optionPanel),
                "col": new Col(this.optionPanel),
                "row": new Row(this.optionPanel),
                "card": new Card(this.optionPanel),
                "card-body": new CardBody(this.optionPanel),
                "input": new Input(this.optionPanel),
                "date-input": new DateInput(this.optionPanel),
                "textarea": new TextArea(this.optionPanel),
                "checkbox": new Checkbox(this.optionPanel),
                "radio": new Radio(this.optionPanel),
                "toggle": new Toggle(this.optionPanel),
                "select": new Select(this.optionPanel),
                "layout": new Layout(this.optionPanel),
                'barline-chart':new BarLineChart(this.optionPanel),
                'pie-chart':new PieChart(this.optionPanel),
                "custom-html": new CustomHtml(this.optionPanel)
            };
    }

    instance(type) {
        return this._instanceMap[type];
    }

    getInstanceMap() {
        return this._instanceMap;
    }

    getComponentIdMap() {
        let componentIdMap = {};

        for(let key in this._instanceMap) {
            componentIdMap[key] = 0;
        }

        return componentIdMap;
    }
}