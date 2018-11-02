/**
 *  Notify Me When
 *
 *  Author: SmartThings
 *  Date: 2013-03-20
 *
 * Change Log:
 *  1. Todd Wackford
 *  2014-10-03: Added capability.button device picker and button.pushed event subscription. For Doorbell.
 */
definition(
    name: "Pushover Me When",
    namespace: "smartthingsuser001",
    author: "SmartThingsUser001",
    description: "Allows you to subscribe to device notifcations and forward them to pushover",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/SmartThingsUser001/smartapp.pushover-me-when/master/images/doorbell.png",
    iconX2Url: "https://raw.githubusercontent.com/SmartThingsUser001/smartapp.pushover-me-when/master/images/doorbell@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/SmartThingsUser001/smartapp.pushover-me-when/master/images/doorbell@3x.png")


preferences {
    section("Choose one or more, when..."){
        input "button", "capability.button", title: "Button Pushed", required: false, multiple: true //tw
        input "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
        input "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
        input "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
        input "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
        input "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
        input "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
        input "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
        input "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
        input "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
        input "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
    }
    section("Application...") {
        input "push", "enum", title: "SmartThings App Notification?", required: true, multiple: false,
        metadata :[
            values: [ 'No', 'Yes' ]
        ]
    }
    section("Pushover...") {
        input "apiKey", "text", title: "API Key", required: true
        input "userKey", "text", title: "User Key", required: true
        input "deviceName", "text", title: "Device Name (blank for all)", required: false
        input "pushoverMessage", "text", title: "Custom Message", required: false
        input "priority", "enum", title: "Priority", required: true,
        metadata :[
           values: [ 'Normal', 'Low', 'High', 'Emergency' ]
        ]
    }
    section("Minimum time between messages (optional, defaults to every message)") {
        input "frequency", "decimal", title: "Seconds", required: false
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribeToEvents()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    subscribeToEvents()
}

def subscribeToEvents() {
    subscribe(button, "button.pushed", eventHandler) //tw
    subscribe(contact, "contact.open", eventHandler)
    subscribe(contactClosed, "contact.closed", eventHandler)
    subscribe(acceleration, "acceleration.active", eventHandler)
    subscribe(motion, "motion.active", eventHandler)
    subscribe(mySwitch, "switch.on", eventHandler)
    subscribe(mySwitchOff, "switch.off", eventHandler)
    subscribe(arrivalPresence, "presence.present", eventHandler)
    subscribe(departurePresence, "presence.not present", eventHandler)
    subscribe(smoke, "smoke.detected", eventHandler)
    subscribe(smoke, "smoke.tested", eventHandler)
    subscribe(smoke, "carbonMonoxide.detected", eventHandler)
    subscribe(water, "water.wet", eventHandler)
}

def eventHandler(evt) {
    log.debug "Notify got evt ${evt}"
    if (frequency) {
        log.debug "Frequency defined at $frequency"
        def lastTime = state[evt.deviceId]
        log.debug "Last time was $lastTime"
        if (lastTime) {
            def timeSince = now() - lastTime
            def delay = frequency * 1000
            log.debug "Time since: $timeSince <-> $delay"
        }
        if (lastTime == null || timeSince >= delay) {
            sendMessage(evt)
        }
    } else {
        sendMessage(evt)
    }
}

private sendMessage(evt) {
    def msg = pushoverMessage ?: defaultText(evt)
    if (push == 1 || push == "1" || push == "Yes") {
        log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

        log.debug "sending push"
        sendPush(msg)
    }

    // Define the initial postBody keys and values for all messages
    def postBody = [
        token: "$apiKey",
        user: "$userKey",
        message: msg,
        priority: 0
    ]

    // Set priority and potential postBody variables based on the user preferences
    switch ( priority ) {
        case "Low":
            postBody['priority'] = -1
            break
        case "High":
            postBody['priority'] = 1
            break
        case "Emergency":
            postBody['priority'] = 2
            postBody['retry'] = "60"
            postBody['expire'] = "3600"
            break
    }

    // We only have to define the device if we are sending to a single device
    if (deviceName) {
        log.debug "Sending Pushover to Device: $deviceName"
        postBody['device'] = "$deviceName"
    } else {
        log.debug "Sending Pushover to All Devices"
    }

    // Prepare the package to be sent
    def params = [
        uri: "https://api.pushover.net/1/messages.json",
        body: postBody
    ]

    log.debug postBody

    if ((apiKey =~ /[A-Za-z0-9]{30}/) && (userKey =~ /[A-Za-z0-9]{30}/)) {
        log.debug "Sending Pushover: API key '${apiKey}' | User key '${userKey}'"
        httpPost(params) {
            response ->
                if(response.status != 200) {
                    sendPush("ERROR: 'Pushover Me When' received HTTP error ${response.status}. Check your keys!")
                    log.error "Received HTTP error ${response.status}. Check your keys!"
                } else {
                    log.debug "HTTP response received [$response.status]"
                }
        }
    }

    if (frequency) {
        state[evt.deviceId] = now()
    }

}

private defaultText(evt) {
    if (evt.name == "presence") {
        if (evt.value == "present") {
            if (includeArticle) {
                "$evt.linkText has arrived at the $location.name"
            }
            else {
                "$evt.linkText has arrived at $location.name"
            }
        }
        else {
            if (includeArticle) {
                "$evt.linkText has left the $location.name"
            }
            else {
                "$evt.linkText has left $location.name"
            }
        }
    }
    else {
        evt.descriptionText
    }
}

private getIncludeArticle() {
    def name = location.name.toLowerCase()
    def segs = name.split(" ")
    !(["work","home"].contains(name) || (segs.size() > 1 && (["the","my","a","an"].contains(segs[0]) || segs[0].endsWith("'s"))))
}