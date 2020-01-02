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

import com.google.protobuf.ByteString;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.authentication.AuthenticatorType.Authenticator;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import org.openbase.type.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;
import org.openbase.type.timing.TimestampType.Timestamp;

/**
 * @author <a href="mailto:sfast@techfak.uni-bielefeld.de">Sebastian Fast</a>
 */
public class AuthenticationClientHandler {

    /**
     * Handles a KeyDistributionCenter (KDC) response
     * Decrypts the TicketGrantingServer (TGS) session key with client's hashed password
     * Creates an Authenticator containing the clientID and current timestamp encrypted with the TGS session key
     *
     * @param userClientPair    pair identifying the user and client logged in
     * @param userCredentials   credentials of the user logged in. Only required if the pair contains a user id.
     * @param clientCredentials credentials of the client logged in. Only required if the pair contains a client id.
     * @param wrapper           TicketSessionKeyWrapper containing the TicketGrantingTicket and TGS session key
     *
     * @return Returns a pair containing:
     * 1. An TicketAuthenticatorWrapperWrapper containing both the TicketGrantingTicket and Authenticator
     * 2. A SessionKey representing the TGS session key
     *
     * @throws CouldNotPerformException If de-/encryption of the ticket fails
     */
    public static TicketWrapperSessionKeyPair handleKeyDistributionCenterResponse(final UserClientPair userClientPair, final LoginCredentials userCredentials, final LoginCredentials clientCredentials, final TicketSessionKeyWrapper wrapper) throws CouldNotPerformException {
        byte[] ticketGrantingServiceSessionKey = wrapper.getSessionKey().toByteArray();

        // decrypt TGS session key
        if (userClientPair.hasClientId() && !userClientPair.getClientId().isEmpty()) {
            ticketGrantingServiceSessionKey = EncryptionHelper.decrypt(ticketGrantingServiceSessionKey, clientCredentials, byte[].class);
        }
        if (userClientPair.hasUserId() && !userClientPair.getUserId().isEmpty()) {
            ticketGrantingServiceSessionKey = EncryptionHelper.decrypt(ticketGrantingServiceSessionKey, userCredentials, byte[].class);
        }

        // create Authenticator with empty timestamp
        // set timestamp in initTGSRequest()
        Authenticator.Builder authenticator = Authenticator.newBuilder();
        authenticator.setUserClientPair(userClientPair);
        authenticator.setTimestamp(TimestampProcessor.getCurrentTimestamp());

        // create a pair containing session key and wrapper with authenticator encrypted with session key.
        return createTicketWrapperSessionKeyPair(ticketGrantingServiceSessionKey, authenticator.build(), wrapper.getTicket());
    }

    /**
     * Handles a TicketGrantingService response
     * Decrypts the ServiceServer (SS) session key with TGS session key
     * Creates an Authenticator containing the clientID and empty timestamp encrypted with the SS session key
     *
     * @param userClientPair                  pair identifying the user and client logged in
     * @param ticketGrantingServiceSessionKey TGS session key provided by handleKDCResponse()
     * @param wrapper                         TicketSessionKeyWrapper containing the ClientServerTicket and SS session key
     *
     * @return Returns a pair containing:
     * 1. An TicketAuthenticatorWrapperWrapper containing both the ClientServerTicket and Authenticator
     * 2. A SessionKey representing the SS session key
     *
     * @throws CouldNotPerformException If de-/encryption of the ticket fails
     */
    public static TicketWrapperSessionKeyPair handleTicketGrantingServiceResponse(final UserClientPair userClientPair, final byte[] ticketGrantingServiceSessionKey, final TicketSessionKeyWrapper wrapper) throws CouldNotPerformException {
        // decrypt SS session key
        byte[] SSSessionKey = EncryptionHelper.decryptSymmetric(wrapper.getSessionKey(), ticketGrantingServiceSessionKey, byte[].class);

        // create Authenticator with empty timestamp
        // set timestamp in initSSRequest()
        Authenticator authenticator = Authenticator.newBuilder().setUserClientPair(userClientPair).build();

        // create a pair containing session key and wrapper with authenticator encrypted with session key.
        return createTicketWrapperSessionKeyPair(SSSessionKey, authenticator, wrapper.getTicket());
    }

