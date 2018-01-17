package org.openbase.bco.dal.remote.unit.unitgroup;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import java.util.concurrent.*;

import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.bco.dal.lib.layer.unit.unitgroup.UnitGroup;
import org.openbase.bco.dal.remote.service.AbstractServiceRemote;
import org.openbase.bco.dal.remote.service.ServiceRemoteManager;
import org.openbase.bco.dal.remote.unit.AbstractUnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.iface.provider.PingProvider;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.SnapshotType;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
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
import rst.domotic.unit.unitgroup.UnitGroupDataType;
import rst.domotic.unit.unitgroup.UnitGroupDataType.UnitGroupData;
import rst.domotic.unit.unitgroup.UnitGroupDataType.UnitGroupData.Builder;
import rst.vision.ColorType;
import rst.vision.HSBColorType;
import rst.vision.RGBColorType;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupRemote extends AbstractUnitRemote<UnitGroupData> implements UnitGroup {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitGroupDataType.UnitGroupData.getDefaultInstance()));
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
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescription.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SnapshotType.Snapshot.getDefaultInstance()));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitGroupRemote.class);
    private final ServiceRemoteManager<UnitGroupData> serviceRemoteManager;

    public UnitGroupRemote() throws InstantiationException {
        super(UnitGroupData.class);
        this.serviceRemoteManager = new ServiceRemoteManager<UnitGroupData>(this) {
            @Override
            protected Set<ServiceTemplate.ServiceType> getManagedServiceTypes() throws NotAvailableException, InterruptedException {
                return getSupportedServiceTypes();
            }

            @Override
            protected void notifyServiceUpdate(Observable source, Object data) throws NotAvailableException, InterruptedException {
                updateUnitData();
            }
        };
    }

    private UnitGroupData.Builder generateBuilder() throws NotAvailableException {
        try {
            UnitGroupData.Builder dataBuilder = UnitGroupData.newBuilder();
            dataBuilder.setId(getConfig().getId());
            dataBuilder.setLabel(getConfig().getLabel());
            return dataBuilder;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("UnitGroupData.Builder", ex);
        }
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
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
    public void waitForData(long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        // super waitForData is disabled because unit remote is not a RSBRemoteService
        // TODO: Refactor to support UnitRemotes which are not extended RSBRemoteService instances.
        serviceRemoteManager.waitForData(timeout, timeUnit);
        updateUnitData();
    }

    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        // super waitForData is disabled because unit remote is not a RSBRemoteService
        // TODO: Refactor to support UnitRemotes which are not extended RSBRemoteService instances.
        serviceRemoteManager.waitForData();
        updateUnitData();
    }

    private void updateUnitData() throws InterruptedException {
        try {
            applyExternalDataUpdate(serviceRemoteManager.updateBuilderWithAvailableServiceStates(generateBuilder(), getDataClass(), getSupportedServiceTypes()).build());
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
    public Future<ActionFuture> applyAction(final ActionDescription actionDescription) throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.applyAction(actionDescription);
    }

    @Override
    public ServiceRemote getServiceRemote(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType) throws NotAvailableException {
        return serviceRemoteManager.getServiceRemote(serviceType);
    }

    @Override
    public Future<Long> ping() {
        return serviceRemoteManager.ping();
    }

    @Override
    public Long getPing() {
        return serviceRemoteManager.getPing();
    }

    @Override
    public CompletableFuture<UnitGroupData> requestData() {
        return FutureProcessor.toCompletableFuture(() -> {
            Future<Builder> builderFuture = serviceRemoteManager.requestData(generateBuilder());
            try {
                applyExternalDataUpdate(builderFuture.get().build());
                return builderFuture.get().build();
            } catch (Exception ex) {
                throw new CouldNotPerformException("Could not update current status!", ex);
            }
        });
    }
}
