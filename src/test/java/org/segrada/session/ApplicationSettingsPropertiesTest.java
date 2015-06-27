package org.segrada.session;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

public class ApplicationSettingsPropertiesTest {
	/**
	 * instance to test
	 */
	private ApplicationSettingsProperties applicationSettings;

	@Before
	public void setUp() throws Exception {
		applicationSettings = new ApplicationSettingsProperties();
	}

	@Test
	public void testGetSetting() throws Exception {
		assertEquals("TEST", applicationSettings.getSetting("environment"));

		assertEquals("memory:segradatest", applicationSettings.getSetting("orientDB.url"));
	}

	@Test
	public void testSetSetting() throws Exception {
		assertNull(applicationSettings.getSetting("dummy"));

		applicationSettings.setSetting("dummy", "foobar");

		assertEquals("foobar", applicationSettings.getSetting("dummy"));
	}

	@Test
	public void testGetKeys() throws Exception {
		Collection<String> keys = applicationSettings.getKeys();

		assertNotNull(keys);
		assertTrue(keys.size() > 0);
		assertTrue(keys.contains("environment"));
	}
}