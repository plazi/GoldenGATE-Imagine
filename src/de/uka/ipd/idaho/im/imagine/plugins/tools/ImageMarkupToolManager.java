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
package de.uka.ipd.idaho.im.imagine.plugins.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathExpression;
import de.uka.ipd.idaho.gamta.util.gPath.GPathParser;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.MonitorableDocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.Resource;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceManager;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.HelpEditorDialog;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
import de.uka.ipd.idaho.im.imagine.GoldenGateImagine;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Manager for Image Markup tools. Image Markup tools are basically GoldenGATE
 * document processors bundled with a document normalization level, a label,
 * and an explanatory tooltip text.
 * 
 * @author sautter
 */
public class ImageMarkupToolManager extends AbstractResourceManager implements SelectionActionProvider, ImageMarkupToolProvider {
	
	private static final String FILE_EXTENSION = ".imTool";
	
	private DpImageMarkupTool[] toolsMenuTools;
	private DpImageMarkupTool[] contextMenuTools;
	
	/** public zero-argument constructor for class loading */
	public ImageMarkupToolManager() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Tool Manager";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Image Markup Tool";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin#setImagineParent(de.uka.ipd.idaho.im.imagine.GoldenGateImagine)
	 */
	public void setImagineParent(GoldenGateImagine ggImagine) { /* we don't seem to need this one here */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin#initImagine()
	 */
	public void initImagine() {
		
		//	get whole tools name list
		String[] imtNames = this.getResourceNames();
		
		//	sort tools for main and context menu
		LinkedList toolsMenuTools = new LinkedList();
		LinkedList contextMenuTools = new LinkedList();
		for (int n = 0; n < imtNames.length; n++) {
			DpImageMarkupTool imt = ((DpImageMarkupTool) this.getImageMarkupTool(imtNames[n]));
			if (imt == null)
				continue;
			if (imt.useInToolsMenu)
				toolsMenuTools.addLast(imt);
			if (imt.useAsSelectionAction)
				contextMenuTools.addLast(imt);
		}
		this.toolsMenuTools = ((DpImageMarkupTool[]) toolsMenuTools.toArray(new DpImageMarkupTool[toolsMenuTools.size()]));
		this.contextMenuTools = ((DpImageMarkupTool[]) contextMenuTools.toArray(new DpImageMarkupTool[contextMenuTools.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(ImWord start, ImWord end, final ImDocumentMarkupPanel idmp) {
		
		//	we're only working on annotations, so no use checking anything further if we're working across text streams
		if (!start.getTextStreamId().equals(end.getTextStreamId()))
			return null;
		
		//	get annotations spanning argument words
		ImAnnotation[] allSpanningAnnots = idmp.document.getAnnotationsSpanning(start, end);
		LinkedList spanningAnnotList = new LinkedList();
		for (int a = 0; a < allSpanningAnnots.length; a++) {
			if (idmp.areAnnotationsPainted(allSpanningAnnots[a].getType()))
				spanningAnnotList.add(allSpanningAnnots[a]);
		}
		final ImAnnotation[] spanningAnnots = ((ImAnnotation[]) spanningAnnotList.toArray(new ImAnnotation[spanningAnnotList.size()]));
		
		//	collect actions
		LinkedList actions = new LinkedList();
		
		//	test spanning annotations inside out, and collect actions
		for (int a = spanningAnnots.length; a > 0; a--) {
			ImDocumentRoot wrappedAnnot = new ImDocumentRoot(spanningAnnots[a-1], ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS);
			for (int t = 0; t < this.contextMenuTools.length; t++)
				if (this.contextMenuTools[t].displayFor(wrappedAnnot)) {
					final ImAnnotation annot = spanningAnnots[a-1];
					final DpImageMarkupTool imt = this.contextMenuTools[t];
					actions.add(new SelectionAction(("imt" + this.contextMenuTools[t].getName()), this.contextMenuTools[t].label, this.contextMenuTools[t].tooltip) {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							invoker.applyMarkupTool(imt, annot);
							return true;
						}
					});
				}
			if (actions.size() != 0)
				break;
		}
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, ImDocumentMarkupPanel idmp) {
		
		//	mark selection
		BoundingBox selectedBox = new BoundingBox(Math.min(start.x, end.x), Math.max(start.x, end.x), Math.min(start.y, end.y), Math.max(start.y, end.y));
		
		//	get selected words
		final ImWord[] selectedWords = page.getWordsInside(selectedBox);
		if (selectedWords.length == 0)
			return null;
		
		//	order words
		Arrays.sort(selectedWords, ImUtils.textStreamOrder);
		
		//	check if we have a single continuous stream
		boolean singleContinuousStream = true;
		for (int w = 1; w < selectedWords.length; w++)
			if (selectedWords[w].getPreviousWord() != selectedWords[w-1]) {
				singleContinuousStream = false;
				break;
			}
		
		//	if we have a single continuous selection, we can handle it like a word selection
		return (singleContinuousStream ? this.getActions(selectedWords[0], selectedWords[selectedWords.length - 1], idmp) : null);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getEditMenuItemNames()
	 */
	public String[] getEditMenuItemNames() {
		return null; // we're not integrating ourselves in the 'Edit' menu
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		String[] toolsMenuImtNames = new String[this.toolsMenuTools.length];
		for (int t = 0; t < this.toolsMenuTools.length; t++)
			toolsMenuImtNames[t] = this.toolsMenuTools[t].name;
		return toolsMenuImtNames;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (name == null)
			return null;
		else return this.getImageMarkupTool(name, this.loadSettingsResource(name));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Image Markup Tools";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuItems()
	 */
	public JMenuItem[] getMainMenuItems() {
		if (!this.dataProvider.isDataEditable())
			return new JMenuItem[0];
		
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Create");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				createImageMarkupTool();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				editImageMarkupTools();
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	private boolean createImageMarkupTool() {
		return (this.createImageMarkupTool(new Settings(), null) != null);
	}
	
	private boolean cloneImageMarkupTool() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createImageMarkupTool();
		else {
			String name = "New " + selectedName;
			return (this.createImageMarkupTool(this.loadSettingsResource(selectedName), name) != null);
		}
	}
	
	private String createImageMarkupTool(Settings modelImt, String name) {
		CreateImageMarkupToolDialog cpd = new CreateImageMarkupToolDialog(this.getImageMarkupTool(name, modelImt), name);
		cpd.setVisible(true);
		
		if (cpd.isCommitted()) {
			Settings imt = cpd.getImageMarkupTool();
			String imtName = cpd.getImageMarkupToolName();
			if (!imtName.endsWith(FILE_EXTENSION)) imtName += FILE_EXTENSION;
			try {
				if (this.storeSettingsResource(imtName, imt)) {
					this.resourceNameList.refresh();
					return imtName;
				}
			} catch (IOException ioe) {}
		}
		return null;
	}
	
	private void editImageMarkupTools() {
		final ImageMarkupToolEditorPanel[] editor = new ImageMarkupToolEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Image Markup Tools", true);
		editDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		editDialog.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent we) {
				this.closeDialog();
			}
			public void windowClosing(WindowEvent we) {
				this.closeDialog();
			}
			private void closeDialog() {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeSettingsResource(editor[0].name, editor[0].getSettings());
					} catch (IOException ioe) {}
				}
				if (editDialog.isVisible()) editDialog.dispose();
			}
		});
		
		editDialog.setLayout(new BorderLayout());
		
		JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton button;
		button = new JButton("Create");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				createImageMarkupTool();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				cloneImageMarkupTool();
			}
		});
		editButtons.add(button);
		button = new JButton("Delete");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (deleteResource(resourceNameList.getSelectedName()))
					resourceNameList.refresh();
			}
		});
		editButtons.add(button);
		
		editDialog.add(editButtons, BorderLayout.NORTH);
		
		final JPanel editorPanel = new JPanel(new BorderLayout());
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
		else {
			Settings set = this.loadSettingsResource(selectedName);
			if (set == null)
				editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
			else {
				editor[0] = new ImageMarkupToolEditorPanel(selectedName, this.getImageMarkupTool(selectedName, set));
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeSettingsResource(editor[0].name, editor[0].getSettings());
					}
					catch (IOException ioe) {
						if (DialogFactory.confirm((ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].name + "\nProceed?"), "Could Not Save Image Markup Tool", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].name);
							editorPanel.validate();
							return;
						}
					}
				}
				editorPanel.removeAll();
				
				if (dataName == null)
					editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
				else {
					Settings set = loadSettingsResource(dataName);
					if (set == null)
						editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
					else {
						editor[0] = new ImageMarkupToolEditorPanel(dataName, getImageMarkupTool(dataName, set));
						editorPanel.add(editor[0], BorderLayout.CENTER);
					}
				}
				editorPanel.validate();
			}
		};
		this.resourceNameList.addDataListListener(dll);
		
		editDialog.setSize(DEFAULT_EDIT_DIALOG_SIZE);
		editDialog.setLocationRelativeTo(editDialog.getOwner());
		editDialog.setVisible(true);
		
		this.resourceNameList.removeDataListListener(dll);
	}
	
	private class DpImageMarkupTool implements ImageMarkupTool, Resource {
		final String name;
		final String label;
		final String tooltip;
		
		final int xmlWrapperFlags;
		
		final boolean useInToolsMenu;
		final boolean useAsSelectionAction;
		
		final LinkedHashMap toolsMenuPreclusions;
		final String[] selectionActionFilters;
		
		final DocumentProcessorManager processorProvider;
		final String processorName;
		
		DpImageMarkupTool(String name, String label, String tooltip, int xmlWrapperFlags, String dpName, DocumentProcessorManager dpManager, boolean useInToolsMenu, LinkedHashMap toolsMenuPreclusions, boolean useAsSelectionAction, String[] selectionActionFilters) {
			this.name = name;
			this.label = label;
			this.tooltip = tooltip;
			this.xmlWrapperFlags = xmlWrapperFlags;
			this.useInToolsMenu = useInToolsMenu;
			this.toolsMenuPreclusions = new LinkedHashMap();
			if (toolsMenuPreclusions != null)
				this.toolsMenuPreclusions.putAll(toolsMenuPreclusions);
			this.useAsSelectionAction = useAsSelectionAction;
			this.selectionActionFilters = selectionActionFilters;
			this.processorProvider = dpManager;
			this.processorName = dpName;
		}
		
		public String getName() {
			return this.name;
		}
		public String getTypeLabel() {
			return "Image Markup Tool";
		}
		public String getProviderClassName() {
			return ImageMarkupToolManager.class.getName();
		}
		public String getLabel() {
			return this.label;
		}
		public String getTooltip() {
			return this.tooltip;
		}
		public String getHelpText() {
			return loadStringResource(this.name + ".help.html");
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	wrap document (or annotation)
			if (pm != null)
				pm.setStep("Wrapping document");
			ImDocumentRoot wrappedDoc = ((annot == null) ? new ImDocumentRoot(doc, this.xmlWrapperFlags) : new ImDocumentRoot(annot, this.xmlWrapperFlags));
			
			//	check preclusions (only if annotation is null)
			if (annot == null) {
				if (pm != null)
					pm.setStep("Checking document");
				String precludingError = this.getPrecludingError(wrappedDoc);
				if (precludingError != null) {
					boolean isWarning = false;
					if (precludingError.startsWith("W:")) {
						precludingError = precludingError.substring("W:".length());
						isWarning = true;
					}
					if (idmp == null)
						return;
					int choice = DialogFactory.confirm(("The document does not seem to be fit for " + this.label + ":\n" + precludingError + "\n\nExecuting " + this.label + " anyway might produce undesired results. Proceed?"), ("Document not Fit for '" + this.label + "'"), JOptionPane.YES_NO_OPTION, (isWarning ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE));
					if (choice != JOptionPane.YES_OPTION)
						return;
				}
			}
			
			//	get document processor from manager
			if (pm != null)
				pm.setStep("Loading document processor");
			DocumentProcessor dp = this.processorProvider.getDocumentProcessor(this.processorName);
			
			//	create parameters
			Properties parameters = new Properties();
			if (idmp != null)
				parameters.setProperty(DocumentProcessor.INTERACTIVE_PARAMETER, DocumentProcessor.INTERACTIVE_PARAMETER);
			
			//	process document (or annotation)
			if ((dp instanceof MonitorableDocumentProcessor) && (pm != null))
				((MonitorableDocumentProcessor) dp).process(wrappedDoc, parameters, pm);
			else {
				if (pm != null)
					pm.setStep("Processing document");
				dp.process(wrappedDoc, parameters);
			}
		}
		String getPrecludingError(QueriableAnnotation doc) {
			if (!this.useInToolsMenu)
				return (this.label + " is not applicable to whole documents");
			if (this.toolsMenuPreclusions == null)
				return null;
			String errorOrWarning = null;
			String warning = null;
			for (Iterator pit = this.toolsMenuPreclusions.keySet().iterator(); pit.hasNext();) {
				String gpe = ((String) pit.next());
				if (GPath.evaluateExpression(gpe, doc, null).asBoolean().value) {
					errorOrWarning = ((String) this.toolsMenuPreclusions.get(gpe));
					if (errorOrWarning.startsWith("W:")) {
						if (warning == null)
							warning = errorOrWarning;
					}
					else return errorOrWarning;
				}
			}
			return warning;
		}
		boolean displayFor(QueriableAnnotation annot) {
			if (!this.useAsSelectionAction)
				return false;
			if (this.selectionActionFilters == null)
				return true;
			for (int f = 0; f < this.selectionActionFilters.length; f++) {
				if (GPath.evaluateExpression(this.selectionActionFilters[f], annot, null).asBoolean().value)
					return true;
			}
			return false;
		}
	}
	
	private static final String LABEL_ATTRIBUTE = "LABEL";
	private static final String TOOLTIP_ATTRIBUTE = "TOOLTIP";
	
	private static final String XML_WRAPPER_FLAGS_ATTRIBUTE = "XML_WRAPPER_FLAGS";
	
	private static final String PROCESSOR_NAME_ATTRIBUTE = "PROCESSOR_NAME";
	private static final String PROCESSOR_PROVIDER_CLASS_NAME_ATTRIBUTE = "PROCESSOR_PROVIDER_CLASS";
	
	private static final String LOCATION_ATTRIBUTE = "LOCATION";
	private static final String TOOLS_MENU_LOCATION = "Tools Menu";
	private static final String SELECTION_ACTION_LOCATION = "Selection Actions";
	private static final String BOTH_LOCATION = "Tools Menu & Selection Actions";
	private static final String[] LOCATIONS = {
		TOOLS_MENU_LOCATION,
		SELECTION_ACTION_LOCATION,
		BOTH_LOCATION,
	};
	
	private static final String RAW_NORMALIZATION_LEVEL = "Raw (words strictly in layout order)";
	private static final String WORD_NORMALIZATION_LEVEL = "Words (words in layout order, but de-hyphenated)";
	private static final String PARAGRAPH_NORMALIZATION_LEVEL = "Paragraphs (logical paragraphs kept together)";
	private static final String STREAM_NORMALIZATION_LEVEL = "Text Streams (logical text streams one after another)";
	private static final String[] NORMALIZATION_LEVELS = {
		RAW_NORMALIZATION_LEVEL,
		WORD_NORMALIZATION_LEVEL,
		PARAGRAPH_NORMALIZATION_LEVEL,
		STREAM_NORMALIZATION_LEVEL,
	};
	
	private static final String FILTER_ATTRIBUTE = "FILTER";
	
	private static final String PRECLUSION_ATTRIBUTE_PREFIX = "PRECLUSION";
	private static final String PRECLUSION_FILTER_ATTRIBUTE_SUFFIX = "FILTER";
	private static final String PRECLUSION_MESSAGE_ATTRIBUTE_SUFFIX = "MESSAGE";
	
	/** @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getDataNamesForResource(java.lang.String)
	 */
	public String[] getDataNamesForResource(String name) {
		StringVector nameCollector = new StringVector();
		nameCollector.addElementIgnoreDuplicates(name + "@" + this.getClass().getName());
		
		Settings settings = this.loadSettingsResource(name);
		String processorName = settings.getSetting(PROCESSOR_NAME_ATTRIBUTE);
		String processorProviderClassName = settings.getSetting(PROCESSOR_PROVIDER_CLASS_NAME_ATTRIBUTE);
		if (processorName.indexOf('@') != -1) {
			if (processorProviderClassName == null)
				processorProviderClassName = processorName.substring(processorName.indexOf('@') + 1);
			processorName = processorName.substring(0, processorName.indexOf('@'));
		}
		
		if (this.dataProvider.isDataAvailable(name + ".help.html"))
			nameCollector.addElementIgnoreDuplicates(name + ".help.html@" + this.getClass().getName());
		
		ResourceManager rm = this.parent.getResourceProvider(processorProviderClassName);
		if (rm != null)
			nameCollector.addContentIgnoreDuplicates(rm.getDataNamesForResource(processorName));
		
		return nameCollector.toStringArray();
	}

	/** @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getRequiredResourceNames(java.lang.String, boolean)
	 */
	public String[] getRequiredResourceNames(String name, boolean recourse) {
		StringVector nameCollector = new StringVector();
		Settings settings = this.loadSettingsResource(name);
		
		String processorName = settings.getSetting(PROCESSOR_NAME_ATTRIBUTE);
		if (processorName != null) {
			String processorProviderClassName = settings.getSetting(PROCESSOR_PROVIDER_CLASS_NAME_ATTRIBUTE);
			if (processorName.indexOf('@') != -1) {
				if (processorProviderClassName == null)
					processorProviderClassName = processorName.substring(processorName.indexOf('@') + 1);
				processorName = processorName.substring(0, processorName.indexOf('@'));
			}
			nameCollector.addElement(processorName + "@" + processorProviderClassName);
		}
		
		int nameIndex = 0;
		while (recourse && (nameIndex < nameCollector.size())) {
			String resName = nameCollector.get(nameIndex);
			int split = resName.indexOf('@');
			if (split != -1) {
				String plainResName = resName.substring(0, split);
				String resProviderClassName = resName.substring(split + 1);
				
				ResourceManager rm = this.parent.getResourceProvider(resProviderClassName);
				if (rm != null)
					nameCollector.addContentIgnoreDuplicates(rm.getRequiredResourceNames(plainResName, recourse));
			}
			nameIndex++;
		}
		
		return nameCollector.toStringArray();
	}
	
	private DpImageMarkupTool getImageMarkupTool(String name, Settings settings) {
		if (settings == null)
			return null;
		try {
			String label = settings.getSetting(LABEL_ATTRIBUTE, "");
			String tooltip = settings.getSetting(TOOLTIP_ATTRIBUTE, "");
			
			int xmlWrapperFlags = Integer.parseInt(settings.getSetting(XML_WRAPPER_FLAGS_ATTRIBUTE, ("" + ImDocumentRoot.NORMALIZATION_LEVEL_WORDS)));
			
			String processorName = settings.getSetting(PROCESSOR_NAME_ATTRIBUTE);
			String processorProviderClassName = settings.getSetting(PROCESSOR_PROVIDER_CLASS_NAME_ATTRIBUTE);
			if (processorName.indexOf('@') != -1) {
				if (processorProviderClassName == null)
					processorProviderClassName = processorName.substring(processorName.indexOf('@') + 1);
				processorName = processorName.substring(0, processorName.indexOf('@'));
			}
			
			String location = settings.getSetting(LOCATION_ATTRIBUTE, TOOLS_MENU_LOCATION); // use panel-only as default
			
			DocumentProcessorManager dpm = this.parent.getDocumentProcessorProvider(processorProviderClassName);
			if (dpm == null)
				return null;
			
			LinkedHashMap preclusions = new LinkedHashMap();
			if (location.indexOf(TOOLS_MENU_LOCATION) != -1)
				for (int f = 0; f < settings.size(); f++) {
					String filter = settings.getSetting(PRECLUSION_ATTRIBUTE_PREFIX + f + PRECLUSION_FILTER_ATTRIBUTE_SUFFIX);
					String message = settings.getSetting(PRECLUSION_ATTRIBUTE_PREFIX + f + PRECLUSION_MESSAGE_ATTRIBUTE_SUFFIX);
					if ((filter != null) && (message != null))
						preclusions.put(filter, message);
				}
			
			ArrayList filters = new ArrayList();
			if (location.indexOf(SELECTION_ACTION_LOCATION) != -1)
				for (int f = 0; f < settings.size(); f++) {
					String filter = settings.getSetting(FILTER_ATTRIBUTE + f);
					if (filter != null)
						filters.add(filter);
				}
			
			return new DpImageMarkupTool(name, label, tooltip, xmlWrapperFlags, processorName, dpm, (location.indexOf(TOOLS_MENU_LOCATION) != -1), (preclusions.isEmpty() ? null : preclusions), (location.indexOf(SELECTION_ACTION_LOCATION) != -1), (filters.isEmpty() ? null : ((String[]) filters.toArray(new String[filters.size()]))));
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private class CreateImageMarkupToolDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private ImageMarkupToolEditorPanel editor;
		private String imtName = null;
		
		CreateImageMarkupToolDialog(DpImageMarkupTool imt, String name) {
			super("Create Image Markup Tool", true);
			
			this.nameField = new JTextField((name == null) ? "New Image Markup Tool" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					imtName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					imtName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new ImageMarkupToolEditorPanel(name, imt);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(this.nameField, BorderLayout.NORTH);
			this.add(this.editor, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 600));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.imtName != null);
		}
		
		Settings getImageMarkupTool() {
			return this.editor.getSettings();
		}
		
		String getImageMarkupToolName() {
			return this.imtName;
		}
	}
	
	private class ImageMarkupToolEditorPanel extends JPanel implements DocumentListener, ItemListener {
		
		private String name;
		
		private boolean dirty = false;
		private boolean helpTextDirty = false;
		
		private JTextField label = new JTextField();
		private JTextField toolTip = new JTextField();
		private String helpText;
		private JButton editHelpText = new JButton("Edit Help Text");
		
		private JComboBox location = new JComboBox(LOCATIONS);
		
		private JComboBox normalizationLevel = new JComboBox(NORMALIZATION_LEVELS);
		private JCheckBox normalizeChars = new JCheckBox("Normalize Chars");
		
		private JCheckBox excludeTables = new JCheckBox("Exclude Tables");
		private JCheckBox excludeCaptionsFootnotes = new JCheckBox("Exclude Captions & Footnotes");
		private JCheckBox useRandomAnnotationIDs = new JCheckBox("Random Annotation IDs", true);
		private JCheckBox showWordsAnnotations = new JCheckBox("Show Word Annotations");
		
		private LinkedHashMap preclusions = new LinkedHashMap();
		private String[] filters = new String[0];
		private JButton editPreclusionsAndFilters = new JButton("Preclusions (0) & Filters (0)");
		
		private DocumentProcessorManager processorProvider;
		private String processorName;
		private JLabel processorLabel = new JLabel("", JLabel.LEFT);
		private String processorTypeLabel = null;
		
		ImageMarkupToolEditorPanel(String name, DpImageMarkupTool imt) {
			super(new BorderLayout(), true);
			this.name = name;
			this.add(getExplanationLabel(), BorderLayout.CENTER);
			
			this.location.setEditable(false);
			this.location.setSelectedItem(TOOLS_MENU_LOCATION);
			this.normalizationLevel.setEditable(false);
			this.normalizationLevel.setSelectedItem(PARAGRAPH_NORMALIZATION_LEVEL);
			this.editPreclusionsAndFilters.setBorder(BorderFactory.createRaisedBevelBorder());
			this.editPreclusionsAndFilters.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editPreclusionsAndFilters();
				}
			});
			
			if (imt != null) {
				this.label.setText(imt.label);
				this.toolTip.setText(imt.tooltip);
				this.helpText = imt.getHelpText();
				this.processorName = imt.processorName;
				this.processorProvider = imt.processorProvider;
				if (this.processorProvider != null)
					this.processorTypeLabel = this.processorProvider.getResourceTypeLabel();
				this.location.setSelectedItem(imt.useInToolsMenu ? (imt.useAsSelectionAction ? BOTH_LOCATION : TOOLS_MENU_LOCATION) : SELECTION_ACTION_LOCATION);
				
				int normalizationLevel = (imt.xmlWrapperFlags & ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS);
				if (normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_RAW)
					this.normalizationLevel.setSelectedItem(RAW_NORMALIZATION_LEVEL);
				else if (normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_WORDS)
					this.normalizationLevel.setSelectedItem(WORD_NORMALIZATION_LEVEL);
				else if (normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS)
					this.normalizationLevel.setSelectedItem(STREAM_NORMALIZATION_LEVEL);
				else this.normalizationLevel.setSelectedItem(PARAGRAPH_NORMALIZATION_LEVEL);
				this.normalizeChars.setSelected((imt.xmlWrapperFlags & ImDocumentRoot.NORMALIZE_CHARACTERS) != 0);
				this.excludeTables.setSelected((imt.xmlWrapperFlags & ImDocumentRoot.EXCLUDE_TABLES) != 0);
				this.excludeCaptionsFootnotes.setSelected((imt.xmlWrapperFlags & ImDocumentRoot.EXCLUDE_CAPTIONS_AND_FOOTNOTES) != 0);
				this.useRandomAnnotationIDs.setSelected((imt.xmlWrapperFlags & ImDocumentRoot.USE_RANDOM_ANNOTATION_IDS) != 0);
				this.showWordsAnnotations.setSelected((imt.xmlWrapperFlags & ImDocumentRoot.SHOW_TOKENS_AS_WORD_ANNOTATIONS) != 0);
				
				if (imt.toolsMenuPreclusions != null)
					this.preclusions.putAll(imt.toolsMenuPreclusions);
				if (imt.selectionActionFilters != null)
					this.filters = imt.selectionActionFilters;
				this.editPreclusionsAndFilters.setText("Preclusions (" + this.preclusions.size() + ") & Filters (" + this.filters.length + ")");
			}
			this.processorLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if ((me.getClickCount() > 1) && (processorName != null) && (processorProvider != null))
						processorProvider.editDocumentProcessor(processorName);
				}
			});
			
			this.label.getDocument().addDocumentListener(this);
			this.toolTip.getDocument().addDocumentListener(this);
			this.editHelpText.setBorder(BorderFactory.createRaisedBevelBorder());
			this.editHelpText.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editHelpText();
				}
			});
			
			this.location.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			this.normalizationLevel.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			this.normalizeChars.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			this.updateLabels();
			
			JPanel functionPanel = new JPanel(new GridBagLayout(), true);
			functionPanel.setBorder(BorderFactory.createEtchedBorder());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.weighty = 1;
			gbc.gridheight = 1;
			gbc.fill = GridBagConstraints.BOTH;
			
			gbc.gridy = 0;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			functionPanel.add(new JLabel("Label in 'Tools' Menu", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			functionPanel.add(this.label, gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 0;
			functionPanel.add(this.editHelpText, gbc.clone());
			
			gbc.gridy ++;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			functionPanel.add(new JLabel("Image Markup Tool Explanation", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.gridwidth = 2;
			gbc.weightx = 2;
			functionPanel.add(this.toolTip, gbc.clone());
			
			gbc.gridy ++;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			functionPanel.add(new JLabel("Use Image Markup Tool in ...", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			functionPanel.add(this.location, gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 0;
			functionPanel.add(this.editPreclusionsAndFilters, gbc.clone());
			
			gbc.gridy ++;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			functionPanel.add(new JLabel("Use Document Normalization Level ...", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			functionPanel.add(this.normalizationLevel, gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 0;
			functionPanel.add(this.normalizeChars, gbc.clone());
			
			gbc.gridy ++;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			functionPanel.add(this.showWordsAnnotations, gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			functionPanel.add(this.excludeCaptionsFootnotes, gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 0;
			functionPanel.add(this.excludeTables, gbc.clone());
			
			gbc.gridy ++;
			gbc.gridx = 0;
			gbc.gridwidth = 3;
			gbc.weightx = 3;
			gbc.weighty = 1;
			functionPanel.add(this.processorLabel, gbc.clone());
			
			DocumentProcessorManager[] dpms = parent.getDocumentProcessorProviders();
			for (int p = 0; p < dpms.length; p++) {
				final String className = dpms[p].getClass().getName();
				gbc.gridy++;
				
				JButton button = new JButton("Use " + dpms[p].getResourceTypeLabel());
				button.setBorder(BorderFactory.createRaisedBevelBorder());
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						selectProcessor(className);
					}
				});
				
				gbc.gridx = 0;
				gbc.weightx = 2;
				gbc.gridwidth = 2;
				functionPanel.add(button, gbc.clone());
				
				button = new JButton("Create " + dpms[p].getResourceTypeLabel());
				button.setBorder(BorderFactory.createRaisedBevelBorder());
				button.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						createProcessor(className);
					}
				});
				
				gbc.gridx = 2;
				gbc.weightx = 0;
				gbc.gridwidth = 1;
				functionPanel.add(button, gbc.clone());
			}
			
			this.add(functionPanel, BorderLayout.SOUTH);
		}
		
		public void changedUpdate(DocumentEvent de) {}
		
		public void insertUpdate(DocumentEvent de) {
			this.dirty = true;
		}
		
		public void removeUpdate(DocumentEvent de) {
			this.dirty = true;
		}
		
		public void itemStateChanged(ItemEvent ie) {
			this.dirty = true;
		}
		
		boolean isDirty() {
			return (this.dirty || this.helpTextDirty);
		}
		
		private void updateLabels() {
			if ((this.processorName != null) && (this.processorProvider != null)) {
				this.processorTypeLabel = this.processorProvider.getResourceTypeLabel();
				this.processorLabel.setText(this.processorTypeLabel + " '" + this.processorName + "' (double click to edit)");
			}
			else this.processorLabel.setText("<No DocumentProcessor selected yet>");
		}
		
		private void editPreclusionsAndFilters() {
			PreclusionFilterEditorDialog pfed = new PreclusionFilterEditorDialog(this.name, this.preclusions, this.filters);
			pfed.setVisible(true);
			if (pfed.isCommitted()) {
				LinkedHashMap preclusions = pfed.getPreclusions();
				if (preclusions != null) {
					this.preclusions.clear();
					this.preclusions.putAll(preclusions);
				}
				String[] filters = pfed.getFilters();
				if (filters != null)
					this.filters = filters;
				this.editPreclusionsAndFilters.setText("Preclusions (" + this.preclusions.size() + ") & Filters (" + this.filters.length + ")");
				this.dirty = true;
			}
		}
		
		private void editHelpText() {
			HelpEditorDialog hed = new HelpEditorDialog(("Edit Help Text for '" + this.name + "'"), this.helpText);
			hed.setVisible(true);
			if (hed.isCommitted()) {
				this.helpText = hed.getHelpText();
				this.helpTextDirty = true;
			}
		}
		
		private void selectProcessor(String providerClassName) {
			DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(providerClassName);
			if (dpm != null) {
				ResourceDialog rd = ResourceDialog.getResourceDialog(dpm, ("Select " + dpm.getResourceTypeLabel()), "Select");
				rd.setLocationRelativeTo(rd.getOwner());
				rd.setVisible(true);
				
				//	get processor
				String dpn = rd.getSelectedResourceName();
				if (dpn != null) {
					this.processorName = dpn;
					this.processorProvider = dpm;
					this.updateLabels();
					if (this.label.getText().trim().length() == 0)
						this.label.setText(rd.getSelectedResourceName());
					if (this.toolTip.getText().trim().length() == 0) {
						String tml = this.processorProvider.getToolsMenuLabel();
						if (tml == null) tml = "Apply";
						this.toolTip.setText(tml + " " + this.processorProvider.getResourceTypeLabel() + " '" + this.processorName + "'");
					}
					this.dirty = true;
				}
			}
		}
		
		private void createProcessor(String providerClassName) {
			DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(providerClassName);
			if (dpm != null) {
				String dpn = dpm.createDocumentProcessor();
				
				//	get processor
				if (dpn != null) {
					this.processorName = dpn;
					this.processorProvider = dpm;
					this.updateLabels();
					if (this.label.getText().trim().length() == 0)
						this.label.setText(dpn);
					if (this.toolTip.getText().trim().length() == 0) {
						String tml = this.processorProvider.getToolsMenuLabel();
						if (tml == null) tml = "Apply";
						this.toolTip.setText(tml + " " + this.processorProvider.getResourceTypeLabel() + " '" + this.processorName + "'");
					}
					this.dirty = true;
				}
			}
		}
		
		Settings getSettings() {
			Settings set = new Settings();
			
			String label = this.label.getText();
			if (label.trim().length() == 0) {
				if (this.processorName != null) label += ((label.length() == 0) ? "" : ", ") + this.processorName;
			}
			if (label.trim().length() != 0)
				set.setSetting(LABEL_ATTRIBUTE, label);
			
			String toolTip = this.toolTip.getText();
			if (toolTip.trim().length() == 0) {
				if (this.processorName != null)
					toolTip += ((toolTip.length() == 0) ? "Apply " : " and apply ") + (this.processorTypeLabel + " '" + this.processorName + "'");
			}
			if (toolTip.trim().length() != 0)
				set.setSetting(TOOLTIP_ATTRIBUTE, toolTip);
			
			if ((this.processorName != null) && (this.processorProvider != null))
				set.setSetting(PROCESSOR_NAME_ATTRIBUTE, (this.processorName + "@" + this.processorProvider.getClass().getName()));
			
			set.setSetting(LOCATION_ATTRIBUTE, this.location.getSelectedItem().toString());
			
			int xmlWrapperFlags;
			if (RAW_NORMALIZATION_LEVEL.equals(this.normalizationLevel.getSelectedItem()))
				xmlWrapperFlags = ImDocumentRoot.NORMALIZATION_LEVEL_RAW;
			else if (WORD_NORMALIZATION_LEVEL.equals(this.normalizationLevel.getSelectedItem()))
				xmlWrapperFlags = ImDocumentRoot.NORMALIZATION_LEVEL_WORDS;
			else if (STREAM_NORMALIZATION_LEVEL.equals(this.normalizationLevel.getSelectedItem()))
				xmlWrapperFlags = ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS;
			else xmlWrapperFlags = ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS;
			
			if (this.normalizeChars.isSelected())
				xmlWrapperFlags |= ImDocumentRoot.NORMALIZE_CHARACTERS;
			if (this.excludeTables.isSelected())
				xmlWrapperFlags |= ImDocumentRoot.EXCLUDE_TABLES;
			if (this.excludeCaptionsFootnotes.isSelected())
				xmlWrapperFlags |= ImDocumentRoot.EXCLUDE_CAPTIONS_AND_FOOTNOTES;
			if (this.useRandomAnnotationIDs.isSelected())
				xmlWrapperFlags |= ImDocumentRoot.USE_RANDOM_ANNOTATION_IDS;
			if (this.showWordsAnnotations.isSelected())
				xmlWrapperFlags |= ImDocumentRoot.SHOW_TOKENS_AS_WORD_ANNOTATIONS;
			
			set.setSetting(XML_WRAPPER_FLAGS_ATTRIBUTE, ("" + xmlWrapperFlags));
			
			int preclusionCount = 0;
			for (Iterator fit = this.preclusions.keySet().iterator(); fit.hasNext();) {
				String filter = ((String) fit.next());
				set.setSetting((PRECLUSION_ATTRIBUTE_PREFIX + preclusionCount + PRECLUSION_FILTER_ATTRIBUTE_SUFFIX), filter);
				set.setSetting((PRECLUSION_ATTRIBUTE_PREFIX + preclusionCount + PRECLUSION_MESSAGE_ATTRIBUTE_SUFFIX), ((String) this.preclusions.get(filter)));
				preclusionCount++;
			}
			
			for (int f = 0; f < this.filters.length; f++)
				set.setSetting((FILTER_ATTRIBUTE + f), this.filters[f]);
			
			if (this.helpTextDirty && (this.helpText != null)) try {
				storeStringResource((this.name + ".help.html"), this.helpText);
			} catch (IOException ioe) {}
			
			return set;
		}
	}
	
	private class PreclusionFilterEditorDialog extends DialogPanel {
		
		private PreclusionEditorPanel preclusionEditor;
		private LinkedHashMap preclusions = null;
		private FilterEditorPanel filterEditor;
		private String[] filters = null;
		
		PreclusionFilterEditorDialog(String name, LinkedHashMap preclusions, String[] filters) {
			super(("Edit Preclusions & Filters for '" + name + "'"), true);
			
			//	initialize main buttons
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					PreclusionFilterEditorDialog.this.preclusions = preclusionEditor.getPreclusions();
					PreclusionFilterEditorDialog.this.filters = filterEditor.getFilters();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					PreclusionFilterEditorDialog.this.preclusions = null;
					PreclusionFilterEditorDialog.this.filters = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editors
			this.preclusionEditor = new PreclusionEditorPanel(preclusions);
			this.filterEditor = new FilterEditorPanel(filters);
			
			//	put editor in tabs
			JTabbedPane tabs = new JTabbedPane();
			tabs.addTab("Preclusions", this.preclusionEditor);
			tabs.addTab("Filters", this.filterEditor);
			
			//	put the whole stuff together
			this.setLayout(new BorderLayout());
			this.add(tabs, BorderLayout.CENTER);
			this.add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(this.getOwner());
		}
		
		boolean isCommitted() {
			return ((this.filters != null) && (this.preclusions != null));
		}
		
		LinkedHashMap getPreclusions() {
			return this.preclusions;
		}
		
		String[] getFilters() {
			return this.filters;
		}
	}
	
	private class PreclusionEditorPanel extends JPanel {
		private ArrayList preclusions = new ArrayList();
		private int selectedPreclusion = -1;
		
		private JLabel filterLabel = new JLabel("GPath Filter Expression", JLabel.CENTER);
		private JLabel messageLabel = new JLabel("Message to Show", JLabel.CENTER);
		private JLabel isWarningLabel = new JLabel("Warning, not Error?", JLabel.CENTER);
		private JPanel linePanelSpacer = new JPanel();
		private JPanel linePanel = new JPanel(new GridBagLayout());
		
		PreclusionEditorPanel(LinkedHashMap data) {
			super(new BorderLayout(), true);
			
			this.filterLabel.setBorder(BorderFactory.createRaisedBevelBorder());
			this.filterLabel.setPreferredSize(new Dimension(160, 21));
			this.filterLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					selectPreclusion(-1);
					linePanel.requestFocusInWindow();
				}
			});
			
			this.messageLabel.setBorder(BorderFactory.createRaisedBevelBorder());
			this.messageLabel.setPreferredSize(new Dimension(160, 21));
			this.messageLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					selectPreclusion(-1);
					linePanel.requestFocusInWindow();
				}
			});
			
			this.isWarningLabel.setBorder(BorderFactory.createRaisedBevelBorder());
			this.isWarningLabel.setPreferredSize(new Dimension(100, 21));
			this.isWarningLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					selectPreclusion(-1);
					linePanel.requestFocusInWindow();
				}
			});
			
			this.linePanelSpacer.setBackground(Color.WHITE);
			this.linePanelSpacer.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					selectPreclusion(-1);
					linePanel.requestFocusInWindow();
				}
			});
			this.linePanel.setBorder(BorderFactory.createLineBorder(this.getBackground(), 3));
			this.linePanel.setFocusable(true);
			
	        final String upKey = "GO_UP";
	        this.linePanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), upKey);
	        this.linePanel.getActionMap().put(upKey, new AbstractAction() {
	            public void actionPerformed(ActionEvent ae) {
	                System.out.println(upKey);
	                if (selectedPreclusion > 0)
	                	selectPreclusion(selectedPreclusion - 1);
	                else if (selectedPreclusion == -1)
                		selectPreclusion(preclusions.size() - 1);
	            }
	        });
	        final String moveUpKey = "MOVE_UP";
	        this.linePanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK, true), moveUpKey);
	        this.linePanel.getActionMap().put(moveUpKey, new AbstractAction() {
	            public void actionPerformed(ActionEvent ae) {
	                System.out.println(moveUpKey);
					moveUp();
	            }
	        });
	        
	        final String downKey = "GO_DOWN";
	        this.linePanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), downKey);
	        this.linePanel.getActionMap().put(downKey, new AbstractAction() {
	            public void actionPerformed(ActionEvent ae) {
	                System.out.println(downKey);
                	if (selectedPreclusion == -1)
                		selectPreclusion(0);
                	else if ((selectedPreclusion + 1) < preclusions.size())
	                	selectPreclusion(selectedPreclusion + 1);
	            }
	        });
	        final String moveDownKey = "MOVE_DOWN";
	        this.linePanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK, true), moveDownKey);
	        this.linePanel.getActionMap().put(moveDownKey, new AbstractAction() {
	            public void actionPerformed(ActionEvent ae) {
	                System.out.println(moveDownKey);
					moveDown();
	            }
	        });

			JScrollPane linePanelBox = new JScrollPane(this.linePanel);
			
			JButton addPreclusionButton = new JButton("Add Preclusion");
			addPreclusionButton.setBorder(BorderFactory.createRaisedBevelBorder());
			addPreclusionButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					addPreclusion();
				}
			});
			
			JButton testPreclusionButton = new JButton("Test Filter");
			testPreclusionButton.setBorder(BorderFactory.createRaisedBevelBorder());
			testPreclusionButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					testFilter();
				}
			});
			
			JButton removePreclusionButton = new JButton("Remove Preclusion");
			removePreclusionButton.setBorder(BorderFactory.createRaisedBevelBorder());
			removePreclusionButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					removePreclusion();
				}
			});
			
			JPanel buttonPanel = new JPanel(new GridBagLayout(), true);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 3;
			gbc.insets.bottom = 3;
			gbc.insets.left = 3;
			gbc.insets.right = 3;
			gbc.weighty = 0;
			gbc.weightx = 1;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridy = 0;
			gbc.gridx = 0;
			buttonPanel.add(addPreclusionButton, gbc.clone());
			gbc.gridx = 1;
			buttonPanel.add(testPreclusionButton, gbc.clone());
			gbc.gridx = 2;
			buttonPanel.add(removePreclusionButton, gbc.clone());
			
			JButton upButton = new JButton("Up");
			upButton.setBorder(BorderFactory.createRaisedBevelBorder());
			upButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					moveUp();
				}
			});
			JButton downButton = new JButton("Down");
			downButton.setBorder(BorderFactory.createRaisedBevelBorder());
			downButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					moveDown();
				}
			});
			
			JPanel reorderButtonPanel = new JPanel(new GridBagLayout());
			gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 3;
			gbc.insets.right = 3;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.gridheight = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			
			gbc.gridy = 0;
			reorderButtonPanel.add(upButton, gbc.clone());
			gbc.gridy = 1;
			reorderButtonPanel.add(downButton, gbc.clone());
			
			this.add(reorderButtonPanel, BorderLayout.WEST);
			this.add(linePanelBox, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
			
			this.setContent(data);
		}
		
		void layoutPreclusions() {
			this.linePanel.removeAll();
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 0;
			gbc.insets.bottom = 0;
			gbc.insets.left = 0;
			gbc.insets.right = 0;
			gbc.weighty = 0;
			gbc.gridy = 0;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.BOTH;
			
			gbc.gridx = 0;
			gbc.weightx = 1;
			this.linePanel.add(this.filterLabel, gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 0;
			this.linePanel.add(this.messageLabel, gbc.clone());
			gbc.gridx = 2;
			this.linePanel.add(this.isWarningLabel, gbc.clone());
			gbc.gridy++;
			
			for (int l = 0; l < this.preclusions.size(); l++) {
				ImageMarkupToolPreclusion line = ((ImageMarkupToolPreclusion) this.preclusions.get(l));
				line.index = l;
				
				gbc.gridx = 0;
				gbc.weightx = 1;
				this.linePanel.add(line.filterPanel, gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 0;
				this.linePanel.add(line.messagePanel, gbc.clone());
				gbc.gridx = 2;
				this.linePanel.add(line.isWarning, gbc.clone());
				gbc.gridy++;
			}
			
			gbc.gridx = 0;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.gridwidth = 3;
			this.linePanel.add(this.linePanelSpacer, gbc.clone());
			
			this.validate();
			this.repaint();
		}
		
		void selectPreclusion(int index) {
			if (this.selectedPreclusion == index) return;
			this.selectedPreclusion = index;
			
			for (int l = 0; l < this.preclusions.size(); l++) {
				ImageMarkupToolPreclusion line = ((ImageMarkupToolPreclusion) this.preclusions.get(l));
				if (l == this.selectedPreclusion) {
					line.filterPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
					line.messagePanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
					line.isWarning.setBorder(BorderFactory.createLineBorder(Color.BLUE));
				}
				else {
					line.setEditing(false);
					line.filterPanel.setBorder(BorderFactory.createLineBorder(this.getBackground()));
					line.messagePanel.setBorder(BorderFactory.createLineBorder(this.getBackground()));
					line.isWarning.setBorder(BorderFactory.createLineBorder(this.getBackground()));
				}
			}
			
			this.linePanel.validate();
			this.linePanel.repaint();
		}
		
		void moveUp() {
			if (this.selectedPreclusion < 1)
				return;
			this.preclusions.add(this.selectedPreclusion, this.preclusions.remove(this.selectedPreclusion - 1));
			this.selectedPreclusion--;
			this.layoutPreclusions();
		}
		
		void moveDown() {
			if ((this.selectedPreclusion == -1) || ((this.selectedPreclusion + 1) == this.preclusions.size()))
				return;		
			this.preclusions.add(this.selectedPreclusion, this.preclusions.remove(this.selectedPreclusion + 1));
			this.selectedPreclusion++;
			this.layoutPreclusions();
		}
		
		void addPreclusion() {
			AddPreclusionDialog apd = new AddPreclusionDialog();
			apd.setVisible(true);
			String filter = apd.getFilter();
			if (filter != null)
				this.addPreclusion(filter, apd.getMessage(), apd.isWarning());
		}
		
		void addPreclusion(String filter, String message, boolean isWarning) {
			ImageMarkupToolPreclusion line = new ImageMarkupToolPreclusion(filter, message, isWarning);
			line.filterPanel.setBorder(BorderFactory.createLineBorder(this.getBackground()));
			line.messagePanel.setBorder(BorderFactory.createLineBorder(this.getBackground()));
			line.isWarning.setBorder(BorderFactory.createLineBorder(this.getBackground()));
			this.preclusions.add(line);
			this.layoutPreclusions();
			this.selectPreclusion(this.preclusions.size() - 1);
		}
		
		void removePreclusion() {
			if (this.selectedPreclusion == -1) return;
			int newSelectedPreclusion = this.selectedPreclusion;
			this.preclusions.remove(this.selectedPreclusion);
			this.selectedPreclusion = -1;
			this.layoutPreclusions();
			if (newSelectedPreclusion == this.preclusions.size())
				newSelectedPreclusion--;
			this.selectPreclusion(newSelectedPreclusion);
		}
		
		boolean validateFilter(String filter) {
			if (filter.length() != 0) try {
				GPathParser.parseExpression(filter);
				return true;
			} catch (Exception e) {}
			return false;
		}
		
		boolean validateMessage(String message) {
			return ((message != null) && (message.trim().length() != 0));
		}
		
		void testFilter() {
			if (this.selectedPreclusion == -1) return;
			this.testFilter(((ImageMarkupToolPreclusion) this.preclusions.get(this.selectedPreclusion)).getFilter());
		}
		void testFilter(String filter) {
			if (filter.length() == 0)
				return;
			
			QueriableAnnotation testDoc = Gamta.getTestDocument();
			if (testDoc == null)
				return;
			
			GPathExpression gpe;
			try {
				gpe = GPathParser.parseExpression(filter);
			}
			catch (Exception e) {
				DialogFactory.alert(("The filter expression is invalid:\n" + e.getMessage()), "Filter Expression Invalid", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			GPathObject gpo = GPath.evaluateExpression(gpe, testDoc, null);
			DialogFactory.alert(("The filter expression " + (gpo.asBoolean().value ? "DOES" : " DOES NOT") + " preclude the current document."), "Filter Match Result", JOptionPane.INFORMATION_MESSAGE);
		}
		
		private class ImageMarkupToolPreclusion {
			int index = 0;
			private boolean isEditing = false;
			
			private String filter;
			private boolean filterDirty = false;
			private int filterInputPressedKey = -1;
			private String message;
			private boolean messageDirty = false;
			private int messageInputPressedKey = -1;
			
			JPanel filterPanel = new JPanel(new BorderLayout(), true);
			private JLabel filterDisplay = new JLabel("", JLabel.LEFT);
			private JPanel filterEditor = new JPanel(new BorderLayout(), true);
			private JTextField filterInput = new JTextField("");
			private JButton filterTest = new JButton("Test");
			
			JPanel messagePanel = new JPanel(new BorderLayout(), true);
			private JLabel messageDisplay = new JLabel("", JLabel.LEFT);
			private JTextField messageInput = new JTextField("");
			
			JCheckBox isWarning = new JCheckBox("");
			
			ImageMarkupToolPreclusion(String filter, String message, boolean isWarning) {
				this.filter = filter;
				this.message = message;
				
				this.filterDisplay.setText(this.filter);
				this.filterDisplay.setOpaque(true);
				this.filterDisplay.setBackground(Color.WHITE);
				this.filterDisplay.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
				this.filterDisplay.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						if (me.getButton() != MouseEvent.BUTTON1) return;
						if (me.getClickCount() > 1)
							setEditing(true);
						select();
					}
				});
				
				this.filterInput.setText(this.filter);
				this.filterInput.setBorder(BorderFactory.createLoweredBevelBorder());
				this.filterInput.addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent ke) {
						filterInputPressedKey = ke.getKeyCode();
					}
					public void keyReleased(KeyEvent ke) {
						filterInputPressedKey = -1;
					}
					public void keyTyped(KeyEvent ke) {
						if (filterInputPressedKey == KeyEvent.VK_ESCAPE) {
							revertFilter();
							setEditing(false);
						}
						filterDirty = true;
					}
				});
				this.filterInput.addFocusListener(new FocusAdapter() {
					public void focusLost(FocusEvent fe) {
						updateFilter();
					}
				});
				this.filterInput.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						setEditing(false);
					}
				});
				
				this.filterTest.setBorder(BorderFactory.createRaisedBevelBorder());
				this.filterTest.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						testFilter(getFilter());
					}
				});
				
				this.filterEditor.add(this.filterInput, BorderLayout.CENTER);
				this.filterEditor.add(this.filterTest, BorderLayout.EAST);
				
				this.messageDisplay.setText(this.message);
				this.messageDisplay.setOpaque(true);
				this.messageDisplay.setBackground(Color.WHITE);
				this.messageDisplay.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
				this.messageDisplay.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						if (me.getButton() != MouseEvent.BUTTON1) return;
						if (me.getClickCount() > 1)
							setEditing(true);
						select();
					}
				});
				
				this.messageInput.setText(this.message);
				this.messageInput.setBorder(BorderFactory.createLoweredBevelBorder());
				this.messageInput.addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent ke) {
						messageInputPressedKey = ke.getKeyCode();
					}
					public void keyReleased(KeyEvent ke) {
						messageInputPressedKey = -1;
					}
					public void keyTyped(KeyEvent ke) {
						if (messageInputPressedKey == KeyEvent.VK_ESCAPE) {
							revertMessage();
							setEditing(false);
						}
						messageDirty = true;
					}
				});
				this.messageInput.addFocusListener(new FocusAdapter() {
					public void focusLost(FocusEvent fe) {
						updateMessage();
					}
				});
				this.messageInput.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						setEditing(false);
					}
				});
				
				this.isWarning.setSelected(isWarning);
				this.isWarning.setOpaque(true);
				this.isWarning.setBackground(Color.WHITE);
				this.isWarning.setHorizontalAlignment(JCheckBox.CENTER);
				this.isWarning.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
				
				this.filterPanel.setPreferredSize(new Dimension(160, 21));
				this.messagePanel.setPreferredSize(new Dimension(160, 21));
				this.isWarning.setPreferredSize(new Dimension(100, 21));
				
				this.layoutParts(false);
			}
			String getFilter() {
				this.updateFilter();
				return this.filter;
			}
			private void updateFilter() {
				if (!this.filterDirty) return;
				
				this.filter = this.filterInput.getText().trim();
				this.filterDisplay.setText(this.filter);
				if (validateFilter(this.filter)) {
					this.filterDisplay.setBackground(Color.WHITE);
					this.filterInput.setBackground(Color.WHITE);
				}
				else {
					this.filterDisplay.setBackground(Color.ORANGE);
					this.filterInput.setBackground(Color.ORANGE);
				}
				
				this.filterDirty = false;
			}
			private void revertFilter() {
				this.filterInput.setText(this.filter);
				this.filterDirty = true;
				this.updateFilter();
			}
			String getMessage() {
				this.updateMessage();
				return this.message;
			}
			private void updateMessage() {
				if (!this.messageDirty) return;
				
				this.message = this.messageInput.getText().trim();
				this.messageDisplay.setText(this.message);
				if (validateFilter(this.message)) {
					this.messageDisplay.setBackground(Color.WHITE);
					this.messageInput.setBackground(Color.WHITE);
				}
				else {
					this.messageDisplay.setBackground(Color.ORANGE);
					this.messageInput.setBackground(Color.ORANGE);
				}
				
				this.messageDirty = false;
			}
			private void revertMessage() {
				this.messageInput.setText(this.message);
				this.messageDirty = true;
				this.updateMessage();
			}
			boolean isWarning() {
				return this.isWarning.isSelected();
			}
			void setEditing(boolean editing) {
				if (this.isEditing == editing)
					return;
				if (this.isEditing) {
					this.updateFilter();
					this.updateMessage();
				}
				this.isEditing = editing;
				this.layoutParts(this.isEditing);
				if (!this.isEditing)
					linePanel.requestFocusInWindow();
			}
			void layoutParts(boolean editing) {
				this.filterPanel.removeAll();
				this.messagePanel.removeAll();
				if (editing) {
					this.filterPanel.add(this.filterEditor, BorderLayout.CENTER);
					this.messagePanel.add(this.messageInput, BorderLayout.CENTER);
				}
				else {
					this.filterPanel.add(this.filterDisplay, BorderLayout.CENTER);
					this.messagePanel.add(this.messageDisplay, BorderLayout.CENTER);
				}
				this.filterPanel.validate();
				this.filterPanel.repaint();
				this.messagePanel.validate();
				this.messagePanel.repaint();
			}
			void select() {
				selectPreclusion(this.index);
			}
		}
		
		private class AddPreclusionDialog extends DialogPanel {
			private boolean committed = true;
			private JTextField filterInput;
			private JTextField messageInput;
			private JCheckBox isWarning = new JCheckBox("");
			
			AddPreclusionDialog() {
				super("Add Preclusion", true);
				
				this.filterInput = new JTextField();
				this.filterInput.setBorder(BorderFactory.createLoweredBevelBorder());
				this.filterInput.setEditable(true);
				
				this.messageInput = new JTextField();
				this.messageInput.setBorder(BorderFactory.createLoweredBevelBorder());
				this.messageInput.setEditable(true);
				
				this.isWarning.setBorder(BorderFactory.createLoweredBevelBorder());
				this.isWarning.setHorizontalAlignment(JCheckBox.CENTER);
				
				JPanel inputPanel = new JPanel(new GridLayout(2, 3), true);
				inputPanel.add(new JLabel("Preclusion GPath Filter Expression", JLabel.CENTER));
				inputPanel.add(new JLabel("Message to Show on Match", JLabel.CENTER));
				inputPanel.add(new JLabel("Warning, not Error?", JLabel.CENTER));
				inputPanel.add(this.filterInput);
				inputPanel.add(this.messageInput);
				inputPanel.add(this.isWarning);
				
				JButton addPreclusionButton = new JButton("Add Preclusion");
				addPreclusionButton.setPreferredSize(new Dimension(100, 21));
				addPreclusionButton.setBorder(BorderFactory.createRaisedBevelBorder());
				addPreclusionButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						String filter = getFilter();
						if (!validateFilter(filter)) {
							DialogFactory.alert(("'" + filter + "' is not a valid GPath expression or filter name."), "Invalid Filter", JOptionPane.ERROR_MESSAGE);
							return;
						}
						String message = getMessage();
						if (!validateMessage(message)) {
							DialogFactory.alert(("'" + message + "' is not a valid mapping message."), "Invalid Message", JOptionPane.ERROR_MESSAGE);
							return;
						}
						dispose();
					}
				});
				
				JButton testFilterButton = new JButton("Test Filter");
				testFilterButton.setPreferredSize(new Dimension(100, 21));
				testFilterButton.setBorder(BorderFactory.createRaisedBevelBorder());
				testFilterButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						testFilter(getFilter());
					}
				});
				
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setPreferredSize(new Dimension(100, 21));
				cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						committed = false;
						dispose();
					}
				});
				
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
				buttonPanel.add(addPreclusionButton);
				buttonPanel.add(testFilterButton);
				buttonPanel.add(cancelButton);
				
				this.setLayout(new BorderLayout());
				this.add(inputPanel, BorderLayout.NORTH);
				this.add(new JLabel("<HTML>" +
						"Enter the GPath filter to map to an error message to display if the custom function is applied to a document matching the filter." +
						"</HTML>"
						), BorderLayout.CENTER);
				this.add(buttonPanel, BorderLayout.SOUTH);
				this.setSize(500, 180);
				this.setResizable(true);
				this.setLocationRelativeTo(this.getOwner());
			}
			
			String getFilter() {
				return (this.committed ? this.filterInput.getText() : null);
			}
			
			String getMessage() {
				return (this.committed ? this.messageInput.getText() : null);
			}
			
			boolean isWarning() {
				return (this.committed ? this.isWarning.isSelected() : false);
			}
		}
		void setContent(LinkedHashMap preclusions) {
			this.preclusions.clear();
			this.selectedPreclusion = -1;
			for (Iterator fit = preclusions.keySet().iterator(); fit.hasNext();) {
				String filter = ((String) fit.next());
				String message = ((String) preclusions.get(filter));
				if (message != null)
					this.addPreclusion(filter, (message.startsWith("W:") ? message.substring("W:".length()) : message), message.startsWith("W:"));
			}
			this.layoutPreclusions();
			this.selectPreclusion(0);
		}
		LinkedHashMap getPreclusions() {
			LinkedHashMap preclusions = new LinkedHashMap();
			for (int c = 0; c < this.preclusions.size(); c++) {
				ImageMarkupToolPreclusion cfp = ((ImageMarkupToolPreclusion) this.preclusions.get(c));
				String filter = cfp.getFilter();
				String message = cfp.getMessage();
				if (this.validateFilter(filter) && this.validateMessage(message))
					preclusions.put(filter, ((cfp.isWarning() ? "W:" : "") + message));
			}
			return preclusions;
		}
	}
	
	private class FilterEditorPanel extends JPanel {
		private ArrayList filters = new ArrayList();
		private int selectedFilter = -1;
		
		private JLabel filterLabel = new JLabel("Filter", JLabel.CENTER);
		private JPanel linePanelSpacer = new JPanel();
		private JPanel linePanel = new JPanel(new GridBagLayout());
		
		FilterEditorPanel(String[] filters) {
			super(new BorderLayout(), true);
			
			this.filterLabel.setBorder(BorderFactory.createRaisedBevelBorder());
			this.filterLabel.setPreferredSize(new Dimension(160, 21));
			this.filterLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					selectFilter(-1);
					linePanel.requestFocusInWindow();
				}
			});
			
			this.linePanelSpacer.setBackground(Color.WHITE);
			this.linePanelSpacer.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					selectFilter(-1);
					linePanel.requestFocusInWindow();
				}
			});
			this.linePanel.setBorder(BorderFactory.createLineBorder(this.getBackground(), 3));
			this.linePanel.setFocusable(true);
			
	        final String upKey = "GO_UP";
	        this.linePanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), upKey);
	        this.linePanel.getActionMap().put(upKey, new AbstractAction() {
	            public void actionPerformed(ActionEvent ae) {
	                System.out.println(upKey);
	                if (selectedFilter > 0)
	                	selectFilter(selectedFilter - 1);
	                else if (selectedFilter == -1)
                		selectFilter(FilterEditorPanel.this.filters.size() - 1);
	            }
	        });
	        
	        final String downKey = "GO_DOWN";
	        this.linePanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), downKey);
	        this.linePanel.getActionMap().put(downKey, new AbstractAction() {
	            public void actionPerformed(ActionEvent ae) {
	                System.out.println(downKey);
                	if (selectedFilter == -1)
                		selectFilter(0);
                	else if ((selectedFilter + 1) < FilterEditorPanel.this.filters.size())
	                	selectFilter(selectedFilter + 1);
	            }
	        });

			JScrollPane linePanelBox = new JScrollPane(this.linePanel);
			
			JButton addFilterButton = new JButton("Add Filter");
			addFilterButton.setBorder(BorderFactory.createRaisedBevelBorder());
			addFilterButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					addFilter();
				}
			});
			
			JButton testFilterButton = new JButton("Test Filter");
			testFilterButton.setBorder(BorderFactory.createRaisedBevelBorder());
			testFilterButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					testFilter();
				}
			});
			
			JButton removeFilterButton = new JButton("Remove Filter");
			removeFilterButton.setBorder(BorderFactory.createRaisedBevelBorder());
			removeFilterButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					removeFilter();
				}
			});
			
			JPanel buttonPanel = new JPanel(new GridBagLayout(), true);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 3;
			gbc.insets.bottom = 3;
			gbc.insets.left = 3;
			gbc.insets.right = 3;
			gbc.weighty = 0;
			gbc.weightx = 1;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridy = 0;
			gbc.gridx = 0;
			buttonPanel.add(addFilterButton, gbc.clone());
			gbc.gridx = 1;
			buttonPanel.add(testFilterButton, gbc.clone());
			gbc.gridx = 2;
			buttonPanel.add(removeFilterButton, gbc.clone());
			
			this.add(linePanelBox, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
			
			this.setContent(filters);
		}
		
		void layoutFilters() {
			this.linePanel.removeAll();
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 0;
			gbc.insets.bottom = 0;
			gbc.insets.left = 0;
			gbc.insets.right = 0;
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.BOTH;
			
			this.linePanel.add(this.filterLabel, gbc.clone());
			gbc.gridy++;
			
			for (int l = 0; l < this.filters.size(); l++) {
				ImageMarkupToolFilter line = ((ImageMarkupToolFilter) this.filters.get(l));
				line.index = l;
				
				this.linePanel.add(line.filterPanel, gbc.clone());
				gbc.gridy++;
			}
			
			gbc.weighty = 1;
			this.linePanel.add(this.linePanelSpacer, gbc.clone());
			
			this.validate();
			this.repaint();
		}
		
		void selectFilter(int index) {
			if (this.selectedFilter == index)
				return;
			this.selectedFilter = index;
			
			for (int l = 0; l < this.filters.size(); l++) {
				ImageMarkupToolFilter line = ((ImageMarkupToolFilter) this.filters.get(l));
				if (l == this.selectedFilter)
					line.filterPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
				else {
					line.setEditing(false);
					line.filterPanel.setBorder(BorderFactory.createLineBorder(this.getBackground()));
				}
			}
			
			this.linePanel.validate();
			this.linePanel.repaint();
		}
		
		void addFilter() {
			AddFilterDialog acd = new AddFilterDialog();
			acd.setVisible(true);
			String filter = acd.getFilter();
			if (filter != null)
				this.addFilter(filter);
		}
		
		void addFilter(String filter) {
			ImageMarkupToolFilter line = new ImageMarkupToolFilter(filter);
			line.filterPanel.setBorder(BorderFactory.createLineBorder(this.getBackground()));
			this.filters.add(line);
			this.layoutFilters();
			this.selectFilter(this.filters.size() - 1);
		}
		
		void removeFilter() {
			if (this.selectedFilter == -1) return;
			int newSelectedFilter = this.selectedFilter;
			this.filters.remove(this.selectedFilter);
			this.selectedFilter = -1;
			this.layoutFilters();
			if (newSelectedFilter == this.filters.size())
				newSelectedFilter--;
			this.selectFilter(newSelectedFilter);
		}
		
		boolean validateFilter(String filter) {
			if (filter.length() != 0) try {
				GPathParser.parseExpression(filter);
				return true;
			} catch (Exception e) {}
			return false;
		}
		
		void testFilter() {
			if (this.selectedFilter == -1) return;
			this.testFilter(((ImageMarkupToolFilter) this.filters.get(this.selectedFilter)).getFilter());
		}
		void testFilter(String filter) {
			if (filter.length() == 0)
				return;
			
			QueriableAnnotation testDoc = Gamta.getTestDocument();
			if (testDoc == null)
				return;
			
			GPathExpression gpe;
			try {
				gpe = GPathParser.parseExpression(filter);
			}
			catch (Exception e) {
				DialogFactory.alert(("The filter expression is invalid:\n" + e.getMessage()), "Filter Expression Invalid", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			ArrayList matchAnnotList = new ArrayList();
			QueriableAnnotation[] testAnnots = testDoc.getAnnotations();
			for (int a = 0; a < testAnnots.length; a++) {
				GPathObject gpo = GPath.evaluateExpression(gpe, testAnnots[a], null);
				if ((gpo != null) && gpo.asBoolean().value)
					matchAnnotList.add(testAnnots[a]);
			}
			Annotation[] matchAnnots = ((Annotation[]) matchAnnotList.toArray(new Annotation[matchAnnotList.size()]));
			
			AnnotationDisplayDialog add;
			Window top = DialogPanel.getTopWindow();
			if (top instanceof JDialog)
				add = new AnnotationDisplayDialog(((JDialog) top), "Matches of Filter", matchAnnots, true);
			else if (top instanceof JFrame)
				add = new AnnotationDisplayDialog(((JFrame) top), "Matches of Filter", matchAnnots, true);
			else add = new AnnotationDisplayDialog(((JFrame) null), "Matches of Filter", matchAnnots, true);
			add.setLocationRelativeTo(top);
			add.setVisible(true);
		}
		
		private class ImageMarkupToolFilter {
			int index = 0;
			private boolean isEditing = false;
			
			private String filter;
			private boolean filterDirty = false;
			private int filterInputPressedKey = -1;
			
			JPanel filterPanel = new JPanel(new BorderLayout(), true);
			private JLabel filterDisplay = new JLabel("", JLabel.LEFT);
			private JPanel filterEditor = new JPanel(new BorderLayout(), true);
			private JTextField filterInput = new JTextField("");
			private JButton filterTest = new JButton("Test");
			
			ImageMarkupToolFilter(String filter) {
				this.filter = filter;
				
				this.filterDisplay.setText(this.filter);
				this.filterDisplay.setOpaque(true);
				this.filterDisplay.setBackground(Color.WHITE);
				this.filterDisplay.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
				this.filterDisplay.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						if (me.getButton() != MouseEvent.BUTTON1) return;
						if (me.getClickCount() > 1)
							setEditing(true);
						select();
					}
				});
				
				this.filterInput.setText(this.filter);
				this.filterInput.setBorder(BorderFactory.createLoweredBevelBorder());
				this.filterInput.addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent ke) {
						filterInputPressedKey = ke.getKeyCode();
					}
					public void keyReleased(KeyEvent ke) {
						filterInputPressedKey = -1;
					}
					public void keyTyped(KeyEvent ke) {
						if (filterInputPressedKey == KeyEvent.VK_ESCAPE) {
							revertFilter();
							setEditing(false);
						}
						filterDirty = true;
					}
				});
				this.filterInput.addFocusListener(new FocusAdapter() {
					public void focusLost(FocusEvent fe) {
						updateFilter();
					}
				});
				this.filterInput.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						setEditing(false);
					}
				});
				
				this.filterTest.setBorder(BorderFactory.createRaisedBevelBorder());
				this.filterTest.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						testFilter(getFilter());
					}
				});
				
				this.filterEditor.add(this.filterInput, BorderLayout.CENTER);
				this.filterEditor.add(this.filterTest, BorderLayout.EAST);
				
				this.filterPanel.setPreferredSize(new Dimension(160, 21));
				
				this.layoutParts(false);
			}
			String getFilter() {
				this.updateFilter();
				return this.filter;
			}
			private void updateFilter() {
				if (!this.filterDirty) return;
				
				this.filter = this.filterInput.getText().trim();
				this.filterDisplay.setText(this.filter);
				if (validateFilter(this.filter)) {
					this.filterDisplay.setBackground(Color.WHITE);
					this.filterInput.setBackground(Color.WHITE);
				}
				else {
					this.filterDisplay.setBackground(Color.ORANGE);
					this.filterInput.setBackground(Color.ORANGE);
				}
				
				this.filterDirty = false;
			}
			private void revertFilter() {
				this.filterInput.setText(this.filter);
				this.filterDirty = true;
				this.updateFilter();
			}
			void setEditing(boolean editing) {
				if (this.isEditing == editing)
					return;
				if (this.isEditing)
					this.updateFilter();
				this.isEditing = editing;
				this.layoutParts(this.isEditing);
				if (!this.isEditing)
					linePanel.requestFocusInWindow();
			}
			void layoutParts(boolean editing) {
				this.filterPanel.removeAll();
				if (editing)
					this.filterPanel.add(this.filterEditor, BorderLayout.CENTER);
				else this.filterPanel.add(this.filterDisplay, BorderLayout.CENTER);
				this.filterPanel.validate();
				this.filterPanel.repaint();
			}
			void select() {
				selectFilter(this.index);
			}
		}
		
		private class AddFilterDialog extends DialogPanel {
			private boolean committed = true;
			private JTextField filterInput;
			
			AddFilterDialog() {
				super("Add Filter", true);
				
				this.filterInput = new JTextField();
				this.filterInput.setBorder(BorderFactory.createLoweredBevelBorder());
				this.filterInput.setEditable(true);
				
				JPanel inputPanel = new JPanel(new BorderLayout(), true);
				inputPanel.add(this.filterInput, BorderLayout.CENTER);
				
				JButton addFilterButton = new JButton("Add Filter");
				addFilterButton.setPreferredSize(new Dimension(100, 21));
				addFilterButton.setBorder(BorderFactory.createRaisedBevelBorder());
				addFilterButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						String filter = getFilter();
						if (!validateFilter(filter)) {
							DialogFactory.alert(("'" + filter + "' is not a valid GPath expression or filter name."), "Invalid Filter", JOptionPane.ERROR_MESSAGE);
							return;
						}
						dispose();
					}
				});
				
				JButton testFilterButton = new JButton("Test Filter");
				testFilterButton.setPreferredSize(new Dimension(100, 21));
				testFilterButton.setBorder(BorderFactory.createRaisedBevelBorder());
				testFilterButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						testFilter(getFilter());
					}
				});
				
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setPreferredSize(new Dimension(100, 21));
				cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						committed = false;
						dispose();
					}
				});
				
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
				buttonPanel.add(addFilterButton);
				buttonPanel.add(testFilterButton);
				buttonPanel.add(cancelButton);
				
				this.setLayout(new BorderLayout());
				this.add(inputPanel, BorderLayout.NORTH);
				this.add(new JLabel("<HTML>" +
						"Enter the filter to whose matches the custom function is applicable.<br>" +
						"The filter can be changed later in the table.</HTML>"
						), BorderLayout.CENTER);
				this.add(buttonPanel, BorderLayout.SOUTH);
				this.setSize(500, 180);
				this.setResizable(true);
				this.setLocationRelativeTo(this.getOwner());
			}
			
			String getFilter() {
				return (this.committed ? this.filterInput.getText().trim() : null);
			}
		}
		
		void setContent(String[] filters) {
			this.filters.clear();
			this.selectedFilter = -1;
			for (int f = 0; f < filters.length; f++)
				this.addFilter(filters[f]);
			this.layoutFilters();
			this.selectFilter(0);
		}
		
		String[] getFilters() {
			ArrayList filters = new ArrayList(this.filters.size());
			
			for (int c = 0; c < this.filters.size(); c++) {
				ImageMarkupToolFilter mc = ((ImageMarkupToolFilter) this.filters.get(c));
				String filter = mc.getFilter();
				if (this.validateFilter(filter))
					filters.add(filter);
			}
			
			return ((String[]) filters.toArray(new String[filters.size()]));
		}
	}
	
}