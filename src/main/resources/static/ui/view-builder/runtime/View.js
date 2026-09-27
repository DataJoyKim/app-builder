class View {
    constructor(
        canvasId,
        globalVariable,
        actionExecutor,
        actionsFactory,
        componentFactory,
        utils
        ) {
        this.canvasId = canvasId;
        this.globalVariable = globalVariable;
        this.actionExecutor = actionExecutor;
        this.actionsFactory = actionsFactory;
        this.componentFactory = componentFactory;
        this.utils = utils;
    }

    init(objectCode, viewData) {
        const self = this;

        this.utils.httpClient.get(`/${APPLICATION_ID}/pages/${objectCode}/definition`,{},
            function(response){
                let view;
                if(viewData) {
                    view = viewData;
                }
                else {
                    try {
                        view = JSON.parse(response.viewObjectContent.content);
                    }
                    catch(e) {
                        console.error("JSON 파싱 실패:", e.message)
                        alert("화면을 파싱하는데 실패하였습니다.");
                        return;
                    }
                }

                self.globalVariable.init();
                self.actionExecutor.init();

                self.globalVariable.setCode(response.codeMap);

                const render = new Render(self.actionsFactory, self.componentFactory);
                render.init(self.canvasId, response.viewObject, view, response.viewActions);
            },
            function(error){
                alert("화면을 불러오는데 실패하였습니다.");
            }
        );
    }
}