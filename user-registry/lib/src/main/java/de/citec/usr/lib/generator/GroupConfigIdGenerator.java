/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.usr.lib.generator;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.extension.protobuf.IdGenerator;
import rst.person.PersonType.Person;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class GroupConfigIdGenerator implements IdGenerator<String, Person> {

    @Override
    public String generateId(Person message) throws CouldNotPerformException {
        
        //TODO: thuxohl, mpohling UUID?
        throw new UnsupportedOperationException("");
    }
}
