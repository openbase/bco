package org.openbase.bco.registry.unit.remote;

/*-
 * #%L
 * BCO Registry Unit Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openbase.bco.authentication.lib.AuthorisationHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthorizedUnitRegistryRemote extends UnitRegistryRemote {

    private final SessionManager sessionManager;

    public AuthorizedUnitRegistryRemote(SessionManager sessionManager) throws InstantiationException {
        super();
        this.sessionManager = sessionManager;
    }

    @Override
    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException, NotAvailableException {
        try {
            sessionManager.isAuthenticated();
        } catch (CouldNotPerformException ex) {
            // TODO: print warning and check all rights 
            throw new CouldNotPerformException("Authorisation failed", ex);
        }
        
        //TODO: getId if current client/user from SessionManager
        String id = "";
        List<UnitConfig> unitsWithReadAccess = new ArrayList<>();
        for(UnitConfig unitConfig : super.getUnitConfigs()) {
            if(AuthorisationHelper.hasReadAuthority(unitConfig, id, getAuthroisationGroupMap())) {
                unitsWithReadAccess.add(unitConfig);
            }
        }
        return unitsWithReadAccess;
    }

    private Map<String, UnitConfig> getAuthroisationGroupMap() throws CouldNotPerformException {
        Map<String, UnitConfig> authorisationGroupMap = new HashMap<>();
        for(UnitConfig unitConfig : getAuthorizationGroupUnitConfigRemoteRegistry().getMessages()) {
            authorisationGroupMap.put(unitConfig.getId(), unitConfig);
        }
        return authorisationGroupMap;
    }
}
