definition(
    name: "Shade Control Instance",
    parent: "hubitat:Shade Controls",
    namespace: "hubitat",
    author: "Bruce Ravenel",
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
            input "master", "capability.windowShade", title: "Select control shade", submitOnChange: true
            input "shades", "capability.windowShade", title: "Select shades to control", submitOnChange: true, multiple: true
        }
    }
}

def updated() {
    unsubscribe()
    initialize()
}

def installed() {
initiali    ze()
}

void initialize() {
    subscribe(master, "windowShade", "handler")
    subscribe(master, "level", "phandler")
}

def handler(evt) {
    log.info "$evt.name:$evt.value"
    switch(evt.value) {
        case "opening": shades.open(); break
        case "closing": shades.close(); break
    }
}

def phandler(evt) {
    log.info "$evt.name:$evt.value"
    shades.setLevel((evt.value as Integer))
    shades.setPosition((evt.value as Integer))
}