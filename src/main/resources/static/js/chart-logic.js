console.log("✅ Đã tải file chart-logic.js phiên bản MỚI NHẤT (Hỗ trợ cấu hình Limits)");

const CHART_CONFIG = {
    margin: { top: 30, right: 30, bottom: 65, left: 60 },
    height: 350,
    colors: {
        red: "#e74c3c",
        blue: "#3498db",
        green: "#2ecc71",
        black: "#2c3e50",
        grid: "#f0f0f0",
        warn: "#f39c12",  // Vàng cam cho Warning
        error: "#c0392b"  // Đỏ sậm cho Error
    }
};

// ============================================================================
// HÀM TIỆN ÍCH
// ============================================================================
function createChartSvg(selector) {
    const container = d3.select(selector);
    container.selectAll("*").remove(); 

    const width = container.node().getBoundingClientRect().width;
    const height = CHART_CONFIG.height;

    const svg = container.append("svg")
        .attr("width", width)
        .attr("height", height)
        .attr("viewBox", `0 0 ${width} ${height}`)
        .style("max-width", "100%")
        .style("height", "auto");

    const innerWidth = width - CHART_CONFIG.margin.left - CHART_CONFIG.margin.right;
    const innerHeight = height - CHART_CONFIG.margin.top - CHART_CONFIG.margin.bottom;

    const g = svg.append("g")
        .attr("transform", `translate(${CHART_CONFIG.margin.left},${CHART_CONFIG.margin.top})`);

    return { svg, g, innerWidth, innerHeight };
}

function drawGridAndAxes(g, xScale, yScale, innerWidth, innerHeight, xAxisLabel, yAxisLabel, isPointScale = false, dataLength = 0) {
    g.append("g")
        .attr("class", "grid")
        .call(d3.axisLeft(yScale).tickSize(-innerWidth).tickFormat(""))
        .selectAll("line").style("stroke", CHART_CONFIG.colors.grid);

    const xAxis = d3.axisBottom(xScale);
    let shouldRotate = false;

    if (isPointScale && dataLength > 15) {
        const step = Math.ceil(dataLength / 15);
        xAxis.tickValues(xScale.domain().filter((d, i) => i % step === 0));
        shouldRotate = true;
    }

    const xAxisGroup = g.append("g")
        .attr("transform", `translate(0,${innerHeight})`)
        .call(xAxis);

    if (shouldRotate) {
        xAxisGroup.selectAll("text")
            .attr("transform", "rotate(-45)")
            .style("text-anchor", "end")
            .attr("dx", "-0.8em")
            .attr("dy", "0.15em");
    }

    g.append("text")
        .attr("x", innerWidth / 2)
        .attr("y", innerHeight + (shouldRotate ? 55 : 40))
        .attr("text-anchor", "middle")
        .style("font-size", "12px")
        .style("fill", "#64748b")
        .style("font-weight", "600")
        .text(xAxisLabel);

    g.append("g").call(d3.axisLeft(yScale));

    g.append("text")
        .attr("transform", "rotate(-90)")
        .attr("x", -innerHeight / 2)
        .attr("y", -45)
        .attr("text-anchor", "middle")
        .style("font-size", "12px")
        .style("fill", "#64748b")
        .style("font-weight", "600")
        .text(yAxisLabel);
}

