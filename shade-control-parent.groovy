/* Shade Controls
*

Copyright 2021 Hubitat Inc. All Rights Reserved
*/
definition(
name: "Shade Controls",
singleInstance: true,
namespace: "hubitat",
author: "Bruce Ravenel",
description: "Create shade automations",
category: "Convenience",
iconUrl: "",
iconX2Url: "",
installOnOpen: true
)

preferences {
    page(name: "mainPage")
    page(name: "removePage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: " ", install: true, uninstall: false) {
        section {
            app(name: "childShade", appName: "Shade Control Instance", namespace: "hubitat", title: "Create New Shade Control", multiple: true, displayChildApps: true)
            paragraph " "
            href "removePage", title: "Remove Shade Controls", description: ""
        }
    }
}

def removePage() {
    dynamicPage(name: "removePage", title: "Remove all Shade Controls", install: false, uninstall: true) {
        section ("WARNING!\n\nRemoving removes all Shade Controls\n") {
        }
    }
}

def installed() {
}

def updated() {
}