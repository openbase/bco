/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.pem.remote;

import de.citec.jp.JPUserRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPReadOnly;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.extension.rsb.com.RPCHelper;
import de.citec.jul.extension.rsb.com.RSBRemoteService;
import de.citec.jul.storage.registry.RemoteRegistry;
import de.citec.usr.lib.generator.UserConfigIdGenerator;
import de.citec.usr.lib.registry.UserRegistryInterface;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.person.PersonRegistryType.PersonRegistry;
import rst.person.PersonType.Person;

/**
 *
 * @author mpohling
 */
public class UserRegistryRemote extends RSBRemoteService<PersonRegistry> implements UserRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PersonRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Person.getDefaultInstance()));
    }

    private final RemoteRegistry<String, Person, Person.Builder, PersonRegistry.Builder> userRemoteRegistry;

    public UserRegistryRemote() throws InstantiationException {
        try {
            userRemoteRegistry = new RemoteRegistry<>(new UserConfigIdGenerator());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException {
        super.init(JPService.getProperty(JPUserRegistryScope.class).getValue());
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        try {
            notifyUpdated(requestStatus());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial registry sync failed!", ex), logger, LogLevel.WARN);
        }
    }

    @Override
    public void notifyUpdated(final PersonRegistry data) throws CouldNotPerformException {
        userRemoteRegistry.notifyRegistryUpdated(data.getPersonList());
    }

    public RemoteRegistry<String, Person, Person.Builder, PersonRegistry.Builder> getPersonRemoteRegistry() {
        return userRemoteRegistry;
    }

    @Override
    public Person registerUser(final Person person) throws CouldNotPerformException {
        try {
            return (Person) callMethod("registerPersonConfig", person);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register person config!", ex);
        }
    }

    @Override
    public Person getUserById(String personId) throws CouldNotPerformException, NotAvailableException {
        getData();
        return userRemoteRegistry.getMessage(personId);
    }

    @Override
    public Boolean containsUser(final Person person) throws CouldNotPerformException {
        getData();
        return userRemoteRegistry.contains(person);
    }

    @Override
    public Boolean containsUserById(final String personId) throws CouldNotPerformException {
        getData();
        return userRemoteRegistry.contains(personId);
    }

    @Override
    public Person updateUser(final Person person) throws CouldNotPerformException {
        try {
            return (Person) callMethod("updatePerson", person);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update person[" + person + "]!", ex);
        }
    }

    @Override
    public Person removeUser(final Person person) throws CouldNotPerformException {
        try {
            return (Person) callMethod("removePerson", person);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove person[" + person + "]!", ex);
        }
    }

    @Override
    public List<Person> getUsers() throws CouldNotPerformException, NotAvailableException {
        getData();
        List<Person> messages = userRemoteRegistry.getMessages();
        return messages;
    }

    @Override
    public Future<Boolean> isUserRegistryReadOnly() throws CouldNotPerformException {
        if (JPService.getProperty(JPReadOnly.class).getValue() || !isConnected()) {
            return CompletableFuture.completedFuture(true);
        }
        try {
            return RPCHelper.callRemoteMethod(this, Boolean.class);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not return read only state of the person config registry!!", ex);
        }
    }
}
