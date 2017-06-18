package org.openbase.bco.authentication.lib;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.crypto.KeyGenerator;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.RejectedException;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketSessionKeyWrapperType;

/**
 *
 * @author Sebastian Fast <sfast@techfak.uni-bielefeld.de>
 */
public class SessionManager {
    
    private TicketAuthenticatorWrapper clientServerTicket;
    private byte[] sessionKey;
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(KeyGenerator.class);
    private ClientRemote clientRemote;
    
    public SessionManager() {
        this.clientRemote = new ClientRemote();
    }

    public TicketAuthenticatorWrapper getClientServerTicket() {
        return clientServerTicket;
    }

    public byte[] getSessionKey() {
        return sessionKey;
    }
    
    /**
     * Wraps the whole login process into one method
     * @param clientId Identifier of the client - must be present in client database
     * @param clientPassword Password of the client
     * @return Returns Returns an TicketAuthenticatorWrapperWrapper containing both the ClientServerTicket and Authenticator
     * @throws RejectedException Throws, if an error occurred during login process, e.g. clientId non existent
     */
    public boolean login(String clientId, String clientPassword) throws InitializationException, InterruptedException, CouldNotPerformException, IOException, ExecutionException {
        this.clientRemote.init();
        this.clientRemote.activate();

        Thread.sleep(500);
        
        // init KDC request on client side
        byte[] clientPasswordHash = EncryptionHelper.hash(clientPassword);

        // request TGT
        TicketSessionKeyWrapperType.TicketSessionKeyWrapper tskw = clientRemote.requestTicketGrantingTicket(clientId).get();
        
        // handle KDC response on client side
        List<Object> list = AuthenticationClientHandler.handleKDCResponse(clientId, clientPasswordHash, tskw);
        TicketAuthenticatorWrapper taw = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        byte[] TGSSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side

        // request CST
        tskw = clientRemote.requestClientServerTicket(taw).get();

        // handle TGS response on client side
        list = AuthenticationClientHandler.handleTGSResponse(clientId, TGSSessionKey, tskw);
        this.clientServerTicket = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        this.sessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side

        clientRemote.shutdown();
        return true;
    }
    
    /**
     * Logs a user out by setting CST and session key to null
     */
    public void logout() {
        this.clientServerTicket = null;
        this.sessionKey = null;
    }
}
