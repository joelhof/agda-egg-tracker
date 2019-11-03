import '@vaadin/vaadin-text-field';
import '@vaadin/vaadin-button';
import '@vaadin/vaadin-notification';

window.addEventListener('load', () => {
     initUI();
});

function initUI() {
  const notification = document.querySelector('vaadin-notification');

  notification.renderer = function(root, n) {
      console.log('render notification', n);
    // Check if there is a content generated with the previous renderer call not to recreate it.
    if (root.firstElementChild) {
      return;
    }
    const container = window.document.createElement('div');
    const notificationMessage = window.document.createElement('b');
    notificationMessage.textContent = n.error;
    container.appendChild(notificationMessage);
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
            console.log(data);
            try {
                const response = postEggs(data);
                response.then()
                    .catch(error => {
                        notification.error = error;
                        notification.open();
                        console.log("show notification", error, " notification: ", notification);
                    } );
                console.log(response.status);
                eggCount.value = '';
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

async function postEggs(data) {
    const response = await fetch("http://localhost:19080/diary/entry", {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    return await response.text();
}