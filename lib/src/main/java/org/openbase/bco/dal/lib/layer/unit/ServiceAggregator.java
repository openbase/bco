package org.openbase.bco.dal.lib.layer.unit;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.bco.dal.lib.layer.service.collection.*;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.SnapshotType;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.*;
import org.openbase.type.domotic.state.BlindStateType.BlindState;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.PowerConsumptionStateType.PowerConsumptionState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.StandbyStateType.StandbyState;
import org.openbase.type.domotic.state.TamperStateType.TamperState;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface ServiceAggregator extends
        BrightnessStateOperationServiceCollection,
        ColorStateOperationServiceCollection,
        PowerStateOperationServiceCollection,
        BlindStateOperationServiceCollection,
        StandbyStateOperationServiceCollection,
        TargetTemperatureStateOperationServiceCollection,
        MotionStateProviderServiceCollection,
        SmokeAlarmStateProviderServiceCollection,
        SmokeStateProviderServiceCollection,
        TemperatureStateProviderServiceCollection,
        PowerConsumptionStateProviderServiceCollection,
        TamperStateProviderServiceCollection,
        IlluminanceStateProviderServiceCollection,
        EmphasisStateOperationServiceCollection {

    ServiceRemote getServiceRemote(final ServiceType serviceType) throws NotAvailableException;

    @RPCMethod
    Future<SnapshotType.Snapshot> recordSnapshot(final UnitType unitType);

    @Override
    default BlindState getBlindState(final UnitType unitType) throws NotAvailableException {
        return ((BlindStateOperationServiceCollection) getServiceRemote(ServiceType.BLIND_STATE_SERVICE)).getBlindState(unitType);
    }

    @Override
    default BrightnessState getBrightnessState(final UnitType unitType) throws NotAvailableException {
        return ((BrightnessStateOperationServiceCollection) getServiceRemote(ServiceType.BRIGHTNESS_STATE_SERVICE)).getBrightnessState(unitType);
    }

    @Override
    default ColorStateType.ColorState getColorState(final UnitType unitType) throws NotAvailableException {
        return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceType.COLOR_STATE_SERVICE)).getColorState(unitType);
    }

    @Override
    default PowerState getPowerState(final UnitType unitType) throws NotAvailableException {
        return ((PowerStateOperationServiceCollection) getServiceRemote(ServiceType.POWER_STATE_SERVICE)).getPowerState(unitType);
    }

    @Override
    default StandbyState getStandbyState(final UnitType unitType) throws NotAvailableException {
        return ((StandbyStateOperationServiceCollection) getServiceRemote(ServiceType.STANDBY_STATE_SERVICE)).getStandbyState(unitType);
    }

    @Override
    default TemperatureState getTargetTemperatureState(final UnitType unitType) throws NotAvailableException {
        return ((TargetTemperatureStateOperationServiceCollection) getServiceRemote(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).getTargetTemperatureState(unitType);
    }

    @Override
    default MotionStateType.MotionState getMotionState(final UnitType unitType) throws NotAvailableException {
        return ((MotionStateProviderServiceCollection) getServiceRemote(ServiceType.MOTION_STATE_SERVICE)).getMotionState(unitType);
    }

    @Override
    default AlarmStateType.AlarmState getSmokeAlarmState(final UnitType unitType) throws NotAvailableException {
        return ((SmokeAlarmStateProviderServiceCollection) getServiceRemote(ServiceType.SMOKE_ALARM_STATE_SERVICE)).getSmokeAlarmState(unitType);
    }

    @Override
    default SmokeStateType.SmokeState getSmokeState(final UnitType unitType) throws NotAvailableException {
        return ((SmokeStateProviderServiceCollection) getServiceRemote(ServiceType.SMOKE_STATE_SERVICE)).getSmokeState(unitType);
    }

    @Override
    default TemperatureState getTemperatureState(UnitType unitType) throws NotAvailableException {
        return ((TemperatureStateProviderServiceCollection) getServiceRemote(ServiceType.TEMPERATURE_STATE_SERVICE)).getTemperatureState(unitType);
    }

    @Override
    default PowerConsumptionState getPowerConsumptionState(UnitType unitType) throws NotAvailableException {
        return ((PowerConsumptionStateProviderServiceCollection) getServiceRemote(ServiceType.POWER_CONSUMPTION_STATE_SERVICE)).getPowerConsumptionState(unitType);
    }

    @Override
    default TamperState getTamperState(final UnitType unitType) throws NotAvailableException {
        return ((TamperStateProviderServiceCollection) getServiceRemote(ServiceType.TAMPER_STATE_SERVICE)).getTamperState(unitType);
    }

    @Override
    default IlluminanceStateType.IlluminanceState getIlluminanceState(final UnitType unitType) throws NotAvailableException {
        return ((IlluminanceStateProviderServiceCollection) getServiceRemote(ServiceType.ILLUMINANCE_STATE_SERVICE)).getIlluminanceState(unitType);
    }

    @Override
    default Future<ActionDescription> setNeutralWhite() {
        try {
            return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceType.COLOR_STATE_SERVICE)).setNeutralWhite();
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    @Override
    default Future<ActionDescription> setNeutralWhite(final ActionParameter actionParameter) {
        try {
            return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceType.COLOR_STATE_SERVICE)).setNeutralWhite(actionParameter);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    @Override
    default EmphasisStateType.EmphasisState getEmphasisState(final UnitType unitType) throws NotAvailableException {
        return ((EmphasisStateOperationServiceCollection) getServiceRemote(ServiceType.EMPHASIS_STATE_SERVICE)).getEmphasisState(unitType);
    }
}
