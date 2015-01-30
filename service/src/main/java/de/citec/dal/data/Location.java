/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.dal.data;

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

    private final static Scope SCOPE_PREFIX = new Scope("/home");
    public final static Location HOME = new Location();

	private final String name;
	private final List<Location> children;
	private final Location parent;

        private Location() {
            this.name = "home";
            this.parent = this;
            this.children = new ArrayList<>();
        }
        
        public Location(String name) {
		this.name = name;
		this.parent = HOME;
		this.children = new ArrayList<>();
		this.parent.addChild(this);
	}
        
	public Location(String name, Location parent) {
		this.name = name;
		this.parent = parent;
		this.children = new ArrayList<>();
		this.parent.addChild(this);
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

	public Location getParent() {
		return parent;
	}

	public Scope getScope() {
		if(parent.equals(this)) {
			return SCOPE_PREFIX;
		}
		return parent.getScope().concat(new Scope(ScopeProvider.SEPARATOR+name));
	}
}
