class VbSheet extends ViewObject {
    constructor(optionPanel) {
        super(optionPanel);
        this.optionPanel = optionPanel;
    }

    componentId() {
        return 'sheet';
    }

    componentOptions() {
        return {
           id:'mySheet' + super.getComponentIdNumber(),
           dataProvider:'',
           width: "100%",
           height: "400px",
           useSeq: true,
           useStatus: true,
           useDelete: true,
           useDnd:false,
           useExpendLastColumn:true,
           offCellFocus:false,
           offDisableColumnColor:false,
           columns:[]
       };
    }

/* =======================================
 * Runtime Component Setting
 * ======================================= */
    renderRuntime(options, children) {
        return $(`
            <div id="${options.id}" dataProvider="${options.dataProvider}">
            </div>
        `);
    }

    scriptRuntime(el, options) {

        const columns = options.columns;
        for(const col of columns) {
            if(col.type === 'combo') {
                if(col.codeName) {
                    let codes = [];
                    if(col.useFirstItem) {
                        codes.push({code: col.firstItemValue, name: col.firstItemLabel});
                    }

                    const comboCodeData = VB.globalVariable.getCode()[col.codeName];
                    if(comboCodeData) {
                        codes = codes.concat(comboCodeData);
                    }

                    col.comboCodes = codes;
                }
            }
        }

        // 시트 생성
        Sheet.initSheet(
            options.id,
            options.width,
            options.height,
            {
                useSeq:options.useSeq,
                useStatus:options.useStatus,
                useDelete:options.useDelete,
                useDnd:options.useDnd,
                useExpendLastColumn:options.useExpendLastColumn,
                offCellFocus:options.offCellFocus,
                offDisableColumnColor:options.offDisableColumnColor
            },
            columns
            );

        const sheet = window[options.id];

        // 시트 이벤트 생성
        $("#"+options.id).attr('dataProvider',options.dataProvider)
            .on('clearData', function(){

            })
            .on('getData', function(){
                return sheet.getSaveData();
            })
            .on('setData', function(e, data){
                VB.globalVariable.setMessage(options.dataProvider, data);
            })
            .on('newData', function(e, data){
                sheet.addRowData(data);
            })
            .on('bindData', function(e, data){
                VB.globalVariable.setMessage(options.dataProvider, data);
                sheet.setSheetData(data);
            })
            .on('selectedRowData', function(e, data){
                return sheet.getSelectedRowData();
            });
    }

/* =======================================
 * Builder Component Setting
 * ======================================= */
    renderBuilder(options) {
        let el = `
            <div id="${options.id}" class="component vb-item" data-type="${this.componentId()}">
                ${super.componentDeleteBtn()}
                <div class="vb-builder-placeholder">
                    <i class="fas fa-table"></i>
                    <span>Sheet</span>
                    <small>${options.id}</small>
                </div>
            </div>
        `;

        return $(el);
    }

    styleBuilder() {
        return `
            .vb-item[data-type="${this.componentId()}"] {
                background-color: #fafbfc;
                height: 400px;
                width: 100%;
                display: flex;
                justify-content: center;
                align-items: center;
                border-radius: 10px;
                border: 2px dashed #c7cbd1;
                transition: border-color .12s ease, background-color .12s ease;
            }
            .vb-item[data-type="${this.componentId()}"]:hover {
                border-color: #4A90E2;
                background-color: #eef5ff;
            }
            .vb-item[data-type="${this.componentId()}"] .vb-builder-placeholder {
                display: flex;
                flex-direction: column;
                align-items: center;
                gap: 6px;
                color: #9199a3;
                pointer-events: none;
            }
            .vb-item[data-type="${this.componentId()}"] .vb-builder-placeholder i {
                font-size: 28px;
                color: #4A90E2;
            }
            .vb-item[data-type="${this.componentId()}"] .vb-builder-placeholder span {
                font-size: 14px;
                font-weight: 600;
                color: #374151;
            }
            .vb-item[data-type="${this.componentId()}"] .vb-builder-placeholder small {
                font-size: 11px;
                color: #9199a3;
            }
        `;
    }

/* =======================================
 * Option Panel Setting
 * ======================================= */
    optionPanelView($panel, options) {
        $panel.append(this.optionPanel.sectionTitle('기본'));
        $panel.append(this.optionPanel.input('component-id',{label:'컴포넌트명', size:'col-6', enabled:false}));

        let $rowId = this.optionPanel.row();
        $rowId.append(this.optionPanel.input('id',{label:'ID', size:'col-6'}));
        $rowId.append(this.optionPanel.input('dataProvider',{label:'dataProvider', size:'col-6'}));
        $panel.append($rowId);

        $panel.append(this.optionPanel.sectionTitle('표시'));
        let $rowGrid = this.optionPanel.row();
        $rowGrid.append(this.optionPanel.input('width',{label:'넓이', size:'col-6'}));
        $rowGrid.append(this.optionPanel.input('height',{label:'높이', size:'col-6'}));
        $rowGrid.append(this.optionPanel.button('column-setting',{label:'컬럼 설정', btnLabel:'설정',size:'col-12', icon:'fas fa-cog'}));
        $panel.append($rowGrid);

        $panel.append(this.optionPanel.sectionTitle('옵션'));
        let $rowOption = this.optionPanel.row();
        $rowOption.append(this.optionPanel.toggle('useSeq',{label:'Seq컬럼', size:'col-12'}));
        $rowOption.append(this.optionPanel.toggle('useStatus',{label:'상태컬럼', size:'col-12'}));
        $rowOption.append(this.optionPanel.toggle('useDelete',{label:'삭제컬럼', size:'col-12'}));
        $rowOption.append(this.optionPanel.toggle('useDnd',{label:'DnD컬럼', size:'col-12'}));
        $rowOption.append(this.optionPanel.toggle('useExpendLastColumn',{label:'마지막컬럼 자동확장', size:'col-12'}));
        $rowOption.append(this.optionPanel.toggle('offCellFocus',{label:'셀 포커싱 비활성화', size:'col-12'}));
        $rowOption.append(this.optionPanel.toggle('offDisableColumnColor',{label:'수정불가 컬럼 색상 비활성화', size:'col-12'}));
        $panel.append($rowOption);
    }

