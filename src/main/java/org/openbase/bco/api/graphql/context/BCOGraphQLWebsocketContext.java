package org.openbase.bco.api.graphql.context;

import graphql.kickstart.servlet.context.GraphQLWebSocketContext;
import org.dataloader.DataLoaderRegistry;
import org.openbase.jul.exception.NotAvailableException;

import javax.security.auth.Subject;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.Locale;

public class BCOGraphQLWebsocketContext extends AbstractBCOGraphQLContext implements GraphQLWebSocketContext {

    private final Session session;
    private final HandshakeRequest handshakeRequest;

    private final String token;
    private final String languageCode;

    public BCOGraphQLWebsocketContext(DataLoaderRegistry dataLoaderRegistry, Subject subject, Session session, HandshakeRequest handshakeRequest) {
        super(dataLoaderRegistry, subject);
        this.session = session;
        this.handshakeRequest = handshakeRequest;

        if (handshakeRequest.getHeaders().get("Authorization") != null) {
            this.token = handshakeRequest.getHeaders().get("Authorization").get(0);
        } else {
            this.token = null;
        }

        if (handshakeRequest.getHeaders().get("Accept-Language") != null) {
            final String language = handshakeRequest.getHeaders().get("Accept-Language").get(0);
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
