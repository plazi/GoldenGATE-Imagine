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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextLayout;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import de.uka.ipd.idaho.easyIO.help.Help;
import de.uka.ipd.idaho.easyIO.help.HelpChapter;
import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.easyIO.streams.PeekInputStream;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.util.GenericGamtaXML;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.gamta.util.swing.ProgressMonitorDialog;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentSaver;
import de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin;
import de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePluginDataProvider;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceSplashScreen;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.HelpChapterDataProviderBased;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImDocument.ImDocumentListener;
import de.uka.ipd.idaho.im.ImObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
import de.uka.ipd.idaho.im.imagine.GoldenGateImagine;
import de.uka.ipd.idaho.im.imagine.plugins.ImageDocumentDropHandler;
import de.uka.ipd.idaho.im.imagine.plugins.ImageDocumentExporter;
import de.uka.ipd.idaho.im.imagine.plugins.ImageEditToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider;
import de.uka.ipd.idaho.im.pdf.PdfExtractor;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.PagePoint;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.PageThumbnail;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.TwoClickActionMessenger;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.TwoClickSelectionAction;
import de.uka.ipd.idaho.im.util.ImImageEditorPanel.ImImageEditTool;
import de.uka.ipd.idaho.im.util.ImfIO;
import de.uka.ipd.idaho.im.util.SymbolTable;

/**
 * Default GUI for GoldenGATE Imagine
 * 
 * @author sautter
 */
public class GoldenGateImagineUI extends JFrame implements ImagingConstants, GoldenGateConstants {
	
	private GoldenGateImagine ggImagine;
	private Settings config;
	
	private JMenuBar mainMenu = new JMenuBar();
	private JMenu undoMenu = new JMenu("Undo");
	
	private JCheckBoxMenuItem allowReactionPrompts = new JCheckBoxMenuItem("Prompt in Reaction to Input");
	
	private GoldenGatePluginDataProvider helpDataProvider;
	private HelpChapter helpContent;
	private Help help;
	private JMenu helpMenu;
	
	private JFileChooser fileChooser = new JFileChooser();
	private PdfExtractor pdfExtractor;
	
	private ViewControl viewControl = new ViewControl();
	private JTabbedPane docTabs = new JTabbedPane();
	
	private SelectionActionUsageStats saUsageStats = new SelectionActionUsageStats();
	
