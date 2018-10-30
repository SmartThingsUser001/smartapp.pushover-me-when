/**
 *  Pushover Me When
 *
 *  Author: jnovack@gmail.com
 *  Date: 2013-08-22
 *  Code: https://github.com/smartthings-users/smartapp.pushover-me-when
 *
 * Copyright (C) 2013 Justin J. Novack <jnovack@gmail.com>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following
 * conditions: The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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

preferences
{
    section("Devices...") {
        input "voltageSensors", "capability.voltageMeasurement", title: "Which Voltage Sensors?", multiple: true, required: false
    }
    section("Application...") {
        input "push", "enum", title: "SmartThings App Notification?", required: true, multiple: false,
        metadata :[
           values: [ 'No', 'Yes' ]
        ]
    }
    section("Sensor Thresholds...") {
        input "thresholdValue", "text",
            title: "Threshold Value",
            required: true
        paragraph "The string value entered here will be parsed and used internally to filter device triggers"
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
}

def installed()
{
    log.debug "'Pushover Me When' installed with settings: ${settings}"
    initialize()
}

def updated()
{
    log.debug "'Pushover Me When' updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize()
{
    /**
     * You can customize each of these to only receive one type of notification
     * by subscribing only to the individual event for each type. Additional
     * logic would be required in the Preferences section and the device handler.
     */
    log.debug "Initializing"

    if (voltageSensors) {
        // voltage value
        subscribe(voltageSensors, "voltage", handler)
    }
}

def handler(evt) {
    log.debug "EVENT: $evt.name is $evt.value"

    if (evt.name == "voltage") {
        def doubleThreshold = Double.parseDouble(thresholdValue)
        def observed = evt.numberValue
        if (observed < doubleThreshold) {
            log.debug "Not sending event: ${observed} < ${doubleThreshold}"
            return
        }
    } else {
        return
    }

    log.debug "Sending event! ${observed} >= ${doubleThreshold}"

    def postMessage = pushoverMessage
    if (!postMessage) {
        postMessage = "${evt.displayName} is ${evt.value}"
    }

    if (push == "Yes")
    {
        sendPush(postMessage);
    }


    // Define the initial postBody keys and values for all messages
    def postBody = [
        token: "$apiKey",
        user: "$userKey",
        message: postMessage,
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
    if (deviceName)
    {
        log.debug "Sending Pushover to Device: $deviceName"
        postBody['device'] = "$deviceName"
    }
    else
    {
        log.debug "Sending Pushover to All Devices"
    }

    // Prepare the package to be sent
    def params = [
        uri: "https://api.pushover.net/1/messages.json",
        body: postBody
    ]

    log.debug postBody

    if ((apiKey =~ /[A-Za-z0-9]{30}/) && (userKey =~ /[A-Za-z0-9]{30}/))
    {
        log.debug "Sending Pushover: API key '${apiKey}' | User key '${userKey}'"
        httpPost(params){
            response ->
                if(response.status != 200)
                {
                    sendPush("ERROR: 'Pushover Me When' received HTTP error ${response.status}. Check your keys!")
                    log.error "Received HTTP error ${response.status}. Check your keys!"
                }
                else
                {
                    log.debug "HTTP response received [$response.status]"
                }
        }
    }
    else {
        // Do not sendPush() here, the user may have intentionally set up bad keys for testing.
        log.error "API key '${apiKey}' or User key '${userKey}' is not properly formatted!"
    }
}
