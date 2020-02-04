import Chart from 'chart.js';
import '@vaadin/vaadin-text-field';

let currentPeriod;
let chart;

window.addEventListener('load', () => {
     initControls();
     initChart();
     fetchDiaryEntries(currentPeriod);
});

function displayChart(entries) {
    chart.destroy();
    const averager = (accumulator, currentValue) => {
        accumulator.sum = accumulator.sum + currentValue;
        accumulator.count = ++accumulator.count;
        return accumulator;
    };
    var averageAcc = entries.map(e => e.eggs)
        .reduce(averager, {sum: 0.0, count: 0});
    console.log('init statistics ui');
    updateMean(averageAcc);
    const movingAverager = (value, index, entries) => {
        var start = (index - 6) < 0 ? 0 : (index - 6);
        var previousWeek = entries.slice(start, index + 1)
            .reduce(averager, {sum: 0.0, count: 0});
        console.log(start, previousWeek);
        return previousWeek.sum / previousWeek.count;
    };
    var movingAverage = entries
        .map(e => e.eggs)
        .map(movingAverager);
    var ctx = document.getElementById("egg-chart");
    chart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: entries.map(e => new Date(e.timestamp))
                           .map(d => toISODate(d)),
            datasets: [
                {   
                    backgroundColor: '#adffe5',
                    fill: false,
                    label: 'Dagiliga ägg',
                    data: entries.map(e => e.eggs)
                },
                {
                    backgroundColor: '',
                    label: 'Rullande 7-dagars medelvärde',
                    type: 'line',
                    data: movingAverage
                }
            ]
        }
    });
}

function initChart() {
    var ctx = document.getElementById("egg-chart");
    chart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: [],
            datasets: [
                {   
                    backgroundColor: '#adffe5',
                    fill: false,
                    label: 'Dagiliga ägg',
                    data: []
                },
                {
                    backgroundColor: '',
                    label: 'Rullande 7-dagars medelvärde',
                    type: 'line',
                    data: []
                }
            ]
        }
    });
}

function initControls() {
    const today = new Date();
    const oneYearAgo = new Date();
    oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
    currentPeriod = new Object();
    currentPeriod['startDate'] = toISODate(oneYearAgo);
    currentPeriod['endDate'] = toISODate(today);8
    const startDateField = document.querySelector('#startdate');
    startDateField.value = currentPeriod.startDate;
    startDateField.addEventListener('change', e => {
        const startDate = startDateField.value;
        currentPeriod.startDate = startDate;
        fetchDiaryEntries(currentPeriod);
        console.log('start date changed', startDate);
    });
    const endDateField = document.querySelector('#enddate');
    endDateField.value = currentPeriod.endDate;
    endDateField.addEventListener('change', e => {
        const endDate = endDateField.value;
        currentPeriod.endDate = endDate;
        fetchDiaryEntries(currentPeriod);
        console.log('end date changed', endDate);
    });
}

function fetchDiaryEntries(period) {

    const response = fetchEntries(
        period.startDate,
        period.endDate
    );
    response.then(r => r.json())
            .then(entries => {
                console.log(entries);
                return entries;
            })
            .then(entries => displayChart(entries))
}

async function fetchEntries(start, end) {
    const host = window.location.origin;
    const response = await fetch(host + "/diary/entries?from=" + start + "&to=" + end,
     {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
     });

    // const data = [{"eggs":6,"timestamp":1574977754934},{"eggs":7,"timestamp":1574893748527},{"eggs":5,"timestamp":1574632510021},{"eggs":2,"timestamp":1573593347407},{"eggs":6,"timestamp":1572818173386},{"eggs":5,"timestamp":1572468610972}]
    // const response = {};
    // response.json = () => data;
    return response;
}

function updateMean(avergaAccumulator) {
    const meanField = document.querySelector('#mean');
    meanField.textContent = avergaAccumulator.sum / avergaAccumulator.count;
}

function toISODate(date) {
    return date.toISOString().substring(0,10);
}
