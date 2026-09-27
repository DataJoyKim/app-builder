class BarLineChart extends ViewObject {
    constructor(optionPanel) {
        super(optionPanel);
        this.optionPanel = optionPanel;
    }

    componentId() {
        return 'barline-chart';
    }

    componentOptions() {
        return {
           id:'myChart' + super.getComponentIdNumber(),
           dataProvider:'',
           width: "100%",
           height: "400px",
            title: '',
            titleLeft: '',
            titleTop: '',
            titleTextAlign: '',
            titleFontSize: '',
            direction: 'VERTICAL',
            useLegend: true,
            legendLeft:'',
            legendTop:'',
            legendOrient:'',
            legendFontSize:'',
            staticCategory: '',
            seriesSetting: []
       };
    }

/* =======================================
 * Runtime Component Setting
 * ======================================= */
    renderRuntime(options, children) {
        return $(`
            <div id="${options.id}" dataProvider="${options.dataProvider}" style="width: ${options.width}; height: ${options.height};">
            </div>
        `);
    }

    scriptRuntime(el, options) {
        const chart = new Chart(options.id);
        const chartOp = {};

        // 차트 타이틀
        if(options.title) {
            chartOp.title = {};
            chartOp.title.text = options.title;

            if(options.titleLeft) {
                chartOp.title.left = options.titleLeft;
            }

            if(options.titleTop) {
                chartOp.title.top = options.titleTop;
            }

            if(options.titleTextAlign) {
                chartOp.title.textAlign = options.titleTextAlign;
            }

            if(options.titleFontSize) {
                chartOp.title.textStyle = {};
                chartOp.title.textStyle.fontSize = options.titleFontSize;
            }
        }

        // 차트 Tooltip
        chartOp.tooltip = {trigger: 'axis'};

        // 차트 범례
        if(options.useLegend) {
            const legendData = [];
            for(const setting of options.seriesSetting) {
                legendData.push(setting.name);
            }

            chartOp.legend = {
                show: true,
                left: (options.legendLeft) ? options.legendLeft : 'center', // 범례 위치
                top: (options.legendTop) ? options.legendTop : 'bottom', // 범례 위치
                orient: (options.legendOrient) ? options.legendOrient : 'horizontal', // 가로/세로
                itemGap: 20, // 범례 간격
                itemWidth: 25, // 아이콘 크기
                itemHeight: 14,
                textStyle: {fontSize: (options.legendFontSize) ? options.legendFontSize : 14}, // 글자 스타일
                data: legendData
            }
        }

        // 차트 카테고리
        let category;
        if(options.staticCategory) {
            category = options.staticCategory.split(',').map(v => v.trim());
        }

        chartOp.xAxis = {};
        chartOp.yAxis = {};

        if(options.direction === 'HORIZONTAL') { // 가로 bar
            chartOp.xAxis.type = 'value';
            chartOp.yAxis.type = 'category';
            if(category) {
                chartOp.yAxis.data = category;
            }
        }
        else { // 세로 bar
            chartOp.xAxis.type = 'category';
            if(category) {
                chartOp.xAxis.data = category;
            }
            chartOp.yAxis.type = 'value';
        }

        // 차트 항목 설정
        if(options.seriesSetting) {
            let series = [];
            for(const setting of options.seriesSetting) {
                let seriesObj = {};
                seriesObj.name = setting.name;
                seriesObj.type = setting.type;
                if(setting.color) {
                    seriesObj.itemStyle = {};
                    seriesObj.itemStyle.color = setting.color;
                }

                series.push(seriesObj);
            }

            chartOp.series = series;
        }

        // 차트 옵션 셋팅
        chart.setOption(chartOp);

        // 시트 이벤트 생성
        $("#"+options.id).attr('dataProvider',options.dataProvider)
            .on('clearData', function(){
                chart.setData([]);
            })
            .on('setData', function(e, data){
                VB.globalVariable.setMessage(options.dataProvider, data);
            })
            .on('bindData', function(e, data){
                VB.globalVariable.setMessage(options.dataProvider, data);

                const CATEGORY_ALIAS = 'ch_category';

                const dataGroup = data.reduce((acc, item) => {
                    acc[item[CATEGORY_ALIAS]] = item;
                    return acc;
                }, {});

                if(!category) {
                    category = [];
                    for(const cat in dataGroup) {
                        category.push(cat);
                    }

                    const axis = (options.direction === 'HORIZONTAL') ? 'yAxis' : 'xAxis';
                    chartOp[axis].data = category;
                    chart.setOption(chartOp);
                }

                const result = [];

                for (const setting of options.seriesSetting) {
                    const seriesData = [];
                    for (const cat of category) {
                        const dataObj = dataGroup[cat];

                        if (!dataObj) {
                            seriesData.push(null);
                            continue;
                        }

                        const value = dataObj[setting.alias];
                        seriesData.push(value == null || value === '' ? null : Number(value));
                    }

                    result.push({name: setting.name, data: seriesData});
                }

                chart.setData(result);
            });
    }

/* =======================================
 * Builder Component Setting
 * ======================================= */
    renderBuilder(options) {
        let el = `
            <div id="${options.id}" class="component vb-item" data-type="${this.componentId()}" style="width: ${options.width}; height: ${options.height};">
                ${super.componentDeleteBtn()}
                <div class="vb-builder-placeholder">
                    <i class="fas fa-chart-bar"></i>
                    <span>Bar/Line Chart</span>
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

        let $rowGrid = this.optionPanel.row();
        $rowGrid.append(this.optionPanel.input('width',{label:'넓이', size:'col-6'}));
        $rowGrid.append(this.optionPanel.input('height',{label:'높이', size:'col-6'}));
        $panel.append($rowGrid);

        $panel.append(this.optionPanel.sectionTitle('데이터'));
        let $op1Grid = this.optionPanel.row();
        $op1Grid.append(this.optionPanel.select('direction',{label:'방향', size:'col-6',
            options:`
                <option value="VERTICAL">세로</option>
                <option value="HORIZONTAL">가로</option>
            `}));
        $op1Grid.append(this.optionPanel.input('staticCategory',{label:'정적 카테고리', size:'col-6'}));
        $panel.append($op1Grid);

        let $op3Grid = this.optionPanel.row();
        $op3Grid.append(this.optionPanel.button('seriesSetting',{label:'차트항목 설정', btnLabel:'설정',size:'col-12', icon:'fas fa-cog'}));
        $panel.append($op3Grid);

        $panel.append(this.optionPanel.sectionTitle('제목'));
        let $op4Grid = this.optionPanel.row();
        $op4Grid.append(this.optionPanel.input('title',{label:'제목', size:'col-12'}));
        $panel.append($op4Grid);

        let $op5Grid = this.optionPanel.row();
        $op5Grid.append(this.optionPanel.input('titleLeft',{label:'제목 Left', size:'col-6', placeholder:'left|center|right|10px|10%'}));
        $op5Grid.append(this.optionPanel.input('titleTop',{label:'제목 Top', size:'col-6',placeholder:'top|middle|bottom|10px|10%'}));
        $op5Grid.append(this.optionPanel.input('titleTextAlign',{label:'제목 TextAlign', size:'col-6',placeholder:'left|center|right'}));
        $op5Grid.append(this.optionPanel.input('titleFontSize',{label:'제목 FontSize', size:'col-6', placeholder:'15'}));
        $panel.append($op5Grid);

        $panel.append(this.optionPanel.sectionTitle('범례'));
        let $op6Grid = this.optionPanel.row();
        $op6Grid.append(this.optionPanel.toggle('useLegend',{label:'범례 사용', size:'col-12'}));
        $op6Grid.append(this.optionPanel.input('legendLeft',{label:'범례 Left', size:'col-6', placeholder:'left|center|right|10px|10%'}));
        $op6Grid.append(this.optionPanel.input('legendTop',{label:'범례 Top', size:'col-6',placeholder:'top|middle|bottom|10px|10%'}));
        $op6Grid.append(this.optionPanel.select('legendOrient',{label:'범례 배치', size:'col-6',
            options:`
                <option value="vertical">세로</option>
                <option value="horizontal">가로</option>
            `}));
        $op6Grid.append(this.optionPanel.input('legendFontSize',{label:'범례 FontSize', size:'col-6', placeholder:'15'}));
        $panel.append($op6Grid);
    }

    optionPanelScript($el, options) {
        this.optionPanel.setValue('component-id',this.componentId());
        this.optionPanel.setValue('id',options.id);
        this.optionPanel.setValue('dataProvider',options.dataProvider);
        this.optionPanel.setValue('width',options.width);
        this.optionPanel.setValue('height',options.height);
        this.optionPanel.setValue('title',options.title);
        this.optionPanel.setValue('titleLeft',options.titleLeft);
        this.optionPanel.setValue('titleTop',options.titleTop);
        this.optionPanel.setValue('titleTextAlign',options.titleTextAlign);
        this.optionPanel.setValue('titleFontSize',options.titleFontSize);
        this.optionPanel.setValue('seriesSetting',options.seriesSetting);
        this.optionPanel.setValue('staticCategory',options.staticCategory);
        this.optionPanel.setValue('direction',options.direction);
        this.optionPanel.check('useLegend',options.useLegend);
        this.optionPanel.setValue('legendLeft',options.legendLeft);
        this.optionPanel.setValue('legendTop',options.legendTop);
        this.optionPanel.setValue('legendOrient',options.legendOrient);
        this.optionPanel.setValue('legendFontSize',options.legendFontSize);
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

        this.optionPanel.inputEvent('title',(e) => {
            super.changeOptionValue($el, options, 'title', $(e.target).val());
        });
        this.optionPanel.inputEvent('titleLeft',(e) => {
            super.changeOptionValue($el, options, 'titleLeft', $(e.target).val());
        });
        this.optionPanel.inputEvent('titleTop',(e) => {
            super.changeOptionValue($el, options, 'titleTop', $(e.target).val());
        });
        this.optionPanel.inputEvent('titleTextAlign',(e) => {
            super.changeOptionValue($el, options, 'titleTextAlign', $(e.target).val());
        });
        this.optionPanel.inputEvent('titleFontSize',(e) => {
            super.changeOptionValue($el, options, 'titleFontSize', $(e.target).val());
        });
        this.optionPanel.inputEvent('staticCategory',(e) => {
            super.changeOptionValue($el, options, 'staticCategory', $(e.target).val());
        });
        this.optionPanel.inputEvent('direction',(e) => {
            super.changeOptionValue($el, options, 'direction', $(e.target).val());
        });

        this.optionPanel.changeEvent('useLegend',(e) => {
            super.changeOptionValue($el, options, 'useLegend', $(e.target).is(':checked'));
        });
        this.optionPanel.inputEvent('legendLeft',(e) => {
            super.changeOptionValue($el, options, 'legendLeft', $(e.target).val());
        });
        this.optionPanel.inputEvent('legendTop',(e) => {
            super.changeOptionValue($el, options, 'legendTop', $(e.target).val());
        });
        this.optionPanel.inputEvent('legendOrient',(e) => {
            super.changeOptionValue($el, options, 'legendOrient', $(e.target).val());
        });
        this.optionPanel.inputEvent('legendFontSize',(e) => {
            super.changeOptionValue($el, options, 'legendFontSize', $(e.target).val());
        });

        this.optionPanel.clickEvent('seriesSetting',(e) => {
            const changeOptionValue = super.changeOptionValue;
            App.modalPopup.open(`/${APPLICATION_ID}/console/view-chart-item`,{title:'차트항목 설정 팝업',size:"modal-lg",messageId:'CHART_ITEM_REQUEST'},{seriesSetting:options.seriesSetting});
            App.modalPopup.receiveParam('CHART_ITEM_RESULT',function(data){
                if(data.seriesSetting) {
                    changeOptionValue($el, options, 'seriesSetting', data.seriesSetting);
                }
            });
        });
    }
}