package org.openbase.bco.api.graphql.context;

/*-
 * #%L
 * BCO GraphQL API
 * %%
 * Copyright (C) 2020 openbase.org
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

import graphql.kickstart.servlet.context.GraphQLWebSocketContext;
import org.dataloader.DataLoaderRegistry;
import org.openbase.jul.exception.NotAvailableException;

import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.Locale;

public class BCOGraphQLWebsocketContext extends AbstractBCOGraphQLContext implements GraphQLWebSocketContext {


    private final Session session;
    private final HandshakeRequest handshakeRequest;

    private final String token;
    private final String languageCode;

    public BCOGraphQLWebsocketContext(DataLoaderRegistry dataLoaderRegistry, Session session, HandshakeRequest handshakeRequest) {
        super(dataLoaderRegistry);
        this.session = session;
        this.handshakeRequest = handshakeRequest;

        if (handshakeRequest.getHeaders().get(GQLHeader.AUTHORIZATION.getKey()) != null) {
            this.token = handshakeRequest.getHeaders().get(GQLHeader.AUTHORIZATION.getKey()).get(0);
        } else {
            this.token = null;
        }

        if (handshakeRequest.getHeaders().get(GQLHeader.ACCEPT_LANGUAGE.getKey()) != null) {
            final String language = handshakeRequest.getHeaders().get(GQLHeader.ACCEPT_LANGUAGE.getKey()).get(0);
            this.languageCode = (language != null) ? language : Locale.getDefault().getLanguage();
        } else {
            this.languageCode = Locale.getDefault().getLanguage();
        }
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public HandshakeRequest getHandshakeRequest() {
        return handshakeRequest;
    }

    public String getToken() throws NotAvailableException {
        if (token == null) {
            throw new NotAvailableException("AuthToken");
        }

        return token;
    }

    public String getLanguageCode() {
        return this.languageCode;
    }
}
