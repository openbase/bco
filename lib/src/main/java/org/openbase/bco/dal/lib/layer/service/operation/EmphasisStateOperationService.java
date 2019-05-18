package org.openbase.bco.dal.lib.layer.service.operation;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import java.util.concurrent.Future;

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.provider.EmphasisStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.EmphasisStateType.EmphasisState;

/**
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public interface EmphasisStateOperationService extends OperationService, EmphasisStateProviderService {

    /**
     * Method sets the emphasis state of this unit.
     * @param emphasisState the new emphasis state to apply.
     * @return a action description representing the state of the update.
     */
    @RPCMethod(legacy = true)
    default Future<ActionDescription> setEmphasisState(final EmphasisState emphasisState) {
        try {
            return getServiceProvider().applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(emphasisState, ServiceType.EMPHASIS_STATE_SERVICE));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    /**
     * Method sets the emphasis state of this unit.
     * @param emphasisState the new emphasis state to apply.
     * @param actionParameter a declaration of custom action parameter to use e.g. define the authentication ticket to use.
     * @return a action description representing the state of the update.
     */
    default Future<ActionDescription> setEmphasisState(final EmphasisState emphasisState, final ActionParameter actionParameter) {
        try {
            return getServiceProvider().applyAction(actionParameter.toBuilder().setServiceStateDescription(ActionDescriptionProcessor.generateServiceStateDescription(emphasisState, ServiceType.EMPHASIS_STATE_SERVICE)));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    /**
     * Method sets the {@code securityValue} of the current emphasis state.
     * The comfort and economy value are recomputed related to the new security value while preserving their ratios.
     *
     * @param securityValue the security value to update.
     * @param actionParameter a declaration of custom action parameter to use e.g. define the authentication ticket to use.
     *
     * @return a action description representing the state of the update.
     */
    default Future<ActionDescription> setSecurityEmphasis(final double securityValue, final ActionParameter actionParameter) {
        try {
            return setEmphasisState(updateSecurityEmphasis(securityValue, getEmphasisState()), actionParameter);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ex);
        }
    }

    /**
     * Method sets the {@code economyValue} of the current emphasis state.
     * The security and comfort value are recomputed related to the new economy value while preserving their ratios.
     *
     * @param economyValue the economy value to update.
     * @param actionParameter a declaration of custom action parameter to use e.g. define the authentication ticket to use.
     *
     * @return a action description representing the state of the update.
     */
    default Future<ActionDescription> setEconomyEmphasis(final double economyValue, final ActionParameter actionParameter) {
        try {
            return setEmphasisState(updateEconomyEmphasis(economyValue, getEmphasisState()), actionParameter);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ex);
        }
    }

    /**
     * Method sets the {@code comfortValue} of the current emphasis state.
     * The security and economy value are recomputed related to the new comfort value while preserving their ratios.
     *
     * @param comfortValue the comfort value to update.
     * @param actionParameter a declaration of custom action parameter to use e.g. define the authentication ticket to use.
     *
     * @return a action description representing the state of the update.
     */
    default Future<ActionDescription> setComfortEmphasis(final double comfortValue, final ActionParameter actionParameter) {
        try {
            return setEmphasisState(updateComfortEmphasis(comfortValue, getEmphasisState()), actionParameter);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ex);
        }
    }

    /**
     * Method sets the {@code securityValue} of the current emphasis state.
     * The comfort and economy value are recomputed related to the new security value while preserving their ratios.
     *
     * @param securityValue the security value to update.
     *
     * @return a action description representing the state of the update.
     */
    default Future<ActionDescription> setSecurityEmphasis(final double securityValue) {
        try {
            return setEmphasisState(updateSecurityEmphasis(securityValue, getEmphasisState()));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ex);
        }
    }

    /**
     * Method sets the {@code economyValue} of the current emphasis state.
     * The security and comfort value are recomputed related to the new economy value while preserving their ratios.
     *
     * @param economyValue the economy value to update.
     *
     * @return a action description representing the state of the update.
     */
    default Future<ActionDescription> setEconomyEmphasis(final double economyValue) {
        try {
            return setEmphasisState(updateEconomyEmphasis(economyValue, getEmphasisState()));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ex);
        }
    }

    /**
     * Method sets the {@code comfortValue} of the current emphasis state.
     * The security and economy value are recomputed related to the new comfort value while preserving their ratios.
     *
     * @param comfortValue the comfort value to update.
     *
     * @return a action description representing the state of the update.
     */
    default Future<ActionDescription> setComfortEmphasis(final double comfortValue) {
        try {
            return setEmphasisState(updateComfortEmphasis(comfortValue, getEmphasisState()));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ex);
        }
    }

    /**
     * Method updates the {@code securityValue} of the given {@code currentEmphasisState}.
     * The comfort and economy value are recomputed related to the new security value while preserving their ratios.
     *
     * @param securityValue        the security value to update.
     * @param currentEmphasisState the emphasis state used as baseline.
     *
     * @return a newly build EmphasisState offering the updated values.
     */
    default EmphasisState updateSecurityEmphasis(final double securityValue, final EmphasisState currentEmphasisState) {
        final double comfortEconomySum = (Double.isNaN(currentEmphasisState.getComfort()) ? 0 : currentEmphasisState.getComfort()) + (Double.isNaN(currentEmphasisState.getEconomy()) ? 0 : currentEmphasisState.getEconomy());
        final double comfortRatio = (comfortEconomySum == 0 ? 0.5d : currentEmphasisState.getComfort() / comfortEconomySum);
        final double economyRatio = (comfortEconomySum == 0 ? 0.5d : currentEmphasisState.getEconomy() / comfortEconomySum);
        return EmphasisState.newBuilder()
                .setSecurity(securityValue)
                .setEconomy((1d - securityValue) * economyRatio)
                .setComfort((1d - securityValue) * comfortRatio).build();
    }

    /**
     * Method updates the {@code economyValue} of the given {@code currentEmphasisState}.
     * The security and comfort value are recomputed related to the new economy value while preserving their ratios.
     *
     * @param economyValue         the economy value to update.
     * @param currentEmphasisState the emphasis state used as baseline.
     *
     * @return a newly build EmphasisState offering the updated values.
     */
    default EmphasisState updateEconomyEmphasis(final double economyValue, final EmphasisState currentEmphasisState) {
        final double comfortSecuritySum = (Double.isNaN(currentEmphasisState.getComfort()) ? 0 : currentEmphasisState.getComfort()) + (Double.isNaN(currentEmphasisState.getSecurity()) ? 0 : currentEmphasisState.getSecurity());
        final double comfortRatio = (comfortSecuritySum == 0 ? 0.5d : currentEmphasisState.getComfort() / comfortSecuritySum);
        final double securityRatio = (comfortSecuritySum == 0 ? 0.5d : currentEmphasisState.getSecurity() / comfortSecuritySum);
        return EmphasisState.newBuilder()
                .setSecurity((1d - economyValue) * securityRatio)
                .setEconomy(economyValue)
                .setComfort((1d - economyValue) * comfortRatio).build();
    }

    /**
     * Method updates the {@code comfortValue} of the given {@code currentEmphasisState}.
     * The security and economy value are recomputed related to the new comfort value while preserving their ratios.
     *
     * @param comfortValue         the comfort value to update.
     * @param currentEmphasisState the emphasis state used as baseline.
     *
     * @return a newly build EmphasisState offering the updated values.
     */
    default EmphasisState updateComfortEmphasis(final double comfortValue, final EmphasisState currentEmphasisState) {
        final double economySecuritySum = (Double.isNaN(currentEmphasisState.getEconomy()) ? 0 : currentEmphasisState.getEconomy()) + (Double.isNaN(currentEmphasisState.getSecurity()) ? 0 : currentEmphasisState.getSecurity());
        final double economyRatio = (economySecuritySum == 0 ? 0.5d : currentEmphasisState.getEconomy() / economySecuritySum);
        final double securityRatio = (economySecuritySum == 0 ? 0.5d : currentEmphasisState.getSecurity() / economySecuritySum);
        return EmphasisState.newBuilder()
                .setSecurity((1d - comfortValue) * securityRatio)
                .setEconomy((1d - comfortValue) * economyRatio)
                .setComfort(comfortValue).build();
    }
}
