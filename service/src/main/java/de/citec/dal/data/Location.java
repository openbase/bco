/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data;

import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.rsb.ScopeProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rsb.Scope;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 */
public class Location implements ScopeProvider {

	private static final Location NO_PARENT = null;

	private final String name;
	private final List<Location> children;
	private final Location parent;
	private final Scope scope;

	public Location(String name) {
		this(name, NO_PARENT);
	}

	public Location(String name, Location parent) {
		this.name = name;
		this.parent = parent;
		this.scope = generateScope(this);
		this.children = new ArrayList<>();
		if (!isRootLocation()) {
			this.parent.addChild(this);
		}
	}

	protected void addChild(Location location) {
		children.add(location);
	}

	public String getName() {
		return name;
	}

	public List<Location> getChildren() {
		return Collections.unmodifiableList(children);
	}

	public Location getParent() throws NotAvailableException {
		if (parent == NO_PARENT) {
			throw new NotAvailableException("Parent location", "Root location detected!");
		}
		return parent;
	}

	public final boolean isRootLocation() {
		try {
			getParent();
		} catch (NotAvailableException ex) {
			return true;
		}
		return false;
	}

	public static Scope generateScope(final Location location) {
		if (location.isRootLocation()) {
			return new Scope(ScopeProvider.SEPARATOR + location.getName().toLowerCase());
		}

		try {
			return location.getParent().getScope();
		} catch (NotAvailableException ex) {
			throw new AssertionError("Parent location must be available for non root location!", ex);
		}
	}

	@Override
	public Scope getScope() {
		return scope;
	}
}
