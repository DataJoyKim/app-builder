/**
 * Metric(지표 카드): 대시보드 KPI 하나를 숫자로 크게 보여준다.
 *
 * 데이터는 다른 차트와 같이 dataProvider(= 워크플로우 응답 메시지 ID)로 받는 행 배열이다.
 *  - 값: valueAlias 컬럼을 aggregate(마지막 행/첫 행/합계/평균/최소/최대/행 수)로 하나로 만든다.
 *  - 비교: compareMode 가 column 이면 compareAlias 컬럼을 같은 방식으로 모은 값, previous 면 바로 앞 행의 값과 비교해
 *          증감(▲/▼)을 퍼센트 또는 값 차이로 보여준다. invertTrend 면 감소를 좋은 쪽(초록)으로 본다.
 *  - 스파크라인: useSparkline 이면 행 순서대로 valueAlias 를 작은 선 그래프로 그린다 (x 축 라벨은 차트 공통의 ch_category).
 */
class Metric extends ViewObject {
    constructor(optionPanel) {
        super(optionPanel);
        this.optionPanel = optionPanel;
    }

    componentId() {
        return 'metric';
    }

    componentOptions() {
        return {
            id: 'metric' + super.getComponentIdNumber(),
            dataProvider: '',
            width: '100%',
            height: '',
            title: '지표',
            icon: 'fas fa-chart-line',
            accent: 'blue',
            subText: '',
            valueAlias: 'value',
            aggregate: 'last',
            decimals: '',
            useThousands: true,
            compact: false,
            prefix: '',
            suffix: '',
            compareMode: 'none',
            compareAlias: '',
            compareLabel: '',
            deltaType: 'percent',
            invertTrend: false,
            useSparkline: false
        };
    }

    // 강조색 (파스텔 배경 + 차분한 진한 색). 아이콘 배경과 스파크라인에 쓴다
    static accents() {
        return {
            blue:   {label: '블루',   bg: '#e8f0fa', fg: '#4f7cb3'},
            green:  {label: '그린',   bg: '#e6f4ec', fg: '#3f8a63'},
            purple: {label: '퍼플',   bg: '#eeeafa', fg: '#6a5bab'},
            orange: {label: '오렌지', bg: '#fdf0e2', fg: '#b86e1f'},
            pink:   {label: '핑크',   bg: '#fbe9ef', fg: '#b0507a'},
            teal:   {label: '틸',     bg: '#e3f3f3', fg: '#3a8585'},
            gray:   {label: '그레이', bg: '#efefef', fg: '#555555'}
        };
    }

    static accentOf(name) {
        const accents = Metric.accents();
        return accents[name] || accents.blue;
    }