	GoldenGateImagineUI(GoldenGateImagine ggImagine, Settings config) {
		super("GoldenGATE Imagine - " + ggImagine.getConfigurationName());
		this.ggImagine = ggImagine;
		this.config = config;
		
		//	set window icon
		this.setIconImage(this.ggImagine.getGoldenGateIcon());
		
		//	get PDF reader
		this.pdfExtractor = this.ggImagine.getPdfExtractor();
		
		//	configure file chooser
		this.fileChooser.setMultiSelectionEnabled(false);
		this.fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		this.fileChooser.setSelectedFile(new File((this.config.getSetting("lastDocFolder", (new File(".")).getAbsolutePath())), " ")); // we need this dummy file name so the folder is actually opened instead of being selected in its parent folder
		
		//	build help first, as entries in other menus have to link up to it
		this.helpDataProvider = this.ggImagine.getHelpDataProvider();
		this.helpContent = this.buildHelpContentRoot();
		this.helpMenu = this.createHelpMenu();
		
		//	build main menu
		this.addFileMenu();
		this.addExportMenu();
		this.addEditMenu();
		this.addMenu(this.undoMenu);
		this.addToolsMenu();
		
		//	finish help
		this.finishHelpMenu();
		this.help = new Help("GoldenGATE Imagine", this.helpContent, this.ggImagine.getGoldenGateIcon());
		
		//	build zoom control
		this.viewControl = new ViewControl();
		
		//	build menu panel
		JPanel menuPanel = new JPanel(new BorderLayout(), true);
		menuPanel.add(this.mainMenu, BorderLayout.CENTER);
		menuPanel.add(this.viewControl, BorderLayout.EAST);
		
		//	build drop target
		DropTarget dropTarget = new DropTarget(this, new DropTargetAdapter() {
			public void drop(DropTargetDropEvent dtde) {
				dtde.acceptDrop(dtde.getDropAction());
				handleDrop(dtde.getTransferable());
			}
		});
		dropTarget.setActive(true);
		
		//	update UNDO menu and zoom control on tab changes
		this.docTabs.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent ce) {
				ImDocumentEditorTab idet = getActiveDocument();
				if (idet == null)
					return;
				updateUndoMenu(idet.undoActions);
				updateViewControl(idet);
			}
		});
		
		//	make sure to focus tabs, not zoom control
		this.setFocusTraversalPolicy(new FocusTraversalPolicy() {
			public Component getComponentAfter(Container aContainer, Component aComponent) {
				return docTabs;
			}
			public Component getComponentBefore(Container aContainer, Component aComponent) {
				return docTabs;
			}
			public Component getFirstComponent(Container aContainer) {
				return docTabs;
			}
			public Component getLastComponent(Container aContainer) {
				return docTabs;
			}
			public Component getDefaultComponent(Container aContainer) {
				return docTabs;
			}
		});
		
		//	make document views scroll on page-up and page-down
		this.docTabs.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "docScrollUp");
		this.docTabs.getActionMap().put("docScrollUp", new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				ImDocumentEditorTab idet = getActiveDocument();
				if (idet != null)
					idet.scrollUp();
			}
		});
		this.docTabs.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "docScrollDown");
		this.docTabs.getActionMap().put("docScrollDown", new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				ImDocumentEditorTab idet = getActiveDocument();
				if (idet != null)
					idet.scrollDown();
			}
		});
		
		//	trigger UNDO on Ctrl-Z
		this.docTabs.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "docUndo");
		this.docTabs.getActionMap().put("docUndo", new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				if (undoMenu.getMenuComponentCount() == 0)
					return;
				JMenuItem mi = ((JMenuItem) undoMenu.getMenuComponent(0));
				ActionListener[] miAls = mi.getActionListeners();
				for (int l = 0; l < miAls.length; l++)
					miAls[l].actionPerformed(ae);
			}
		});
		
		//	zoom in and out on Ctrl-<plus> and Ctrl-<minus>
		this.docTabs.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.CTRL_DOWN_MASK), "docZoomIn");
		this.docTabs.getActionMap().put("docZoomIn", new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				viewControl.zoomIn();
			}
		});
		this.docTabs.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK), "docZoomOut");
		this.docTabs.getActionMap().put("docZoomOut", new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				viewControl.zoomOut();
			}
		});
		
		//	set document layout using Ctrl+<arrow-keys>
		this.docTabs.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK), "docPagesHorizontal");
		this.docTabs.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK), "docPagesHorizontal");
		this.docTabs.getActionMap().put("docPagesHorizontal", new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				viewControl.setSideBySidePages(0);
			}
		});
		this.docTabs.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_DOWN_MASK), "docPagesVertical");
		this.docTabs.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_DOWN_MASK), "docPagesVertical");
		this.docTabs.getActionMap().put("docPagesVertical", new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				viewControl.setSideBySidePages(1);
			}
		});
		
		//	initialize most recently used symbols
		SymbolTable.setMostRecentlyUsedSymbols(this.config.getSetting("mostRecentlyUsedSymbols", ""));
		
		//	initialize selection action usage stats
		this.saUsageStats.fillFrom(this.config.getSubset("selectionAction"));
		
		//	make sure we exit on window closing
		this.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent we) {
				exit();
			}
			public void windowClosing(WindowEvent we) {
				exit();
			}
		});
		
		//	assemble major parts
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(menuPanel, BorderLayout.NORTH);
		this.getContentPane().add(this.docTabs, BorderLayout.CENTER);
		this.setSize(1000, 800);
		this.setLocationRelativeTo(null);
	}
	
	private void addFileMenu() {
		HelpChapter menuHelp = new HelpChapterDataProviderBased("Menu 'File'", this.helpDataProvider, "GgImagine.FileMenu.html");
		this.helpContent.addSubChapter(menuHelp);
		JMenuItem helpMi = new JMenuItem("Menu 'File'");
		helpMi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				showHelp("Menu 'File'");
			}
		});
		this.helpMenu.add(helpMi);
		
		JMenu menu = new JMenu("File");
		JMenuItem mi;
		
		mi = new JMenuItem("Open Document");
		mi.addActionListener(new ActionListener() {
			private FileFilter loadFileFilter = null;
			public void actionPerformed(ActionEvent ae) {
				clearFileFilters(fileChooser);
				fileChooser.addChoosableFileFilter(imfFileFilter);
				fileChooser.addChoosableFileFilter(genericPdfFileFilter);
				fileChooser.addChoosableFileFilter(textPdfFileFilter);
				fileChooser.addChoosableFileFilter(imagePdfFileFilter);
				fileChooser.addChoosableFileFilter(imageAndMetaPdfFileFilter);
				if (this.loadFileFilter != null)
					fileChooser.setFileFilter(this.loadFileFilter);
				if (fileChooser.showOpenDialog(GoldenGateImagineUI.this) != JFileChooser.APPROVE_OPTION)
					return;
				File file = fileChooser.getSelectedFile();
				this.loadFileFilter = fileChooser.getFileFilter();
				try {
					InputStream in = new BufferedInputStream(new FileInputStream(file));
					loadDocument(file.getName(), file, null, this.loadFileFilter, in, ((int) file.length()));
					in.close();
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(GoldenGateImagineUI.this, ("An error occurred while loading a document from '" + file.getAbsolutePath() + "':\n" + e.getMessage()), "Error Loading Document", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace(System.out);
				}
			}
		});
		menu.add(mi);
		
		mi = new JMenuItem("Load Document from URL");
		mi.addActionListener(new ActionListener() {
			private FileFilter loadFormat = genericPdfFileFilter;
			public void actionPerformed(ActionEvent ae) {
				UrlLoadDialog uld = new UrlLoadDialog(null, this.loadFormat);
				uld.setVisible(true);
				this.loadFormat = uld.getFormat();
			}
		});
		menu.add(mi);
		
		mi = new JMenuItem("Save Document");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				ImDocumentEditorTab idet = getActiveDocument();
				if (idet == null)
					return;
				if (idet.save())
					return;
				idet.saveAs(fileChooser);
			}
		});
		menu.add(mi);
		
		mi = new JMenuItem("Save Document As");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				ImDocumentEditorTab idet = getActiveDocument();
				if (idet == null)
					return;
				idet.saveAs(fileChooser);
			}
		});
		menu.add(mi);
		
		mi = new JMenuItem("Close Document");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				closeDocument();
			}
		});
		menu.add(mi);
		
		mi = new JMenuItem("Exit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				exit();
			}
		});
		menu.add(mi);
		
		menu.addSeparator();
		mi = new JMenuItem("Select Pages");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				ImDocumentEditorTab idet = getActiveDocument();
				if (idet != null)
					idet.selectVisiblePages();
			}
		});
		menu.add(mi);
		
		this.addMenu(menu);
	}
	
	private void handleDrop(Transferable transfer) {
		DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
		for (int d = 0; d < dataFlavors.length; d++) {
			System.out.println("Trying data flavor " + dataFlavors[d].toString());
			System.out.println(" - MIME type is " + dataFlavors[d].getMimeType());
			System.out.println(" - representation class is " + dataFlavors[d].getRepresentationClass());
			
			//	nothing to work with
			if (dataFlavors[d].getMimeType() == null)
				continue;
			
			//	file drop
			if (("application/x-java-file-list".equalsIgnoreCase(dataFlavors[d].getMimeType()) || dataFlavors[d].getMimeType().toLowerCase().startsWith("application/x-java-file-list; class=")) && List.class.isAssignableFrom(dataFlavors[d].getRepresentationClass())) try {
				List droppedFileList = ((List) transfer.getTransferData(dataFlavors[d]));
				for (int t = 0; t < droppedFileList.size(); t++) {
					File droppedFile = ((File) droppedFileList.get(t));
					try {
						FileFilter matchFileFilter;
						if (imfFileFilter.accept(droppedFile))
							matchFileFilter = imfFileFilter;
						else if (genericPdfFileFilter.accept(droppedFile))
							matchFileFilter = genericPdfFileFilter;
						else continue;
						InputStream in = new BufferedInputStream(new FileInputStream(droppedFile));
						loadDocument(droppedFile.getName(), droppedFile, null, matchFileFilter, in, ((int) droppedFile.length()));
						in.close();
					}
					catch (SecurityException se) {
						System.out.println("Error opening document '" + droppedFile.getName() + "':\n   " + se.getClass().getName() + " (" + se.getMessage() + ")");
						se.printStackTrace(System.out);
						JOptionPane.showMessageDialog(GoldenGateImagineUI.this, ("Not allowed to open file '" + droppedFile.getName() + "':\n" + se.getMessage() + "\n\nIf you are currently running GoldenGATE Editor as an applet, your\nbrowser's security mechanisms might prevent reading files from your local disc."), "Not Allowed To Open File", JOptionPane.ERROR_MESSAGE);
					}
					catch (Exception e) {
						System.out.println("Error opening document '" + droppedFile.getAbsolutePath() + "':\n   " + e.getClass().getName() + " (" + e.getMessage() + ")");
						e.printStackTrace(System.out);
						JOptionPane.showMessageDialog(GoldenGateImagineUI.this, ("Could not open file '" + droppedFile.getAbsolutePath() + "':\n" + e.getMessage()), "Error Opening File", JOptionPane.ERROR_MESSAGE);
					}
				}
				return;
			}
			catch (Exception e) {
				e.printStackTrace(System.out);
			}
			
			//	URL drop
			if (("application/x-java-url".equalsIgnoreCase(dataFlavors[d].getMimeType()) || dataFlavors[d].getMimeType().toLowerCase().startsWith("application/x-java-url; class=")) && URL.class.isAssignableFrom(dataFlavors[d].getRepresentationClass())) try {
				URL droppedUrl = ((URL) transfer.getTransferData(dataFlavors[d]));
				String droppedUrlString = droppedUrl.toString();
				FileFilter matchFileFilter = null;
				if (droppedUrlString.toLowerCase().matches("http\\:\\/\\/(.+\\/)+.+\\.imf"))
					matchFileFilter = imfFileFilter;
				else if (droppedUrlString.toLowerCase().matches("http\\:\\/\\/(.+\\/)+.+\\.pdf"))
					matchFileFilter = genericPdfFileFilter;
				UrlLoadDialog uld = new UrlLoadDialog(droppedUrlString, matchFileFilter);
				uld.setVisible(true);
				return;
			}
			catch (Exception e) {
				e.printStackTrace(System.out);
			}
		}
	}
	
	private class UrlLoadDialog extends DialogPanel {
		private JTextField urlInput = new JTextField("http://");
		private JComboBox formatChooser = new JComboBox();
		private JSpinner timeoutChooser = new JSpinner(new SpinnerNumberModel(15, 5, 300, 5));
		
		UrlLoadDialog(String urlString, FileFilter format) {
			super("Open Document from URL", true);
			
			if (urlString != null)
				this.urlInput.setText(urlString);
			this.urlInput.setBorder(BorderFactory.createLoweredBevelBorder());
			this.urlInput.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					loadUrl();
				}
			});
			
			this.formatChooser.addItem(imfFileFilter);
			this.formatChooser.addItem(genericPdfFileFilter);
			this.formatChooser.addItem(textPdfFileFilter);
			this.formatChooser.addItem(imagePdfFileFilter);
			this.formatChooser.addItem(imageAndMetaPdfFileFilter);
			this.formatChooser.setSelectedItem((format == null) ? genericPdfFileFilter : format);
			this.formatChooser.setBorder(BorderFactory.createLoweredBevelBorder());
			this.formatChooser.setEditable(false);
			
			this.timeoutChooser.setBorder(BorderFactory.createLoweredBevelBorder());
			
			JPanel selectorPanel = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.insets.top = 5;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.insets.bottom = 5;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.weightx = 0;
			selectorPanel.add(new JLabel("URL"), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			selectorPanel.add(this.urlInput, gbc.clone());
			
			gbc.gridy = 1;
			gbc.gridx = 0;
			gbc.weightx = 0;
			selectorPanel.add(new JLabel("Format"), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			selectorPanel.add(this.formatChooser, gbc.clone());
			
			//	initialize timeout selector
			JPanel timeoutPanel = new JPanel(new BorderLayout(), true);
			timeoutPanel.add(new JLabel("Timeout (seconds) "), BorderLayout.WEST);
			timeoutPanel.add(this.timeoutChooser, BorderLayout.CENTER);
			
			//	initialize buttons
			JButton commitButton = new JButton("Open Document");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					loadUrl();
				}
			});
			JButton cancelButton = new JButton("Cancel");
			cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
			cancelButton.setPreferredSize(new Dimension(100, 21));
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dispose();
				}
			});
			
			//	assemble button panel
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			buttonPanel.add(timeoutPanel);
			buttonPanel.add(commitButton);
			buttonPanel.add(cancelButton);
			
			//	put the whole stuff together
			this.add(selectorPanel, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
			
			//	configure dialog proper
			this.setResizable(true);
			this.setSize(new Dimension(400, 120));
			this.setLocationRelativeTo(GoldenGateImagineUI.this);
			
			//	if we have a URL and a file format, we can start loading right after dialog comes up (load from EDT, though)
			if ((urlString != null) && (format != null)) {
				Thread loadTrigger = new Thread() {
					public void run() {
						while (!getDialog().isVisible()) try {
							Thread.sleep(100);
						} catch (InterruptedException ie) {}
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								loadUrl();
							}
						});
					}
				};
				loadTrigger.start();
			}
		}
		
		void loadUrl() {
			String urlString = this.urlInput.getText();
			try {
				this.loadUrl(urlString);
			}
			catch (Exception e) {
				e.printStackTrace(System.out);
				JOptionPane.showMessageDialog(this, ("An error occurred while loading a document from '" + urlString + "':\n  " + e.getMessage() + "\nIf the error was due to a read timeout, increasing the timeout might fix it."), "Error Loading Document", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		void loadUrl(final String urlString) throws IOException {
			final URL url = new URL(urlString);
			
			final int timeoutMillis = (Integer.parseInt(this.timeoutChooser.getValue().toString()) * 1000);
			final long[] lastReadMillis = {System.currentTimeMillis()};
			
			final HttpURLConnection[] urlCon = {null};
			final InputStream[] urlIn = {null};
			final byte[][] urlData = {null};
			final IOException[] error = {null};
			
			//	use progress monitor
			final ProgressMonitorDialog urlLoadPm = new ProgressMonitorDialog(this.getDialog(), ("Buffering Data from '" + urlString + "'"));
			urlLoadPm.setBaseProgress(0);
			urlLoadPm.setMaxProgress(100);
			urlLoadPm.setSize(this.getSize());
			urlLoadPm.setLocationRelativeTo(this.getDialog());
			
			//	use extra thread and progress dialog to buffer document
			final Thread urlLoader = new Thread() {
				public void run() {
					while (!urlLoadPm.getWindow().isVisible()) try {
						Thread.sleep(50);
					} catch (InterruptedException ie) {}
					
					try {
						urlLoadPm.setInfo("Connecting to source");
						urlCon[0] = ((HttpURLConnection) url.openConnection());
						urlCon[0].connect();
						urlLoadPm.setInfo("Connection established");
						urlIn[0] = new BufferedInputStream(urlCon[0].getInputStream());
						urlLoadPm.setInfo("Got input stream");
						if (error[0] != null) // we might have timed out by now
							return;
						lastReadMillis[0] = System.currentTimeMillis();
						int dataBytesTotal = ((urlCon[0].getContentLength() == -1) ? (1024 * 1024) : urlCon[0].getContentLength()); // start with estimate of 1MB if content length missing
						int dataBytesRead = 0;
						ByteArrayOutputStream dataBytes = new ByteArrayOutputStream();
						byte[] dataByteBuffer = new byte[1024];
						for (int r; (r = urlIn[0].read(dataByteBuffer, 0, dataByteBuffer.length)) != -1;) {
							dataBytes.write(dataByteBuffer, 0, r);
							lastReadMillis[0] = System.currentTimeMillis();
							if (error[0] != null)
								break;
							dataBytesRead += r;
							urlLoadPm.setInfo("Read " + r + " more bytes");
							while (dataBytesTotal < dataBytesRead)
								dataBytesTotal += (1024 * 512); // increment estimate by 512KB
							urlLoadPm.setProgress((dataBytesRead * 100) / dataBytesTotal);
						}
						urlIn[0].close();
						urlData[0] = dataBytes.toByteArray();
						urlLoadPm.setInfo("Data buffered completely");
						urlLoadPm.setProgress(100);
					}
					catch (IOException ioe) {
						error[0] = ioe;
						urlLoadPm.setInfo("Error buffering data: " + ioe.getMessage());
					}
					finally {
						urlLoadPm.close();
					}
				}
			};
			urlLoader.start();
			
			final Thread timeoutGuard = new Thread() {
				public void run() {
					while (!urlLoadPm.getWindow().isVisible()) try {
						Thread.sleep(50);
					} catch (InterruptedException ie) {}
					
					try {
						while (true) {
							try {
								Thread.sleep(timeoutMillis / 5);
							} catch (InterruptedException ie) {}
							
							//	we're done reading
							if (urlData[0] != null)
								return;
							
							//	we're on time
							if (System.currentTimeMillis() < (lastReadMillis[0] + timeoutMillis))
								continue;
							
							//	connection timeout
							if (urlIn[0] == null) {
								urlLoader.interrupt();
								error[0] = new IOException("Timeout establishing connection to '" + urlString + "'");
							}
							
							//	read timeout (exception is thrown from reader in that case)
							else try {
								urlLoader.interrupt();
								urlIn[0].close();
							} catch (IOException ioe) {}
							
							//	whichever way, we're done here
							return;
						}
					}
					finally {
						urlLoadPm.close();
					}
				}
			};
			timeoutGuard.start();
			
			//	open progress monitor (waits for buffering to time out or complete)
			urlLoadPm.popUp(true);
			
			//	throw any exception that might have occurred
			if (error[0] != null)
				throw error[0];
			
			//	delegate to main loading method
			this.dispose();
			String docName = urlString;
			if (docName.indexOf("//") != -1)
				docName = docName.substring(docName.indexOf("//") + "//".length());
			docName = docName.replaceAll("[\\/\\:]+", "_");
			loadDocument(docName, null, urlString, this.getFormat(), new ByteArrayInputStream(urlData[0]), urlData[0].length);
		}
		
		FileFilter getFormat() {
			return ((FileFilter) this.formatChooser.getSelectedItem());
		}
	}
	
	private void addExportMenu() {
		HelpChapter menuHelp = new HelpChapterDataProviderBased("Menu 'Export'", this.helpDataProvider, "GgImagine.ExportMenu.html");
		this.helpContent.addSubChapter(menuHelp);
		JMenuItem helpMi = new JMenuItem("Menu 'Export'");
		helpMi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				showHelp("Menu 'Export'");
			}
		});
		this.helpMenu.add(helpMi);
		
		JMenu menu = new JMenu("Export");
		JMenuItem mi;
		
		mi = new JMenuItem("As XML (without element IDs)");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				ImDocumentEditorTab idet = getActiveDocument();
				if (idet == null)
					return;
				clearFileFilters(fileChooser);
				fileChooser.addChoosableFileFilter(xmlFileFilter);
				if (fileChooser.showSaveDialog(GoldenGateImagineUI.this) != JFileChooser.APPROVE_OPTION)
					return;
				File file = fileChooser.getSelectedFile();
				if (file.isDirectory())
					return;
				try {
					exportXml(idet.idmp.document, file, ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS, false);
				}
				catch (IOException ioe) {
					JOptionPane.showMessageDialog(GoldenGateImagineUI.this, ("An error occurred while exporting the document to '" + file.getAbsolutePath() + "':\n" + ioe.getMessage()), "Error Exporting Document", JOptionPane.ERROR_MESSAGE);
					ioe.printStackTrace(System.out);
				}
			}
		});
		menu.add(mi);
		
		mi = new JMenuItem("As XML (with element IDs)");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				ImDocumentEditorTab idet = getActiveDocument();
				if (idet == null)
					return;
				clearFileFilters(fileChooser);
				fileChooser.addChoosableFileFilter(xmlFileFilter);
				if (fileChooser.showSaveDialog(GoldenGateImagineUI.this) != JFileChooser.APPROVE_OPTION)
					return;
				File file = fileChooser.getSelectedFile();
				if (file.isDirectory())
					return;
				try {
					exportXml(idet.idmp.document, file, ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS, true);
				}
				catch (IOException ioe) {
					JOptionPane.showMessageDialog(GoldenGateImagineUI.this, ("An error occurred while exporting the document to '" + file.getAbsolutePath() + "':\n" + ioe.getMessage()), "Error Exporting Document", JOptionPane.ERROR_MESSAGE);
					ioe.printStackTrace(System.out);
				}
			}
		});
		menu.add(mi);
		
		mi = new JMenuItem("As Raw XML (without element IDs)");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				ImDocumentEditorTab idet = getActiveDocument();
				if (idet == null)
					return;
				clearFileFilters(fileChooser);
				fileChooser.addChoosableFileFilter(xmlFileFilter);
				if (fileChooser.showSaveDialog(GoldenGateImagineUI.this) != JFileChooser.APPROVE_OPTION)
					return;
				File file = fileChooser.getSelectedFile();
				if (file.isDirectory())
					return;
				try {
					exportXml(idet.idmp.document, file, (ImDocumentRoot.NORMALIZATION_LEVEL_RAW | ImDocumentRoot.SHOW_TOKENS_AS_WORD_ANNOTATIONS), false);
				}
				catch (IOException ioe) {
					JOptionPane.showMessageDialog(GoldenGateImagineUI.this, ("An error occurred while exporting the document to '" + file.getAbsolutePath() + "':\n" + ioe.getMessage()), "Error Exporting Document", JOptionPane.ERROR_MESSAGE);
					ioe.printStackTrace(System.out);
				}
			}
		});
		menu.add(mi);
		
		mi = new JMenuItem("As Raw XML (with element IDs)");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				ImDocumentEditorTab idet = getActiveDocument();
				if (idet == null)
					return;
				clearFileFilters(fileChooser);
				fileChooser.addChoosableFileFilter(xmlFileFilter);
				if (fileChooser.showSaveDialog(GoldenGateImagineUI.this) != JFileChooser.APPROVE_OPTION)
					return;
				File file = fileChooser.getSelectedFile();
				if (file.isDirectory())
					return;
				try {
					exportXml(idet.idmp.document, file, (ImDocumentRoot.NORMALIZATION_LEVEL_RAW | ImDocumentRoot.SHOW_TOKENS_AS_WORD_ANNOTATIONS), true);
				}
				catch (IOException ioe) {
					JOptionPane.showMessageDialog(GoldenGateImagineUI.this, ("An error occurred while exporting the document to '" + file.getAbsolutePath() + "':\n" + ioe.getMessage()), "Error Exporting Document", JOptionPane.ERROR_MESSAGE);
					ioe.printStackTrace(System.out);
				}
			}
		});
		menu.add(mi);
		
		mi = new JMenuItem("As GAMTA XML");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				ImDocumentEditorTab idet = getActiveDocument();
				if (idet == null)
					return;
				clearFileFilters(fileChooser);
				fileChooser.addChoosableFileFilter(xmlFileFilter);
				if (fileChooser.showSaveDialog(GoldenGateImagineUI.this) != JFileChooser.APPROVE_OPTION)
					return;
				File file = fileChooser.getSelectedFile();
				if (file.isDirectory())
					return;
				try {
					
					//	make sure file has appropriate extension
					if (!file.getName().toLowerCase().endsWith(".xml"))
						file = new File(file.toString() + ".xml");
					
					//	make way
					if (file.exists()) {
						String fileName = file.toString();
						File oldFile = new File(fileName + "." + System.currentTimeMillis() + ".old");
						file.renameTo(oldFile);
						file = new File(fileName);
					}
					
					//	export document
					ImDocumentRoot doc = new ImDocumentRoot(idet.idmp.document, (ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS | ImDocumentRoot.SHOW_TOKENS_AS_WORD_ANNOTATIONS));
					Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
					GenericGamtaXML.storeDocument(doc, out);
					out.close();
				}
				catch (IOException ioe) {
					JOptionPane.showMessageDialog(GoldenGateImagineUI.this, ("An error occurred while exporting the document to '" + file.getAbsolutePath() + "':\n" + ioe.getMessage()), "Error Exporting Document", JOptionPane.ERROR_MESSAGE);
					ioe.printStackTrace(System.out);
				}
			}
		});
		menu.add(mi);
		
		//	add document exports from configuration
		DocumentSaver[] docSavers = this.ggImagine.getDocumentSavers();
		if (docSavers.length != 0) {
			menu.addSeparator();
			for (int s = 0; s < docSavers.length; s++) {
				final DocumentSaver docSaver = docSavers[s];
				JMenuItem dsmi = docSavers[s].getSaveDocumentMenuItem();
				mi = new JMenuItem(dsmi.getText().replaceAll("Save", "Export"));
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						ImDocumentEditorTab idet = getActiveDocument();
						if (idet == null)
							return;
						try {
							exportDocument(idet.idmp.document, docSaver, idet.docName);
						}
						catch (IOException ioe) {
							JOptionPane.showMessageDialog(GoldenGateImagineUI.this, ("An error occurred while exporting the document via " + ((GoldenGatePlugin) docSaver).getPluginName() + ":\n" + ioe.getMessage()), "Error Exporting Document", JOptionPane.ERROR_MESSAGE);
							ioe.printStackTrace(System.out);
						}
					}
				});
				menu.add(mi);
				
				//	add plugin specific help chapter if available
				HelpChapter docSaverHelp = ((GoldenGatePlugin) docSavers[s]).getHelp();
				if (docSaverHelp != null)
					menuHelp.addSubChapter(docSaverHelp);
			}
		}
		
		//	add dedicated exporters
		ImageDocumentExporter[] ides = this.ggImagine.getDocumentExporters();
		if (ides.length != 0) {
			menu.addSeparator();
			for (int e = 0; e < ides.length; e++) {
				final ImageDocumentExporter ide = ides[e];
				mi = new JMenuItem(ide.getExportMenuLabel());
				mi.setToolTipText(ide.getExportMenuTooltip());
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						ImDocumentEditorTab idet = getActiveDocument();
						if (idet != null)
							exportDocument(idet.idmp.document, ide);
					}
				});
				menu.add(mi);
				
				//	add exporter specific help chapter if available
				HelpChapter ideHelp = ((GoldenGatePlugin) ides[e]).getHelp();
				if (ideHelp != null)
					menuHelp.addSubChapter(ideHelp);
			}
		}
		
		//	finally ...
		this.addMenu(menu);
	}
	
	private void exportDocument(final ImDocument doc, final ImageDocumentExporter ide) {
		
		//	get progress monitor
		final ResourceSplashScreen ss = new ResourceSplashScreen(this, "Exporting Document", "Plaease wait while exporting the document.", true, true);
		
		//	apply document processor, in separate thread
		Thread ideThread = new Thread() {
			public void run() {
				try {
					
					//	wait for splash screen progress monitor to come up (we must not reach the dispose() line before the splash screen even comes up)
					while (!ss.isVisible()) try {
						Thread.sleep(10);
					} catch (InterruptedException ie) {}
					
					//	apply image markup tool
					ide.exportDocument(doc, ss);
				}
				
				//	catch whatever might happen
				catch (Throwable t) {
					t.printStackTrace(System.out);
					JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), ("An error occurred while exporting the document:\n" + t.getMessage()), "Error Exporting Document", JOptionPane.ERROR_MESSAGE);
				}
				
				//	clean up
				finally {
					
					//	dispose splash screen progress monitor
					ss.dispose();
				}
			}
		};
		ideThread.start();
		
		//	open splash screen progress monitor (this waits)
		ss.setVisible(true);
	}
	
	private void exportXml(ImDocument doc, File file, int configFlags, boolean exportIDs) throws IOException {
		
		//	make sure file has appropriate extension
		if (!file.getName().toLowerCase().endsWith(".xml"))
			file = new File(file.toString() + ".xml");
		
		//	make way
		if (file.exists()) {
			String fileName = file.toString();
			File oldFile = new File(fileName + "." + System.currentTimeMillis() + ".old");
			file.renameTo(oldFile);
			file = new File(fileName);
		}
		
		//	export document
		ImDocumentRoot xmlDoc = new ImDocumentRoot(doc, configFlags);
		Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
		AnnotationUtils.writeXML(xmlDoc, out, exportIDs);
		out.flush();
		out.close();
	}
	
	private void exportDocument(ImDocument doc, DocumentSaver docSaver, String docName) throws IOException {
		
		//	obtain document save peration
		DocumentSaveOperation dso = docSaver.getSaveOperation(docName, null);
		if (dso == null)
			return;
		
		//	export file
		ImDocumentRoot xmlDoc = new ImDocumentRoot(doc, ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS);
		xmlDoc.setShowTokensAsWordsAnnotations(true);
		dso.saveDocument(xmlDoc);
	}
	
	private void addEditMenu() {
		HelpChapter menuHelp = new HelpChapterDataProviderBased("Menu 'Edit'", this.helpDataProvider, "GgImagine.EditMenu.html");
		this.helpContent.addSubChapter(menuHelp);
		JMenuItem helpMi = new JMenuItem("Menu 'Edit'");
		helpMi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				showHelp("Menu 'Edit'");
			}
		});
		this.helpMenu.add(helpMi);
		
		JMenu menu = new JMenu("Edit");
		JMenuItem mi;
		
		menu.add(this.allowReactionPrompts);
		
		ImageMarkupToolProvider[] imtps = this.ggImagine.getImageMarkupToolProviders();
		for (int p = 0; p < imtps.length; p++) {
			String[] emImtNames = imtps[p].getEditMenuItemNames();
			if ((emImtNames == null) || (emImtNames.length == 0))
				continue;
			if (menu.getMenuComponentCount() != 0)
				menu.addSeparator();
			for (int n = 0; n < emImtNames.length; n++) {
				final ImageMarkupTool emImt = imtps[p].getImageMarkupTool(emImtNames[n]);
				mi = new JMenuItem(emImt.getLabel());
				mi.setToolTipText(emImt.getTooltip());
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						ImDocumentEditorTab idet = getActiveDocument();
						if (idet != null)
							idet.idmp.applyMarkupTool(emImt, null);
					}
				});
				menu.add(mi);
				
				//	add help chapter if available
				String imtHelpText = emImt.getHelpText();
				menuHelp.addSubChapter(new HelpChapter(emImt.getLabel(), ((imtHelpText == null) ? "Help is coming soon." : imtHelpText)));
			}
		}
		
		if (menu.getMenuComponentCount() != 0)
			this.addMenu(menu);
	}
	
	private void addToolsMenu() {
		HelpChapter menuHelp = new HelpChapterDataProviderBased("Menu 'Tools'", this.helpDataProvider, "GgImagine.ToolsMenu.html");
		this.helpContent.addSubChapter(menuHelp);
		JMenuItem helpMi = new JMenuItem("Menu 'Tools'");
		helpMi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				showHelp("Menu 'Tools'");
			}
		});
		this.helpMenu.add(helpMi);
		helpMi = null; // set to null to mark first entry of custom tool section
		
		JMenu menu = new JMenu("Tools");
		JMenuItem mi;
		
		ImageMarkupToolProvider[] imtps = this.ggImagine.getImageMarkupToolProviders();
		for (int p = 0; p < imtps.length; p++) {
			String[] tmImtNames = imtps[p].getToolsMenuItemNames();
			if ((tmImtNames == null) || (tmImtNames.length == 0))
				continue;
			if (menu.getMenuComponentCount() != 0)
				menu.addSeparator();
			for (int n = 0; n < tmImtNames.length; n++) {
				final ImageMarkupTool tmImt = imtps[p].getImageMarkupTool(tmImtNames[n]);
				mi = new JMenuItem(tmImt.getLabel());
				mi.setToolTipText(tmImt.getTooltip());
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						ImDocumentEditorTab idet = getActiveDocument();
						if (idet != null)
							idet.idmp.applyMarkupTool(tmImt, null);
					}
				});
				menu.add(mi);
				
				//	add help menu entry (with separator before first IMT specific entry)
				if (helpMi == null)
					this.helpMenu.addSeparator();
				helpMi = new JMenuItem(tmImt.getLabel());
				helpMi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						showHelp(tmImt.getLabel());
					}
				});
				this.helpMenu.add(helpMi);
				
				//	add help chapter if available
				String imtHelpText = tmImt.getHelpText();
				menuHelp.addSubChapter(new HelpChapter(tmImt.getLabel(), ((imtHelpText == null) ? "Help is coming soon." : imtHelpText)));
			}
		}
		
		/* TODO
Add "Advanced" menu to GG Imagine
- add respective menu item getter to all GG Imagine Plugins
- use this, for instance, for "Learn Document Profile" option in document structure detector ...
- ... and for "Learn Document Metadata Layout" in document metadata editor
==> finally, create pattern learner for the latter
		 */
		
		if (LOCAL_MASTER_CONFIG_NAME.equals(this.ggImagine.getConfigurationName())) {
			
			//	TODO make Plugins menu available (Analyzer hot reload, etc.) ==> simplifies testing
			
			//	TODO prompt for normalization level before applying DocumentProcessor ==> even more simplifies testing
			
			DocumentProcessorManager[] dpms = this.ggImagine.getDocumentProcessorProviders();
			if ((dpms.length != 0 ) && (menu.getMenuComponentCount() != 0))
				menu.addSeparator();
			for (int m = 0; m < dpms.length; m++) {
				final DocumentProcessorManager dpm = dpms[m];
				final String toolsMenuLabel = dpm.getToolsMenuLabel();
				if (toolsMenuLabel == null)
					continue;
				mi = new JMenuItem(toolsMenuLabel + " " + dpm.getResourceTypeLabel());
				mi.setToolTipText(toolsMenuLabel + " a " + dpm.getResourceTypeLabel() + " " + ("Run".equals(toolsMenuLabel) ? "on" : "to") + " the document");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						ImDocumentEditorTab idet = getActiveDocument();
						if (idet == null)
							return;
						ResourceDialog rd = ResourceDialog.getResourceDialog(dpm, ("Select " + dpm.getResourceTypeLabel() + " To " + toolsMenuLabel), toolsMenuLabel);
						rd.setVisible(true);
						final String dpName = rd.getSelectedResourceName();
						if (dpName == null)
							return;
						idet.idmp.applyMarkupTool(new ImageMarkupTool() {
							public String getLabel() {
								return (dpm.getResourceTypeLabel() + " '" + dpName + "'");
							}
							public String getTooltip() {
								return null; // no need for a tooltip here
							}
							public String getHelpText() {
								return null; // no need for a help text here
							}
							public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
								
								//	wrap document (or annotation)
								if (pm != null)
									pm.setStep("Wrapping document");
								ImDocumentRoot wrappedDoc = new ImDocumentRoot(doc, ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS);
								
								//	get document processor from manager
								if (pm != null)
									pm.setStep("Loading document processor");
								DocumentProcessor dp = dpm.getDocumentProcessor(dpName);
								
								//	create parameters
								Properties parameters = new Properties();
								parameters.setProperty(DocumentProcessor.INTERACTIVE_PARAMETER, DocumentProcessor.INTERACTIVE_PARAMETER);
								
								//	process document (or annotation)
								if (pm != null)
									pm.setStep("Processing document");
								dp.process(wrappedDoc, parameters);
							}
						}, null);
					}
				});
				menu.add(mi);