// BƯỚC SỬA QUAN TRỌNG: Ép kiểu string sang số để vẽ chính xác
function applyThresholdLines(g, width, yScale, warnVal, errVal) {
    if (warnVal !== undefined && warnVal !== null && warnVal !== "") {
        const w = parseFloat(warnVal);
        if (!isNaN(w)) {
            g.append("line").attr("x1", 0).attr("x2", width).attr("y1", yScale(w)).attr("y2", yScale(w))
                .attr("stroke", CHART_CONFIG.colors.warn).attr("stroke-dasharray", "5,5").attr("stroke-width", 2);
            g.append("text").attr("x", width).attr("y", yScale(w) - 5).attr("text-anchor", "end")
                .style("fill", CHART_CONFIG.colors.warn).style("font-size", "11px").style("font-weight", "bold").text(`Warn: ${w}%`);
        }
    }
    if (errVal !== undefined && errVal !== null && errVal !== "") {
        const e = parseFloat(errVal);
        if (!isNaN(e)) {
            g.append("line").attr("x1", 0).attr("x2", width).attr("y1", yScale(e)).attr("y2", yScale(e))
                .attr("stroke", CHART_CONFIG.colors.error).attr("stroke-dasharray", "5,5").attr("stroke-width", 2);
            g.append("text").attr("x", width).attr("y", yScale(e) - 5).attr("text-anchor", "end")
                .style("fill", CHART_CONFIG.colors.error).style("font-size", "11px").style("font-weight", "bold").text(`Error: ${e}%`);
        }
    }
}

// Hàm lấy dữ liệu Limits an toàn từ UI
function getLimits() {
    return typeof qcLimitsConfig !== 'undefined' ? qcLimitsConfig : {
        adapterWarn: 5, adapterError: 10,
        nWarn: 5, nError: 20,
        dupWarn: 70, dupError: 50
    };
}

// ============================================================================
// 1. Adapter Content Chart (CÓ VẼ ĐƯỜNG LIMITS)
// ============================================================================
window.drawAdapterChart = function(selector, data) {
    if (!data || data.length === 0) return;

    if (!document.getElementById("d3-tooltip-style")) {
        const style = document.createElement('style');
        style.id = "d3-tooltip-style";
        style.innerHTML = `.d3-tooltip { position: absolute; padding: 8px 12px; font: 13px sans-serif; background: rgba(44, 62, 80, 0.95); color: #fff; border-radius: 6px; pointer-events: none; opacity: 0; z-index: 1000; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }`;
        document.head.appendChild(style);
    }

    let tooltip = d3.select("body").select(".d3-tooltip");
    if (tooltip.empty()) tooltip = d3.select("body").append("div").attr("class", "d3-tooltip");

    d3.select(selector).selectAll("*").remove();

    const margin = {top: 20, right: 220, bottom: 65, left: 50}; 
    const width = 850 - margin.left - margin.right;
    const height = 350 - margin.top - margin.bottom;

    const svg = d3.select(selector)
        .append("svg").attr("width", "100%").attr("viewBox", `0 0 ${width + margin.left + margin.right} ${height + margin.top + margin.bottom}`)
        .append("g").attr("transform", `translate(${margin.left},${margin.top})`);

    let allLabels = [];
    data.forEach(series => {
        if(series.points) series.points.forEach(p => { if (!allLabels.includes(p.x)) allLabels.push(p.x); });
    });

    const x = d3.scalePoint().domain(allLabels).range([0, width]);
    const y = d3.scaleLinear().domain([0, 100]).range([height, 0]);

    svg.append("g")
        .attr("class", "grid").call(d3.axisLeft(y).tickSize(-width).tickFormat("").ticks(5))
        .selectAll("line").attr("stroke", "#e0e0e0").attr("stroke-dasharray", "3,3");

    // VẼ ĐƯỜNG GIỚI HẠN ADAPTER TỪ CẤU HÌNH GIAO DIỆN
    const limits = getLimits();
    applyThresholdLines(svg, width, y, limits.adapterWarn, limits.adapterError);

    const step = Math.ceil(allLabels.length / 15);
    svg.append("g")
        .attr("transform", `translate(0,${height})`)
        .call(d3.axisBottom(x).tickValues(x.domain().filter((d,i) => i % step === 0)))
        .selectAll("text").attr("transform", "rotate(-45)").style("text-anchor", "end").attr("dx", "-0.8em").attr("dy", "0.15em");

    svg.append("g").call(d3.axisLeft(y).ticks(5).tickFormat(d => d + "%"));

    const color = d3.scaleOrdinal(d3.schemeCategory10);
    const line = d3.line().x(d => x(d.x)).y(d => y(+d.y)).curve(d3.curveMonotoneX);

    data.forEach((series, index) => {
        if(series.points && series.points.length > 0) {
            svg.append("path").datum(series.points).attr("fill", "none").attr("stroke", color(index)).attr("stroke-width", 2.5).attr("d", line)
                .style("cursor", "pointer")
                .on("mouseover", function(event) {
                    svg.selectAll("path").attr("opacity", 0.2);
                    d3.select(this).attr("stroke-width", 5).attr("opacity", 1);
                    const maxVal = d3.max(series.points, d => +d.y).toFixed(3);
                    tooltip.transition().duration(200).style("opacity", 1);
                    tooltip.html(`<strong>${series.adapterName}</strong><br/>Max Value: ${maxVal}%`)
                           .style("left", (event.pageX + 15) + "px").style("top", (event.pageY - 28) + "px");
                })
                .on("mouseout", function() {
                    svg.selectAll("path").attr("opacity", 1).attr("stroke-width", 2.5);
                    tooltip.transition().duration(500).style("opacity", 0);
                });

            const legendGroup = svg.append("g").style("cursor", "pointer")
                .on("mouseover", () => {
                    svg.selectAll("path").attr("opacity", 0.2);
                    svg.selectAll("path").filter((d, i) => i === index).attr("stroke-width", 5).attr("opacity", 1);
                })
                .on("mouseout", () => {
                    svg.selectAll("path").attr("opacity", 1).attr("stroke-width", 2.5);
                });

            legendGroup.append("rect").attr("x", width + 20).attr("y", index * 22).attr("width", 12).attr("height", 12).attr("rx", 2).style("fill", color(index));
            legendGroup.append("text").attr("x", width + 38).attr("y", index * 22 + 10).style("font-size", "12px").style("fill", "#333").text(series.adapterName);
        }
    });

    svg.append("text").attr("text-anchor", "middle").attr("x", width / 2).attr("y", height + 55).style("font-weight", "600").style("font-size", "12px").style("fill", "#64748b").text("Position in read (bp)");
    svg.append("text").attr("text-anchor", "middle").attr("transform", "rotate(-90)").attr("y", -35).attr("x", -height / 2).style("font-weight", "600").style("font-size", "12px").style("fill", "#64748b").text("% Cumulative Adapter");
};

