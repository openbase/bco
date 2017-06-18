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

<<<<<<< HEAD
import java.util.ArrayList;
=======
import java.io.IOException;
import java.io.StreamCorruptedException;
>>>>>>> 3ea6bfd38d223ba917cdb4de595765c964e8581b
import java.util.List;
import org.openbase.jul.exception.RejectedException;
import rst.domotic.authentication.TicketAuthenticatorWrapperType;
import rst.domotic.authentication.AuthenticatorType;
import rst.domotic.authentication.TicketSessionKeyWrapperType;
import rst.timing.TimestampType;

/**
 *
<<<<<<< HEAD
 * @author Sebastian Fast <sfast@techfak.uni-bielefeld.de>
=======
 * @author <a href="mailto:sfast@techfak.uni-bielefeld.de">Sebastian Fast</a>
>>>>>>> 3ea6bfd38d223ba917cdb4de595765c964e8581b
 */
public class AuthenticationClientHandler {
    
    /**
     * Handles a KeyDistributionCenter (KDC) response
     * Decrypts the TicketGrantingServer (TGS) session key with client's hashed password
     * Creates an Authenticator containing the clientID and current timestamp encrypted with the TGS session key
     * @param clientId Identifier of the client - must be present in client database
     * @param hashedClientPassword Client's hashed password
     * @param wrapper TicketSessionKeyWrapper containing the TicketGrantingTicket and TGS session key
     * @return Returns a list of objects containing: 
     *         1. An TicketAuthenticatorWrapperWrapper containing both the TicketGrantingTicket and Authenticator
     *         2. A SessionKey representing the TGS session key
     *
     * @throws StreamCorruptedException If the decryption of the session key fails, probably because the entered password was wrong.
     * @throws IOException If de- or encryption fail because of a general I/O error.
     */
<<<<<<< HEAD
    public static List<Object> handleKDCResponse(String clientId, byte[] hashedClientPassword, TicketSessionKeyWrapperType.TicketSessionKeyWrapper wrapper) throws RejectedException {
        // decrypt TGS session key
        byte[] TGSSessionKey = (byte[]) EncryptionHelper.decrypt(wrapper.getSessionKey(), hashedClientPassword);

        // create Authenticator with empty timestamp
        // set timestamp in initTGSRequest()
        TimestampType.Timestamp.Builder tb = TimestampType.Timestamp.newBuilder();
        tb.setTime(System.currentTimeMillis());
        AuthenticatorType.Authenticator.Builder ab = AuthenticatorType.Authenticator.newBuilder();
        ab.setClientId(clientId);
        ab.setTimestamp(tb.build());

        // create TicketAuthenticatorWrapper
        TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper.Builder atb = TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper.newBuilder();
        atb.setAuthenticator(EncryptionHelper.encrypt(ab.build(), TGSSessionKey));
        atb.setTicket(wrapper.getTicket());

        // create wrapper list
        List<Object> list = new ArrayList<>();
        list.add(atb.build());
        list.add(TGSSessionKey);

        return list;
    }
=======
    public List<Object> handleKDCResponse(String clientID, byte[] hashedClientPassword, TicketSessionKeyWrapper wrapper) throws IOException;
>>>>>>> 3ea6bfd38d223ba917cdb4de595765c964e8581b

