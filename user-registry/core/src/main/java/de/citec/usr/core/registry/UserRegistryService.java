/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.usr.core.registry;

import de.citec.jp.JPUserDatabaseDirectory;
import de.citec.jp.JPUserRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.storage.file.ProtoBufJSonFileProvider;
import de.citec.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import java.util.Map;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.extension.rsb.com.RSBCommunicationService;
import de.citec.jul.extension.rsb.iface.RSBLocalServerInterface;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.rsb.com.RPCHelper;
import de.citec.usr.lib.generator.UserConfigIdGenerator;
import de.citec.usr.lib.registry.UserRegistryInterface;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import rst.person.PersonRegistryType;
import rst.person.PersonRegistryType.PersonRegistry;
import rst.person.PersonType.Person;

/**
 *
 * @author mpohling
 */
public class UserRegistryService extends RSBCommunicationService<PersonRegistry, PersonRegistry.Builder> implements UserRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PersonRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Person.getDefaultInstance()));
    }

    private ProtoBufFileSynchronizedRegistry<String, Person, Person.Builder, PersonRegistry.Builder> userRegistry;

    public UserRegistryService() throws InstantiationException, InterruptedException {
        super(PersonRegistry.newBuilder());
        try {
            ProtoBufJSonFileProvider protoBufJSonFileProvider = new ProtoBufJSonFileProvider();
            userRegistry = new ProtoBufFileSynchronizedRegistry<>(Person.class, getBuilderSetup(), getFieldDescriptor(PersonRegistry.PERSON_FIELD_NUMBER), new UserConfigIdGenerator(), JPService.getProperty(JPUserDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);

            userRegistry.loadRegistry();

            userRegistry.addObserver(new Observer<Map<String, IdentifiableMessage<String, Person, Person.Builder>>>() {

                @Override
                public void update(Observable<Map<String, IdentifiableMessage<String, Person, Person.Builder>>> source, Map<String, IdentifiableMessage<String, Person, Person.Builder>> data) throws Exception {
                    notifyChange();
                }
            });

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        super.init(JPService.getProperty(JPUserRegistryScope.class).getValue());
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
            logger.warn("Initial consistency check failed!");
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

        try {
            deactivate();
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }

    @Override
    public final void notifyChange() throws CouldNotPerformException {
        // sync read only flags
        setField(PersonRegistryType.PersonRegistry.PERSON_REGISTRY_READ_ONLY_FIELD_NUMBER, userRegistry.isReadOnly());
        super.notifyChange();
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(UserRegistryInterface.class, this, server);
    }

    public ProtoBufFileSynchronizedRegistry<String, Person, Person.Builder, PersonRegistry.Builder> getPersonRegistry() {
        return userRegistry;
    }

    @Override
    public Person registerUser(Person person) throws CouldNotPerformException {
        return userRegistry.register(person);
    }

    @Override
    public Boolean containsUser(Person person) throws CouldNotPerformException {
        return userRegistry.contains(person);
    }

    @Override
    public Boolean containsUserById(String personId) throws CouldNotPerformException {
        return userRegistry.contains(personId);
    }

    @Override
    public Person updateUser(Person person) throws CouldNotPerformException {
        return userRegistry.update(person);
    }

    @Override
    public Person removeUser(Person person) throws CouldNotPerformException {
        return userRegistry.remove(person);
    }

    @Override
    public Person getUserById(String personId) throws CouldNotPerformException {
        return userRegistry.get(personId).getMessage();
    }

    @Override
    public List<Person> getUsers() throws CouldNotPerformException {
        return userRegistry.getMessages();
    }

    @Override
    public Future<Boolean> isUserRegistryReadOnly() throws CouldNotPerformException {
        return CompletableFuture.completedFuture(userRegistry.isReadOnly());
    }
}