//				
//				//	add help chapter if available SKIP THOSE, TOO GENERIC (MOSTLY ADMIN DOCUMENTATION)
//				HelpChapter dpmHelp = dpms[m].getHelp();
//				if (dpmHelp != null)
//					menuHelp.addSubChapter(dpmHelp);
			}
			
		}
		
		if (menu.getMenuComponentCount() != 0)
			this.addMenu(menu);
	}
	
	private HelpChapter buildHelpContentRoot() {
		HelpChapter helpRoot = new HelpChapterDataProviderBased("GoldenGATE Imagine", this.helpDataProvider, "GgImagine.html");
		helpRoot.addSubChapter(new HelpChapterDataProviderBased("Glossary", this.helpDataProvider, "GgImagine.Glossary.html"));
		
		HelpChapter editorHelp = new HelpChapterDataProviderBased("Editor", this.helpDataProvider, "GgImagine.Editor.html");
		helpRoot.addSubChapter(editorHelp);
		SelectionActionProvider[] saps = this.ggImagine.getSelectionActionProviders();
		for (int p = 0; p < saps.length; p++) {
			HelpChapter sapHelp = saps[p].getHelp();
			if (sapHelp != null)
				editorHelp.addSubChapter(sapHelp);
		}
		
		ImageDocumentDropHandler[] dropHandlers = this.ggImagine.getDropHandlers();
		if (dropHandlers.length != 0) {
			HelpChapter dragDropHelp = new HelpChapterDataProviderBased("Drag & Drop", this.helpDataProvider, "GgImagine.DragDrop.html");
			helpRoot.addSubChapter(dragDropHelp);
			for (int h = 0; h < dropHandlers.length; h++) {
				HelpChapter dhHelp = dropHandlers[h].getHelp();
				if (dhHelp != null)
					dragDropHelp.addSubChapter(dhHelp);
			}
		}
		
		HelpChapter pageImageHelp = new HelpChapterDataProviderBased("Page Image Editing", this.helpDataProvider, "GgImagine.PageImageEditing.html");
		helpRoot.addSubChapter(pageImageHelp);
		ImageEditToolProvider[] ietps = this.ggImagine.getImageEditToolProviders();
		for (int p = 0; p < ietps.length; p++) {
			HelpChapter ietpHelp = ietps[p].getHelp();
			if (ietpHelp != null)
				pageImageHelp.addSubChapter(ietpHelp);
		}
		
		return helpRoot;
	}
	
	private JMenu createHelpMenu() {
		JMenu helpMenu = new JMenu("Help");
		
		JMenuItem mi = new JMenuItem("Help");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				showHelp(null);
			}
		});
		helpMenu.add(mi);
		
		helpMenu.addSeparator();
		
		return helpMenu;
	}
	
	private void finishHelpMenu() {
		this.helpMenu.addSeparator();
		
		JMenuItem ami = new JMenuItem("About");
		ami.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				ggImagine.showAbout();
			}
		});
		this.helpMenu.add(ami);
		
		JMenuItem rmi = new JMenuItem("View Readme");
		rmi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				ggImagine.showReadme();
			}
		});
		this.helpMenu.add(rmi);
		
		this.addMenu(this.helpMenu);
	}
	
	/**
	 * Show some help information.
	 * @param on the subject of the desired help information
	 */
	public void showHelp(String on) {
		if (this.help != null)
			this.help.showHelp(on);
	}
	
	private class ViewControl extends JPanel {
		private JLabel scrollPosition = new JLabel("Page 0 of 0", JLabel.CENTER);
		private int pageImageDpi = ImDocumentMarkupPanel.DEFAULT_RENDERING_DPI;
		private JComboBox zoomSelector = new JComboBox();
		private JComboBox layoutSelector = new JComboBox();
		ViewControl() {
			super(new GridLayout(1, 0), true);
			
			this.scrollPosition.setOpaque(true);
			this.scrollPosition.setBackground(Color.WHITE);
			this.scrollPosition.setBorder(BorderFactory.createLoweredBevelBorder());
			
			this.zoomSelector.addItem(new ZoomLevel(ImDocumentMarkupPanel.DEFAULT_RENDERING_DPI / 4));
			this.zoomSelector.addItem(new ZoomLevel(ImDocumentMarkupPanel.DEFAULT_RENDERING_DPI / 3));
			this.zoomSelector.addItem(new ZoomLevel(ImDocumentMarkupPanel.DEFAULT_RENDERING_DPI / 2));
			this.zoomSelector.addItem(new ZoomLevel((ImDocumentMarkupPanel.DEFAULT_RENDERING_DPI * 2) / 3));
			this.zoomSelector.addItem(new ZoomLevel(ImDocumentMarkupPanel.DEFAULT_RENDERING_DPI));
			this.zoomSelector.addItem(new ZoomLevel((ImDocumentMarkupPanel.DEFAULT_RENDERING_DPI * 3) / 2));
			this.zoomSelector.addItem(new ZoomLevel(ImDocumentMarkupPanel.DEFAULT_RENDERING_DPI * 2));
			this.zoomSelector.addItem(new ZoomLevel(ImDocumentMarkupPanel.DEFAULT_RENDERING_DPI * 3));
			this.zoomSelector.addItem(new ZoomLevel(ImDocumentMarkupPanel.DEFAULT_RENDERING_DPI * 4));
			this.zoomSelector.addItem(new ZoomLevel(0));
			this.zoomSelector.setSelectedItem(new ZoomLevel(ImDocumentMarkupPanel.DEFAULT_RENDERING_DPI));
			this.zoomSelector.setEditable(false);
			this.zoomSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					zoomChanged();
				}
			});
			
			this.layoutSelector.addItem("Pages Top-Down");
			this.layoutSelector.addItem("Pages Left-Right");
			this.layoutSelector.setSelectedItem("Pages Top-Down");
			this.layoutSelector.setEditable(false);
			this.layoutSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					layoutChanged();
				}
			});
			
			this.add(this.scrollPosition);
			this.add(this.zoomSelector);
			this.add(this.layoutSelector);
		}
		
		void zoomIn() {
			int szi = this.zoomSelector.getSelectedIndex();
			if ((szi + 2) < this.zoomSelector.getItemCount()) // we have to block 'Original Resolution' at end of list
				this.zoomSelector.setSelectedIndex(szi + 1);
		}
		void zoomOut() {
			int szi = this.zoomSelector.getSelectedIndex();
			if (szi > 0)
				this.zoomSelector.setSelectedIndex(szi - 1);
		}
		void zoomChanged() {
			if (this.inUpdate)
				return;
			ImDocumentEditorTab idet = getActiveDocument();
			if (idet == null)
				return;
			ZoomLevel zl = ((ZoomLevel) this.zoomSelector.getSelectedItem());
			if (zl.dpi == 0)
				idet.setRenderingDpi(this.pageImageDpi);
			else idet.setRenderingDpi(zl.dpi);
			KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
		}
		int getRenderingDpi() {
			return ((ZoomLevel) this.zoomSelector.getSelectedItem()).dpi;
		}
		
		void setSideBySidePages(int sbsp) {
			this.layoutSelector.setSelectedItem((sbsp == 1) ? "Pages Top-Down" : "Pages Left-Right");
		}
		void layoutChanged() {
			if (this.inUpdate)
				return;
			ImDocumentEditorTab idet = getActiveDocument();
			if (idet == null)
				return;
			idet.setSideBySidePages("Pages Top-Down".equals(this.layoutSelector.getSelectedItem()) ? 1 : 0);
			KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
		}
		boolean isLeftRightLayout() {
			return "Pages Left-Right".equals(this.layoutSelector.getSelectedItem());
		}
		
		private boolean inUpdate = false;
		void update(ImDocumentEditorTab idet) {
			this.inUpdate = true;
			idet.updateScrollPosition();
			this.pageImageDpi = idet.idmp.getMaxPageImageDpi();
			this.zoomSelector.setSelectedItem(new ZoomLevel(idet.idmp.getRenderingDpi()));
			this.layoutSelector.setSelectedItem((idet.idmp.getSideBySidePages() == 1) ? "Pages Top-Down" : "Pages Left-Right");
			this.inUpdate = false;
		}
		
		private class ZoomLevel {
			final int dpi;
			ZoomLevel(int dpi) {
				this.dpi = dpi;
			}
			public String toString() {
				if (this.dpi == 0)
					return "Original Resolution";
				else return (((this.dpi * 100) / ImDocumentMarkupPanel.DEFAULT_RENDERING_DPI) + "%");
			}
			public boolean equals(Object obj) {
				return ((obj instanceof ZoomLevel) && (((ZoomLevel) obj).dpi == this.dpi));
			}
		}
	}
	
	void addMenu(JMenu menu) {
		this.mainMenu.add(menu);
	}
	
	void updateViewControl(ImDocumentEditorTab idet) {
		this.viewControl.update(idet);
	}
	
	void updateUndoMenu(final LinkedList undoActions) {
		this.undoMenu.removeAll();
		for (Iterator uait = undoActions.iterator(); uait.hasNext();) {
			final UndoAction ua = ((UndoAction) uait.next());
			JMenuItem mi = new JMenuItem(ua.label);
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					while (undoActions.size() != 0) {
						UndoAction eua = ((UndoAction) undoActions.removeFirst());
						eua.execute();
						if (eua == ua)
							break;
					}
					updateUndoMenu(undoActions);
					ua.target.idmp.validate();
					ua.target.idmp.repaint();
					ua.target.idmp.validateControlPanel();
				}
			});
			this.undoMenu.add(mi);
			if (this.undoMenu.getMenuComponentCount() >= 10)
				break;
		}
		this.undoMenu.setEnabled(undoActions.size() != 0);
	}
	
	void loadDocument(final String docName, final File docSource, final String docSourceUrl, final FileFilter fileFilter, final InputStream in, final int inLength) throws IOException {
		if (docSource != null)
			this.config.setSetting("lastDocFolder", docSource.getParentFile().getAbsolutePath());
		
		//	load IMF
		if (docName.toLowerCase().endsWith(".imf")) {
			final IOException[] loadException = {null};
			final ResourceSplashScreen loadScreen = new ResourceSplashScreen(this, ("Loading IMF Archive '" + docName + "'"), "", true, false);
			System.out.println("Creating load thread");
			Thread loadThread = new Thread("LoaderThread") {
				public void run() {
					try {
						loadScreen.setStep("Loading IMF Archive");
						ImDocument doc = ImfIO.loadDocument(in, loadScreen, inLength);
						doc.setAttribute(DOCUMENT_NAME_ATTRIBUTE, docName);
						if ((docSourceUrl != null) && !doc.hasAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE))
							doc.setAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE, docSourceUrl);
						ggImagine.notifyDocumentOpened(doc);
						addDocument(new ImDocumentEditorTab(GoldenGateImagineUI.this, doc, docName, docSource));
						in.close();
					}
					catch (IOException ioe) {
						loadException[0] = ioe;
					}
					finally {
						loadScreen.dispose();
					}
				}
			};
			loadThread.start();
			loadScreen.setVisible(true);
			if (loadException[0] == null)
				return;
			else throw loadException[0];
		}
		
		//	load PDF
		if (docName.toLowerCase().endsWith(".pdf")) {
			final IOException[] loadException = {null};
			final ResourceSplashScreen loadScreen = new ResourceSplashScreen(this, ("Loading PDF '" + docName + "'"), "", true, false);
			Thread loadThread = new Thread("LoaderThread") {
				public void run() {
					try {
						loadScreen.setStep("Loading PDF Document");
						ImDocument doc;
						
						//	load in slave JVM
						if ("slave".equals(config.getSetting("pdfDecodeMode", "main"))) {
							
							String pdfConverterCommand = "java -jar -Dorg.icepdf.core.streamcache.enabled=false ImageMarkupPDF.jar -l M -o O";
							
							if (docSource != null)
								pdfConverterCommand += (" -s \"" + docSource.getAbsolutePath() + "\"");
							
							if (fileFilter == textPdfFileFilter)
								pdfConverterCommand += " -t D";
							else if (fileFilter == imagePdfFileFilter)
								pdfConverterCommand += " -t S";
							else if (fileFilter == imageAndMetaPdfFileFilter)
								pdfConverterCommand += " -t M";
							
							System.out.println("PDF converter command is " + pdfConverterCommand);
							Process pdfConverter = Runtime.getRuntime().exec(pdfConverterCommand, new String[0], new File("."));
							System.out.println("PDF converter started");
							
							if (docSource == null) {
								OutputStream toConverter = new BufferedOutputStream(pdfConverter.getOutputStream());
								byte[] buffer = new byte[1024];
								int read;
								while ((read = in.read(buffer, 0, buffer.length)) != -1)
									toConverter.write(buffer, 0, read);
								in.close();
								toConverter.flush();
								toConverter.close();
								System.out.println("Buffered PDF data sent");
							}
							
							final InputStream fromConverterError = new BufferedInputStream(pdfConverter.getErrorStream());
							new Thread() {
								public void run() {
									try {
										for (int b; (b = fromConverterError.read()) != -1;)
											System.err.print((char) b);
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}.start();
							LineInputStream fromConverter = new LineInputStream(new BufferedInputStream(pdfConverter.getInputStream()));
							System.out.println("Start receiving converter response");
							for (byte[] line; (line = fromConverter.readLine()) != null;) {
								if (line.length < 3)
									continue;
								if ((line[0] == 'S') && (line[1] == ':'))
									loadScreen.setStep(new String(line, "S:".length(), (line.length - "S:".length())).trim());
								else if ((line[0] == 'I') && (line[1] == ':'))
									loadScreen.setInfo(new String(line, "I:".length(), (line.length - "I:".length())).trim());
								else if ((line[0] == 'P') && (line[1] == ':'))
									loadScreen.setProgress(Integer.parseInt(new String(line, "P:".length(), (line.length - "P:".length())).trim()));
								else if ((line[0] == 'B') && (line[1] == 'P') && (line[2] == ':'))
									loadScreen.setBaseProgress(Integer.parseInt(new String(line, "BP:".length(), (line.length - "BP:".length())).trim()));
								else if ((line[0] == 'M') && (line[1] == 'P') && (line[2] == ':'))
									loadScreen.setMaxProgress(Integer.parseInt(new String(line, "MP:".length(), (line.length - "MP:".length())).trim()));
								else break;
							}
							
							System.out.println("Converter process response finished, reading document");
							doc = ImfIO.loadDocument(fromConverter, loadScreen);
							fromConverter.close();
						}
						
						//	load in local JVM
						else {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							byte[] buffer = new byte[1024];
							int read;
							while ((read = in.read(buffer, 0, buffer.length)) != -1)
								baos.write(buffer, 0, read);
							in.close();
							
							if (fileFilter == textPdfFileFilter)
								doc = pdfExtractor.loadTextPdf(baos.toByteArray(), loadScreen);
							else if (fileFilter == imagePdfFileFilter)
								doc = pdfExtractor.loadImagePdf(baos.toByteArray(), loadScreen);
							else if (fileFilter == imageAndMetaPdfFileFilter)
								doc = pdfExtractor.loadImagePdf(baos.toByteArray(), true, loadScreen);
							else doc = pdfExtractor.loadGenericPdf(baos.toByteArray(), loadScreen);
						}
						
						//	add document
						doc.setAttribute(DOCUMENT_NAME_ATTRIBUTE, docName);
						if ((docSourceUrl != null) && !doc.hasAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE))
							doc.setAttribute(DOCUMENT_SOURCE_LINK_ATTRIBUTE, docSourceUrl);
						ggImagine.notifyDocumentOpened(doc);
						addDocument(new ImDocumentEditorTab(GoldenGateImagineUI.this, doc, docName, docSource));
					}
					catch (IOException ioe) {
						loadException[0] = ioe;
					}
					finally {
						loadScreen.dispose();
					}
				}
			};
			loadThread.start();
			loadScreen.setVisible(true);
			if (loadException[0] == null)
				return;
			else throw loadException[0];
		}
	}
	
	private static class LineInputStream extends PeekInputStream {
		LineInputStream(InputStream in) throws IOException {
			super(in, 2048);
		}
		//	returns a line of bytes, INCLUDING its terminal line break bytes
		byte[] readLine() throws IOException {
			if (this.peek() == -1)
				return null;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			for (int b; (b = this.peek()) != -1;) {
				baos.write(this.read());
				if (b == '\r') {
					if (this.peek() == '\n')
						baos.write(this.read());
					break;
				}
				else if (b == '\n')
					break;
			}
			return baos.toByteArray();
		}
	}
	
	void addDocument(ImDocumentEditorTab idet) {
		this.docTabs.addTab(idet.docName, idet);
		this.docTabs.setSelectedComponent(idet);
	}
	
	void removeDocument(ImDocumentEditorTab idet) {
		this.docTabs.remove(idet);
	}
	
	ImDocumentEditorTab getActiveDocument() {
		return ((this.docTabs.getComponentCount() == 0) ? null : ((ImDocumentEditorTab) this.docTabs.getSelectedComponent()));
	}
	
	boolean closeDocument() {
		ImDocumentEditorTab idet = getActiveDocument();
		if (idet == null)
			return true;
		if (idet.isDirty()) {
			int choice = JOptionPane.showConfirmDialog(GoldenGateImagineUI.this, ("Document '" + idet.docName + "' has un-saved changes. Save them before closing it?"), "Save Changes?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if (choice == JOptionPane.CANCEL_OPTION)
				return false;
			if (choice == JOptionPane.YES_OPTION) {
				if (!idet.save() && !idet.saveAs(this.fileChooser))
					return false;
			}
		}
		idet.close();
		return true;
	}
	
	void exit() {
		while (this.getActiveDocument() != null) {
			if (!this.closeDocument())
				return;
		}
		this.ggImagine.exit();
		this.config.setSetting("mostRecentlyUsedSymbols", SymbolTable.getMostRecentlyUsedSymbols());
		this.saUsageStats.storeTo(this.config.getSubset("selectionAction"));
		this.dispose();
	}
	
	private static abstract class UndoAction {
		final String label;
		final ImDocumentEditorTab target;
		final int modCount;
		UndoAction(String label, ImDocumentEditorTab target) {
			this.label = label;
			this.target = target;
			this.modCount = this.target.modCount;
		}
		final void execute() {
			try {
				this.target.inUndoAction = true;
				this.doExecute();
				this.target.modCount = this.modCount;
			}
			finally {
				this.target.inUndoAction = false;
			}
		}
		abstract void doExecute();
	}
	
	private static class MultipartUndoAction extends UndoAction {
		LinkedList parts = new LinkedList();
		MultipartUndoAction(String label, ImDocumentEditorTab target) {
			super(label, target);
		}
		synchronized void addUndoAction(UndoAction ua) {
			this.parts.addFirst(ua);
		}
		void doExecute() {
			while (this.parts.size() != 0)
				((UndoAction) this.parts.removeFirst()).doExecute();
		}
	}
	
	/* TODO add page navigator to editor tabs
	 * - represent pages a thumbnails
	 * - pages stacked vertical --> navigator on left edge, scrolling top down
	 * - pages side-by-side --> navigator at bottom, scrolling left right
	 * - click on page thumbnail --> scroll directly to that page
	 * 
	 * - when document navigator visible, scroll along with main editor window ...
	 * - ... but let document navigator scroll by itself without scrolling main editor window (obviously ...)
	 *
	 * - when page numbers are added to or removed from pages, update page thumbnails in document navigator
	 */
	
	/* TODO
	 * To allow users a look at some page even when in a sub dialog
	 * - offer showPage() method in ImDocumentMarkupPanel (doing nothing by default) ...
	 * - ... and overwrite that in GgImagineUI to actually scroll to that page
	 * - JavaDoc: "if an instance of this class sits inside a JScrollPane, this method can be overwritten to trigger a scroll" 
	 */
	
	private static class ImDocumentEditorTab extends JPanel {
		GoldenGateImagineUI parent;
		
		String docName;
		File docSource;
		
		ImDocumentMarkupPanel idmp;
		JScrollPane idmpBox;
		Rectangle idmpViewSize;
		
		boolean idmpBoxInFastScroll = false;
		
		ImDocumentListener undoRecorder;
		LinkedList undoActions = new LinkedList();
		MultipartUndoAction multipartUndoAction = null;
		boolean inUndoAction = false;
		
		ImDocumentListener reactionTrigger = null;
		boolean imToolActive = false;
		HashSet inReactionObjects = new HashSet();
		
		int modCount = 0;
		int savedModCount = 0;
		
		ImDocumentEditorTab(GoldenGateImagineUI parent, ImDocument doc, String docName, File docSource) {
			super(new BorderLayout(), true);
			this.parent = parent;
			this.docName = docName;
			this.docSource = docSource;
			
			this.idmp = new ImDocumentEditorPanel(doc);
			
			//	inject highlight colors for annotations, regions, and text streams
			Settings annotationColors = this.parent.config.getSubset("annotation.color");
			String[] annotationTypes = annotationColors.getKeys();
			for (int t = 0; t < annotationTypes.length; t++) {
				Color ac = GoldenGateImagine.getColor(annotationColors.getSetting(annotationTypes[t]));
				if (ac != null)
					this.idmp.setAnnotationColor(annotationTypes[t], ac);
			}
			Settings layoutObjectColors = this.parent.config.getSubset("layoutObject.color");
			String[] layoutObjectTypes = layoutObjectColors.getKeys();
			for (int t = 0; t < layoutObjectTypes.length; t++) {
				Color loc = GoldenGateImagine.getColor(layoutObjectColors.getSetting(layoutObjectTypes[t]));
				if (loc != null)
					this.idmp.setLayoutObjectColor(layoutObjectTypes[t], loc);
			}
			Settings textStreamColors = this.parent.config.getSubset("textStream.color");
			String[] textStreamTypes = textStreamColors.getKeys();
			for (int t = 0; t < textStreamTypes.length; t++) {
				Color tsc = GoldenGateImagine.getColor(textStreamColors.getSetting(textStreamTypes[t]));
				if (tsc != null)
					this.idmp.setTextStreamTypeColor(textStreamTypes[t], tsc);
			}
			
			//	TODO also use this for REDO logging
			//	prepare recording UNDO actions
			this.undoRecorder = new UndoRecorder();
			this.idmp.document.addDocumentListener(this.undoRecorder);
			
			//	get reaction providers
			ReactionProvider[] reactionProviders = this.parent.ggImagine.getReactionProviders();
			if (reactionProviders.length != 0) {
				this.reactionTrigger = new ReactionTrigger(reactionProviders);
				this.idmp.document.addDocumentListener(this.reactionTrigger);
			}
			
			//	get drop handlers
			final ImageDocumentDropHandler[] dropHandlers = this.parent.ggImagine.getDropHandlers();
			
			//	add drop target if any drop handlers present
			if (dropHandlers.length != 0) {
				DropTarget dropTarget = new DropTarget(this.idmp, new DropTargetAdapter() {
					public void drop(DropTargetDropEvent dtde) {
						dtde.acceptDrop(dtde.getDropAction()); // we can do this only once, and we have to do it before inspecting data
						if (!this.handleDrop(dtde))
							ImDocumentEditorTab.this.parent.handleDrop(dtde.getTransferable());
					}
					private boolean handleDrop(DropTargetDropEvent dtde) {
						PagePoint dpp = idmp.pagePointAt(dtde.getLocation().x, dtde.getLocation().y);
						if (dpp == null)
							return false;
						for (int h = 0; h < dropHandlers.length; h++) try {
							if (dropHandlers[h].handleDrop(idmp, dpp.page, dpp.x, dpp.y, dtde))
								return true;
						}
						catch (Exception e) {
							e.printStackTrace(System.out);
						}
						return false;
					}
				});
				dropTarget.setActive(true);
			}
			
			//	set document view to current configuration
			int renderingDpi = this.parent.viewControl.getRenderingDpi();
			if ((0 < renderingDpi) && (renderingDpi != ImDocumentMarkupPanel.DEFAULT_RENDERING_DPI))
				this.idmp.setRenderingDpi(renderingDpi);
			if (this.parent.viewControl.isLeftRightLayout())
				this.idmp.setSideBySidePages(0);
			
			//	make document view scrollable
			this.idmpBox = new JScrollPane();
			this.idmpBox.setViewport(new IdmpViewport(this.idmp));
			this.idmpViewSize = this.idmpBox.getVisibleRect();
			
			//	set scroll distances
			final JScrollBar vsb = this.idmpBox.getVerticalScrollBar();
			vsb.setUnitIncrement(this.idmpViewSize.height / 10);
			vsb.setBlockIncrement(this.idmpViewSize.height / 10);
			final JScrollBar hsb = this.idmpBox.getHorizontalScrollBar();
			hsb.setUnitIncrement(this.idmpViewSize.width / 10);
			hsb.setBlockIncrement(this.idmpViewSize.width / 10);
			
			//	track window resizing
			this.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent ce) {
					idmpViewSize = idmpBox.getViewport().getViewRect();
					vsb.setUnitIncrement(idmpViewSize.height / 10);
					vsb.setBlockIncrement(idmpViewSize.height / 10);
					hsb.setUnitIncrement(idmpViewSize.width / 10);
					hsb.setBlockIncrement(idmpViewSize.width / 10);
				}
			});
			
			//	make scroll tractable, and enable fast scrolling (disables page rendering when scrolling at high speed)
			vsb.addMouseListener(new MouseAdapter() {
				public void mouseReleased(MouseEvent me) {
					setIdmpBoxFastScroll(false);
				}
			});
			vsb.addAdjustmentListener(new AdjustmentListener() {
				private AdjustmentEvent lastAe = null;
				private long lastAeTime = -1;
				public void adjustmentValueChanged(AdjustmentEvent ae) {
					if (idmp.getSideBySidePages() != 1) {
						this.lastAe = null;
						this.lastAeTime = -1;
						return;
					}
					updateScrollPosition();
					long aeTime = System.currentTimeMillis();
					//	valueIsAdjusting is only true if mouse button held down in scrollbar _outside_ the buttons at the ends
					if (ae.getValueIsAdjusting()) {
						float valueDelta = ((this.lastAe == null) ? ae.getValue() : (ae.getValue() - this.lastAe.getValue()));
						int timeDelta = ((this.lastAe == null) ? 10 : Math.max(10, ((int) (aeTime - this.lastAeTime))));
						setIdmpBoxFastScroll(Math.abs(valueDelta / timeDelta) > Math.max(1, (idmpViewSize.height / 100)));
					}
					else setIdmpBoxFastScroll(false);
					this.lastAe = ae;
					this.lastAeTime = aeTime;
				}
			});
			hsb.addMouseListener(new MouseAdapter() {
				public void mouseReleased(MouseEvent me) {
					setIdmpBoxFastScroll(false);
				}
			});
			hsb.addAdjustmentListener(new AdjustmentListener() {
				private AdjustmentEvent lastAe = null;
				private long lastAeTime = -1;
				public void adjustmentValueChanged(AdjustmentEvent ae) {
					if (idmp.getSideBySidePages() == 1) {
						this.lastAe = null;
						this.lastAeTime = -1;
						return;
					}
					updateScrollPosition();
					long aeTime = System.currentTimeMillis();
					//	valueIsAdjusting is only true if mouse button held down in scrollbar _outside_ the buttons at the ends
					if (ae.getValueIsAdjusting()) {
						float valueDelta = ((this.lastAe == null) ? ae.getValue() : (ae.getValue() - this.lastAe.getValue()));
						int timeDelta = ((this.lastAe == null) ? 10 : Math.max(10, ((int) (aeTime - this.lastAeTime))));
						setIdmpBoxFastScroll(Math.abs(valueDelta / timeDelta) > Math.max(1, (idmpViewSize.width / 100)));
					}
					else setIdmpBoxFastScroll(false);
					this.lastAe = ae;
					this.lastAeTime = aeTime;
				}
			});
			
			//	assemble UI components
			this.add(this.idmpBox, BorderLayout.CENTER);
			this.add(this.idmp.getControlPanel(), BorderLayout.EAST);
		}
		
		void updateScrollPosition() {
			Rectangle viewRect = this.idmpBox.getViewport().getViewRect();
			int viewCenterX = ((int) (viewRect.getMinX() + (viewRect.getWidth() / 2)));
			int viewCenterY = ((int) (viewRect.getMinY() + (viewRect.getHeight() / 2)));
			PagePoint viewPagePoint = this.idmp.pagePointAt(viewCenterX, viewCenterY);
			if (viewPagePoint != null) {
				Object pageNumber = viewPagePoint.page.getAttribute(PAGE_NUMBER_ATTRIBUTE);
				this.parent.viewControl.scrollPosition.setText("Page " + (viewPagePoint.page.pageId + 1) + " / " + this.idmp.document.getPageCount() + ((pageNumber == null) ? "" : (" (Nr. " + pageNumber + ")")));
			}
		}
		
		void setIdmpBoxFastScroll(boolean ibfs) {
			if (this.idmpBoxInFastScroll == ibfs)
				return;
			if (ibfs) {
//				System.out.println("Entering fast scroll mode");
				this.idmpBoxInFastScroll = true;
			}
			else {
//				System.out.println("Quitting fast scroll mode");
				this.idmpBoxInFastScroll = false;
				this.idmp.validate();
				this.idmp.repaint();
			}
		}
		
		private class ImDocumentEditorPanel extends ImDocumentMarkupPanel {
			ImDocumentEditorPanel(ImDocument document) {
				super(document);
			}
			public void beginAtomicAction(String label) {
				startMultipartUndoAction(label);
			}
			public void endAtomicAction() {
				finishMultipartUndoAction();
			}
			protected SelectionAction[] getActions(ImWord start, ImWord end) {
				LinkedList actions = new LinkedList(Arrays.asList(super.getActions(start, end)));
				SelectionActionProvider[] saps = ImDocumentEditorTab.this.parent.ggImagine.getSelectionActionProviders();
				for (int p = 0; p < saps.length; p++) {
					SelectionAction[] sas = saps[p].getActions(start, end, this);
					if ((sas != null) && (sas.length != 0)) {
						if (actions.size() != 0)
							actions.add(SelectionAction.SEPARATOR);
						actions.addAll(Arrays.asList(sas));
					}
				}
				return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
			}
			protected SelectionAction[] getActions(ImPage page, Point start, Point end) {
				LinkedList actions = new LinkedList(Arrays.asList(super.getActions(page, start, end)));
				SelectionActionProvider[] saps = ImDocumentEditorTab.this.parent.ggImagine.getSelectionActionProviders();
				for (int p = 0; p < saps.length; p++) {
					SelectionAction[] sas = saps[p].getActions(start, end, page, this);
					if ((sas != null) && (sas.length != 0)) {
						if (actions.size() != 0)
							actions.add(SelectionAction.SEPARATOR);
						actions.addAll(Arrays.asList(sas));
					}
				}
				return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
			}
			protected boolean[] markAdvancedSelectionActions(SelectionAction[] sas) {
				return ImDocumentEditorTab.this.parent.saUsageStats.markAdvancedSelectionActions(sas);
			}
			protected void selectionActionPerformed(SelectionAction sa) {
				ImDocumentEditorTab.this.parent.saUsageStats.selectionActionUsed(sa);
			}
			protected ImImageEditTool[] getImageEditTools() {
				LinkedList tools = new LinkedList(Arrays.asList(super.getImageEditTools()));
				ImageEditToolProvider[] ietps = ImDocumentEditorTab.this.parent.ggImagine.getImageEditToolProviders();
				for (int p = 0; p < ietps.length; p++) {
					ImImageEditTool[] iets = ietps[p].getImageEditTools();
					if (iets != null)
						tools.addAll(Arrays.asList(iets));
				}
				return ((ImImageEditTool[]) tools.toArray(new ImImageEditTool[tools.size()]));
			}
			public ProgressMonitor getProgressMonitor(String title, String text, boolean supportPauseResume, boolean supportAbort) {
				return new ResourceSplashScreen(ImDocumentEditorTab.this.parent, title, text, supportPauseResume, supportAbort);
			}
			public boolean setWordSelection(ImWord startWord, ImWord endWord) {
				if (!super.setWordSelection(startWord, endWord))
					return false;
				
				//	get position of word selection, and compare to current view
				Rectangle vpPos = idmpBox.getViewport().getViewRect();
				Rectangle swPos = this.getPosition(startWord);
				if (swPos == null)
					return true;
				Rectangle wsPos;
				if ((endWord == null) || (endWord == startWord))
					wsPos = swPos;
				else {
					Rectangle ewPos = this.getPosition(endWord);
					
					//	word selection doesn't fit view vertically, use start word
					if (vpPos.height < (ewPos.y + ewPos.height - swPos.y))
						wsPos = swPos;
					
					//	word selection doesn't fit view horizontally, use start word
					else if (vpPos.width < (Math.max((swPos.x + swPos.width), (ewPos.x + ewPos.width)) - Math.min(swPos.x, ewPos.x)))
						wsPos = swPos;
					
					//	word selection fits view
					else wsPos = swPos.union(ewPos);
				}
				
				//	scroll selection to view if required (moving near center)
				if (!vpPos.contains(wsPos)) {
//					idmpBox.getViewport().scrollRectToVisible(wsPos); // DOESN'T SEEM TO WORK AS SUPPOSED TO, FOR WHATEVER REASON
					int vx;
					if ((vpPos.x <= wsPos.x) && ((vpPos.x + vpPos.width) >= (wsPos.x + wsPos.width))) // selection in bounds horizontally, no need for scrolling
						vx = vpPos.x;
					else /* center selection in viewport */ {
						int wscx = (wsPos.x + (wsPos.width / 2));
						vx = (wscx - (vpPos.width / 2));
						if (vpPos.x < vx) // scrolling right, don't go all that far
							vx -= (vpPos.width / 4);
						else if (vpPos.x > vx) // scrolling up, don't go all that far
							vx += (vpPos.width / 4);
						if (vx < 0)
							vx = 0;
					}
					int vy;
					if ((vpPos.y <= wsPos.y) && ((vpPos.y + vpPos.height) >= (wsPos.y + wsPos.height))) // selection in bounds horizontally, no need for scrolling
						vy = vpPos.y;
					else /* center selection in viewport */ {
						int wscy = (wsPos.y + (wsPos.height / 2));
						vy = (wscy - (vpPos.height / 2));
						if (vpPos.y < vy) // scrolling down, don't go all that far
							vy -= (vpPos.height / 4);
						else if (vpPos.y > vy) // scrolling up, don't go all that far
							vy += (vpPos.height / 4);
						if (vy < 0)
							vy = 0;
					}
					idmpBox.getViewport().setViewPosition(new Point(vx, vy));
				}
				
				//	pass on super class success
				return true;
			}
			public void setPageVisible(int pageId, boolean pv) {
				if (pv == this.isPageVisible(pageId))
					return;
				super.setPageVisible(pageId, pv);
				ImDocumentEditorTab.this.validate();
				ImDocumentEditorTab.this.repaint();
			}
			public void setPagesVisible(int fromPageId, int toPageId, boolean pv) {
				boolean pageVisibilityUnchanged = true;
				for (int p = fromPageId; p <= toPageId; p++)
					if (pv != this.isPageVisible(p)) {
						pageVisibilityUnchanged = false;
						break;
					}
				if (pageVisibilityUnchanged)
					return;
				super.setPagesVisible(fromPageId, toPageId, pv);
				ImDocumentEditorTab.this.validate();
				ImDocumentEditorTab.this.repaint();
			}
			public void setVisiblePages(int[] visiblePageIDs) {
				boolean pageVisibilityUnchanged = true;
				HashSet visiblePageIdSet = new HashSet();
				for (int i = 0; i < visiblePageIDs.length; i++)
					visiblePageIdSet.add(new Integer(visiblePageIDs[i]));
				for (int p = 0; p < this.document.getPageCount(); p++)
					if (this.isPageVisible(p) != visiblePageIdSet.contains(new Integer(p))) {
						pageVisibilityUnchanged = false;
						break;
					}
				if (pageVisibilityUnchanged)
					return;
				super.setVisiblePages(visiblePageIDs);
				ImDocumentEditorTab.this.validate();
				ImDocumentEditorTab.this.repaint();
			}
			public void setSideBySidePages(int sbsp) {
				if (sbsp == this.getSideBySidePages())
					return;
				super.setSideBySidePages(sbsp);
				ImDocumentEditorTab.this.validate();
				ImDocumentEditorTab.this.repaint();
			}
			public void applyMarkupTool(ImageMarkupTool imt, ImAnnotation annot) {
				imToolActive = true;
				super.applyMarkupTool(imt, annot);
				imToolActive = false;
			}
			public void paint(Graphics graphics) {
				if (idmpBoxInFastScroll)
					return;
				super.paint(graphics);
			}
			public void validate() {
				if (idmpBoxInFastScroll)
					return;
				super.validate();
			}
			public void repaint() {
				if (idmpBoxInFastScroll)
					return;
				super.repaint();
			}
		}
		
		private class UndoRecorder implements ImDocumentListener {
			public void typeChanged(final ImObject object, final String oldType) {
				if (inUndoAction)
					return;
				addUndoAction(new UndoAction(("Change Object Type to '" + object.getType() + "'"), ImDocumentEditorTab.this) {
					void doExecute() {
						object.setType(oldType);
					}
				});
			}
			public void regionAdded(final ImRegion region) {
				if (inUndoAction)
					return;
				addUndoAction(new UndoAction(("Add '" + region.getType() + "' Region"), ImDocumentEditorTab.this) {
					void doExecute() {
						idmp.document.getPage(region.pageId).removeRegion(region);
					}
				});
			}
			public void regionRemoved(final ImRegion region) {
				if (inUndoAction)
					return;
				if (region instanceof ImWord)
					addUndoAction(new UndoAction(("Remove Word '" + region.getAttribute(ImWord.STRING_ATTRIBUTE) + "'"), ImDocumentEditorTab.this) {
						void doExecute() {
							idmp.document.getPage(region.pageId).addWord((ImWord) region);
						}
					});
				else addUndoAction(new UndoAction(("Remove '" + region.getType() + "' Region"), ImDocumentEditorTab.this) {
					void doExecute() {
						idmp.document.getPage(region.pageId).addRegion(region);
					}
				});
			}
			public void attributeChanged(final ImObject object, final String attributeName, final Object oldValue) {
				if (inUndoAction)
					return;
				if (oldValue == null)
					addUndoAction(new UndoAction(("Add " + attributeName + " Attribute to " + object.getType()), ImDocumentEditorTab.this) {
						void doExecute() {
							object.setAttribute(attributeName, oldValue); // we need to set here instead of removing, as some objects have built-in special attributes (ImWord !!!)
						}
					});
				else if (object.getAttribute(attributeName) == null)
					addUndoAction(new UndoAction(("Remove '" + attributeName + "' Attribute from " + object.getType()), ImDocumentEditorTab.this) {
						void doExecute() {
							object.setAttribute(attributeName, oldValue);
						}
					});
				else addUndoAction(new UndoAction(("Change '" + attributeName + "' Attribute of " + object.getType() + " to '" + object.getAttribute(attributeName).toString() + "'"), ImDocumentEditorTab.this) {
					void doExecute() {
						object.setAttribute(attributeName, oldValue);
					}
				});
			}
			public void annotationAdded(final ImAnnotation annotation) {
				if (inUndoAction)
					return;
				addUndoAction(new UndoAction(("Add '" + annotation.getType() + "' Annotation"), ImDocumentEditorTab.this) {
					void doExecute() {
						/* We need to re-get annotation and make our own
						 * comparison, as removing and re-adding thwarts
						 * this simple approach */
						ImAnnotation[] annots = annotation.getDocument().getAnnotations(annotation.getFirstWord(), null);
						for (int a = 0; a < annots.length; a++) {
							if (!annots[a].getLastWord().getLocalID().equals(annotation.getLastWord().getLocalID()))
								continue;
							if (!annots[a].getType().equals(annotation.getType()))
								continue;
							idmp.document.removeAnnotation(annots[a]);
							break;
						}
					}
				});
			}
			public void annotationRemoved(final ImAnnotation annotation) {
				if (inUndoAction)
					return;
				addUndoAction(new UndoAction(("Remove '" + annotation.getType() + "' Annotation"), ImDocumentEditorTab.this) {
					void doExecute() {
						ImAnnotation reAnnot = idmp.document.addAnnotation(annotation.getFirstWord(), annotation.getLastWord(), annotation.getType());
						if (reAnnot != null)
							reAnnot.copyAttributes(annotation);
					}
				});
			}
		}
		
		private class ReactionTrigger implements ImDocumentListener {
			private ReactionProvider[] reactionProviders;
			ReactionTrigger(ReactionProvider[] reactionProviders) {
				this.reactionProviders = reactionProviders;
			}
			public void typeChanged(final ImObject object, final String oldType) {
				if (inUndoAction || imToolActive || !inReactionObjects.add(object))
					return;
				try {
					for (int p = 0; p < this.reactionProviders.length; p++)
						this.reactionProviders[p].typeChanged(object, oldType, idmp, parent.allowReactionPrompts.isSelected());
				}
				catch (Throwable t) {
					System.out.println("Error reacting to object type change: " + t.getMessage());
					t.printStackTrace(System.out);
				}
				finally {
					inReactionObjects.remove(object);
				}
			}
			public void regionAdded(final ImRegion region) {
				if (inUndoAction || imToolActive || !inReactionObjects.add(region))
					return;
				try {
					for (int p = 0; p < this.reactionProviders.length; p++)
						this.reactionProviders[p].regionAdded(region, idmp, parent.allowReactionPrompts.isSelected());
				}
				catch (Throwable t) {
					System.out.println("Error reacting to region addition: " + t.getMessage());
					t.printStackTrace(System.out);
				}
				finally {
					inReactionObjects.remove(region);
				}
			}
			public void regionRemoved(final ImRegion region) {
				if (inUndoAction || imToolActive || !inReactionObjects.add(region))
					return;
				try {
					for (int p = 0; p < this.reactionProviders.length; p++)
						this.reactionProviders[p].regionRemoved(region, idmp, parent.allowReactionPrompts.isSelected());
				}
				catch (Throwable t) {
					System.out.println("Error reacting to region removal: " + t.getMessage());
					t.printStackTrace(System.out);
				}
				finally {
					inReactionObjects.remove(region);
				}
			}
			public void attributeChanged(final ImObject object, final String attributeName, final Object oldValue) {
				if (inUndoAction || imToolActive || !inReactionObjects.add(object))
					return;
				try {
					for (int p = 0; p < this.reactionProviders.length; p++)
						this.reactionProviders[p].attributeChanged(object, attributeName, oldValue, idmp, parent.allowReactionPrompts.isSelected());
				}
				catch (Throwable t) {
					System.out.println("Error reacting to object attribute change: " + t.getMessage());
					t.printStackTrace(System.out);
				}
				finally {
					inReactionObjects.remove(object);
				}
			}
			public void annotationAdded(final ImAnnotation annotation) {
				if (inUndoAction || imToolActive || !inReactionObjects.add(annotation))
					return;
				try {
					for (int p = 0; p < this.reactionProviders.length; p++)
						this.reactionProviders[p].annotationAdded(annotation, idmp, parent.allowReactionPrompts.isSelected());
				}
				catch (Throwable t) {
					System.out.println("Error reacting to annotation addition: " + t.getMessage());
					t.printStackTrace(System.out);
				}
				finally {
					inReactionObjects.remove(annotation);
				}
			}
			public void annotationRemoved(final ImAnnotation annotation) {
				if (inUndoAction || imToolActive || !inReactionObjects.add(annotation))
					return;
				try {
					for (int p = 0; p < this.reactionProviders.length; p++)
						this.reactionProviders[p].annotationRemoved(annotation, idmp, parent.allowReactionPrompts.isSelected());
				}
				catch (Throwable t) {
					System.out.println("Error reacting to annotation removal: " + t.getMessage());
					t.printStackTrace(System.out);
				}
				finally {
					inReactionObjects.remove(annotation);
				}
			}
		}
		
		void scrollUp() {
			if (this.idmp.getSideBySidePages() == 1) {
				JScrollBar vsb = this.idmpBox.getVerticalScrollBar();
				vsb.setValue(Math.max(vsb.getMinimum(), (vsb.getValue() - this.idmpBox.getViewport().getViewRect().height)));
			}
			else {
				JScrollBar hsb = this.idmpBox.getHorizontalScrollBar();
				hsb.setValue(Math.max(hsb.getMinimum(), (hsb.getValue() - this.idmpBox.getViewport().getViewRect().width)));
			}
		}
		
		void scrollDown() {
			if (this.idmp.getSideBySidePages() == 1) {
				JScrollBar vsb = this.idmpBox.getVerticalScrollBar();
				vsb.setValue(Math.min(vsb.getMaximum(), (vsb.getValue() + this.idmpBox.getViewport().getViewRect().height)));
			}
			else {
				JScrollBar hsb = this.idmpBox.getHorizontalScrollBar();
				hsb.setValue(Math.min(hsb.getMaximum(), (hsb.getValue() + this.idmpBox.getViewport().getViewRect().width)));
			}
		}
		
		void setRenderingDpi(int renderingDpi) {
			int oldRenderingDpi = this.idmp.getRenderingDpi();
			if (renderingDpi == oldRenderingDpi)
				return;
			Dimension viewSize = this.idmpBox.getViewport().getExtentSize();
			Point oldViewPos = this.idmpBox.getViewport().getViewPosition();
			Point oldViewCenter = new Point((oldViewPos.x + (viewSize.width / 2)), (oldViewPos.y + (viewSize.height / 2)));
			this.idmp.setRenderingDpi(renderingDpi);
			this.validate();
			this.repaint();
			Point newViewCenter = new Point(((oldViewCenter.x * renderingDpi) / oldRenderingDpi), ((oldViewCenter.y * renderingDpi) / oldRenderingDpi));
			Point newViewPos = new Point(Math.max((newViewCenter.x - (viewSize.width / 2)), 0), Math.max((newViewCenter.y - (viewSize.height / 2)), 0));
			this.idmpBox.getViewport().setViewPosition(newViewPos);
		}
		
		void setSideBySidePages(int sbsp) {
			int oldSbsp = this.idmp.getSideBySidePages();
			if (sbsp == oldSbsp)
				return;
			Dimension viewSize = this.idmpBox.getViewport().getExtentSize();
			Point viewPos = this.idmpBox.getViewport().getViewPosition();
			Point viewCenter = new Point((viewPos.x + (viewSize.width / 2)), (viewPos.y + (viewSize.height / 2)));
			Component viewCenterPage = this.idmp.getComponentAt(viewCenter);
			while (((viewCenterPage == null) || (viewCenterPage == this.idmp)) && (viewCenter.x > 0) && (viewCenter.y > 0)) {
				viewCenter = new Point((viewCenter.x - 20), (viewCenter.y - 20));
				viewCenterPage = this.idmp.getComponentAt(viewCenter);
			}
			this.idmp.setSideBySidePages(sbsp);
			this.validate();
			this.repaint();
			if (viewCenterPage != null)
				this.idmpBox.getViewport().setViewPosition(viewCenterPage.getLocation());
		}
		
		void selectVisiblePages() {
			
			//	create selector tiles and compute size
			PageSelectorTile[] psts = new PageSelectorTile[idmp.document.getPageCount()];
			int ptWidth = 0;
			int ptHeight = 0;
			for (int p = 0; p < psts.length; p++) {
				PageThumbnail pt = this.idmp.getPageThumbnail(p);
				psts[p] = new PageSelectorTile(pt, this.idmp.isPageVisible(p));
				ptWidth = Math.max(ptWidth, pt.getPreferredSize().width);
				ptHeight = Math.max(ptHeight, pt.getPreferredSize().height);
			}
			
			//	set selector tile size (adding 4 for border width)
			for (int p = 0; p < psts.length; p++)
				psts[p].setPreferredSize(new Dimension(((ptWidth * 2) + 4), ((ptHeight * 2) + 4)));
			
			//	create dialog
			final DialogPanel vps = new DialogPanel("Select Visible Pages", true);
			vps.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			vps.setSize(vps.getOwner().getSize());
			vps.setLocationRelativeTo(vps.getOwner());
			
			//	compute number of selector tiles that fit side by side
			int sideBySidePsts = ((vps.getSize().width + 10) / (((ptWidth * 2) + 4) + 10));
			
			//	line up selector tiles
			JPanel pstPanel = new JPanel(new GridBagLayout(), true);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.insets.top = 5;
			gbc.insets.bottom = 5;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.weightx = 0;
			gbc.weighty = 0;
			gbc.gridx = 0;
			gbc.gridy = 0;
			for (int p = 0; p < psts.length; p++) {
				pstPanel.add(psts[p], gbc.clone());
				gbc.gridx++;
				if (gbc.gridx == sideBySidePsts) {
					gbc.gridx = 0;
					gbc.gridy++;
				}
			}
			gbc.gridwidth = Math.min(psts.length, sideBySidePsts);
			gbc.weighty = 1;
			gbc.gridx = 0;
			gbc.gridy++;
			pstPanel.add(new JPanel(), gbc.clone());
			JScrollPane pstPanelBox = new JScrollPane(pstPanel);
			pstPanelBox.getVerticalScrollBar().setUnitIncrement(50);
			pstPanelBox.getVerticalScrollBar().setBlockIncrement(50);
			
			//	add buttons
			final boolean[] cancelled = {false};
			JButton ok = new JButton("OK");
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					vps.dispose();
				}
			});
			JButton cancel = new JButton("Cancel");
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					cancelled[0] = true;
					vps.dispose();
				}
			});
			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			buttons.add(ok);
			buttons.add(cancel);
			
			//	assemble dialog content
			vps.add(pstPanelBox, BorderLayout.CENTER);
			vps.add(buttons, BorderLayout.SOUTH);
			
			//	show dialog
			vps.setVisible(true);
			
			//	cancelled
			if (cancelled[0])
				return;
			
			//	select visible pages
			int[] visiblePageIDs = new int[psts.length];
			for (int p = 0; p < psts.length; p++)
				visiblePageIDs[p] = (psts[p].pageVisible ? p : -1);
			this.idmp.setVisiblePages(visiblePageIDs);
		}
		
		private class PageSelectorTile extends JPanel {
			private PageThumbnail pt;
			boolean pageVisible;
			PageSelectorTile(PageThumbnail pt, boolean pageVisible) {
				super(new BorderLayout(), true);
				this.pt = pt;
				this.pageVisible = pageVisible;
				this.setBorder();
				this.setToolTipText(this.pt.getTooltipText());
				this.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						togglePageVisible();
					}
				});
			}
			void setBorder() {
				this.setBorder(BorderFactory.createLineBorder((this.pageVisible ? Color.DARK_GRAY : Color.LIGHT_GRAY), 2));
			}
			public void paint(Graphics g) {
				super.paint(g);
				this.pt.paint(g, 2, 2, (this.getWidth()-4), (this.getHeight()-4), this);
			}
			void togglePageVisible() {
				this.pageVisible = !this.pageVisible;
				this.setBorder();
				this.validate();
				this.repaint();
			}
		}
		
		private void addUndoAction(UndoAction ua) {
			if (this.inUndoAction)
				return;
			if (this.multipartUndoAction == null) {
				this.modCount++;
				this.undoActions.addFirst(ua);
				this.parent.updateUndoMenu(this.undoActions);
			}
			else this.multipartUndoAction.addUndoAction(ua);
		}
		
		private void startMultipartUndoAction(String label) {
			this.multipartUndoAction = new MultipartUndoAction(label, this);
		}
		
		private void finishMultipartUndoAction() {
			if ((this.multipartUndoAction != null) && (this.multipartUndoAction.parts.size() != 0)) {
				this.modCount++;
				this.undoActions.addFirst(this.multipartUndoAction);
				this.parent.updateUndoMenu(this.undoActions);
			}
			this.multipartUndoAction = null;
		}
		
		private static class IdmpViewport extends JViewport implements TwoClickActionMessenger {
			private static Color halfTransparentRed = new Color(Color.red.getRed(), Color.red.getGreen(), Color.red.getBlue(), 128);
			private ImDocumentMarkupPanel idvp;
			private String tcaMessage = null;
			IdmpViewport(ImDocumentMarkupPanel idvp) {
				this.idvp = idvp;
				this.idvp.setTwoClickActionMessenger(this);
				this.setView(this.idvp);
				this.setOpaque(false);
			}
			public void twoClickActionChanged(TwoClickSelectionAction tcsa) {
				this.tcaMessage = ((tcsa == null) ? null : tcsa.getActiveLabel());
				this.validate();
				this.repaint();
			}
			public void paint(Graphics g) {
				super.paint(g);
				if (this.tcaMessage == null)
					return;
				Font f = new Font("Helvetica", Font.PLAIN, 20);
				g.setFont(f);
				TextLayout wtl = new TextLayout(this.tcaMessage, f, ((Graphics2D) g).getFontRenderContext());
				g.setColor(halfTransparentRed);
				g.fillRect(0, 0, this.getViewRect().width, ((int) Math.ceil(wtl.getBounds().getHeight() + (wtl.getDescent() * 3))));
				g.setColor(Color.white);
				((Graphics2D) g).drawString(this.tcaMessage, ((this.getViewRect().width - wtl.getAdvance()) / 2), ((int) Math.ceil(wtl.getBounds().getHeight() + wtl.getDescent())));
			}
		}
