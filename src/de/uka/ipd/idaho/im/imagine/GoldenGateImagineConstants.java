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
package de.uka.ipd.idaho.im.imagine;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/**
 * Constants for the GoldenGATE Imagine
 * 
 * @author sautter
 */
public interface GoldenGateImagineConstants {
	
	/** The default title for a GoldenGATE Imagine window, namely 'GoldenGATE Imagine' */
	public static final String DEFAULT_WINDOW_TITLE = "GoldenGATE Imagine";
	
	/** The current GoldenGATE Imagine version number, prefixed by a 'V' */
	public static final String VERSION_STRING = "IM";
	
	/**
	 * The version date of the GoldenGATE Imagine Core, indicating when the
	 * editor core was last modified
	 */
	public static final String VERSION_DATE = "2014.11.10.21.00";
	
	/** the name of the file containing the startup parameters */
	public static final String PARAMETER_FILE_NAME = "Parameters.cnfg";
	
	/** the name of the file containing GoldenGATE Imagine's configuration parameters */
	public static final String CONFIG_FILE_NAME = "GgImagine.cnfg";
	
	/** the name of GoldenGATE Imagine's readme file */
	public static final String README_FILE_NAME = "README.txt";
	
	
	/** the name of the file holding the URLs to obtain configurations from */
	public static final String CONFIG_HOST_FILE_NAME = "ConfigHosts.cnfg";
	
	/** the name of the folder containing the configurations available locally */
	public static final String CONFIG_FOLDER_NAME = "Configurations";
	
	
	/** the name of the setting holding the initial memory for the Java VM */
	public static final String START_MEMORY_NAME = "START_MEMORY";
	
	/** the name of the setting holding the maximum memory for the Java VM */
	public static final String MAX_MEMORY_NAME = "MAX_MEMORY";
	
	/** default value for initial VM memory: 128 MB */
	public static final String DEFAULT_START_MEMORY = "128";
	
	/** default value for maximum VM memory: 512 MB */
	public static final String DEFAULT_MAX_MEMORY = "512";
	
	
	/** the parameter indicating the GoldenGATE Imagine main class that it was started in an appropriate way */
	public static final String RUN_PARAMETER = "RUN";
	
	/** the parameter indicating the GoldenGATE Imagine main class that it is granted access to the network and the web */
	public static final String ONLINE_PARAMETER = "ONLINE";
	
	/** the parameter specifying the base path where GoldenGATE Imagine runs */
	public static final String BASE_PATH_PARAMETER = "PATH";
	
	/** the parameter specifying the logging behavior and log file name */
	public static final String LOG_PARAMETER = "LOG";
	
	
	/** the name of the file holding the URLs to obtain core system updates from */
	public static final String UPDATE_HOST_FILE_NAME = "UpdateHosts.cnfg";
	
	/** the name of the folder to store updates locally */
	public static final String UPDATE_FOLDER_NAME = "Update";
	
	
	/** the setting holding the name of the www proxy (if any) */
	public static final String PROXY_NAME = "PROXY_NAME";
	
	/** the setting holding the port of the www proxy (if any) */
	public static final String PROXY_PORT = "PROXY_PORT";
	
	/** the setting holding the user name for the www proxy (if any) */
	public static final String PROXY_USER = "PROXY_USER";
	
	/** the setting holding the password for the www proxy (if any) */
	public static final String PROXY_PWD = "PROXY_PWD";
	
	
	/** the setting for the log file folder */
	public static final String LOG_PATH = "LOG_PATH";
	
	/** the default folder for log files data*/
	public static final String LOG_FOLDER_NAME = "Logs";
	
	/** the folder for misc data*/
	public static final String DATA_FOLDER_NAME = "Data";
	
	/** the folder for help html files */
	public static final String DOCUMENTATION_FOLDER_NAME = "Documentation";
	
	/** the folder for plugin components*/
	public static final String PLUGIN_FOLDER_NAME = "Plugins";
	
	
	/** the suffix to append to the name of a jar file for the name of the folder holding subordinate jar files: 'Bin' */
	public static final String JAR_BIN_FOLDER_SUFFIX = "Bin";
	
	/** the suffix to append to the name of a jar file for the name of the folder holding the jar's data: 'Data' */
	public static final String JAR_DATA_FOLDER_SUFFIX = "Data";
	
	
	/** the default name of the file containing the GoldenGATE logo icon image */
	public static final String ICON_FILE_NAME = "GoldenGATE.logo.gif";
	
	
	/** constant menu item to use for plugins to indicate a separator in their menu */
	public static final JMenuItem MENU_SEPARATOR_ITEM = new JMenuItem();
	
	/**
	 * Status dialog for monitoring extended processes like updates or
	 * configuration-related actions. The popUp() method does not block, but
	 * starts a separate thread that stays alive until the dialog is closed.
	 * 
	 * @author sautter
	 */
	public static class StatusDialog extends JFrame implements Runnable {
		
