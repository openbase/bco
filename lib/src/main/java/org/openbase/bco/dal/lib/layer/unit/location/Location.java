package org.openbase.bco.dal.lib.layer.unit.location;

/*
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.bco.dal.lib.layer.service.collection.BlindStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.BrightnessStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.ColorStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.MotionStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.PowerConsumptionStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.PowerStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.SmokeAlarmStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.SmokeStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.StandbyStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.TamperStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.TargetTemperatureStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.TemperatureStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.PresenceStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.iface.ScopeProvider;
import org.openbase.jul.iface.Configurable;
import org.openbase.jul.iface.Snapshotable;
import org.openbase.jul.iface.provider.LabelProvider;
import org.openbase.jul.pattern.provider.DataProvider;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.service.ServiceConfigType;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.state.AlarmStateType;
import rst.domotic.state.BlindStateType.BlindState;
import rst.domotic.state.BrightnessStateType;
import rst.domotic.state.ColorStateType;
import rst.domotic.state.MotionStateType;
import rst.domotic.state.PowerConsumptionStateType;
import rst.domotic.state.PowerStateType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PowerStateType.PowerState.State;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.state.SmokeStateType;
import rst.domotic.state.StandbyStateType;
import rst.domotic.state.TamperStateType;
import rst.domotic.state.TemperatureStateType;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.location.LocationDataType.LocationData;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface Location extends ScopeProvider, LabelProvider, DataProvider<LocationData>, Configurable<String, UnitConfig>, Unit,
        PresenceStateProviderService, Snapshotable<Snapshot>,
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
        TamperStateProviderServiceCollection {

    /**
     * TODO: Will return controller/remotes in the final implementation. Waiting for a
     * remote pool...
     *
     * @return
     * @throws CouldNotPerformException
     * @deprecated
     */
    @Deprecated
    public List<String> getNeighborLocationIds() throws CouldNotPerformException;

    default public Set<ServiceTemplateType.ServiceTemplate.ServiceType> getSupportedServiceTypes() throws NotAvailableException, InterruptedException {
        final Set<ServiceTemplateType.ServiceTemplate.ServiceType> serviceTypeSet = new HashSet<>();
        try {
            for (final ServiceConfigType.ServiceConfig serviceConfig : getConfig().getServiceConfigList()) {
                serviceTypeSet.add(serviceConfig.getServiceTemplate().getType());
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("SupportedServiceTypes", new CouldNotPerformException("Could not generate supported service type list!", ex));
        }
        return serviceTypeSet;
    }

    public ServiceRemote getServiceRemote(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType);

    @Override
    default public PresenceState getPresenceState() throws NotAvailableException {
        try {
            return getData().getPresenceState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PresenceState", ex);
        }
    }

    @Override
    default public Future<Void> setBlindState(BlindState blindState, UnitTemplate.UnitType unitType) throws CouldNotPerformException {
        return ((BlindStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.BLIND_STATE_SERVICE)).setBlindState(blindState, unitType);
    }

    @Override
    default public Future<Void> setBlindState(final BlindState blindState) throws CouldNotPerformException {
        return ((BlindStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.BLIND_STATE_SERVICE)).setBlindState(blindState);
    }

    @Override
    default public BlindState getBlindState() throws NotAvailableException {
        return ((BlindStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.BLIND_STATE_SERVICE)).getBlindState();
    }

    @Override
    default public BlindState getBlindState(final UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((BlindStateOperationServiceCollection) getServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType.BLIND_STATE_SERVICE)).getBlindState(unitType);
    }

    @Override
    default public Future<Void> setBrightnessState(BrightnessStateType.BrightnessState brightnessState) throws CouldNotPerformException {
        return ((BrightnessStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE)).setBrightnessState(brightnessState);
    }

    @Override
    default public Future<Void> setBrightnessState(BrightnessStateType.BrightnessState brightnessState, UnitTemplate.UnitType unitType) throws CouldNotPerformException {
        return ((BrightnessStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE)).setBrightnessState(brightnessState, unitType);
    }

    @Override
    default public BrightnessStateType.BrightnessState getBrightnessState() throws NotAvailableException {
        return ((BrightnessStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE)).getBrightnessState();
    }

    @Override
    default public BrightnessStateType.BrightnessState getBrightnessState(UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((BrightnessStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE)).getBrightnessState(unitType);
    }

    @Override
    default public Future<Void> setColorState(ColorStateType.ColorState colorState) throws CouldNotPerformException {
        return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.COLOR_STATE_SERVICE)).setColorState(colorState);
    }

    @Override
    default public Future<Void> setColorState(ColorStateType.ColorState colorState, UnitTemplate.UnitType unitType) throws CouldNotPerformException {
        return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.COLOR_STATE_SERVICE)).setColorState(colorState, unitType);
    }

    @Override
    default public ColorStateType.ColorState getColorState() throws NotAvailableException {
        return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.COLOR_STATE_SERVICE)).getColorState();
    }

    @Override
    default public ColorStateType.ColorState getColorState(UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((ColorStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.COLOR_STATE_SERVICE)).getColorState(unitType);
    }

    @Override
    default public Future<Void> setPowerState(PowerStateType.PowerState powerState) throws CouldNotPerformException {
        return ((PowerStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.POWER_STATE_SERVICE)).setPowerState(powerState);
    }

    @Override
    default public Future<Void> setPowerState(PowerStateType.PowerState powerState, UnitTemplate.UnitType unitType) throws CouldNotPerformException {
        return ((PowerStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.POWER_STATE_SERVICE)).setPowerState(powerState, unitType);
    }

    default public void setPowerState(final State state) throws CouldNotPerformException {
        setPowerState(PowerState.newBuilder().setValue(state).build());
    }

    @Override
    default public PowerStateType.PowerState getPowerState() throws NotAvailableException {
        return ((PowerStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.POWER_STATE_SERVICE)).getPowerState();
    }

    @Override
    default public PowerStateType.PowerState getPowerState(UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((PowerStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.POWER_STATE_SERVICE)).getPowerState(unitType);
    }

    @Override
    default public Future<Void> setStandbyState(StandbyStateType.StandbyState standbyState) throws CouldNotPerformException {
        return ((StandbyStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.STANDBY_STATE_SERVICE)).setStandbyState(standbyState);
    }

    @Override
    default public Future<Void> setStandbyState(StandbyStateType.StandbyState state, UnitTemplate.UnitType unitType) throws CouldNotPerformException {
        return ((StandbyStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.STANDBY_STATE_SERVICE)).setStandbyState(state, unitType);
    }

    @Override
    default public StandbyStateType.StandbyState getStandbyState() throws NotAvailableException {
        return ((StandbyStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.STANDBY_STATE_SERVICE)).getStandbyState();
    }

    @Override
    default public StandbyStateType.StandbyState getStandbyState(UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((StandbyStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.STANDBY_STATE_SERVICE)).getStandbyState(unitType);
    }

    @Override
    default public Future<Void> setTargetTemperatureState(TemperatureStateType.TemperatureState temperatureState) throws CouldNotPerformException {
        return ((TargetTemperatureStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).setTargetTemperatureState(temperatureState);
    }

    @Override
    default public Future<Void> setTargetTemperatureState(TemperatureStateType.TemperatureState temperatureState, UnitTemplate.UnitType unitType) throws CouldNotPerformException {
        return ((TargetTemperatureStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).setTargetTemperatureState(temperatureState, unitType);
    }

    @Override
    default public TemperatureStateType.TemperatureState getTargetTemperatureState() throws NotAvailableException {
        return ((TargetTemperatureStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).getTargetTemperatureState();
    }

    @Override
    default public TemperatureStateType.TemperatureState getTargetTemperatureState(UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((TargetTemperatureStateOperationServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).getTargetTemperatureState(unitType);
    }

    @Override
    default public MotionStateType.MotionState getMotionState() throws NotAvailableException {
        return ((MotionStateProviderServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.MOTION_STATE_SERVICE)).getMotionState();
    }

    @Override
    default public MotionStateType.MotionState getMotionState(UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((MotionStateProviderServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.MOTION_STATE_SERVICE)).getMotionState(unitType);
    }

    @Override
    default public AlarmStateType.AlarmState getSmokeAlarmState() throws NotAvailableException {
        return ((SmokeAlarmStateProviderServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.SMOKE_ALARM_STATE_SERVICE)).getSmokeAlarmState();
    }

    @Override
    default public AlarmStateType.AlarmState getSmokeAlarmState(UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((SmokeAlarmStateProviderServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.SMOKE_ALARM_STATE_SERVICE)).getSmokeAlarmState(unitType);
    }

    @Override
    default public SmokeStateType.SmokeState getSmokeState() throws NotAvailableException {
        return ((SmokeStateProviderServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.SMOKE_STATE_SERVICE)).getSmokeState();
    }

    @Override
    default public SmokeStateType.SmokeState getSmokeState(UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((SmokeStateProviderServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.SMOKE_STATE_SERVICE)).getSmokeState(unitType);
    }

    @Override
    default public TemperatureState getTemperatureState() throws NotAvailableException {
        return ((TemperatureStateProviderServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.TEMPERATURE_STATE_SERVICE)).getTemperatureState();
    }

    @Override
    default public TemperatureStateType.TemperatureState getTemperatureState(UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((TemperatureStateProviderServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.TEMPERATURE_STATE_SERVICE)).getTemperatureState(unitType);
    }

    @Override
    default public PowerConsumptionStateType.PowerConsumptionState getPowerConsumptionState() throws NotAvailableException {
        return ((PowerConsumptionStateProviderServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.POWER_CONSUMPTION_STATE_SERVICE)).getPowerConsumptionState();
    }

    @Override
    default public PowerConsumptionStateType.PowerConsumptionState getPowerConsumptionState(UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((PowerConsumptionStateProviderServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.POWER_CONSUMPTION_STATE_SERVICE)).getPowerConsumptionState(unitType);
    }

    @Override
    default public TamperStateType.TamperState getTamperState() throws NotAvailableException {
        return ((TamperStateProviderServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.TAMPER_STATE_SERVICE)).getTamperState();
    }

    @Override
    default public TamperStateType.TamperState getTamperState(UnitTemplate.UnitType unitType) throws NotAvailableException {
        return ((TamperStateProviderServiceCollection) getServiceRemote(ServiceTemplate.ServiceType.TAMPER_STATE_SERVICE)).getTamperState(unitType);
    }
}
