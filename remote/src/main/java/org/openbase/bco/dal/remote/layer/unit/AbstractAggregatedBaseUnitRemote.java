package org.openbase.bco.dal.remote.layer.unit;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.bco.dal.lib.layer.unit.MultiUnit;
import org.openbase.bco.dal.lib.layer.unit.UnitProcessor;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.service.ServiceRemoteFactoryImpl;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.communication.controller.RPCHelper;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.SnapshotType;
import org.openbase.type.domotic.action.SnapshotType.Snapshot;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractAggregatedBaseUnitRemote<D extends Message> extends AbstractUnitRemote<D> implements MultiUnit<D> {

    private final Map<ServiceType, List<UnitConfig>> serviceTypeUnitMap;

    public AbstractAggregatedBaseUnitRemote(final Class<D> dataClass) {
        super(dataClass);
        this.serviceTypeUnitMap = new HashMap<>();
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        UnitConfig unitConfig = super.applyConfigUpdate(config);

        serviceTypeUnitMap.clear();
        for (final UnitConfig aggregatedUnitConfig : getAggregatedUnitConfigList()) {
            for (final ServiceDescription serviceDescription : Registries.getTemplateRegistry().getUnitTemplateByType(aggregatedUnitConfig.getUnitType()).getServiceDescriptionList()) {
                if (!serviceTypeUnitMap.containsKey(serviceDescription.getServiceType())) {
                    serviceTypeUnitMap.put(serviceDescription.getServiceType(), new ArrayList<>());
                }
                serviceTypeUnitMap.get(serviceDescription.getServiceType()).add(aggregatedUnitConfig);
            }
        }

        return unitConfig;
    }

    @Override
    public boolean isServiceAvailable(ServiceType serviceType) {
        try {
            return serviceTypeUnitMap.containsKey(serviceType) || !isServiceAggregated(serviceType);
        } catch (NotAvailableException e) {
            return false;
        }
    }

    @Override
    public ServiceRemote getServiceRemote(final ServiceType serviceType) throws NotAvailableException {
        if (!isServiceAvailable(serviceType)) {
            throw new NotAvailableException("ServiceRemote for serviceType[" + serviceType.name() + "] in not available for " + this);
        }
        try {
            return ServiceRemoteFactoryImpl.getInstance().newInitializedInstance(serviceType, serviceTypeUnitMap.get(serviceType), true);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ServiceRemote of serviceType[" + serviceType.name() + "] in not available for " + this, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new NotAvailableException("ServiceRemote of serviceType[" + serviceType.name() + "] in not available for " + this, ex);
        }
    }

    /**
     * Apply an authenticated action on the controller. This only works with the default session manager because the
     * service type has to be extracted from the action description in the authenticated value. This method internally
     * calls {@link #applyActionAuthenticated(AuthenticatedValue, ServiceType)}.
     *
     * @param authenticatedValue the authenticated value containing the applied action.
     *
     * @return a future of the task triggered on the controller.
     */
    @Override
    public Future<AuthenticatedValue> applyActionAuthenticated(final AuthenticatedValue authenticatedValue) {
        try {
            if (!authenticatedValue.hasValue() || authenticatedValue.getValue().isEmpty()) {
                throw new NotAvailableException("Value in AuthenticatedValue");
            }

            final ActionDescription actionDescription;
            try {
                if (SessionManager.getInstance().isLoggedIn()) {
                    actionDescription = EncryptionHelper.decryptSymmetric(authenticatedValue.getValue(), SessionManager.getInstance().getSessionKey(), ActionDescription.class);
                } else {
                    actionDescription = ActionDescription.parseFrom(authenticatedValue.getValue());
                }
            } catch (CouldNotPerformException | InvalidProtocolBufferException ex) {
                throw new CouldNotPerformException("Could not extract ActionDescription from AuthenticatedValue", ex);
            }
            return applyActionAuthenticated(authenticatedValue, actionDescription.getServiceStateDescription().getServiceType());
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(AuthenticatedValue.class, ex);
        }
    }

    /**
     * Apply an authenticated action on the controller.
     *
     * @param authenticatedValue the authenticated value containing the applied action.
     * @param serviceType        the service type is required to determine if the service type is aggregated. If it is not,
     *                           the returned future has to be wrapped into a {@link org.openbase.jul.extension.type.util.TransactionSynchronizationFuture}
     *                           for synchronization purposes.
     *
     * @return a future of the task triggered on the controller.
     */
    public Future<AuthenticatedValue> applyActionAuthenticated(final AuthenticatedValue authenticatedValue, final ServiceType serviceType) {
        try {
            if (!isServiceAggregated(serviceType)) {
                return super.applyActionAuthenticated(authenticatedValue);
            }
            return RPCHelper.callRemoteMethod(authenticatedValue, this, AuthenticatedValue.class);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(AuthenticatedValue.class, ex);
        }
    }

    @Override
    public Future<Snapshot> recordSnapshot() {
        return recordSnapshot(UnitType.UNKNOWN);
    }

    @Override
    public Future<Snapshot> recordSnapshot(final UnitType unitType) {
        return GlobalCachedExecutorService.submit(() -> {
            final Snapshot.Builder snapshotBuilder = Snapshot.newBuilder();
            final Set<UnitRemote> unitRemoteSet = new HashSet<>();
            for (final String aggregatedUnitId : getAggregatedUnitIdList()) {
                UnitConfig aggregatedUnitConfig = Registries.getUnitRegistry().getUnitConfigById(aggregatedUnitId);
                if (!UnitConfigProcessor.isDalUnit(aggregatedUnitConfig)) {
                    continue;
                }

                if (unitType == UnitType.UNKNOWN || aggregatedUnitConfig.getUnitType() == unitType) {
                    unitRemoteSet.add(Units.getUnit(aggregatedUnitId, false));
                }
            }

            // take the snapshot
            final Map<UnitRemote, Future<SnapshotType.Snapshot>> snapshotFutureMap = new HashMap<>();
            for (final UnitRemote<?> remote : unitRemoteSet) {
                try {
                    if (UnitProcessor.isDalUnit(remote)) {
                        if (!remote.isConnected()) {
                            throw new InvalidStateException("Unit[" + remote.getLabel() + "] is currently not reachable!");
                        }
                        snapshotFutureMap.put(remote, remote.recordSnapshot());
                    }
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not record snapshot of " + remote.getLabel(), ex), logger, LogLevel.WARN);
                }
            }

            // build snapshot
            for (final Map.Entry<UnitRemote, Future<SnapshotType.Snapshot>> snapshotFutureEntry : snapshotFutureMap.entrySet()) {
                try {
                    snapshotBuilder.addAllServiceStateDescription(snapshotFutureEntry.getValue().get(5, TimeUnit.SECONDS).getServiceStateDescriptionList());
                } catch (ExecutionException | TimeoutException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not record snapshot of " + snapshotFutureEntry.getKey().getLabel(), ex), logger);
                }
            }

            return snapshotBuilder.build();
        });
    }
}
