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
    updateStatistics(averageAcc);
    const movingAverager = (value, index, entries) => {
        var start = (index - 6) < 0 ? 0 : (index - 6);
        var previousWeek = entries.slice(start, index + 1)
            .reduce(averager, {sum: 0.0, count: 0});
        return previousWeek.sum / previousWeek.count;
    };
    let movingAverage = entries
        .map(e => e.eggs)
        .map(movingAverager);
    let ctx = document.getElementById("egg-chart");
    let colors = entries.map(e => {

        if (e.event && e.event.includes('död')) {
            return '#000000';
        }
        if (e.event && e.event.includes('sjuk')) {
            return '#eb4c34';
        }
        return '#adffe5';
    }
    );
    chart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: entries.map(e => new Date(e.timestamp))
                           .map(d => toISODate(d)),
            datasets: [
                {   
                    backgroundColor: colors,
                    fill: false,
                    label: 'Dagliga ägg',
                    data: entries.map(e => e.eggs)
                },
                {
                    backgroundColor: '',
                    label: 'Rullande 7-dagars medelvärde',
                    type: 'line',
                    data: movingAverage,
                    options: {
                        tooltips: {
                            callbacks: {
                                label: function(tooltipItem, data) {
                                    let label = data.datasets[tooltipItem.datasetIndex].label;

                                    if (label) {
                                        label += ': ';
                                        label += data.datasets[tooltipItem.datasetIndex].data[tooltipItem.index];
                                    }
                                    return label;
                                },
                                footer: function(tooltipItem, data) {
                                    return entries[tooltipItem[0].index].event || '';
                                }
                            }
                        }
                    }
                }
            ]
        },
        options: {
            tooltips: {
                callbacks: {
                    label: function(tooltipItem, data) {
                        let label = data.datasets[tooltipItem.datasetIndex].label;

                        if (label) {
                            label += ': ';
                            label += data.datasets[tooltipItem.datasetIndex].data[tooltipItem.index];
                        }
                        return label;
                    },
                    footer: function(tooltipItem, data) {
                        return entries[tooltipItem[0].index].event || '';
                    }
                }
            }
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
                    label: 'Dagliga ägg',
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
    initAppState();
    initStartDateField();
    initEndDateField();

    function initEndDateField() {
        const endDateField = document.querySelector('#enddate');
        endDateField.value = currentPeriod.endDate;
        endDateField.addEventListener('change', e => {
            const endDate = endDateField.value;
            currentPeriod.endDate = endDate;
            fetchDiaryEntries(currentPeriod);
            console.log('end date changed', endDate);
        });
    }

    function initStartDateField() {
        const startDateField = document.querySelector('#startdate');
        startDateField.value = currentPeriod.startDate;
        startDateField.addEventListener('change', e => {
            const startDate = startDateField.value;
            currentPeriod.startDate = startDate;
            fetchDiaryEntries(currentPeriod);
            console.log('start date changed', startDate);
        });
    }

    function initAppState() {
        const today = new Date();
        const oneYearAgo = new Date();
        oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
        currentPeriod = {};
        currentPeriod['startDate'] = toISODate(oneYearAgo);
        currentPeriod['endDate'] = toISODate(today);
    }
}

function fetchDiaryEntries(period) {
    /*displayChart([{
        "eggs": 3,
        "timestamp": 1611621137516,
        "event": null
    },
        {
            "eggs": 5,
            "timestamp": 1611621137516 + 24 * 3600,
            "event": "Brun höna hängig, sjuk"
        },
        {
            "eggs": 7,
            "timestamp": 1611621137516 + 48 * 3600,
            "event": "Brun höna död"
        }]);*/
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

function updateStatistics(avergaAccumulator) {
    const sumField = document.querySelector('#sum');
    sumField.textContent = avergaAccumulator.sum;
    const meanField = document.querySelector('#mean');
    meanField.textContent = avergaAccumulator.sum / avergaAccumulator.count;
}

function toISODate(date) {
    return date.toISOString().substring(0,10);
}
