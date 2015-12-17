/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.usr.core.registry;

import de.citec.jp.JPGroupConfigDatabaseDirectory;
import de.citec.jp.JPUserConfigDatabaseDirectory;
import de.citec.jp.JPUserRegistryScope;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.rsb.com.RPCHelper;
import de.citec.jul.extension.rsb.com.RSBCommunicationService;
import de.citec.jul.extension.rsb.iface.RSBLocalServerInterface;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.storage.file.ProtoBufJSonFileProvider;
import de.citec.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import de.citec.usr.lib.generator.GroupConfigIdGenerator;
import de.citec.usr.lib.generator.UserConfigIdGenerator;
import de.citec.usr.lib.registry.UserRegistryInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.authorization.GroupConfigType.GroupConfig;
import rst.authorization.UserConfigType.UserConfig;
import rst.authorization.UserRegistryType.UserRegistry;

/**
 *
 * @author mpohling
 */
public class UserRegistryService extends RSBCommunicationService<UserRegistry, UserRegistry.Builder> implements UserRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserConfig.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(GroupConfig.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, UserConfig, UserConfig.Builder, UserRegistry.Builder> userRegistry;
    private ProtoBufFileSynchronizedRegistry<String, GroupConfig, GroupConfig.Builder, UserRegistry.Builder> groupRegistry;

    public UserRegistryService() throws InstantiationException, InterruptedException {
        super(UserRegistry.newBuilder());
        try {
            ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
            userRegistry = new ProtoBufFileSynchronizedRegistry<>(UserConfig.class, getBuilderSetup(), getFieldDescriptor(UserRegistry.USER_CONFIG_FIELD_NUMBER), new UserConfigIdGenerator(), JPService.getProperty(JPUserConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
            groupRegistry = new ProtoBufFileSynchronizedRegistry<>(GroupConfig.class, getBuilderSetup(), getFieldDescriptor(UserRegistry.GROUP_CONFIG_FIELD_NUMBER), new GroupConfigIdGenerator(), JPService.getProperty(JPGroupConfigDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);

            userRegistry.loadRegistry();
            groupRegistry.loadRegistry();

            userRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, UserConfig, UserConfig.Builder>>>() {

                @Override
                public void update(Observable<Map<String, IdentifiableMessage<String, UserConfig, UserConfig.Builder>>> source, Map<String, IdentifiableMessage<String, UserConfig, UserConfig.Builder>> data) throws Exception {
                    notifyChange();
                }
            });

            groupRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, GroupConfig, GroupConfig.Builder>>>() {

                @Override
                public void update(Observable<Map<String, IdentifiableMessage<String, GroupConfig, GroupConfig.Builder>>> source, Map<String, IdentifiableMessage<String, GroupConfig, GroupConfig.Builder>> data) throws Exception {
                    notifyChange();
                }
            });

        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        try {
        super.init(JPService.getProperty(JPUserRegistryScope.class).getValue());
        } catch (JPServiceException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        try {
            super.activate();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not activate location registry!", ex);
        }

        try {
            userRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
        }

        try {
            groupRegistry.checkConsistency();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial consistency check failed!", ex), logger, LogLevel.WARN);
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
    }

    @Override
    public void shutdown() {
        if (userRegistry != null) {
            userRegistry.shutdown();
        }

        if (groupRegistry != null) {
            groupRegistry.shutdown();
        }

        try {
            deactivate();
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }

    @Override
    public final void notifyChange() throws CouldNotPerformException {
        // sync read only flags
        setField(UserRegistry.USER_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, userRegistry.isReadOnly());
        setField(UserRegistry.GROUP_CONFIG_REGISTRY_READ_ONLY_FIELD_NUMBER, groupRegistry.isReadOnly());
        super.notifyChange();
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(UserRegistryInterface.class, this, server);
    }

    public ProtoBufFileSynchronizedRegistry<String, UserConfig, UserConfig.Builder, UserRegistry.Builder> getUserRegistry() {
        return userRegistry;
    }

    public ProtoBufFileSynchronizedRegistry<String, GroupConfig, GroupConfig.Builder, UserRegistry.Builder> getGroupRegistry() {
        return groupRegistry;
    }

    @Override
    public UserConfig registerUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        return userRegistry.register(userConfig);
    }

    @Override
    public Boolean containsUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        return userRegistry.contains(userConfig);
    }

    @Override
    public Boolean containsUserConfigById(String userConfigId) throws CouldNotPerformException {
        return userRegistry.contains(userConfigId);
    }

    @Override
    public UserConfig updateUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        return userRegistry.update(userConfig);
    }

    @Override
    public UserConfig removeUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        return userRegistry.remove(userConfig);
    }

    @Override
    public UserConfig getUserConfigById(String userConfigId) throws CouldNotPerformException {
        return userRegistry.get(userConfigId).getMessage();
    }

    @Override
    public List<UserConfig> getUserConfigs() throws CouldNotPerformException {
        return userRegistry.getMessages();
    }

    @Override
    public Future<Boolean> isUserConfigRegistryReadOnly() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(userRegistry.isReadOnly());
    }

    @Override
    public List<UserConfig> getUserConfigsByGroupConfig(GroupConfig groupConfig) throws CouldNotPerformException {
        getData();
        List<UserConfig> userConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, GroupConfig, GroupConfig.Builder> group : groupRegistry.getEntries()) {
            if (group.getMessage().equals(groupConfig)) {
                for (String memeberId : group.getMessage().getMemberIdList()) {
                    userConfigs.add(getUserConfigById(memeberId));
                }
                return userConfigs;
            }
        }
        return userConfigs;
    }

    @Override
    public GroupConfig registerGroupConfig(GroupConfig groupConfig) throws CouldNotPerformException {
        return groupRegistry.register(groupConfig);
    }

    @Override
    public Boolean containsGroupConfig(GroupConfig groupConfig) throws CouldNotPerformException {
        return groupRegistry.contains(groupConfig);
    }

    @Override
    public Boolean containsGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        return groupRegistry.contains(groupConfigId);
    }

    @Override
    public GroupConfig updateGroupConfig(GroupConfig groupConfig) throws CouldNotPerformException {
        return groupRegistry.update(groupConfig);
    }

    @Override
    public GroupConfig removeGroupConfig(GroupConfig groupConfig) throws CouldNotPerformException {
        return groupRegistry.remove(groupConfig);
    }

    @Override
    public GroupConfig getGroupConfigById(String groupConfigId) throws CouldNotPerformException {
        return groupRegistry.get(groupConfigId).getMessage();
    }

    @Override
    public List<GroupConfig> getGroupConfigs() throws CouldNotPerformException {
        return groupRegistry.getMessages();
    }

    @Override
    public List<GroupConfig> getGroupConfigsbyUserConfig(UserConfig userConfig) throws CouldNotPerformException {
        getData();
        List<GroupConfig> groupConfigs = new ArrayList<>();
        for (IdentifiableMessage<String, GroupConfig, GroupConfig.Builder> group : groupRegistry.getEntries()) {
            group.getMessage().getMemberIdList().stream().filter((memeberId) -> (userConfig.getId().equals(memeberId))).forEach((_item) -> {
                groupConfigs.add(group.getMessage());
            });
        }
        return groupConfigs;
    }

    @Override
    public Future<Boolean> isGroupConfigRegistryReadOnly() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(groupRegistry.isReadOnly());
    }
}
