package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2020 openbase.org
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

import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.processing.json.JSonObjectFileProcessor;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Class for a protected token store.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TokenStore extends AbstractProtectedStore<String, Map> {

    /**
     * Create a new token store.
     */
    public TokenStore() {
        super(new JSonObjectFileProcessor<>(Map.class));
    }

    /**
     * Get a token for a given identifier.
     *
     * @param id the identifier of the token
     * @return the token belonging to the identifier
     * @throws NotAvailableException if no token for the identifier is available
     */
    public String getToken(final String id) throws NotAvailableException {
        return getEntry(id);
    }

    /**
     * Add or replace a token in the store for a given id.
     *
     * @param id    the id for which the token is added or replaced
     * @param token the token
     */
    public void addToken(final String id, final String token) {
        addEntry(id, token);
    }

    /**
     * Remove a token from the store.
     *
     * @param id the identifier for the token
     */
    public void removeToken(final String id) {
        removeEntry(id);
    }

    /**
     * {@inheritDoc}
     *
     * @param dataCollection {@inheritDoc}
     * @param map            {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void load(final Map dataCollection, final Map<String, String> map) {
        for (Object object : dataCollection.entrySet()) {
            final Entry<String, String> entry = (Entry<String, String>) object;
            map.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param map {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected Map save(final Map<String, String> map) {
        return map;
    }
}
