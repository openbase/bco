package org.openbase.bco.dal.lib.layer.service.provider;

/*-
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

import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.type.domotic.state.ActivityMultiStateType.ActivityMultiState;

import java.util.HashSet;
import java.util.Set;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.ACTIVITY_MULTI_STATE_SERVICE;

/**
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public interface ActivityMultiStateProviderService extends ProviderService {

    @RPCMethod(legacy = true)
    default ActivityMultiState getActivityMultiState() throws NotAvailableException {
        return (ActivityMultiState) getServiceProvider().getServiceState(ACTIVITY_MULTI_STATE_SERVICE);
    }

    static ActivityMultiState verifyActivityMultiState(final ActivityMultiState activityMultiState) throws VerificationFailedException {
        if (activityMultiState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        ActivityMultiState.Builder builder = activityMultiState.toBuilder();
        final Set<String> activityIdSet = new HashSet<>(activityMultiState.getActivityIdList());
        if (activityIdSet.size() != activityMultiState.getActivityIdCount()) {
            builder.clearActivityId();
            builder.addAllActivityId(activityIdSet);
        }
        return builder.build();

        //TODO validate that an activity config exists for every id
    }
}