//		
//		String getDocumentName() {
//			if (this.file != null)
//				return this.file.getName();
//			else return ((String) this.idmp.document.getAttribute(DOCUMENT_NAME_ATTRIBUTE, "Unknown Document"));
//		}
		
		boolean isDirty() {
			return (this.modCount != this.savedModCount);
		}
		
		boolean save() {
			if (!this.isDirty())
				return true;
			else if (this.docSource != null)
				return this.saveAs(this.docSource);
			else return false;
		}
		
		boolean saveAs(JFileChooser fileChooser) {
			clearFileFilters(fileChooser);
			fileChooser.addChoosableFileFilter(imfFileFilter);
			if (this.docSource != null)
				fileChooser.setSelectedFile(this.docSource);
			else if (true) {
				File docFolder = fileChooser.getSelectedFile();
				if (docFolder != null)
					docFolder = docFolder.getAbsoluteFile();
				if ((docFolder != null) && docFolder.isFile())
					docFolder = docFolder.getParentFile();
				if (docFolder != null)
					fileChooser.setSelectedFile(new File(docFolder, this.docName));
			}
			if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
				return false;
			File file = fileChooser.getSelectedFile();
			if (file.isDirectory())
				return false;
			return this.saveAs(file);
		}
		
		boolean saveAs(File file) {
			
			//	check file name
			if (!file.getName().endsWith(".imf"))
				file = new File(file.getAbsolutePath() + ".imf");
			
			//	create splash screen
			final ResourceSplashScreen saveSplashScreen = new ResourceSplashScreen(this.parent, "Saving Document, Please Wait", "", false, false);
			
			//	save document, in separate thread
			final boolean[] saveSuccess = {false};
			final File[] saveFile = {file};
			Thread saveThread = new Thread() {
				public void run() {
					try {
						
						//	wait for splash screen to come up (we must not reach the dispose() line before the splash screen even comes up)
						while (!saveSplashScreen.isVisible()) try {
							Thread.sleep(10);
						} catch (InterruptedException ie) {}
						
						//	notify listeners that saving is imminent
						ImDocumentEditorTab.this.parent.ggImagine.notifyDocumentSaving(ImDocumentEditorTab.this.idmp.document);
						
						//	make way
						if (saveFile[0].exists()) {
							String fileName = saveFile[0].getAbsolutePath();
							saveFile[0].renameTo(new File(fileName + "." + System.currentTimeMillis() + ".old"));
							saveFile[0] = new File(fileName);
						}
						
						//	save document
						OutputStream out = new BufferedOutputStream(new FileOutputStream(saveFile[0]));
						ImfIO.storeDocument(ImDocumentEditorTab.this.idmp.document, out, saveSplashScreen);
						out.flush();
						out.close();
						ImDocumentEditorTab.this.savedModCount = ImDocumentEditorTab.this.modCount;
						ImDocumentEditorTab.this.docName = saveFile[0].getName();
						ImDocumentEditorTab.this.docSource = saveFile[0];
						ImDocumentEditorTab.this.parent.docTabs.setTitleAt(ImDocumentEditorTab.this.parent.docTabs.indexOfComponent(ImDocumentEditorTab.this), ImDocumentEditorTab.this.docName);
						saveSuccess[0] = true;
						
						//	notify listeners of saving success
						ImDocumentEditorTab.this.parent.ggImagine.notifyDocumentSaved(ImDocumentEditorTab.this.idmp.document);
					}
					
					//	catch whatever might happen
					catch (Throwable t) {
						JOptionPane.showMessageDialog(ImDocumentEditorTab.this, ("An error occurred while saving the document to '" + saveFile[0].getAbsolutePath() + "':\n" + t.getMessage()), "Error Saving Document", JOptionPane.ERROR_MESSAGE);
						t.printStackTrace(System.out);
					}
					
					//	dispose splash screen
					finally {
						saveSplashScreen.dispose();
					}
				}
			};
			saveThread.start();
			
			//	open splash screen (this waits)
			saveSplashScreen.setVisible(true);
			
			//	finally ...
			return saveSuccess[0];
		}
		
		void close() {
			this.idmp.document.removeDocumentListener(this.undoRecorder);
			if (this.reactionTrigger != null)
				this.idmp.document.removeDocumentListener(this.reactionTrigger);
			this.parent.removeDocument(this);
			this.parent.ggImagine.notifyDocumentClosed(this.idmp.document.docId);
			Settings annotationColors = this.parent.config.getSubset("annotation.color");
			String[] annotationTypes = this.idmp.getAnnotationTypes();
			for (int t = 0; t < annotationTypes.length; t++) {
				Color ac = this.idmp.getAnnotationColor(annotationTypes[t]);
				if (ac != null)
					annotationColors.setSetting(annotationTypes[t], GoldenGateImagine.getHex(ac));
			}
			Settings layoutObjectColors = this.parent.config.getSubset("layoutObject.color");
			String[] layoutObjectTypes = this.idmp.getLayoutObjectTypes();
			for (int t = 0; t < layoutObjectTypes.length; t++) {
				Color loc = this.idmp.getLayoutObjectColor(layoutObjectTypes[t]);
				if (loc != null)
					layoutObjectColors.setSetting(layoutObjectTypes[t], GoldenGateImagine.getHex(loc));
			}
			Settings textStreamColors = this.parent.config.getSubset("textStream.color");
			String[] textStreamTypes = this.idmp.getTextStreamTypes();
			for (int t = 0; t < textStreamTypes.length; t++) {
				Color tsc = this.idmp.getTextStreamTypeColor(textStreamTypes[t]);
				if (tsc != null)
					textStreamColors.setSetting(textStreamTypes[t], GoldenGateImagine.getHex(tsc));
			}
		}
	}
	
	private static final FileFilter imfFileFilter = new FileFilter() {
		public boolean accept(File file) {
			return (file.isDirectory() || file.getName().toLowerCase().endsWith(".imf"));
		}
		public String getDescription() {
			return "Image Markup Files";
		}
		public String toString() {
			return this.getDescription();
		}
	};
	private static final FileFilter genericPdfFileFilter = new FileFilter() {
		public boolean accept(File file) {
			return (file.isDirectory() || file.getName().toLowerCase().endsWith(".pdf"));
		}
		public String getDescription() {
			return "PDF Documents";
		}
		public String toString() {
			return this.getDescription();
		}
	};
	private static final FileFilter textPdfFileFilter = new FileFilter() {
		public boolean accept(File file) {
			return (file.isDirectory() || file.getName().toLowerCase().endsWith(".pdf"));
		}
		public String getDescription() {
			return "PDF Documents (born-digital)";
		}
		public String toString() {
			return this.getDescription();
		}
	};
	private static final FileFilter imagePdfFileFilter = new FileFilter() {
		public boolean accept(File file) {
			return (file.isDirectory() || file.getName().toLowerCase().endsWith(".pdf"));
		}
		public String getDescription() {
			return "PDF Documents (scanned)";
		}
		public String toString() {
			return this.getDescription();
		}
	};
	private static final FileFilter imageAndMetaPdfFileFilter = new FileFilter() {
		public boolean accept(File file) {
			return (file.isDirectory() || file.getName().toLowerCase().endsWith(".pdf"));
		}
		public String getDescription() {
			return "PDF Documents (scanned, with meta pages)";
		}
		public String toString() {
			return this.getDescription();
		}
	};
	private static final FileFilter xmlFileFilter = new FileFilter() {
		public boolean accept(File file) {
			return (file.isDirectory() || file.getName().toLowerCase().endsWith(".xml"));
		}
		public String getDescription() {
			return "XML Documents";
		}
	};
	private static void clearFileFilters(JFileChooser fileChooser) {
		FileFilter[] fileFilters = fileChooser.getChoosableFileFilters();
		for (int f = 0; f < fileFilters.length; f++)
			fileChooser.removeChoosableFileFilter(fileFilters[f]);
	}
