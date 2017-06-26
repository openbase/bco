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
     * @param clientID Identifier of the client - must be present in client database
     * @param clientPasswordHash Client's hashed password
     * @param wrapper TicketSessionKeyWrapper containing the TicketGrantingTicket and TGS session key
     * @return Returns a list of objects containing:
     * 1. An TicketAuthenticatorWrapperWrapper containing both the TicketGrantingTicket and Authenticator
     * 2. A SessionKey representing the TGS session key
     *
     * @throws StreamCorruptedException If the decryption of the session key fails, probably because the entered password was wrong.
     * @throws IOException If de- or encryption fail because of a general I/O error.
     */
    public static List<Object> handleKeyDistributionCenterResponse(String clientID, byte[] clientPasswordHash, TicketSessionKeyWrapper wrapper) throws StreamCorruptedException, IOException {
        // decrypt TGS session key
        byte[] ticketGrantingServiceSessionKey = (byte[]) EncryptionHelper.decrypt(wrapper.getSessionKey(), clientPasswordHash);

        // create Authenticator with empty timestamp
        // set timestamp in initTGSRequest()
        Authenticator.Builder authenticator = Authenticator.newBuilder();
        authenticator.setClientId(clientID);
        authenticator.setTimestamp(TimestampProcessor.getCurrentTimestamp());

        // create TicketAuthenticatorWrapper
        TicketAuthenticatorWrapper.Builder ticketAuthenticatorWrapper = TicketAuthenticatorWrapper.newBuilder();
        ticketAuthenticatorWrapper.setAuthenticator(EncryptionHelper.encrypt(authenticator.build(), ticketGrantingServiceSessionKey));
        ticketAuthenticatorWrapper.setTicket(wrapper.getTicket());

        // create wrapper list
        List<Object> list = new ArrayList<>();
        list.add(ticketAuthenticatorWrapper.build());
        list.add(ticketGrantingServiceSessionKey);

        return list;
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
     * @throws StreamCorruptedException If the decryption of the SS session key fails.
     * @throws IOException If de- or encryption fail because of a general I/O error.
     */
    public static List<Object> handleTicketGrantingServiceResponse(String clientID, byte[] ticketGrantingServiceSessionKey, TicketSessionKeyWrapper wrapper) throws StreamCorruptedException, IOException {
        // decrypt SS session key
        byte[] SSSessionKey = (byte[]) EncryptionHelper.decrypt(wrapper.getSessionKey(), ticketGrantingServiceSessionKey);

        // create Authenticator with empty timestamp
        // set timestamp in initSSRequest()
        Authenticator.Builder authenticator = Authenticator.newBuilder();
        authenticator.setClientId(clientID);

        // create TicketAuthenticatorWrapper
        TicketAuthenticatorWrapper.Builder ticketAuthenticatorWrapper = TicketAuthenticatorWrapper.newBuilder();
        ticketAuthenticatorWrapper.setAuthenticator(EncryptionHelper.encrypt(authenticator.build(), SSSessionKey));
        ticketAuthenticatorWrapper.setTicket(wrapper.getTicket());

        // create wrapper list
        List<Object> list = new ArrayList<>();
        list.add(ticketAuthenticatorWrapper.build());
        list.add(SSSessionKey);

        return list;
    }

    /**
     * Initializes a ServiceServer request
     * Sets current timestamp in Authenticator
     *
     * @param serviceServerSessionKey SS session key provided by handleTGSResponse()
     * @param wrapper TicketAuthenticatorWrapper wrapper that contains both encrypted Authenticator and CST
     * @return Returns a wrapper class containing both the CST and modified Authenticator
     *
     * @throws StreamCorruptedException If the decryption of the Authenticator fails.
     * @throws IOException If de- or encryption fail because of a general I/O error.
     */
    public static TicketAuthenticatorWrapper initServiceServerRequest(byte[] serviceServerSessionKey, TicketAuthenticatorWrapper wrapper) throws StreamCorruptedException, IOException {
        // decrypt authenticator
        Authenticator authenticator = (Authenticator) EncryptionHelper.decrypt(wrapper.getAuthenticator(), serviceServerSessionKey);

        // create Authenticator
        Authenticator.Builder ab = authenticator.toBuilder();
        ab.setTimestamp(TimestampProcessor.getCurrentTimestamp());

        // create TicketAuthenticatorWrapper
        TicketAuthenticatorWrapper.Builder ticketAuthenticatorWrapper = wrapper.toBuilder();
        ticketAuthenticatorWrapper.setAuthenticator(EncryptionHelper.encrypt(ab.build(), serviceServerSessionKey));

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
     */
    public static TicketAuthenticatorWrapper handleServiceServerResponse(final byte[] serviceServerSessionKey, final TicketAuthenticatorWrapper lastWrapper, final TicketAuthenticatorWrapper currentWrapper) throws RejectedException, IOException {
        // decrypt authenticators
        Authenticator lastAuthenticator = (Authenticator) EncryptionHelper.decrypt(lastWrapper.getAuthenticator(), serviceServerSessionKey);
        Authenticator currentAuthenticator = (Authenticator) EncryptionHelper.decrypt(currentWrapper.getAuthenticator(), serviceServerSessionKey);

        // compare both timestamps
        AuthenticationClientHandler.validateTimestamp(lastAuthenticator.getTimestamp(), currentAuthenticator.getTimestamp());

        return currentWrapper;
    }

    /**
     * Validate if the timestamps are equal
     *
     * @param now the first timestamp
     * @param then the second timestamp
     * @throws RejectedException thrown if the timestamps have a different time
     */
    public static void validateTimestamp(Timestamp now, Timestamp then) throws RejectedException {
        if (now.getTime() != then.getTime()) {
            throw new RejectedException("Timestamps do not match");
        }
    }
}
