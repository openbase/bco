package org.openbase.bco.authenticator.lib;

import java.util.List;
import org.openbase.jul.exception.RejectedException;
import rst.domotic.authentification.AuthenticatorTicketType.AuthenticatorTicket;
import rst.domotic.authentification.LoginResponseType.LoginResponse;

/**
 *
 * @author sebastian
 */
public interface AuthenticationClientHandler {
    
    /**
     * Handles a KDC response
     * Decrypts the TGS session key with client's hashed password
     * Creates an Authenticator containing the clientID and current timestamp encrypted with the TGS session key
     * @param clientID Identifier of the client - must be present in client database
     * @param hashedClientPassword Client's hashed password
     * @param wrapper TicketSessionKeyWrapper containing the TGT and TGS session key
     * @return Returns a list of objects containing: 
     *         1. An AuthenticatorTicketWrapper containing both the TGT and Authenticator
     *         2. A SessionKey representing the TGS session key
     * @TODO: Exception description
     */
    public List<Object> handleKDCResponse(String clientID, byte[] hashedClientPassword, LoginResponse wrapper) throws RejectedException; 

    /**
     * Handles a TGS response
     * Decrypts the SS session key with TGS session key
     * Creates an Authenticator containing the clientID and empty timestamp encrypted with the SS session key
     * @param clientID Identifier of the client - must be present in client database
     * @param TGSSessionKey TGS session key provided by handleKDCResponse()
     * @param wrapper TicketSessionKeyWrapper containing the CST and SS session key
     * @return Returns a list of objects containing: 
     *         1. An AuthenticatorTicketWrapper containing both the CST and Authenticator
     *         2. A SessionKey representing the SS session key
     * @TODO: Exception description
     */
    public List<Object> handleTGSResponse(String clientID, byte[] TGSSessionKey, LoginResponse wrapper) throws RejectedException;
    
    /**
     * Initializes a SS request
     * Sets current timestamp in Authenticator
     * @param SSSessionKey SS session key provided by handleTGSResponse()
     * @param wrapper AuthenticatorTicket wrapper that contains both encrypted Authenticator and CST
     * @return Returns a wrapper class containing both the CST and modified Authenticator
     * @TODO: Exception description
     */
    public AuthenticatorTicket initSSRequest(byte[] SSSessionKey, AuthenticatorTicket wrapper) throws RejectedException;
    
    /**
     * Handles a SS response
     * Decrypts Authenticator of both last- and currentWrapper with SSSessionKey
     * Compares timestamps of both Authenticators with each other
     * @param SSSessionKey SS session key provided by handleTGSResponse()
     * @param lastWrapper Last TicketAuthenticatorWrapper provided by either handleTGSResponse() or handleSSResponse()
     * @param currentWrapper Current TicketAuthenticatorWrapper provided by (Remote?)
     * @return Returns an AuthenticatorTicketWrapper containing both the CST and Authenticator
     * @throws RejectedException Throws, if timestamps do not match
     * @TODO: Exception description
     */
    public AuthenticatorTicket handleSSResponse(byte[] SSSessionKey, AuthenticatorTicket lastWrapper, AuthenticatorTicket currentWrapper) throws RejectedException;
}
