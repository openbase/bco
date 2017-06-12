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
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.AuthenticatorType.Authenticator;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import rst.timing.TimestampType.Timestamp;

/**
 *
 * @author Sebastian Fast <sfast@techfak.uni-bielefeld.de>
 */
public class AuthenticationClientHandlerImpl implements AuthenticationClientHandler {

    @Override
    public List<Object> handleKDCResponse(String clientID, byte[] hashedClientPassword, TicketSessionKeyWrapper wrapper) throws StreamCorruptedException, IOException {
        // decrypt TGS session key
        byte[] TGSSessionKey;

        TGSSessionKey = (byte[]) EncryptionHelper.decrypt(wrapper.getSessionKey(), hashedClientPassword);

        // create Authenticator with empty timestamp
        // set timestamp in initTGSRequest()
        Timestamp.Builder tb = Timestamp.newBuilder();
        tb.setTime(System.currentTimeMillis());
        Authenticator.Builder ab = Authenticator.newBuilder();
        ab.setClientId(clientID);
        ab.setTimestamp(tb.build());

        // create TicketAuthenticatorWrapper
        TicketAuthenticatorWrapper.Builder atb = TicketAuthenticatorWrapper.newBuilder();
        atb.setAuthenticator(EncryptionHelper.encrypt(ab.build(), TGSSessionKey));
        atb.setTicket(wrapper.getTicket());

        // create wrapper list
        List<Object> list = new ArrayList<>();
        list.add(atb.build());
        list.add(TGSSessionKey);

        return list;
    }

    @Override
    public List<Object> handleTGSResponse(String clientID, byte[] TGSSessionKey, TicketSessionKeyWrapper wrapper) throws StreamCorruptedException, IOException {
        // decrypt SS session key
        byte[] SSSessionKey;
        SSSessionKey = (byte[]) EncryptionHelper.decrypt(wrapper.getSessionKey(), TGSSessionKey);

        // create Authenticator with empty timestamp
        // set timestamp in initSSRequest()
        Authenticator.Builder ab = Authenticator.newBuilder();
        ab.setClientId(clientID);

        // create TicketAuthenticatorWrapper
        TicketAuthenticatorWrapper.Builder atb = TicketAuthenticatorWrapper.newBuilder();
        atb.setAuthenticator(EncryptionHelper.encrypt(ab.build(), SSSessionKey));
        atb.setTicket(wrapper.getTicket());

        // create wrapper list
        List<Object> list = new ArrayList<>();
        list.add(atb.build());
        list.add(SSSessionKey);

        return list;
    }

    @Override
    public TicketAuthenticatorWrapper initSSRequest(byte[] SSSessionKey, TicketAuthenticatorWrapper wrapper) throws StreamCorruptedException, IOException {
        // decrypt authenticator
        Authenticator authenticator = (Authenticator) EncryptionHelper.decrypt(wrapper.getAuthenticator(), SSSessionKey);

        // create Authenticator
        Timestamp.Builder tb = Timestamp.newBuilder();
        tb.setTime(System.currentTimeMillis());
        Authenticator.Builder ab = authenticator.toBuilder();
        ab.setTimestamp(tb.build());

        // create TicketAuthenticatorWrapper
        TicketAuthenticatorWrapper.Builder atb = wrapper.toBuilder();
        atb.setAuthenticator(EncryptionHelper.encrypt(ab.build(), SSSessionKey));

        return atb.build();
    }

    @Override
    public TicketAuthenticatorWrapper handleSSResponse(byte[] SSSessionKey, TicketAuthenticatorWrapper lastWrapper, TicketAuthenticatorWrapper currentWrapper) throws RejectedException, IOException {
        // decrypt authenticators
        Authenticator lastAuthenticator = (Authenticator) EncryptionHelper.decrypt(lastWrapper.getAuthenticator(), SSSessionKey);
        Authenticator currentAuthenticator = (Authenticator) EncryptionHelper.decrypt(currentWrapper.getAuthenticator(), SSSessionKey);

        // compare both timestamps
        this.validateTimestamp(lastAuthenticator.getTimestamp(), currentAuthenticator.getTimestamp());

        return currentWrapper;
    }

    private void validateTimestamp(Timestamp now, Timestamp then) throws RejectedException {
        if (now.getTime() != then.getTime()) {
            throw new RejectedException("Timestamps do not match");
        }
    }

}
