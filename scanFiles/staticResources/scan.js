
window.setInterval(function(){
	httpRequest = new XMLHttpRequest();
	httpRequest.open('GET', '/devices/listDevices');
	httpRequest.send();
	httpRequest.onreadystatechange = function(){
		// Process the server response here.
		if (httpRequest.readyState === XMLHttpRequest.DONE) {
			if (httpRequest.status === 200) {
				//alert(httpRequest.responseText);
				let data = JSON.parse(httpRequest.responseText);
				let elementList = document.getElementById("deviceList");
				while(elementList.childElementCount > data.devices.length){
					elementList.removeChild(elementList.lastElementChild);
				}
				while(elementList.childElementCount < data.devices.length){
					let el = document.createElement("a");
					el.classList.add("device");
					
					el.appendChild(document.createElement("img"));
					let nameBox = document.createElement("div");
					nameBox.classList.add("nameBox");
					let devTitle  = document.createElement("span");
					let devStatus = document.createElement("span");
					devTitle .classList.add("device-title" );
					devStatus.classList.add("device-status");
					nameBox.appendChild(devTitle);
					nameBox.appendChild(devStatus);
					el.appendChild(nameBox);

					elementList.appendChild(el);
				}
				for(i=0; i<elementList.childElementCount && i<data.devices.length; i++){
					let el = elementList.children[i];
					let dev =  data.devices[i];

					if(el.children[0].getAttribute("src") != dev.icon)el.children[0].setAttribute("src", dev.icon);
					if(el.children[1].children[0].textContent != dev.name)el.children[1].children[0].textContent = dev.name;

					if(dev.type == "scannerScript"){
						if(el.children[1].children[1].textContent != dev.status.mode)el.children[1].children[1].textContent = dev.status.mode;
					}

					// console.log(data.devices[i]);
				}
			} else {
				//alert('There was a problem with the request.');
			}
		}
	}
}, 1000);


function slash(){
	document.getElementById("slash").textContent = document.getElementById("slash").textContent + "/";
	return false;
}