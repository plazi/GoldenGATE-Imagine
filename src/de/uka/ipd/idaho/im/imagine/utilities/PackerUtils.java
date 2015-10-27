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
 *     * Neither the name of the Universität Karlsruhe (TH) nor the
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
package de.uka.ipd.idaho.im.imagine.utilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;


/**
 * @author sautter
 *
 */
public class PackerUtils {
	
	private static final SimpleDateFormat readmeTimestamper = new SimpleDateFormat("yyyy.MM.dd.HH.mm");
	
	private static final Color uneditableReadmeColor = new Color(224, 224, 224);
	
	private static final String buildLabelString(List lines) {
		StringBuffer labelString = new StringBuffer();
		for (int l = 0; l < lines.size(); l++) {
			String line = lines.get(l).toString();
			StringBuffer lineBuilder = new StringBuffer();
			int i = 0;
			while (line.startsWith(" ", i)) {
				lineBuilder.append("&nbsp;");
				i++;
			}
			labelString.append(((i == 0) ? line : (lineBuilder.toString() + line.substring(i))) + (((l+1) == lines.size()) ? "" : "<BR>"));
		}
		return labelString.toString();
	}
	
	public static final void updateReadmeFile(File rootFolder, final String configName, Component parent, long timestamp, int tolerance) throws IOException {
		
		//	check timestamp
		File readmeFile = new File(rootFolder, README_FILE_NAME);
		if ((timestamp - tolerance) < readmeFile.lastModified())
			return;
		
		//	read existing file
		final ArrayList readme = new ArrayList();
		BufferedReader br = new BufferedReader(new FileReader(readmeFile));
		String line;
		while ((line = br.readLine()) != null)
			readme.add(line);
		br.close();
		
		//	create configuration info lines
		final String[] entryHeader = {
				("NEW IN VERSION " + readmeTimestamper.format(new Date(timestamp))),
				"",
			};
		
		//	parse existing readme
		final ArrayList top = new ArrayList();
		final ArrayList bottom = new ArrayList();
		
		//	read up to change log header
		int l = 0;
		while ((l < readme.size()) && !"CHANGE LOG".equals(readme.get(l)))
			top.add(readme.get(l++));
		
		//	no change log header so far
		if (l == readme.size()) {
			
			//	remove tailing empty lines
			while ((top.size() != 0) && "".equals(top.get(top.size() - 1)))
				top.remove(top.size() - 1);
			
			//	add change log header
			top.add("");
			top.add("");
			top.add("CHANGE LOG");
			top.add("");
			top.add("Changes to " + ((configName == null) ? "GoldenGATE Imagine" : "this configuration") + " in reverse chronological order");
			
			//	add new entry header
			top.add("");
			top.add("");
			top.addAll(Arrays.asList(entryHeader));
		}
		
		//	add new entry below change log header
		else {
			
			//	read up to first entry
			while ((l < readme.size()) && !readme.get(l).toString().startsWith("NEW IN VERSION"))
				top.add(readme.get(l++));
			
			//	remove tailing empty lines
			while ((top.size() != 0) && "".equals(top.get(top.size() - 1)))
				top.remove(top.size() - 1);
			
			//	add new entry header
			top.add("");
			top.add("");
			top.addAll(Arrays.asList(entryHeader));
			
			//	store older entries
			bottom.add("");
			bottom.add("");
			while (l < readme.size())
				bottom.add(readme.get(l++));
		}
		
		//	clean up
		readme.clear();
		
		//	create dialog
		final JDialog readmeDialog = new JDialog(((JFrame) null), ("Create Readme File Entry" + ((configName == null) ? "" : (" for Configuration '" + configName + "'"))), true);
		
		JLabel label = new JLabel(("<HTML>Please create a new entry for the readme file of " + ((configName == null) ? "GoldenGATE Imagine" : ("configuration '" + configName + "'")) + ".</HTML>"), JLabel.LEFT);
		label.setFont(new Font(label.getFont().getFamily(), Font.BOLD, 12));
		label.setBorder(BorderFactory.createLineBorder(label.getBackground(), 5));
		
		final JPanel readmePanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets.top = 0;
		gbc.insets.bottom = 0;
		gbc.insets.left = 5;
		gbc.insets.right = 5;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		
		Font readmeFont = new Font("Verdana", Font.PLAIN, 12);
		
		
		JLabel topLabel = new JLabel(("<HTML>" + buildLabelString(top) + "<BR></HTML>"), JLabel.LEFT);
		topLabel.setOpaque(true);
		topLabel.setBackground(uneditableReadmeColor);
		topLabel.setFont(readmeFont);
		
		readmePanel.add(topLabel, gbc.clone());
		gbc.gridy++;
		
		
		final JTextArea entryField = new JTextArea("<Enter New Entry>");
		entryField.setFont(readmeFont);
		entryField.setBorder(BorderFactory.createLineBorder(uneditableReadmeColor));
		
		readmePanel.add(entryField, gbc.clone());
		gbc.gridy++;
		
		if (bottom.size() != 0) {
			JLabel bottomLabel = new JLabel(("<HTML>" + buildLabelString(bottom) + "</HTML>"), JLabel.LEFT);
			bottomLabel.setOpaque(true);
			bottomLabel.setBackground(uneditableReadmeColor);
			bottomLabel.setFont(readmeFont);
			
			readmePanel.add(bottomLabel, gbc.clone());
			gbc.gridy++;
		}
		
		
		final JScrollPane readmePanelBox = new JScrollPane(readmePanel);
		readmePanelBox.getVerticalScrollBar().setUnitIncrement(50);
		readmePanelBox.getVerticalScrollBar().setBlockIncrement(100);
		
		
		JButton okButton = new JButton("OK");
		okButton.setBorder(BorderFactory.createRaisedBevelBorder());
		okButton.setPreferredSize(new Dimension(70, 21));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String entry = entryField.getText().trim();
				if ((entry.length() == 0) || entry.startsWith("<")) {
					JOptionPane.showMessageDialog(readmeDialog, ("Please enter a meaningful entry text for configuration '" + configName + "'."), "Description Missing", JOptionPane.ERROR_MESSAGE);
					entryField.requestFocusInWindow();
					return;
				}
				
				readme.addAll(top);
				readme.addAll(Arrays.asList(entry.split("((\\n\\r)|(\\r\\n)|\\n|\\r)")));
				readme.addAll(bottom);
				
				readmeDialog.dispose();
			}
		});
		
		JButton skipButton = new JButton("Skip");
		skipButton.setBorder(BorderFactory.createRaisedBevelBorder());
		skipButton.setPreferredSize(new Dimension(70, 21));
		skipButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				readmeDialog.dispose();
			}
		});
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(okButton);
		buttonPanel.add(skipButton);
		
		
		readmeDialog.setLayout(new BorderLayout());
		readmeDialog.add(label, BorderLayout.NORTH);
		readmeDialog.add(readmePanelBox, BorderLayout.CENTER);
		readmeDialog.add(buttonPanel, BorderLayout.SOUTH);
		
		readmeDialog.setSize(700, 600);
		readmeDialog.setLocationRelativeTo(parent);
		readmeDialog.setResizable(true);
		
		readmeDialog.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent we) {
				readmePanelBox.getViewport().scrollRectToVisible(entryField.getBounds());
			}
		});
		entryField.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent de) {}
			public void insertUpdate(DocumentEvent de) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						Rectangle r = entryField.getBounds();
						if (r.height > readmePanelBox.getVisibleRect().height)
							return;
						Point p = r.getLocation();
						p = SwingUtilities.convertPoint(entryField.getParent(), p, readmePanelBox.getViewport());
						r = new Rectangle(p.x, p.y, r.width, r.height);
						readmePanelBox.getViewport().scrollRectToVisible(r);
					}
				});
			}
			public void removeUpdate(DocumentEvent de) {}
		});
		
		readmeDialog.setVisible(true);
		
		
		//	nothing changed
		if (readme.isEmpty())
			return;
		
		//	write update
		BufferedWriter bw = new BufferedWriter(new FileWriter(readmeFile, false));
		for (l = 0; l < readme.size(); l++) {
			bw.write(readme.get(l).toString());
			bw.newLine();
		}
		bw.flush();
		bw.close();
	}
	
	
	public static final String LOCAL_MASTER_CONFIGURATION = "Local Master Configuration";
	
	public static final String README_FILE_NAME = "README.txt";
	
	public static final String[] getConfigurationNames(File rootFolder) {
		List configNameList = new LinkedList();
		configNameList.add(LOCAL_MASTER_CONFIGURATION);
		
		File configFolder = new File(rootFolder, "Configurations");
		File[] configFolders = configFolder.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return (file.isDirectory() && !file.getName().endsWith(".old"));
			}
		});
		if (configFolders != null) {
			String[] configFolderNames = new String[configFolders.length];
			for (int c = 0; c < configFolders.length; c++)
				configFolderNames[c] = configFolders[c].getName();
			Arrays.sort(configFolderNames);
			configNameList.addAll(Arrays.asList(configFolderNames));
		}
		
		return ((String[]) configNameList.toArray(new String[configNameList.size()]));
	}
	
	public static final String selectConfigurationName(File rootFolder, String message, boolean allowNone, String preSelected) {
		List configNameList = new LinkedList();
		if (allowNone)
			configNameList.add("<No Configuration>");
		configNameList.add(LOCAL_MASTER_CONFIGURATION);
		
		File configFolder = new File(rootFolder, "Configurations");
		File[] configFolders = configFolder.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return (file.isDirectory() && !file.getName().endsWith(".old"));
			}
		});
		if (configFolders != null) {
			String[] configFolderNames = new String[configFolders.length];
			for (int c = 0; c < configFolders.length; c++)
				configFolderNames[c] = configFolders[c].getName();
			Arrays.sort(configFolderNames);
			configNameList.addAll(Arrays.asList(configFolderNames));
		}
		
		String[] configNames = ((String[]) configNameList.toArray(new String[configNameList.size()]));
		Object configName = JOptionPane.showInputDialog(null, message, "Select Configuration", JOptionPane.QUESTION_MESSAGE, null, configNames, ((preSelected == null) ? (allowNone ? "<No Configuration>" : null) : preSelected));
		if ("<No Configuration>".equals(configName))
			configName = null;
		return ((String) configName);
	}
	
	public static final String[] selectConfigurationNames(File rootFolder, String message, boolean allowNone, Set preSelected) {
		List configNameList = new LinkedList();
		if (allowNone)
			configNameList.add("<No Configuration>");
		configNameList.add(LOCAL_MASTER_CONFIGURATION);
		configNameList.addAll(Arrays.asList(getConfigNames(rootFolder)));
		
		String[] configNames = ((String[]) configNameList.toArray(new String[configNameList.size()]));
		configNames = selectStrings(configNames, preSelected, preSelected, message, null);
		for (int c = 0; c < configNames.length; c++)
			if ("<No Configuration>".equals(configNames[c]))
				configNames[c] = null;
		return configNames;
	}
	
	public static final String[] getConfigNames(File rootFolder) {
		List configNameList = new LinkedList();
		File configFolder = new File(rootFolder, "Configurations");
		File[] configFolders = configFolder.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return (file.isDirectory() && !file.getName().endsWith(".old"));
			}
		});
		if (configFolders != null) {
			String[] configFolderNames = new String[configFolders.length];
			for (int c = 0; c < configFolders.length; c++)
				configFolderNames[c] = configFolders[c].getName();
			Arrays.sort(configFolderNames);
			configNameList.addAll(Arrays.asList(configFolderNames));
		}
		return ((String[]) configNameList.toArray(new String[configNameList.size()]));
	}
	
	public static final File getConfigFolder(File rootFolder, String configName) {
		if (LOCAL_MASTER_CONFIGURATION.equals(configName))
			return rootFolder;
		return new File(rootFolder, ("Configurations/" + configName));
	}
	
	public static final String[] getConfigFileNames(File rootFolder, String configName) {
		File configFolder = getConfigFolder(rootFolder, configName);
		
		String[] configFileNames = listFilesRelative(configFolder, 10);
		if (configFolder == rootFolder) {
			Set filteredConfigFileNames = new TreeSet();
			for (int f = 0; f < configFileNames.length; f++) {
				if ("GoldenGATE.cnfg".equals(configFileNames[f]))
					filteredConfigFileNames.add(configFileNames[f]);
				if ("GgImagine.cnfg".equals(configFileNames[f]))
					filteredConfigFileNames.add(configFileNames[f]);
				else if (configFileNames[f].startsWith("Plugins/"))
					filteredConfigFileNames.add(configFileNames[f]);
				else if (configFileNames[f].startsWith("Data/"))
					filteredConfigFileNames.add(configFileNames[f]);
				else if (configFileNames[f].startsWith("Documentation/"))
					filteredConfigFileNames.add(configFileNames[f]);
				else if (configFileNames[f].startsWith("CustomFunctions/"))
					filteredConfigFileNames.add(configFileNames[f]);
				else if (configFileNames[f].startsWith("CustomShortcuts/"))
					filteredConfigFileNames.add(configFileNames[f]);
			}
			return ((String[]) filteredConfigFileNames.toArray(new String[filteredConfigFileNames.size()]));
		}
		else return configFileNames;
	}
	
	public static final String[] selectStrings(final String[] strings, Set preSelectedStrings, final Set highlightStrings, String title, Component parent) {
		final Set selectedStrings = new TreeSet();
		if (preSelectedStrings != null)
			selectedStrings.addAll(preSelectedStrings);
		
		final JDialog d = new JDialog(((JFrame) null), title, true);
		
		JTable t = new JTable(new TableModel() {
			public Class getColumnClass(int columnIndex) {
				return ((columnIndex == 0) ? Boolean.class : String.class);
			}
			public int getColumnCount() {
				return 2;
			}
			public String getColumnName(int columnIndex) {
				return ((columnIndex == 0) ? "Update" : "Jar Name");
			}
			public int getRowCount() {
				return strings.length;
			}
			
			public Object getValueAt(int rowIndex, int columnIndex) {
				String string = strings[rowIndex];
				String displayString = string;
				displayString = displayString.replaceAll("\\<", "\\&lt\\;");
				displayString = displayString.replaceAll("\\>", "\\&gt\\;");
				if (columnIndex == 0)
					return new Boolean(selectedStrings.contains(string));
				else if (highlightStrings.contains(string))
					return ("<HTML><B>" + displayString + "</B></HTML>");
				else return ("<HTML>" + displayString + "</HTML>");
			}
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return (columnIndex == 0);
			}
			public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
				String string = strings[rowIndex];
				Boolean selected = ((Boolean) newValue);
				if (selected.booleanValue())
					selectedStrings.add(string);
				else selectedStrings.remove(string);
			}
			
			public void addTableModelListener(TableModelListener tml) {}
			public void removeTableModelListener(TableModelListener tml) {}
			
		});
		t.getColumnModel().getColumn(0).setMaxWidth(60);
		
		JScrollPane sp = new JScrollPane(t);
		sp.getVerticalScrollBar().setUnitIncrement(50);
		sp.getVerticalScrollBar().setBlockIncrement(100);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		JButton ok = new JButton("OK");
		ok.setBorder(BorderFactory.createRaisedBevelBorder());
		ok.setPreferredSize(new Dimension(70, 21));
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				d.dispose();
			}
		});
		
		JButton cancel = new JButton("Cancel");
		cancel.setBorder(BorderFactory.createRaisedBevelBorder());
		cancel.setPreferredSize(new Dimension(70, 21));
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				selectedStrings.clear();
				d.dispose();
			}
		});
		
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttons.add(ok);
		buttons.add(cancel);
		
		d.getContentPane().setLayout(new BorderLayout());
		d.getContentPane().add(sp, BorderLayout.CENTER);
		d.getContentPane().add(buttons, BorderLayout.SOUTH);
		
		d.setSize(500, 600);
		d.setLocationRelativeTo(parent);
		d.setResizable(true);
		d.setVisible(true);
		
		return ((String[]) selectedStrings.toArray(new String[selectedStrings.size()]));
	}
	
	public static final void writeZipFileEntries(File rootFolder, ZipOutputStream zipper, String[] fileNames) throws IOException {
		for (int f = 0; f < fileNames.length; f++) {
			File sourceFile = new File(rootFolder, fileNames[f]);
			writeZipFileEntry(sourceFile, fileNames[f], zipper);
		}
	}
	
	public static final void writeZipFileEntry(File sourceFile, String fileName, ZipOutputStream zipper) throws IOException {
		FileInputStream source = new FileInputStream(sourceFile);
		writeZipFileEntry(source, fileName, sourceFile.lastModified(), zipper);
		source.close();
	}
	
	public static final void writeZipFileEntry(InputStream source, String fileName, ZipOutputStream zipper) throws IOException {
		writeZipFileEntry(source, fileName, System.currentTimeMillis(), zipper);
	}
	
	public static final void writeZipFileEntry(InputStream source, String fileName, long lastModified, ZipOutputStream zipper) throws IOException {
		ZipEntry zipEntry = new ZipEntry(fileName);
		zipper.putNextEntry(zipEntry);
		
		byte[] buffer = new byte[1024];
		int read;
		while ((read = source.read(buffer)) != -1)
			zipper.write(buffer, 0, read);
		
		zipEntry.setTime(lastModified);
		zipper.closeEntry();
		
		System.out.println(" - '" + fileName + "' added to zip file.");
	}
	
	public static final String normalizePath(String path) {
		path = path.replaceAll("\\\\", "/");
		while (path.startsWith("./"))
			path = path.substring(2);
		while (path.startsWith("/"))
			path = path.substring(1);
		while (path.endsWith("/."))
			path = path.substring(0, (path.length() - 2));
		int pathLength;
		do {
			pathLength = path.length();
			path = path.replaceAll("\\/\\.\\/", "/");
		} while (path.length() < pathLength);
		return path;
	}
	
	public static final String[] listFilesAbsolute(File rootFolder, int depth) {
		
		File[] files = rootFolder.listFiles();
		if (files == null) return new String[0];
		
		Set fileList = new TreeSet();
		for (int f = 0; f < files.length; f++) {
			if (files[f].isDirectory()) {
				if (!files[f].equals(rootFolder) && !files[f].getName().startsWith("_") && !files[f].getName().startsWith("Configurations") && !files[f].getName().toLowerCase().equals("cache") && !files[f].getName().endsWith(".old"))
					if (depth > 0)
						fileList.addAll(Arrays.asList(listFilesAbsolute(files[f], (depth-1))));
			}
			else if (!files[f].getName().endsWith(".old") && !files[f].getName().endsWith(".log") && !files[f].getName().startsWith("_"))
				fileList.add(normalizePath(files[f].getAbsolutePath()));
		}
		
		return ((String[]) fileList.toArray(new String[fileList.size()]));
	}
	
	public static final String[] listFilesRelative(File rootFolder, int depth) {
		String rootFolderPrefix = normalizePath(rootFolder.getAbsolutePath());
		if (!rootFolderPrefix.endsWith("/")) rootFolderPrefix += "/";
		int rootFolderPrefixLength = rootFolderPrefix.length();
		
		String[] fileList = listFilesAbsolute(rootFolder, depth);
		
		for (int l = 0; l < fileList.length; l++)
			fileList[l] = fileList[l].substring(rootFolderPrefixLength);
		
		return fileList;
	}
}