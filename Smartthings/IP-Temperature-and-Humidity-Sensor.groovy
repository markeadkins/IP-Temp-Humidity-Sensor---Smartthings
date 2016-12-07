/**
 *  IP Temperature and Humidity Sensor
 *
 *  Copyright 2016 Mark Adkins
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "IP Temperature and Humidity Sensor", namespace: "markeadkins", author: "Mark Adkins") {
    capability "Polling"
    capability "Refresh"
    capability "TemperatureMeasurement"
    capability "RelativeHumidityMeasurement"
    
    command "refresh"
	}

    preferences {
    	input("DeviceIP", "string", title:"Device IP Address", description: "Please enter your device's IP Address", required: true, displayDuringSetup: true)
		input("DevicePort", "string", title:"Device Port", description: "Please enter your device's Port", required: true, displayDuringSetup: true)
	}

	simulator {
	}

	tiles {
		valueTile("temperature", "device.temperature", inactiveLabel: false, width: 2, height: 2) {
			state "temperature", label:'${currentValue}°',
				backgroundColors:[
					[value: 31, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
				]
		}
        
       	valueTile("humidity", "device.humidity", inactiveLabel: false, width: 2, height: 2){
        	state "humidity", label:'${currentValue}%',
            	backgroundColors:[
					[value: 31, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
				]
        }
        
		standardTile("refresh", "device.switch", width: 1, height: 1, inactiveLabel: false, canChangeIcon: false, decoration: "flat") {
			state("default", label:'refresh', action:"refresh.refresh", icon:"st.secondary.refresh-icon")
		}
        
        main("humidity")
        
        details(["temperature", "humidity", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"

	def msg = parseLanMessage(description)
    log.debug "The msg is "
    log.debug msg
	def json = msg.json
    log.debug json
    
    log.debug json.object.temp
    log.debug json.object.humidity
    
    sendEvent(name: "temperature", value: json.object.temp as Double)
    sendEvent(name: "humidity", value: json.object.humidity as Double)
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    initialize()
}

def initialize() {
	log.debug "Initialized with settings: ${settings}"
    refresh()
}

def refresh() {
	log.debug "Refresh Triggered"
    runCmd("refresh")
}

def runCmd(String varCommand) {	
    def host = DeviceIP
	def hosthex = convertIPtoHex(host).toUpperCase()
	def porthex = convertPortToHex(DevicePort).toUpperCase()
	device.deviceNetworkId = "$hosthex:$porthex"
    
    log.debug "The device id configured is: $device.deviceNetworkId"
    
    def headers = [:]
	headers.put("HOST", "$host:$DevicePort")
    headers.put("Content-Type", "application/json")
    log.debug headers
    
    try {
		def hubAction = new physicalgraph.device.HubAction(
			method: "GET",
			path: "/json",
			headers: headers
			)
		//hubAction.options = [outputMsgToS3:false]
		log.debug hubAction
		hubAction
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}

def poll() {
	log.debug "Executing 'poll'"
    
	if (device.deviceNetworkId != null) {
		refresh()
	}
	else {
		//sendEvent(name: 'status', value: "error" as String)
		//sendEvent(name: 'network', value: "Not Connected" as String)
		log.debug "DNI: Not set"
	}
}

private String convertIPtoHex(ipAddress) {
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	//log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
	return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
	//log.debug hexport
	return hexport
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	//log.debug("Convert hex to ip: $hex")
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}