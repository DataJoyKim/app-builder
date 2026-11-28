class PieChart extends ViewObject {
    constructor(optionPanel) {
        super(optionPanel);
        this.optionPanel = optionPanel;
    }

    componentId() {
        return 'pie-chart';
    }

    componentOptions() {
        return {
           id:'pieChart' + super.getComponentIdNumber(),
           dataProvider:'',
           width: "100%",
           height: "400px",
            title: '',
            titleLeft: '',
            titleTop: '',
            titleTextAlign: '',
            titleFontSize: '',
            useLegend: true,
            legendLeft:'',
            legendTop:'',
            legendOrient:'',
            legendFontSize:'',
            seriesName: '',
            valueAlias: 'value',
            radiusInner: '0%',
            radiusOuter: '70%',
            roseType: '',
            centerX: '50%',
            centerY: '50%',
            useLabel: true,
            labelPosition: 'outside',
            labelFormatter: '',
            colorPalette: ''
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
        const CATEGORY_ALIAS = 'ch_category';

        const chart = new Chart(options.id);
        const chartOp = {};

        // 슬라이스 색상 팔레트
        if(options.colorPalette) {
            chartOp.color = options.colorPalette.split(',').map(v => v.trim()).filter(v => v);
        }

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

        // 차트 Tooltip (파이는 항목단위로 보여주는게 자연스러움)
        chartOp.tooltip = {trigger: 'item'};

        // 차트 범례
        if(options.useLegend) {
            chartOp.legend = {
                show: true,
                left: (options.legendLeft) ? options.legendLeft : 'center', // 범례 위치
                top: (options.legendTop) ? options.legendTop : 'bottom', // 범례 위치
                orient: (options.legendOrient) ? options.legendOrient : 'horizontal', // 가로/세로
                itemGap: 20, // 범례 간격
                itemWidth: 25, // 아이콘 크기
                itemHeight: 14,
                textStyle: {fontSize: (options.legendFontSize) ? options.legendFontSize : 14}, // 글자 스타일
                data: []
            }
        }

        // 라벨(조각에 표시되는 이름/비율)
        const labelOption = (options.useLabel) ? {
            show: true,
            position: (options.labelPosition) ? options.labelPosition : 'outside',
            formatter: (options.labelFormatter) ? options.labelFormatter : undefined
        } : {
            show: false
        };

        // 파이 시리즈 설정
        const seriesObj = {
            name: (options.seriesName) ? options.seriesName : options.title,
            type: 'pie',
            radius: [(options.radiusInner) ? options.radiusInner : '0%', (options.radiusOuter) ? options.radiusOuter : '70%'],
            center: [(options.centerX) ? options.centerX : '50%', (options.centerY) ? options.centerY : '50%'],
            avoidLabelOverlap: true,
            label: labelOption,
            data: []
        };

        // 남딩게일(장미형) 차트 여부
        if(options.roseType) {
            seriesObj.roseType = options.roseType;
        }

        chartOp.series = [seriesObj];

        // 차트 옵션 셋팅
        chart.setOption(chartOp);

        // 시트 이벤트 생성
        $("#"+options.id).attr('dataProvider',options.dataProvider)
            .on('clearData', function(){
                chart.setData([Object.assign({}, seriesObj, {data: []})]);
            })
            .on('setData', function(e, data){
                VB.globalVariable.setMessage(options.dataProvider, data);
            })
            .on('bindData', function(e, data){
                VB.globalVariable.setMessage(options.dataProvider, data);

                const sliceData = [];
                const legendData = [];

                for(const row of data) {
                    const name = row[CATEGORY_ALIAS];
                    const value = row[options.valueAlias];

                    sliceData.push({
                        name: name,
                        value: (value == null || value === '') ? null : Number(value)
                    });
                    legendData.push(name);
                }

                if(options.useLegend) {
                    chartOp.legend.data = legendData;
                    chart.setOption(chartOp);
                }

                chart.setData([Object.assign({}, seriesObj, {data: sliceData})]);
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
                    <i class="fas fa-chart-pie"></i>
                    <span>Pie Chart</span>
                    <small>${options.id}</small>
                </div>
            </div>
        `;

        return $(el);
    }

    styleBuilder() {
        return `
            .vb-item[data-type="${this.componentId()}"] {
                background-color: #fafafa;
                display: flex;
                justify-content: center;
                align-items: center;
                border-radius: 10px;
                border: 2px dashed #cacaca;
                transition: border-color .12s ease, background-color .12s ease;
            }
            .vb-item[data-type="${this.componentId()}"]:hover {
                border-color: #9a9a9a;
                background-color: #f4f4f4;
            }
            .vb-item[data-type="${this.componentId()}"] .vb-builder-placeholder {
                display: flex;
                flex-direction: column;
                align-items: center;
                gap: 6px;
                color: #999999;
                pointer-events: none;
            }
            .vb-item[data-type="${this.componentId()}"] .vb-builder-placeholder i {
                font-size: 28px;
                color: #a0a0a0;
            }
            .vb-item[data-type="${this.componentId()}"] .vb-builder-placeholder span {
                font-size: 14px;
                font-weight: 600;
                color: #3d3d3d;
            }
            .vb-item[data-type="${this.componentId()}"] .vb-builder-placeholder small {
                font-size: 11px;
                color: #999999;
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
        $op1Grid.append(this.optionPanel.input('valueAlias',{label:'값 alias', size:'col-6', placeholder:'value'}));
        $op1Grid.append(this.optionPanel.input('seriesName',{label:'시리즈명', size:'col-6'}));
        $panel.append($op1Grid);

        $panel.append(this.optionPanel.sectionTitle('모양'));
        let $op2Grid = this.optionPanel.row();
        $op2Grid.append(this.optionPanel.input('radiusInner',{label:'안쪽 반지름', size:'col-6', placeholder:'0%(파이) / 40%(도넛)'}));
        $op2Grid.append(this.optionPanel.input('radiusOuter',{label:'바깥 반지름', size:'col-6', placeholder:'70%'}));
        $panel.append($op2Grid);

        let $op3Grid = this.optionPanel.row();
        $op3Grid.append(this.optionPanel.select('roseType',{label:'남딩게일(장미형)', size:'col-6',
            options:`
                <option value="">사용안함</option>
                <option value="radius">radius (반지름형)</option>
                <option value="area">area (면적형)</option>
            `}));
        $op3Grid.append(this.optionPanel.input('centerX',{label:'중심 X', size:'col-3', placeholder:'50%'}));
        $op3Grid.append(this.optionPanel.input('centerY',{label:'중심 Y', size:'col-3', placeholder:'50%'}));
        $panel.append($op3Grid);

        $panel.append(this.optionPanel.sectionTitle('라벨'));
        let $op4Grid = this.optionPanel.row();
        $op4Grid.append(this.optionPanel.toggle('useLabel',{label:'라벨 표시', size:'col-12'}));
        $op4Grid.append(this.optionPanel.select('labelPosition',{label:'라벨 위치', size:'col-6',
            options:`
                <option value="outside">바깥쪽</option>
                <option value="inside">안쪽</option>
                <option value="center">중앙</option>
            `}));
        $op4Grid.append(this.optionPanel.input('labelFormatter',{label:'라벨 포맷', size:'col-6', placeholder:'{b}: {d}%'}));
        $panel.append($op4Grid);

        $panel.append(this.optionPanel.sectionTitle('제목'));
        let $op5Grid = this.optionPanel.row();
        $op5Grid.append(this.optionPanel.input('title',{label:'제목', size:'col-12'}));
        $panel.append($op5Grid);

        let $op6Grid = this.optionPanel.row();
        $op6Grid.append(this.optionPanel.input('titleLeft',{label:'제목 Left', size:'col-6', placeholder:'left|center|right|10px|10%'}));
        $op6Grid.append(this.optionPanel.input('titleTop',{label:'제목 Top', size:'col-6',placeholder:'top|middle|bottom|10px|10%'}));
        $op6Grid.append(this.optionPanel.input('titleTextAlign',{label:'제목 TextAlign', size:'col-6',placeholder:'left|center|right'}));
        $op6Grid.append(this.optionPanel.input('titleFontSize',{label:'제목 FontSize', size:'col-6', placeholder:'15'}));
        $panel.append($op6Grid);

        $panel.append(this.optionPanel.sectionTitle('범례'));
        let $op7Grid = this.optionPanel.row();
        $op7Grid.append(this.optionPanel.toggle('useLegend',{label:'범례 사용', size:'col-12'}));
        $op7Grid.append(this.optionPanel.input('legendLeft',{label:'범례 Left', size:'col-6', placeholder:'left|center|right|10px|10%'}));
        $op7Grid.append(this.optionPanel.input('legendTop',{label:'범례 Top', size:'col-6',placeholder:'top|middle|bottom|10px|10%'}));
        $op7Grid.append(this.optionPanel.select('legendOrient',{label:'범례 배치', size:'col-6',
            options:`
                <option value="vertical">세로</option>
                <option value="horizontal">가로</option>
            `}));
        $op7Grid.append(this.optionPanel.input('legendFontSize',{label:'범례 FontSize', size:'col-6', placeholder:'15'}));
        $panel.append($op7Grid);

        $panel.append(this.optionPanel.sectionTitle('색상'));
        let $op8Grid = this.optionPanel.row();
        $op8Grid.append(this.optionPanel.input('colorPalette',{label:'조각 색상 팔레트', size:'col-12', placeholder:'#4A90E2, #F5A623, #7ED321 (콤마로 구분)'}));
        $panel.append($op8Grid);
    }

    optionPanelScript($el, options) {
        this.optionPanel.setValue('component-id',this.componentId());
        this.optionPanel.setValue('id',options.id);
        this.optionPanel.setValue('dataProvider',options.dataProvider);
        this.optionPanel.setValue('width',options.width);
        this.optionPanel.setValue('height',options.height);
        this.optionPanel.setValue('valueAlias',options.valueAlias);
        this.optionPanel.setValue('seriesName',options.seriesName);
        this.optionPanel.setValue('radiusInner',options.radiusInner);
        this.optionPanel.setValue('radiusOuter',options.radiusOuter);
        this.optionPanel.setValue('roseType',options.roseType);
        this.optionPanel.setValue('centerX',options.centerX);
        this.optionPanel.setValue('centerY',options.centerY);
        this.optionPanel.check('useLabel',options.useLabel);
        this.optionPanel.setValue('labelPosition',options.labelPosition);
        this.optionPanel.setValue('labelFormatter',options.labelFormatter);
        this.optionPanel.setValue('title',options.title);
        this.optionPanel.setValue('titleLeft',options.titleLeft);
        this.optionPanel.setValue('titleTop',options.titleTop);
        this.optionPanel.setValue('titleTextAlign',options.titleTextAlign);
        this.optionPanel.setValue('titleFontSize',options.titleFontSize);
        this.optionPanel.check('useLegend',options.useLegend);
        this.optionPanel.setValue('legendLeft',options.legendLeft);
        this.optionPanel.setValue('legendTop',options.legendTop);
        this.optionPanel.setValue('legendOrient',options.legendOrient);
        this.optionPanel.setValue('legendFontSize',options.legendFontSize);
        this.optionPanel.setValue('colorPalette',options.colorPalette);
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

        this.optionPanel.inputEvent('valueAlias',(e) => {
            super.changeOptionValue($el, options, 'valueAlias', $(e.target).val());
        });
        this.optionPanel.inputEvent('seriesName',(e) => {
            super.changeOptionValue($el, options, 'seriesName', $(e.target).val());
        });

        this.optionPanel.inputEvent('radiusInner',(e) => {
            super.changeOptionValue($el, options, 'radiusInner', $(e.target).val());
        });
        this.optionPanel.inputEvent('radiusOuter',(e) => {
            super.changeOptionValue($el, options, 'radiusOuter', $(e.target).val());
        });
        this.optionPanel.inputEvent('roseType',(e) => {
            super.changeOptionValue($el, options, 'roseType', $(e.target).val());
        });
        this.optionPanel.inputEvent('centerX',(e) => {
            super.changeOptionValue($el, options, 'centerX', $(e.target).val());
        });
        this.optionPanel.inputEvent('centerY',(e) => {
            super.changeOptionValue($el, options, 'centerY', $(e.target).val());
        });

        this.optionPanel.changeEvent('useLabel',(e) => {
            super.changeOptionValue($el, options, 'useLabel', $(e.target).is(':checked'));
        });
        this.optionPanel.inputEvent('labelPosition',(e) => {
            super.changeOptionValue($el, options, 'labelPosition', $(e.target).val());
        });
        this.optionPanel.inputEvent('labelFormatter',(e) => {
            super.changeOptionValue($el, options, 'labelFormatter', $(e.target).val());
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

        this.optionPanel.inputEvent('colorPalette',(e) => {
            super.changeOptionValue($el, options, 'colorPalette', $(e.target).val());
        });
    }
}
