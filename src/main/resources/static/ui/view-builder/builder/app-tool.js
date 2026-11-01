// 컴포넌트 랜더링
$(function () {
    $.getJSON('/ui/view-builder/config/config.json', function(configs) {
        console.log('configs', configs);
        window.VB = window.VB || {};
        window.VB.configs = configs;
        window.VB.version = configs.version;
        console.log(`version ${VB.version}`);

        $.when($.getScript(`${VB.configs.paths.module}/ModuleLoader.js`))
            .done(function() {
                window.VB.module = new ModuleLoader(VB.version, VB.configs, true);

                VB.module.load(function(){
                    ViewManager.init('layout');

                    window.VB.builder = new ViewBuilder('canvas', VB.actionsFactory, VB.componentFactory, VB.dropComponent, VB.viewDataLoader);
                });
            });
    });
});