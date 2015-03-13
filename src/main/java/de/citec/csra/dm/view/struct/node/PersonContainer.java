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
public class PersonContainer extends NodeContainer<Person> {

    public PersonContainer(Person owner) {
        super("Owner", owner);
        super.add(owner.getFirstName(), "First Name");
        super.add(owner.getLastName(), "Last Name");
        super.add(owner.getUserName(), "User Name");
        super.add(owner.getId(), "ID");
    }
}