		/**
		 * Button optically matching the status dialog.
		 * 
		 * @author sautter
		 */
		public static class StatusDialogButton extends JButton {
			/**
			 * Constructor
			 * @param text the button label
			 */
			public StatusDialogButton(String text) {
				this(text, 0, 0);
			}
			/**
			 * Constructor
			 * @param text the button label
			 * @param space the white spacing around the button
			 */
			public StatusDialogButton(String text, int space) {
				this(text, space, space);
			}
			/**
			 * Constructor
			 * @param text the button label
			 * @param horizontalSpace the white spacing left and right of the
			 *            button
			 * @param verticalSpace the vertical white spacing above and below
			 *            the button
			 */
			public StatusDialogButton(String text, int horizontalSpace, int verticalSpace) {
				super(" " + text.trim() + " ");
				this.setBackground(Color.WHITE);
				this.setForeground(Color.RED);
				this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(verticalSpace, horizontalSpace, verticalSpace, horizontalSpace,Color.WHITE), BorderFactory.createLineBorder(Color.RED, 1)));
			}
		}
		
		private JLabel stepLabel = new JLabel("", JLabel.CENTER);
		
		private LinkedList labelLines = new LinkedList();
		private JLabel statusLabel = new JLabel("", JLabel.CENTER);
		
		private JComponent custom = null;
		
		private JPanel contentPanel = new JPanel(new BorderLayout());
		
		private TitledBorder titleBorder;
		
		private Thread thread = null;
		
		/**
		 * Constructor
		 * @param icon the icon for the dialog
		 * @param title the title for the dialog
		 */
		public StatusDialog(Image icon, String title) {
			super(title);
			this.setIconImage(icon);
			this.setUndecorated(true);
			
			this.contentPanel.add(this.statusLabel, BorderLayout.CENTER);
			
			Font labelFont = UIManager.getFont("Label.font");
			Border outer = BorderFactory.createMatteBorder(2, 3, 2, 3, Color.LIGHT_GRAY);
			Border inner = BorderFactory.createEtchedBorder(Color.RED, Color.DARK_GRAY);
			Border compound = BorderFactory.createCompoundBorder(outer, inner);
			this.titleBorder = BorderFactory.createTitledBorder(compound, this.getTitle(), TitledBorder.LEFT, TitledBorder.TOP, labelFont.deriveFont(Font.BOLD));
			this.contentPanel.setBorder(this.titleBorder);
			this.contentPanel.setBackground(Color.WHITE);
			
			this.getContentPane().setLayout(new BorderLayout());
			this.getContentPane().add(this.contentPanel, BorderLayout.CENTER);
			
			this.setSize(400, 120);
			this.setLocationRelativeTo(null);
			this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		}
		
		/* (non-Javadoc)
		 * @see java.awt.Frame#setTitle(java.lang.String)
		 */
		public void setTitle(String title) {
			super.setTitle(title);
			this.titleBorder.setTitle(title);
		}
		
		/**
		 * Set the custom component of the dialog, to be displayed at its bottom
		 * (BorderLayout.SOUTH position). This can be a progress bar, a button,
		 * etc. Only one component can be present at any time. A second
		 * invocation of this method will replace the custom component set in
		 * any previous invocation.
		 * @param custom the custom component to display
		 */
		public void setCustomComponent(JComponent custom) {
			if (this.custom != null)
				this.contentPanel.remove(this.custom);
			this.custom = custom;
			if (this.custom != null)
				this.contentPanel.add(this.custom, BorderLayout.SOUTH);
			this.contentPanel.validate();
			this.contentPanel.repaint();
		}
		
		/**
		 * Set the major step, displayed in bold at the top of the dialog.
		 * Setting the text to null or a whitespace-only string hides the step
		 * label. If the argument string does not start with '&lt;HTML&gt;', it
		 * is displayed in all bold, non-italics.
		 * @param step the step label to set
		 */
		public void setStepLabel(String step) {
			if (step == null)
				step = "";
			if (step.trim().length() == 0) {
				this.contentPanel.remove(this.stepLabel);
				this.contentPanel.validate();
				this.contentPanel.repaint();
				return;
			}
			else {
				this.contentPanel.validate();
				this.contentPanel.repaint();
				this.contentPanel.add(this.stepLabel, BorderLayout.NORTH);
				if ((step.length() < 6) || !"<html>".equals(step.substring(0, 6).toLowerCase()))
					step = ("<HTML><B>" + step + "<B></HTML>");
				try {
					this.stepLabel.setText(step);
					this.stepLabel.validate();
				} catch (RuntimeException re) {/* catch that stupid BadLocationException Swing tends to throw when dialog looses focus */}
			}
		}
		
		/**
		 * Add a line to the status label of the dialog, which always displays
		 * the last three lines. Adding a null or whitespace-only status has no
		 * effect.
		 * @param status the status label to add
		 */
		public void setStatusLabel(String status) {
			if ((status == null) || (status.trim().length() == 0))
				return;
			this.labelLines.addLast(status);
			while (this.labelLines.size() > 3)
				this.labelLines.removeFirst();
			StringBuffer labelString = new StringBuffer("<HTML>");
			for (Iterator llit = this.labelLines.iterator(); llit.hasNext();) {
				if (labelString.length() > 6)
					labelString.append("<BR>");
				labelString.append((String) llit.next());
			}
			labelString.append("</HTML>");
			try {
				this.statusLabel.setText(labelString.toString());
				this.statusLabel.validate();
			} catch (RuntimeException re) {/* catch that stupid BadLocationException Swing tends to throw when dialog looses focus */}
		}
		
		/**
		 * Make the dialog pop up. This method does not block.
		 */
		public void popUp() {
			
			//	check and create display thread
			synchronized (this) {
				if (this.thread != null)
					return;
				this.thread = new Thread(this);
				this.thread.start();
			}
			
			//	if we're the Swing EDT, we cannot wait as we have to paint the dialog
			if (SwingUtilities.isEventDispatchThread())
				return;
			
			//	wait for dialog to show
			while (!this.isVisible()) try {
				Thread.sleep(50);
			} catch (InterruptedException ie) {}
		}
		
		public void run() {
			this.setVisible(true);
			
			//	wait for dialog to actually show
			while (!this.isVisible()) try {
				Thread.sleep(50);
			} catch (InterruptedException ie) {}
			
			//	wait for dialog to disappear
			while (this.isVisible()) try {
				Thread.sleep(50);
			} catch (InterruptedException ie) {}
			
			//	make way for becoming visible next time
			this.thread = null;
		}
	}
}