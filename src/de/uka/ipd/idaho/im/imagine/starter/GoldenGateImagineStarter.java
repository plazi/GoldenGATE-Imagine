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
package de.uka.ipd.idaho.im.imagine.starter;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import de.uka.ipd.idaho.im.imagine.GoldenGateImagineConstants;
import de.uka.ipd.idaho.im.imagine.util.UpdateUtils;
import de.uka.ipd.idaho.im.imagine.util.UpdateUtils.UpdateStatusDialog;

/**
 * Starter for GoldenGATE Imagine Application, downloading updates, etc. before
 * starting actual application.
 * 
 * @author sautter
 */
public class GoldenGateImagineStarter implements GoldenGateImagineConstants {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		//	set platform L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		//	read data path
		String dataBasePath = "./";
		
		//	get base path (if different from current directory)
		StringBuffer argAssembler = new StringBuffer();
		for (int a = 0; a < args.length; a++) {
			String arg = args[a];
			if (arg == null)
				continue;
			if (arg.startsWith(BASE_PATH_PARAMETER + "="))
				dataBasePath = arg.substring((BASE_PATH_PARAMETER + "=").length());
			else {
				if ((arg.indexOf(' ') != -1) && !arg.startsWith("\""))
					arg = ("\"" + arg + "\"");
				argAssembler.append(" " + arg);
			}
		}
		File basePath = new File(dataBasePath);
		
		//	load startup parameters
		String startMemory = DEFAULT_START_MEMORY;
		String maxMemory = DEFAULT_MAX_MEMORY;
		String proxyName = null;
		String proxyPort = null;
		String proxyUser = null;
		String proxyPwd = null;
		try {
			BufferedReader parameterReader = new BufferedReader(new FileReader(new File(basePath, PARAMETER_FILE_NAME)));
			String line;
			while ((line = parameterReader.readLine())  != null) {
				if (line.startsWith(START_MEMORY_NAME + "="))
					startMemory = line.substring(START_MEMORY_NAME.length() + 1).trim();
				else if (line.startsWith(MAX_MEMORY_NAME + "="))
					maxMemory = line.substring(MAX_MEMORY_NAME.length() + 1).trim();
				else if (line.startsWith(PROXY_NAME + "="))
					proxyName = line.substring(PROXY_NAME.length() + 1).trim();
				else if (line.startsWith(PROXY_PORT + "="))
					proxyPort = line.substring(PROXY_PORT.length() + 1).trim();
				else if (line.startsWith(PROXY_USER + "="))
					proxyUser = line.substring(PROXY_USER.length() + 1).trim();
				else if (line.startsWith(PROXY_PWD + "="))
					proxyPwd = line.substring(PROXY_PWD.length() + 1).trim();
			}
			parameterReader.close();
		}
		catch (FileNotFoundException fnfe) {
			System.out.println("GoldenGateImagineStarter: " + fnfe.getClass().getName() + " (" + fnfe.getMessage() + ") while reading GoldenGATE startup parameters.");
		}
		catch (IOException ioe) {
			System.out.println("GoldenGateImagineStarter: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while reading GoldenGATE startup parameters.");
		}
		
		//	configure web access
		if (proxyName != null) {
			System.getProperties().put("proxySet", "true");
			System.getProperties().put("proxyHost", proxyName);
			if (proxyPort != null) System.getProperties().put("proxyPort", proxyPort);
			
			if ((proxyUser != null) && (proxyPwd != null)) {
				//	initialize proxy authentication
			}
		}
		
		//	open monitoring dialog
		UpdateStatusDialog sd = new UpdateStatusDialog(Toolkit.getDefaultToolkit().getImage(new File(new File(basePath, DATA_FOLDER_NAME), ICON_FILE_NAME).toString()));
		sd.popUp();
		
		//	ask if web access allowed
		boolean online = (JOptionPane.showConfirmDialog(sd, "Allow GoldenGATE Imagine and its components to access the web?", "Allow Web Access?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION);
		
		//	download updates if allowed to
		if (online) {
			sd.setTitle(STATUS_DIALOG_MAIN_TITLE + " - Downloading Updates");
			File ggiJar = new File("./GgImagine.jar");
			String[] updateHosts = UpdateUtils.readUpdateHosts(basePath);
			UpdateUtils.downloadUpdates(basePath, ggiJar.lastModified(), updateHosts, sd);
		}
		
		//	install updates
		sd.setTitle(STATUS_DIALOG_MAIN_TITLE + " - Installing Updates");
		UpdateUtils.installUpdates(basePath, sd);
		
		//	clean up .log and .old files
		sd.setTitle(STATUS_DIALOG_MAIN_TITLE + " - Cleaning Up Old Files");
		cleanUpFiles(basePath, ".old");
		cleanUpFiles(basePath, ".new");
		cleanUpFiles(basePath, ".log");
		
		//	start GoldenGATE itself
		sd.setTitle(STATUS_DIALOG_MAIN_TITLE + " - Starting Main Program");
		String command = "java -jar -Xms" + startMemory + "m -Xmx" + maxMemory + "m GgImagine.jar " + RUN_PARAMETER + (online ? (" " + ONLINE_PARAMETER) : "") + argAssembler.toString();
		System.out.println("GoldenGateImagineStarter: command is '" + command + "'");
		final Process ggiProcess = Runtime.getRuntime().exec(command, null, basePath);
		
		//	close startup frame
		sd.dispose();
		
		//	close output when GoldenGATE closed
		new Thread(new Runnable() {
			public void run() {
				try {
					int ggExit = ggiProcess.waitFor();
					System.out.println("GoldenGATE Imagine termined: " + ggExit);
				} catch (Exception e) {}
				System.exit(0);
			}
		}).start();
	}
	
	private static final String STATUS_DIALOG_MAIN_TITLE = "GoldenGATE Imagine Starting";
	
	private static void cleanUpFiles(File folder, final String ending) {
		File[] files = folder.listFiles(new FileFilter() {
			public boolean accept(File file) {
				return ((file != null) && (file.isDirectory() || file.getName().endsWith(ending)));
			}
		});
		if (files == null)
			return;
		for (int f = 0; f < files.length; f++) {
			if (files[f].getName().endsWith(ending))
				files[f].delete();
			else cleanUpFiles(files[f], ending);
		}
	}
}
