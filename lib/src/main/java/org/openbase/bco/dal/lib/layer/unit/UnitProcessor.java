package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.AuthenticatedServiceProcessor;
import org.openbase.bco.authentication.lib.AuthenticationBaseData;
import org.openbase.bco.authentication.lib.AuthenticationClientHandler;
import org.openbase.bco.authentication.lib.AuthorizationHelper.PermissionType;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.auth.AuthorizationWithTokenHelper;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.SnapshotType.Snapshot;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitProcessor {

    public static boolean isHostUnit(final Unit<?> unit) throws CouldNotPerformException {
        return UnitConfigProcessor.isHostUnit(unit.getConfig());
    }

    public static boolean isDalUnit(final Unit<?> unit) throws CouldNotPerformException {
        return UnitConfigProcessor.isDalUnit(unit.getConfig());
    }

    public static boolean isBaseUnit(final Unit<?> unit) throws CouldNotPerformException {
        return UnitConfigProcessor.isBaseUnit(unit.getConfig());
    }

    public static void verifyUnitType(final Unit<?> unit) throws VerificationFailedException {
        try {
            UnitConfigProcessor.verifyUnitType(unit.getConfig(), unit.getUnitType());
        } catch (NotAvailableException ex) {
            throw new VerificationFailedException("Could not verify unit type!", ex);
        }
    }

    public static void verifyUnitConfig(final Unit<?> unit) throws VerificationFailedException {
        try {
            UnitConfigProcessor.verifyUnitConfig(unit.getConfig(), unit.getUnitType());
        } catch (NotAvailableException ex) {
            throw new VerificationFailedException("Could not verify unit type!", ex);
        }
    }

    public static void verifyUnit(final Unit<?> unit) throws VerificationFailedException {
        verifyUnitConfig(unit);
        verifyUnitType(unit);
    }

    /**
     * This method returns the unit class resolved by the given unit.
     *
     * @param unit the unit to extract the unit class.
     *
     * @return the unit data class.
     *
     * @throws org.openbase.jul.exception.NotAvailableException is thrown if the data class could not be detected.
     */
    public static Class<? extends Message> getUnitDataClass(final Unit<?> unit) throws NotAvailableException {
        return UnitConfigProcessor.getUnitDataClass(unit.getUnitType());
    }

    public static Future<Void> restoreSnapshot(final Snapshot snapshot, final Logger logger, final Unit ... units) {
        return restoreSnapshotAuthenticated(snapshot, null, logger, units);
    }

    public static Future<Void> restoreSnapshot(final Snapshot snapshot, final Logger logger, final Collection<? extends Unit<?>> unitCollection) {
        return restoreSnapshotAuthenticated(snapshot, null, logger, unitCollection);
    }

    public static Future<Void> restoreSnapshotAuthenticated(final Snapshot snapshot, final AuthenticationBaseData authenticationBaseData, final Logger logger, final Collection<? extends Unit<?>> unitCollection) {
        return restoreSnapshotAuthenticated(snapshot, authenticationBaseData, logger, unitCollection.toArray(new Unit[unitCollection.size()]));
    }

    public static Future<AuthenticatedValue> restoreSnapshotAuthenticated(final AuthenticatedValue authenticatedSnapshot, final Logger logger, final UnitConfig responsibleUnit, final Collection<? extends Unit<?>> unitCollection) {
        return restoreSnapshotAuthenticated(authenticatedSnapshot, logger, responsibleUnit, unitCollection.toArray(new Unit[unitCollection.size()]));
    }

    public static Future<AuthenticatedValue> restoreSnapshotAuthenticated(final AuthenticatedValue authenticatedSnapshot, final Logger logger, final UnitConfig responsibleUnit, final Unit<?>... units) {
        return GlobalCachedExecutorService.submit(() ->
                AuthenticatedServiceProcessor.authenticatedAction(authenticatedSnapshot, Snapshot.class, (snapshot, authenticationBaseData) -> {
                    AuthorizationWithTokenHelper.canDo(authenticationBaseData, responsibleUnit, PermissionType.READ, Registries.getUnitRegistry(), responsibleUnit.getUnitType(), ServiceType.UNKNOWN);

                    final Future<Void> internalTask = UnitProcessor.restoreSnapshotAuthenticated(snapshot, authenticationBaseData, logger, units);
                    try {
                        internalTask.get(30, TimeUnit.SECONDS);
                        return null;
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new CouldNotPerformException("Authenticated action was interrupted!", ex);
                    } catch (ExecutionException | TimeoutException ex) {
                        throw new CouldNotPerformException("Authenticated action failed!", ex);
                    } finally {
                        if (!internalTask.isDone()) {
                            internalTask.cancel(true);
                        }
                    }
                }));
    }

    public static Future<Void> restoreSnapshotAuthenticated(final Snapshot snapshot, final AuthenticationBaseData authenticationBaseData, final Logger logger, final Unit<?>... units) {
        try {
            if (authenticationBaseData != null) {
                try {
                    final TicketAuthenticatorWrapper initializedTicket = AuthenticationClientHandler.initServiceServerRequest(authenticationBaseData.getSessionKey(), authenticationBaseData.getTicketAuthenticatorWrapper());

                    return FutureProcessor.allOf((input, time, timeUnit) -> {
                        try {
                            for (Future<AuthenticatedValue> authenticatedValueFuture : input) {
                                AuthenticationClientHandler.handleServiceServerResponse(authenticationBaseData.getSessionKey(), initializedTicket, authenticatedValueFuture.get().getTicketAuthenticatorWrapper());
                            }
                        } catch (ExecutionException ex) {
                            throw new FatalImplementationErrorException("AllOf called result processable even though some futures did not finish", GlobalCachedExecutorService.getInstance(), ex);
                        }
                        return null;
                    }, generateSnapshotActions(snapshot, initializedTicket, authenticationBaseData.getSessionKey(), logger, units));
                } catch (CouldNotPerformException ex) {
                    throw new CouldNotPerformException("Could not update ticket for further requests", ex);
                }
            } else {
                return FutureProcessor.allOf((input, time, timeUnit)  -> null, generateSnapshotActions(snapshot, null, null, logger, units));
            }
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(Void.class, new CouldNotPerformException("Could not record snapshot authenticated!", ex));
        }
    }

    public static Collection<Future<AuthenticatedValue>> generateSnapshotActions(final Snapshot snapshot, final TicketAuthenticatorWrapper ticketAuthenticatorWrapper, final byte[] sessionKey, final Logger logger, final Unit<?>... units) {
        final Map<String, Unit<?>> unitMap = new HashMap<>();
        for (Unit<?> unit : units) {
            try {
                unitMap.put(unit.getId(), unit);
            } catch (NotAvailableException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not resolve id to acquire unit state for snapshot", ex), logger, LogLevel.WARN);
            }
        }

        final Collection<Future<AuthenticatedValue>> futureCollection = new ArrayList<>();
        for (final ServiceStateDescription serviceStateDescription : snapshot.getServiceStateDescriptionList()) {
            final Unit<?> unit = unitMap.get(serviceStateDescription.getUnitId());

            if (unit == null) {
                logger.error("Could not resolve unit {} from snapshot", serviceStateDescription.getUnitId());
                continue;
            }

            try {
                final ActionParameter.Builder actionParameterBuilder = ActionDescriptionProcessor.generateDefaultActionParameter(serviceStateDescription);
                final ActionDescription.Builder actionDescriptionBuilder = ActionDescriptionProcessor.generateActionDescriptionBuilder(actionParameterBuilder);

                AuthenticatedValue.Builder authenticatedValue = AuthenticatedValue.newBuilder();
                if (ticketAuthenticatorWrapper != null) {
                    // prepare authenticated value to request action
                    authenticatedValue.setTicketAuthenticatorWrapper(ticketAuthenticatorWrapper);
                    authenticatedValue.setValue(EncryptionHelper.encryptSymmetric(actionDescriptionBuilder.build(), sessionKey));
                } else {
                    authenticatedValue.setValue(actionDescriptionBuilder.build().toByteString());
                }
                futureCollection.add(unit.applyActionAuthenticated(authenticatedValue.build()));
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not acquire unit state for snapshot", ex), logger, LogLevel.WARN);
            }
        }

        return futureCollection;
    }
}
