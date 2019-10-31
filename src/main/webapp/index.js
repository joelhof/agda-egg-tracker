import '@vaadin/vaadin-text-field';
import '@vaadin/vaadin-button';

window.addEventListener('load', () => {
     initUI();
});

function initUI() {
  const eggCount = document.querySelector('#eggs');
  const addButton = document.querySelector('#post');

  eggCount.addEventListener('change', e => {
       const current = eggCount.value;
       addButton.disabled = !eggCount.checkValidity();
  });
  eggCount.addEventListener('input', e => {
        const current = eggCount.value;
        addButton.disabled = !eggCount.checkValidity();
  });

  addButton.addEventListener('click', e => {

    const data = {
        'eggs': eggCount.value,
        'timestamp': Date.now()
    };
    console.log(data);
    try {
        const response = postEggs(data);
        response.then()
        .catch()
        console.log(response.status);
        eggCount.value = '';
    } catch (error) {
        console.log('Error, egg count not posted', error);
    }
  });
}

async function postEggs(data) {
    const response = await fetch("http://localhost:19080/diary/entry", {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    return await response.text();
}