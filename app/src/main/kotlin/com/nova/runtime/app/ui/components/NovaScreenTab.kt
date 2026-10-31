package com.nova.runtime.app.ui.components

/**
 * 5 Core Destinations for NOVA Mobile OS:
 * SYSTEM, INDEX, ACTION, COMMAND, CONFIG.
 */
enum class NovaScreenTab(val displayName: String) {
    DASHBOARD("Home"),
    DOCUMENTS("Files"),
    ACTIONS("Actions"),
    COMMAND("Ask"),
    SETTINGS("Settings");

    companion object {
        val SYSTEM = DASHBOARD
        val INDEX = DOCUMENTS
        val ACTION = ACTIONS
        val CONFIG = SETTINGS
    }
}
