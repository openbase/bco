/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.csra.dm;

import de.unibi.csra.dm.struct.DeviceConfig;
import de.unibi.csra.dm.struct.DataStream;
import de.unibi.csra.dm.struct.GlobalConfig;
import de.unibi.csra.dm.struct.DeviceClass;
import de.unibi.csra.dm.struct.DeviceInstance;
import de.unibi.csra.dm.tools.Serializer;
import de.unibi.agai.clparser.CLParser;
import de.unibi.agai.clparser.command.CLDataStreamDirectory;
import de.unibi.agai.clparser.command.CLDeviceClassDirectory;
import de.unibi.agai.clparser.command.CLDeviceConfigDirectory;
import de.unibi.agai.clparser.command.CLDeviceInstanceDirectory;
import de.unibi.agai.clparser.command.CLDeviceManagerConfigPath;
import de.unibi.agai.clparser.command.CLGlobalConfigDirectory;
import de.unibi.agai.clparser.command.ShowGUIFlag;
import de.unibi.csra.dm.exception.InvalidOperationException;
import de.unibi.csra.dm.exception.NotAvailableException;
import de.unibi.csra.dm.tools.Manageable;
import de.unibi.csra.dm.view.DevieManagerGUI;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mpohling
 */
public class DeviceManager {

	public final static String DATA_UPDATE = "DataUpdate";

	private static DeviceManager instance;

	private final Serializer serializer;
	private final PropertyChangeSupport changes;

	private final Map<String, DeviceClass> deviceClassMap;
	private final Map<String, DeviceInstance> deviceInstanceMap;
	private final Map<String, DeviceConfig> deviceConfigMap;
	private final Map<String, DataStream> dataStreamMap;
	private final Map<String, GlobalConfig> globalConfigMap;

