package org.openbase.bco.app.preset.agent

import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote
import org.openbase.bco.dal.remote.trigger.GenericBoundedDoubleValueTrigger
import org.openbase.bco.dal.remote.trigger.GenericServiceStateValueTrigger
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.InitializationException
import org.openbase.jul.exception.VerificationFailedException
import org.openbase.jul.pattern.trigger.TriggerPool.TriggerAggregation
import org.openbase.type.domotic.service.ServiceTemplateType
import org.openbase.type.domotic.state.PresenceStateType
import org.openbase.type.domotic.unit.UnitConfigType
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData

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
 * @author [Timo Michalski](mailto:tmichalski@techfak.uni-bielefeld.de)
 */
class PresenceLightSceneAgent : AbstractSceneAgent() {

    @Throws(InitializationException::class, InterruptedException::class)
    override fun init(config: UnitConfigType.UnitConfig) {
        super.init(config)
        try {
            val locationRemote = Units.getUnit(getConfig().placementConfig.locationId, false, Units.LOCATION)
                ?: throw VerificationFailedException("Configured target location not found!")

            // activation trigger
            registerActivationTrigger(
                GenericServiceStateValueTrigger<LocationRemote, LocationData, PresenceStateType.PresenceState.State>(
                    locationRemote,
                    PresenceStateType.PresenceState.State.PRESENT,
                    ServiceTemplateType.ServiceTemplate.ServiceType.PRESENCE_STATE_SERVICE
                ), TriggerAggregation.AND
            )
            registerActivationTrigger(
                GenericBoundedDoubleValueTrigger<LocationRemote, LocationData>(
                    locationRemote,
                    MIN_ILLUMINANCE_UNTIL_TRIGGER,
                    GenericBoundedDoubleValueTrigger.TriggerOperation.LOW_ACTIVE,
                    ServiceTemplateType.ServiceTemplate.ServiceType.ILLUMINANCE_STATE_SERVICE,
                    "getIlluminance"
                ), TriggerAggregation.AND
            )

            // deactivation trigger
            registerDeactivationTrigger(
                GenericServiceStateValueTrigger<LocationRemote, LocationData, PresenceStateType.PresenceState.State>(
                    locationRemote,
                    PresenceStateType.PresenceState.State.ABSENT,
                    ServiceTemplateType.ServiceTemplate.ServiceType.PRESENCE_STATE_SERVICE
                ), TriggerAggregation.OR
            )
        } catch (ex: CouldNotPerformException) {
            throw InitializationException(this, ex)
        }
    }

    companion object {
        const val MIN_ILLUMINANCE_UNTIL_TRIGGER = 100.0
    }
}
