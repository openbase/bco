/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import de.citec.dal.util.MultiException;
import de.citec.dal.util.Observable;
import de.citec.dal.util.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Activatable;
import rsb.RSBException;

/**
 *
 * @author mpohling
 */
public class WatchDog implements Activatable {

	private final Object EXECUTION_LOCK = new Object();
	private final Object STATE_LOCK = new Object();

	private static final long DELAY = 10000;

	public enum ServiceState {

		Unknown, Constructed, Initializing, Running, Terminating, Finished, Failed
	};

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final Activatable service;
	private final String serviceName;
	private Minder minder;
	private ServiceState serviceState = ServiceState.Unknown;

	private final Observable<ServiceState> serviceStateObserable;

	public WatchDog(final Activatable task, final String serviceName) {
		this.service = task;
		this.serviceName = serviceName;
		this.serviceStateObserable = new Observable<>();

		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				try {
					deactivate();
				} catch (InterruptedException ex) {
					logger.error("Could not shutdown "+serviceName+"!", ex);
				}
			}
		});

		setServiceState(ServiceState.Constructed);
	}

	@Override
	public void activate() {
		logger.trace("Try to activate service: " + serviceName);
		synchronized (EXECUTION_LOCK) {
			logger.trace("Init activation of service: " + serviceName);
			if (minder != null) {
				logger.warn("Skip activation, Service[" + serviceName + "] already running!");
				return;
			}
			minder = new Minder(serviceName + "WatchDog");
			logger.trace("Start activation of service: " + serviceName);
			minder.start();
		}

		try {
			waitForActivation();
		} catch (InterruptedException ex) {
			logger.warn("Could not wait for service activation!", ex);
		}
	}

	@Override
	public void deactivate() throws InterruptedException {
		logger.trace("Try to deactivate service: " + serviceName);
		synchronized (EXECUTION_LOCK) {
			logger.trace("Init deactivation of service: " + serviceName);
			if (minder == null) {
				logger.warn("Skip deactivation, Service[" + serviceName + "] not running!");
				return;
			}

			logger.trace("Init service interruption...");
			minder.interrupt();
			logger.trace("Wait for service interruption...");
			minder.join();
			minder = null;
			logger.trace("Service interrupted!");
		}
	}

	@Override
	public boolean isActive() {
		return minder != null;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void waitForActivation() throws InterruptedException {

		synchronized (STATE_LOCK) {
			if (serviceState == ServiceState.Running) {
				return;
			}

			addObserver(new Observer<ServiceState>() {

				@Override
				public void update(Observable<ServiceState> source, ServiceState data) throws Exception {
					if (data == ServiceState.Running) {
						synchronized (STATE_LOCK) {
							STATE_LOCK.notify();
						}
					}
				}
			});
			STATE_LOCK.wait();
		}
	}

	private class Minder extends Thread {

		private Minder(String name) {
			super(name);
			setServiceState(ServiceState.Initializing);
		}

		@Override
		public void run() {
			try {
				while (!isInterrupted()) {
					if (!service.isActive()) {
						setServiceState(ServiceState.Initializing);
						try {
							service.activate();
							setServiceState(ServiceState.Running);
						} catch (RSBException ex) {
							logger.error("Could not start Service[" + serviceName + "]! Try again in " + (DELAY / 1000) + " seconds...", ex);
							setServiceState(ServiceState.Failed);
						}
					}
					waitWithinDelay();
				}
			} catch (InterruptedException ex) {
				logger.debug("Catch Service[" + serviceName + "] interruption.");
			}

			while (service.isActive()) {
				setServiceState(ServiceState.Terminating);
				try {
					service.deactivate();
					setServiceState(ServiceState.Finished);
				} catch (RSBException | InterruptedException ex) {
					logger.error("Could not shutdown Service[" + serviceName + "]! Try again in " + (DELAY / 1000) + " seconds...", ex);
					try {
						waitWithinDelay();
					} catch (InterruptedException exx) {
						logger.debug("Catch Service[" + serviceName + "] interruption during shutdown!");
					}
				}
			}
		}

		private void waitWithinDelay() throws InterruptedException {
			Thread.sleep(DELAY);
		}
	}

	public Activatable getService() {
		return service;
	}

	private void setServiceState(final ServiceState serviceState) {
		try {
			synchronized (STATE_LOCK) {
				if (this.serviceState == serviceState) {
					return;
				}
				this.serviceState = serviceState;
			}
			logger.info(this + " is now " + serviceState.name().toLowerCase() + ".");
			serviceStateObserable.notifyObservers(serviceState);
		} catch (MultiException ex) {
			logger.warn("Could not notify statechange to all instanzes!", ex);
			ex.printExceptionStack();
		}
	}

	public ServiceState getServiceState() {
		return serviceState;
	}

	public void addObserver(Observer<ServiceState> observer) {
		serviceStateObserable.addObserver(observer);
	}

	public void removeObserver(Observer<ServiceState> observer) {
		serviceStateObserable.removeObserver(observer);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"["+serviceName+"]";
	}
}
