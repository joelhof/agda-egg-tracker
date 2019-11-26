import '@vaadin/vaadin-text-field';
import '@vaadin/vaadin-button';
import '@vaadin/vaadin-notification';

window.addEventListener('load', () => {
     initUI();
     fetchDiaryEntries();
});

function initUI() {
  console.log(window.location.origin);
  const notification = document.querySelector('vaadin-notification');
  notification.renderer = function(root, n) {  
    // Check if there is a content generated with the previous renderer call not to recreate it.
    if (root.firstElementChild) {
      const detailedMessage = root.firstElementChild.getElementsByTagName('a')[0];
      detailedMessage.textContent = n.detailMessage;
      return;
    }

    const container = window.document.createElement('div');
    const header = window.document.createElement('b');
    header.textContent = n.header;
    const br = window.document.createElement('br');
    const details = window.document.createElement('a');
    details.textContent = n.detailMessage;

    container.appendChild(header);
    container.appendChild(br);
    container.appendChild(details);
    root.appendChild(container);
  }

  const eggCount = initEggCountField();
  const addButton = initSaveButton();

    function initSaveButton() {
        const addButton = document.querySelector('#post');
        addButton.addEventListener('click', e => {
            const data = {
                'eggs': eggCount.value,
                'timestamp': Date.now()
            };
            try {
                const response = postEggs(data);
                response.then(resp => {
                    showNotification(notification, 'Allt gick bra!', resp);
                    eggCount.value = '';
                }
                )
                .catch(error => {
                    showNotification(notification, 'Åh nej! Något gick fel', error);
                    console.log("show notification", error, " notification: ", notification);
                } );
                
            }
            catch (error) {
                console.log('Error, egg count not posted', error);
            }
        });
        return addButton;
    }

    function initEggCountField() {
        const eggCount = document.querySelector('#eggs');
        eggCount.addEventListener('change', e => {
            const current = eggCount.value;
            addButton.disabled = !eggCount.checkValidity();
        });
        eggCount.addEventListener('input', e => {
            const current = eggCount.value;
            addButton.disabled = !eggCount.checkValidity();
        });
        return eggCount;
    }
}

function showNotification(notification, header, detailedMessage) {
    notification.header = header;
    notification.detailMessage = detailedMessage;
    notification.open();
}

async function postEggs(data) {
    const host = window.location.origin;
    const response = await fetch(host + "/diary/entry", {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    return await response.text();
}

function fetchDiaryEntries() {
    const response = fetchTodaysEntries();
    response
        .then(errorHandler())
        .then(r => r.json())
        .then(r => console.log("Succes!", r))
        .catch(error => console.log("Failed!", error));
}

function errorHandler() {
    return r => {
        if (!r.ok) {
            throw Error(r.status, r.text());
        }
    };
}

async function fetchTodaysEntries() {
    const host = window.location.origin;
    const response = await fetch(host + "/diary/entries?date=2019-11-24", {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
    });

    return response;
}
