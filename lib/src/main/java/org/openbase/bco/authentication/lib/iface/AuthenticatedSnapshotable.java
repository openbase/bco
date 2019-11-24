package org.openbase.bco.authentication.lib.iface;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2019 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Snapshotable;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.type.domotic.action.SnapshotType;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;

import java.util.concurrent.Future;

public interface AuthenticatedSnapshotable extends Snapshotable<SnapshotType.Snapshot> {

    /**
     * @param authenticatedSnapshot the authenticated value providing a encrypted snapshot.
     *
     * @return the authenticated value providing feedback about this action.
     *
     * @deprecated Deprecated because not yet fully compatible with unit allocation.
     */
    @RPCMethod
    @Deprecated
    Future<AuthenticatedValue> restoreSnapshotAuthenticated(final AuthenticatedValue authenticatedSnapshot);
}
