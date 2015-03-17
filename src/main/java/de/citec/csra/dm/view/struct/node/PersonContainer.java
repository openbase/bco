/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.person.PersonType.Person;

/**
 *
 * @author thuxohl
 */
public class PersonContainer extends NodeContainer<Person.Builder> {

    public PersonContainer(Person.Builder owner) {
        super("Owner", owner);
        super.add(owner.getFirstName(), "first_name");
        super.add(owner.getLastName(), "last_name");
        super.add(owner.getUserName(), "user_name");
        super.add(owner.getId(), "id");
    }
}
