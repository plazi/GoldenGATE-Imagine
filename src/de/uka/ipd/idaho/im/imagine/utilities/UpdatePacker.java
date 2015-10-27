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

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import de.uka.ipd.idaho.im.imagine.util.UpdateUtils;

/**
 * @author sautter
 *
 */
public class UpdatePacker {
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		try {
			buildUpdate();
		}
		catch (Exception e) {
			System.out.println("An error occurred creating the update zip file:\n" + e.getMessage());
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(null, ("An error occurred creating the update zip file:\n" + e.getMessage()), "Update Creation Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private static void buildUpdate() throws Exception {	
		File rootFolder = new File(PackerUtils.normalizePath(new File(".").getAbsolutePath()));
		System.out.println("Root folder is '" + rootFolder.getAbsolutePath() + "'");
		
		long updateTimestamp = System.currentTimeMillis();
		System.out.println("Update timestamp is " + updateTimestamp);
		
//		String updateName = "GgUpdate." + timestamper.format(new Date(updateTimestamp)) + ".zip";
		String updateName = UpdateUtils.getUpdateName(updateTimestamp);
		System.out.println("Building update '" + updateName + "'");
		
		long lastUpdateTimestamp = getLastUpdateTimestamp(rootFolder);
		System.out.println("Last update was " + lastUpdateTimestamp);
		
		//	TODO read relevant JAR names from config file
		
		String[] updateFileNames = getUpdateJars(rootFolder, updateName, lastUpdateTimestamp);
		if (updateFileNames.length == 0) {
			System.out.println("No jar files selected for update, aborting.");
			return;
		}
		System.out.println("Including the following jar files in update:");
		for (int u = 0; u < updateFileNames.length; u++)
			System.out.println(" - " + updateFileNames[u]);
		
		PackerUtils.updateReadmeFile(rootFolder, null, null, updateTimestamp, (5 * 60 * 1000));
		
		File updateZipFile = new File(rootFolder, ("_Updates/" + updateName));
		System.out.println("Creating update zip file '" + updateZipFile.getAbsolutePath() + "'");
		
		updateZipFile.getParentFile().mkdirs();
		updateZipFile.createNewFile();
		ZipOutputStream updateZipper = new ZipOutputStream(new FileOutputStream(updateZipFile));
		
		PackerUtils.writeZipFileEntries(rootFolder, updateZipper, updateFileNames);
		PackerUtils.writeZipFileEntry(new File(rootFolder, PackerUtils.README_FILE_NAME), PackerUtils.README_FILE_NAME, updateZipper);
		
		updateZipper.flush();
		updateZipper.close();
		
		System.out.println("Update zip file '" + updateZipFile.getAbsolutePath() + "' created successfully.");
		JOptionPane.showMessageDialog(null, ("Update '" + updateName + "' created successfully."), "Update Created Successfully", JOptionPane.INFORMATION_MESSAGE);
	}
	
	private static long getLastUpdateTimestamp(File rootFolder) {
		File updateFolder = new File(rootFolder, "_Updates/");
		File[] updateFiles = updateFolder.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return (file.isFile() && file.getName().endsWith(".zip") && file.getName().startsWith("GgUpdate.IM."));
			}
		});
		
		long lastUpdateTimestamp = 0;
		if (updateFiles != null)
			for (int u = 0; u < updateFiles.length; u++)
				lastUpdateTimestamp = Math.max(lastUpdateTimestamp, updateFiles[u].lastModified());
		
		return lastUpdateTimestamp;
	}
	
	private static String[] getUpdateJars(File folder, String updateName, long lastUpdateTimestamp) {
		String[] jars = getJars(folder);
		
		Set selectedJars = new TreeSet();
		Set newJars = new TreeSet();
		for (int j = 0; j < jars.length; j++)
			if (lastUpdateTimestamp < new File(folder, jars[j]).lastModified()) {
				if ((jars[j].indexOf("/") == -1) && (jars[j].indexOf("Starter.jar") == -1))
					selectedJars.add(jars[j]);
				newJars.add(jars[j]);
			}
		
		return PackerUtils.selectStrings(jars, selectedJars, newJars, ("Select Jar Files to Include in Update '" + updateName + "'"), null);
	}
	
	private static String[] getJars(File folder) {
		String[] files = PackerUtils.listFilesRelative(folder, 8);
		Set jars = new TreeSet();
		for (int f = 0; f < files.length; f++)
			if (files[f].endsWith(".jar"))
				jars.add(files[f]);
		return ((String[]) jars.toArray(new String[jars.size()]));
	}
}
