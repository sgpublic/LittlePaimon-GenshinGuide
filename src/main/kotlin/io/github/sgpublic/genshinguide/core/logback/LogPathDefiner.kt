package io.github.sgpublic.genshinguide.core.logback

import ch.qos.logback.core.PropertyDefinerBase
import io.github.sgpublic.genshinguide.Config

class LogPathDefiner: PropertyDefinerBase() {
    override fun getPropertyValue(): String {
        return Config.logDir
    }
}