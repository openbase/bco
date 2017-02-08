package org.openbase.bco.dal.remote.unit.location;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.bco.dal.lib.layer.unit.location.Location;
import org.openbase.bco.dal.remote.service.ServiceRemoteManager;
import org.openbase.bco.dal.remote.unit.AbstractUnitRemote;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.pattern.Observable;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionConfigType;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.state.AlarmStateType;
import rst.domotic.state.AlarmStateType.AlarmState;
import rst.domotic.state.BlindStateType;
import rst.domotic.state.MotionStateType.MotionState;
import rst.domotic.state.PowerConsumptionStateType.PowerConsumptionState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.BlindStateType.BlindState;
import rst.domotic.state.BrightnessStateType;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.ColorStateType;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.MotionStateType;
import rst.domotic.state.PowerConsumptionStateType;
import rst.domotic.state.PowerStateType;
import rst.domotic.state.PowerStateType.PowerState.State;
import rst.domotic.state.PresenceStateType;
import rst.domotic.state.SmokeStateType;
import rst.domotic.state.SmokeStateType.SmokeState;
import rst.domotic.state.StandbyStateType;
import rst.domotic.state.StandbyStateType.StandbyState;
import rst.domotic.state.TamperStateType;
import rst.domotic.state.TamperStateType.TamperState;
import rst.domotic.state.TemperatureStateType;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.location.LocationDataType;
import rst.domotic.unit.location.LocationDataType.LocationData;
import rst.vision.ColorType;
import rst.vision.HSBColorType;
import rst.vision.RGBColorType;

/*
 * #%L
 * BCO DAL Remote
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
/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class LocationRemote extends AbstractUnitRemote<LocationData> implements Location {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationDataType.LocationData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSBColorType.HSBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorStateType.ColorState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorType.Color.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RGBColorType.RGBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerStateType.PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmStateType.AlarmState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionStateType.MotionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionStateType.PowerConsumptionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BlindStateType.BlindState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SmokeStateType.SmokeState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(StandbyStateType.StandbyState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperStateType.TamperState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessStateType.BrightnessState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureStateType.TemperatureState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PresenceStateType.PresenceState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionConfigType.ActionConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Snapshot.getDefaultInstance()));
    }

    private final ServiceRemoteManager serviceRemoteManager;

    public LocationRemote() {
        super(LocationData.class);
        this.serviceRemoteManager = new ServiceRemoteManager() {
            @Override
            protected Set<ServiceTemplateType.ServiceTemplate.ServiceType> getManagedServiceTypes() throws NotAvailableException, InterruptedException {
                return getSupportedServiceTypes();
            }

            @Override
            protected void notifyServiceUpdate(Observable source, Object data) throws NotAvailableException, InterruptedException {
                // anything needed here?
            }
        };
    }

    @Override
    public String getLabel() throws NotAvailableException {
        try {
            return getConfig().getLabel();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("label", ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        CachedLocationRegistryRemote.waitForData();
        serviceRemoteManager.activate();
        super.activate();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        serviceRemoteManager.deactivate();
        super.deactivate();
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        try {
            return getData().getBrightnessState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Brightness", ex);
        }
    }

    @Override
    public Future<Void> setColorState(ColorState color) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(color, this, Void.class);
    }

    @Override
    public ColorState getColorState() throws NotAvailableException {
        try {
            return getData().getColorState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Color", ex);
        }
    }

    public void setPowerState(final State state) throws CouldNotPerformException {
        LocationRemote.this.setPowerState(PowerState.newBuilder().setValue(state).build());
    }

    @Override
    public Future<Void> setPowerState(final PowerState state) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(state, this, Void.class);
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PowerState", ex);
        }
    }

    @Override
    public Future<Void> setBlindState(BlindState state) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(state, this, Void.class);
    }

    @Override
    public BlindState getBlindState() throws NotAvailableException {
        try {
            return getData().getBlindState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ShutterState", ex);
        }
    }

    @Override
    public Future<Void> setStandbyState(StandbyState state) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(state, this, Void.class);
    }

    @Override
    public StandbyState getStandbyState() throws NotAvailableException {
        try {
            return getData().getStandbyState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("StandbyState", ex);
        }
    }

    @Override
    public Future<Void> setTargetTemperatureState(TemperatureState value) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(value, this, Void.class);
    }

    @Override
    public TemperatureState getTargetTemperatureState() throws NotAvailableException {
        try {
            return getData().getTargetTemperatureState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("TargetTemperature", ex);
        }
    }

    @Override
    public MotionState getMotionState() throws NotAvailableException {
        try {
            return getData().getMotionState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("MotionState", ex);
        }
    }

    @Override
    public AlarmState getSmokeAlarmState() throws NotAvailableException {
        try {
            return getData().getSmokeAlarmState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("AlarmState", ex);
        }
    }

    @Override
    public SmokeState getSmokeState() throws NotAvailableException {
        try {
            return getData().getSmokeState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("SmokeState", ex);
        }
    }

    @Override
    public TemperatureState getTemperatureState() throws NotAvailableException {
        try {
            return getData().getTemperatureState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Temperature", ex);
        }
    }

    @Override
    public PowerConsumptionState getPowerConsumptionState() throws NotAvailableException {
        try {
            return getData().getPowerConsumptionState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PowerConsumptionState", ex);
        }
    }

    @Override
    public TamperState getTamperState() throws NotAvailableException {
        try {
            return getData().getTamperState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("TamperState", ex);
        }
    }

    @Override
    public Future<Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(this, Snapshot.class);
    }

    @Override
    public Future<Void> restoreSnapshot(final Snapshot snapshot) throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(snapshot, this, Void.class);
    }

    @Override
    public Future<Void> applyAction(ActionConfigType.ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(actionConfig, this, Void.class);
    }

    @Override
    public ServiceRemote getServiceRemote(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType) {
        return serviceRemoteManager.getServiceRemote(serviceType);
    }

    @Override
    public List<String> getNeighborLocationIds() throws CouldNotPerformException {
        List<String> neighborIdList = new ArrayList<>();
        try {
            for (UnitConfig locationConfig : CachedLocationRegistryRemote.getRegistry().getNeighborLocations(getId())) {
                neighborIdList.add(locationConfig.getId());
            }
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Could not get CachedLocationRegistryRemote!", ex);
        }
        return neighborIdList;
    }
}
