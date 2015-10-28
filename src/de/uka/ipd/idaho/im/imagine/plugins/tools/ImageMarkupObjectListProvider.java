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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.defaultImplementation.PlainTokenSequence;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathParser;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
import de.uka.ipd.idaho.gamta.util.swing.AttributeEditor;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilter;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.HelpEditorDialog;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
import de.uka.ipd.idaho.im.imagine.GoldenGateImagine;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.basic.AttributeToolProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Annotation viewer listing annotations in a table. The displayed annotations
 * can be filtered through custom filters that may be changed in an ad-hoc
 * fashion. The listing allows for merging annotations and editing their
 * attributes, among others. This view is both helpful for sorting out
 * annotations, and for finding ones with specific error conditions, e.g. a
 * lacking attribute.<br>
 * To simplify matters for users, administrative mode allows to pre-configure
 * listings, which are then readily accessible through the menu.
 * If none of these parameters is specified, the user can select or enter
 * filters manually in an extra panel at the top of the dialog.
 * 
 * @author sautter
 */
public class ImageMarkupObjectListProvider extends AbstractResourceManager implements ImageMarkupToolProvider, SelectionActionProvider {
	
	private static final String FILE_EXTENSION = ".imObjectList";
	
	private static final String ANNOT_LISTER_IMT_NAME = "ListAnnots";
	private static final String REGION_LISTER_IMT_NAME = "ListRegions";
	
	private ImageMarkupTool annotLister = new AnnotLister();
	private ImageMarkupTool regionLister = new RegionLister();
	
	private AttributeToolProvider attributeToolProvider;
	
