package org.openbase.bco.registry.print

import org.openbase.bco.registry.print.BCOUnitQueryPrinter.ConsoleColors
import org.openbase.bco.registry.print.BCOUnitQueryPrinter.printUnit
import org.openbase.bco.registry.remote.Registries
import org.openbase.bco.registry.remote.login.BCOLogin
import org.openbase.jps.core.JPService
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.NotAvailableException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.type.processing.LabelProcessor
import org.openbase.jul.extension.type.processing.ScopeProcessor
import org.openbase.jul.processing.StringProcessor
import org.openbase.jul.processing.StringProcessor.Alignment.RIGHT
import org.openbase.type.domotic.state.EnablingStateType.EnablingState.State.ENABLED
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
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
    const val APP_NAME = "bco-query-label"

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
        println("Usage:     $APP_NAME [options] -- [UNIT_ID / UNIT_ALIAS]")
        println()
        println("Example:   $APP_NAME ColorableLight-8")
        println("           $APP_NAME 844a5b35-4b9c-4db2-9d22-4842db77bc95")
        println()
        println("Print:     \${LABEL} @ \${LOCATION}")
        println()
    }

    @Throws(CouldNotPerformException::class)
    fun printUnit(unitConfig: UnitConfig) {

        // calculate max unit label length
        val maxUnitLabelLength = LabelProcessor.getBestMatch(unitConfig.label).length
        val maxLocationUnitLabelLength = getLocationLabel(unitConfig).length

        // print
        printUnit(unitConfig, maxUnitLabelLength, maxLocationUnitLabelLength)
    }

    @Throws(CouldNotPerformException::class)
    fun printUnit(
        unitConfig: UnitConfig,
        maxUnitLabelLength: Int,
        maxLocationUnitLabelLength: Int,
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
        println(prefix
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
}