// ============================================================================
// 2. Per Base Sequence Quality Chart
// ============================================================================
window.drawPerBaseQualityChart = function(selector, data) {
    if (!data || data.length === 0) return;
    d3.select(selector).selectAll("*").remove();

    const margin = {top: 30, right: 30, bottom: 65, left: 50}; 
    const width = 850 - margin.left - margin.right;
    const height = 400 - margin.top - margin.bottom;

    const svg = d3.select(selector).append("svg").attr("width", "100%").attr("viewBox", `0 0 ${width + margin.left + margin.right} ${height + margin.top + margin.bottom}`)
        .append("g").attr("transform", `translate(${margin.left},${margin.top})`);

    const x = d3.scaleBand().domain(data.map(d => d.base)).range([0, width]).paddingInner(0.2).paddingOuter(0.2);
    const y = d3.scaleLinear().domain([0, 40]).range([height, 0]);

    const bgGroup = svg.append("g").attr("class", "background-zones");
    bgGroup.append("rect").attr("x", 0).attr("width", width).attr("y", y(40)).attr("height", y(28) - y(40)).attr("fill", "#c8e6c9").attr("opacity", 0.7);
    bgGroup.append("rect").attr("x", 0).attr("width", width).attr("y", y(28)).attr("height", y(20) - y(28)).attr("fill", "#fff9c4").attr("opacity", 0.7);
    bgGroup.append("rect").attr("x", 0).attr("width", width).attr("y", y(20)).attr("height", y(0) - y(20)).attr("fill", "#ffcdd2").attr("opacity", 0.7);

    const step = Math.ceil(data.length / 15);
    svg.append("g")
        .attr("transform", `translate(0,${height})`)
        .call(d3.axisBottom(x).tickValues(x.domain().filter((d,i) => i % step === 0)))
        .selectAll("text").attr("transform", "rotate(-45)").style("text-anchor", "end").attr("dx", "-0.8em").attr("dy", "0.15em");

    svg.append("g").call(d3.axisLeft(y));
    svg.append("g").attr("class", "grid").call(d3.axisLeft(y).tickSize(-width).tickFormat("")).selectAll("line").attr("stroke", "#e0e0e0").attr("stroke-dasharray", "3,3");

    const boxWidth = x.bandwidth();
    const boxGroup = svg.append("g").attr("class", "box-plots");

    data.forEach(d => {
        const center = x(d.base) + boxWidth / 2;
        boxGroup.append("line").attr("x1", center).attr("x2", center).attr("y1", y(d.uw)).attr("y2", y(d.lw)).attr("stroke", "black");
        boxGroup.append("line").attr("x1", center - boxWidth/4).attr("x2", center + boxWidth/4).attr("y1", y(d.uw)).attr("y2", y(d.uw)).attr("stroke", "black");
        boxGroup.append("line").attr("x1", center - boxWidth/4).attr("x2", center + boxWidth/4).attr("y1", y(d.lw)).attr("y2", y(d.lw)).attr("stroke", "black");
        boxGroup.append("rect").attr("x", x(d.base)).attr("width", boxWidth).attr("y", y(d.q3)).attr("height", y(d.q1) - y(d.q3)).attr("fill", "yellow").attr("stroke", "black");
        boxGroup.append("line").attr("x1", x(d.base)).attr("x2", x(d.base) + boxWidth).attr("y1", y(d.median)).attr("y2", y(d.median)).attr("stroke", "red").attr("stroke-width", 1.5);
    });

    const meanLine = d3.line().x(d => x(d.base) + boxWidth / 2).y(d => y(d.mean)).curve(d3.curveMonotoneX);
    svg.append("path").datum(data).attr("fill", "none").attr("stroke", "blue").attr("stroke-width", 1.5).attr("d", meanLine);

    svg.append("text").attr("text-anchor", "middle").attr("x", width / 2).attr("y", height + 55).style("font-weight", "600").style("font-size", "12px").style("fill", "#64748b").text("Position in read (bp)");
    svg.append("text").attr("text-anchor", "middle").attr("transform", "rotate(-90)").attr("y", -margin.left + 15).attr("x", -height / 2).style("font-weight", "600").style("font-size", "12px").style("fill", "#64748b").text("Quality Scores (Phred)");
};