	/** zero-argument constructor for class loading */
	public ImageMarkupObjectListProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "IM Object List Provider";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	load custom filter history
		try {
			InputStream is = this.dataProvider.getInputStream("customFilterHistory.cnfg");
			this.customFilterHistory.addContent(StringVector.loadList(is));
			is.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin#setImagineParent(de.uka.ipd.idaho.im.imagine.GoldenGateImagine)
	 */
	public void setImagineParent(GoldenGateImagine ggImagine) { /* we don't seem to need this one here */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractGoldenGateImaginePlugin#initImagine()
	 */
	public void initImagine() {
		
		//	connect to attribute tool provider
		this.attributeToolProvider = ((AttributeToolProvider) this.parent.getPlugin(AttributeToolProvider.class.getName()));
		if (this.attributeToolProvider == null)
			throw new RuntimeException("Cannot work without attribute tools");
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#exit()
	 */
	public void exit() {
		
		//	store custom filter history if possible
		if (this.dataProvider.isDataEditable("customFilterHistory.cnfg")) try {
			OutputStream os = this.dataProvider.getOutputStream("customFilterHistory.cnfg");
			this.customFilterHistory.storeContent(os);
			os.flush();
			os.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#isOperational()
	 */
	public boolean isOperational() {
		return !GraphicsEnvironment.isHeadless();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "IM Object List";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getDataNamesForResource(java.lang.String)
	 */
	public String[] getDataNamesForResource(String name) {
		String[] dns = {name, (name + ".help.html")};
		return dns;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(ImWord start, ImWord end, ImDocumentMarkupPanel idmp) {

		//	we're single word selection only
		if (start != end)
			return null;
		
		//	collect painted annotations spanning or overlapping whole selection
		ImAnnotation[] spanningAnnots = idmp.document.getAnnotationsSpanning(start, end);
		final TreeSet spanningAnnotTypes = new TreeSet();
		for (int a = 0; a < spanningAnnots.length; a++) {
			if (idmp.areAnnotationsPainted(spanningAnnots[a].getType()))
				spanningAnnotTypes.add(spanningAnnots[a].getType());
		}
		
		//	nothing to work with
		if (spanningAnnotTypes.size() == 0)
			return null;
		
		//	collect available actions
		LinkedList actions = new LinkedList();
		
		//	a single annotation to work with
		if (spanningAnnotTypes.size() == 1) {
			final String annotType = ((String) spanningAnnotTypes.first());
			actions.add(new SelectionAction("listAnnots", ("List " + annotType + " Annotations"), ("Open a list of all " + annotType + " annotations")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					invoker.applyMarkupTool(new AnnotTypeLister(annotType), null);
					return true;
				}
			});
		}
		
		//	multiple annotation types to work with
		else actions.add(new SelectionAction("listAnnots", "List Annotations ...", ("Open a list of annotations from the document")) {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				return false;
			}
			public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
				JMenu pm = new JMenu("List Annotations");
				for (Iterator atit = spanningAnnotTypes.iterator(); atit.hasNext();) {
					final String annotType = ((String) atit.next());
					JMenuItem mi = new JMenuItem("- " + annotType);
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							invoker.applyMarkupTool(new AnnotTypeLister(annotType), null);
						}
					});
					pm.add(mi);
				}
				return pm;
			}
		});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, ImDocumentMarkupPanel idmp) {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider#getEditMenuItemNames()
	 */
	public String[] getEditMenuItemNames() {
		String[] emins = {ANNOT_LISTER_IMT_NAME, REGION_LISTER_IMT_NAME};
		return emins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		return this.getResourceNames();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (ANNOT_LISTER_IMT_NAME.equals(name))
			return this.annotLister;
		else if (REGION_LISTER_IMT_NAME.equals(name))
			return this.regionLister;
		else return this.getCustomLister(name);
	}
	
	private class AnnotLister implements ImageMarkupTool {
		public String getLabel() {
			return "List Annotations";
		}
		public String getTooltip() {
			return "List annotations from document";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we're only processing the document as a whole
			if (annot != null)
				return;
			
			//	list annotations on paragraph level normalized document
			listImObjects("Annotation List", doc, ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS, idmp, null);
		}
	}
	
	private class AnnotTypeLister implements ImageMarkupTool {
		private String annotType;
		AnnotTypeLister(String annotType) {
			this.annotType = annotType;
		}
		public String getLabel() {
			return ("List " + this.annotType + " Annotations");
		}
		public String getTooltip() {
			return ("List " + this.annotType + " annotations present in document");
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we're only processing the document as a whole
			if (annot != null)
				return;
			
			//	list annotations of matching type on paragraph level normalized document
			listImObjects("Annotation List", doc, ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS, idmp, getGPathFilter(this.annotType));
		}
	}
	
	private class RegionLister implements ImageMarkupTool {
		public String getLabel() {
			return "List Regions";
		}
		public String getTooltip() {
			return "List regions from document";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we're only processing the document as a whole
			if (annot != null)
				return;
			
			//	list regions on un-normalized normalized document
			listImObjects("Region List", doc, ImDocumentRoot.NORMALIZATION_LEVEL_RAW, idmp, null);
		}
	}
	
	private CustomImObjectLister getCustomLister(String name) {
		Settings cl = this.loadSettingsResource(name);
		if ((cl == null) || cl.isEmpty())
			return null;
		int normalizationLevel;
		try {
			normalizationLevel = Integer.parseInt(cl.getSetting(NORMALIZATION_LEVEL_ATTRIBUTE));
		} catch (RuntimeException re) { return null; }
		String label = cl.getSetting(LABEL_ATTRIBUTE);
		String tooltip = cl.getSetting(TOOLTIP_ATTRIBUTE);
		String filter = cl.getSetting(FILTER_ATTRIBUTE);
		return new CustomImObjectLister(name, label, tooltip, normalizationLevel, filter);
	}
	
	private class CustomImObjectLister implements ImageMarkupTool {
		private String name;
		private String label;
		private String tooltip;
		private int normalizationLevel;
		private String filterString;
		private AnnotationFilter filter;
		CustomImObjectLister(String name, String label, String tooltip, int normalizationLevel, String filterString) {
			this.name = name;
			this.label = label;
			this.tooltip = tooltip;
			this.normalizationLevel = normalizationLevel;
			this.filterString = filterString;
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
			
			//	we're only processing the document as a whole
			if (annot != null)
				return;
			
			//	get filter
			if (this.filter == null)
				this.filter = getGPathFilter(this.filterString);
			if (this.filter == null)
				return;
			
			//	offer editing annotations on paragraph level normalized document
			listImObjects(this.label, doc, this.normalizationLevel, idmp, this.filter);
		}
	}
	
	private AnnotationFilter getGPathFilter(String filterString) {
		
		//	validate & compile path expression
		String error = GPathParser.validatePath(filterString);
		GPath filterPath = null;
		if (error == null) try {
			filterPath = new GPath(filterString);
		}
		catch (Exception e) {
			error = e.getMessage();
		}
		
		//	validation successful
		if (error == null)
			return new GPathAnnotationFilter(filterString, filterPath);
		
		//	validation error
		else {
			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("The filter expression is not valid:\n" + error), "GPath Validation", JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}
	
	private class GPathAnnotationFilter implements AnnotationFilter {
		private String filterString;
		private GPath filterPath;
		GPathAnnotationFilter(String filterString, GPath filterPath) {
			this.filterString = filterString;
			this.filterPath = filterPath;
		}
		public boolean accept(Annotation annotation) {
			return false;
		}
		public QueriableAnnotation[] getMatches(QueriableAnnotation data) {
			try {
				return this.filterPath.evaluate(data, GPath.getDummyVariableResolver());
			}
			catch (GPathException gpe) {
				JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), gpe.getMessage(), "GPath Error", JOptionPane.ERROR_MESSAGE);
				return new QueriableAnnotation[0];
			}
		}
		public MutableAnnotation[] getMutableMatches(MutableAnnotation data) {
			QueriableAnnotation[] matches = this.getMatches(data);
			Set matchIDs = new HashSet();
			for (int m = 0; m < matches.length; m++)
				matchIDs.add(matches[m].getAnnotationID());
			MutableAnnotation[] mutableAnnotations = data.getMutableAnnotations();
			ArrayList mutableMatches = new ArrayList();
			for (int m = 0; m < mutableAnnotations.length; m++)
				if (matchIDs.contains(mutableAnnotations[m].getAnnotationID()))
					mutableMatches.add(mutableAnnotations[m]);
			return ((MutableAnnotation[]) mutableMatches.toArray(new MutableAnnotation[mutableMatches.size()]));
		}
		public String getName() {
			return this.filterString;
		}
		public String getProviderClassName() {
			return "Homegrown";
		}
		public String getTypeLabel() {
			return "Custom Filter";
		}
		public boolean equals(Object obj) {
			return ((obj != null) && this.filterString.equals(obj.toString()));
		}
		public String toString() {
			return this.filterString;
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Image Markup Object Lists";
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
				createCustomLister();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				editCustomListers();
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	private boolean createCustomLister() {
		return (this.createCustomLister(null, null) != null);
	}
	
	private boolean cloneCustomLister() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createCustomLister();
		else {
			String name = "New " + selectedName;
			return (this.createCustomLister(this.getCustomLister(selectedName), name) != null);
		}
	}
	
	private String createCustomLister(CustomImObjectLister modelCol, String name) {
		CreateCustomListerDialog ccld = new CreateCustomListerDialog(modelCol, name);
		ccld.setVisible(true);
		
		if (ccld.isCommitted()) {
			Settings col = ccld.getCustomLister();
			String colName = ccld.getImageMarkupToolName();
			if (!colName.endsWith(FILE_EXTENSION)) colName += FILE_EXTENSION;
			try {
				if (this.storeSettingsResource(colName, col)) {
					this.resourceNameList.refresh();
					return colName;
				}
			} catch (IOException ioe) {}
		}
		return null;
	}
	
	private void editCustomListers() {
		final CustomListerEditorPanel[] editor = new CustomListerEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Image Markup Object Lists", true);
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
				createCustomLister();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				cloneCustomLister();
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
			CustomImObjectLister cl = this.getCustomLister(selectedName);
			if (cl == null)
				editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
			else {
				editor[0] = new CustomListerEditorPanel(selectedName, cl);
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
						if (JOptionPane.showConfirmDialog(editDialog, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].name + "\nProceed?"), "Could Not Save Analyzer", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
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
					CustomImObjectLister cl = getCustomLister(dataName);
					if (cl == null)
						editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
					else {
						editor[0] = new CustomListerEditorPanel(dataName, cl);
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
	
	private static final String LABEL_ATTRIBUTE = "LABEL";
	private static final String TOOLTIP_ATTRIBUTE = "TOOLTIP";
	
	private static final String NORMALIZATION_LEVEL_ATTRIBUTE = "NORMALIZATION_LEVEL";
	private static final String FILTER_ATTRIBUTE = "FILTER";
	
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
	
	private class CreateCustomListerDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private CustomListerEditorPanel editor;
		private String clName = null;
		
		CreateCustomListerDialog(CustomImObjectLister col, String name) {
			super("Create Image Markup Object Lists", true);
			
			this.nameField = new JTextField((name == null) ? "New Image Markup Object Lists" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					clName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					clName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new CustomListerEditorPanel(name, col);
			
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
			return (this.clName != null);
		}
		
		Settings getCustomLister() {
			return this.editor.getSettings();
		}
		
		String getImageMarkupToolName() {
			return this.clName;
		}
	}
	
	private class CustomListerEditorPanel extends JPanel implements DocumentListener, ItemListener {
		
		private String name;
		
		private boolean dirty = false;
		private boolean helpTextDirty = false;
		
		private JTextField label = new JTextField();
		private JTextField toolTip = new JTextField();
		private String helpText;
		private JButton editHelpText = new JButton("Edit Help Text");
		
		private JComboBox normalizationLevel = new JComboBox(NORMALIZATION_LEVELS);
		private JTextField filter = new JTextField();
		
		CustomListerEditorPanel(String name, CustomImObjectLister col) {
			super(new BorderLayout(), true);
			this.name = name;
			this.add(getExplanationLabel(), BorderLayout.CENTER);
			
			this.normalizationLevel.setEditable(false);
			this.normalizationLevel.setSelectedItem(PARAGRAPH_NORMALIZATION_LEVEL);
			
			if (col != null) {
				this.label.setText(col.label);
				this.toolTip.setText(col.tooltip);
				this.helpText = col.getHelpText();
				if (col.normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_RAW)
					this.normalizationLevel.setSelectedItem(RAW_NORMALIZATION_LEVEL);
				else if (col.normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_WORDS)
					this.normalizationLevel.setSelectedItem(WORD_NORMALIZATION_LEVEL);
				else if (col.normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS)
					this.normalizationLevel.setSelectedItem(STREAM_NORMALIZATION_LEVEL);
				else this.normalizationLevel.setSelectedItem(PARAGRAPH_NORMALIZATION_LEVEL);
				this.filter.setText(col.filterString);
			}
			
			this.label.getDocument().addDocumentListener(this);
			this.toolTip.getDocument().addDocumentListener(this);
			this.editHelpText.setBorder(BorderFactory.createRaisedBevelBorder());
			this.editHelpText.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editHelpText();
				}
			});
			
			this.normalizationLevel.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			this.filter.getDocument().addDocumentListener(this);
			
			JButton testFilter = new JButton("Test");
			testFilter.setBorder(BorderFactory.createRaisedBevelBorder());
			testFilter.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					testFilter(filter.getText());
				}
			});
			JPanel filterPanel = new JPanel(new BorderLayout(), true);
			filterPanel.add(this.filter, BorderLayout.CENTER);
			filterPanel.add(testFilter, BorderLayout.EAST);
			
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
			functionPanel.add(new JLabel("Object List Explanation", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.gridwidth = 2;
			gbc.weightx = 2;
			functionPanel.add(this.toolTip, gbc.clone());
			
			gbc.gridy ++;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			functionPanel.add(new JLabel("Use Document Normalization Level ...", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 2;
			functionPanel.add(this.normalizationLevel, gbc.clone());
			
			gbc.gridy ++;
			gbc.gridx = 0;
			gbc.gridwidth = 1;
			gbc.weightx = 0;
			functionPanel.add(new JLabel("Use Filter ...", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 2;
			functionPanel.add(filterPanel, gbc.clone());
			
			this.add(functionPanel, BorderLayout.SOUTH);
		}
		
		void testFilter(String filter) {
			QueriableAnnotation testDoc = Gamta.getTestDocument();
			if (testDoc == null)
				return;
			
			AnnotationFilter af = getGPathFilter(filter);
			if (af == null)
				return;
			
			Annotation[] data = af.getMatches(testDoc);
			
			AnnotationDisplayDialog add;
			Window top = DialogPanel.getTopWindow();
			if (top instanceof JDialog)
				add = new AnnotationDisplayDialog(((JDialog) top), "Matches of Filter", data, true);
			else if (top instanceof JFrame)
				add = new AnnotationDisplayDialog(((JFrame) top), "Matches of Filter", data, true);
			else add = new AnnotationDisplayDialog(((JFrame) null), "Matches of Filter", data, true);
			add.setLocationRelativeTo(top);
			add.setVisible(true);
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
		
		private void editHelpText() {
			HelpEditorDialog hed = new HelpEditorDialog(("Edit Help Text for '" + this.name + "'"), this.helpText);
			hed.setVisible(true);
			if (hed.isCommitted()) {
				this.helpText = hed.getHelpText();
				this.helpTextDirty = true;
			}
		}
		
		Settings getSettings() {
			Settings set = new Settings();
			
			String label = this.label.getText();
			if (label.trim().length() != 0)
				set.setSetting(LABEL_ATTRIBUTE, label);
			
			String toolTip = this.toolTip.getText();
			if (toolTip.trim().length() != 0)
				set.setSetting(TOOLTIP_ATTRIBUTE, toolTip);
			
			if (RAW_NORMALIZATION_LEVEL.equals(this.normalizationLevel.getSelectedItem()))
				set.setSetting(NORMALIZATION_LEVEL_ATTRIBUTE, ("" + ImDocumentRoot.NORMALIZATION_LEVEL_RAW));
			else if (WORD_NORMALIZATION_LEVEL.equals(this.normalizationLevel.getSelectedItem()))
				set.setSetting(NORMALIZATION_LEVEL_ATTRIBUTE, ("" + ImDocumentRoot.NORMALIZATION_LEVEL_WORDS));
			else if (STREAM_NORMALIZATION_LEVEL.equals(this.normalizationLevel.getSelectedItem()))
				set.setSetting(NORMALIZATION_LEVEL_ATTRIBUTE, ("" + ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS));
			else set.setSetting(NORMALIZATION_LEVEL_ATTRIBUTE, ("" + ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS));
			
			String filter = this.filter.getText();
			if (filter.trim().length() != 0)
				set.setSetting(FILTER_ATTRIBUTE, filter);
			
			if (this.helpTextDirty && (this.helpText != null)) try {
				storeStringResource((this.name + ".help.html"), this.helpText);
			} catch (IOException ioe) {}
			
			return set;
		}
	}
	
	/**
	 * Open a list of Image Markup objects. Depending on the argument normalization
	 * level, the listed objects are regions or annotations. If the argument
	 * filter is null, users can ad-hoc enter custom filter expressions in the
	 * list dialog, which is helpful for exploring a document.
	 * @param title the title for the list dialog
	 * @param doc the document to list objects from
	 * @param normalizationLevel the normalization level to use
	 * @param idmp the markup panel hosting the document
	 * @param filter the filter to use
	 */
	public void listImObjects(String title, ImDocument doc, int normalizationLevel, ImDocumentMarkupPanel idmp, AnnotationFilter filter) {
		
		//	wrap document
		ImDocumentRoot wrappedDoc = new ImDocumentRoot(doc, (normalizationLevel | ImDocumentRoot.SHOW_TOKENS_AS_WORD_ANNOTATIONS | ImDocumentRoot.USE_RANDOM_ANNOTATION_IDS));
		
		//	are we showing layout objects?
		boolean showingLayoutObjects = ((normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_RAW) || (normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_WORDS));
		
		//	open list viewer around wrapper, and work on wrapper (it all goes through to document anyway)
		ObjectListDialog ond = new ObjectListDialog(title, wrappedDoc, showingLayoutObjects, idmp, filter);
		ond.setVisible(true);
		
		//	show any changes
		idmp.validate();
		idmp.repaint();
	}
	
	private StringVector customFilterHistory = new StringVector();
	private HashMap customFiltersByName = new HashMap();
	private int customFilterHistorySize = 10;
	
	//	TODO give value as 'head ... tail' instead of 'head ...'
	
	private class ObjectListDialog extends DialogPanel {
		
		private ObjectListPanel objectDisplay;
		
		private String originalTitle;
		private boolean showingLayoutObjects;
		
		private JComboBox filterSelector = new JComboBox();
		private boolean customFilterSelectorKeyPressed = false;
		
		ObjectListDialog(String title, MutableAnnotation wrappedDoc, boolean showingLayoutObjects, ImDocumentMarkupPanel target, AnnotationFilter filter) {
			super(title, true);
			this.originalTitle = title;
			this.showingLayoutObjects = showingLayoutObjects;
			
			JButton closeButton = new JButton("Close");
			closeButton.setBorder(BorderFactory.createRaisedBevelBorder());
			closeButton.setPreferredSize(new Dimension(100, 21));
			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					ObjectListDialog.this.dispose();
				}
			});
			
			//	create editor
			this.objectDisplay = new ObjectListPanel(wrappedDoc, showingLayoutObjects, target);
			
			//	set filter if given
			if (filter != null)
				this.objectDisplay.setFilter(filter);
			
			//	put the whole stuff together
			if (filter == null)
				this.add(this.buildFilterPanel(wrappedDoc, target), BorderLayout.NORTH);
			this.add(this.objectDisplay, BorderLayout.CENTER);
			this.add(closeButton, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(500, 650));
			this.setLocationRelativeTo(this.getOwner());
		}
		
		private JPanel buildFilterPanel(MutableAnnotation data, ImDocumentMarkupPanel target) {
			
			StringVector filters = new StringVector();
			TreeSet filterTypes = new TreeSet();
			for (int f = 0; f < customFilterHistory.size(); f++) {
				String cf = customFilterHistory.get(f);
				if ((cf.indexOf('[') == -1) && (cf.indexOf('/') == -1))
					filters.addElementIgnoreDuplicates(cf);
				else filterTypes.add(cf);
			}
			filters.addContentIgnoreDuplicates(customFilterHistory);
			if (this.showingLayoutObjects) {
				String[] layoutObjectTypes = target.getLayoutObjectTypes();
				for (int t = 0; t < layoutObjectTypes.length; t++) {
					if (target.areRegionsPainted(layoutObjectTypes[t]))
						filterTypes.add(layoutObjectTypes[t]);
				}
			}
			else {
				String[] annotTypes = target.getAnnotationTypes();
				for (int t = 0; t < annotTypes.length; t++) {
					if (target.areAnnotationsPainted(annotTypes[t]))
						filterTypes.add(annotTypes[t]);
				}
			}
			for (Iterator ftit = filterTypes.iterator(); ftit.hasNext();)
				filters.addElementIgnoreDuplicates((String) ftit.next());
			
			this.filterSelector.setModel(new DefaultComboBoxModel(filters.toStringArray()));
			this.filterSelector.setEditable(true);
			this.filterSelector.setSelectedItem("");
			this.filterSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.filterSelector.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (!filterSelector.isVisible())
						return;
					else if (customFilterSelectorKeyPressed && !filterSelector.isPopupVisible())
						ObjectListDialog.this.applyFilter();
				}
			});
			((JTextComponent) this.filterSelector.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent ke) {
					customFilterSelectorKeyPressed = true;
				}
				public void keyReleased(KeyEvent ke) {
					customFilterSelectorKeyPressed = false;
				}
			});
			
