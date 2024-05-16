package org.openbase.bco.app.preset

import org.openbase.bco.dal.control.layer.unit.app.AbstractAppController
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription
import org.openbase.type.domotic.state.ActivationStateType
import org.openbase.type.domotic.state.ActivationStateType.ActivationState
import org.openbase.type.domotic.unit.UnitConfigType
import org.slf4j.LoggerFactory

/*
* #%L
* BCO App Preset
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
 * A class that can be used as template for new apps.
 */
class TemplateApp : AbstractAppController() {

    private var executing = false

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun applyConfigUpdate(config: UnitConfigType.UnitConfig): UnitConfigType.UnitConfig =
        getManageWriteLockInterruptible(this).use {
            super.applyConfigUpdate(config).also {
                LOGGER.info(getLabel() + " config has been changed.")
            }
        }

    override fun shutdown() = super.shutdown().also {
        LOGGER.info(getLabel() + " is shutting down.")
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun execute(activationState: ActivationStateType.ActivationState): ActionDescription =
        activationState.responsibleAction.also {
            executing = true
            LOGGER.info(getLabel() + " is running.")
        }

    @Throws(InterruptedException::class, CouldNotPerformException::class)
    override fun stop(activationState: ActivationState) =
        super.stop(activationState).also {
            executing = false
            LOGGER.info(getLabel() + " has been stopped.")
        }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TemplateApp::class.java)
    }
}