// ============================================================================
// 3. Per Sequence Quality Scores
// ============================================================================
window.drawPerSequenceQualityChart = function(selector, data) {
    if (!data || data.length === 0) return;
    const { g, innerWidth, innerHeight } = createChartSvg(selector);

    const xScale = d3.scaleLinear().domain(d3.extent(data, d => d.quality)).range([0, innerWidth]);
    const yScale = d3.scaleLinear().domain([0, d3.max(data, d => d.count)]).range([innerHeight, 0]);

    drawGridAndAxes(g, xScale, yScale, innerWidth, innerHeight, "Mean Sequence Quality (Phred Score)", "Count");

    const line = d3.line().x(d => xScale(d.quality)).y(d => yScale(d.count)).curve(d3.curveMonotoneX);
    g.append("path").datum(data).attr("fill", "none").attr("stroke", CHART_CONFIG.colors.red).attr("stroke-width", 2).attr("d", line);
};

// ============================================================================
// 4. Per Base Sequence Content
// ============================================================================
window.drawPerBaseSequenceContentChart = function(selector, data) {
    if (!data || data.length === 0) return;
    const { g, innerWidth, innerHeight } = createChartSvg(selector);

    const xScale = d3.scalePoint().domain(data.map(d => d.position)).range([0, innerWidth]);
    const yScale = d3.scaleLinear().domain([0, 100]).range([innerHeight, 0]);

    drawGridAndAxes(g, xScale, yScale, innerWidth, innerHeight, "Position in read (bp)", "Percentage (%)", true, data.length);

    const bases = [
        { key: "t", color: CHART_CONFIG.colors.red, label: "%T" },
        { key: "c", color: CHART_CONFIG.colors.blue, label: "%C" },
        { key: "a", color: CHART_CONFIG.colors.green, label: "%A" },
        { key: "g", color: CHART_CONFIG.colors.black, label: "%G" }
    ];

    bases.forEach(base => {
        const line = d3.line().x(d => xScale(d.position)).y(d => yScale(d[base.key])).curve(d3.curveLinear);
        g.append("path").datum(data).attr("fill", "none").attr("stroke", base.color).attr("stroke-width", 1.5).attr("d", line);
    });

    const legend = g.append("g").attr("transform", `translate(${innerWidth - 50}, 10)`);
    bases.forEach((base, i) => {
        legend.append("text").attr("y", i * 15).attr("fill", base.color).style("font-size", "10px").style("font-weight", "bold").text(base.label);
    });
};