			JButton applyFilterButton = new JButton("Apply");
			applyFilterButton.setBorder(BorderFactory.createRaisedBevelBorder());
			applyFilterButton.setPreferredSize(new Dimension(50, 21));
			applyFilterButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					ObjectListDialog.this.applyFilter();
				}
			});
			
			JPanel filterPanel = new JPanel(new BorderLayout());
			filterPanel.add(this.filterSelector, BorderLayout.CENTER);
			filterPanel.add(applyFilterButton, BorderLayout.EAST);
			return filterPanel;
		}
		
		void applyFilter() {
			Object filterObject = this.filterSelector.getSelectedItem();
			if (filterObject != null) {
				final String filterString = filterObject.toString().trim();
				
				//	filter from history selected
				if (customFiltersByName.containsKey(filterString))
					this.objectDisplay.setFilter((AnnotationFilter) customFiltersByName.get(filterString));
				
				//	new filter entered
				else {
					
					//	validate & compile path expression
					String error = GPathParser.validatePath(filterString);
					GPath filterPath = null;
					if (error == null) try {
						filterPath = new GPath(filterString);
					}
					catch (Exception e) {
						error = e.getMessage();
					}
					
					//	validation successful
					if (error == null) {
						
						//	create & cache filter
						AnnotationFilter filter = new GPathAnnotationFilter(filterString, filterPath);
						customFiltersByName.put(filterString, filter);
						
						//	make way
						customFilterHistory.removeAll(filterString);
						this.filterSelector.removeItem(filterString);
						
						//	store new filter in history
						customFilterHistory.insertElementAt(filterString, 0);
						this.filterSelector.insertItemAt(filterString, 0);
						this.filterSelector.setSelectedIndex(0);
						
						//	shrink history
						while (customFilterHistory.size() > customFilterHistorySize) {
							customFiltersByName.remove(customFilterHistory.get(customFilterHistorySize));
							customFilterHistory.remove(customFilterHistorySize);
							filterSelector.removeItemAt(customFilterHistorySize);
						}
						
						//	apply filter
						this.objectDisplay.setFilter(filter);
					}
					
					//	path validation error
					else {
						JOptionPane.showMessageDialog(this, ("The expression is not valid:\n" + error), "GPath Validation", JOptionPane.ERROR_MESSAGE);
						this.filterSelector.requestFocusInWindow();
					}
				}
			}
		}
		
		private class ObjectListPanel extends JPanel {
			
			private Dimension attributeDialogSize = new Dimension(400, 300);
			private Point attributeDialogLocation = null;
			
			private JTable objectTable;
			
			private ObjectTray[] objectTrays;
			private HashMap objectTraysByID = new HashMap();
			
			private MutableAnnotation wrappedDoc;
			private ImDocumentMarkupPanel target;
			private boolean showingLayoutObjects;
			private String layoutObjectName;
			
			private AnnotationFilter filter = null;
			private int matchObjectCount = 0;
			private boolean singleTypeMatch = false;
			
			private JRadioButton showMatches = new JRadioButton("Show Matches Only", true);
			private JRadioButton highlightMatches = new JRadioButton("Highlight Matches", false);
			
			private JButton mergeButton;
			private JButton renameButton;
			private JButton removeButton;
			private JButton deleteButton;
			private JButton editAttributesButton;
			private JButton renameAttributeButton;
			private JButton modifyAttributeButton;
			private JButton removeAttributeButton;
			
			private int sortColumn = -1;
			private boolean sortDescending = false;
			
			ObjectListPanel(MutableAnnotation data, boolean showingLayoutObjects, ImDocumentMarkupPanel target) {
				super(new BorderLayout(), true);
				this.setBorder(BorderFactory.createEtchedBorder());
				this.wrappedDoc = data;
				this.target = target;
				this.showingLayoutObjects = showingLayoutObjects;
				this.layoutObjectName = (this.showingLayoutObjects ? "Region" : "Annotation");
				
				this.objectTable = new JTable();
				this.objectTable.setDefaultRenderer(Object.class, new TooltipAwareComponentRenderer(5, data.getTokenizer()));
				this.objectTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
				this.objectTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent lse) {
						adjustMenu();
					}
				});
				
				final JTableHeader header = this.objectTable.getTableHeader();
				header.setReorderingAllowed(false);
				header.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
		                int newSortColumn = header.columnAtPoint(me.getPoint());
		                if (newSortColumn == sortColumn)
		                	sortDescending = !sortDescending;
		                else {
		                	sortDescending = false;
		                	sortColumn = newSortColumn;
		                }
		                sortObjects();
					}
				});
				
				this.refreshAnnotations();
				
				JScrollPane annotationTableBox = new JScrollPane(this.objectTable);
				
				this.showMatches.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (showMatches.isSelected()) {
							refreshAnnotations();
						}
					}
				});
				this.highlightMatches.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						if (highlightMatches.isSelected()) {
							refreshAnnotations();
						}
					}
				});
				
				ButtonGroup displayModeButtonGroup = new ButtonGroup();
				displayModeButtonGroup.add(this.showMatches);
				displayModeButtonGroup.add(this.highlightMatches);
				
				JPanel displayModePanel = new JPanel(new GridLayout(1, 2));
				displayModePanel.add(this.showMatches);
				displayModePanel.add(this.highlightMatches);
				
				JPanel functionPanel = new JPanel(new BorderLayout());
				functionPanel.add(this.buildMenu(), BorderLayout.NORTH);
				functionPanel.add(displayModePanel, BorderLayout.SOUTH);
				
				this.add(functionPanel, BorderLayout.NORTH);
				this.add(annotationTableBox, BorderLayout.CENTER);
			}
			
			private JPanel buildMenu() {
				this.mergeButton = new JButton("Merge");
				this.mergeButton.setToolTipText("Merge " + layoutObjectName.toLowerCase() + "s, i.e., change their type");
				this.mergeButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.mergeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						Annotation[] annotations = getSelectedObjects();
						if (annotations.length < 2)
							return;
						int start = annotations[0].getStartIndex();
						int end = annotations[0].getEndIndex();
						for (int a = 1; a < annotations.length; a++) {
							start = Math.min(start, annotations[a].getStartIndex());
							end = Math.max(end, annotations[a].getEndIndex());
						}
						Annotation mAnnotation = null;
						for (int a = 0; a < annotations.length; a++)
							if ((annotations[a].getStartIndex() == start) && (annotations[a].getEndIndex() == end)) {
								mAnnotation = annotations[a];
								break;
							}
						if (mAnnotation == null)
							mAnnotation = wrappedDoc.addAnnotation(annotations[0].getType(), start, (end - start));
						for (int a = 0; a < annotations.length; a++) {
							AttributeUtils.copyAttributes(annotations[a], mAnnotation, AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
							if (annotations[a] != mAnnotation)
								wrappedDoc.removeAnnotation(annotations[a]);
						}
						refreshAnnotations();
					}
				});
				
				this.renameButton = new JButton("Rename");
				this.renameButton.setToolTipText("Rename " + layoutObjectName.toLowerCase() + "s, i.e., change their type");
				this.renameButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.renameButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						Annotation[] annotations = getSelectedObjects();
						if (annotations.length == 0)
							return;
						String[] types = ((target == null) ? wrappedDoc.getAnnotationTypes() : target.getAnnotationTypes());
						Arrays.sort(types, ANNOTATION_TYPE_ORDER);
						String newType = ImUtils.promptForObjectType(("Enter New " + layoutObjectName + " Type"), ("Enter or select new " + layoutObjectName.toLowerCase() + " type"), types, annotations[0].getType(), true);
						if (newType == null)
							return;
						for (int a = 0; a < annotations.length; a++)
							annotations[a].changeTypeTo(newType);
						refreshAnnotations();
						if (showingLayoutObjects)
							target.setRegionsPainted(newType, true);
						else target.setAnnotationsPainted(newType, true);
					}
				});
				
				this.removeButton = new JButton("Remove");
				this.removeButton.setToolTipText("Remove " + layoutObjectName.toLowerCase() + "s from the document");
				this.removeButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.removeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						Annotation[] annotations = getSelectedObjects();
						if (annotations.length != 0) {
							for (int a = 0; a < annotations.length; a++)
								wrappedDoc.removeAnnotation(annotations[a]);
							refreshAnnotations();
						}
					}
				});
				
				this.deleteButton = new JButton("Delete");
				this.deleteButton.setToolTipText("Delete " + layoutObjectName.toLowerCase() + "s, including their content");
				this.deleteButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.deleteButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						Annotation[] annotations = getSelectedObjects();
						if (annotations.length != 0) {
							for (int a = 0; a < annotations.length; a++)
								wrappedDoc.removeTokens(annotations[a]);
							refreshAnnotations();
						}
					}
				});
				
				this.editAttributesButton = new JButton("Edit Attributes");
				this.editAttributesButton.setToolTipText("Edit " + layoutObjectName.toLowerCase() + " attributes");
				this.editAttributesButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.editAttributesButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