    optionPanelScript($el, options) {
        this.optionPanel.setValue('component-id',this.componentId());
        this.optionPanel.setValue('id',options.id);
        this.optionPanel.setValue('dataProvider',options.dataProvider);
        this.optionPanel.check('useSeq',options.useSeq);
        this.optionPanel.check('useStatus',options.useStatus);
        this.optionPanel.check('useDelete',options.useDelete);
        this.optionPanel.check('useDnd',options.useDnd);
        this.optionPanel.check('useExpendLastColumn',options.useExpendLastColumn);
        this.optionPanel.check('offCellFocus',options.offCellFocus);
        this.optionPanel.check('offDisableColumnColor',options.offDisableColumnColor);
        this.optionPanel.setValue('width',options.width);
        this.optionPanel.setValue('height',options.height);
        this.optionPanel.setValue('columns',options.columns);
    }

    optionPanelEvent($el, options, componentFactory) {
        this.optionPanel.inputEvent('id',(e) => {
            super.changeOptionValue($el, options, 'id', $(e.target).val());
        });

        this.optionPanel.inputEvent('dataProvider',(e) => {
            super.changeOptionValue($el, options, 'dataProvider', $(e.target).val());
        });

        this.optionPanel.inputEvent('width',(e) => {
            let value = $(e.target).val();
            super.changeOptionValue($el, options, 'width', value);
            $el.css('width', value);
        });

        this.optionPanel.inputEvent('height',(e) => {
            let value = $(e.target).val();
            super.changeOptionValue($el, options, 'height', value);
            $el.css('height', value);
        });

        this.optionPanel.clickEvent('useSeq',(e) => {
            super.changeOptionValue($el, options, 'useSeq', $(e.target).is(':checked'));
        });

        this.optionPanel.clickEvent('useStatus',(e) => {
            super.changeOptionValue($el, options, 'useStatus', $(e.target).is(':checked'));
        });

        this.optionPanel.clickEvent('useDelete',(e) => {
            super.changeOptionValue($el, options, 'useDelete', $(e.target).is(':checked'));
        });

        this.optionPanel.clickEvent('useDnd',(e) => {
            super.changeOptionValue($el, options, 'useDnd', $(e.target).is(':checked'));
        });

        this.optionPanel.clickEvent('useExpendLastColumn',(e) => {
            super.changeOptionValue($el, options, 'useExpendLastColumn', $(e.target).is(':checked'));
        });

        this.optionPanel.clickEvent('offCellFocus',(e) => {
            super.changeOptionValue($el, options, 'offCellFocus', $(e.target).is(':checked'));
        });

        this.optionPanel.clickEvent('offDisableColumnColor',(e) => {
            super.changeOptionValue($el, options, 'offDisableColumnColor', $(e.target).is(':checked'));
        });

        this.optionPanel.clickEvent('column-setting',(e) => {
            const changeOptionValue = super.changeOptionValue;
            App.modalPopup.open(`/${APPLICATION_ID}/console/view-sheet-column`,{title:'컬럼설정 팝업',size:"modal-xl",messageId:'SHEET_COLUMN_REQUEST'},{columns:options.columns});
            App.modalPopup.receiveParam('SHEET_COLUMN_RESULT',function(data){
                if(data.columns) {
                    changeOptionValue($el, options, 'columns', data.columns);
                }
            });
        });
    }
}