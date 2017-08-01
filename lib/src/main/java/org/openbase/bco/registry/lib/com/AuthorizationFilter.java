package org.openbase.bco.registry.lib.com;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import org.openbase.bco.authentication.lib.AuthorizationHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthorizationFilter extends AbstractFilter<UnitConfig> {

    private SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> authorizationGroupRegistry;

    public AuthorizationFilter() {
    }

    public void setAuthorizationGroupRegistry(final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> authorizationGroupRegistry) {
        this.authorizationGroupRegistry = authorizationGroupRegistry;
    }

    @Override
    public void beforeFilter() throws CouldNotPerformException {
        try {
            SessionManager.getInstance().isAuthenticated();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Authentication failed", ex);
        }
    }

    @Override
    public boolean verify(UnitConfig unitConfig) {
        if (authorizationGroupRegistry != null) {
            return AuthorizationHelper.canAccess(unitConfig.getPermissionConfig(), SessionManager.getInstance().getUserId(), authorizationGroupRegistry.getEntryMap());
        } else {
            return AuthorizationHelper.canAccess(unitConfig.getPermissionConfig(), SessionManager.getInstance().getUserId(), null);
        }
    }
}