    /**
     * Create a pair of a session key and a ticket wrapper.
     *
     * @param sessionKey    the session key put into the pair.
     * @param authenticator authenticator will be encrypted with the session key and added to the ticket wrapper.
     * @param ticket        encrypted ticket added to the ticket wrapper.
     *
     * @return a pair bundling the ticket wrapper and the session key.
     *
     * @throws CouldNotPerformException if encryption of the authenticator with the session key fails.
     */
    private static TicketWrapperSessionKeyPair createTicketWrapperSessionKeyPair(final byte[] sessionKey, final Authenticator authenticator, final ByteString ticket) throws CouldNotPerformException {
        // create TicketAuthenticatorWrapper
        TicketAuthenticatorWrapper.Builder ticketAuthenticatorWrapper = TicketAuthenticatorWrapper.newBuilder();
        ticketAuthenticatorWrapper.setAuthenticator(EncryptionHelper.encryptSymmetric(authenticator, sessionKey));
        ticketAuthenticatorWrapper.setTicket(ticket);

        return new TicketWrapperSessionKeyPair(sessionKey, ticketAuthenticatorWrapper.build());
    }

    /**
     * Initializes a ServiceServer request by setting the current timestamp in the authenticator.
     *
     * @param serviceServerSessionKey SS session key provided by handleTGSResponse()
     * @param wrapper                 TicketAuthenticatorWrapper wrapper that contains both encrypted Authenticator and CST
     *
     * @return Returns a wrapper class containing both the CST and modified Authenticator
     *
     * @throws CouldNotPerformException if de-/encrypting the authenticator inside the wrapper fails
     */
    public static TicketAuthenticatorWrapper initServiceServerRequest(final byte[] serviceServerSessionKey, final TicketAuthenticatorWrapper wrapper) throws CouldNotPerformException {
        // decrypt authenticator
        final Authenticator.Builder authenticator = EncryptionHelper.decryptSymmetric(wrapper.getAuthenticator(), serviceServerSessionKey, Authenticator.class).toBuilder();

        // update timestamp
        authenticator.setTimestamp(TimestampProcessor.getCurrentTimestamp());

        // update ticket authenticatorWrapper
        final TicketAuthenticatorWrapper.Builder ticketAuthenticatorWrapper = wrapper.toBuilder();
        ticketAuthenticatorWrapper.setAuthenticator(EncryptionHelper.encryptSymmetric(authenticator.build(), serviceServerSessionKey));

        return ticketAuthenticatorWrapper.build();
    }

    public static TicketAuthenticatorWrapper initServiceServerRequest(final TicketWrapperSessionKeyPair ticketWrapperSessionKeyPair) throws CouldNotPerformException {
        return initServiceServerRequest(ticketWrapperSessionKeyPair.getSessionKey(), ticketWrapperSessionKeyPair.getTicketAuthenticatorWrapper());
    }

    /**
     * Handles a ServiceServer response
     * Decrypts Authenticator of both last- and currentWrapper with ServiceServerSessionKey
     * Compares timestamps of both Authenticators with each other
     *
     * @param serviceServerSessionKey SS session key provided by handleTGSResponse()
     * @param lastWrapper             Last TicketAuthenticatorWrapper provided by either handleTGSResponse() or handleSSResponse()
     * @param currentWrapper          Current TicketAuthenticatorWrapper provided by (Remote?)
     *
     * @return Returns an TicketAuthenticatorWrapperWrapper containing both the CST and Authenticator
     *
     * @throws CouldNotPerformException If de-/encryption of the authenticator fails or the timestamps do not match
     */
    public static TicketAuthenticatorWrapper handleServiceServerResponse(final byte[] serviceServerSessionKey, final TicketAuthenticatorWrapper lastWrapper, final TicketAuthenticatorWrapper currentWrapper) throws CouldNotPerformException {
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
     * @param now  the first timestamp
     * @param then the second timestamp
     *
     * @throws RejectedException thrown if the timestamps have a different time
     */
    public static void validateTimestamp(final Timestamp now, final Timestamp then) throws RejectedException {
        if (now.getTime() + 1 != then.getTime()) {
            throw new RejectedException("Timestamps do not match");
        }
    }

    /**
     * Internal wrapper type used to bundle a session key with a ticket wrapper.
     */
    public static class TicketWrapperSessionKeyPair {

        private final byte[] sessionKey;
        private final TicketAuthenticatorWrapper ticketAuthenticatorWrapper;

        TicketWrapperSessionKeyPair(final byte[] sessionKey, final TicketAuthenticatorWrapper ticketAuthenticatorWrapper) {
            this.sessionKey = sessionKey;
            this.ticketAuthenticatorWrapper = ticketAuthenticatorWrapper;
        }

        /**
         * Get the stored session key.
         *
         * @return the session key.
         */
        public byte[] getSessionKey() {
            return sessionKey;
        }

        /**
         * Get the stored ticket wrapper.
         *
         * @return the wrapper.
         */
        public TicketAuthenticatorWrapper getTicketAuthenticatorWrapper() {
            return ticketAuthenticatorWrapper;
        }
    }
}