// ============================================================================
// 5. Per Sequence GC Content
// ============================================================================
window.drawPerSequenceGCContentChart = function(selector, data) {
    if (!data || data.length === 0) return;
    const { g, innerWidth, innerHeight } = createChartSvg(selector);

    const xScale = d3.scaleLinear().domain([0, 100]).range([0, innerWidth]);
    const yScale = d3.scaleLinear().domain([0, d3.max(data, d => Math.max(d.actualCount, d.theoreticalCount))]).range([innerHeight, 0]);

    drawGridAndAxes(g, xScale, yScale, innerWidth, innerHeight, "Mean GC content (%)", "Count");

    const lineActual = d3.line().x(d => xScale(d.gcPercent)).y(d => yScale(d.actualCount)).curve(d3.curveMonotoneX);
    g.append("path").datum(data).attr("fill", "none").attr("stroke", CHART_CONFIG.colors.red).attr("stroke-width", 2).attr("d", lineActual);

    const lineTheoretical = d3.line().x(d => xScale(d.gcPercent)).y(d => yScale(d.theoreticalCount)).curve(d3.curveMonotoneX);
    g.append("path").datum(data).attr("fill", "none").attr("stroke", CHART_CONFIG.colors.blue).attr("stroke-width", 2).attr("d", lineTheoretical);
};

// ============================================================================
// 6. Per Base N Content (CÓ VẼ ĐƯỜNG LIMITS)
// ============================================================================
window.drawPerBaseNContentChart = function(selector, data) {
    if (!data || data.length === 0) return;
    const { g, innerWidth, innerHeight } = createChartSvg(selector);

    const xScale = d3.scalePoint().domain(data.map(d => d.position)).range([0, innerWidth]);
    const yScale = d3.scaleLinear().domain([0, 100]).range([innerHeight, 0]);

    drawGridAndAxes(g, xScale, yScale, innerWidth, innerHeight, "Position in read (bp)", "Percentage (%)", true, data.length);

    // VẼ ĐƯỜNG GIỚI HẠN N CONTENT
    const limits = getLimits();
    applyThresholdLines(g, innerWidth, yScale, limits.nWarn, limits.nError);

    const line = d3.line().x(d => xScale(d.position)).y(d => yScale(d.nPercent)).curve(d3.curveLinear);
    g.append("path").datum(data).attr("fill", "none").attr("stroke", CHART_CONFIG.colors.red).attr("stroke-width", 2).attr("d", line);
};

