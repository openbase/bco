package org.openbase.bco.authentication.lib;

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

import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;

/**
 * A pair of authentication and authorization user.
 */
public class AuthPair {

    /**
     * The id of the user who authenticate an action.
     */
    private final String authenticatedBy;

    /**
     * The id of the user who authorize an action.
     */
    private final String authorizedBy;

    /**
     * Creates a new empty auth pair which means the action is performed with other rights.
     */
    public AuthPair() {
        this.authenticatedBy = null;
        this.authorizedBy = null;
    }


    /**
     * Creates a new auth user pair.
     *
     * @param authenticatedBy the id of the user who authenticate an action.
     * @param authorizedBy    the id of the user who authorize an action.
     */
    public AuthPair(String authenticatedBy, String authorizedBy) {
        this.authenticatedBy = authenticatedBy;
        this.authorizedBy = authorizedBy;
    }

    /**
     * Creates a new auth user pair.
     *
     * @param authenticationUserClientPair pair identifying the responsible user for an action. If the pair contains
     *                                     a user id it is set as the authenticated id, else the client id.
     * @param authorizedBy                 the id of the user who authorize an action.
     */
    public AuthPair(final UserClientPair authenticationUserClientPair, final String authorizedBy) {
        if (authenticationUserClientPair.hasUserId() && authenticationUserClientPair.getUserId().isEmpty()) {
            this.authenticatedBy = authenticationUserClientPair.getUserId();
        } else {
            this.authenticatedBy = authenticationUserClientPair.getClientId();
        }
        this.authorizedBy = authorizedBy;
    }

    /**
     * The id of the user who authenticate an action.
     *
     * @return the user unit id as string.
     */
    public String getAuthenticatedBy() {
        return authenticatedBy;
    }

    /**
     * The id of the user who authorize an action.
     *
     * @return the user unit id as string.
     */
    public String getAuthorizedBy() {
        return authorizedBy;
    }
}
