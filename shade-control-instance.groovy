// adapted from code written by Bruce Ravenel (https://community.hubitat.com/t/help-combining-three-shades-as-one/57028/23)
definition(
name: "Shade Control Instance",
parent: "hubitat:Shade Controls",
namespace: "biz.briansbrain",
author: "Brian Batchelder",
description: "Multi-shade control",
category: "Convenience",
iconUrl: "",
iconX2Url: ""
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Multi-Shade Controller", uninstall: true, install: true) {
        section {
            input "origLabel", "text", title: "Name this Shade Control", submitOnChange: true
            if(origLabel) app.updateLabel(origLabel)
            input "controlShade", "capability.windowShade", title: "Select control shade", submitOnChange: true
            input "controlledShades", "capability.windowShade", title: "Select shades to control", submitOnChange: true, multiple: true
		    input name: "debugOutput", type: "bool", title: "Enable debug logging?", defaultValue: false
        }
    }
}

def updated() {
    unsubscribe()
    initialize()
    if (debugOutput) runIn(1800, disableDebugLogging)
}

def installed() {
    initialize()
}

void initialize() {
    debugLog "------------------------------------------------------------"
    log.info "Shade Control Instance initialize()"
    subscribe(controlShade, "windowShade", "controlWindowShadeHandler")
    subscribe(controlShade, "level", "levelHandler")
    subscribe(controlledShades, "windowShade", "instanceWindowShadeHandler")
    atomicState.initialized = true
    debugLog "shades = " + shades
    for (shade in controlledShades) {
        dumpShade(shade)
        updateShadeState(shade)
    }
}

def dumpShade(shade) {
    if (!debugOutput) return

    String shadeId = "childShade-" + shade.getId()
    debugLog "id = " + shadeId

    String shadeLabel = shade.getLabel()
    debugLog "label = " + shadeLabel

    String shadeStatus = shade.getStatus()
    if (shadeStatus == "ACTIVE") {
        debugLog "Shade $shadeId ($shadeLabel) is active" 
    } else if (shadeStatus == "INACTIVE") {
        debugLog "Shade $shadeId ($shadeLabel) is inactive"
    } else {
        debugLog "Shade $shadeId ($shadeLabel) status is unknown: " + shadeStatus
    }
    debugLog "status = " + shadeStatus

    debugLog "states = " + shade.getCurrentStates()
    debugLog "supported attributes = " + shade.getSupportedAttributes()
    if (shade.hasAttribute("windowShade")) {
        debugLog "has windowShade attribute = " + shade.hasAttribute("windowShade")
        Object windowShade = shade.currentValue("windowShade")
        debugLog "windowShade = " + shade.currentValue("windowShade")
        switch(windowShade) {
            case "open": debugLog "Shade is open"; break
            case "closed": debugLog "Shade is closed"; break
            case "opening": debugLog "Shade is opening"; break
            case "closing": debugLog "Shade is closing"; break
            default: debugLog "Shade is in unknown state"; break
        }
       if (windowShade == "open") {
           debugLog "Shade is open"
       } else if (windowShade == "closed") {
           debugLog "Shade is closed"
       } else if (windowShade == "opening") {
           debugLog "Shade is opening"
       } else if (windowShade == "closing") {
           debugLog "Shade is closing"
       } else {
           debugLog "Shade is in unknown state"
       }
    }
}

def updateShadeState(shade) {
    String shadeId = "childShade-" + shade.getId()
    String shadeLabel = shade.getLabel()
    String shadeStatus = shade.getStatus()
    Object shadeState = null
    if (shade.hasAttribute("windowShade")) {
        shadeState = shade.currentValue("windowShade")
    }

    debugLog "Updating cached state of shade $shadeId ($shadeLabel) (status is $shadeStatus) to $shadeState"

    atomicState.updateMapValue(shadeId, "label", shadeLabel)
    atomicState.updateMapValue(shadeId, "status", shadeStatus)
    atomicState.updateMapValue(shadeId, "windowShade", shadeState)
}

def updateAllShadeStates() {
    for (shade in controlledShades) {
        dumpShade(shade)
        updateShadeState(shade)
    }
}

def processShadeStates() {
    String allShadesState = "uninitialized"
    Boolean allShadesHaveSameState = true
    atomicState.each { key, value ->
        if (key.startsWith("childShade-")) {
            String shadeId = key
            String shadeLabel = value.label        
            String shadeStatus = value.status
            Object shadeState = value.windowShade
            debugLog "Processing shade $shadeId ($shadeLabel), state = $shadeState"

            if (shadeStatus == "ACTIVE") {
                if (allShadesState == "uninitialized") {
                    allShadesState = shadeState
                } 
                if (allShadesHaveSameState && (allShadesState != shadeState)) {
                    allShadesHaveSameState = false
                    allShadesState = "inconsistent"
                }
            }
        } else {
            debugLog "atomicState: skipping key = $key, value = $value"
        }
    }
    
    if (allShadesHaveSameState) {
        debugLog "May update controlShade windowShade state to $allShadesState"
        switch(allShadesState) {
            case "open":
            case "closed": updateControlShadeState(allShadesState); break
            default: debugLog "Not updating controlShade windowShade state to $allShadesState."
        }        
    } else {
        debugLog "Will not update controlShade windowShade state (state is $allShadesState)"
    }
}

def updateControlShadeState(newState) {
    debugLog "Updating controlShade windowShade state to $newState."
    controlShade.sendEvent([ "windowShade":newState ]);
}

def updateAndProcessAllShadeStates() {
    debugLog "updateAndProcessAllShadeStates()"
    updateAllShadeStates()
    processShadeStates()
}

def controlWindowShadeHandler(evt) {
    debugLog "shade instance controlWindowShadeHandler(): $evt.name:$evt.value - will call on 'opening' shades.open() or on 'closing' shades.close()"
    switch(evt.value) {
        case "opening": controlledShades.open(); runIn(70, updateAndProcessAllShadeStates); log.info "Opening shades"; break // greater than control shade transition (60)
        case "closing": controlledShades.close(); runIn(70, updateAndProcessAllShadeStates); log.info "Closing shades"; break
    }
}

def instanceWindowShadeHandler(evt) {
    debugLog "shade instance instanceWindowShadeHandler(): $evt.name:$evt.value"
    debugLog "shade instance instanceWindowShadeHandler(): deviceId = " + evt.deviceId
    debugLog "shade instance instanceWindowShadeHandler(): getDeviceId() = " + evt.getDeviceId()
    Object shade = evt.getDevice()
    debugLog "shade instance instanceWindowShadeHandler(): getDevice() = " + shade
    dumpShade(shade)
    updateShadeState(shade)
    processShadeStates()
    // debugLog "shade instance instanceWindowShadeHandler(): event properties = " + evt.getProperties()
}

def levelHandler(evt) {
    debugLog "shade instance control shade levelHandler(): $evt.name:$evt.value - setLevel(${evt.value}), setPosition(${evt.value})"
    controlledShades.setLevel((evt.value as Integer))
    controlledShades.setPosition((evt.value as Integer))
}

def debugLog(message) {
    if (debugOutput) debugLog message
}

def disableDebugLogging(){
	log.warn "debug logging disabled..."
	device.updateSetting("debugOutput",[value:"false",type:"bool"])
}