// ============================================================================
// 7. Sequence Length Distribution
// ============================================================================
window.drawSequenceLengthDistributionChart = function(selector, data) {
    if (!data || data.length === 0) return;
    const { g, innerWidth, innerHeight } = createChartSvg(selector);

    const xScale = d3.scaleLinear().domain(d3.extent(data, d => d.length)).range([0, innerWidth]);
    const yScale = d3.scaleLinear().domain([0, d3.max(data, d => d.count)]).range([innerHeight, 0]);

    drawGridAndAxes(g, xScale, yScale, innerWidth, innerHeight, "Sequence Length (bp)", "Count");

    const line = d3.line().x(d => xScale(d.length)).y(d => yScale(d.count)).curve(d3.curveMonotoneX);
    g.append("path").datum(data).attr("fill", "none").attr("stroke", CHART_CONFIG.colors.red).attr("stroke-width", 2).attr("d", line);
};

// ============================================================================
// 8. Sequence Duplication Levels (CÓ VẼ ĐƯỜNG LIMITS)
// ============================================================================
window.drawDuplicationLevelsChart = function(selector, data) {
    if (!data || data.length === 0) return;
    const { g, innerWidth, innerHeight } = createChartSvg(selector);

    const xScale = d3.scalePoint().domain(data.map(d => d.level)).range([0, innerWidth]);
    const yScale = d3.scaleLinear().domain([0, 100]).range([innerHeight, 0]);

    drawGridAndAxes(g, xScale, yScale, innerWidth, innerHeight, "Sequence Duplication Level", "Percentage (%)");

    // VẼ ĐƯỜNG GIỚI HẠN DUPLICATION
    const limits = getLimits();
    applyThresholdLines(g, innerWidth, yScale, limits.dupWarn, limits.dupError);

    const lineDedup = d3.line().x(d => xScale(d.level)).y(d => yScale(d.dedupPercent)).curve(d3.curveLinear);
    const lineTotal = d3.line().x(d => xScale(d.level)).y(d => yScale(d.totalPercent)).curve(d3.curveLinear);

    g.append("path").datum(data).attr("fill", "none").attr("stroke", CHART_CONFIG.colors.red).attr("stroke-width", 2).attr("d", lineDedup);
    g.append("path").datum(data).attr("fill", "none").attr("stroke", CHART_CONFIG.colors.blue).attr("stroke-width", 2).attr("d", lineTotal);
};

// ============================================================================
// 9. Overrepresented Sequences (Table)
// ============================================================================
window.drawOverrepresentedTable = function(selector, data) {
    const container = document.querySelector(selector);
    if (!data || data.length === 0) {
        container.innerHTML = "<div class='text-center mt-6'><span class='bg-emerald-100 text-emerald-700 px-3 py-1 rounded-full text-sm font-medium'><i class='fa-solid fa-check-circle mr-1'></i> Không có trình tự nào bị lặp lại quá mức (No overrepresented sequences).</span></div>";
        return;
    }

    let rows = data.map(d => `
        <tr class="border-b border-slate-200 hover:bg-slate-50 font-mono text-sm">
            <td class="py-2 px-4 break-all">${d.sequence}</td>
            <td class="py-2 px-4 text-right">${d.count}</td>
            <td class="py-2 px-4 text-right">${d.percentage.toFixed(4)}%</td>
            <td class="py-2 px-4 text-slate-500">${d.source}</td>
        </tr>
    `).join('');

    container.innerHTML = `
        <div class="overflow-x-auto border border-slate-200 rounded-lg">
            <table class="w-full text-left border-collapse">
                <thead>
                    <tr class="bg-blue-900 text-white text-sm">
                        <th class="py-3 px-4 font-semibold">Sequence</th>
                        <th class="py-3 px-4 font-semibold text-right">Count</th>
                        <th class="py-3 px-4 font-semibold text-right">Percentage</th>
                        <th class="py-3 px-4 font-semibold">Possible Source</th>
                    </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
        </div>
    `;
};