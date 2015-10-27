/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uka.ipd.idaho.im.imagine.application;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.uka.ipd.idaho.easyIO.EasyIO;
import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.swing.ProgressMonitorDialog;
import de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration;
import de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration.ConfigurationDescriptor;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants.StatusDialog.StatusDialogButton;
import de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils;
import de.uka.ipd.idaho.goldenGate.configuration.FileConfiguration;
import de.uka.ipd.idaho.goldenGate.configuration.UrlConfiguration;
import de.uka.ipd.idaho.im.imagine.GoldenGateImagine;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Application wrapper for GoldenGATE Imagine
 * 
 * @author sautter
 */
public class GoldenGateImagineApplication implements GoldenGateConstants {
	private static File BASE_PATH = null;
	private static boolean ONLINE = false;
	
	private static Settings PARAMETERS = new Settings();
	private static StringVector UPDATE_HOSTS = new StringVector();
	private static StringVector CONFIG_HOSTS = new StringVector();
	
	private static final String LOG_TIMESTAMP_DATE_FORMAT = "yyyyMMdd-HHmm";
	private static final DateFormat LOG_TIMESTAMP_FORMATTER = new SimpleDateFormat(LOG_TIMESTAMP_DATE_FORMAT);
	
	private static void writeParameterFile() {
		File parameterFile = new File(BASE_PATH, PARAMETER_FILE_NAME);
		
		//	rename existing file to .old if necessary
		if (parameterFile.exists()) {
			String parameterFileName = parameterFile.toString();
			File oldParameterFile = new File(parameterFileName + ".old");
			if (oldParameterFile.exists() && oldParameterFile.delete())
				oldParameterFile = new File(parameterFileName + ".old");
			parameterFile.renameTo(oldParameterFile);
			parameterFile = new File(parameterFileName);
		}
		
		try {
			BufferedWriter parameterWriter = new BufferedWriter(new FileWriter(parameterFile));
			parameterWriter.write(START_MEMORY_NAME + "=" + PARAMETERS.getSetting(START_MEMORY_NAME, DEFAULT_START_MEMORY));
			parameterWriter.newLine();
			parameterWriter.write(MAX_MEMORY_NAME + "=" + PARAMETERS.getSetting(MAX_MEMORY_NAME, DEFAULT_MAX_MEMORY));
			parameterWriter.newLine();
			if (PARAMETERS.containsKey(LOG_PATH)) {
				parameterWriter.write(LOG_PATH + "=" + PARAMETERS.getSetting(LOG_PATH));
				parameterWriter.newLine();
			}
			if (PARAMETERS.containsKey(PROXY_NAME)) {
				parameterWriter.write(PROXY_NAME + "=" + PARAMETERS.getSetting(PROXY_NAME));
				parameterWriter.newLine();
			}
			if (PARAMETERS.containsKey(PROXY_PORT)) {
				parameterWriter.write(PROXY_PORT + "=" + PARAMETERS.getSetting(PROXY_PORT));
				parameterWriter.newLine();
			}
			if (PARAMETERS.containsKey(PROXY_USER) && PARAMETERS.containsKey(PROXY_PWD)) {
				parameterWriter.write(PROXY_USER + "=" + PARAMETERS.getSetting(PROXY_USER));
				parameterWriter.newLine();
				parameterWriter.write(PROXY_PWD + "=" + PARAMETERS.getSetting(PROXY_PWD));
				parameterWriter.newLine();
			}
			parameterWriter.flush();
			parameterWriter.close();
		} catch (IOException e) {}
	}
	
	private static void writeUpdateHostFile() {
		File updateHostFile = new File(BASE_PATH, UPDATE_HOST_FILE_NAME);
		
		//	remane existing file to .old if necessary
		if (updateHostFile.exists()) {
			String updateHostFileName = updateHostFile.toString();
			File oldUpdateHostFile = new File(updateHostFileName + ".old");
			if (oldUpdateHostFile.exists() && oldUpdateHostFile.delete())
				oldUpdateHostFile = new File(updateHostFileName + ".old");
			updateHostFile.renameTo(oldUpdateHostFile);
			updateHostFile = new File(updateHostFileName);
		}
		
		try {
			UPDATE_HOSTS.storeContent(updateHostFile);
		} catch (IOException ioe) {}
	}
	
	private static void writeConfigHostFile() {
		File configHostFile = new File(BASE_PATH, CONFIG_HOST_FILE_NAME);
		
		//	remane existing file to .old if necessary
		if (configHostFile.exists()) {
			String configHostFileName = configHostFile.toString();
			File oldConfigHostFile = new File(configHostFileName + ".old");
			if (oldConfigHostFile.exists() && oldConfigHostFile.delete())
				oldConfigHostFile = new File(configHostFileName + ".old");
			configHostFile.renameTo(oldConfigHostFile);
			configHostFile = new File(configHostFileName);
		}
		
		try {
			CONFIG_HOSTS.storeContent(configHostFile);
		} catch (IOException ioe) {}
	}
	
