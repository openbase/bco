package org.openbase.bco.manager.user.lib;

/*-
 * #%L
 * BCO Manager User Library
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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

import org.openbase.bco.dal.lib.layer.unit.authorizationgroup.AuthorizationGroup;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.protobuf.MessageController;
import org.openbase.jul.iface.Configurable;
import org.openbase.jul.iface.Identifiable;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.authorizationgroup.AuthorizationGroupDataType.AuthorizationGroupData;

public interface AuthorizationGroupController extends Identifiable<String>, Configurable<String, UnitConfig>, AuthorizationGroup, MessageController<AuthorizationGroupData, AuthorizationGroupData.Builder> {

    void init(final UnitConfig config) throws InitializationException, InterruptedException;

}
