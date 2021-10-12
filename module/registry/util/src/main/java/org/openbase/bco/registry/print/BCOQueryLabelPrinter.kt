package org.openbase.bco.registry.print

import org.openbase.jps.core.JPService
import org.openbase.bco.registry.remote.Registries
import org.openbase.bco.registry.remote.login.BCOLogin
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.NotAvailableException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.type.processing.LabelProcessor
import org.openbase.jul.extension.type.processing.ScopeProcessor
import org.openbase.jul.processing.StringProcessor
import org.openbase.jul.processing.StringProcessor.Alignment.RIGHT
import java.lang.InterruptedException
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.type.domotic.state.EnablingStateType.EnablingState.State.ENABLED
import java.lang.Exception
import kotlin.system.exitProcess

/*
 * #%L
 * BCO Registry Utility
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
 * @author [Divine Threepwood](mailto:divine@openbase.org)
 */
object BCOQueryLabelPrinter {
    const val APP_NAME = "bco-id-to-label"
    @JvmStatic
    fun main(args: Array<String>) {
        var resultsFound = false
        val query: String = if (args.isEmpty()) {
            ""
        } else {
            args[args.size - 1]
        }
        try {

            // handle jps
            if (args.isNotEmpty()) {
                // remove query from jp args
                val jpArgs = args.copyOfRange(0, args.size - 1)
                JPService.setApplicationName(APP_NAME)

                // help
                if (args.isNotEmpty() && (args[0] == "-h" || args[0] == "--help")) {
                    JPService.printHelp()
                    printHelp()
                    exitProcess(0)
                }
                JPService.parseAndExitOnError(jpArgs)
            }

            // init
            Registries.waitForData()
            BCOLogin.getSession().autoLogin(true).get()
            if (args.isEmpty()) {
                println("At least one id or alias has to be provided!")
                printHelp()
                exitProcess(255)
            }

            // print by id
            try {
                printUnit(Registries.getUnitRegistry().getUnitConfigById(query))
                resultsFound = true
            } catch (ex: NotAvailableException) {
                // continue if not available
            }

            // print by alias
            try {
                printUnit(Registries.getUnitRegistry().getUnitConfigByAlias(query))
                resultsFound = true
            } catch (ex: NotAvailableException) {
                // continue if not available
            }
        } catch (ex: InterruptedException) {
            println("killed")
            exitProcess(253)
        } catch (ex: Exception) {
            ExceptionPrinter.printHistory(CouldNotPerformException("Could not query!", ex), System.err)
            printHelp()
            exitProcess(254)
        }
        if (resultsFound) {
            exitProcess(0)
        } else {
            println(ConsoleColors.CYAN + "No match for " + ConsoleColors.RESET + query + ConsoleColors.CYAN + " found" + ConsoleColors.RESET + " :(")
            printHelp()
        }
        exitProcess(255)
    }

    private fun printHelp() {
        println()
        println("Usage:     " + APP_NAME + " [options] -- [UNIT_ID / UNIT_ALIAS]")
        println()
        println("Example:   \"+APP_NAME+\" ColorableLight-8")
        println("           \"+APP_NAME+\" 844a5b35-4b9c-4db2-9d22-4842db77bc95")
        println("           bco-query -v colorablelight-12")
        println()
        println("Print:     \${LABEL} @ \${LOCATION} \${SCOPE}")
        println()
    }

    @Throws(CouldNotPerformException::class)
    fun printUnit(unitConfig: UnitConfig) {

        // calculate max unit label length
        val maxUnitLabelLength = LabelProcessor.getBestMatch(unitConfig.label).length
        val maxLocationUnitLabelLength = getLocationLabel(unitConfig).length
        val maxScopeLength = ScopeProcessor.generateStringRep(unitConfig.scope).length

        // print
        printUnit(unitConfig, maxUnitLabelLength, maxLocationUnitLabelLength, maxScopeLength)
    }

    @Throws(CouldNotPerformException::class)
    fun printUnit(
        unitConfig: UnitConfig,
        maxUnitLabelLength: Int,
        maxLocationUnitLabelLength: Int,
        maxScopeLength: Int
    ) {
        val prefix: String
        val suffix: String
        if (unitConfig.enablingState.value == ENABLED) {
            prefix = ConsoleColors.GREEN
            suffix = ""
        } else {
            prefix = ConsoleColors.RED
            suffix = " (" + ConsoleColors.YELLOW + "DISABLED" + ConsoleColors.RESET + ")"
        }
        println(
            prefix
                    + StringProcessor.fillWithSpaces(
                LabelProcessor.getBestMatch(unitConfig.label),
                maxUnitLabelLength,
                RIGHT
            )
                    + " @ " + StringProcessor.fillWithSpaces(getLocationLabel(unitConfig), maxLocationUnitLabelLength)
                    + ConsoleColors.RESET
                    + suffix
        )
    }

