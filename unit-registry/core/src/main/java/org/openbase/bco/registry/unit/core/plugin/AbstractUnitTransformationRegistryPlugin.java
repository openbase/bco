package org.openbase.bco.registry.unit.core.plugin;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.*;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.iface.Transformer;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import org.openbase.rct.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public abstract class AbstractUnitTransformationRegistryPlugin extends ProtobufRegistryPluginAdapter<String, UnitConfig, Builder> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected TransformerFactory transformerFactory;
    protected TransformPublisher transformPublisher;

    public AbstractUnitTransformationRegistryPlugin() throws org.openbase.jul.exception.InstantiationException {
        try {
            this.transformerFactory = TransformerFactory.getInstance();
        } catch (Exception ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init(final ProtoBufRegistry<String, UnitConfig, Builder> registry) throws InitializationException, InterruptedException {
        try {
            super.init(registry);
            this.transformPublisher = GlobalTransformPublisher.getInstance();
            for (IdentifiableMessage<String, UnitConfig, Builder> entry : registry.getEntries()) {
                publishTransformation(entry);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    protected void verifyPublication(final Transform transformation, String targetFrame, String sourceFrame) throws VerificationFailedException {
        // wait until transformation was published
        try {
            int maxChecks = 10;
            Exception exception = new CouldNotPerformException("This should not happen");
            for (int i = 0; i < maxChecks; i++) {
                try {
                    // check if transformation was published
                    if (transformation.equalsWithoutTime(GlobalTransformReceiver.getInstance().lookupTransform(targetFrame, sourceFrame, System.currentTimeMillis()))) {
                        // was published
                        if (!JPService.testMode() && JPService.verboseMode()) {
                            logger.info("Published " + targetFrame + " to " + sourceFrame);
                        }
                        return;
                    }
                } catch (TransformerException ex) {
                    exception = ex;
                    // try again if needed
                    Thread.sleep(100);
                }
            }
            throw new TimeoutException("Transformation not published in time!", exception);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new VerificationFailedException(new ShutdownInProgressException(this));
        } catch (CouldNotPerformException ex) {
            throw new VerificationFailedException("Could not verify publication!", ex);
        }
    }

    @Override
    public void afterRegister(IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) {
        publishTransformation(entry);
    }

    @Override
    public void afterUpdate(IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry) throws CouldNotPerformException {
        publishTransformation(entry);
    }

    protected abstract void publishTransformation(final IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry);
}