    static escape(text) {
        return String(text == null ? '' : text)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    // 런타임과 빌더가 같이 쓰는 카드 스타일
    static css() {
        return `
            .vb-metric {
                position: relative;
                display: flex;
                flex-direction: column;
                gap: 6px;
                min-height: 116px;
                padding: 16px 18px;
                background: #fff;
                border: 1px solid #ebebeb;
                border-radius: 10px;
                box-shadow: 0 1px 2px rgba(0, 0, 0, .04);
                overflow: hidden;
                box-sizing: border-box;
            }
            .vb-metric-head {
                display: flex;
                align-items: center;
                justify-content: space-between;
                gap: 8px;
            }
            .vb-metric-title {
                font-size: 13px;
                font-weight: 600;
                color: #6f6f6f;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                min-width: 0;
            }
            .vb-metric-icon {
                flex: 0 0 auto;
                display: inline-flex;
                align-items: center;
                justify-content: center;
                width: 32px;
                height: 32px;
                border-radius: 8px;
                font-size: 14px;
            }
            .vb-metric-value {
                font-size: 28px;
                font-weight: 700;
                line-height: 1.15;
                color: #242323;
                letter-spacing: -.01em;
                font-variant-numeric: tabular-nums;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
            .vb-metric-value.is-empty {
                color: #c4c4c4;
            }
            .vb-metric-foot {
                display: flex;
                align-items: center;
                flex-wrap: wrap;
                gap: 6px;
                font-size: 12px;
                color: #8a8a8a;
                min-height: 18px;
            }
            .vb-metric-delta {
                display: inline-flex;
                align-items: center;
                gap: 3px;
                padding: 1px 7px;
                border-radius: 999px;
                font-weight: 600;
                font-variant-numeric: tabular-nums;
            }
            .vb-metric-delta:empty {
                display: none;
            }
            .vb-metric-delta.is-good {
                background: #e6f4ec;
                color: #3f8a63;
            }
            .vb-metric-delta.is-bad {
                background: #fbeaea;
                color: #b5524f;
            }
            .vb-metric-delta.is-flat {
                background: #f2f2f2;
                color: #6f6f6f;
            }
            .vb-metric-spark {
                height: 40px;
                margin: 2px -4px -4px;
            }
        `;
    }

    static mountRuntimeStyle() {
        if (Metric.__runtimeStyleMounted) return;

        const styleEl = document.createElement('style');
        styleEl.textContent = Metric.css();
        document.head.appendChild(styleEl);

        Metric.__runtimeStyleMounted = true;
    }

    // 카드 안쪽 (제목, 아이콘, 값, 비교, 스파크라인 자리)
    static cardInner(options, sample) {
        const accent = Metric.accentOf(options.accent);
        const esc = Metric.escape;
        const icon = options.icon
            ? `<span class="vb-metric-icon" style="background:${accent.bg};color:${accent.fg};"><i class="${esc(options.icon)}"></i></span>`
            : '';
        const sub = options.compareMode !== 'none' && options.compareLabel ? options.compareLabel : options.subText;

        return `
            <div class="vb-metric-head">
                <span class="vb-metric-title">${esc(options.title)}</span>
                ${icon}
            </div>
            <div class="vb-metric-value ${sample ? '' : 'is-empty'}">${sample ? esc(sample.value) : '-'}</div>
            <div class="vb-metric-foot">
                <span class="vb-metric-delta ${sample && sample.delta ? 'is-good' : ''}">${sample && sample.delta ? esc(sample.delta) : ''}</span>
                <span class="vb-metric-sub">${esc(sub)}</span>
            </div>
            ${options.useSparkline ? `<div class="vb-metric-spark" id="${esc(options.id)}_spark"></div>` : ''}
        `;
    }

/* =======================================
 * 값 계산 / 포맷
 * ======================================= */
    static toNumber(value) {
        if (value == null || value === '') return null;
        const n = Number(String(value).replace(/,/g, ''));
        return isNaN(n) ? null : n;
    }

    static aggregate(rows, alias, mode) {
        if (!alias || !rows || rows.length === 0) {
            return mode === 'count' ? (rows ? rows.length : 0) : null;
        }

        if (mode === 'count') return rows.length;
        if (mode === 'first') return Metric.toNumber(rows[0][alias]);
        if (mode === 'last' || !mode) return Metric.toNumber(rows[rows.length - 1][alias]);

        const values = rows.map(row => Metric.toNumber(row[alias])).filter(v => v !== null);
        if (values.length === 0) return null;

        switch (mode) {
            case 'sum': return values.reduce((a, b) => a + b, 0);
            case 'avg': return values.reduce((a, b) => a + b, 0) / values.length;
            case 'min': return Math.min(...values);
            case 'max': return Math.max(...values);
            default: return values[values.length - 1];
        }
    }

    static format(value, options) {
        if (value == null) return '-';

        const decimals = options.decimals === '' || options.decimals == null ? null : Math.max(0, Math.min(10, parseInt(options.decimals, 10) || 0));
        let text;

        if (options.compact) {
            text = new Intl.NumberFormat('ko-KR', {
                notation: 'compact',
                maximumFractionDigits: decimals == null ? 1 : decimals
            }).format(value);
        }
        else {
            const fmt = {useGrouping: options.useThousands !== false};
            if (decimals != null) {
                fmt.minimumFractionDigits = decimals;
                fmt.maximumFractionDigits = decimals;
            }
            else {
                fmt.maximumFractionDigits = 2;
            }
            text = new Intl.NumberFormat('ko-KR', fmt).format(value);
        }

        return (options.prefix || '') + text + (options.suffix || '');
    }

    // 비교 대상 값. 없으면 null
    static compareValue(rows, options) {
        if (!rows || rows.length === 0) return null;

        if (options.compareMode === 'column') {
            return Metric.aggregate(rows, options.compareAlias, options.aggregate);
        }
        if (options.compareMode === 'previous') {
            if (rows.length < 2) return null;
            // 값은 마지막 행 기준으로 보고, 그 앞 행과 비교한다
            return Metric.toNumber(rows[rows.length - 2][options.valueAlias]);
        }
        return null;
    }

    static delta(current, previous, options) {
        if (current == null || previous == null) return null;

        const diff = current - previous;
        const direction = diff > 0 ? 1 : (diff < 0 ? -1 : 0);
        let text;

        if (options.deltaType === 'value') {
            text = Metric.format(Math.abs(diff), options);
        }
        else {
            if (previous === 0) {
                text = diff === 0 ? '0%' : '-';
            }
            else {
                const pct = Math.abs(diff / Math.abs(previous) * 100);
                text = new Intl.NumberFormat('ko-KR', {maximumFractionDigits: 1}).format(pct) + '%';
            }
        }

        const arrow = direction > 0 ? '▲' : (direction < 0 ? '▼' : '−');
        const good = options.invertTrend ? direction < 0 : direction > 0;

        return {
            text: arrow + ' ' + text,
            css: direction === 0 ? 'is-flat' : (good ? 'is-good' : 'is-bad')
        };
    }

/* =======================================
 * Runtime Component Setting
 * ======================================= */
    renderRuntime(options, children) {
        Metric.mountRuntimeStyle();

        const height = options.height ? `height: ${options.height};` : '';
        return $(`
            <div id="${Metric.escape(options.id)}" dataProvider="${Metric.escape(options.dataProvider)}" class="vb-metric" style="width: ${options.width}; ${height}">
                ${Metric.cardInner(options, null)}
            </div>
        `);
    }

    scriptRuntime(el, options) {
        const $el = $("#" + options.id);
        const accent = Metric.accentOf(options.accent);
        let spark = null;

        if (options.useSparkline && typeof Chart !== 'undefined' && document.getElementById(options.id + '_spark')) {
            spark = new Chart(options.id + '_spark');
            spark.setOption({
                grid: {left: 4, right: 4, top: 4, bottom: 4},
                tooltip: {trigger: 'axis', confine: true},
                xAxis: {type: 'category', show: false, boundaryGap: false, data: []},
                yAxis: {type: 'value', show: false, scale: true},
                series: [{type: 'line', data: []}]
            });
            $(window).on('resize.' + options.id, () => spark.resize());
        }

        const renderSpark = (rows) => {
            if (!spark) return;
            spark.setOption({
                xAxis: {data: rows.map((row, i) => row['ch_category'] != null ? row['ch_category'] : String(i + 1))},
                series: [{
                    type: 'line',
                    name: options.title,
                    data: rows.map(row => Metric.toNumber(row[options.valueAlias])),
                    smooth: true,
                    showSymbol: false,
                    lineStyle: {width: 2, color: accent.fg},
                    itemStyle: {color: accent.fg},
                    areaStyle: {color: accent.bg, opacity: .8}
                }]
            });
        };

        const show = (rows) => {
            rows = Array.isArray(rows) ? rows : (rows ? [rows] : []);

            // 비교가 "앞 행"이면 값도 마지막 행 기준이어야 앞뒤가 맞는다
            const aggregateMode = options.compareMode === 'previous' ? 'last' : options.aggregate;
            const value = Metric.aggregate(rows, options.valueAlias, aggregateMode);

            $el.find('.vb-metric-value')
                .text(Metric.format(value, options))
                .toggleClass('is-empty', value == null);

            const delta = Metric.delta(value, Metric.compareValue(rows, options), options);
            $el.find('.vb-metric-delta')
                .removeClass('is-good is-bad is-flat')
                .addClass(delta ? delta.css : '')
                .text(delta ? delta.text : '');

            renderSpark(rows);
        };

        $el.attr('dataProvider', options.dataProvider)
            .on('clearData', function() {
                show([]);
            })
            .on('setData', function(e, data) {
                VB.globalVariable.setMessage(options.dataProvider, data);
            })
            .on('bindData', function(e, data) {
                VB.globalVariable.setMessage(options.dataProvider, data);
                show(data);
            });
    }

/* =======================================
 * Builder Component Setting
 * ======================================= */
    // 빌더에서는 예시 숫자로 실제 카드 모양을 미리 보여준다
    static builderSample(options) {
        return {
            value: Metric.format(options.aggregate === 'count' ? 128 : 12345.678, options),
            delta: options.compareMode !== 'none' ? '▲ 12.5%' : ''
        };
    }

    renderBuilder(options) {
        const height = options.height ? `height: ${options.height};` : '';
        return $(`
            <div id="${Metric.escape(options.id)}" class="component vb-item vb-metric" data-type="${this.componentId()}" style="width: ${options.width}; ${height}">
                ${super.componentDeleteBtn()}
                <div class="vb-metric-builder-body">${Metric.cardInner(options, Metric.builderSample(options))}</div>
            </div>
        `);
    }

    // 옵션을 바꾸면 카드 미리보기를 다시 그린다
    refreshBuilder($el, options) {
        $el.find('.vb-metric-builder-body').html(Metric.cardInner(options, Metric.builderSample(options)));
    }

    styleBuilder() {
        return Metric.css() + `
            .vb-item[data-type="${this.componentId()}"] .vb-metric-builder-body {
                display: flex;
                flex-direction: column;
                gap: 6px;
                pointer-events: none;
            }
            /* 빌더에서는 스파크라인 자리에 예시 곡선만 그린다 */
            .vb-item[data-type="${this.componentId()}"] .vb-metric-spark {
                background:
                    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 40' preserveAspectRatio='none'%3E%3Cpath d='M0 30 C 15 26, 22 34, 35 24 S 55 12, 65 18 S 85 8, 100 6' fill='none' stroke='%23b9b9b9' stroke-width='2' vector-effect='non-scaling-stroke'/%3E%3C/svg%3E") center / 100% 100% no-repeat;
            }
        `;
    }

/* =======================================
 * Option Panel Setting
 * ======================================= */
    optionPanelView($panel, options) {
        const accentOptions = Object.entries(Metric.accents())
            .map(([key, a]) => `<option value="${key}">${a.label}</option>`).join('');

        $panel.append(this.optionPanel.sectionTitle('기본'));
        $panel.append(this.optionPanel.input('component-id', {label: '컴포넌트명', size: 'col-6', enabled: false}));

        let $rowId = this.optionPanel.row();
        $rowId.append(this.optionPanel.input('id', {label: 'ID', size: 'col-6'}));
        $rowId.append(this.optionPanel.input('dataProvider', {label: 'dataProvider', size: 'col-6'}));
        $panel.append($rowId);

        let $rowSize = this.optionPanel.row();
        $rowSize.append(this.optionPanel.input('width', {label: '넓이', size: 'col-6'}));
        $rowSize.append(this.optionPanel.input('height', {label: '높이', size: 'col-6', placeholder: '비우면 내용에 맞춤'}));
        $panel.append($rowSize);

        $panel.append(this.optionPanel.sectionTitle('표시'));
        let $rowTitle = this.optionPanel.row();
        $rowTitle.append(this.optionPanel.input('title', {label: '제목', size: 'col-12'}));
        $rowTitle.append(this.optionPanel.input('icon', {label: '아이콘', size: 'col-6', placeholder: 'fas fa-users (비우면 숨김)'}));
        $rowTitle.append(this.optionPanel.select('accent', {label: '강조색', size: 'col-6', options: accentOptions}));
        $rowTitle.append(this.optionPanel.input('subText', {label: '보조 문구', size: 'col-12', placeholder: '예: 이번 달 누적'}));
        $panel.append($rowTitle);

        $panel.append(this.optionPanel.sectionTitle('값'));
        let $rowValue = this.optionPanel.row();
        $rowValue.append(this.optionPanel.input('valueAlias', {label: '값 alias', size: 'col-6', placeholder: 'value'}));
        $rowValue.append(this.optionPanel.select('aggregate', {label: '집계', size: 'col-6', options: `
                <option value="last">마지막 행</option>
                <option value="first">첫 행</option>
                <option value="sum">합계</option>
                <option value="avg">평균</option>
                <option value="min">최소</option>
                <option value="max">최대</option>
                <option value="count">행 수</option>
            `}));
        $panel.append($rowValue);

        $panel.append(this.optionPanel.sectionTitle('숫자 형식'));
        let $rowFormat = this.optionPanel.row();
        $rowFormat.append(this.optionPanel.input('prefix', {label: '앞 문자', size: 'col-4', placeholder: '₩'}));
        $rowFormat.append(this.optionPanel.input('suffix', {label: '뒤 문자', size: 'col-4', placeholder: '건, %, 원'}));
        $rowFormat.append(this.optionPanel.input('decimals', {label: '소수 자리', size: 'col-4', placeholder: '자동'}));
        $rowFormat.append(this.optionPanel.toggle('useThousands', {label: '천 단위 구분(,)', size: 'col-12'}));
        $rowFormat.append(this.optionPanel.toggle('compact', {label: '짧게 표시 (1.2만, 3.4억)', size: 'col-12'}));
        $panel.append($rowFormat);

        $panel.append(this.optionPanel.sectionTitle('비교 (증감)'));
        let $rowCompare = this.optionPanel.row();
        $rowCompare.append(this.optionPanel.select('compareMode', {label: '비교 대상', size: 'col-6', options: `
                <option value="none">사용안함</option>
                <option value="column">비교 컬럼</option>
                <option value="previous">앞 행 (시계열)</option>
            `}));
        $rowCompare.append(this.optionPanel.input('compareAlias', {label: '비교 alias', size: 'col-6', placeholder: '예: prev_value'}));
        $rowCompare.append(this.optionPanel.select('deltaType', {label: '증감 표시', size: 'col-6', options: `
                <option value="percent">퍼센트(%)</option>
                <option value="value">값 차이</option>
            `}));
        $rowCompare.append(this.optionPanel.input('compareLabel', {label: '비교 문구', size: 'col-6', placeholder: '예: 전월 대비'}));
        $rowCompare.append(this.optionPanel.toggle('invertTrend', {label: '감소가 좋음 (오류 수 등)', size: 'col-12'}));
        $panel.append($rowCompare);

        $panel.append(this.optionPanel.sectionTitle('스파크라인'));
        let $rowSpark = this.optionPanel.row();
        $rowSpark.append(this.optionPanel.toggle('useSparkline', {label: '추이 그래프 표시 (행 순서대로)', size: 'col-12'}));
        $panel.append($rowSpark);
    }

    // 옵션패널 입력 필드 (text/select) 와 토글
    static inputFields() {
        return ['id', 'dataProvider', 'width', 'height', 'title', 'icon', 'accent', 'subText', 'valueAlias', 'aggregate',
            'prefix', 'suffix', 'decimals', 'compareMode', 'compareAlias', 'deltaType', 'compareLabel'];
    }

    static toggleFields() {
        return ['useThousands', 'compact', 'invertTrend', 'useSparkline'];
    }

    optionPanelScript($el, options) {
        this.optionPanel.setValue('component-id', this.componentId());
        for (const key of Metric.inputFields()) {
            this.optionPanel.setValue(key, options[key] == null ? '' : options[key]);
        }
        for (const key of Metric.toggleFields()) {
            this.optionPanel.check(key, !!options[key]);
        }
    }

    optionPanelEvent($el, options, componentFactory) {
        for (const key of Metric.inputFields()) {
            this.optionPanel.inputEvent(key, (e) => {
                const value = $(e.target).val();
                super.changeOptionValue($el, options, key, value);

                if (key === 'id') {
                    return;
                }
                if (key === 'width') {
                    $el.css('width', value);
                }
                else if (key === 'height') {
                    $el.css('height', value || '');
                }
                this.refreshBuilder($el, options);
            });
        }

        for (const key of Metric.toggleFields()) {
            this.optionPanel.changeEvent(key, (e) => {
                super.changeOptionValue($el, options, key, $(e.target).is(':checked'));
                this.refreshBuilder($el, options);
            });
        }
    }
}
