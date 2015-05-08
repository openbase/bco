/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.extension.rsb.scope.ScopeProvider;
import de.citec.jul.extension.rsb.scope.ScopeTransformer;
import rsb.Scope;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author Divine Threepwood
 */
public class Location implements ScopeProvider {

//	private static final Location NO_PARENT = null;

//	private final String name;
//	private final List<Location> children;
//	private final Location parent;
//	private final Scope scope;
    private final LocationConfig config;

	public Location(final LocationConfig config) {
        this.config = config;
//		this(name, NO_PARENT);

	}

//	public Location(String name, Location parent) {
//		this.name = name;
//		this.parent = parent;
//		this.scope = generateScope(this);
//		this.children = new ArrayList<>();
//
//		if (!isRootLocation()) {
//			this.parent.addChild(this);
//		}
//	}

//	protected void addChild(Location location) {
//		children.add(location);
//	}

	public String getLabel() {
		return config.getLabel();
	}

//	public List<Location> getChildren() {
//		return Collections.unmodifiableList(children);
//	}

	public final boolean isRoot() {
		return config.getRoot();
	}


//	public static Scope generateScope(final Location location) {
//		if (location.isRootLocation()) {
//			return new Scope(Scope.COMPONENT_SEPARATOR + location.getName().toLowerCase());
//		}
//
//		try {
//			return generateScope(location.getParent()).concat(new Scope(Scope.COMPONENT_SEPARATOR + location.getName().toLowerCase()));
//		} catch (NotAvailableException ex) {
//			throw new AssertionError("Parent location must be available for non root location!", ex);
//		}
//	}

	@Override
	public Scope getScope() throws CouldNotPerformException{
		return ScopeTransformer.transform(config.getScope());
	}

    public LocationConfig getConfig() {
        return config;
    }

	@Override
	public String toString() {
        try {
            return getClass().getSimpleName()+"["+getScope()+"]";
        } catch (CouldNotPerformException ex) {
            return getClass().getSimpleName()+"[?]";
        }
	}
}
