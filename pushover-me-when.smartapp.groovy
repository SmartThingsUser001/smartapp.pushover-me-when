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

preferences
{
    section("Devices...") {
        input "switches", "capability.switch", title: "Which Switches?", multiple: true, required: false
        input "motionSensors", "capability.motionSensor", title: "Which Motion Sensors?", multiple: true, required: false
        input "contactSensors", "capability.contactSensor", title: "Which Contact Sensors?", multiple: true, required: false
        input "presenceSensors", "capability.presenceSensor", title: "Which Presence Sensors?", multiple: true, required: false
        input "accelerationSensors", "capability.accelerationSensor", title: "Which Acceleration Sensors?", multiple: true, required: false
        input "locks", "capability.lock", title: "Which Locks?", multiple: true, required: false
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
        input "priority", "enum", title: "Priority", required: true,
        metadata :[
           values: [ 'Normal', 'Low', 'High', 'Emergency' ]
        ]
    }
}

def installed()
{
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated()
{
    log.debug "Updated with settings: ${settings}"
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

    if (switches) {
        // switch.on or switch.off
        subscribe(switches, "switch", handler)
    }
    if (motionSensors) {
        // motion.active or motion.inactive
        subscribe(motionSensors, "motion", handler)
    }
    if (contactSensors) {
        // contact.open or contact.closed
        subscribe(contactSensors, "contact", handler)
    }
    if (presenceSensors) {
        // presence.present or 'presence.not present'  (Why the space? It is dumb.)
        subscribe(presenceSensors, "presence", handler)
    }
    if (accelerationSensors) {
        // acceleration.active or acceleration.inactive
        subscribe(accelerationSensors, "acceleration", handler)
    }
    if (locks) {
        // lock.locked or lock.unlocked
        subscribe(locks, "lock", handler)
    }
}

def handler(evt) {
    log.debug "$evt.displayName is $evt.value"

    if (push == "Yes")
    {
        sendPush("${evt.displayName} is ${evt.value}");
    }

    if (apiKey && userKey)
    {
        log.debug "Sending Pushover with API Key [$apiKey] and User Key [$userKey]"

        // Define the initial postBody keys and values for all messages
        def postBody = [
            token: "$apiKey",
            user: "$userKey",
            message: "${evt.displayName} is ${evt.value}",
            priority: "$pushPriority"
        ]

        // Declare pushPriority as Normal to limit logic as it is the most used type
        def pushPriority = 0

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

        if (deviceName)
        {
            log.debug "Sending Pushover to Device: $deviceName"
            postBody['device'] = "$deviceName"
        }
        else
        {
            log.debug "Sending Pushover to All Devices"
        }

        def params = [
            uri: "https://api.pushover.net/1/messages.json",
            body: postBody
        ]

        log.debug postBody

        httpPost(params){
            response ->
                log.debug "Response Received: Status [$response.status]"

                if(response.status != 200)
                {
                    sendPush("Received HTTP Error Response ${response.status}. Check Install Parameters!")
                }
        }
    }
}
