package org.openbase.bco.dal.lib.layer.unit;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.Set;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.bco.dal.lib.layer.service.collection.BlindStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.BrightnessStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.ColorStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.IlluminanceStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.MotionStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.PowerConsumptionStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.PowerStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.SmokeAlarmStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.SmokeStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.StandbyStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.TamperStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.TargetTemperatureStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.TemperatureStateProviderServiceCollection;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.annotations.RPCMethod;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.SnapshotType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.AlarmStateType;
import rst.domotic.state.BlindStateType.BlindState;
import rst.domotic.state.BrightnessStateType;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.ColorStateType;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.IlluminanceStateType;
import rst.domotic.state.MotionStateType;
import rst.domotic.state.PowerConsumptionStateType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.SmokeStateType;
import rst.domotic.state.StandbyStateType.StandbyState;
import rst.domotic.state.TamperStateType;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface MultiUnitServiceFusion extends BrightnessStateOperationServiceCollection,
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
        IlluminanceStateProviderServiceCollection {

    public Set<ServiceType> getSupportedServiceTypes() throws NotAvailableException, InterruptedException;

    public ServiceRemote getServiceRemote(final ServiceType serviceType) throws NotAvailableException;

    @RPCMethod
    public Future<SnapshotType.Snapshot> recordSnapshot(final UnitType unitType) throws CouldNotPerformException, InterruptedException;

    @Override
    default public Future<ActionFuture> setBlindState(final BlindState blindState, final UnitType unitType) throws CouldNotPerformException {
        return ((BlindStateOperationServiceCollection) getServiceRemote(ServiceType.BLIND_STATE_SERVICE)).setBlindState(blindState, unitType);
    }

    @Override
    default public Future<ActionFuture> setBlindState(final BlindState blindState) throws CouldNotPerformException {
        return ((BlindStateOperationServiceCollection) getServiceRemote(ServiceType.BLIND_STATE_SERVICE)).setBlindState(blindState);
    }

    @Override
    default public BlindState getBlindState() throws NotAvailableException {
        return ((BlindStateOperationServiceCollection) getServiceRemote(ServiceType.BLIND_STATE_SERVICE)).getBlindState();
    }

    @Override
    default public BlindState getBlindState(final UnitType unitType) throws NotAvailableException {
        return ((BlindStateOperationServiceCollection) getServiceRemote(ServiceType.BLIND_STATE_SERVICE)).getBlindState(unitType);
    }

    @Override
    default public Future<ActionFuture> setBrightnessState(final BrightnessState brightnessState) throws CouldNotPerformException {
        return ((BrightnessStateOperationServiceCollection) getServiceRemote(ServiceType.BRIGHTNESS_STATE_SERVICE)).setBrightnessState(brightnessState);
    }

    @Override
    default public Future<ActionFuture> setBrightnessState(final BrightnessState brightnessState, final UnitType unitType) throws CouldNotPerformException {
        return ((BrightnessStateOperationServiceCollection) getServiceRemote(ServiceType.BRIGHTNESS_STATE_SERVICE)).setBrightnessState(brightnessState, unitType);
    }

    @Override
    default public BrightnessStateType.BrightnessState getBrightnessState() throws NotAvailableException {
        return ((BrightnessStateOperationServiceCollection) getServiceRemote(ServiceType.BRIGHTNESS_STATE_SERVICE)).getBrightnessState();
    }

    @Override
    default public BrightnessState getBrightnessState(final UnitType unitType) throws NotAvailableException {
        return ((BrightnessStateOperationServiceCollection) getServiceRemote(ServiceType.BRIGHTNESS_STATE_SERVICE)).getBrightnessState(unitType);
    }

    @Override
    default public Future<ActionFuture> setColorState(final ColorState colorState) throws CouldNotPerformException {
        return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceType.COLOR_STATE_SERVICE)).setColorState(colorState);
    }

    @Override
    default public Future<ActionFuture> setColorState(final ColorState colorState, final UnitType unitType) throws CouldNotPerformException {
        return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceType.COLOR_STATE_SERVICE)).setColorState(colorState, unitType);
    }

    @Override
    default public ColorStateType.ColorState getColorState() throws NotAvailableException {
        return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceType.COLOR_STATE_SERVICE)).getColorState();
    }

    @Override
    default public ColorStateType.ColorState getColorState(final UnitType unitType) throws NotAvailableException {
        return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceType.COLOR_STATE_SERVICE)).getColorState(unitType);
    }

    @Override
    default public Future<ActionFuture> setPowerState(final PowerState powerState) throws CouldNotPerformException {
        return ((PowerStateOperationServiceCollection) getServiceRemote(ServiceType.POWER_STATE_SERVICE)).setPowerState(powerState);
    }

    @Override
    default public Future<ActionFuture> setPowerState(final PowerState powerState, final UnitType unitType) throws CouldNotPerformException {
        return ((PowerStateOperationServiceCollection) getServiceRemote(ServiceType.POWER_STATE_SERVICE)).setPowerState(powerState, unitType);
    }

    @Override
    default public Future<ActionFuture> setPowerState(final PowerState.State state) throws CouldNotPerformException {
        return setPowerState(PowerState.newBuilder().setValue(state).build());
    }

    @Override
    default public Future<ActionFuture> setPowerState(final PowerState.State state, final UnitType unitType) throws CouldNotPerformException {
        return setPowerState(PowerState.newBuilder().setValue(state).build(), unitType);
    }

    @Override
    default public PowerState getPowerState() throws NotAvailableException {
        return ((PowerStateOperationServiceCollection) getServiceRemote(ServiceType.POWER_STATE_SERVICE)).getPowerState();
    }

    @Override
    default public PowerState getPowerState(final UnitType unitType) throws NotAvailableException {
        return ((PowerStateOperationServiceCollection) getServiceRemote(ServiceType.POWER_STATE_SERVICE)).getPowerState(unitType);
    }

    @Override
    default public Future<ActionFuture> setStandbyState(final StandbyState standbyState) throws CouldNotPerformException {
        return ((StandbyStateOperationServiceCollection) getServiceRemote(ServiceType.STANDBY_STATE_SERVICE)).setStandbyState(standbyState);
    }

    @Override
    default public Future<ActionFuture> setStandbyState(final StandbyState state, UnitType unitType) throws CouldNotPerformException {
        return ((StandbyStateOperationServiceCollection) getServiceRemote(ServiceType.STANDBY_STATE_SERVICE)).setStandbyState(state, unitType);
    }

    @Override
    default public StandbyState getStandbyState() throws NotAvailableException {
        return ((StandbyStateOperationServiceCollection) getServiceRemote(ServiceType.STANDBY_STATE_SERVICE)).getStandbyState();
    }

    @Override
    default public StandbyState getStandbyState(final UnitType unitType) throws NotAvailableException {
        return ((StandbyStateOperationServiceCollection) getServiceRemote(ServiceType.STANDBY_STATE_SERVICE)).getStandbyState(unitType);
    }

    @Override
    default public Future<ActionFuture> setTargetTemperatureState(final TemperatureState temperatureState) throws CouldNotPerformException {
        return ((TargetTemperatureStateOperationServiceCollection) getServiceRemote(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).setTargetTemperatureState(temperatureState);
    }

    @Override
    default public Future<ActionFuture> setTargetTemperatureState(final TemperatureState temperatureState, final UnitType unitType) throws CouldNotPerformException {
        return ((TargetTemperatureStateOperationServiceCollection) getServiceRemote(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).setTargetTemperatureState(temperatureState, unitType);
    }

    @Override
    default public TemperatureState getTargetTemperatureState() throws NotAvailableException {
        return ((TargetTemperatureStateOperationServiceCollection) getServiceRemote(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).getTargetTemperatureState();
    }

    @Override
    default public TemperatureState getTargetTemperatureState(final UnitType unitType) throws NotAvailableException {
        return ((TargetTemperatureStateOperationServiceCollection) getServiceRemote(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).getTargetTemperatureState(unitType);
    }

    @Override
    default public MotionStateType.MotionState getMotionState() throws NotAvailableException {
        return ((MotionStateProviderServiceCollection) getServiceRemote(ServiceType.MOTION_STATE_SERVICE)).getMotionState();
    }

    @Override
    default public MotionStateType.MotionState getMotionState(final UnitType unitType) throws NotAvailableException {
        return ((MotionStateProviderServiceCollection) getServiceRemote(ServiceType.MOTION_STATE_SERVICE)).getMotionState(unitType);
    }

    @Override
    default public AlarmStateType.AlarmState getSmokeAlarmState() throws NotAvailableException {
        return ((SmokeAlarmStateProviderServiceCollection) getServiceRemote(ServiceType.SMOKE_ALARM_STATE_SERVICE)).getSmokeAlarmState();
    }

    @Override
    default public AlarmStateType.AlarmState getSmokeAlarmState(final UnitType unitType) throws NotAvailableException {
        return ((SmokeAlarmStateProviderServiceCollection) getServiceRemote(ServiceType.SMOKE_ALARM_STATE_SERVICE)).getSmokeAlarmState(unitType);
    }

    @Override
    default public SmokeStateType.SmokeState getSmokeState() throws NotAvailableException {
        return ((SmokeStateProviderServiceCollection) getServiceRemote(ServiceType.SMOKE_STATE_SERVICE)).getSmokeState();
    }

    @Override
    default public SmokeStateType.SmokeState getSmokeState(final UnitType unitType) throws NotAvailableException {
        return ((SmokeStateProviderServiceCollection) getServiceRemote(ServiceType.SMOKE_STATE_SERVICE)).getSmokeState(unitType);
    }

    @Override
    default public TemperatureState getTemperatureState() throws NotAvailableException {
        return ((TemperatureStateProviderServiceCollection) getServiceRemote(ServiceType.TEMPERATURE_STATE_SERVICE)).getTemperatureState();
    }

    @Override
    default public TemperatureState getTemperatureState(UnitType unitType) throws NotAvailableException {
        return ((TemperatureStateProviderServiceCollection) getServiceRemote(ServiceType.TEMPERATURE_STATE_SERVICE)).getTemperatureState(unitType);
    }

    @Override
    default public PowerConsumptionStateType.PowerConsumptionState getPowerConsumptionState() throws NotAvailableException {
        return ((PowerConsumptionStateProviderServiceCollection) getServiceRemote(ServiceType.POWER_CONSUMPTION_STATE_SERVICE)).getPowerConsumptionState();
    }

    @Override
    default public PowerConsumptionStateType.PowerConsumptionState getPowerConsumptionState(UnitType unitType) throws NotAvailableException {
        return ((PowerConsumptionStateProviderServiceCollection) getServiceRemote(ServiceType.POWER_CONSUMPTION_STATE_SERVICE)).getPowerConsumptionState(unitType);
    }

    @Override
    default public TamperStateType.TamperState getTamperState() throws NotAvailableException {
        return ((TamperStateProviderServiceCollection) getServiceRemote(ServiceType.TAMPER_STATE_SERVICE)).getTamperState();
    }

    @Override
    default public TamperStateType.TamperState getTamperState(final UnitType unitType) throws NotAvailableException {
        return ((TamperStateProviderServiceCollection) getServiceRemote(ServiceType.TAMPER_STATE_SERVICE)).getTamperState(unitType);
    }

    @Override
    public default IlluminanceStateType.IlluminanceState getIlluminanceState() throws NotAvailableException {
        return ((IlluminanceStateProviderServiceCollection) getServiceRemote(ServiceType.ILLUMINANCE_STATE_SERVICE)).getIlluminanceState();
    }

    @Override
    public default IlluminanceStateType.IlluminanceState getIlluminanceState(final UnitType unitType) throws NotAvailableException {
        return ((IlluminanceStateProviderServiceCollection) getServiceRemote(ServiceType.ILLUMINANCE_STATE_SERVICE)).getIlluminanceState(unitType);
    }

    @Override
    default public Future<ActionFuture> setNeutralWhite() throws CouldNotPerformException {
        return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceType.COLOR_STATE_SERVICE)).setNeutralWhite();
    }
}