	public static final synchronized DeviceManager getInstance() {
		if (instance == null) {
			instance = new DeviceManager();
		}
		return instance;
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {

		/* Setup CLParser */
		CLParser.setProgramName("DeviceManager");
		CLParser.registerCommand(CLDeviceManagerConfigPath.class);
		CLParser.registerCommand(CLDataStreamDirectory.class);
		CLParser.registerCommand(CLDeviceClassDirectory.class);
		CLParser.registerCommand(CLDeviceConfigDirectory.class);
		CLParser.registerCommand(CLDeviceInstanceDirectory.class);
		CLParser.registerCommand(CLGlobalConfigDirectory.class);
		CLParser.registerCommand(ShowGUIFlag.class, true);
		CLParser.analyseAndExitOnError(args);

		if (CLParser.getAttribute(ShowGUIFlag.class).getValue()) {
			DevieManagerGUI.main(args);
		}
		getInstance();
	}

	private DeviceManager() {
		this.deviceClassMap = new HashMap<>();
		this.deviceInstanceMap = new HashMap<>();
		this.deviceConfigMap = new HashMap<>();
		this.dataStreamMap = new HashMap<>();
		this.globalConfigMap = new HashMap<>();
		this.serializer = new Serializer();
		this.changes = new PropertyChangeSupport(this);
		this.load();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				save();
			}
		});
	}

	public void addDeviceInstance(final DeviceInstance device) throws InvalidOperationException {
		checkID(device);
		deviceInstanceMap.put(device.getId(), device);
		notifyDataUpdate();
	}

	public void addDeviceClass(final DeviceClass deviceClass) throws InvalidOperationException {
		checkID(deviceClass);
		deviceClassMap.put(deviceClass.getId(), deviceClass);
		notifyDataUpdate();
	}

	public void addDeviceConfig(final DeviceConfig deviceConfig) throws InvalidOperationException {
		checkID(deviceConfig);
		deviceConfigMap.put(deviceConfig.getId(), deviceConfig);
		notifyDataUpdate();
	}

	public void addDataStream(final DataStream dataStream) throws InvalidOperationException {
		checkID(dataStream);
		dataStreamMap.put(dataStream.getId(), dataStream);
		notifyDataUpdate();
	}

	public void addGlobalConfig(final GlobalConfig globalConfig) throws InvalidOperationException {
		checkID(globalConfig);
		globalConfigMap.put(globalConfig.getId(), globalConfig);
		notifyDataUpdate();
	}

	public final void notifyDataUpdate() {
		changes.firePropertyChange(DATA_UPDATE, null, null);
	}
	
	private void checkID(Manageable context) throws InvalidOperationException {
		String id = context.getId();
		
		if(id.equals("")) {
			throw new InvalidOperationException("Invalid ID");
		}
	}

	public final void save() {

		for (DeviceClass deviceClass : deviceClassMap.values()) {
			try {
				serializer.serialize(deviceClass, deviceClass.getFile(), DeviceClass.class);
			} catch (IOException ex) {
				Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, "Could not save " + deviceClass, ex);
			}
		}

		for (DeviceInstance deviceInstance : deviceInstanceMap.values()) {
			try {
				serializer.serialize(deviceInstance, deviceInstance.getFile(), DeviceInstance.class);
			} catch (IOException ex) {
				Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, "Could not save " + deviceInstance, ex);
			}
		}

		for (DeviceConfig deviceConfig : deviceConfigMap.values()) {
			try {
				serializer.serialize(deviceConfig, deviceConfig.getFile(), DeviceConfig.class);
			} catch (IOException ex) {
				Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, "Could not save " + deviceConfig, ex);
			}
		}

		for (DataStream dataStream : dataStreamMap.values()) {
			try {
				serializer.serialize(dataStream, dataStream.getFile(), DataStream.class);
			} catch (IOException ex) {
				Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, "Could not save " + dataStream, ex);
			}
		}

		for (GlobalConfig globalConfig : globalConfigMap.values()) {
			try {
				serializer.serialize(globalConfig, globalConfig.getFile(), GlobalConfig.class);
			} catch (IOException ex) {
				Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, "Could not save " + globalConfig, ex);
			}
		}
	}

	public final void load() {
		
		//TODO display warning in case of changes which will not be saved.
		deviceClassMap.clear();
		deviceInstanceMap.clear();
		deviceConfigMap.clear();
		dataStreamMap.clear();
		globalConfigMap.clear();
		
		FileFilter fileFilter = new FileFilter() {

			@Override
			public boolean accept(File file) {
				return (!file.isHidden()) && file.isFile();
			}
		};
		
		for (File file : CLParser.getAttribute(CLDeviceClassDirectory.class).getValue().listFiles(fileFilter)) {
			try {
				addDeviceClass(serializer.deserialize(file, DeviceClass.class));
			} catch (InvalidOperationException | IOException ex) {
				Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, "Could not load " + file, ex);
			}
		}

		for (File file : CLParser.getAttribute(CLDeviceInstanceDirectory.class).getValue().listFiles(fileFilter)) {
			try {
				addDeviceInstance(serializer.deserialize(file, DeviceInstance.class));
			} catch (InvalidOperationException | IOException ex) {
				Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, "Could not load " + file, ex);
			}
		}

		for (File file : CLParser.getAttribute(CLDeviceConfigDirectory.class).getValue().listFiles(fileFilter)) {
			try {
				addDeviceConfig(serializer.deserialize(file, DeviceConfig.class));
			} catch (InvalidOperationException | IOException ex) {
				Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, "Could not load " + file, ex);
			}
		}

		for (File file : CLParser.getAttribute(CLDataStreamDirectory.class).getValue().listFiles(fileFilter)) {
			try {
				addDataStream(serializer.deserialize(file, DataStream.class));
			} catch (InvalidOperationException | IOException ex) {
				Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, "Could not load " + file, ex);
			}
		}

		for (File file : CLParser.getAttribute(CLGlobalConfigDirectory.class).getValue().listFiles(fileFilter)) {
			try {
				addGlobalConfig(serializer.deserialize(file, GlobalConfig.class));
			} catch (InvalidOperationException | IOException ex) {
				Logger.getLogger(DeviceManager.class.getName()).log(Level.SEVERE, "Could not load " + file, ex);
			}
		}
	}

	public Map<String, DeviceClass> getDeviceClassMap() {
		return Collections.unmodifiableMap(deviceClassMap);
	}

	public Map<String, DeviceInstance> getDeviceInstanceMap() {
		return Collections.unmodifiableMap(deviceInstanceMap);
	}

	public Map<String, DeviceConfig> getDeviceConfigMap() {
		return Collections.unmodifiableMap(deviceConfigMap);
	}

	public Map<String, DataStream> getDataStreamMap() {
		return Collections.unmodifiableMap(dataStreamMap);
	}

	public Map<String, GlobalConfig> getGlobalConfigMap() {
		return Collections.unmodifiableMap(globalConfigMap);
	}

	public List<DeviceInstance> getDeviceInstanceList() {
		return new ArrayList(deviceInstanceMap.values());
	}

	public List<DeviceClass> getDeviceClassList() {
		return new ArrayList(deviceClassMap.values());
	}

	public List<DeviceConfig> getDeviceConfigList() {
		return new ArrayList(deviceConfigMap.values());
	}

	public List<DataStream> getDataStreamList() {
		return new ArrayList(dataStreamMap.values());
	}

	public List<GlobalConfig> getGlobalConfigList() {
		return new ArrayList(globalConfigMap.values());
	}

	public DeviceInstance getDeviceInstance(final String id) throws NotAvailableException {
		if(!deviceInstanceMap.containsKey(id)) {
			throw new NotAvailableException(id);
		}
		return deviceInstanceMap.get(id);
	}

	public DeviceClass getDeviceClass(final String id) throws NotAvailableException {
		if(!deviceClassMap.containsKey(id)) {
			throw new NotAvailableException(id);
		}
		return deviceClassMap.get(id);
	}

	public DeviceConfig getDeviceConfig(final String id) throws NotAvailableException {
		if(!deviceConfigMap.containsKey(id)) {
			throw new NotAvailableException(id);
		}
		return deviceConfigMap.get(id);
	}

	public DataStream getDataStream(final String id) throws NotAvailableException {
		if(!dataStreamMap.containsKey(id)) {
			throw new NotAvailableException(id);
		}
		return dataStreamMap.get(id);
	}

	public GlobalConfig getGlobalConfig(final String id) throws NotAvailableException {
		if(!globalConfigMap.containsKey(id)) {
			throw new NotAvailableException(id);
		}
		return globalConfigMap.get(id);
	}
	
	public void removeDeviceInstance(final String id) {
		deviceInstanceMap.remove(id).getFile().delete();
		notifyDataUpdate();
	}

	public void removeDeviceClass(final String id) {
		deviceClassMap.remove(id).getFile().delete();
		notifyDataUpdate();
	}

	public void removeDeviceConfig(final String id) {
		deviceConfigMap.get(id).getFile().delete();
		notifyDataUpdate();
	}

	public void removeDataStream(final String id) {
		dataStreamMap.remove(id).getFile().delete();
		notifyDataUpdate();
	}

	public void removeGlobalConfig(final String id) {
		globalConfigMap.remove(id).getFile().delete();
		notifyDataUpdate();
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		changes.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		changes.removePropertyChangeListener(listener);
	}
}
