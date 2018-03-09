package org.openbase.bco.authentication.lib.com;

import com.google.protobuf.GeneratedMessage;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.iface.AuthenticatedRequestable;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableRemote;
import org.openbase.jul.pattern.Observer;
import rsb.Event;
import rsb.Handler;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;

import java.util.concurrent.Future;

public class AbstractAuthenticatedConfigurableRemote<M extends GeneratedMessage, CONFIG extends GeneratedMessage> extends AbstractConfigurableRemote<M, CONFIG> {

    private final Observer<String> loginObserver;

    public AbstractAuthenticatedConfigurableRemote(final Class<M> dataClass, final Class<CONFIG> configClass) {
        super(dataClass, configClass);
        this.setMessageProcessor(new AuthenticatedMessageProcessor<>(dataClass));

        this.loginObserver = (source, data) -> requestData();
        SessionManager.getInstance().addLoginObserver(this.loginObserver);
    }

    @Override
    protected Handler generateHandler() {
        return new AuthenticatedUpdateHandler();
    }

    @Override
    protected Future<Event> internalRequestStatus() throws CouldNotPerformException {
        if (SessionManager.getInstance().isLoggedIn()) {
            Event event = new Event(TicketAuthenticatorWrapper.class, SessionManager.getInstance().initializeServiceServerRequest());
            return getRemoteServer().callAsync(AuthenticatedRequestable.REQUEST_DATA_AUTHENTICATED_METHOD, event);
        } else {
            return super.internalRequestStatus();
        }
    }

    private class AuthenticatedUpdateHandler implements Handler {

        @Override
        public void internalNotify(Event event) {
            try {
                logger.debug("Internal notification while logged in[" + SessionManager.getInstance().isLoggedIn() + "]");
                if (event.getData() != null && SessionManager.getInstance().isLoggedIn()) {
                    requestData();
                } else {
                    applyEventUpdate(event);
                }
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Internal notification failed!", ex), logger);
            }
        }
    }
}
