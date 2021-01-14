package org.openbase.bco.app.cloudconnector;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import org.openbase.bco.authentication.lib.TokenStore;
import org.openbase.jul.exception.NotAvailableException;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class CloudConnectorTokenStore extends TokenStore {

    static final String DEFAULT_TOKEN_STORE_FILENAME = "cloud_connector_token_store.json";

    private static final String SEPARATOR = "@";
    private static final String CLOUD_POSTFIX = "Cloud";
    private static final String BCO_POSTFIX = "BCO";
    private static final String CLOUD_CONNECTOR_TOKEN_KEY = "CLOUD_CONNECTOR_TOKEN";

    public String getCloudToken(final String userId) throws NotAvailableException {
        return getToken(userId + SEPARATOR + CLOUD_POSTFIX);
    }

    public String getBCOToken(final String userId) throws NotAvailableException {
        return getToken(userId + SEPARATOR + BCO_POSTFIX);
    }

    public String getCloudConnectorToken() throws NotAvailableException {
        return getToken(CLOUD_CONNECTOR_TOKEN_KEY);
    }

    public void addCloudToken(final String userId, final String token) {
        addToken(userId + SEPARATOR + CLOUD_POSTFIX, token);
    }

    public void addBCOToken(final String userId, final String token) {
        addToken(userId + SEPARATOR + BCO_POSTFIX, token);
    }

    public void addCloudConnectorToken(final String token) {
        addToken(CLOUD_CONNECTOR_TOKEN_KEY, token);
    }

    public boolean hasCloudToken(final String userId) {
        return hasEntry(userId + SEPARATOR + CLOUD_POSTFIX);
    }

    public boolean hasBCOToken(final String userId) {
        return hasEntry(userId + SEPARATOR + BCO_POSTFIX);
    }

    public boolean hasCloudConnectorToken() {
        return hasEntry(CLOUD_CONNECTOR_TOKEN_KEY);
    }

    public void removeCloudToken(final String userId) {
        removeToken(userId + SEPARATOR + CLOUD_POSTFIX);
    }

    public void removeBCOToken(final String userId) {
        removeToken(userId + SEPARATOR + BCO_POSTFIX);
    }

    public Map<String, String> getCloudEntries() {
        final Map<String, String> map = new HashMap<>();
        for (final Entry<String, String> entry : getEntryMap().entrySet()) {
            final String[] split = entry.getKey().split(SEPARATOR);
            if (split.length == 2 && split[1].equals(CLOUD_POSTFIX)) {
                map.put(split[0], entry.getValue());
            }
        }
        return map;
    }
}
