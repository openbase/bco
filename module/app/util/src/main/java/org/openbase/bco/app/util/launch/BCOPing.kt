package org.openbase.bco.app.util.launch

import org.openbase.bco.app.util.launch.BCOPing.AnsiColor.*
import org.openbase.bco.app.util.launch.BCOSystemValidator.AnsiColor
import org.openbase.bco.registry.remote.Registries
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote
import org.openbase.jps.core.JPService
import org.openbase.jps.preset.JPVerbose
import org.openbase.jul.communication.jp.JPComHost
import org.openbase.jul.communication.jp.JPComPort
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.ExceptionProcessor
import org.openbase.jul.exception.TimeoutException
import org.openbase.jul.exception.printer.ExceptionPrinter
import java.util.concurrent.TimeUnit.SECONDS

/*-
 * #%L
 * BCO App Utility
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */ /**
 *
 * @author [Divine Threepwood](mailto:divine@openbase.org)
 *
 * A simple command to check if an bco instance is running and if it responds.
 */
object BCOPing {

    val SCOPE_COLOR = ANSI_GREEN
    val RETURN_LIMITER_COLOR = ANSI_GREEN
    val PARAMETER_LIMITER_COLOR = ANSI_RED
    val SUB_HEADLINE = ANSI_CYAN
    val TYPE_LIMITER_COLOR = ANSI_RED
    val UNIT_TYPE_COLOR = SUB_HEADLINE
    val REGISTRY_TYPE_COLOR = SUB_HEADLINE
    fun colorize(text: String): String {
        var text = text
        text = AnsiColor.colorizeRegex(text, "\\/", SCOPE_COLOR)
        text = AnsiColor.colorizeRegex(text, "\\:", RETURN_LIMITER_COLOR)
        text = AnsiColor.colorizeRegex(text, "\\)", PARAMETER_LIMITER_COLOR)
        text = AnsiColor.colorizeRegex(text, "\\(", PARAMETER_LIMITER_COLOR)
        text = AnsiColor.colorizeRegex(text, "\\>", TYPE_LIMITER_COLOR)
        text = AnsiColor.colorizeRegex(text, "\\<", TYPE_LIMITER_COLOR)
        return text
    }

    @JvmStatic
    fun main(args: Array<String>) {
        JPService.setApplicationName("bco-ping")
        JPService.registerProperty(JPVerbose::class.java)
        JPService.registerProperty(JPComHost::class.java)
        JPService.registerProperty(JPComPort::class.java)
        JPService.parseAndExitOnError(args)
        ExceptionPrinter.setBeQuit(true)
        try {
            val ping = Registries.getUnitRegistry().ping()[5, SECONDS]
            if (JPService.verboseMode()) {
                println(
                    "ping is " + BCOSystemValidator.AnsiColor.colorize(
                        BCOSystemValidator.pingFormat.format(
                            ping
                        ), BCOSystemValidator.AnsiColor.ANSI_CYAN
                    ) + " milli"
                )
            }
        } catch (ex: InterruptedException) {
            if (JPService.verboseMode()) {
                println("killed")
            }
            System.exit(253)
            return
        } catch (ex: TimeoutException) {
            if (JPService.verboseMode()) {
                print("no response")
            }
            System.exit(1)
        } catch (ex: Exception) {
            if (JPService.verboseMode()) {
                print("ping failed: ${ExceptionProcessor.getInitialCauseMessage(ex)}")
            }
            System.exit(255)
        }
        System.exit(0)
    }

    enum class AnsiColor(val color: String) {
        // todo: move to jul
        ANSI_RESET("\u001B[0m"), ANSI_BLACK("\u001B[30m"), ANSI_RED("\u001B[31m"), ANSI_GREEN("\u001B[32m"), ANSI_YELLOW(
            "\u001B[33m"
        ),
        ANSI_BLUE("\u001B[34m"), ANSI_PURPLE("\u001B[35m"), ANSI_CYAN("\u001B[36m"), ANSI_WHITE("\u001B[37m");

        companion object {
            fun colorize(text: String, color: AnsiColor): String {
                return color.color + text + ANSI_RESET.color
            }

            fun colorizeRegex(text: String, regex: String, color: AnsiColor): String {
                return text.replace(regex.toRegex(), colorize(regex, color))
            }
        }
    }
}