//	
//	private static class SplashScreen extends JDialog implements ControllingProgressMonitor {
//		private JLabel textLabel = new JLabel("Please wait while GoldenGATE Resource is running ...", JLabel.LEFT);
//		private ProgressMonitorPanel pmp;
//		
//		SplashScreen(JFrame owner, String title, String text, boolean supportPauseResume, boolean supportAbort) {
//			super(owner, title, true);
//			this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//			
//			this.pmp = new ProgressMonitorPanel(supportPauseResume, supportAbort);
//			
//			this.setLayout(new BorderLayout());
//			this.add(this.textLabel, BorderLayout.NORTH);
//			this.add(this.pmp, BorderLayout.CENTER);
//			
//			this.setSize(new Dimension(400, ((supportPauseResume || supportAbort) ? 150 : 130)));
//			this.setLocationRelativeTo(owner);
//			if (text != null)
//				this.setText(text);
//		}
//		
//		public synchronized void setAbortExceptionMessage(String aem) {
//			this.pmp.setAbortExceptionMessage(aem);
//		}
//		public boolean supportsAbort() {
//			return this.pmp.supportsAbort();
//		}
//		public synchronized void setAbortEnabled(boolean ae) {
//			this.pmp.setAbortEnabled(ae);
//		}
//		public boolean supportsPauseResume() {
//			return this.pmp.supportsPauseResume();
//		}
//		public synchronized void setPauseResumeEnabled(boolean pre) {
//			this.pmp.setPauseResumeEnabled(pre);
//		}
//		public synchronized void setStep(String step) {
//			this.pmp.setStep(step);
//		}
//		public synchronized void setInfo(String info) {
//			this.pmp.setInfo(info);
//		}
//		public synchronized void setBaseProgress(int baseProgress) {
//			this.pmp.setBaseProgress(baseProgress);
//		}
//		public synchronized void setMaxProgress(int maxProgress) {
//			this.pmp.setMaxProgress(maxProgress);
//		}
//		public synchronized void setProgress(int progress) {
//			this.pmp.setProgress(progress);
//		}
//		/**
//		 * Set the text displayed on the label of the splash screen
//		 * @param text the new text
//		 */
//		public synchronized void setText(String text) {
//			this.textLabel.setText((text == null) ? "" : text);
//		}
//	}
	
	private static class SelectionActionUsageStats extends TreeMap {
		private static class SelectionActionUsage {
			int shown = 0;
			int used = 0;
			int usedLast = 0;
			SelectionActionUsage() {}
		}
		
		private int isSaAdvancedPivotIndex = 10;
		private int saUseCounter = 1;
		
		private SelectionActionUsage getSelectionActionUsage(String saName) {
			SelectionActionUsage saUsage = ((SelectionActionUsage) this.get(saName));
			if (saUsage == null) {
				saUsage = new SelectionActionUsage();
				this.put(saName, saUsage);
			}
			return saUsage;
		}
		
		boolean[] markAdvancedSelectionActions(SelectionAction[] sas) {
			float[] isSaAdvancedScoresBySa = new float[sas.length];
			float[] isSaAdvancedScoresByVal = new float[sas.length];
			for (int a = 0; a < sas.length; a++) {
				float isSaAdvancedScore = 0;
				if (sas[a] != SelectionAction.SEPARATOR) {
					SelectionActionUsage saUsage = this.getSelectionActionUsage(sas[a].name);
					saUsage.shown++;
					isSaAdvancedScore += (((float) saUsage.used) / saUsage.shown); // MFU part
					isSaAdvancedScore += (((float) saUsage.usedLast) / this.saUseCounter); // MRU part
				}
				isSaAdvancedScoresBySa[a] = isSaAdvancedScore;
				isSaAdvancedScoresByVal[a] = isSaAdvancedScore;
			}
			
			Arrays.sort(isSaAdvancedScoresByVal);
			float isSaAdvancedThreshold = ((sas.length < this.isSaAdvancedPivotIndex) ? 0 : isSaAdvancedScoresByVal[sas.length - this.isSaAdvancedPivotIndex]);
			
			boolean[] isSaAdvanced = new boolean[sas.length];
			for (int a = 0; a < sas.length; a++)
				isSaAdvanced[a] = (isSaAdvancedScoresBySa[a] < isSaAdvancedThreshold);
			
			return isSaAdvanced;
		}
		
		void selectionActionUsed(SelectionAction sa) {
			SelectionActionUsage saUsage = this.getSelectionActionUsage(sa.name);
			saUsage.used++;
			saUsage.usedLast = this.saUseCounter++;
		}
		
		void fillFrom(Settings set) {
			this.isSaAdvancedPivotIndex = Math.max(1, Integer.parseInt(set.getSetting("isAdvancedPivotIndex", ("" + this.isSaAdvancedPivotIndex))));
			this.saUseCounter = Math.max(1, Integer.parseInt(set.getSetting("useCounter", "1")));
			
			String[] saNames = set.getSubsetPrefixes();
			for (int n = 0; n < saNames.length; n++) {
				Settings saUsageSet = set.getSubset(saNames[n]);
				SelectionActionUsage saUsage = this.getSelectionActionUsage(saNames[n]);
				saUsage.shown = Integer.parseInt(saUsageSet.getSetting("shown", "0"));
				saUsage.used = Integer.parseInt(saUsageSet.getSetting("used", "0"));
				saUsage.usedLast = Integer.parseInt(saUsageSet.getSetting("usedLast", "0"));
			}
		}
		void storeTo(Settings set) {
			set.setSetting("isAdvancedPivotIndex", ("" + this.isSaAdvancedPivotIndex));
			set.setSetting("useCounter", ("" + this.saUseCounter));
			
			for (Iterator sanit = this.keySet().iterator(); sanit.hasNext();) {
				String saName = ((String) sanit.next());
				SelectionActionUsage saUsage = this.getSelectionActionUsage(saName);
				Settings saUsageSet = set.getSubset(saName);
				saUsageSet.setSetting("shown", ("" + saUsage.shown));
				saUsageSet.setSetting("used", ("" + saUsage.used));
				saUsageSet.setSetting("usedLast", ("" + saUsage.usedLast));
			}
		}
	}
}