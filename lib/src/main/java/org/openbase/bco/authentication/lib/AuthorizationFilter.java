package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2018 openbase.org
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

import java.util.Map;

import org.openbase.bco.authentication.lib.AuthorizationHelper;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.pattern.AbstractFilter;
import org.openbase.jul.pattern.Observer;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthorizationFilter extends AbstractFilter<UnitConfig> {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthorizationFilter.class);

    private final SessionManager sessionManager;

    private Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> authorizationGroups;
    private Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations;

    /**
     * Create a new authorization filter using the default session manager.
     */
    public AuthorizationFilter() {
        this(SessionManager.getInstance());
    }

    /**
     * Create an authorization filter using the given session manager.
     *
     * @param sessionManager The session manager which is called to identify who is logged in.
     */
    public AuthorizationFilter(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * Set the authorization groups which are used to compute the read permissions in this filter.
     *
     * @param authorizationGroups A map of authorization groups indexed by their id.
     */
    public void setAuthorizationGroups(final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> authorizationGroups) {
        this.authorizationGroups = authorizationGroups;
    }

    /**
     * Set the locations which are used to compute the read permissions in this filter.
     *
     * @param locations A map of locations indexed by their id.
     */
    public void setLocations(final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations) {
        this.locations = locations;
    }

    /**
     * If somebody is logged in test if his ticket is still valid.
     *
     * @throws CouldNotPerformException If the ticket for the user has become invalid.
     */
    @Override
    public void beforeFilter() throws CouldNotPerformException {
        try {
            if (!JPService.getProperty(JPAuthentication.class).getValue()) {
                return;
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not check JPEnableAuthenticationProperty", ex);
        }
        try {
            CachedAuthenticationRemote.getRemote();
            sessionManager.isAuthenticated();
        } catch (CouldNotPerformException ex) {
            if (ex.getCause() instanceof InvalidStateException) {
                System.out.println("Could not check authenticated because in shutdown");
            } else {
                throw new CouldNotPerformException("Authentication failed", ex);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Verify a unitConfig by checking if the currently logged in user has read permissions for it.
     *
     * @param unitConfig The unitConfig which is verified.
     * @return True if the currently logged in user has read permissions and else false.
     * @throws CouldNotPerformException It the read permissions for the unit cannot be computed.
     */
    @Override
    public boolean filter(UnitConfig unitConfig) throws CouldNotPerformException {
        try {
            if (!JPService.getProperty(JPAuthentication.class).getValue()) {
                return true;
            }
        } catch (JPNotAvailableException ex) {
            return false;
        }

        return AuthorizationHelper.canRead(unitConfig, sessionManager.getUserAtClientId(), authorizationGroups, locations);
    }

    /**
     * Filtering will change when the login changes so this method will register
     * the observer as a login observer on the session manager.
     * {@inheritDoc}
     *
     * @param observer {@inheritDoc}
     */
    @Override
    public void addObserver(Observer observer) {
        sessionManager.addLoginObserver(observer);
    }

    /**
     * Remove an added observer from the session manager.
     * {@inheritDoc}
     *
     * @param observer {@inheritDoc}
     */
    @Override
    public void removeObserver(Observer observer) {
        sessionManager.removeLoginObserver(observer);
    }
}
