/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.AbstractUnitController;
import de.citec.dal.hal.unit.Unit;
import de.citec.jul.exception.NotAvailableException;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 */
public class UnitRegistry extends AbstractRegistry<String, Unit> {

//	private static UnitRegistry instance;

	public UnitRegistry() {
//		instance = this;
	}

//	public static synchronized UnitRegistry getInstance() {
//		if (instance == null) {
//			instance = new UnitRegistry();
//		}
//		return instance;
//	}

	public Unit getUnit(String label, Location location, Class<? extends AbstractUnitController> unitClass) throws NotAvailableException {
		return get(AbstractUnitController.generateID(label, location, unitClass));
	}
}
