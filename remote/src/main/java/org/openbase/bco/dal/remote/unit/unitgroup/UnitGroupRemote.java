package org.openbase.bco.dal.remote.unit.unitgroup;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.bco.dal.lib.layer.unit.unitgroup.UnitGroup;
import org.openbase.bco.dal.remote.service.AbstractServiceRemote;
import org.openbase.bco.dal.remote.service.ServiceRemoteManager;
import org.openbase.bco.dal.remote.unit.AbstractUnitRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionConfigType;
import rst.domotic.action.SnapshotType;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.state.AlarmStateType;
import rst.domotic.state.BlindStateType;
import rst.domotic.state.BrightnessStateType;
import rst.domotic.state.ColorStateType;
import rst.domotic.state.MotionStateType;
import rst.domotic.state.PowerConsumptionStateType;
import rst.domotic.state.PowerStateType;
import rst.domotic.state.PresenceStateType;
import rst.domotic.state.SmokeStateType;
import rst.domotic.state.StandbyStateType;
import rst.domotic.state.TamperStateType;
import rst.domotic.state.TemperatureStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.location.LocationDataType;
import rst.domotic.unit.unitgroup.UnitGroupDataType.UnitGroupData;
import rst.vision.ColorType;
import rst.vision.HSBColorType;
import rst.vision.RGBColorType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupRemote extends AbstractUnitRemote<UnitGroupData> implements UnitGroup {

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
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SnapshotType.Snapshot.getDefaultInstance()));
    }
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UnitGroupRemote.class);
    private final ServiceRemoteManager serviceRemoteManager;

    public UnitGroupRemote() throws InstantiationException {
        super(UnitGroupData.class);
        this.serviceRemoteManager = new ServiceRemoteManager(this) {
            @Override
            protected Set<ServiceTemplateType.ServiceTemplate.ServiceType> getManagedServiceTypes() throws NotAvailableException, InterruptedException {
                return getSupportedServiceTypes();
            }

            @Override
            protected void notifyServiceUpdate(Observable source, Object data) throws NotAvailableException, InterruptedException {
                updateUnitData();
            }
        };
    }

    @Override
    public void init(final UnitConfig unitGroupUnitConfig) throws InitializationException, InterruptedException {
        try {
            Registries.getUnitRegistry().waitForData();

            if (!unitGroupUnitConfig.hasUnitGroupConfig()) {
                throw new VerificationFailedException("Given unit config does not contain a unit group config!");
            }

            if (unitGroupUnitConfig.getUnitGroupConfig().getMemberIdList().isEmpty()) {
                throw new VerificationFailedException("UnitGroupConfig has no unit members!");
            }

            List<UnitConfig> unitConfigs = new ArrayList<>();
            for (String unitConfigId : unitGroupUnitConfig.getUnitGroupConfig().getMemberIdList()) {
                unitConfigs.add(Registries.getUnitRegistry().getUnitConfigById(unitConfigId));
            }

            if (unitConfigs.isEmpty()) {
                throw new CouldNotPerformException("Could not resolve any unit members!");
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init(unitGroupUnitConfig);
    }
    
    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        UnitConfig unitConfig = super.applyConfigUpdate(config);
        serviceRemoteManager.applyConfigUpdate(unitConfig.getUnitGroupConfig().getMemberIdList());
        return unitConfig;
    }
    

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        serviceRemoteManager.activate();
        updateUnitData();
        // super activation is disabled because unit remote is not a RSBRemoteService
        // TODO: Refactor to support UnitRemotes which are not extended RSBRemoteService instances.
        // super.activate();
    }

    @Override
    public boolean isActive() {
        return serviceRemoteManager.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        serviceRemoteManager.deactivate();
    }

    @Override
    public void waitForData(long timeout, TimeUnit timeUnit) throws NotAvailableException, InterruptedException {
        //todo reimplement with respect to the given timeout.
        try {
            super.waitForData(timeout, timeUnit);
            for (AbstractServiceRemote remote : serviceRemoteManager.getServiceRemoteList()) {
                remote.waitForData(timeout, timeUnit);
            }
            updateUnitData();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ServiceData", ex);
        }
    }

    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        // super waitForData is disabled because unit remote is not a RSBRemoteService
        // TODO: Refactor to support UnitRemotes which are not extended RSBRemoteService instances.
        for (AbstractServiceRemote remote : serviceRemoteManager.getServiceRemoteList()) {
            remote.waitForData();
        }
        updateUnitData();
    }
    
    private void updateUnitData() throws InterruptedException {
        try {
            UnitGroupData.Builder dataBuilder = UnitGroupData.newBuilder();
            dataBuilder.setId(getConfig().getId());
            dataBuilder.setLabel(getConfig().getLabel());
            applyExternalDataUpdate(serviceRemoteManager.updateBuilderWithAvailableServiceStates(dataBuilder, getDataClass(), getSupportedServiceTypes()).build());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update current status!", ex), LOGGER, LogLevel.WARN);
        }
    }
    
    @Override
    public Future<SnapshotType.Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.recordSnapshot();
    }

    @Override
    public Future<SnapshotType.Snapshot> recordSnapshot(final UnitTemplateType.UnitTemplate.UnitType unitType) throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.recordSnapshot(unitType);
    }

    @Override
    public Future<Void> restoreSnapshot(final SnapshotType.Snapshot snapshot) throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.restoreSnapshot(snapshot);
    }

    @Override
    public Future<Void> applyAction(final ActionConfigType.ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.applyAction(actionConfig);
    }
    
    @Override
    public ServiceRemote getServiceRemote(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType) throws NotAvailableException {
        return serviceRemoteManager.getServiceRemote(serviceType);
    }
}
