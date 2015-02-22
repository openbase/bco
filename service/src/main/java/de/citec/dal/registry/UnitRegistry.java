/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import de.citec.dal.hal.unit.UnitInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 */
public class UnitRegistry {

	private static UnitRegistry instance;

	private final Map<String, UnitInterface> unitMap;

	private UnitRegistry() {
		instance = this;
		this.unitMap = new HashMap<>();
	}

	public static synchronized UnitRegistry getInstance() {
		if (instance == null) {
			instance = new UnitRegistry();
		}
		return instance;
	}

	public void registerUnit(final UnitInterface unit) throws CouldNotPerformException {
		if (unitMap.containsKey(unit.getId())) {
			throw new CouldNotPerformException("Could not register " + unit + "! Unit with same id already registered!");
		}
		unitMap.put(unit.getId(), unit);
	}

	public UnitInterface getUnit(String id) throws NotAvailableException {
		if (unitMap.containsKey(id)) {
			throw new NotAvailableException("Unit[" + id + "]");
		}
		return unitMap.get(id);
	}
}
