/**
 * 화면빌더 내에서 사용할수있는 utils 함수.
 */
class ViewBuilderUtil {
    constructor() {
        this.grid = App.grid;
        this.modalPopup = App.modalPopup;
        this.popup = App.popup;
        this.httpClient = App.httpClient;
        this.workflowClient = App.workflowClient;
        this.toolbar = {
            refresh: function () {
                window.location.reload();
            },
            share: function () {
                const url = location.origin + "/" + APPLICATION_ID + "/pages/" + App.objectCode;

                navigator.clipboard.writeText(url)
                    .then(() => {
                        alert("URL이 복사되었습니다.");
                    });
            }
        }
    }
}