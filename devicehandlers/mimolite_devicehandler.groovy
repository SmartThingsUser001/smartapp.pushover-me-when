/**
 *  FortrezZ Flow Meter Interface
 *
 *  Copyright 2016 FortrezZ, LLC
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
 *  Based on Todd Wackford's MimoLite Garage Door Opener
 */
metadata {
    // Automatically generated. Make future change here.
    definition (name: "FortrezZ MIMOlite", namespace: "fortrezz", author: "FortrezZ, LLC") {
        capability "Configuration"
        capability "Refresh"
        capability "Contact Sensor"
        capability "Voltage Measurement"
        capability "Button"

        attribute "powered", "string"
        
        fingerprint deviceId: "0x1000", inClusters: "0x72,0x86,0x71,0x30,0x31,0x35,0x70,0x85,0x25,0x03"
    }

    simulator {
    // Simulator stuff
    
    }
    
    preferences {
       input "RelaySwitchDelay", "decimal", title: "Delay between relay switch on and off in seconds. Only Numbers 0 to 3.0 allowed. 0 value will remove delay and allow relay to function as a standard switch", description: "Numbers 0 to 3.1 allowed.", defaultValue: 0, required: false, displayDuringSetup: true
    }


    // UI tile definitions 
    tiles {
        standardTile("button", "device.button", width: 2, height: 2) {
            state "default", label: ".....", action: "pushed", icon: "st.Home.home30", backgroundColor: "#B0E0E6"
            state "pushed", label: "Ding-Dong", icon: "st.Home.home30", backgroundColor: "#53a7c0"
        }
        standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
            state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
            state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        main (["button"])
        details(["button", "configure", "refresh"])
    }
}

def parse(String description) {
    log.debug "description is: ${description}"

    def result = null
    def cmd = zwave.parse(description, [0x20: 1, 0x84: 1, 0x30: 1, 0x70: 1, 0x31: 5])
    
    log.debug "command value is: $cmd.CMD"
    
    if (cmd.CMD == "7105") {                //Mimo sent a power loss report
        log.debug "Device lost power"
    } else {
        log.debug "Device gained power"
        sendEvent(name: "button", value: "pushed", descriptionText: "$device.displayName was pressed", unit : "" )
    }
    //log.debug "${device.currentValue('contact')}" // debug message to make sure the contact tile is working
    if (cmd) {
        result = createEvent(zwaveEvent(cmd))
    }
  //log.debug "Parse returned ${result?.descriptionText} $cmd.CMD"
    return result
}

def updated() {
  //log.debug "Settings Updated..."
    configure()
}
//notes about zwaveEvents:
// these are special overloaded functions which MUST be returned with a map similar to (return [name: "switch", value: "on"])
// not doing so will produce a null on the parse function, this will mess you up in the future.
// Perhaps can use 'createEvent()' and return that as long as a map is inside it.
def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) { 
    //log.debug "switchBinaryReport ${cmd}"
    if (cmd.value) // if the switch is on it will not be 0, so on = true
    {
        return [name: "switch", value: "on"] // change switch value to on
    }
    else // if the switch sensor report says its off then do...
    {
        return [name: "switch", value: "off"] // change switch value to off
    }
       
}

// working on next for the analogue and digital stuff.
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) // basic set is essentially our digital sensor for SIG1
{
    //log.debug "sent a BasicSet command"
    //refresh()  
    delayBetween([zwave.sensorMultilevelV5.sensorMultilevelGet().format()])// requests a report of the anologue input voltage
    return [name: "contact", value: cmd.value ? "open" : "closed"]}
    //[name: "contact", value: cmd.value ? "open" : "closed", type: "digital"]}
    
