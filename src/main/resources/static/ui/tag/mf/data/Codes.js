import { AbstractData } from './AbstractData.js';

export class Codes extends AbstractData {
    render() {
        let self = this;

        let codes = new Array();
        for (const child of this.children) {
            let childTagName = child.tagName.toLowerCase();

            if(childTagName != super.getTagEl().code) {
                continue;
            }

            codes.push({
                name: child.getAttribute('name'),
                type: child.getAttribute('type')
            });
        }

        if(codes.length > 0) {
            App.httpClient.post(`/${APPLICATION_ID}/code`,{},codes,
                function(response){
                    if(response) {
                        let codeVariable = self.getCodeVariable();
                        if(codeVariable) {
                            for(let key in response) {
                                codeVariable[key] = response[key];
                            }

                            self.setCodeVariable(codeVariable);
                        }
                        else {
                            self.setCodeVariable(response);
                        }
                    }
                },
                function(error){
                    alert('코드를 불러오는데 실패하였습니다.');
                },
                {
                    async:false
                }
            );
        }


    }
}