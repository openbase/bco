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

import java.util.ArrayList;
import java.util.List;
import org.openbase.jul.exception.RejectedException;
import rst.domotic.authentication.TicketAuthenticatorWrapperType;
import rst.domotic.authentication.AuthenticatorType;
import rst.domotic.authentication.TicketSessionKeyWrapperType;
import rst.timing.TimestampType;

/**
 *
 * @author Sebastian Fast <sfast@techfak.uni-bielefeld.de>
 */
public class AuthenticationClientHandlerImpl implements AuthenticationClientHandler {
    
    @Override
    public List<Object> handleKDCResponse(String clientID, byte[] hashedClientPassword, TicketSessionKeyWrapperType.TicketSessionKeyWrapper wrapper) throws RejectedException {
        // decrypt TGS session key
        byte[] TGSSessionKey = (byte[]) EncryptionHelper.decrypt(wrapper.getSessionKey(), hashedClientPassword);

        // create Authenticator with empty timestamp
        // set timestamp in initTGSRequest()
        TimestampType.Timestamp.Builder tb = TimestampType.Timestamp.newBuilder();
        tb.setTime(System.currentTimeMillis());
        AuthenticatorType.Authenticator.Builder ab = AuthenticatorType.Authenticator.newBuilder();
        ab.setClientId(clientID);
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

    @Override
    public List<Object> handleTGSResponse(String clientID, byte[] TGSSessionKey, TicketSessionKeyWrapperType.TicketSessionKeyWrapper wrapper) throws RejectedException {
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
        List<Object> list = new ArrayList<Object>();
        list.add(atb.build());
        list.add(SSSessionKey);

        return list;
    }

    @Override
    public TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper initSSRequest(byte[] SSSessionKey, TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper wrapper) throws RejectedException {
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

    @Override
    public TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper handleSSResponse(byte[] SSSessionKey, TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper lastWrapper, TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper currentWrapper) throws RejectedException {
        // decrypt authenticators
        AuthenticatorType.Authenticator lastAuthenticator = (AuthenticatorType.Authenticator) EncryptionHelper.decrypt(lastWrapper.getAuthenticator(), SSSessionKey);
        AuthenticatorType.Authenticator currentAuthenticator = (AuthenticatorType.Authenticator) EncryptionHelper.decrypt(currentWrapper.getAuthenticator(), SSSessionKey);

        // compare both timestamps
        
        this.validateTimestamp(lastAuthenticator.getTimestamp(), currentAuthenticator.getTimestamp());

        return currentWrapper;
    }

    private void validateTimestamp(TimestampType.Timestamp now, TimestampType.Timestamp then) throws RejectedException {
        if (now.getTime() != then.getTime()) {
            throw new RejectedException("Timestamps do not match");
        }
    }

}
