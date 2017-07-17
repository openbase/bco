package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 openbase.org
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

import com.google.protobuf.Descriptors;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.authentication.PermissionType.Permission;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthorisationHelper {

    public enum AuthrisationType {
        READ(Permission.READ_FIELD_NUMBER),
        WRITE(Permission.WRITE_FIELD_NUMBER),
        ACCES(Permission.ACCESS_FIELD_NUMBER);

        private final Descriptors.FieldDescriptor field;

        private AuthrisationType(final int fieldNumber) {
            this.field = Permission.getDescriptor().findFieldByNumber(fieldNumber);
        }

        public boolean hasPermission(Permission permission) {
            return (boolean) permission.getField(field);
        }
    }

    public static boolean hasAuthority(final UnitConfig unitConfig, final String userId, final Map<String, UnitConfig> AuthorisationGroupMap, final AuthrisationType type) throws CouldNotPerformException {
        return true;
    }

    public static boolean hasReadAuthority(final UnitConfig unitConfig, final String userId, final Map<String, UnitConfig> AuthorisationGroupMap) throws CouldNotPerformException {
        return hasAuthority(unitConfig, userId, AuthorisationGroupMap, AuthorisationHelper.AuthrisationType.READ);
    }

    public static boolean hasWriteAuthority(final UnitConfig unitConfig, final String userId, final Map<String, UnitConfig> AuthorisationGroupMap) throws CouldNotPerformException {
        return hasAuthority(unitConfig, userId, AuthorisationGroupMap, AuthorisationHelper.AuthrisationType.WRITE);
    }

    public static boolean hasAccesAuthority(final UnitConfig unitConfig, final String userId, final Map<String, UnitConfig> AuthorisationGroupMap) throws CouldNotPerformException {
        return hasAuthority(unitConfig, userId, AuthorisationGroupMap, AuthorisationHelper.AuthrisationType.ACCES);
    }
}