//						Annotation[] annotations = getSelectedObjects();
//						if (annotations.length == 1)
//							editObjectAttributes(annotations[0]);
						int[] selectedRows = objectTable.getSelectedRows();
						if (selectedRows.length == 1)
							editObjectAttributes(selectedRows[0]);
						/* TODO
						 * - facilitate Previous-ing and Next-ing through object list with attributes dialog
						 * - 'Previous' and 'Next' both imply 'OK'
						 * - use current sort order
						 */
					}
				});
				
				this.renameAttributeButton = new JButton("Rename Attribute");
				this.renameAttributeButton.setToolTipText("Rename an attribute of " + layoutObjectName.toLowerCase() + "s");
				this.renameAttributeButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.renameAttributeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						renameObjectAttribute();
					}
				});
				
				this.modifyAttributeButton = new JButton("Modify Attribute");
				this.modifyAttributeButton.setToolTipText("Modify an attribute of " + layoutObjectName.toLowerCase() + "s");
				this.modifyAttributeButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.modifyAttributeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						modifyObjectAttribute();
					}
				});
				
				this.removeAttributeButton = new JButton("Remove Attribute");
				this.removeAttributeButton.setToolTipText("Remove an attribute from " + layoutObjectName.toLowerCase() + "s");
				this.removeAttributeButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.removeAttributeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						removeObjectAttribute();
					}
				});
				
				JPanel menuPanel = new JPanel(new GridLayout(1, 0, 2, 2));
				menuPanel.add(this.mergeButton);
				menuPanel.add(this.renameButton);
				menuPanel.add(this.removeButton);
				menuPanel.add(this.deleteButton);
				menuPanel.add(this.editAttributesButton);
				menuPanel.add(this.renameAttributeButton);
				menuPanel.add(this.modifyAttributeButton);
				menuPanel.add(this.removeAttributeButton);
				return menuPanel;
			}
			
			void adjustMenu() {
				int[] rows = this.objectTable.getSelectedRows();
				if (rows.length == 0) {
					this.mergeButton.setEnabled(false);
					this.renameButton.setEnabled(this.singleTypeMatch);
					this.removeButton.setEnabled(0 < this.matchObjectCount);
					this.deleteButton.setEnabled(false);
					this.editAttributesButton.setEnabled(this.matchObjectCount == 1);
					this.renameAttributeButton.setEnabled(0 < this.matchObjectCount);
					this.modifyAttributeButton.setEnabled(0 < this.matchObjectCount);
					this.removeAttributeButton.setEnabled(0 < this.matchObjectCount);
				}
				else {
					Arrays.sort(rows);
					int fRow = rows[0];
					int lRow = rows[rows.length-1];
					this.mergeButton.setEnabled(this.isMergeableSelection(rows));
					this.renameButton.setEnabled(this.isSingleTypeSelection(rows));
					this.removeButton.setEnabled(true);
					this.deleteButton.setEnabled(true);
					this.editAttributesButton.setEnabled(fRow == lRow);
					this.renameAttributeButton.setEnabled(true);
					this.modifyAttributeButton.setEnabled(true);
					this.removeAttributeButton.setEnabled(true);
				}
			}
			
			boolean isSingleTypeSelection(int[] rows) {
				if (rows.length <= 1)
					return true;
				if (this.singleTypeMatch && this.showMatches.isSelected())
					return true;
				String type = this.objectTrays[rows[0]].wrappedObject.getType();
				for (int r = 1; r < rows.length; r++) {
					if (!type.equals(this.objectTrays[rows[r]].wrappedObject.getType()))
						return false;
				}
				return true;
			}
			
			boolean isMergeableSelection(int[] rows) {
				if (rows.length <= 1)
					return false;
				if (((this.sortColumn != 1) && (this.sortColumn != -1)) || this.sortDescending)
					return false;
				if (this.showingLayoutObjects)
					return false;
				for (int r = 1; r < rows.length; r++) {
					if ((rows[r-1] + 1) != rows[r])
						return false;
				}
				if (!this.isSingleTypeSelection(rows))
					return false;
				ImWord fImw = ((ImWord) this.objectTrays[rows[0]].wrappedObject.getAttribute(ImAnnotation.FIRST_WORD_ATTRIBUTE));
				if ((fImw == null) || (fImw.getTextStreamId() == null))
					return false;
				for (int r = 1; r < rows.length; r++) {
					ImWord rImw = ((ImWord) this.objectTrays[rows[r]].wrappedObject.getAttribute(ImAnnotation.FIRST_WORD_ATTRIBUTE));
					if ((rImw == null) || !fImw.getTextStreamId().equals(rImw.getTextStreamId()))
						return false;
				}
				return true;
			}
			
			void setFilter(AnnotationFilter filter) {
				this.filter = filter;
				this.refreshAnnotations();
			}
			
			void refreshAnnotations() {
				
				//	apply filter
				MutableAnnotation[] annotations = ((this.filter == null) ? new MutableAnnotation[0] : this.filter.getMutableMatches(this.wrappedDoc));
				this.objectTrays = new ObjectTray[annotations.length];
				
				//	set up statistics
				String type = ((annotations.length == 0) ? "" : annotations[0].getType());
				Set matchIDs = new HashSet();
				
				//	check matching annotations
				for (int a = 0; a < annotations.length; a++) {
					if (!type.equals(annotations[a].getType()))
						type = "";
					matchIDs.add(annotations[a].getAnnotationID());
					if (this.objectTraysByID.containsKey(annotations[a].getAnnotationID()))
						this.objectTrays[a] = ((ObjectTray) this.objectTraysByID.get(annotations[a].getAnnotationID()));
					else {
						this.objectTrays[a] = new ObjectTray(annotations[a]);
						this.objectTraysByID.put(annotations[a].getAnnotationID(), this.objectTrays[a]);
					}
					this.objectTrays[a].isMatch = true;
				}
				
				//	more than one type
				if (type.length() == 0) {
					this.singleTypeMatch = false;
					this.matchObjectCount = annotations.length;
					this.showMatches.setSelected(true);
					this.showMatches.setEnabled(false);
					this.highlightMatches.setEnabled(false);
					setTitle(originalTitle + " - " + annotations.length + " " + this.layoutObjectName + "s");
				}
				
				//	all of same type, do match highlight display if required
				else {
					this.singleTypeMatch = true;
					this.matchObjectCount = matchIDs.size();
					this.showMatches.setEnabled(true);
					this.highlightMatches.setEnabled(true);
					
					//	highlight matches
					if (this.highlightMatches.isSelected()) {
						annotations = this.wrappedDoc.getMutableAnnotations(type);
						this.objectTrays = new ObjectTray[annotations.length];
						for (int a = 0; a < annotations.length; a++) {
							if (this.objectTraysByID.containsKey(annotations[a].getAnnotationID()))
								this.objectTrays[a] = ((ObjectTray) this.objectTraysByID.get(annotations[a].getAnnotationID()));
							else {
								this.objectTrays[a] = new ObjectTray(annotations[a]);
								this.objectTraysByID.put(annotations[a].getAnnotationID(), this.objectTrays[a]);
							}
							this.objectTrays[a].isMatch = matchIDs.contains(annotations[a].getAnnotationID());
						}
						setTitle(originalTitle + " - " + annotations.length + " " + this.layoutObjectName + "s, " + matchIDs.size() + " matches");
					}
					else setTitle(originalTitle + " - " + annotations.length + " " + this.layoutObjectName + "s");
				}
				
				this.objectTable.setModel(new ObjectListTableModel(this.objectTrays));
				this.objectTable.getColumnModel().getColumn(0).setMaxWidth(120);
				this.objectTable.getColumnModel().getColumn(1).setMaxWidth(50);
				this.objectTable.getColumnModel().getColumn(2).setMaxWidth(50);
				
				this.sortColumn = -1;
				this.sortDescending = false;
				this.refreshDisplay();
			}
			
			void sortObjects() {
				Arrays.sort(this.objectTrays, new Comparator() {
					public int compare(Object o1, Object o2) {
						ObjectTray at1 = ((ObjectTray) o1);
						ObjectTray at2 = ((ObjectTray) o2);
						int c;
						if (sortColumn == 0)
							c = at1.wrappedObject.getType().compareToIgnoreCase(at2.wrappedObject.getType());
						else if (sortColumn == 1)
							c = (at1.wrappedObject.getStartIndex() - at2.wrappedObject.getStartIndex());
						else if (sortColumn == 2)
							c = (at1.wrappedObject.size() - at2.wrappedObject.size());
						else if (sortColumn == 3)
							c = String.CASE_INSENSITIVE_ORDER.compare(at1.wrappedObject.getValue(), at2.wrappedObject.getValue());
						else c = 0;
						
						return ((sortDescending ? -1 : 1) * ((c == 0) ? AnnotationUtils.compare(at1.wrappedObject, at2.wrappedObject) : c));
					}
				});
				this.refreshDisplay();
			}
			
			void refreshDisplay() {
				for(int i = 0; i < this.objectTable.getColumnCount();i++)
					this.objectTable.getColumnModel().getColumn(i).setHeaderValue(this.objectTable.getModel().getColumnName(i));
				this.objectTable.getTableHeader().revalidate();
				this.objectTable.getTableHeader().repaint();
				this.objectTable.revalidate();
				this.objectTable.repaint();
				ObjectListDialog.this.validate();
			}
			
			void editObjectAttributes(int objectIndex) {
				Annotation[] objects = new Annotation[this.objectTrays.length];
				for (int o = 0; o < this.objectTrays.length; o++)
					objects[o] = this.objectTrays[o].wrappedObject;
				
				//	keep going while user skips up and down
				boolean dirty = false;
				while (objectIndex != -1) {
					
					//	create dialog
					AttributeEditorDialog aed = new AttributeEditorDialog(ObjectListDialog.this.getDialog(), objects, objectIndex);
					aed.setVisible(true);
					
					//	read editing result
					objectIndex = aed.getSelectedObjectIndex();
					dirty = (dirty | aed.isDirty());
				}
				
				//	finish
				if (dirty)
					refreshAnnotations();
			}
			
			private class AttributeEditorDialog extends DialogPanel {
				private int objectIndex;
				private boolean dirty = false;
				private AttributeEditor attributeEditor;
				AttributeEditorDialog(Window owner, Annotation[] objects, int objectIndex) {
					super(owner, ("Edit " + layoutObjectName + " Attributes"), true);
					this.objectIndex = objectIndex;
					
					//	set size and location
					this.setSize(attributeDialogSize);
					if (attributeDialogLocation == null)
						this.setLocationRelativeTo(owner);
					else this.setLocation(attributeDialogLocation);
					
					//	create attribute editor
					this.attributeEditor = new AttributeEditor(objects[objectIndex], objects[objectIndex].getType(), objects[objectIndex].getValue(), objects);
					
					//	create buttons
					JButton previous = new JButton("Previous");
					previous.setBorder(BorderFactory.createRaisedBevelBorder());
					previous.setPreferredSize(new Dimension(80, 21));
					previous.setEnabled(this.objectIndex > 0);
					previous.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							AttributeEditorDialog.this.objectIndex--;
							AttributeEditorDialog.this.dirty = attributeEditor.writeChanges();
							AttributeEditorDialog.this.dispose();
						}
					});
					JButton ok = new JButton("OK");
					ok.setBorder(BorderFactory.createRaisedBevelBorder());
					ok.setPreferredSize(new Dimension(80, 21));
					ok.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							AttributeEditorDialog.this.objectIndex = -1;
							AttributeEditorDialog.this.dirty = attributeEditor.writeChanges();
							AttributeEditorDialog.this.dispose();
						}
					});
					JButton cancel = new JButton("Cancel");
					cancel.setBorder(BorderFactory.createRaisedBevelBorder());
					cancel.setPreferredSize(new Dimension(80, 21));
					cancel.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							AttributeEditorDialog.this.objectIndex = -1;
							AttributeEditorDialog.this.dispose();
						}
					});
					JButton next = new JButton("Next");
					next.setBorder(BorderFactory.createRaisedBevelBorder());
					next.setPreferredSize(new Dimension(80, 21));
					next.setEnabled((this.objectIndex + 1) < objects.length);
					next.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							AttributeEditorDialog.this.objectIndex++;
							AttributeEditorDialog.this.dirty = attributeEditor.writeChanges();
							AttributeEditorDialog.this.dispose();
						}
					});
					
					//	tray up buttons
					JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
					buttons.add(previous);
					buttons.add(ok);
					buttons.add(cancel);
					buttons.add(next);
					
					//	assemble the whole thing
					this.add(this.attributeEditor, BorderLayout.CENTER);
					this.add(buttons, BorderLayout.SOUTH);
				}
				
				//	remember size and location on closing
				public void dispose() {
					attributeDialogSize = this.getSize();
					attributeDialogLocation = this.getLocation(attributeDialogLocation);
					super.dispose();
				}
				
				int getSelectedObjectIndex() {
					return this.objectIndex;
				}
				
				boolean isDirty() {
					return this.dirty;
				}
			}
			
			void renameObjectAttribute() {
				Annotation[] annotations = this.getSelectedObjects();
				if (annotations.length == 0)
					return;
				if (attributeToolProvider.renameAttribute(null, annotations))
					refreshAnnotations();
			}
			
			void modifyObjectAttribute() {
				Annotation[] annotations = this.getSelectedObjects();
				if (annotations.length == 0)
					return;
				if (attributeToolProvider.modifyAttribute(null, annotations))
					refreshAnnotations();
			}
			
			void removeObjectAttribute() {
				Annotation[] annotations = this.getSelectedObjects();
				if (annotations.length == 0)
					return;
				if (attributeToolProvider.removeAttribute(null, annotations))
					refreshAnnotations();
			}
			
			Annotation[] getSelectedObjects() {
				int[] rows = this.objectTable.getSelectedRows();
				if (rows.length == 0) {
					ArrayList objectList = new ArrayList();
					for (int t = 0; t < this.objectTrays.length; t++) {
						if (this.objectTrays[t].isMatch)
							objectList.add(this.objectTrays[t].wrappedObject);
					}
					return ((Annotation[]) objectList.toArray(new Annotation[objectList.size()]));
				}
				else {
					Annotation[] objects = new Annotation[rows.length];
					for (int r = 0; r < rows.length; r++)
						objects[r] = this.objectTrays[rows[r]].wrappedObject;
					return objects;
				}
			}
			
			private class ObjectTray {
				final MutableAnnotation wrappedObject;
				boolean isMatch = false;
				ObjectTray(MutableAnnotation annotation) {
					this.wrappedObject = annotation;
				}
			}
			
			private class ObjectListTableModel implements TableModel {
				private ObjectTray[] objectTrays;
				private boolean isMatchesOnly = true;
				ObjectListTableModel(ObjectTray[] annotations) {
					this.objectTrays = annotations;
					for (int a = 0; a < this.objectTrays.length; a++)
						this.isMatchesOnly = (this.isMatchesOnly && this.objectTrays[a].isMatch);
				}
				
				public int getColumnCount() {
					return 4;
				}
				public Class getColumnClass(int columnIndex) {
					return String.class;
				}
				public String getColumnName(int columnIndex) {
					String sortExtension = ((columnIndex == sortColumn) ? (sortDescending ? " (d)" : " (a)") : "");
					if (columnIndex == 0) return ("Type" + sortExtension);
					if (columnIndex == 1) return ("Start" + sortExtension);
					if (columnIndex == 2) return ("End" + sortExtension);
					if (columnIndex == 3) return ((showingLayoutObjects ? "Words" : "Value") + sortExtension);
					return null;
				}
				
				public int getRowCount() {
					return this.objectTrays.length;
				}
				public Object getValueAt(int rowIndex, int columnIndex) {
					Annotation wo = this.objectTrays[rowIndex].wrappedObject;
					if (this.isMatchesOnly || !this.objectTrays[rowIndex].isMatch) {
						if (columnIndex == 0) return wo.getType();
						if (columnIndex == 1) return "" + wo.getStartIndex();
						if (columnIndex == 2) return "" + wo.getEndIndex();
						if (columnIndex == 3) return wo.getValue();
						return null;
					}
					else {
						String value = null;
						if (columnIndex == 0) value = wo.getType();
						if (columnIndex == 1) value = "" + wo.getStartIndex();
						if (columnIndex == 2) value = "" + wo.getEndIndex();
						if (columnIndex == 3) value = wo.getValue();
						return ((value == null) ? null : ("<HTML><B>" + value + "</B></HTML>"));
					}
				}
				
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return false;
				}
				public void setValueAt(Object newValue, int rowIndex, int columnIndex) {}
				
				public void addTableModelListener(TableModelListener l) {}
				public void removeTableModelListener(TableModelListener l) {}
			}
			
			private class TooltipAwareComponentRenderer extends DefaultTableCellRenderer {
				private HashSet tooltipColumns = new HashSet();
				private Tokenizer tokenizer;
				TooltipAwareComponentRenderer(int tooltipColumn, Tokenizer tokenizer) {
					this.tooltipColumns.add("" + tooltipColumn);
					this.tokenizer = tokenizer;
				}
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					JComponent component = ((value instanceof JComponent) ? ((JComponent) value) : (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column));
					if (this.tooltipColumns.contains("" + row) && (component instanceof JComponent))
						((JComponent) component).setToolTipText(this.produceTooltipText(new PlainTokenSequence(value.toString(), this.tokenizer)));
					return component;
				}
				private String produceTooltipText(TokenSequence tokens) {
					if (tokens.size() < 100) return TokenSequenceUtils.concatTokens(tokens);
					
					StringVector lines = new StringVector();
					int startToken = 0;
					int lineLength = 0;
					Token lastToken = null;
					
					for (int t = 0; t < tokens.size(); t++) {
						Token token = tokens.tokenAt(t);
						lineLength += token.length();
						if (lineLength > 100) {
							lines.addElement(TokenSequenceUtils.concatTokens(tokens, startToken, (t - startToken + 1)));
							startToken = (t + 1);
							lineLength = 0;
						} else if (Gamta.insertSpace(lastToken, token)) lineLength++;
					}
					if (startToken < tokens.size())
						lines.addElement(TokenSequenceUtils.concatTokens(tokens, startToken, (tokens.size() - startToken)));
					
					return ("<HTML>" + lines.concatStrings("<BR>") + "</HTML>");
				}
			}
		}
	}
}