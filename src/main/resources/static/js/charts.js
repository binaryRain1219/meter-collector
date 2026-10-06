// Chart.js 공통 설정. 색은 app.css 의 CSS 변수에서 읽는다.
(function () {
    const css = getComputedStyle(document.documentElement);
    const token = name => css.getPropertyValue(name).trim();
    const seriesColor = i => token('--series-' + ((i % 8) + 1));

    Chart.defaults.font.family = 'system-ui, -apple-system, "Segoe UI", "Apple SD Gothic Neo", sans-serif';
    Chart.defaults.color = token('--text-muted');
    Chart.defaults.maintainAspectRatio = false;
    Chart.defaults.animation = false;

    function axes(unit) {
        return {
            x: {
                grid: {display: false},
                border: {color: token('--axis')},
                ticks: {maxRotation: 0, autoSkipPadding: 16}
            },
            y: {
                beginAtZero: true,
                grid: {color: token('--grid')},
                border: {display: false},
                title: {display: true, text: unit}
            }
        };
    }

    function tooltip(unit, digits) {
        return {
            backgroundColor: token('--surface'),
            titleColor: token('--text-primary'),
            bodyColor: token('--text-secondary'),
            borderColor: token('--border'),
            borderWidth: 1,
            padding: 10,
            callbacks: {
                label: ctx => ctx.raw == null
                    ? `${ctx.dataset.label}: 값 없음`
                    : `${ctx.dataset.label}: ${Number(ctx.raw).toFixed(digits)} ${unit}`
            }
        };
    }

    // 여러 장비의 선 그래프. series: [{name, values}]
    window.lineChart = function (canvas, labels, series, unit) {
        return new Chart(canvas, {
            type: 'line',
            data: {
                labels,
                datasets: series.map((s, i) => ({
                    label: s.name,
                    data: s.values,
                    borderColor: seriesColor(i),
                    backgroundColor: seriesColor(i),
                    borderWidth: 2,
                    pointRadius: 2,  // 앞뒤가 비어 있는 구간 값도 보이게
                    pointHoverRadius: 4,
                    spanGaps: false
                }))
            },
            options: {
                interaction: {mode: 'index', intersect: false},
                scales: axes(unit),
                plugins: {
                    legend: {position: 'top', align: 'end', labels: {boxWidth: 12, boxHeight: 2}},
                    tooltip: tooltip(unit, 2)
                }
            }
        });
    };

    // 한 계열 막대 그래프.
    window.barChart = function (canvas, labels, values, name, unit) {
        return new Chart(canvas, {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    label: name,
                    data: values,
                    backgroundColor: seriesColor(0),
                    borderRadius: {topLeft: 4, topRight: 4},
                    borderSkipped: 'bottom',
                    categoryPercentage: 0.9,
                    barPercentage: 0.9
                }]
            },
            options: {
                interaction: {mode: 'index', intersect: false},
                scales: axes(unit),
                plugins: {
                    legend: {display: false},
                    tooltip: tooltip(unit, 3)
                }
            }
        });
    };
})();
