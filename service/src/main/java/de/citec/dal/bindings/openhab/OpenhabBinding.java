/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab;

import de.citec.dal.bindings.openhab.transform.OpenhabCommandTransformer;
import de.citec.dal.util.DALRegistry;
import de.citec.jul.rsb.RSBCommunicationService;
import de.citec.jul.rsb.RSBInformerInterface.InformerType;
import de.citec.jul.rsb.RSBRemoteService;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.InvocationFailedException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
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
import rst.homeautomation.states.ActiveDeactiveType;

/**
 * @author thuxohl
 * @author mpohling
 */
public class OpenhabBinding implements OpenhabBindingInterface {

	public static final String RPC_METHODE_INTERNAL_RECEIVE_UPDATE = "internalReceiveUpdate";
	public static final String RPC_METHODE_EXECUTE_COMMAND = "executeCommand";

	public static final Scope SCOPE_DAL = new Scope("/dal");
	public static final Scope SCOPE_OPENHAB = new Scope("/openhab");

	private static final Logger logger = LoggerFactory.getLogger(OpenhabBinding.class);

	private static OpenhabBinding instance;

	private final RSBRemoteService<RSBBinding> openhabRemoteService;
	private final RSBCommunicationService<DALBinding, DALBinding.Builder> dalCommunicationService;
	private final DALRegistry registry;

	static {
		DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(OpenhabCommandType.OpenhabCommand.getDefaultInstance()));
		DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RSBBindingType.RSBBinding.getDefaultInstance()));
		DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DALBindingType.DALBinding.getDefaultInstance()));
	}

	public synchronized static OpenhabBinding getInstance() throws InstantiationException {
		if (instance == null) {
			instance = new OpenhabBinding();
		}
		return instance;
	}

	private OpenhabBinding() throws InstantiationException {
        try {
		this.registry = DALRegistry.getInstance();

		openhabRemoteService = new RSBRemoteService<RSBBinding>() {

			@Override
			public void notifyUpdated(RSBBinding data) {
				OpenhabBinding.this.notifyUpdated(data);
			}
		};
		openhabRemoteService.init(SCOPE_OPENHAB);

		dalCommunicationService = new RSBCommunicationService<DALBinding, DALBinding.Builder>(SCOPE_DAL, DALBinding.newBuilder()) {

			@Override
			public void registerMethods(LocalServer server) throws RSBException {
				OpenhabBinding.this.registerMethods(server);
			}
		};
		try {
			dalCommunicationService.init(InformerType.Single);
		} catch (RSBException ex) {
			logger.warn("Unable to initialize the communication service in [" + getClass().getSimpleName() + "]", ex);
		}

		openhabRemoteService.activate();
		dalCommunicationService.activate();
        
        dalCommunicationService.getData().setState(ActiveDeactiveType.ActiveDeactive.newBuilder().setState(ActiveDeactiveType.ActiveDeactive.ActiveDeactiveState.ACTIVE));
		dalCommunicationService.notifyChange();
        } catch (CouldNotPerformException ex) {
            throw new de.citec.jul.exception.InstantiationException(this, ex); 
        }
	}

	public final void notifyUpdated(RSBBinding data) {
		switch (data.getState().getState()) {
			case ACTIVE:
				logger.info("Active dal binding state!");
				break;
			case DEACTIVE:
				logger.info("Deactive dal binding state!");
				break;
			case UNKNOWN:
				logger.info("Unkown dal binding state!");
				break;
		}
	}

	public final void registerMethods(LocalServer server) {
		try {
			server.addMethod(RPC_METHODE_INTERNAL_RECEIVE_UPDATE, new InternalReceiveUpdateCallback());
		} catch (RSBException ex) {
			logger.warn("Could not add methods to local server in [" + getClass().getSimpleName() + "]", ex);
		}
	}

	@Override
	public void internalReceiveUpdate(OpenhabCommand command) throws CouldNotPerformException {
		try {
			try {
				((AbstractOpenHABDeviceController) registry.getDevice(command.getItem())).receiveUpdate(command);
			} catch (ClassCastException ex) {
				throw new CouldNotPerformException("Resolved device is not supported by " + this + "!", ex);
			}
		} catch (Exception ex) {
			throw new CouldNotPerformException("Skip item update [" + command.getItem() + " = " + OpenhabCommandTransformer.getCommandData(command) + "]!", ex);
		}
	}

	public static class InternalReceiveUpdateCallback extends EventCallback {

		@Override
		public Event invoke(final Event request) throws Throwable {
			try {
				OpenhabBinding.instance.internalReceiveUpdate((OpenhabCommand) request.getData());
			} catch (Throwable cause) {
                throw ExceptionPrinter.printHistory(logger, new InvocationFailedException(this, OpenhabBinding.instance, cause));
			}
			return RSBCommunicationService.RPC_SUCCESS;
		}
	}

	@Override
	public Future executeCommand(OpenhabCommandType.OpenhabCommand command) throws CouldNotPerformException {
		try {
			if (JPService.getAttribute(JPHardwareSimulationMode.class).getValue()) {
				internalReceiveUpdate(command);
				return null;
			}
			if (!openhabRemoteService.isConnected()) {
				throw new InvalidStateException("Could not reache server!");
			}

			openhabRemoteService.callMethod(RPC_METHODE_EXECUTE_COMMAND, command);
			return null; // TODO: mpohling implement future handling.
		} catch (RSBException | ExecutionException | TimeoutException | CouldNotPerformException ex) {
			throw new CouldNotPerformException("Could not execute " + command + "!", ex);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[version=" + getClass().getPackage().getImplementationVersion() + "]";
	}
	/**
	 * TODO mpohling: add the following code to the openhabbinding pom.
	 *
	 * <plugin>
	 * <groupId>org.apache.maven.plugins</groupId>
	 * <artifactId>maven-jar-plugin</artifactId>
	 * <configuration>
	 * <archive>
	 * <manifest>
	 * <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
	 * <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
	 * </manifest>
	 * </archive>
	 * </configuration>
	 * </plugin>
	 */

}
