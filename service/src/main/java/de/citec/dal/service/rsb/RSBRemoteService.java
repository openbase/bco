/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service.rsb;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.data.Location;
import static de.citec.dal.service.rsb.RSBCommunicationService.RPC_REQUEST_STATUS;
import de.citec.dal.util.DALException;
import de.citec.dal.util.NotAvailableException;
import de.citec.dal.util.Observable;
import de.citec.dal.util.Observer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.Factory;
import rsb.Handler;
import rsb.Listener;
import rsb.RSBException;
import rsb.Scope;
import rsb.patterns.Future;
import rsb.patterns.RemoteServer;

/**
 *
 * @author mpohling
 * @param <M>
 * @param <MB>
 */
public abstract class RSBRemoteService<M extends GeneratedMessage> extends Observable<M> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private Listener listener;
	private WatchDog listenerWatchDog, remoteServerWatchDog;
	private final Handler mainHandler;
	private RemoteServer remoteServer;

	protected Scope scope;
	private M data;
	private boolean initialized;

	public RSBRemoteService() {
		this.mainHandler = new InternalUpdateHandler();
		this.initialized = false;
	}

	public void init(final String label, final Location location) {
		init(generateScope(label, location));
	}

	public synchronized void init(final Scope scope) {

		if (initialized) {
			logger.warn("Skip initialization because " + this + " already initialized!");
		}

		this.scope = new Scope(scope.toString().toLowerCase());
		logger.debug("Init RSBCommunicationService for component " + getClass().getSimpleName() + " on " + this.scope + ".");

		initListener(this.scope);
		initRemoteServer(this.scope);

		try {
			addHandler(mainHandler, true);
		} catch (InterruptedException ex) {
			logger.warn("Could not register main handler!", ex);
		}
		initialized = true;
	}

	private void initListener(final Scope scope) {
		try {
			this.listener = Factory.getInstance().createListener(scope.concat(RSBCommunicationService.SCOPE_SUFFIX_INFORMER));
			this.listenerWatchDog = new WatchDog(listener, "RSBListener["+scope.concat(RSBCommunicationService.SCOPE_SUFFIX_INFORMER)+"]");
		} catch (Exception ex) {
			logger.error("Could not create Listener on scope [" + scope.toString() + "]!", ex);
		}
	}

	private void initRemoteServer(final Scope scope) {
		try {
			this.remoteServer = Factory.getInstance().createRemoteServer(scope.concat(RSBCommunicationService.SCOPE_SUFFIX_RPC));
			this.remoteServerWatchDog = new WatchDog(remoteServer, "RSBRemoteServer["+scope.concat(RSBCommunicationService.SCOPE_SUFFIX_RPC)+"]");
			this.listenerWatchDog.addObserver(new Observer<WatchDog.ServiceState>() {

				@Override
				public void update(Observable<WatchDog.ServiceState> source, WatchDog.ServiceState data) throws Exception {
					if (data == WatchDog.ServiceState.Running) {
						requestStatus();
					}
				}
			});
		} catch (Exception ex) {
			logger.error("Could not create RemoteServer on scope [" + scope.toString() + "]!", ex);
		}
	}

	public void addHandler(final Handler handler, final boolean wait) throws InterruptedException {
		try {
			listener.addHandler(handler, wait);
		} catch (InterruptedException ex) {
			logger.error("Could not register Handler!", ex);
		}
	}

	public void activate() {
		if (!initialized) {
			logger.warn("Skip activation because " + this + " is not initialized!");
		}
		activateListener();
		activateRemoteServer();
	}

	public void deactivate() throws InterruptedException {
		if (!initialized) {
			logger.warn("Skip deactivation because " + this + " is not initialized!");
		}
		deactivateListener();
		deactivateRemoteServer();
	}

	private void activateListener() {
		listenerWatchDog.activate();
	}

	private void deactivateListener() throws InterruptedException {
		listenerWatchDog.deactivate();
	}

	private void activateRemoteServer() {
		remoteServerWatchDog.activate();
	}

	private void deactivateRemoteServer() throws InterruptedException {
		remoteServerWatchDog.deactivate();
	}

	public <R, T extends Object> R callMethod(String methodName) throws RSBException, ExecutionException, TimeoutException {
		return callMethod(methodName, null);
	}

	public <R, T extends Object> Future<Object> callMethodAsync(String methodName) throws DALException {
		return callMethodAsync(methodName, null);
	}

	public <R, T extends Object> R callMethod(String methodName, T type) throws RSBException, ExecutionException, TimeoutException {

		if (!initialized) {
			logger.warn("Skip callMethod because " + this + " is not initialized!");
		}
		try {
			System.out.println("Calling method [" + methodName + "] on scope: " + remoteServer.getScope().toString());
			return remoteServer.call(methodName, type);
		} catch (RSBException | ExecutionException | TimeoutException ex) {
			logger.error("Could not call remote Methode[" + methodName + "] on Scope[" + remoteServer.getScope() + "].", ex);
			throw ex;
		}
	}

	public <R, T extends Object> Future<R> callMethodAsync(String methodName, T type) throws DALException {

		if (!initialized) {
			logger.warn("Skip callMethod because " + this + " is not initialized!");
		}
		try {
			System.out.println("Calling method [" + methodName + "] on scope: " + remoteServer.getScope().toString());
			return remoteServer.callAsync(methodName, type);
		} catch (RSBException ex) {
			throw new DALException("Could not call remote Methode[" + methodName + "] on Scope[" + remoteServer.getScope() + "].", ex);
		}
	}

	public void requestStatus() throws DALException {
		logger.debug("requestStatus updated.");
		callMethodAsync(RPC_REQUEST_STATUS);
	}

	@Override
	public void shutdown() {
		try {
			deactivate();
		} catch (InterruptedException ex) {
			logger.warn("Could not deactivate remote service!", ex);
		}
		super.shutdown();
	}

	public static Scope generateScope(final String id, final Location location) {
		return location.getScope().concat(new Scope(Location.COMPONENT_SEPERATOR + id));
	}

	public M getData() throws NotAvailableException {
		if (data == null) {
			throw new NotAvailableException("data");
		}
		return data;
	}

	public abstract void notifyUpdated(M data);

	private class InternalUpdateHandler implements Handler {

		@Override
		public void internalNotify(Event event) {
			logger.debug("Internal notification: " + event.toString());
			try {
				data = (M) event.getData();
				notifyUpdated(data);
				notifyObservers(data);
			} catch (Exception ex) {
				logger.error("Could not notify new data, given datatype is not valid!", ex);
			}
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[scope:" + scope.toString() + "]";
	}
}
