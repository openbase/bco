/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.usr.lib.registry;

import de.citec.jul.exception.CouldNotPerformException;
import java.util.List;
import java.util.concurrent.Future;
import rst.person.PersonType.Person;

/**
 *
 * @author mpohling
 */
public interface UserRegistryInterface {

    public Person registerUser(Person person) throws CouldNotPerformException;

    public Boolean containsUser(Person person) throws CouldNotPerformException;

    public Boolean containsUserById(String personId) throws CouldNotPerformException;

    public Person updateUser(Person person) throws CouldNotPerformException;

    public Person removeUser(Person person) throws CouldNotPerformException;

    public Person getUserById(final String personId) throws CouldNotPerformException;

    public List<Person> getUsers() throws CouldNotPerformException;

    public Future<Boolean> isUserRegistryReadOnly() throws CouldNotPerformException;

    public void shutdown();
}
