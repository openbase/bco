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
import java.util.Map;
import org.openbase.bco.registry.lib.authorization.AuthorizationHelper;
import org.openbase.bco.authentication.lib.CachedAuthenticationRemote;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthorizationFilter extends AbstractFilter<UnitConfig> {
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthorizationFilter.class);
    
    private SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> authorizationGroupRegistry;
    private SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> locationRegistry;
    
    public AuthorizationFilter() {
    }

    public void setAuthorizationGroupRegistry(final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> authorizationGroupRegistry) {
        this.authorizationGroupRegistry = authorizationGroupRegistry;
    }

    public void setLocationRegistry(final SynchronizedRemoteRegistry<String, UnitConfig, UnitConfig.Builder> locationRegistry) {
        this.locationRegistry = locationRegistry;
    }
    
    @Override
    public void beforeFilter() throws CouldNotPerformException {
        try {
            CachedAuthenticationRemote.getRemote();
            SessionManager.getInstance().isAuthenticated();
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
    
    @Override
    public boolean verify(UnitConfig unitConfig) throws CouldNotPerformException {
        Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> authorizationGroups = null;
        if (authorizationGroupRegistry != null) {
            authorizationGroups = authorizationGroupRegistry.getEntryMap();
        }
        Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locations = null;
        if (locationRegistry != null) {
            locations = locationRegistry.getEntryMap();
        }
        
        try {
            return AuthorizationHelper.canRead(unitConfig, SessionManager.getInstance().getUserAtClientId(), authorizationGroups, locations);
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException(ex);
        } catch(NotAvailableException ex) {
            LOGGER.debug("Permission for unit ["+ScopeGenerator.generateStringRep(unitConfig.getScope())+"] not available!");
            return false;
        }
    }
}
