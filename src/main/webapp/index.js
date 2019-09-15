import '@vaadin/vaadin-text-field';
import '@vaadin/vaadin-button';

window.addEventListener('load', () => {
     initUI();
});

function initUI() {
  const eggCount = document.querySelector('#eggs');
  const addButton = document.querySelector('#post');

  addButton.addEventListener('click', e => {
    console.log('You have reported ' + eggCount.value + ' eggs!');
    eggCount.value = '';
  });
}