    /**
     * Handles a TGS response
     * Decrypts the ServiceServer (SS) session key with TGS session key
     * Creates an Authenticator containing the clientID and empty timestamp encrypted with the SS session key
     * @param clientID Identifier of the client - must be present in client database
     * @param TGSSessionKey TGS session key provided by handleKDCResponse()
     * @param wrapper TicketSessionKeyWrapper containing the ClientServerTicket and SS session key
     * @return Returns a list of objects containing: 
     *         1. An TicketAuthenticatorWrapperWrapper containing both the ClientServerTicket and Authenticator
     *         2. A SessionKey representing the SS session key
     *
     * @throws StreamCorruptedException If the decryption of the SS session key fails.
     * @throws IOException If de- or encryption fail because of a general I/O error.
     */
<<<<<<< HEAD
    public static List<Object> handleTGSResponse(String clientID, byte[] TGSSessionKey, TicketSessionKeyWrapperType.TicketSessionKeyWrapper wrapper) throws RejectedException {
        // decrypt SS session key
        byte[] SSSessionKey = (byte[]) EncryptionHelper.decrypt(wrapper.getSessionKey(), TGSSessionKey);

        // create Authenticator with empty timestamp
        // set timestamp in initSSRequest()
        AuthenticatorType.Authenticator.Builder ab = AuthenticatorType.Authenticator.newBuilder();
        ab.setClientId(clientID);

        // create TicketAuthenticatorWrapper
        TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper.Builder atb = TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper.newBuilder();
        atb.setAuthenticator(EncryptionHelper.encrypt(ab.build(), SSSessionKey));
        atb.setTicket(wrapper.getTicket());

        // create wrapper list
        List<Object> list = new ArrayList<>();
        list.add(atb.build());
        list.add(SSSessionKey);

        return list;
    }
  
=======
    public List<Object> handleTGSResponse(String clientID, byte[] TGSSessionKey, TicketSessionKeyWrapper wrapper) throws IOException;
    
>>>>>>> 3ea6bfd38d223ba917cdb4de595765c964e8581b
    /**
     * Initializes a SS request
     * Sets current timestamp in Authenticator
     * @param SSSessionKey SS session key provided by handleTGSResponse()
<<<<<<< HEAD
     * @param wrapper TicketAuthenticatorWrapper wrapper that contains both encrypted Authenticator and ClientServerTicket
     * @return Returns a wrapper class containing both the ClientServerTicket and modified Authenticator
     * @TODO: Exception description
     */
    public static TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper initSSRequest(byte[] SSSessionKey, TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper wrapper) throws RejectedException {
        // decrypt authenticator
        AuthenticatorType.Authenticator authenticator = (AuthenticatorType.Authenticator) EncryptionHelper.decrypt(wrapper.getAuthenticator(), SSSessionKey);

        // create Authenticator
        TimestampType.Timestamp.Builder tb = TimestampType.Timestamp.newBuilder();
        tb.setTime(System.currentTimeMillis());
        AuthenticatorType.Authenticator.Builder ab = authenticator.toBuilder();
        ab.setTimestamp(tb.build());

        // create TicketAuthenticatorWrapper
        TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper.Builder atb = wrapper.toBuilder();
        atb.setAuthenticator(EncryptionHelper.encrypt(ab.build(), SSSessionKey));

        return atb.build();
    }

=======
     * @param wrapper TicketAuthenticatorWrapper wrapper that contains both encrypted Authenticator and CST
     * @return Returns a wrapper class containing both the CST and modified Authenticator
     *
     * @throws StreamCorruptedException If the decryption of the Authenticator fails.
     * @throws IOException If de- or encryption fail because of a general I/O error.
     */
    public TicketAuthenticatorWrapper initSSRequest(byte[] SSSessionKey, TicketAuthenticatorWrapper wrapper) throws StreamCorruptedException, IOException;
    
>>>>>>> 3ea6bfd38d223ba917cdb4de595765c964e8581b
    /**
     * Handles a SS response
     * Decrypts Authenticator of both last- and currentWrapper with SSSessionKey
     * Compares timestamps of both Authenticators with each other
     * @param SSSessionKey SS session key provided by handleTGSResponse()
     * @param lastWrapper Last TicketAuthenticatorWrapper provided by either handleTGSResponse() or handleSSResponse()
     * @param currentWrapper Current TicketAuthenticatorWrapper provided by (Remote?)
<<<<<<< HEAD
     * @return Returns an TicketAuthenticatorWrapperWrapper containing both the ClientServerTicket and Authenticator
     * @throws RejectedException Throws, if timestamps do not match
     * @TODO: Exception description
     */
    public static TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper handleSSResponse(byte[] SSSessionKey, TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper lastWrapper, TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper currentWrapper) throws RejectedException {
        // decrypt authenticators
        AuthenticatorType.Authenticator lastAuthenticator = (AuthenticatorType.Authenticator) EncryptionHelper.decrypt(lastWrapper.getAuthenticator(), SSSessionKey);
        AuthenticatorType.Authenticator currentAuthenticator = (AuthenticatorType.Authenticator) EncryptionHelper.decrypt(currentWrapper.getAuthenticator(), SSSessionKey);

        // compare both timestamps
        validateTimestamp(lastAuthenticator.getTimestamp(), currentAuthenticator.getTimestamp());

        return currentWrapper;
    }

    private static void validateTimestamp(TimestampType.Timestamp now, TimestampType.Timestamp then) throws RejectedException {
        if (now.getTime() != then.getTime()) {
            throw new RejectedException("Timestamps do not match");
        }
    }

=======
     * @return Returns an TicketAuthenticatorWrapperWrapper containing both the CST and Authenticator
     *
     * @throws RejectedException If the timestamps do not match.
     * @throws IOException If the decryption of the Authenticators using the SSSessionKey fails.
     */
    public TicketAuthenticatorWrapper handleSSResponse(byte[] SSSessionKey, TicketAuthenticatorWrapper lastWrapper, TicketAuthenticatorWrapper currentWrapper) throws RejectedException, IOException;
>>>>>>> 3ea6bfd38d223ba917cdb4de595765c964e8581b
}