	/**	the main method to run GoldenGATE Imagine as a standalone application
	 * @param args the arguments, which have the following meaning:<ul>
	 * <li>args[0]: the RUN parameter (if not specified, the GoldenGATE.bat startup script will be created)</li>
	 * <li>args[1]: the ONLINE parameter (if not specified, GoldenGATE will run purely offline and will not allow its plugin components to access the network or WWW)</li>
	 * <li>args[2]: the root path of the GoldenGATE installation (if not specified, GoldenGATE will use the current path instead, i.e. './')</li>
	 * <li>args[3]: the file to open (this parameter is used to handle a file drag&dropped on the GoldenGATE.bat startup script)</li>
	 * </ul>
	 */
	public static void main(String[] args) throws Exception {
		
		//	set platform L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		//	if no args, jar has been started directly
		if ((args.length == 0) || !RUN_PARAMETER.equals(args[0])) {
			File file = new File("./GoldenGateImagine.bat");
			
			//	create batch file if not exists
			if (!file.exists()) {
				try {
					EasyIO.writeFile(file, ("java -jar -Xms" + DEFAULT_START_MEMORY + "m -Xmx" + DEFAULT_MAX_MEMORY + "m GoldenGATE.jar " + RUN_PARAMETER + " %1"));
				} catch (IOException ioe) {}
			}
			
			//	batch file exists, show error
			else JOptionPane.showMessageDialog(null, "Please use GoldenGateImagineStarter.jar to start GoldenGATE Imagine.", "Please Use GoldenGateImagineStarter.jar", JOptionPane.INFORMATION_MESSAGE);
			
			//	we're done here
			System.exit(0);
			return;
		}
		
		//	adjust basic parameters
		String basePath = "./";
		StringVector filesToOpen = new StringVector();
		String logFileName = ("GgImagine." + LOG_TIMESTAMP_FORMATTER.format(new Date()) + ".log");
		
		//	parse remaining args
		for (int a = 1; a < args.length; a++) {
			String arg = args[a];
			if (arg != null) {
				if (arg.startsWith(BASE_PATH_PARAMETER + "="))
					basePath = arg.substring((BASE_PATH_PARAMETER + "=").length());
				else if (ONLINE_PARAMETER.equals(arg))
					ONLINE = true;
				else if (arg.equals(LOG_PARAMETER + "=IDE") || arg.equals(LOG_PARAMETER + "=NO"))
					logFileName = null;
				else if (arg.startsWith(LOG_PARAMETER + "="))
					logFileName = arg.substring((LOG_PARAMETER + "=").length());
				else filesToOpen.addElementIgnoreDuplicates(arg);
			}
		}
		
		//	remember program base path
		BASE_PATH = new File(basePath);
		
		//	keep user posted
		StatusDialog sd = new StatusDialog(Toolkit.getDefaultToolkit().getImage(new File(new File(BASE_PATH, DATA_FOLDER_NAME), ICON_FILE_NAME).toString()), "GoldenGATE Editor Initializing");
		sd.popUp();
		
		//	load parameters
		sd.setStatusLabel("Loading parameters");
		try {
			StringVector parameters = StringVector.loadList(new File(BASE_PATH, PARAMETER_FILE_NAME));
			for (int p = 0; p < parameters.size(); p++) try {
				String param = parameters.get(p);
				int split = param.indexOf('=');
				if (split != -1) {
					String key = param.substring(0, split).trim();
					String value = param.substring(split + 1).trim();
					if ((key.length() != 0) && (value.length() != 0))
						PARAMETERS.setSetting(key, value);
				}
			} catch (Exception e) {}
		} catch (Exception e) {}
		
		//	configure web access
		if (PARAMETERS.containsKey(PROXY_NAME)) {
			System.getProperties().put("proxySet", "true");
			System.getProperties().put("proxyHost", PARAMETERS.getSetting(PROXY_NAME));
			if (PARAMETERS.containsKey(PROXY_PORT))
				System.getProperties().put("proxyPort", PARAMETERS.getSetting(PROXY_PORT));
			
			if (PARAMETERS.containsKey(PROXY_USER) && PARAMETERS.containsKey(PROXY_PWD)) {
				//	initialize proxy authentication
			}
		}
		
		//	create log files if required
		File logFolder = null;
		File logFileOut = null;
		File logFileErr = null;
		if (logFileName != null) try {
			
			//	truncate log file extension
			if (logFileName.endsWith(".log"))
				logFileName = logFileName.substring(0, (logFileName.length() - ".log".length()));
			
			//	create absolute log files
			if (logFileName.startsWith("/") || (logFileName.indexOf(':') != -1)) {
				logFileOut = new File(logFileName + ".out.log");
				logFileErr = new File(logFileName + ".err.log");
				logFolder = logFileOut.getAbsoluteFile().getParentFile();
			}
			
			//	create relative log files (the usual case)
			else {
				
				//	get log path
				String logFolderName = PARAMETERS.getSetting(LOG_PATH, LOG_FOLDER_NAME);
				if (logFolderName.startsWith("/") || (logFolderName.indexOf(':') != -1))
					logFolder = new File(logFolderName);
				else logFolder = new File(BASE_PATH, logFolderName);
				logFolder = logFolder.getAbsoluteFile();
				logFolder.mkdirs();
				
				//	create log files
				logFileOut = new File(logFolder, (logFileName + ".out.log"));
				logFileErr = new File(logFolder, (logFileName + ".err.log"));
			}
			
			//	redirect System.out
			logFileOut.getParentFile().mkdirs();
			logFileOut.createNewFile();
			System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(logFileOut)), true, "UTF-8"));
			
			//	redirect System.err
			logFileErr.getParentFile().mkdirs();
			logFileErr.createNewFile();
			System.setErr(new PrintStream(new BufferedOutputStream(new FileOutputStream(logFileErr)), true, "UTF-8"));
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Could not create log files in folder '" + logFolder.getAbsolutePath() + "'." +
					"\nCommon reasons are a full hard drive, lack of write permissions to the folder, or system protection software." +
					"\nUse the 'Configure' button in the configuration selector dialog to select a different log location." +
					"\nThen exit and re-start GoldenGATE Imagine to apply the change." +
					"\n\nNote that you can work normally without the log files, it's just that in case of an error, there are" +
					"\nno log files to to help investigate what exactly went wrong and help developers fix the problem.", "Error Creating Log Files", JOptionPane.ERROR_MESSAGE);
		}
		
		//	load update hosts (necessary for editing)
		sd.setStatusLabel("Loading update hosts");
		try {
			StringVector updateHosts = StringVector.loadList(new File(BASE_PATH, UPDATE_HOST_FILE_NAME));
			UPDATE_HOSTS.addContentIgnoreDuplicates(updateHosts);
		} catch (Exception e) {}
		
		//	load configuration hosts
		sd.setStatusLabel("Loading config hosts");
		try {
			StringVector configHosts = StringVector.loadList(new File(BASE_PATH, CONFIG_HOST_FILE_NAME));
			CONFIG_HOSTS.addContentIgnoreDuplicates(configHosts);
		} catch (Exception e) {}
		
		//	get available configurations
		sd.setStepLabel("Listing Configurations");
		ConfigurationDescriptor[] configurations = getConfigurations(BASE_PATH, sd);
		
		//	hide status dialog
		sd.setVisible(false);
		
		//	select new configuration
		ConfigurationDescriptor configuration = selectConfiguration(configurations, BASE_PATH);
		
		//	check if cancelled
		if (configuration == null) {
			System.exit(0);
			return;
		}
		
		//	open GoldenGATE Imagine window
		GoldenGateConfiguration ggiConfig = null;
		
		//	local master configuration selected
		if (LOCAL_MASTER_CONFIG_NAME.equals(configuration.name))
			ggiConfig = new FileConfiguration(configuration.name, BASE_PATH, true, ONLINE, null);
		
		//	other local configuration selected
		else if (configuration.host == null)
			ggiConfig = new FileConfiguration(configuration.name, new File(new File(BASE_PATH, CONFIG_FOLDER_NAME), configuration.name), false, ONLINE, null);
		
		//	remote configuration selected
		else ggiConfig = new UrlConfiguration((configuration.host + (configuration.host.endsWith("/") ? "" : "/") + configuration.name), configuration.name);
		
		//	TODOne (selector popup remains specific to this class): create factory for configuration, select implementation there
		GoldenGateImagine goldenGateImagine = GoldenGateImagine.openGoldenGATE(ggiConfig, BASE_PATH, true);
		System.out.println("GoldenGATE Imagine core created, configuration is " + configuration.name);
		
		//	load GoldenGATE Imagine specific settings
		final Settings ggiSettings = Settings.loadSettings(new File(BASE_PATH, "GgImagine.cnfg"));
		
		//	open GoldenGATE Imagine window
		GoldenGateImagineUI ggiUi = new GoldenGateImagineUI(goldenGateImagine, ggiSettings);
		ggiUi.setIconImage(ggiConfig.getIconImage());
		ggiUi.setVisible(true);
		System.out.println(" - window opened");
		
		//	store configuration and exit when window closed
		ggiUi.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent we) {
				try {
					Settings.storeSettingsAsText(new File(BASE_PATH, "GgImagine.cnfg"), ggiSettings);
				}
				catch (IOException ioe) {
					ioe.printStackTrace(System.out);
				}
				System.exit(0);
			}
		});
	}
	
	private static ConfigurationDescriptor selectConfiguration(ConfigurationDescriptor[] configurations, File dataBasePath) {
		
		//	show selection dialog
		SelectConfigurationDialog scd = new SelectConfigurationDialog(Toolkit.getDefaultToolkit().getImage(new File(new File(dataBasePath, DATA_FOLDER_NAME), ICON_FILE_NAME).toString()), configurations, dataBasePath);
		scd.setVisible(true);
		
		//	wait for selection dialog (it's a JFrame now, setVisible() won't block)
		while (!scd.isVisible()) try {
			Thread.sleep(50);
		} catch (InterruptedException ie) {}
		while (scd.isVisible()) try {
			Thread.sleep(50);
		} catch (InterruptedException ie) {}
		
		//	dialog cancelled
		if (scd.selectedConfig == null)
			return null;
		
		//	get selected configuration, doing update if required
		return ConfigurationUtils.getConfiguration(configurations, scd.selectedConfig.name, dataBasePath, true);
	}
	
	private static ConfigurationDescriptor[] getConfigurations(File dataBasePath, final StatusDialog sd) {
		
		//	collect configurations
		final ArrayList configList = new ArrayList();
		
		//	show activity
		sd.setStatusLabel("- installed/available locally");
		
		//	add local default configuration
		ConfigurationDescriptor defaultConfig = new ConfigurationDescriptor(null, LOCAL_MASTER_CONFIG_NAME, System.currentTimeMillis());
		configList.add(defaultConfig);
		
		//	load local non-default configurations
		ConfigurationDescriptor[] configs = ConfigurationUtils.getLocalConfigurations(dataBasePath);
		for (int c = 0; c < configs.length; c++) {
			if (configs[c].name.endsWith(".imagine"))
				configList.add(configs[c]);
		}
		
		//	get downloaded zip files
		configs = ConfigurationUtils.getZipConfigurations(dataBasePath);
		for (int c = 0; c < configs.length; c++) {
			if (configs[c].name.endsWith(".imagine"))
				configList.add(configs[c]);
		}
		
		//	no permission to download configurations, we're done here
		if (!ONLINE)
			return ((ConfigurationDescriptor[]) configList.toArray(new ConfigurationDescriptor[configList.size()]));
		
		//	set up for remembering to store config hosts
		final boolean[] configHostsModified = {false};
		
		//	get available configurations from configuration hosts
		for (int h = 0; h < CONFIG_HOSTS.size(); h++) {
			final int ch = h;
			final String configHost = CONFIG_HOSTS.get(ch).trim();
			if (configHost.startsWith("//"))
				continue;
			
			//	show activity
			sd.setStatusLabel("- from " + configHost);
			
			//	create control objects
			final Object configFetcherLock = new Object();
			final boolean[] addHostConfigs = {true};
			
			//	build buttons
			JButton skipButton = new StatusDialogButton("Skip", 5, 2);
			skipButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					synchronized (configFetcherLock) {
						addHostConfigs[0] = false;
						configFetcherLock.notify();
					}
				}
			});
			
			JButton alwaysSkipButton = new StatusDialogButton("Always Skip", 5, 2);
			alwaysSkipButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					synchronized (configFetcherLock) {
						if (JOptionPane.showConfirmDialog(sd, ("This will permanently de-activate '" + configHost + "' as a configuration host. Proceed?\nTo re-activate it, click the 'Configure' button in the 'Select Configuration' dialog and remove the\ninitial double slash ('//') from '" + configHost + "' in the 'Config Hosts' field."), "Really Deactivate?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
							CONFIG_HOSTS.setElementAt(("//" + configHost), ch);
							configHostsModified[0] = true;
						}
						addHostConfigs[0] = false;
						configFetcherLock.notify();
					}
				}
			});
			
			//	line up button panel
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			buttonPanel.setBackground(Color.WHITE);
			buttonPanel.add(skipButton);
			buttonPanel.add(alwaysSkipButton);
			
			//	show buttons
			sd.setCustomComponent(buttonPanel);
			
			//	load configs in extra thread
			Thread configFetcher = new Thread() {
				public void run() {
					ConfigurationDescriptor[] hostConfigs = ConfigurationUtils.getRemoteConfigurations(configHost);
					synchronized (configFetcherLock) {
						if (!addHostConfigs[0])
							return;
						for (int c = 0; c < hostConfigs.length; c++) {
							if (hostConfigs[c].name.endsWith(".imagine"))
								configList.add(hostConfigs[c]);
						}
						configFetcherLock.notify();
					}
					System.out.println("Config fetcher terminared for " + configHost);
				}
			};
			
			//	start download and block main thread
			synchronized (configFetcherLock) {
				configFetcher.start();
				System.out.println("Config fetcher started for " + configHost);
				try {
					configFetcherLock.wait();
				}
				catch (InterruptedException ie) {
					System.out.println("Interrupted waiting for config fetcher from " + configHost);
					addHostConfigs[0] = false;
				}
			}
		}
		
		//	store config hosts if modified
		if (configHostsModified[0])
			writeConfigHostFile();
		
		//	finally ...
		return ((ConfigurationDescriptor[]) configList.toArray(new ConfigurationDescriptor[configList.size()]));
	}
	
	/**
	 * Dialog for selecting the GoldenGATE Configuration to load
	 * 
	 * @author sautter
	 */
	private static class SelectConfigurationDialog extends JFrame {
		private static final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm");
		private ConfigurationDescriptor[] configs;
		private File masterBasePath;
		private ConfigurationDescriptor selectedConfig = null;
		private JButton makeMasterConfigButton = new JButton("Make Master");
		SelectConfigurationDialog(Image icon, ConfigurationDescriptor[] configs, File basePath) {
			super("Select Configuration");
			this.setIconImage(icon);
			this.configs = configs;
			this.masterBasePath = basePath;
			this.getContentPane().setLayout(new BorderLayout());
			
			this.getContentPane().add(new JLabel("Please select the configuration to load.", JLabel.LEFT), BorderLayout.NORTH);
			
			final JTable configList = new JTable(new ConfigTableModel());
			configList.getColumnModel().getColumn(2).setMinWidth(100);
			configList.getColumnModel().getColumn(2).setMaxWidth(100);
			configList.setShowHorizontalLines(true);
			configList.setShowVerticalLines(true);
			configList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			if (this.configs.length != 0) 
				configList.setRowSelectionInterval(0, 0);
			configList.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() > 1) {
						int index = configList.getSelectedRow();
						if (index != -1) {
							SelectConfigurationDialog.this.selectedConfig = SelectConfigurationDialog.this.configs[index];
							dispose();
						}
					}
				}
			});
			configList.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent ke) {
					if (ke.getKeyChar() != KeyEvent.VK_ENTER)
						return;
					int index = configList.getSelectedRow();
					if (index != -1) {
						SelectConfigurationDialog.this.selectedConfig = SelectConfigurationDialog.this.configs[index];
						dispose();
					}
					ke.consume();
				}
				public void keyReleased(KeyEvent ke) {
					if (ke.getKeyChar() == KeyEvent.VK_ENTER)
						ke.consume();
				}
				public void keyTyped(KeyEvent ke) {
					if (ke.getKeyChar() == KeyEvent.VK_ENTER)
						ke.consume();
				}
			});
			configList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent lse) {
					int index = configList.getSelectedRow();
					makeMasterConfigButton.setEnabled(!LOCAL_MASTER_CONFIG_NAME.equals(SelectConfigurationDialog.this.configs[index].name));
				}
			});
			
			JScrollPane configListBox = new JScrollPane(configList);
			configListBox.setViewportBorder(BorderFactory.createLoweredBevelBorder());
			this.getContentPane().add(configListBox, BorderLayout.CENTER);
			
			JButton okButton = new JButton("OK");
			okButton.setBorder(BorderFactory.createRaisedBevelBorder());
			okButton.setPreferredSize(new Dimension(100, 21));
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					int index = configList.getSelectedRow();
					if (index != -1)
						SelectConfigurationDialog.this.selectedConfig = SelectConfigurationDialog.this.configs[index];
					dispose();
				}
			});
			JButton cancelButton = new JButton("Cancel");
			cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
			cancelButton.setPreferredSize(new Dimension(100, 21));
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					SelectConfigurationDialog.this.selectedConfig = null;
					dispose();
				}
			});
			JButton configButton = new JButton("Configure");
			configButton.setBorder(BorderFactory.createRaisedBevelBorder());
			configButton.setPreferredSize(new Dimension(100, 21));
			configButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					configure();
					SelectConfigurationDialog.this.selectedConfig = null;
				}
			});
			this.makeMasterConfigButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.makeMasterConfigButton.setPreferredSize(new Dimension(100, 21));
			this.makeMasterConfigButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					int index = configList.getSelectedRow();
					if (index != -1) {
						
						//	copy selected configuration
						int masterConfigIndex = makeMasterConfig(SelectConfigurationDialog.this.configs[index]);
						
						//	select updated master configuration afterward
						if (masterConfigIndex != -1) {
							configList.setRowSelectionInterval(masterConfigIndex, masterConfigIndex);
							configList.validate();
							configList.repaint();
						}
					}
				}
			});
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);
			buttonPanel.add(configButton);
			this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
			
			//	get feedback
			this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			this.setSize(500, Math.min(Math.max(200, (100 + (configs.length * 25))), 600));
			this.setLocationRelativeTo(null);
		}
		
		private class ConfigTableModel implements TableModel {
			ConfigTableModel() {}
			public int getColumnCount() {
				return 3;
			}
			public int getRowCount() {
				return configs.length;
			}
			public String getColumnName(int columnIndex) {
				if (columnIndex == 0) return "Config Name";
				else if (columnIndex == 1) return "Config Host";
				else if (columnIndex == 2) return "Last Modified";
				else return null;
			}
			public Class getColumnClass(int columnIndex) {
				return String.class;
			}
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}
			public Object getValueAt(int rowIndex, int columnIndex) {
				if (columnIndex == 0) return configs[rowIndex].name;
				else if (columnIndex == 1) return ((configs[rowIndex].host == null) ? "Local" : configs[rowIndex].host);
				else if (columnIndex == 2) return timestampFormat.format(new Date(configs[rowIndex].timestamp));
				else return null;
			}
			public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}
			
			public void addTableModelListener(TableModelListener l) {}
			public void removeTableModelListener(TableModelListener l) {}
		}
		
		private void configure() {
			ConfigurationDialog sd = new ConfigurationDialog();
			sd.setLocationRelativeTo(null);
			sd.setVisible(true);
			if (sd.committed) {
				boolean parametersDirty = false;
				if (!sd.startMemory.getText().equals(PARAMETERS.getSetting(START_MEMORY_NAME))) {
					try {
						int startMemory = Integer.parseInt(sd.startMemory.getText());
						PARAMETERS.setSetting(START_MEMORY_NAME, ("" + startMemory));
						parametersDirty = true;
					} catch (Exception e) {}
				}
				if (!sd.maxMemory.getText().equals(PARAMETERS.getSetting(MAX_MEMORY_NAME))) {
					try {
						int maxMemory = Integer.parseInt(sd.maxMemory.getText());
						PARAMETERS.setSetting(MAX_MEMORY_NAME, ("" + maxMemory));
						parametersDirty = true;
					} catch (Exception e) {}
				}
				if (!sd.wwwProxyName.getText().equals(PARAMETERS.getSetting(PROXY_NAME, ""))) {
					try {
						String proxyName = sd.wwwProxyName.getText().trim();
						if (proxyName.length() == 0) PARAMETERS.removeSetting(PROXY_NAME);
						else PARAMETERS.setSetting(PROXY_NAME, proxyName);
						parametersDirty = true;
					} catch (Exception e) {}
				}
				if (!sd.wwwProxyPort.getText().equals(PARAMETERS.getSetting(PROXY_PORT, ""))) {
					try {
						String proxyPortString = sd.wwwProxyPort.getText().trim();
						if (proxyPortString.length() == 0) PARAMETERS.removeSetting(PROXY_PORT);
						else {
							int proxyPort = Integer.parseInt(proxyPortString);
							PARAMETERS.setSetting(PROXY_PORT, ("" + proxyPort));
						}
						parametersDirty = true;
					} catch (Exception e) {}
				}
				if (!sd.wwwProxyUser.getText().equals(PARAMETERS.getSetting(PROXY_USER, ""))) {
					try {
						String proxyUser = sd.wwwProxyUser.getText().trim();
						if (proxyUser.length() == 0) PARAMETERS.removeSetting(PROXY_USER);
						else PARAMETERS.setSetting(PROXY_USER, proxyUser);
						parametersDirty = true;
					} catch (Exception e) {}
				}
				if (!sd.wwwProxyPwd.getText().equals(PARAMETERS.getSetting(PROXY_PWD, ""))) {
					try {
						String proxyPwd = sd.wwwProxyPwd.getText().trim();
						if (proxyPwd.length() == 0) PARAMETERS.removeSetting(PROXY_PWD);
						else PARAMETERS.setSetting(PROXY_PWD, proxyPwd);
						parametersDirty = true;
					} catch (Exception e) {}
				}
				if (parametersDirty) writeParameterFile();
				
				StringVector updateHostParser = new StringVector();
				updateHostParser.parseAndAddElements(sd.updateHosts.getText(), "\n");
				for (int p = 0; p < updateHostParser.size(); p++)
					updateHostParser.setElementAt(updateHostParser.get(p).trim(), p);
				updateHostParser.removeAll("");
				if ((updateHostParser.intersect(UPDATE_HOSTS, false).size() != UPDATE_HOSTS.size()) || updateHostParser.union(UPDATE_HOSTS, false).size() != UPDATE_HOSTS.size()) {
					UPDATE_HOSTS.clear();
					UPDATE_HOSTS.addContentIgnoreDuplicates(updateHostParser);
					writeUpdateHostFile();
				}
				
				StringVector configHostParser = new StringVector();
				configHostParser.parseAndAddElements(sd.configHosts.getText(), "\n");
				for (int p = 0; p < configHostParser.size(); p++)
					configHostParser.setElementAt(configHostParser.get(p).trim(), p);
				configHostParser.removeAll("");
				if ((configHostParser.intersect(CONFIG_HOSTS, false).size() != CONFIG_HOSTS.size()) || configHostParser.union(CONFIG_HOSTS, false).size() != CONFIG_HOSTS.size()) {
					CONFIG_HOSTS.clear();
					CONFIG_HOSTS.addContentIgnoreDuplicates(configHostParser);
					writeConfigHostFile();
				}
			}
		}
		
		private int makeMasterConfig(ConfigurationDescriptor config) {
			
			//	find current master configuration
			ConfigurationDescriptor masterConfig = null;
			int masterConfigIndex = -1;
			for (int c = 0; c < this.configs.length; c++)
				if (LOCAL_MASTER_CONFIG_NAME.equals(this.configs[c].name)) {
					masterConfig = this.configs[c];
					masterConfigIndex = c;
					break;
				}
			if ((masterConfig == null) || (masterConfig == config))
				return -1;
			
			//	test if local master configuration empty
			File masterConfigPluginPath = new File(this.masterBasePath, PLUGIN_FOLDER_NAME);
			File[] masterConfigPluginJars = new File[0];
			if (masterConfigPluginPath.exists() && masterConfigPluginPath.isDirectory())
				masterConfigPluginJars = masterConfigPluginPath.listFiles(new FileFilter() {
					public boolean accept(File file) {
						return (file.isFile() && file.getName().toLowerCase().endsWith(".jar"));
					}
				});
			
			//	local master configuration already exists, offer merging selected config into it
			if (masterConfigPluginJars.length != 0) {
				int choice = JOptionPane.showConfirmDialog(this, ("It seems as if there is already a " + LOCAL_MASTER_CONFIG_NAME + " in this GoldenGATE Editor installation." +
						"\nIf you choose to proceed, " + config.name + " will be merged into this existing " + LOCAL_MASTER_CONFIG_NAME + "," +
						"\nupdating any files that are more recent in the former. Do you wish to proceed?"), (LOCAL_MASTER_CONFIG_NAME + " Already Exists"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (choice != JOptionPane.OK_OPTION)
					return -1;
			}
			
			//	inform about implications of loca master configuration
			else JOptionPane.showMessageDialog(this, ("Copying " + config.name + " as your " + LOCAL_MASTER_CONFIG_NAME + " in this GoldenGATE Editor installation implies the following:" +
					"\n1. You can freely modify any resources contained in the " + LOCAL_MASTER_CONFIG_NAME + ", and add new ones." +
					"\n2. The resources copied from " + config.name + " configuration are decoupled from automated updates." +
					"\n3. Neither of 1 and 2 affect the resources in the original " + config.name + " configuration," +
					"\n   only their copies aded to your " + LOCAL_MASTER_CONFIG_NAME + "."), (LOCAL_MASTER_CONFIG_NAME + " Restrictions"), JOptionPane.INFORMATION_MESSAGE);
			
			//	copy files
			try {
				this.updateMasterConfig(new File(new File(this.masterBasePath, CONFIG_FOLDER_NAME), config.name), this.masterBasePath);
			}
			catch (IOException ioe) {
				System.out.println("Error updating master configuration: " + ioe.getMessage());
				ioe.printStackTrace(System.out);
				JOptionPane.showMessageDialog(this, ("An error occurred while copying " + config.name + " as your " + LOCAL_MASTER_CONFIG_NAME + ":" +
						"\n:" + ioe.getMessage()), (LOCAL_MASTER_CONFIG_NAME + " Update Error"), JOptionPane.ERROR_MESSAGE);
			}
			
			//	indicate where the updated configuration is
			return masterConfigIndex;
		}
		
		private void updateMasterConfig(File source, File target) throws IOException {
			
			//	create progress monitor
			final ProgressMonitorDialog pmd = new ProgressMonitorDialog(this, ("Updating " + LOCAL_MASTER_CONFIG_NAME + " ..."));
			pmd.setSize(new Dimension(700, 120));
			pmd.popUp(false);
			
			//	create file list
			pmd.setStep("Listing Files to Update");
			HashSet fileList = new HashSet() {
				private int filesListed = 0;
				public boolean add(Object o) {
					if (super.add(o)) {
						this.filesListed++;
						return true;
					}
					else return false;
				}
				private int filesDone = 0;
				public boolean remove(Object o) {
					pmd.setBaseProgress((100 * this.filesDone) / this.filesListed);
					pmd.setBaseProgress((100 * (this.filesDone + 1)) / this.filesListed);
					if (super.remove(o)) {
						this.filesDone++;
						return true;
					}
					else return false;
				}
			};
			try {
				this.listFiles(source, fileList, pmd);
			}
			catch (RuntimeException re) {
				System.out.println("   - Error listing files: " + re.getMessage());
				re.printStackTrace(System.out);
				pmd.close();
				throw new IOException("Could not list files to update - " + re.getMessage());
			}
			
			//	copy files (have to do this recursively again to keep track of folder structure)
			try {
				this.updateFileOrFolder(source, target, fileList, pmd);
			}
			finally {
				pmd.close();
			}
		}
		
		private void listFiles(File file, HashSet fileList, ProgressMonitor pm) {
			
			//	handle folder
			if (file.isDirectory()) {
				pm.setInfo("- listing files in " + file.getAbsolutePath());
				
				//	copy source files (exclude old and new files, and cache folders)
				File[] files = file.listFiles(new FileFilter() {
					public boolean accept(File file) {
						if (file.isFile())
							return (!file.getName().toLowerCase().endsWith(".old") && !file.getName().toLowerCase().endsWith(".new"));
						else if (file.isDirectory())
							return !file.getName().toLowerCase().equals("cache");
						else return false;
					}
				});
				for (int f = 0; f < files.length; f++)
					this.listFiles(files[f], fileList, pm);
			}
			
			//	handle file
			else fileList.add(file);
		}
		
		private void updateFileOrFolder(File source, File target, HashSet sourceList, ProgressMonitor pm) throws IOException {
			
			//	handle folder
			if (source.isDirectory()) {
				pm.setStep("- Updating Files in " + target.getAbsolutePath());
				
				//	make sure target folder exists
				target.mkdirs();
				
				//	copy source files
				File[] sources = source.listFiles(new FileFilter() {
					public boolean accept(File file) {
						if (file.isFile())
							return (!file.getName().toLowerCase().endsWith(".old") && !file.getName().toLowerCase().endsWith(".new"));
						else if (file.isDirectory())
							return !file.getName().toLowerCase().equals("cache");
						else return false;
					}
				});
				for (int s = 0; s < sources.length; s++)
					this.updateFileOrFolder(sources[s], new File(target, sources[s].getName()), sourceList, pm);
			}
			
			//	handle file (if contained in list)
			else if (source.isFile() && sourceList.remove(source)) {
				pm.setStep("- Updating Files in " + target.getParentFile().getAbsolutePath());
				pm.setInfo("- updating file " + source.getAbsolutePath());
				pm.setProgress(0);
				
				//	check timestamps
				if (target.exists() && (source.lastModified() < (target.lastModified() + 1000))) {
					pm.setProgress(100);
					return;
				}
				
				//	remember original file name
				String targetName = target.getAbsolutePath();
				
				//	create update file
				File update = new File(target.getAbsoluteFile().getParent(), (target.getName() + "." + source.lastModified() + ".updating"));
				if (update.exists()) // clean up possibly corrupted file from earlier update
					update.delete();
				else update.getParentFile().mkdirs();
				update.createNewFile();
				OutputStream updateOut = new BufferedOutputStream(new FileOutputStream(update));
				
				//	connect to source
				InputStream sourceIn = new BufferedInputStream(new FileInputStream(source));
				
				//	copy file
				int count;
				int total = 0;
				byte[] data = new byte[1024];
				while ((count = sourceIn.read(data, 0, 1024)) != -1) {
					updateOut.write(data, 0, count);
					total += count;
					pm.setProgress((100 * total) / ((int) source.length()));
				}
				
				//	close streams
				sourceIn.close();
				updateOut.flush();
				updateOut.close();
				
				//	set timestamp of copied file
				try {
					update.setLastModified(source.lastModified());
					System.out.println("   - last modified set to " + source.lastModified());
				}
				catch (RuntimeException re) {
					System.out.println("   - Error setting file timestamp: " + re.getClass().getName() + " (" + re.getMessage() + ")");
					re.printStackTrace(System.out);
				}
				
				//	switch from old file to new one
				if (target.exists())
					target.renameTo(new File(targetName + "." + source.lastModified() + ".old"));
				update.renameTo(new File(targetName));
			}
		}
		
		/**
		 * dialog for edition local configuration like WWW proxy, JVM memory, etc.
		 * 
		 * @author sautter
		 */
		private class ConfigurationDialog extends JDialog {
			
			private JTextField startMemory = new JTextField(PARAMETERS.getSetting(START_MEMORY_NAME, DEFAULT_START_MEMORY));
			private JTextField maxMemory = new JTextField(PARAMETERS.getSetting(MAX_MEMORY_NAME, DEFAULT_START_MEMORY));
			
			private JTextField wwwProxyName = new JTextField(PARAMETERS.getSetting(PROXY_NAME, ""));
			private JTextField wwwProxyPort = new JTextField(PARAMETERS.getSetting(PROXY_PORT, ""));
			private JTextField wwwProxyUser = new JTextField(PARAMETERS.getSetting(PROXY_USER, ""));
			private JTextField wwwProxyPwd = new JTextField(PARAMETERS.getSetting(PROXY_PWD, ""));
			
			private JTextArea updateHosts = new JTextArea(UPDATE_HOSTS.concatStrings("\n"));
			private JScrollPane updateHostBox = new JScrollPane(this.updateHosts);
			
			private JTextArea configHosts = new JTextArea(CONFIG_HOSTS.concatStrings("\n"));
			private JScrollPane configHostBox = new JScrollPane(this.configHosts);
			
			private JButton logFolder = new JButton();
			private JFileChooser logFolderChooser = new JFileChooser();
			
			private boolean committed = false;
			
			ConfigurationDialog() {
				super(SelectConfigurationDialog.this, "Local GoldenGATE Settings", true);
				
				//	initialize main buttons
				JButton commitButton = new JButton("OK");
				commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
				commitButton.setPreferredSize(new Dimension(100, 21));
				commitButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						committed = true;
						dispose();
					}
				});
				
				JButton abortButton = new JButton("Cancel");
				abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
				abortButton.setPreferredSize(new Dimension(100, 21));
				abortButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						dispose();
					}
				});
				
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				buttonPanel.add(commitButton);
				buttonPanel.add(abortButton);
				
				//	 initialize main system settings
				this.startMemory.setBorder(BorderFactory.createLoweredBevelBorder());
				this.startMemory.setPreferredSize(new Dimension(100, 21));
				this.maxMemory.setBorder(BorderFactory.createLoweredBevelBorder());
				this.maxMemory.setPreferredSize(new Dimension(100, 21));
				
				this.updateHostBox.setPreferredSize(new Dimension(450, 60));
				this.configHostBox.setPreferredSize(new Dimension(450, 60));
				
				this.logFolderChooser.setSelectedFile(this.getLogFolder(PARAMETERS.getSetting(LOG_PATH, LOG_FOLDER_NAME)));
				this.logFolderChooser.setAcceptAllFileFilterUsed(false);
				this.logFolderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				this.logFolder.setText(this.getLogFolderName(this.logFolderChooser.getSelectedFile()));
				this.logFolder.setBorder(BorderFactory.createLoweredBevelBorder());
				this.logFolder.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (logFolderChooser.showOpenDialog(ConfigurationDialog.this) == JFileChooser.APPROVE_OPTION) {
							String logFolderName = getLogFolderName(logFolderChooser.getSelectedFile());
							logFolder.setText(logFolderName);
							PARAMETERS.setSetting(LOG_PATH, logFolderName);
						}
					}
				});
				
				//	assemble data input fields
				JPanel dataPanel = new JPanel(new GridBagLayout(), true);
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.weightx = 1;
				gbc.weighty = 0;
				gbc.gridwidth = 1;
				gbc.gridheight = 1;
				gbc.insets.top = 2;
				gbc.insets.bottom = 2;
				gbc.insets.left = 3;
				gbc.insets.right = 3;
				gbc.fill = GridBagConstraints.BOTH;
				
				gbc.gridy = 0;
				gbc.gridx = 0;
				
				gbc.gridwidth = 1;
				dataPanel.add(new JLabel("Initial JVM Memory", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				gbc.gridwidth = 2;
				dataPanel.add(this.startMemory, gbc.clone());
				gbc.gridx+=2;
				gbc.gridwidth = 1;
				dataPanel.add(new JLabel("Maximum JVM Memory", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				gbc.gridwidth = 2;
				dataPanel.add(this.maxMemory, gbc.clone());
				
				gbc.gridy++;
				gbc.gridx = 0;
				gbc.gridwidth = 1;
				dataPanel.add(new JLabel("Proxy Name", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				gbc.gridwidth = 3;
				dataPanel.add(this.wwwProxyName, gbc.clone());
				gbc.gridx += 3;
				gbc.gridwidth = 1;
				dataPanel.add(new JLabel("Proxy Port", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				dataPanel.add(this.wwwProxyPort, gbc.clone());
				
				gbc.gridy++;
				gbc.gridx = 0;
				gbc.gridwidth = 1;
				dataPanel.add(new JLabel("Proxy User", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				gbc.gridwidth = 2;
				dataPanel.add(this.wwwProxyUser, gbc.clone());
				gbc.gridx += 2;
				gbc.gridwidth = 1;
				dataPanel.add(new JLabel("Proxy Password", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				gbc.gridwidth = 2;
				dataPanel.add(this.wwwProxyPwd, gbc.clone());
				
				gbc.gridy++;
				gbc.gridx = 0;
				gbc.gridwidth = 2;
				dataPanel.add(new JLabel("Log Folder (click to change)", JLabel.RIGHT), gbc.clone());
				gbc.gridx += 2;
				gbc.gridwidth = 4;
				dataPanel.add(this.logFolder, gbc.clone());
				
				gbc.gridy++;
				gbc.gridx = 0;
				gbc.gridwidth = 1;
				gbc.weighty = 1;
				dataPanel.add(new JLabel("Update Hosts", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				gbc.gridwidth = 5;
				dataPanel.add(this.updateHostBox, gbc.clone());
				
				gbc.gridy++;
				gbc.gridx = 0;
				gbc.gridwidth = 1;
				dataPanel.add(new JLabel("Config Hosts", JLabel.RIGHT), gbc.clone());
				gbc.gridx++;
				gbc.gridwidth = 5;
				dataPanel.add(this.configHostBox, gbc.clone());
				
				//	put the whole stuff together
				this.getContentPane().setLayout(new BorderLayout());
				this.getContentPane().add(dataPanel, BorderLayout.CENTER);
				this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
				
				//	set dialog size
				this.setSize(new Dimension(500, 250));
				this.setResizable(true);
			}
			File getLogFolder(String logFolderName) {
				File logFolder = ((logFolderName.startsWith("/") || (logFolderName.indexOf(':') != -1)) ? new File(logFolderName) : new File(BASE_PATH, logFolderName));
				return logFolder.getAbsoluteFile();
			}
			String getLogFolderName(File logFolder) {
				String logFolderName = logFolder.getAbsolutePath();
				logFolderName = logFolderName.replace('\\', '/');
				logFolderName = logFolderName.replaceAll("\\/(\\.?\\/)*", "/");
				while (logFolderName.startsWith("./"))
					logFolderName = logFolderName.substring("./".length());
				String basePathName = BASE_PATH.getAbsolutePath();
				basePathName = basePathName.replace('\\', '/');
				basePathName = basePathName.replaceAll("\\/(\\.?\\/)*", "/");
				while (basePathName.startsWith("./"))
					basePathName = basePathName.substring("./".length());
				return (logFolderName.startsWith(basePathName) ? logFolderName.substring(basePathName.length()) : logFolderName);
			}
		}
	}
}