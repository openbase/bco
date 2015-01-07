/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.service.DalRegistry;
import de.citec.dal.service.HardwareManager;
import de.citec.dal.service.rsb.RSBCommunicationService;
import de.citec.dal.service.rsb.RSBRemoteService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.RSBException;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rsb.patterns.LocalServer;
import rst.homeautomation.openhab.DALBindingType;
import rst.homeautomation.openhab.DALBindingType.DALBinding;
import rst.homeautomation.openhab.OpenhabCommandType;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;
import rst.homeautomation.openhab.RSBBindingType;
import rst.homeautomation.openhab.RSBBindingType.RSBBinding;

/**
 *
 * @author thuxohl
 */
public class RSBBindingConnection implements RSBBindingInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(OpenhabCommandType.OpenhabCommand.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(RSBBindingType.RSBBinding.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(DALBindingType.DALBinding.getDefaultInstance()));
    }
    
    private static final Logger logger = LoggerFactory.getLogger(RSBBindingConnection.class);

    private static RSBBindingConnection instance;
    private final Scope dalRemoteScope = new Scope("/dal");
    private final Scope openhabBindingScope = new Scope("/openhab");
    private final RSBRemoteService<DALBinding> dalRemoteService;
    private final RSBCommunicationService<RSBBinding, RSBBinding.Builder> communicationService;

    private final DalRegistry registry;

    private final HardwareManager hardwareManager;

    public RSBBindingConnection() {
        this.instance = this;
        this.registry = DalRegistry.getInstance();
        this.hardwareManager = HardwareManager.getInstance();

        dalRemoteService = new RSBRemoteService<DALBinding>() {

            @Override
            public void notifyUpdated(DALBinding data) {
                RSBBindingConnection.this.notifyUpdated(data);
            }
        };
        dalRemoteService.init(dalRemoteScope);

        communicationService = new RSBCommunicationService<RSBBinding, RSBBinding.Builder>(openhabBindingScope, RSBBinding.newBuilder()) {

            @Override
            public void registerMethods(LocalServer server) throws RSBException {
                RSBBindingConnection.this.registerMethods(server);
            }
        };
        try {
            communicationService.init();
        } catch (RSBException ex) {
            logger.warn("Unable to initialize the communication service in [" + getClass().getSimpleName() + "]");
        }

        try {
            this.hardwareManager.activate();
        } catch (Exception ex) {
            logger.warn("Hardware manager could not be activated!", ex);
        }

        dalRemoteService.activate();
        {
            try {
                communicationService.activate();
            } catch (Exception ex) {
                logger.warn("Unable to activate the communication service in [" + getClass().getSimpleName() + "]", ex);
            }
        }
    }

    public final void notifyUpdated(DALBinding data) {
        switch (data.getState().getState()) {
            case ACTIVE:
                logger.debug("Active rsb binding state!");
                break;
            case DEACTIVE:
                logger.debug("Deactive rsb binding state!");
                break;
            case UNKNOWN:
                logger.debug("Unkown rsb binding state!");
                break;
        }
    }

    public final void registerMethods(LocalServer server) {
        try {
            server.addMethod("internalReceiveUpdate", new InternalReceiveUpdateCallback());
        } catch (RSBException ex) {
            logger.warn("Could not add methods to local server in [" + getClass().getSimpleName() + "]", ex);
        }
    }

    @Override
    public void internalReceiveUpdate(OpenhabCommand command) {
        hardwareManager.internalReceiveUpdate(command);
    }

    public static class InternalReceiveUpdateCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            instance.internalReceiveUpdate((OpenhabCommand) request.getData());
            return new Event(String.class, "Ok");
        }

    }

    @Override
    public Future executeCommand(OpenhabCommandType.OpenhabCommand command) throws RSBBindingException {
		try {
			dalRemoteService.callMethod("executeCommand", command);
			return null; // TODO: mpohling implement future handling.
		} catch (RSBException | ExecutionException | TimeoutException  ex) {
			throw new RSBBindingException("Could not execute "+command+"!", ex);
		}
    }

    public static RSBBindingInterface getInstance() {
        while (instance == null) {
            logger.warn("WARN: Binding not ready yet!");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(RSBBindingConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return instance;
    }
}
