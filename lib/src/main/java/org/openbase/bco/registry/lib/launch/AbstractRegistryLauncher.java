package org.openbase.bco.registry.lib.launch;

import java.util.List;
import org.openbase.bco.registry.lib.com.AbstractRegistryController;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.storage.registry.RemoteRegistry;

/**
 * #%L
 * #L%
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <L>
 */
 public abstract class AbstractRegistryLauncher<L extends AbstractRegistryController> extends AbstractLauncher<L> {

    public AbstractRegistryLauncher(Class applicationClass, Class<L> launchableClass) throws InstantiationException {
        super(applicationClass, launchableClass);
    }

    @Override
    public void verify() throws VerificationFailedException, InterruptedException {
        MultiException.ExceptionStack exceptionStack = null;
        List<RemoteRegistry> remoteRegistries = getLaunchable().getRemoteRegistries();
        for (RemoteRegistry registry : remoteRegistries) {

            if (!registry.isConsistent()) {
                exceptionStack = MultiException.push(getLaunchable(), new VerificationFailedException(registry.getName() + " started in read only mode!", new InvalidStateException("Registry not consistent!")), exceptionStack);
            }
        }
        try {
            MultiException.checkAndThrow(JPService.getApplicationName() + " started in fallback mode!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw new VerificationFailedException(ex);
        }
    }
}
