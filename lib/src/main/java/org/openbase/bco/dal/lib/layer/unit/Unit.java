package org.openbase.bco.dal.lib.layer.unit;

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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.service.ServiceProvider;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.iface.ScopeProvider;
import org.openbase.jul.iface.Configurable;
import org.openbase.jul.iface.Identifiable;
import org.openbase.jul.iface.Snapshotable;
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.iface.provider.LabelProvider;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.bco.dal.lib.layer.service.Services;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * @param <D> the data type of this unit used for the state synchronization.
 */
public interface Unit<D> extends LabelProvider, ScopeProvider, Identifiable<String>, Configurable<String, UnitConfig>, DataProvider<D>, ServiceProvider, Service, Snapshotable<Snapshot> {

    /**
     * Returns the unit type.
     *
     * @return UnitType
     * @throws NotAvailableException
     */
    public UnitType getType() throws NotAvailableException;

    /**
     * Returns the related template for this unit.
     *
     * @return UnitTemplate
     * @throws NotAvailableException in case the unit template is not available.
     */
    public UnitTemplate getTemplate() throws NotAvailableException;

    public default void verifyOperationServiceState(final Object serviceState) throws VerificationFailedException {

        if (serviceState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        final Method valueMethod;
        try {
            valueMethod = serviceState.getClass().getMethod("getValue");
        } catch (NoSuchMethodException ex) {
            // service state does contain any value so verification is not possible.
            return;
        }

        try {
            verifyOperationServiceStateValue((Enum) valueMethod.invoke(serviceState));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassCastException ex) {
            ExceptionPrinter.printHistory("Operation service verification phase failed!", ex, LoggerFactory.getLogger(getClass()));
        }
    }

    public default void verifyOperationServiceStateValue(final Enum value) throws VerificationFailedException {

        if (value == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceStateValue"));
        }

        if (value.name().equals("UNKNOWN")) {
            throw new VerificationFailedException("UNKNOWN." + value.getClass().getSimpleName() + " is an invalid operation service state of " + this + "!");
        }
    }

    @RPCMethod
    @Override
    public default Future<Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        MultiException.ExceptionStack exceptionStack = null;
        Snapshot.Builder snapshotBuilder = Snapshot.newBuilder();
        for (ServiceDescription serviceDescription : getTemplate().getServiceDescriptionList()) {
            try {
                ServiceStateDescription.Builder serviceStateDescription = ServiceStateDescription.newBuilder().setServiceType(serviceDescription.getType()).setUnitId(getId());

                // skip non operation services.
                if (serviceDescription.getPattern() != ServiceTemplate.ServicePattern.OPERATION) {
                    continue;
                }

                // load operation service attribute by related provider service
                Object serviceAttribute = Services.invokeServiceMethod(serviceDescription.getType(), ServiceTemplate.ServicePattern.PROVIDER, this);
                //System.out.println("load[" + serviceAttribute + "] type: " + serviceAttribute.getClass().getSimpleName());

                // verify operation service state (e.g. ignore UNKNOWN service states)
                verifyOperationServiceState(serviceAttribute);

                // fill action config
                final ServiceJSonProcessor serviceJSonProcessor = new ServiceJSonProcessor();
                try {
                    serviceStateDescription.setServiceAttribute(serviceJSonProcessor.serialize(serviceAttribute));
                } catch (InvalidStateException ex) {
                    // skip if serviceAttribute is empty.
                    continue;
                }
                serviceStateDescription.setUnitId(getId());
                serviceStateDescription.setUnitType(getTemplate().getType());
                serviceStateDescription.setServiceType(serviceDescription.getType());
                serviceStateDescription.setServiceType(serviceDescription.getType());
                serviceStateDescription.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(serviceAttribute));

                // add action config
                snapshotBuilder.addServiceStateDescription(serviceStateDescription.build());
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(this, ex, exceptionStack);
            }
        }
        MultiException.checkAndThrow("Could not record snapshot!", exceptionStack);
        return CompletableFuture.completedFuture(snapshotBuilder.build());
    }

    @RPCMethod
    @Override
    public default Future<Void> restoreSnapshot(final Snapshot snapshot) throws CouldNotPerformException, InterruptedException {
        try {
            Collection<Future> futureCollection = new ArrayList<>();
            for (final ServiceStateDescription serviceStateDescription : snapshot.getServiceStateDescriptionList()) {
                ActionDescription actionDescription = ActionDescription.newBuilder().setServiceStateDescription(serviceStateDescription).build();
                futureCollection.add(applyAction(actionDescription));
            }
            return GlobalCachedExecutorService.allOf(futureCollection);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not record snapshot!", ex);
        }
    }
}