    private fun getLocationLabel(unitConfig: UnitConfig): String {
        return try {
            LabelProcessor.getBestMatch(
                Registries.getUnitRegistry().getUnitConfigById(unitConfig.placementConfig.locationId).label
            )
        } catch (ex: CouldNotPerformException) {
            "?"
        }
    }

    // TODO: move to jul
    object ConsoleColors {
        // Reset
        const val RESET = "\u001b[0m" // Text Reset

        // Regular Colors
        const val BLACK = "\u001b[0;30m" // BLACK
        const val RED = "\u001b[0;31m" // RED
        const val GREEN = "\u001b[0;32m" // GREEN
        const val YELLOW = "\u001b[0;33m" // YELLOW
        const val BLUE = "\u001b[0;34m" // BLUE
        const val PURPLE = "\u001b[0;35m" // PURPLE
        const val CYAN = "\u001b[0;36m" // CYAN
        const val WHITE = "\u001b[0;37m" // WHITE

        // Bold
        const val BLACK_BOLD = "\u001b[1;30m" // BLACK
        const val RED_BOLD = "\u001b[1;31m" // RED
        const val GREEN_BOLD = "\u001b[1;32m" // GREEN
        const val YELLOW_BOLD = "\u001b[1;33m" // YELLOW
        const val BLUE_BOLD = "\u001b[1;34m" // BLUE
        const val PURPLE_BOLD = "\u001b[1;35m" // PURPLE
        const val CYAN_BOLD = "\u001b[1;36m" // CYAN
        const val WHITE_BOLD = "\u001b[1;37m" // WHITE

        // Underline
        const val BLACK_UNDERLINED = "\u001b[4;30m" // BLACK
        const val RED_UNDERLINED = "\u001b[4;31m" // RED
        const val GREEN_UNDERLINED = "\u001b[4;32m" // GREEN
        const val YELLOW_UNDERLINED = "\u001b[4;33m" // YELLOW
        const val BLUE_UNDERLINED = "\u001b[4;34m" // BLUE
        const val PURPLE_UNDERLINED = "\u001b[4;35m" // PURPLE
        const val CYAN_UNDERLINED = "\u001b[4;36m" // CYAN
        const val WHITE_UNDERLINED = "\u001b[4;37m" // WHITE

        // Background
        const val BLACK_BACKGROUND = "\u001b[40m" // BLACK
        const val RED_BACKGROUND = "\u001b[41m" // RED
        const val GREEN_BACKGROUND = "\u001b[42m" // GREEN
        const val YELLOW_BACKGROUND = "\u001b[43m" // YELLOW
        const val BLUE_BACKGROUND = "\u001b[44m" // BLUE
        const val PURPLE_BACKGROUND = "\u001b[45m" // PURPLE
        const val CYAN_BACKGROUND = "\u001b[46m" // CYAN
        const val WHITE_BACKGROUND = "\u001b[47m" // WHITE

        // High Intensity
        const val BLACK_BRIGHT = "\u001b[0;90m" // BLACK
        const val RED_BRIGHT = "\u001b[0;91m" // RED
        const val GREEN_BRIGHT = "\u001b[0;92m" // GREEN
        const val YELLOW_BRIGHT = "\u001b[0;93m" // YELLOW
        const val BLUE_BRIGHT = "\u001b[0;94m" // BLUE
        const val PURPLE_BRIGHT = "\u001b[0;95m" // PURPLE
        const val CYAN_BRIGHT = "\u001b[0;96m" // CYAN
        const val WHITE_BRIGHT = "\u001b[0;97m" // WHITE

        // Bold High Intensity
        const val BLACK_BOLD_BRIGHT = "\u001b[1;90m" // BLACK
        const val RED_BOLD_BRIGHT = "\u001b[1;91m" // RED
        const val GREEN_BOLD_BRIGHT = "\u001b[1;92m" // GREEN
        const val YELLOW_BOLD_BRIGHT = "\u001b[1;93m" // YELLOW
        const val BLUE_BOLD_BRIGHT = "\u001b[1;94m" // BLUE
        const val PURPLE_BOLD_BRIGHT = "\u001b[1;95m" // PURPLE
        const val CYAN_BOLD_BRIGHT = "\u001b[1;96m" // CYAN
        const val WHITE_BOLD_BRIGHT = "\u001b[1;97m" // WHITE

        // High Intensity backgrounds
        const val BLACK_BACKGROUND_BRIGHT = "\u001b[0;100m" // BLACK
        const val RED_BACKGROUND_BRIGHT = "\u001b[0;101m" // RED
        const val GREEN_BACKGROUND_BRIGHT = "\u001b[0;102m" // GREEN
        const val YELLOW_BACKGROUND_BRIGHT = "\u001b[0;103m" // YELLOW
        const val BLUE_BACKGROUND_BRIGHT = "\u001b[0;104m" // BLUE
        const val PURPLE_BACKGROUND_BRIGHT = "\u001b[0;105m" // PURPLE
        const val CYAN_BACKGROUND_BRIGHT = "\u001b[0;106m" // CYAN
        const val WHITE_BACKGROUND_BRIGHT = "\u001b[0;107m" // WHITE
    }
}
