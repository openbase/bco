package org.openbase.bco.dal.lib.layer.service.provider;

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

import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.type.domotic.action.ActionEmphasisType.ActionEmphasis.Category;
import org.openbase.type.domotic.state.EmphasisStateType.EmphasisState;
import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.EMPHASIS_STATE_SERVICE;

/**
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public interface EmphasisStateProviderService extends ProviderService {

    @RPCMethod(legacy = true)
    default EmphasisState getEmphasisState() throws NotAvailableException {
        return (EmphasisState) getServiceProvider().getServiceState(EMPHASIS_STATE_SERVICE);
    }

    static EmphasisState verifyEmphasisState(final EmphasisState emphasisState) throws VerificationFailedException {

        final double MARGIN = 0.01;
        final double security = (Double.isNaN(emphasisState.getSecurity()) ? 0 : emphasisState.getSecurity());
        final double economy = (Double.isNaN(emphasisState.getEconomy()) ? 0 : emphasisState.getEconomy());
        final double comfort = (Double.isNaN(emphasisState.getComfort()) ? 0 : emphasisState.getComfort());

        if (emphasisState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        boolean emphasisFound = false;

        if (emphasisState.hasComfort()) {
            OperationService.verifyValueRange("comfort", comfort, 0d, 1d);
            emphasisFound = true;
        }

        if (emphasisState.hasEconomy()) {
            OperationService.verifyValueRange("economy", economy, 0d, 1d);
            emphasisFound = true;
        }

        if (emphasisState.hasSecurity()) {
            OperationService.verifyValueRange("security", security, 0d, 1d);
            emphasisFound = true;
        }

        if (!emphasisFound) {
            throw new VerificationFailedException("EmphasisState does not contain emphasis!");
        }

        OperationService.verifyValue("emphasis sum", comfort + economy + security, 1d, MARGIN);

        // make sure all of the emphasis weight distances are greater then MARGIN, otherwise correct by default values.
        if (Math.abs(security - economy) <= MARGIN && Math.abs(security - comfort) <= MARGIN) {
            // correct current to default value
            final EmphasisState defaultEmphasisState = EmphasisState.getDefaultInstance();
            return emphasisState.toBuilder()
                    .setSecurity(defaultEmphasisState.getSecurity())
                    .setEconomy(defaultEmphasisState.getEconomy())
                    .setComfort(defaultEmphasisState.getComfort())
                    .build();
        }

        // make sure non pair is equals otherwise correct.
        // a correction of MARGIN can always me applied without colliding with the third
        // value because this is already guaranteed by the previously distance > MARGIN check.
        if (security == economy) {
            // prefer security in case of a conflict with economy.
            if(economy >= MARGIN) {
                return emphasisState.toBuilder()
                        .setSecurity(security + MARGIN)
                        .setEconomy(economy - MARGIN)
                        .setComfort(comfort)
                        .build();
            } else {
                // We are not able to further reduce the economy value because those already reaches its bottom.
                // Therefore we just increase the security value to prefer it.
                // Since the sum of all emphasis values needs to be 1 we need to decrease
                // the same amount of the comfort value which is anyway at its limit.
                return emphasisState.toBuilder()
                        .setSecurity(security + MARGIN)
                        .setEconomy(economy)
                        .setComfort(comfort - MARGIN)
                        .build();
            }
        } else if (security == comfort) {
            // prefer security in case of a conflict with comfort.
            if(comfort >= MARGIN) {
                return emphasisState.toBuilder()
                        .setSecurity(security + MARGIN)
                        .setEconomy(economy)
                        .setComfort(comfort - MARGIN)
                        .build();
            } else {
                // We are not able to further reduce the comfort value because those already reaches its bottom.
                // Therefore we just increase the security value to prefer it.
                // Since the sum of all emphasis values needs to be 1 we need to decrease
                // the same amount of the economy value which is anyway at its limit.
                return emphasisState.toBuilder()
                        .setSecurity(security + MARGIN)
                        .setEconomy(economy - MARGIN)
                        .setComfort(comfort)
                        .build();
            }
        } else if (economy == comfort) {
            // prefer economy in case of a conflict with comfort.
            if(comfort >= MARGIN) {
                return emphasisState.toBuilder()
                        .setSecurity(security)
                        .setEconomy(economy + MARGIN)
                        .setComfort(comfort - MARGIN)
                        .build();
            } else {
                // We are not able to further reduce the comfort value because those already reaches its bottom.
                // Therefore we just increase the economy value to prefer it.
                // Since the sum of all emphasis values needs to be 1 we need to decrease
                // the same amount of the security value which is anyway at its limit.
                return emphasisState.toBuilder()
                        .setSecurity(security - MARGIN)
                        .setEconomy(economy + MARGIN)
                        .setComfort(comfort)
                        .build();
            }
        }

        return emphasisState;
    }

    /**
     * Return the most recent emphasis category use for environment state optimisation.
     * <p>
     * Note: the safety category is always the most important one and therefore excluded from the evaluation.
     *
     * @return the emphasis category to follow most.
     */
    default Category getEmphasisCategory() {
        try {
            return getEmphasisCategory(getEmphasisState());
        } catch (CouldNotPerformException ex) {
            return Category.UNKNOWN;
        }
    }

    /**
     * Return the most recent emphasis category use for environment state optimisation.
     * <p>
     * Note: the safety category is always the most important one and therefore excluded from the evaluation.
     *
     * @param emphasisState the state used to detect the category.
     *
     * @return the emphasis category to follow most.
     */
    static Category getEmphasisCategory(final EmphasisState emphasisState) {
        final double security = (Double.isNaN(emphasisState.getSecurity()) ? 0 : emphasisState.getSecurity());
        final double economy = (Double.isNaN(emphasisState.getEconomy()) ? 0 : emphasisState.getEconomy());
        final double comfort = (Double.isNaN(emphasisState.getComfort()) ? 0 : emphasisState.getComfort());

        if (security > Math.max(economy, comfort)) {
            return Category.SECURITY;
        } else if (economy > Math.max(security, comfort)) {
            return Category.ECONOMY;
        } else if (comfort > Math.max(economy, security)) {
            return Category.COMFORT;
        } else {
            return Category.UNKNOWN;
        }
    }

}
