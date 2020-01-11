
window.addEventListener('load', () => {
     initUI();
     fetchDiaryEntries();
});

function initUI() {
    console.log('init statistics ui')

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
}

async function fetchEntries(start, end) {
    const host = window.location.origin;
    const response = await fetch(host + "/diary/entries?from=" + start + "&to=" + end,
     {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
     });

    return response;
}

function toISODate(date) {
    return date.toISOString.substring(0,10);
}