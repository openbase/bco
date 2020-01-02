package org.openbase.bco.dal.control.layer.unit.authorizationgroup;

/*-
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.dal.control.layer.unit.AbstractBaseUnitController;
import org.openbase.bco.dal.lib.layer.unit.authorizationgroup.AuthorizationGroupController;
import org.openbase.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import org.openbase.type.domotic.unit.authorizationgroup.AuthorizationGroupDataType.AuthorizationGroupData;

public class AuthorizationGroupControllerImpl extends AbstractBaseUnitController<AuthorizationGroupData, AuthorizationGroupData.Builder> implements AuthorizationGroupController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthorizationGroupData.getDefaultInstance()));
    }

    public AuthorizationGroupControllerImpl() throws InstantiationException {
        super(AuthorizationGroupData.newBuilder());
    }
}
