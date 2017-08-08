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
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.BadPaddingException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.AuthenticatorType.Authenticator;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import rst.timing.TimestampType.Timestamp;

/**
 *
 * @author <a href="mailto:sfast@techfak.uni-bielefeld.de">Sebastian Fast</a>
 */
public class AuthenticationClientHandler {

    /**
     * Handles a KeyDistributionCenter (KDC) response
     * Decrypts the TicketGrantingServer (TGS) session key with client's hashed password
     * Creates an Authenticator containing the clientID and current timestamp encrypted with the TGS session key
     *
     * @param id Identifier of the client or user
     * @param key hashed password or private key of user or client respectively
     * @param isUser true if ticket was requested for a user. This is important for the decryption method to be chosen.
     * @param wrapper TicketSessionKeyWrapper containing the TicketGrantingTicket and TGS session key
     * @return Returns a list of objects containing:
     * 1. An TicketAuthenticatorWrapperWrapper containing both the TicketGrantingTicket and Authenticator
     * 2. A SessionKey representing the TGS session key
     *
     * @throws javax.crypto.BadPaddingException If the decryption of the session key fails, probably because the entered key was wrong.
     * @throws IOException If de- or encryption fail because of a general I/O error.
     */
    public static List<Object> handleKeyDistributionCenterResponse(String id, byte[] userKey, byte[] clientKey, TicketSessionKeyWrapper wrapper) throws IOException, BadPaddingException {
        byte[] ticketGrantingServiceSessionKey = wrapper.getSessionKey().toByteArray();

        // decrypt TGS session key
        if (clientKey != null) {
            ticketGrantingServiceSessionKey = EncryptionHelper.decrypt(ticketGrantingServiceSessionKey, clientKey, byte[].class, false);
        }
        if (userKey != null) {
            ticketGrantingServiceSessionKey = EncryptionHelper.decrypt(ticketGrantingServiceSessionKey, userKey, byte[].class, true);
        }

        // create Authenticator with empty timestamp
        // set timestamp in initTGSRequest()
        Authenticator.Builder authenticator = Authenticator.newBuilder();
        authenticator.setClientId(id);
        authenticator.setTimestamp(TimestampProcessor.getCurrentTimestamp());

        // create TicketAuthenticatorWrapper
        TicketAuthenticatorWrapper.Builder ticketAuthenticatorWrapper = TicketAuthenticatorWrapper.newBuilder();
        ticketAuthenticatorWrapper.setAuthenticator(EncryptionHelper.encryptSymmetric(authenticator.build(), ticketGrantingServiceSessionKey));
        ticketAuthenticatorWrapper.setTicket(wrapper.getTicket());

        // create wrapper list
        List<Object> list = new ArrayList<>();
        list.add(ticketAuthenticatorWrapper.build());
        list.add(ticketGrantingServiceSessionKey);

        return list;
    }

    public static List<Object> handleKeyDistributionCenterResponse(String id, byte[] key, boolean isUser, TicketSessionKeyWrapper wrapper) throws IOException, BadPaddingException {
        if (isUser) {
            return handleKeyDistributionCenterResponse(id, key, null, wrapper);
        }

        return handleKeyDistributionCenterResponse(id, null, key, wrapper);
    }

    /**
     * Handles a TicketGrantingService response
     * Decrypts the ServiceServer (SS) session key with TGS session key
     * Creates an Authenticator containing the clientID and empty timestamp encrypted with the SS session key
     *
     * @param clientID Identifier of the client - must be present in client database
     * @param ticketGrantingServiceSessionKey TGS session key provided by handleKDCResponse()
     * @param wrapper TicketSessionKeyWrapper containing the ClientServerTicket and SS session key
     * @return Returns a list of objects containing:
     * 1. An TicketAuthenticatorWrapperWrapper containing both the ClientServerTicket and Authenticator
     * 2. A SessionKey representing the SS session key
     *
     * @throws IOException If de- or encryption fail because of a general I/O error.
     * @throws javax.crypto.BadPaddingException If the decryption of the service server session key fails because of an incorrect key.
     */
    public static List<Object> handleTicketGrantingServiceResponse(String clientID, byte[] ticketGrantingServiceSessionKey, TicketSessionKeyWrapper wrapper) throws IOException, BadPaddingException {
        // decrypt SS session key
        byte[] SSSessionKey = EncryptionHelper.decryptSymmetric(wrapper.getSessionKey(), ticketGrantingServiceSessionKey, byte[].class);

        // create Authenticator with empty timestamp
        // set timestamp in initSSRequest()
        Authenticator.Builder authenticator = Authenticator.newBuilder();
        authenticator.setClientId(clientID);

        // create TicketAuthenticatorWrapper
        TicketAuthenticatorWrapper.Builder ticketAuthenticatorWrapper = TicketAuthenticatorWrapper.newBuilder();
        ticketAuthenticatorWrapper.setAuthenticator(EncryptionHelper.encryptSymmetric(authenticator.build(), SSSessionKey));
        ticketAuthenticatorWrapper.setTicket(wrapper.getTicket());

        // create wrapper list
        List<Object> list = new ArrayList<>();
        list.add(ticketAuthenticatorWrapper.build());
        list.add(SSSessionKey);

        return list;
    }

