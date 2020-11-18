package org.openbase.bco.dal.lib.layer.unit;

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

import com.google.protobuf.AbstractMessage;
import org.openbase.bco.dal.lib.layer.service.OperationServiceFactoryProvider;
import org.openbase.bco.dal.lib.layer.service.UnitDataSourceFactoryProvider;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.Identifiable;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.ArrayList;
import java.util.List;

public interface HostUnitController<D extends AbstractMessage, DB extends D.Builder<DB>, C extends Identifiable<String>> extends BaseUnitController<D, DB>, HostUnit<D>, OperationServiceFactoryProvider, UnitDataSourceFactoryProvider {

    C getHostedUnitController(String id) throws NotAvailableException;

    /**
     * Method return a list of units that are currently managed by this host unit.
     * @return a list of unit ids.
     */
    List<C> getHostedUnitControllerList();

    /**
     * Method return a list of units that are currently managed by this host unit referred by its id.
     * @return a list of unit ids.
     */
    default List<String> getHostedUnitControllerIdList() {
        final ArrayList<String> controllerList = new ArrayList<>();
        for (C unitController : getHostedUnitControllerList()) {
            try {
                controllerList.add(unitController.getId());
            } catch (NotAvailableException ex) {
                new FatalImplementationErrorException("Could not resolve id of hosted controller!", this, ex);
            }
        }
        return controllerList;
    }

    /**
     * Method returns a list of unit configs of the units that are assigned to this host unit.
     *
     * @return the list of unit configs.
     *
     * @throws NotAvailableException in case the registry is not available.
     * @throws InterruptedException  in case the thread was externally interrupted.
     */
    List<UnitConfig> getHostedUnitConfigList() throws NotAvailableException, InterruptedException;
}
