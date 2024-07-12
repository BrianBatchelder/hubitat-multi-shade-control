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
        }
    }
}

def updated() {
    unsubscribe()
    initialize()
}

def installed() {
    initialize()
}

void initialize() {
    log.debug "------------------------------------------------------------"
    log.info "Shade Control Instance initialize()"
    subscribe(controlShade, "windowShade", "controlWindowShadeHandler")
    subscribe(controlShade, "level", "levelHandler")
    subscribe(controlledShades, "windowShade", "instanceWindowShadeHandler")
    atomicState.initialized = true
    // log.info "shades = " + shades
    for (shade in controlledShades) {
        dumpShade(shade)
        updateShadeState(shade)
    }
}

def dumpShade(shade) {
    String shadeId = "childShade-" + shade.getId()
    // log.debug "id = " + shadeId

    String shadeLabel = shade.getLabel()
    // log.info "label = " + shadeLabel
//    atomicState.updateMapValue(shadeId, "label", shadeLabel)

    String shadeStatus = shade.getStatus()
    if (shadeStatus == "ACTIVE") {
        log.debug "Shade $shadeId ($shadeLabel) is active" 
    } else if (shadeStatus == "INACTIVE") {
        log.debug "Shade $shadeId ($shadeLabel) is inactive"
    } else {
        log.debug "Shade $shadeId ($shadeLabel) status is unknown: " + shadeStatus
    }
 //   atomicState.updateMapValue(shadeId, "status", shadeStatus)
    //log.debug "status = " + shadeStatus

    // log.debug "states = " + shade.getCurrentStates()
    // log.debug "supported attributes = " + shade.getSupportedAttributes()
    if (shade.hasAttribute("windowShade")) {
        //log.debug "has windowShade attribute = " + shade.hasAttribute("windowShade")
        Object windowShade = shade.currentValue("windowShade")
        log.debug "windowShade = " + shade.currentValue("windowShade")
        switch(windowShade) {
            case "open": log.debug "Shade is open"; break
            case "closed": log.debug "Shade is closed"; break
            case "opening": log.debug "Shade is opening"; break
            case "closing": log.debug "Shade is closing"; break
            default: log.debug "Shade is in unknown state"; break
        }
//        if (windowShade == "open") {
//            log.debug "Shade is open"
//        } else if (windowShade == "closed") {
//            log.debug "Shade is closed"
//        } else if (windowShade == "opening") {
//            log.debug "Shade is opening"
 //       } else if (windowShade == "closing") {
  //          log.debug "Shade is closing"
  //      } else {
   //         log.debug "Shade is in unknown state"
    //    }
//        atomicState.updateMapValue(shadeId, "windowShade", windowShade)
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

    log.debug "Updating cached state of shade $shadeId ($shadeLabel) (status is $shadeStatus) to $shadeState"

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
            log.debug "Processing shade $shadeId ($shadeLabel), state = $shadeState"

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
            log.debug "atomicState: skipping key = $key, value = $value"
        }
    }
    
    if (allShadesHaveSameState) {
        log.debug "May update controlShade windowShade state to $allShadesState"
        switch(allShadesState) {
            case "open":
            case "closed": updateControlShadeState(allShadesState); break
            default: log.debug "Not updating controlShade windowShade state to $allShadesState."
        }        
    } else {
        log.debug "Will not update controlShade windowShade state (state is $allShadesState)"
    }
}

def updateControlShadeState(newState) {
    log.debug "Updating controlShade windowShade state to $newState."
    controlShade.sendEvent([ "windowShade":newState ]);
}

def updateAndProcessAllShadeStates() {
    log.info "updateAndProcessAllShadeStates()"
    updateAllShadeStates()
    processShadeStates()
}

def controlWindowShadeHandler(evt) {
    log.info "shade instance controlWindowShadeHandler(): $evt.name:$evt.value - will call on 'opening' shades.open() or on 'closing' shades.close()"
    //log.debug "shade instance controlWindowShadeHandler(): event properties = " + evt.getProperties()
    //log.debug "capability.windowShade getLabel() = " + controlShade.getLabel()
    //log.debug "capability.windowShade windowShade setting = " + controlShade.getSetting("windowShade") // null
    //log.debug "capability.windowShade windowShade state = " + controlShade.currentState("windowShade") // enum
    //log.debug "capability.windowShade windowShade value = " + controlShade.currentValue("windowShade") // closing
    //log.debug "capability.windowShade windowShade = " + controlShade.windowShade // null
    //log.debug "capability.windowShade hasAttribute windowShade = " + controlShade.hasAttribute("windowShade") // true
    //log.debug "capability.windowShade controlShade properties = " + controlShade.getProperties()
    //log.debug "capability.windowShade controlShade states = " + controlShade.getCurrentStates()
    switch(evt.value) { // REVISIT: is processShadeStates enough? Should probably refresh the states manually, right?
        case "opening": controlledShades.open(); runIn(70, updateAndProcessAllShadeStates); break // greater than control shade transition (60)
        case "closing": controlledShades.close(); runIn(70, updateAndProcessAllShadeStates); break
    }
}

def instanceWindowShadeHandler(evt) {
    log.info "shade instance instanceWindowShadeHandler(): $evt.name:$evt.value"
    log.debug "shade instance instanceWindowShadeHandler(): deviceId = " + evt.deviceId
    log.debug "shade instance instanceWindowShadeHandler(): getDeviceId() = " + evt.getDeviceId()
    Object shade = evt.getDevice()
    log.debug "shade instance instanceWindowShadeHandler(): getDevice() = " + shade
    dumpShade(shade)
    updateShadeState(shade)
    processShadeStates()
    // log.debug "shade instance instanceWindowShadeHandler(): event properties = " + evt.getProperties()
}

def levelHandler(evt) {
    log.info "shade instance control shade levelHandler(): $evt.name:$evt.value - setLevel(${evt.value}), setPosition(${evt.value})"
    controlledShades.setLevel((evt.value as Integer))
    controlledShades.setPosition((evt.value as Integer))
}