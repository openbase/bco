package org.openbase.bco.registry.unit.remote;

/*
 * #%L
 * REM UnitRegistry Remote
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.lib.jp.JPUnitRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBRemoteService;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.pattern.Remote;
import org.openbase.jul.storage.registry.RemoteRegistry;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitRegistryDataType.UnitRegistryData;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.rsb.ScopeType;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitRegistryRemote extends RSBRemoteService<UnitRegistryData> implements UnitRegistry, Remote<UnitRegistryData> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitTemplate.getDefaultInstance()));
    }

    private final RemoteRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> unitConfigRemoteRegistry;
    private final RemoteRegistry<String, UnitTemplate, UnitTemplate.Builder, UnitRegistryData.Builder> unitTemplateRemoteRegistry;

    public UnitRegistryRemote() throws InstantiationException, InterruptedException {
        super(UnitRegistryData.class);
        try {
            unitConfigRemoteRegistry = new RemoteRegistry<>();
            unitTemplateRemoteRegistry = new RemoteRegistry<>();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * Method initializes the remote with the given scope for the server
     * registry connection.
     *
     * @param scope
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException
     */
    @Override
    public void init(final Scope scope) throws InitializationException, InterruptedException {
        try {
            this.init(ScopeTransformer.transform(scope));
        } catch (CouldNotTransformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * Method initializes the remote with the given scope for the server
     * registry connection.
     *
     * @param scope
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException
     */
    @Override
    public synchronized void init(final ScopeType.Scope scope) throws InitializationException, InterruptedException {
        super.init(scope);
    }

    /**
     * Method initializes the remote with the default registry connection scope.
     *
     * @throws InitializationException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    public void init() throws InitializationException, InterruptedException {
        try {
            this.init(JPService.getProperty(JPUnitRegistryScope.class).getValue());
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws java.lang.InterruptedException
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
//    @Override
//    public void activate() throws InterruptedException, CouldNotPerformException {
//        super.activate();
//        try {
//            waitForData();
//        } catch (CouldNotPerformException ex) {
//            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial registry sync failed!", ex), logger, LogLevel.ERROR);
//        }
//    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        try {
            unitConfigRemoteRegistry.shutdown();
        } finally {
            super.shutdown();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void notifyDataUpdate(final UnitRegistryData data) throws CouldNotPerformException {
        unitConfigRemoteRegistry.notifyRegistryUpdate(data.getUnitConfigList());
    }

    public RemoteRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> getUnitConfigRemoteRegistry() {
        return unitConfigRemoteRegistry;
    }

    public RemoteRegistry<String, UnitTemplate, UnitTemplate.Builder, UnitRegistryData.Builder> getUnitTemplateRemoteRegistry() {
        return unitTemplateRemoteRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfig
     * @return
     * @throws org.openbase.jul.exception.CouldNotPerformException
     */
    @Override
    public Future<UnitConfig> registerUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(unitConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register unit config!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfigId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public UnitConfig getUnitConfigById(String unitConfigId) throws CouldNotPerformException, NotAvailableException {
        validateData();
        return unitConfigRemoteRegistry.getMessage(unitConfigId);
    }

    @Override
    public Boolean containsUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        validateData();
        return unitConfigRemoteRegistry.contains(unitConfig);
    }

    @Override
    public Boolean containsUnitConfigById(final String unitConfigId) throws CouldNotPerformException {
        validateData();
        return unitConfigRemoteRegistry.contains(unitConfigId);
    }

    @Override
    public Future<UnitConfig> updateUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(unitConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update unit config!", ex);
        }
    }

    @Override
    public Future<UnitConfig> removeUnitConfig(final UnitConfig unitConfig) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(unitConfig, this, UnitConfig.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove unit config!", ex);
        }
    }

    @Override
    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException, NotAvailableException {
        validateData();
        return unitConfigRemoteRegistry.getMessages();
    }

    @Override
    public Boolean isUnitConfigRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getUnitConfigRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isUnitConfigRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getUnitConfigRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }

    @Override
    public Future<UnitTemplate> updateUnitTemplate(UnitTemplate unitTemplate) throws CouldNotPerformException {
        try {
            return RPCHelper.callRemoteMethod(unitTemplate, this, UnitTemplate.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update unit template!", ex);
        }
    }

    @Override
    public Boolean containsUnitTemplate(UnitTemplate unitTemplate) throws CouldNotPerformException {
        validateData();
        return unitTemplateRemoteRegistry.contains(unitTemplate);
    }

    @Override
    public Boolean containsUnitTemplateById(String unitTemplateId) throws CouldNotPerformException {
        validateData();
        return unitTemplateRemoteRegistry.contains(unitTemplateId);
    }

    @Override
    public UnitTemplate getUnitTemplateById(String unitTemplateId) throws CouldNotPerformException, InterruptedException {
        validateData();
        return unitTemplateRemoteRegistry.getMessage(unitTemplateId);
    }

    @Override
    public List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException {
        validateData();
        return unitTemplateRemoteRegistry.getMessages();
    }

    @Override
    public UnitTemplate getUnitTemplateByType(UnitTemplate.UnitType type) throws CouldNotPerformException {
        validateData();
        for (UnitTemplate unitTemplate : unitTemplateRemoteRegistry.getMessages()) {
            if (unitTemplate.getType() == type) {
                return unitTemplate;
            }
        }
        throw new NotAvailableException("unit template", "No UnitTemplate with given Type[" + type + "] registered!");
    }

    @Override
    public Boolean isUnitTemplateRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        try {
            if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
                return true;
            }
        } catch (JPServiceException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
        }

        return getData().getUnitTemplateRegistryReadOnly();
    }

    @Override
    public Boolean isUnitTemplateRegistryConsistent() throws CouldNotPerformException {
        try {
            validateData();
            return getData().getUnitTemplateRegistryConsistent();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not check consistency!", ex);
        }
    }
}
