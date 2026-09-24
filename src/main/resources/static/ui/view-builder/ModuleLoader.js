class ModuleLoader {
    constructor(version, configs, edit) {
        this.version = version;
        this.configs = configs;

        this.commonModules = [
            `/ActionExecutor.js`,
            `/GlobalVariable.js`,
            `/ViewManager.js`,
            `/utils/ViewBuilderUtil.js`
        ];

        this.beforeModules = [
            `${configs.paths.actions}/Actions.js`,
            `${configs.paths.components}/ViewObject.js`,
            `${configs.paths.data}/ViewDataLoader.js`
        ];

        this.actionsModules = [
            `${configs.paths.actions}/ActionHandlerScriptEngine.js`,
            `${configs.paths.actions}/Script.js`,
            `${configs.paths.actions}/NewData.js`,
            `${configs.paths.actions}/Workflow.js`
        ];

        this.componentsModules = [
             `${configs.paths.components}/Button.js`,
             `${configs.paths.components}/Card.js`,
             `${configs.paths.components}/CardBody.js`,
             `${configs.paths.components}/CustomHtml.js`,
             `${configs.paths.components}/Form.js`,
             `${configs.paths.components}/VbGrid.js`,
             `${configs.paths.components}/Input.js`,
             `${configs.paths.components}/DateInput.js`,
             `${configs.paths.components}/TextArea.js`,
             `${configs.paths.components}/Checkbox.js`,
             `${configs.paths.components}/Radio.js`,
             `${configs.paths.components}/Toggle.js`,
             `${configs.paths.components}/Select.js`,
             `${configs.paths.components}/BarLineChart.js`,
             `${configs.paths.components}/PieChart.js`,
             `${configs.paths.components}/Layout.js`,
             `${configs.paths.components}/Row.js`,
             `${configs.paths.components}/Col.js`,
             `${configs.paths.components}/VbSheet.js`
        ];

        this.afterModules = [
            `${configs.paths.actions}/ActionsFactory.js`,
            `${configs.paths.components}/ComponentFactory.js`,
            `${configs.paths.runtime}/Render.js`,
            `${configs.paths.runtime}/View.js`,
            `${configs.paths.builder}/DropComponent.js`,
            `${configs.paths.builder}/OptionPanel.js`,
            `${configs.paths.builder}/ComponentPanel.js`
        ];

        const builderModule = [
            `${configs.paths.builder}/RenderBuilder.js`,
            `${configs.paths.builder}/ViewBuilder.js`
        ];

        this.afterModules.push(...builderModule);
    }

    load(_callback) {
        this.loadSequential(this.createModuleUrl(this.commonModules))
            .then(() => this.loadSequential(this.createModuleUrl(this.beforeModules)))
            .then(() => this.loadParallel(this.createModuleUrl(this.actionsModules)))
            .then(() => this.loadParallel(this.createModuleUrl(this.componentsModules)))
            .then(() => this.loadSequential(this.createModuleUrl(this.afterModules)))
            .done(_callback);
    }

    createModuleUrl(modulePath) {
        let modules = new Array();

        for(let module of modulePath) {
            modules.push(`${this.configs.paths.module}${module}?v=${this.version}`);
        }

        return modules;
    }

    loadSequential(urls) {
        let dfd = $.Deferred().resolve();

        urls.forEach(url => {
            dfd = dfd.then(() => $.getScript(url));
        });

        return dfd;
    }

    loadParallel(urls) {
        return $.when.apply($, urls.map(url => $.getScript(url)));
    }
}