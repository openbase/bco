package org.openbase.bco.dal.lib.layer.service.provider;

/*-
 * #%L
 * BCO DAL Library
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

import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.schedule.Timeout;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.LocalPositionStateType.LocalPositionState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.geometry.TranslationType.Translation;
import org.openbase.type.math.Vec3DDoubleType.Vec3DDouble;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface LocalPositionStateProviderService extends ProviderService {

    @RPCMethod(legacy = true)
    default LocalPositionState getLocalPositionState() throws NotAvailableException {
        return (LocalPositionState) getServiceProvider().getServiceState(ServiceType.LOCAL_POSITION_STATE_SERVICE);
    }

    static LocalPositionState verifyLocalPositionState(final LocalPositionState localPositionState) throws VerificationFailedException {
        if (localPositionState == null) {
            throw new VerificationFailedException(new NotAvailableException("ServiceState"));
        }

        // if the state has a position with a translation fill the location ids accordingly
        LocalPositionState.Builder builder = localPositionState.toBuilder();
        if (localPositionState.hasPose() && localPositionState.getPose().hasTranslation()) {
            builder.clearLocationId();
            // convert the translation to a vector
            final Translation translation = localPositionState.getPose().getTranslation();
            final Vec3DDouble vec3DDouble = Vec3DDouble.newBuilder().setX(translation.getX()).setY(translation.getY()).setZ(translation.getZ()).build();
            try {
                // retrieve location list sorted by location type
                List<UnitConfig> locationUnitConfigsByCoordinate = Registries.getUnitRegistry().getLocationUnitConfigsByCoordinate(vec3DDouble).get(UnitRegistry.RCT_TIMEOUT, TimeUnit.MILLISECONDS);
                // if the list is empty the position is invalid so trigger a verification fail
                if (locationUnitConfigsByCoordinate.isEmpty()) {
                    throw new NotAvailableException("Locations for position[" + vec3DDouble + "]");
                }
                // copy ids from locations in list into the builder
                for (final UnitConfig locationUnitConfig : locationUnitConfigsByCoordinate) {
                    builder.addLocationId(locationUnitConfig.getId());
                }
            } catch (CouldNotPerformException | TimeoutException | ExecutionException ex) {
                throw new VerificationFailedException("Could not find location ids for position", ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new VerificationFailedException("Building location id list for position interrupted", ex);
            }
        }

        // validate all location ids in the builder
        for (final String locationId : localPositionState.getLocationIdList()) {
            try {
                Registries.getUnitRegistry().getUnitConfigById(locationId);
            } catch (CouldNotPerformException ex) {
                throw new VerificationFailedException("Invalid location id[" + locationId + "]", ex);
            }
        }

        return builder.build();
    }
}
