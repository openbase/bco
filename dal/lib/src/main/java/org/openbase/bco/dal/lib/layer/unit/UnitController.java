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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.pattern.controller.MessageController;
import org.openbase.jul.schedule.Timeout;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @param <D>  the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface UnitController<D extends AbstractMessage, DB extends D.Builder<DB>> extends Unit<D>, MessageController<D, DB> {

    /**
     * Method initialize this controller with the given unit configuration.
     *
     * @param config the unit configuration
     *
     * @throws InitializationException is throw if any error occurs during the initialization phase.
     * @throws InterruptedException    is thrown if the current thread was externally interrupted.
     */
    void init(final UnitConfig config) throws InitializationException, InterruptedException;

    /**
     * Applies the given service state update for this unit.
     *
     * @param serviceType  the type of the service to update.
     * @param serviceState service state to apply.
     *
     * @throws CouldNotPerformException
     */
    void applyDataUpdate(final Message serviceState, final ServiceType serviceType) throws CouldNotPerformException;

    /**
     * Applies the given service state update for this unit.
     * Please make sure that the applied service state offers a compatible responsible action.
     *
     * @param serviceType         the type of the service to update.
     * @param serviceStateBuilder the service state to apply.
     *
     * @throws CouldNotPerformException
     */
    default void applyDataUpdate(final Message.Builder serviceStateBuilder, final ServiceType serviceType) throws CouldNotPerformException {
        applyDataUpdate(serviceStateBuilder.build(), serviceType);
    }

    /**
     * Method can be used to directly manipulate a service state of this unit which passthroughts the unit alloction.
     * Method should only be used for unit test or when the controller directly needs to adjust the unit state
     * e.g. agents are activated during startup because the autostart flag is configured or local service provider needs to apply action states like the location presence detector.
     * <p>
     * Method internally generates a responsible action for the service state and submits the update via the controllers {@code applyDataUpdate(...)} method.
     * This unit is marked as responsible for this transaction.
     *
     * @param serviceStateBuilder a builder instance of the service state to set.
     * @param serviceType         the related service state.
     * @param <MB>                the message builder type.
     *
     * @return the builder instance given via the {@code serviceStateBuilder} argument with an updated responsible action field.
     *
     * @throws CouldNotPerformException is thrown if the update could not be applied. Check the cause chain for more details.
     */
    default <MB extends Message.Builder> MB applyServiceState(final MB serviceStateBuilder, final ServiceType serviceType) throws CouldNotPerformException {
        return applyServiceState(serviceStateBuilder, serviceType, null);
    }

    /**
     * Method can be used to directly manipulate a service state of this unit which passthroughts the unit alloction.
     * Method should only be used for unit test or when the controller directly needs to adjust the unit state
     * e.g. agents are activated during startup because the autostart flag is configured or local service provider needs to apply action states like the location presence detector.
     * <p>
     * Method internally generates a responsible action for the service state and submits the update via the controllers {@code applyDataUpdate(...)} method.
     *
     * @param serviceStateBuilder a builder instance of the service state to set.
     * @param serviceType         the related service state.
     * @param actionInitiatorId   the initiator who is responsible for the update. In case of a null value this unit is marked as responsible for this transaction.
     * @param <MB>                the message builder type.
     *
     * @return the builder instance given via the {@code serviceStateBuilder} argument with an updated responsible action field.
     *
     * @throws CouldNotPerformException is thrown if the update could not be applied. Check the cause chain for more details.
     */
    default <MB extends Message.Builder> MB applyServiceState(final MB serviceStateBuilder, final ServiceType serviceType, final String actionInitiatorId) throws CouldNotPerformException {
        ActionDescriptionProcessor.generateAndSetResponsibleAction(serviceStateBuilder, serviceType, this, Timeout.INFINITY_TIMEOUT, TimeUnit.MILLISECONDS, false, false, false, Priority.NO, ActionInitiator.newBuilder().setInitiatorId((actionInitiatorId != null ? actionInitiatorId : getId())).build());
        applyDataUpdate(serviceStateBuilder, serviceType);
        return serviceStateBuilder;
    }

    /**
     * Method can be used to directly manipulate a service state of this unit which passthroughts the unit alloction.
     * Method should only be used for unit test or when the controller directly needs to adjust the unit state
     * e.g. agents are activated during startup because the autostart flag is configured or local service provider needs to apply action states like the location presence detector.
     * <p>
     * Method internally generates a responsible action for the service state and submits the update via the controllers {@code applyDataUpdate(...)} method.
     * This unit is marked as responsible for this transaction.
     *
     * @param serviceState a instance of the service state to set.
     * @param serviceType  the related service state.
     * @param <M>          the message type.
     *
     * @return the builder instance given via the {@code serviceState} argument with an updated responsible action field.
     *
     * @throws CouldNotPerformException is thrown if the update could not be applied. Check the cause chain for more details.
     */
    default <M extends Message> M applyServiceState(final M serviceState, final ServiceType serviceType) throws CouldNotPerformException {
        return (M) applyServiceState(serviceState.toBuilder(), serviceType).build();
    }

    /**
     * Method can be used to directly manipulate a service state of this unit which passthroughts the unit alloction.
     * Method should only be used for unit test or when the controller directly needs to adjust the unit state
     * e.g. agents are activated during startup because the autostart flag is configured or local service provider needs to apply action states like the location presence detector.
     * <p>
     * Method internally generates a responsible action for the service state and submits the update via the controllers {@code applyDataUpdate(...)} method.
     *
     * @param serviceState      a instance of the service state to set.
     * @param serviceType       the related service state.
     * @param actionInitiatorId the initiator who is responsible for the update. In case of a null value this unit is marked as responsible for this transaction.
     * @param <M>               the message type.
     *
     * @return the builder instance given via the {@code serviceStateBuilder} argument with an updated responsible action field.
     *
     * @throws CouldNotPerformException is thrown if the update could not be applied. Check the cause chain for more details.
     */
    default <M extends Message> M applyServiceState(final M serviceState, final ServiceType serviceType, final String actionInitiatorId) throws CouldNotPerformException {
        return (M) applyServiceState(serviceState.toBuilder(), serviceType, actionInitiatorId).build();
    }

    /**
     * This method is called if an authorized and scheduled action causes a new service state.
     *
     * @param serviceState the new service state to apply.
     * @param serviceType  The type of the modified service.
     *
     * @return a future object representing the progress of the service state transition.
     */
    Future<ActionDescription> performOperationService(final Message serviceState, final ServiceType serviceType);

    /**
     * Method is only for unit tests where one has to make sure that all actions are removed from the action stack in order to minimize influence of other tests.
     * Note: This method does nothing if the unit test mode is not enabled.
     *
     * @throws InterruptedException     is thrown if the thread was externally interrupted
     * @throws CouldNotPerformException is thrown if no all actions could be canceled.
     */
    void cancelAllActions() throws InterruptedException, CouldNotPerformException;
}
