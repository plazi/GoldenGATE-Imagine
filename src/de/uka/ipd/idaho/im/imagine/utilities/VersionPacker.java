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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * @author sautter
 *
 */
public class VersionPacker {

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		buildVersions();
	}
	
	private static final String[] specialFiles = {
		"GgImagine.cnfg",
		"Parameters.cnfg",
		"UpdateHosts.cnfg",
		"ConfigHosts.cnfg",
		PackerUtils.README_FILE_NAME
	};
	
	private static void buildVersions() {
		File rootFolder = new File(PackerUtils.normalizePath(new File(".").getAbsolutePath()));
		System.out.println("Root folder is '" + rootFolder.getAbsolutePath() + "'");
		
		String[] configNames = PackerUtils.getConfigNames(rootFolder);
		Set preSelectedConfigNames = new TreeSet();
		for (int c = 0; c < configNames.length; c++) {
			File versionZipFile = new File(rootFolder, ("_Zips/" + getVersionZipName(configNames[c])));
			File configFolder = PackerUtils.getConfigFolder(rootFolder, configNames[c]);
			if (versionZipFile.lastModified() < configFolder.lastModified())
				preSelectedConfigNames.add(configNames[c]);
		}
		configNames = PackerUtils.selectConfigurationNames(rootFolder, "Please select the configuration(s) to export into version zip files.", true, preSelectedConfigNames);
		
		try {
			for (int c = 0; c < configNames.length; c++)
				if ((configNames[c] == null) || PackerUtils.LOCAL_MASTER_CONFIGURATION.equals(configNames[c])) {
					PackerUtils.updateReadmeFile(rootFolder, null, null, System.currentTimeMillis(), (5 * 60 * 1000));
					c = configNames.length;
				}
		}
		catch (Exception e) {
			System.out.println("An error occurred while updating " + PackerUtils.README_FILE_NAME + ":\n" + e.getMessage());
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(null, ("An error occurred while updating README.txt:\n" + e.getMessage()), "README Update Error", JOptionPane.ERROR_MESSAGE);
		}
		
		for (int c = 0; c < configNames.length; c++) try {
			buildVersion(rootFolder, configNames[c]);
		}
		catch (Exception e) {
			System.out.println("An error occurred creating the version zip file:\n" + e.getMessage());
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(null, ("An error occurred creating the version zip file:\n" + e.getMessage()), "Version Creation Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private static void buildVersion(File rootFolder, String configName) throws Exception {
		System.out.println("Building GoldenGATE Imagine version with " + ((configName == null) ? "no configuration" : (PackerUtils.LOCAL_MASTER_CONFIGURATION.equals(configName) ? configName : ("configuration '" + configName + "'"))) + ".");
		
		String versionZipName = getVersionZipName(configName);
		System.out.println("Building GoldenGATE Imagine version '" + versionZipName + "'");
		
		String[] coreFileNames = getCoreFileNames(rootFolder);
		String[] configFileNames;
		if (configName == null)
			configFileNames = new String[0];
		else {
			configFileNames = PackerUtils.getConfigFileNames(rootFolder, configName);
			if (PackerUtils.LOCAL_MASTER_CONFIGURATION.equals(configName)) {
				Set coreFiles = new TreeSet(Arrays.asList(coreFileNames));
				Set configFiles = new TreeSet(Arrays.asList(configFileNames));
				configFiles.removeAll(coreFiles);
				configFileNames = ((String[]) configFiles.toArray(new String[configFiles.size()]));
			}
			else for (int f = 0; f < configFileNames.length; f++)
				configFileNames[f] = ("Configurations/" + configName + "/" + configFileNames[f]);
		}
		
		
		File versionZipFile = new File(rootFolder, ("_Zips/" + versionZipName));
		if (versionZipFile.exists()) {
			versionZipFile.renameTo(new File(rootFolder, ("_Zips/" + versionZipName + "." + System.currentTimeMillis() + ".old")));
			versionZipFile = new File(rootFolder, ("_Zips/" + versionZipName));
		}
		System.out.println("Creating version zip file '" + versionZipFile.getAbsolutePath() + "'");
		
		versionZipFile.getParentFile().mkdirs();
		versionZipFile.createNewFile();
		ZipOutputStream versionZipper = new ZipOutputStream(new FileOutputStream(versionZipFile));
		
		for (int s = 0; s < specialFiles.length; s++) {
			File specialFile = new File(rootFolder, ("_VersionPacker.imagine." + specialFiles[s]));
			PackerUtils.writeZipFileEntry(specialFile, specialFiles[s], versionZipper);
		}
		
		PackerUtils.writeZipFileEntries(rootFolder, versionZipper, coreFileNames);
		PackerUtils.writeZipFileEntries(rootFolder, versionZipper, configFileNames);
		
		versionZipper.flush();
		versionZipper.close();
		
		System.out.println("Version zip file '" + versionZipFile.getAbsolutePath() + "' created successfully.");
		JOptionPane.showMessageDialog(null, ("Version '" + versionZipName + "' created successfully."), "Version Created Successfully", JOptionPane.INFORMATION_MESSAGE);
	}
	
	private static final String getVersionZipName(String configName) {
		return ("GgImagine" + ((configName == null) ? "" : (PackerUtils.LOCAL_MASTER_CONFIGURATION.equals(configName) ? "-Full" : ("-" + configName))) + ".zip");
	}
	
	private static String[] getCoreFileNames(File rootFolder) throws IOException {
		Set coreFiles = new TreeSet();
		
		File coreFileList = new File(rootFolder, "_VersionPacker.imagine.cnfg");
		BufferedReader br = new BufferedReader(new FileReader(coreFileList));
		String coreFile;
		while ((coreFile = br.readLine()) != null) {
			coreFile = coreFile.trim();
			if ((coreFile.length() != 0) && !coreFile.startsWith("//"))
				coreFiles.add(coreFile);
		}
		br.close();
		
		for (int s = 0; s < specialFiles.length; s++)
			coreFiles.remove(specialFiles[s]);
		
		return ((String[]) coreFiles.toArray(new String[coreFiles.size()]));
	}
//	public static void main(String[] args) {
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		} catch (Exception e) {}
//		
//		buildVersions();
//	}
//	
//	private static final String[] specialFiles = {
//		"Parameters.cnfg",
//		"UpdateHosts.cnfg",
//		"ConfigHosts.cnfg",
//		PackerUtils.README_FILE_NAME
//	};
//	
//	private static void buildVersions() {
//		File rootFolder = new File(PackerUtils.normalizePath(new File(".").getAbsolutePath()));
//		System.out.println("Root folder is '" + rootFolder.getAbsolutePath() + "'");
//		
//		String[] configNames = PackerUtils.getConfigNames(rootFolder);
//		Set preSelectedConfigNames = new TreeSet();
//		for (int c = 0; c < configNames.length; c++) {
//			File versionZipFile = new File(rootFolder, ("_Zips/" + getVersionZipName(configNames[c])));
//			File configFolder = PackerUtils.getConfigFolder(rootFolder, configNames[c]);
//			if (versionZipFile.lastModified() < configFolder.lastModified())
//				preSelectedConfigNames.add(configNames[c]);
//		}
//		configNames = PackerUtils.selectConfigurationNames(rootFolder, "Please select the configuration(s) to export into version zip files.", true, preSelectedConfigNames);
//		
//		try {
//			for (int c = 0; c < configNames.length; c++)
//				if ((configNames[c] == null) || PackerUtils.LOCAL_MASTER_CONFIGURATION.equals(configNames[c])) {
//					PackerUtils.updateReadmeFile(rootFolder, null, null, System.currentTimeMillis(), (5 * 60 * 1000));
//					c = configNames.length;
//				}
//		}
//		catch (Exception e) {
//			System.out.println("An error occurred while updating " + PackerUtils.README_FILE_NAME + ":\n" + e.getMessage());
//			e.printStackTrace(System.out);
//			JOptionPane.showMessageDialog(null, ("An error occurred while updating README.txt:\n" + e.getMessage()), "README Update Error", JOptionPane.ERROR_MESSAGE);
//		}
//		
//		for (int c = 0; c < configNames.length; c++) try {
//			buildVersion(rootFolder, configNames[c]);
//		}
//		catch (Exception e) {
//			System.out.println("An error occurred creating the version zip file:\n" + e.getMessage());
//			e.printStackTrace(System.out);
//			JOptionPane.showMessageDialog(null, ("An error occurred creating the version zip file:\n" + e.getMessage()), "Version Creation Error", JOptionPane.ERROR_MESSAGE);
//		}
//	}
//	
//	private static void buildVersion(File rootFolder, String configName) throws Exception {
//		System.out.println("Building GoldenGATE Imagine version with " + ((configName == null) ? "no configuration" : (PackerUtils.LOCAL_MASTER_CONFIGURATION.equals(configName) ? configName : ("configuration '" + configName + "'"))) + ".");
//		
//		String versionZipName = getVersionZipName(configName);
//		System.out.println("Building GoldenGATE Imagine version '" + versionZipName + "'");
//		
//		String[] coreFileNames = getCoreFileNames(rootFolder);
//		String[] configFileNames;
//		if (configName == null)
//			configFileNames = new String[0];
//		else {
//			configFileNames = PackerUtils.getConfigFileNames(rootFolder, configName);
//			if (PackerUtils.LOCAL_MASTER_CONFIGURATION.equals(configName)) {
//				Set coreFiles = new TreeSet(Arrays.asList(coreFileNames));
//				Set configFiles = new TreeSet(Arrays.asList(configFileNames));
//				configFiles.removeAll(coreFiles);
//				configFileNames = ((String[]) configFiles.toArray(new String[configFiles.size()]));
//			}
//			else for (int f = 0; f < configFileNames.length; f++)
//				configFileNames[f] = ("Configurations/" + configName + "/" + configFileNames[f]);
//		}
//		
//		
//		File versionZipFile = new File(rootFolder, ("_Zips/" + versionZipName));
//		if (versionZipFile.exists()) {
//			versionZipFile.renameTo(new File(rootFolder, ("_Zips/" + versionZipName + "." + System.currentTimeMillis() + ".old")));
//			versionZipFile = new File(rootFolder, ("_Zips/" + versionZipName));
//		}
//		System.out.println("Creating version zip file '" + versionZipFile.getAbsolutePath() + "'");
//		
//		versionZipFile.getParentFile().mkdirs();
//		versionZipFile.createNewFile();
//		ZipOutputStream versionZipper = new ZipOutputStream(new FileOutputStream(versionZipFile));
//		
//		for (int s = 0; s < specialFiles.length; s++) {
//			File specialFile;
//			if (PackerUtils.README_FILE_NAME.equals(specialFiles[s]))
//				specialFile = new File(rootFolder, PackerUtils.README_FILE_NAME);
//			else specialFile = new File(rootFolder, ("_VersionPacker." + specialFiles[s]));
//			PackerUtils.writeZipFileEntry(specialFile, specialFiles[s], versionZipper);
//		}
//		
//		PackerUtils.writeZipFileEntries(rootFolder, versionZipper, coreFileNames);
//		PackerUtils.writeZipFileEntries(rootFolder, versionZipper, configFileNames);
//		
//		versionZipper.flush();
//		versionZipper.close();
//		
//		System.out.println("Version zip file '" + versionZipFile.getAbsolutePath() + "' created successfully.");
//		JOptionPane.showMessageDialog(null, ("Version '" + versionZipName + "' created successfully."), "Version Created Successfully", JOptionPane.INFORMATION_MESSAGE);
//	}
//	
//	private static final String getVersionZipName(String configName) {
//		return ("GoldenGATE" + ((configName == null) ? "" : (PackerUtils.LOCAL_MASTER_CONFIGURATION.equals(configName) ? "-Full" : ("-" + configName))) + ".zip");
//	}
//	
//	private static String[] getCoreFileNames(File rootFolder) throws IOException {
//		Set coreFiles = new TreeSet();
//		
//		File coreFileList = new File(rootFolder, "_VersionPacker.cnfg");
//		BufferedReader br = new BufferedReader(new FileReader(coreFileList));
//		String coreFile;
//		while ((coreFile = br.readLine()) != null) {
//			coreFile = coreFile.trim();
//			if ((coreFile.length() != 0) && !coreFile.startsWith("//"))
//				coreFiles.add(coreFile);
//		}
//		br.close();
//		
//		for (int s = 0; s < specialFiles.length; s++)
//			coreFiles.remove(specialFiles[s]);
//		
//		return ((String[]) coreFiles.toArray(new String[coreFiles.size()]));
//	}
}