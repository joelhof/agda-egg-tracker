import Chart from 'chart.js';
import '@vaadin/vaadin-text-field';

window.addEventListener('load', () => {
     //initUI();
     fetchDiaryEntries();
});

function displayChart(entries) {

    const averager = (accumulator, currentValue) => {
        accumulator.sum   = accumulator.sum + currentValue;
        accumulator.count = ++accumulator.count;
        return accumulator;
    };
    var averageAcc = entries.map(e => e.eggs)
        .reduce(averager, {sum: 0.0, count: 0});
    var average = averageAcc.sum / averageAcc.count;
    console.log('init statistics ui');
    console.log('Average: ', average);
    var ctx = document.getElementById("egg-chart");
    var chart = new Chart(ctx, {
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
                    label: '10-dagars medelvärde',
                    type: 'line',
                    data: []
                }
            ]
        }
    });
}

function fetchDiaryEntries() {

    // Fetch diary entries for the "selected" time period.
    // For now, fetch the last 12 months.
    const today = new Date();
    const oneYearAgo = new Date();
    oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1)
    const response = fetchEntries(
        toISODate(oneYearAgo),
        toISODate(today)
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
    // const response = await fetch(host + "/diary/entries?from=" + start + "&to=" + end,
    //  {
    //     method: 'GET',
    //     headers: { 'Accept': 'application/json' }
    //  });

    const data = [{"eggs":6,"timestamp":1574977754934},{"eggs":7,"timestamp":1574893748527},{"eggs":5,"timestamp":1574632510021},{"eggs":2,"timestamp":1573593347407},{"eggs":6,"timestamp":1572818173386},{"eggs":5,"timestamp":1572468610972}]
    const response = {};
    response.json = () => data;
    return response;
}

function toISODate(date) {
    return date.toISOString().substring(0,10);
}
