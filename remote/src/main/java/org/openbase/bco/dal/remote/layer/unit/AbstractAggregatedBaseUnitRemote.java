package org.openbase.bco.dal.remote.layer.unit;

/*-
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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.bco.dal.lib.layer.unit.MultiUnit;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.remote.layer.service.ServiceRemoteManager;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.SnapshotType.Snapshot;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.Set;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AbstractAggregatedBaseUnitRemote<D extends Message> extends AbstractUnitRemote<D> implements MultiUnit<D> {

    private final ServiceRemoteManager<D> serviceRemoteManager;

    public AbstractAggregatedBaseUnitRemote(final Class<D> dataClass) {
        super(dataClass);
        this.serviceRemoteManager = new ServiceRemoteManager<D>(this) {
            @Override
            protected Set<ServiceType> getManagedServiceTypes() throws NotAvailableException {
                return getSupportedServiceTypes();
            }

            @Override
            protected void notifyServiceUpdate(Unit source, Message data) {
                // anything needed here?
            }
        };
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        UnitConfig unitConfig = super.applyConfigUpdate(config);
        serviceRemoteManager.applyConfigUpdate(unitConfig.getLocationConfig().getUnitIdList());
        return unitConfig;
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        serviceRemoteManager.activate();
        super.activate();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        serviceRemoteManager.deactivate();
        super.deactivate();
    }

    @Override
    public boolean isServiceAvailable(ServiceType serviceType) {
        return serviceRemoteManager.isServiceAvailable(serviceType);
    }

    @Override
    public Future<Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.recordSnapshot();
    }

    @Override
    public Future<Snapshot> recordSnapshot(final UnitType unitType) throws CouldNotPerformException, InterruptedException {
        return serviceRemoteManager.recordSnapshot(unitType);
    }

    @Override
    public ServiceRemote getServiceRemote(final ServiceType serviceType) throws NotAvailableException {
        return serviceRemoteManager.getServiceRemote(serviceType);
    }

    /**
     * Apply an authenticated action on the controller. This only works with the default session manager because the
     * service type has to be extracted from the action description in the authenticated value. This method internally
     * calls {@link #applyActionAuthenticated(AuthenticatedValue, ServiceType)}.
     *
     * @param authenticatedValue the authenticated value containing the applied action.
     *
     * @return a future of the task triggered on the controller.
     *
     * @throws CouldNotPerformException if the remote task could not be created.
     */
    @Override
    public Future<AuthenticatedValue> applyActionAuthenticated(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
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
     *
     * @throws CouldNotPerformException if the remote task could not be created.
     */
    public Future<AuthenticatedValue> applyActionAuthenticated(final AuthenticatedValue authenticatedValue, final ServiceType serviceType) throws CouldNotPerformException {
        if (isServiceAggregated(serviceType)) {
            return super.applyActionAuthenticated(authenticatedValue);
        }

        try {
            return RPCHelper.callRemoteMethod(authenticatedValue, this, AuthenticatedValue.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not apply action!", ex);
        }
    }
}
