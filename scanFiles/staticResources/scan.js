
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
					el.classList.add("unselected");
					
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
					let el = elementList.children[i]; //element on page for device
					let dev =  data.devices[i];       //device info from server

					if(el.children[0].getAttribute("src") != dev.icon)el.children[0].setAttribute("src", dev.icon);
					if(el.children[1].children[0].textContent != dev.name)el.children[1].children[0].textContent = dev.name;
					
					const deviceNumber = i; //Makes it specific to the function
					const deviceType = dev.type;

					// if(dev.type == "scannerScript"){
						if(el.children[1].children[1].textContent != dev.status.mode)el.children[1].children[1].textContent = dev.status.mode;

						el.onclick = function(){
							let deviceElements = document.getElementsByClassName("device");

							window.selectedDevice = deviceNumber;

							for(i=0; i<deviceElements.length; i++){
								var element = deviceElements[i];
								if(element.classList.contains("selected")){
									element.classList.remove("selected");
									element.classList.add("unselected");
								}
							}
							el.classList.remove("unselected");
							el.classList.add("selected");

							let mainBody = document.getElementById("mainBodySection")
							for(j=0; j<mainBody.children.length; j++){
								if(!mainBody.children[j].classList.contains("hidden"))mainBody.children[j].classList.add("hidden");
							}
							let deviceMainbodyDiv = document.getElementById(deviceType);
							deviceMainbodyDiv.classList.remove("hidden");
							
							
							//clear old stuff
							/*
							let mainBody = document.getElementById("mainBodySection");
							while(mainBody.firstChild)mainBody.removeChild(mainBody.firstChild);

							//set main body stuff for scanner

							let title = document.createElement("h2");
							title.textContent = dev.name;
							title.classList.add("h2-title");
							mainBody.appendChild(title);
							*/

						}
					// }

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