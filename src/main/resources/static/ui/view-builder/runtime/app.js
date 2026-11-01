$(function () {
    $.getJSON('/ui/view-builder/config/config.json', function(configs) {
        console.log('configs', configs);
        window.VB = window.VB || {};
        window.VB.configs = configs;
        window.VB.version = configs.version;
        console.log(`version ${VB.version}`);

        $.when($.getScript(`${VB.configs.paths.module}/ModuleLoader.js`))
            .done(function() {
                window.VB.module = new ModuleLoader(VB.version, VB.configs, false);

                VB.module.load(function(){
                    ViewManager.init('layout');

                    const view = new View('canvas', VB.globalVariable, VB.actionExecutor, VB.actionsFactory, VB.componentFactory, VB.utils);
                    view.init(VB.objectCode);
                });
            });
    });
});