def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd)
{
    //log.debug "sent a sensorBinaryReport command"
    //refresh()    
    return [name: "sensor", value: cmd.sensorValue ? "active" : "inactive"]
}


    
def zwaveEvent (physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) // sensorMultilevelReport is used to report the value of the analog voltage for SIG1
{
    //log.debug "sent a SensorMultilevelReport"
    def ADCvalue = cmd.scaledSensorValue
    sendEvent(name: "voltageCounts", value: ADCvalue)
   
    CalculateVoltage(cmd.scaledSensorValue)
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    // Handles all Z-Wave commands we aren't interested in
   //log.debug("Un-parsed Z-Wave message ${cmd}")
    [:]
}

def CalculateVoltage(ADCvalue)
{
     def map = [:]
     
     log.debug "RAW ADCvalue: $ADCvalue"
     def volt = 0
     if (ADCvalue < 631) {
        volt = 0
     } else if (ADCvalue < 1179) {
        volt = 0.5
     } else if (ADCvalue < 1687) {
        volt = 1
     } else if (ADCvalue < 2062) {
        volt = 1.5
     } else if (ADCvalue < 2327) {
        volt = 2
     } else if (ADCvalue < 2510) {
        volt = 2.5
     } else if (ADCvalue < 2640) {
        volt = 3
     } else if (ADCvalue < 2741) {
        volt = 3.5
     } else if (ADCvalue < 2823) {
        volt = 4
     } else if (ADCvalue < 2892) {
        volt = 4.5
     } else if (ADCvalue < 2953) {
        volt = 5     
     } else if (ADCvalue < 3004) {
        volt = 5.5     
     } else if (ADCvalue < 3051) {
        volt = 6
     } else if (ADCvalue < 3093) {
        volt = 6.5
     } else if (ADCvalue < 3132) {
        volt = 7     
     } else if (ADCvalue < 3167) {
        volt = 7.5
     } else if (ADCvalue < 3200) {
        volt = 8
     } else if (ADCvalue < 3231) {
        volt = 8.5
     } else if (ADCvalue < 3260) {
        volt = 9
     } else if (ADCvalue < 3286) {
        volt = 9.5
     } else if (ADCvalue < 3336) {
        volt = 10
     } else if (ADCvalue < 3380) {
        volt = 11
     } else if (ADCvalue < 3420) {
        volt = 12
     } else if (ADCvalue < 3458) {
        volt = 13
     } else if (ADCvalue < 3492) {
        volt = 14
     } else if (ADCvalue < 3523) {
        volt = 15
     } else {
        volt = 16
     }
     //def volt = (((1.5338*(10**-16))*(ADCvalue**5)) - ((1.2630*(10**-12))*(ADCvalue**4)) + ((3.8111*(10**-9))*(ADCvalue**3)) - ((4.7739*(10**-6))*(ADCvalue**2)) + ((2.8558*(10**-3))*(ADCvalue)) - (2.2721*(10**-2)))

    //def volt = (((3.19*(10**-16))*(ADCvalue**5)) - ((2.18*(10**-12))*(ADCvalue**4)) + ((5.47*(10**-9))*(ADCvalue**3)) - ((5.68*(10**-6))*(ADCvalue**2)) + (0.0028*ADCvalue) - (0.0293))
    //log.debug "$cmd.scale $cmd.precision $cmd.size $cmd.sensorType $cmd.sensorValue $cmd.scaledSensorValue"
    //def voltResult = volt.round(1)// + "v"
    def voltResult = volt
    
    map.name = "voltage"
    map.value = voltResult
    map.unit = "v"
    return map
}
    

def configure() {
    def x = (RelaySwitchDelay*10).toInteger()
    log.debug "Configuring with delay of $x.... " //setting up to monitor power alarm and actuator duration
    
    delayBetween([
        zwave.associationV1.associationSet(groupingIdentifier:3, nodeId:[zwaveHubNodeId]).format(), //  FYI: Group 3: If a power dropout occurs, the MIMOlite will send an Alarm Command Class report 
                                                                                                    //  (if there is enough available residual power)
        zwave.configurationV1.configurationSet(configurationValue: [x], parameterNumber: 11, size: 1).format() // configurationValue for parameterNumber means how many 100ms do you want the relay
                                                                                                               // to wait before it cycles again / size should just be 1 (for 1 byte.)
    ])
}