    /**
     * Initializes a ServiceServer request by setting the current timestamp in the authenticator.
     *
     * @param serviceServerSessionKey SS session key provided by handleTGSResponse()
     * @param wrapper TicketAuthenticatorWrapper wrapper that contains both encrypted Authenticator and CST
     * @return Returns a wrapper class containing both the CST and modified Authenticator
     *
     * @throws javax.crypto.BadPaddingException If the decryption of the Authenticator fails.
     * @throws IOException If de- or encryption fail because of a general I/O error.
     */
    public static TicketAuthenticatorWrapper initServiceServerRequest(byte[] serviceServerSessionKey, TicketAuthenticatorWrapper wrapper) throws IOException, BadPaddingException {
        // decrypt authenticator
        Authenticator.Builder authenticator = EncryptionHelper.decryptSymmetric(wrapper.getAuthenticator(), serviceServerSessionKey, Authenticator.class).toBuilder();

        // update timestamp
        authenticator.setTimestamp(TimestampProcessor.getCurrentTimestamp());

        // update ticket authenticatorWrapper
        TicketAuthenticatorWrapper.Builder ticketAuthenticatorWrapper = wrapper.toBuilder();
        ticketAuthenticatorWrapper.setAuthenticator(EncryptionHelper.encryptSymmetric(authenticator.build(), serviceServerSessionKey));

        return ticketAuthenticatorWrapper.build();
    }

    /**
     * Handles a ServiceServer response
     * Decrypts Authenticator of both last- and currentWrapper with ServiceServerSessionKey
     * Compares timestamps of both Authenticators with each other
     *
     * @param serviceServerSessionKey SS session key provided by handleTGSResponse()
     * @param lastWrapper Last TicketAuthenticatorWrapper provided by either handleTGSResponse() or handleSSResponse()
     * @param currentWrapper Current TicketAuthenticatorWrapper provided by (Remote?)
     * @return Returns an TicketAuthenticatorWrapperWrapper containing both the CST and Authenticator
     *
     * @throws RejectedException If the timestamps do not match.
     * @throws IOException If the decryption of the Authenticators using the SSSessionKey fails.
     * @throws javax.crypto.BadPaddingException if an incorrect key is used
     */
    public static TicketAuthenticatorWrapper handleServiceServerResponse(final byte[] serviceServerSessionKey, final TicketAuthenticatorWrapper lastWrapper, final TicketAuthenticatorWrapper currentWrapper) throws RejectedException, IOException, BadPaddingException {
        // decrypt authenticators
        Authenticator lastAuthenticator = EncryptionHelper.decryptSymmetric(lastWrapper.getAuthenticator(), serviceServerSessionKey, Authenticator.class);
        Authenticator currentAuthenticator = EncryptionHelper.decryptSymmetric(currentWrapper.getAuthenticator(), serviceServerSessionKey, Authenticator.class);

        // compare both timestamps
        AuthenticationClientHandler.validateTimestamp(lastAuthenticator.getTimestamp(), currentAuthenticator.getTimestamp());

        return currentWrapper;
    }

    /**
     * Validate if the timestamps are equal.
     * Compares now + 1 == then, because server adds +1 to authenticator's timestamp.
     *
     * @param now the first timestamp
     * @param then the second timestamp
     * @throws RejectedException thrown if the timestamps have a different time
     */
    public static void validateTimestamp(Timestamp now, Timestamp then) throws RejectedException {
        if (now.getTime() + 1 != then.getTime()) {
            throw new RejectedException("Timestamps do not match");
        }
    }
}
