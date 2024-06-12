package io.github.sgpublic.genshinguide.core.logback

import io.github.sgpublic.kotlin.core.logback.filter.ConsoleFilter
import io.github.sgpublic.genshinguide.Config

open class ConsoleFilter: ConsoleFilter(
    debug = Config.debug,
    baseName = "io.github.sgpublic.genshinguide"
)