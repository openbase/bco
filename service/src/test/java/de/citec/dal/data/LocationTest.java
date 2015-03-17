/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import rsb.Scope;

/**
 *
 * @author Divine Threepwood
 */
public class LocationTest {

	public LocationTest() {
	}

	@BeforeClass
	public static void setUpClass() {
	}

	@AfterClass
	public static void tearDownClass() {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	/**
	 * Test of addChild method, of class Location.
	 */
	@Test
	public void testAddChild() {
//		System.out.println("addChild");
//		Location location = null;
//		Location instance = null;
//		instance.addChild(location);
//		// TODO review the generated test code and remove the default call to fail.
//		fail("The test case is a prototype.");
	}

	/**
	 * Test of getName method, of class Location.
	 */
	@Test
	public void testGetName() {
//		System.out.println("getName");
//		Location instance = null;
//		String expResult = "";
//		String result = instance.getName();
//		assertEquals(expResult, result);
//		// TODO review the generated test code and remove the default call to fail.
//		fail("The test case is a prototype.");
	}

	/**
	 * Test of getChildren method, of class Location.
	 */
	@Test
	public void testGetChildren() {
//		System.out.println("getChildren");
//		Location instance = null;
//		List<Location> expResult = null;
//		List<Location> result = instance.getChildren();
//		assertEquals(expResult, result);
//		// TODO review the generated test code and remove the default call to fail.
//		fail("The test case is a prototype.");
	}

	/**
	 * Test of getParent method, of class Location.
	 */
	@Test
	public void testGetParent() throws Exception {
//		System.out.println("getParent");
//		Location instance = null;
//		Location expResult = null;
//		Location result = instance.getParent();
//		assertEquals(expResult, result);
//		// TODO review the generated test code and remove the default call to fail.
//		fail("The test case is a prototype.");
	}

	/**
	 * Test of isRootLocation method, of class Location.
	 */
	@Test
	public void testIsRootLocation() {
//		System.out.println("isRootLocation");
//		Location instance = null;
//		boolean expResult = false;
//		boolean result = instance.isRootLocation();
//		assertEquals(expResult, result);
//		// TODO review the generated test code and remove the default call to fail.
//		fail("The test case is a prototype.");
	}

	/**
	 * Test of generateScope method, of class Location.
	 */
	@Test
	public void testGenerateScope() {
		System.out.println("generateScope");
		Location location = new Location("tabel", new Location("room", new Location("home")));
		String expResult = "/home/room/tabel/";
		Scope result = Location.generateScope(location);
		assertEquals(expResult, result.toString());
	}

	/**
	 * Test of getScope method, of class Location.
	 */
	@Test
	public void testGetScope() {
//		System.out.println("getScope");
//		Location instance = null;
//		Scope expResult = null;
//		Scope result = instance.getScope();
//		assertEquals(expResult, result);
//		// TODO review the generated test code and remove the default call to fail.
//		fail("The test case is a prototype.");
	}

}
