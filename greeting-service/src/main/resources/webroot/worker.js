onmessage = function (e) {
  const xhr = new XMLHttpRequest();
  xhr.onload = function () {
        if (xhr.status === 200) {
            postMessage(JSON.parse(xhr.responseText));
        }
        else {
            console.log('Request failed. Returned status of ' + xhr.status);
        }
    };

  const delay = e.data.delay ? "&delay=1000" : "";

  xhr.open('GET', '/breaker/greeting/api/greeting?from=' + e.data.from + delay);
  xhr.send();
};
