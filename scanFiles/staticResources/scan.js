


window.setInterval(function(){
	httpRequest = new XMLHttpRequest();
	httpRequest.open('GET', '/some.file');
	httpRequest.send();
	httpRequest.onreadystatechange = function(){
		// Process the server response here.
		if (httpRequest.readyState === XMLHttpRequest.DONE) {
			if (httpRequest.status === 200) {
				//alert(httpRequest.responseText);
				data = JSON.parse(httpRequest.responseText);
				data.devices;

			} else {
				//alert('There was a problem with the request.');
			}
		}
	}
}, 1000);
  
