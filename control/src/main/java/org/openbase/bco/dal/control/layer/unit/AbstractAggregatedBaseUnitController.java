package org.openbase.bco.dal.control.layer.unit;

/*-
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
 */

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.AuthPair;
import org.openbase.bco.authentication.lib.AuthenticationBaseData;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.bco.dal.lib.layer.unit.MultiUnit;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.remote.layer.service.ServiceRemoteManager;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.RecurrenceEventFilter;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.SnapshotType.Snapshot;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractAggregatedBaseUnitController<D extends AbstractMessage & Serializable, DB extends D.Builder<DB>> extends AbstractBaseUnitController<D, DB> implements MultiUnit<D> {

    public static final long MINIMAL_UPDATE_FREQUENCY = 10;

    private final RecurrenceEventFilter<Void> unitEventFilter;
    private final ServiceRemoteManager<D> serviceRemoteManager;

    public AbstractAggregatedBaseUnitController(final DB builder) throws InstantiationException {
        this(builder, 0);
    }

    public AbstractAggregatedBaseUnitController(final DB builder, final long maxUpdateFrequency) throws InstantiationException {
        super(builder);

        // filter updates through internal units by the provided frequency
        unitEventFilter = new RecurrenceEventFilter<Void>(Math.max(maxUpdateFrequency, MINIMAL_UPDATE_FREQUENCY)) {
            @Override
            public void relay() {
                updateUnitData();
            }
        };

        // Create a service remote manager that triggers data updates
        this.serviceRemoteManager = new ServiceRemoteManager<D>(this, getManageLock()) {
            @Override
            protected Set<ServiceType> getManagedServiceTypes() throws NotAvailableException {
                return AbstractAggregatedBaseUnitController.this.getSupportedServiceTypes();
            }

            @Override
            protected void notifyServiceUpdate(final Unit<?> source, final Message data) throws NotAvailableException {
                try {
                    unitEventFilter.trigger();
                } catch (final CouldNotPerformException ex) {
                    logger.error("Could not trigger recurrence event filter for unit[" + getLabel() + "]");
                }
            }
        };
    }

    @Override
    public synchronized UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        UnitConfig unitConfig = super.applyConfigUpdate(config);
        serviceRemoteManager.applyConfigUpdate(getAggregatedUnitConfigList());
        // if already active than update the current state.
        if (isActive()) {
            updateUnitData();
        }
        return unitConfig;
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        if (isActive()) {
            logger.debug("Skip unit activation because it is already active...");
            return;
        }
        logger.debug("Activate unit [" + getLabel() + "]!");

        // activate and service remote manager and update data without notification before calling super activate
        // the super call will then automatically notify the updated unit data
        serviceRemoteManager.activate();
        updateUnitData(false);
        super.activate();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        logger.debug("Deactivate unit [" + getLabel() + "]!");
        super.deactivate();
        serviceRemoteManager.deactivate();
    }

    /**
     * Call to {@link #updateUnitData(boolean)} with notify change as true.
     */
    private void updateUnitData() {
        updateUnitData(true);
    }

    /**
     * Synchronize the data from all internal units managed by the service remote manager into the data builder.
     *
     * @param notifyChange flag determining if the data builder should be notified afterwards.
     */
    private void updateUnitData(final boolean notifyChange) {
        try (final ClosableDataBuilder<DB> dataBuilder = getDataBuilder(this, notifyChange)) {
            serviceRemoteManager.updateBuilderWithAvailableServiceStates(dataBuilder.getInternalBuilder(), getDataClass(), getSupportedServiceTypes());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update current status!", ex), logger, LogLevel.WARN);
        }
    }

    @Override
    public boolean isServiceAvailable(ServiceType serviceType) {

        if (serviceType == ServiceType.UNKNOWN) {
            return false;
        }

        try {
            return serviceRemoteManager.isServiceAvailable(serviceType) || !isServiceAggregated(serviceType);
        } catch (NotAvailableException e) {
            return false;
        }
    }

    @Override
    public Future<Snapshot> recordSnapshot() {
        return serviceRemoteManager.recordSnapshot();
    }

    @Override
    public Future<Snapshot> recordSnapshot(final UnitType unitType) {
        return serviceRemoteManager.recordSnapshot(unitType);
    }

    @Override
    public Future<Void> restoreSnapshot(final Snapshot snapshot) {
        return serviceRemoteManager.restoreSnapshot(snapshot);
    }

    @Override
    public Future<AuthenticatedValue> restoreSnapshotAuthenticated(AuthenticatedValue authenticatedSnapshot) {
        return serviceRemoteManager.restoreSnapshotAuthenticated(authenticatedSnapshot);
    }

    @Override
    public Future<ActionDescription> applyAction(final ActionDescription actionDescription) {
        try {

            // verify that the service type is set
            if (actionDescription.getServiceStateDescription().getServiceType() == ServiceType.UNKNOWN) {
                throw new InvalidStateException("Service type of applied action is unknown!");
            }

            // verify that the service type is available
            if (!isServiceAvailable(actionDescription.getServiceStateDescription().getServiceType())) {
                throw new NotAvailableException("ServiceType[" + actionDescription.getServiceStateDescription().getServiceType().name() + "] is not available for " + this);
            }

            // if service is not aggregated handle action directly via native action scheduling
            // this means we are ready and no special action handling is required
            if (!isServiceAggregated(actionDescription.getServiceStateDescription().getServiceType())) {
                return super.applyAction(actionDescription);
            }

            // apply action on all service remotes
            return serviceRemoteManager.applyAction(actionDescription);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Could not apply action!", ex));
        }
    }

    @Override
    protected Future<ActionDescription> internalApplyActionAuthenticated(final AuthenticatedValue authenticatedValue, final ActionDescription.Builder actionDescriptionBuilder, final AuthenticationBaseData authenticationBaseData, final AuthPair authPair) {

        try {
            // verify that the service type is set
            if (actionDescriptionBuilder.getServiceStateDescription().getServiceType() == ServiceType.UNKNOWN) {
                throw new InvalidStateException("Service type of applied action is unknown!");
            }

            // verify that the service type is available
            if (!isServiceAvailable(actionDescriptionBuilder.getServiceStateDescription().getServiceType())) {
                throw new NotAvailableException("ServiceType[" + actionDescriptionBuilder.getServiceStateDescription().getServiceType().name() + "] is not available for " + this);
            }

            // if service is not aggregated handle action directly via native action scheduling
            // this means we are ready and no special action handling is required
            if (!isServiceAggregated(actionDescriptionBuilder.getServiceStateDescription().getServiceType())) {
                return super.internalApplyActionAuthenticated(authenticatedValue, actionDescriptionBuilder, authenticationBaseData, authPair);
            }

            // apply action on all service remotes, action impact in set via the actionDescriptionBuilder.
            final byte[] sessionKey = (authenticationBaseData != null) ? authenticationBaseData.getSessionKey() : null;
            return FutureProcessor.postProcess((result) -> actionDescriptionBuilder.build(), serviceRemoteManager.applyActionAuthenticated(authenticatedValue, actionDescriptionBuilder, sessionKey));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    @Override
    public ServiceRemote getServiceRemote(final ServiceType serviceType) throws NotAvailableException {
        return serviceRemoteManager.getServiceRemote(serviceType);
    }
}
