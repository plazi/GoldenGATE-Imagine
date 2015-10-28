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
package de.uka.ipd.idaho.im.imagine.plugins.doc;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle;
import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.gamta.ImTokenSequence;
import de.uka.ipd.idaho.im.imagine.GoldenGateImagine;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefTypeSystem;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * This plug-in manages document style parameter lists and helps users with
 * creating and editing them.
 * 
 * @author sautter
 */
public class DocumentStyleManager extends AbstractSelectionActionProvider implements GoldenGateImagineDocumentListener {
	private Settings parameterValueClassNames;
	private Map parameterValueClasses = Collections.synchronizedMap(new HashMap());
	private BibRefTypeSystem refTypeSystem;
	private DocumentStyleProvider styleProvider;
	
	/** zero-argument constructor for class loading */
	public DocumentStyleManager() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Document Style Manager";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Document Style";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	connect to document style provider for storage
		this.styleProvider = ((DocumentStyleProvider) this.parent.getResourceProvider(DocumentStyleProvider.class.getName()));
		
		//	get reference type system for publication types
		this.refTypeSystem = BibRefTypeSystem.getDefaultInstance();
		
		//	read existing style parameters
		try {
			Reader spr = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("styleParameters.cnfg"), "UTF-8"));
			this.parameterValueClassNames = Settings.loadSettings(spr);
			spr.close();
			String[] spns = this.parameterValueClassNames.getKeys();
			for (int p = 0; p < spns.length; p++) try {
				this.parameterValueClasses.put(spns[p], Class.forName(this.parameterValueClassNames.getSetting(spns[p])));
			} catch (Exception e) {}
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#exit()
	 */
	public void exit() {
		
		//	close style editor if open
		if (this.docStyleEditor != null)
			this.docStyleEditor.dispose();
		
		//	store all (known) style parameters and their value classes
		String[] params = DocumentStyle.getParameterNames();
		Arrays.sort(params);
		boolean paramsDirty = false;
		for (int p = 0; p < params.length; p++) {
			Class paramClass = DocumentStyle.getParameterValueClass(params[p]);
			Class eParamClass = ((Class) this.parameterValueClasses.get(params[p]));
			if ((eParamClass == null) || !paramClass.getName().equals(eParamClass.getName())) {
				this.parameterValueClassNames.setSetting(params[p], paramClass.getName());
				paramsDirty = true;
			}
		}
		if (paramsDirty) try {
			Writer spw = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream("styleParameters.cnfg"), "UTF-8"));
			this.parameterValueClassNames.storeAsText(spw);
			spw.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	private boolean checkParamValueClass(String docStyleParamName, Class cls, boolean includeArray) {
		Class paramValueClass = ((Class) this.parameterValueClasses.get(docStyleParamName));
		if (paramValueClass != null) {
			if (paramValueClass.getName().equals(cls.getName()))
				return true;
			else if (includeArray && DocumentStyle.getListElementClass(paramValueClass).getName().equals(cls.getName()))
				return true;
		}
		if (docStyleParamName.startsWith("anchor.")) {
			if (docStyleParamName.endsWith(".minFontSize") || docStyleParamName.endsWith(".maxFontSize") || docStyleParamName.endsWith(".fontSize"))
				return Integer.class.getName().equals(cls.getName());
			else if (docStyleParamName.endsWith(".isBold") || docStyleParamName.endsWith(".isItalics") || docStyleParamName.endsWith(".isAllCaps"))
				return Boolean.class.getName().equals(cls.getName());
			else if (docStyleParamName.endsWith(".pattern"))
				return String.class.getName().equals(cls.getName());
			else if (docStyleParamName.endsWith(".area"))
				return BoundingBox.class.getName().equals(cls.getName());
		}
		return false;
	}
	
	private Class getParamValueClass(String docStyleParamName) {
		Class paramValueClass = ((Class) this.parameterValueClasses.get(docStyleParamName));
		if (paramValueClass != null)
			return paramValueClass;
		if (docStyleParamName.startsWith("anchor.")) {
			if (docStyleParamName.endsWith(".minFontSize") || docStyleParamName.endsWith(".maxFontSize") || docStyleParamName.endsWith(".fontSize"))
				return Integer.class;
			else if (docStyleParamName.endsWith(".isBold") || docStyleParamName.endsWith(".isItalics") || docStyleParamName.endsWith(".isAllCaps"))
				return Boolean.class;
			else if (docStyleParamName.endsWith(".pattern"))
				return String.class;
			else if (docStyleParamName.endsWith(".area"))
				return BoundingBox.class;
		}
		return String.class;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin#setImagineParent(de.uka.ipd.idaho.im.imagine.GoldenGateImagine)
	 */
	public void setImagineParent(GoldenGateImagine ggImagine) { /* we don't really need GG Imagine, at least for now */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin#initImagine()
	 */
	public void initImagine() { /* nothing to initialize */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentOpened(de.uka.ipd.idaho.im.ImDocument)
	 */
	public void documentOpened(ImDocument doc) { /* we only react to documents being closed */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentSaving(de.uka.ipd.idaho.im.ImDocument)
	 */
	public void documentSaving(ImDocument doc) { /* we only react to documents being closed */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentSaved(de.uka.ipd.idaho.im.ImDocument)
	 */
	public void documentSaved(ImDocument doc) { /* we only react to documents being closed */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentClosed(java.lang.String)
	 */
	public void documentClosed(String docId) {
		Settings docStyle = ((Settings) this.docStylesByDocId.get(docId));
		if (docStyle == null)
			return;
		String docStyleName = docStyle.getSetting(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE);
		if (docStyleName == null)
			return;
		this.styleProvider.storeStyle(docStyleName, docStyle);
	}
	
	private Map docStylesByDocId = Collections.synchronizedMap(new HashMap());
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(ImWord start, ImWord end, final ImDocumentMarkupPanel idmp) {
		
		//	cross page selection, unlikely a style edit
		if (start.pageId != end.pageId)
			return null;
		
		//	get document style name and style
		String docStyleName = ((String) idmp.document.getAttribute(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE));
		
		//	no document style assigned
		if (docStyleName == null) {
			
			//	no document style editing, offer adding or creating one
			if ((this.docStyleEditor == null) || (this.docStyleEditor.docStyleName == null))
				return this.getAssignDocStyleAction(idmp.document);
			
			//	use editing document style
			else docStyleName = this.docStyleEditor.docStyleName;
		}
		
		//	style already assigned, offer extending or modifying it
		final Settings docStyle = this.getDocStyle(idmp.document.docId, docStyleName);
		if (this.docStyleEditor != null)
			this.docStyleEditor.setDocStyle(idmp.document, docStyleName, docStyle);
		
		//	assess font style and size, and collect word string
		boolean isBold = true;
		boolean isItalics = true;
		boolean isAllCaps = true;
		boolean hasCaps = false;
		int minFontSize = 72;
		int maxFontSize = 0;
		final String fWordString;
		int left = Integer.MAX_VALUE;
		int right = 0;
		int top = Integer.MAX_VALUE;
		int bottom = 0;
		
		//	single text stream, assess word sequence
		if (start.getTextStreamId().equals(end.getTextStreamId())) {
			
			//	make sure start does not lie after end (would run for loop to end of text stream)
			if (ImUtils.textStreamOrder.compare(start, end) > 0) {
				ImWord imw = start;
				start = end;
				end = imw;
			}
			
			//	assess single word
			if (start == end) {
				isBold = (isBold && start.hasAttribute(ImWord.BOLD_ATTRIBUTE));
				isItalics = (isItalics && start.hasAttribute(ImWord.ITALICS_ATTRIBUTE));
				isAllCaps = (isAllCaps && start.getString().equals(start.getString().toUpperCase()));
				hasCaps = (hasCaps || !start.getString().equals(start.getString().toLowerCase()));
				try {
					int fs = Integer.parseInt((String) start.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE));
					minFontSize = Math.min(minFontSize, fs);
					maxFontSize = Math.max(maxFontSize, fs);
				} catch (RuntimeException re) {}
				fWordString = start.getString();
				left = Math.min(left, start.bounds.left);
				right = Math.max(right, start.bounds.right);
				top = Math.min(top, start.bounds.top);
				bottom = Math.max(bottom, start.bounds.bottom);
			}
			
			//	assess word sequence
			else {
				for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
					isBold = (isBold && imw.hasAttribute(ImWord.BOLD_ATTRIBUTE));
					isItalics = (isItalics && imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE));
					isAllCaps = (isAllCaps && imw.getString().equals(imw.getString().toUpperCase()));
					hasCaps = (hasCaps || !imw.getString().equals(imw.getString().toLowerCase()));
					try {
						int fs = Integer.parseInt((String) imw.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE));
						minFontSize = Math.min(minFontSize, fs);
						maxFontSize = Math.max(maxFontSize, fs);
					} catch (RuntimeException re) {}
					left = Math.min(left, imw.bounds.left);
					right = Math.max(right, imw.bounds.right);
					top = Math.min(top, imw.bounds.top);
					bottom = Math.max(bottom, imw.bounds.bottom);
					if (imw == end)
						break;
				}
				fWordString = (((end.getTextStreamPos() - start.getTextStreamPos()) < 15) ? ImUtils.getString(start, end, true) : null);
			}
		}
		
		//	different text streams, only use argument words proper
		else {
			isBold = (isBold && start.hasAttribute(ImWord.BOLD_ATTRIBUTE));
			isItalics = (isItalics && start.hasAttribute(ImWord.ITALICS_ATTRIBUTE));
			isAllCaps = (isAllCaps && start.getString().equals(start.getString().toUpperCase()));
			hasCaps = (hasCaps || !start.getString().equals(start.getString().toLowerCase()));
			try {
				int fs = Integer.parseInt((String) start.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE));
				minFontSize = Math.min(minFontSize, fs);
				maxFontSize = Math.max(maxFontSize, fs);
			} catch (RuntimeException re) {}
			left = Math.min(left, start.bounds.left);
			right = Math.max(right, start.bounds.right);
			top = Math.min(top, start.bounds.top);
			bottom = Math.max(bottom, start.bounds.bottom);
			
			isBold = (isBold && end.hasAttribute(ImWord.BOLD_ATTRIBUTE));
			isItalics = (isItalics && end.hasAttribute(ImWord.ITALICS_ATTRIBUTE));
			isAllCaps = (isAllCaps && end.getString().equals(end.getString().toUpperCase()));
			hasCaps = (hasCaps || !end.getString().equals(end.getString().toLowerCase()));
			try {
				int fs = Integer.parseInt((String) end.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE));
				minFontSize = Math.min(minFontSize, fs);
				maxFontSize = Math.max(maxFontSize, fs);
			} catch (RuntimeException re) {}
			left = Math.min(left, end.bounds.left);
			right = Math.max(right, end.bounds.right);
			top = Math.min(top, end.bounds.top);
			bottom = Math.max(bottom, end.bounds.bottom);
			
			fWordString = (start.getString() + " " + end.getString());
		}
		
		//	measure margins
		int horiMargin = (end.bounds.left - start.bounds.right);
		int vertMargin = (end.bounds.top - start.bounds.bottom);
		
		//	fix parameter values, scaling bounds and margins to default 72 DPI
		final boolean fIsBold = isBold;
		final boolean fIsItalics = isItalics;
		final boolean fIsAllCaps = (isAllCaps && hasCaps);
		final int fMinFontSize = minFontSize;
		final int fMaxFontSize = maxFontSize;
		int pageDpi = idmp.document.getPage(start.pageId).getImageDPI();
		/* cut word based bounding boxes a little slack, adding some pixels in
		 * each direction, maybe (DPI / 12), a.k.a. some 2 millimeters, to help
		 * with slight word placement variations */
		final BoundingBox fWordBounds = DocumentStyle.scaleBox(new BoundingBox((left - (pageDpi / 12)), (right + (pageDpi / 12)), (top - (pageDpi / 12)), (bottom + (pageDpi / 12))), pageDpi, 72);
		final int fHoriMargin = ((horiMargin < 0) ? 0 : DocumentStyle.scaleInt(horiMargin, pageDpi, 72));
		final int fVertMargin = ((vertMargin < 0) ? 0 : DocumentStyle.scaleInt(vertMargin, pageDpi, 72));
		
		//	get available parameter names, including ones from style proper (anchors !!!)
		TreeSet docStyleParamNameSet = new TreeSet(Arrays.asList(this.parameterValueClassNames.getKeys()));
		String[] dsDocStyleParamNames = docStyle.getKeys();
		for (int p = 0; p < dsDocStyleParamNames.length; p++) {
			if (dsDocStyleParamNames[p].startsWith("anchor."))
				docStyleParamNameSet.add(dsDocStyleParamNames[p]);
		}
		final String[] docStyleParamNames = ((String[]) docStyleParamNameSet.toArray(new String[docStyleParamNameSet.size()]));
		
		//	collect actions
		ArrayList actions = new ArrayList();
		
		//	collect style parameter group names that use font properties
		final TreeSet fpDocStyleParamGroupNames = new TreeSet();
		for (int p = 0; p < docStyleParamNames.length; p++) {
			if ((fMinFontSize <= fMaxFontSize) && docStyleParamNames[p].endsWith(".fontSize") && checkParamValueClass(docStyleParamNames[p], Integer.class, false))
				fpDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
			else if ((fMinFontSize < 72) && docStyleParamNames[p].endsWith(".minFontSize") && checkParamValueClass(docStyleParamNames[p], Integer.class, false))
				fpDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
			else if ((0 < fMaxFontSize) && docStyleParamNames[p].endsWith(".maxFontSize") && checkParamValueClass(docStyleParamNames[p], Integer.class, false))
				fpDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
			else if (fIsBold && (docStyleParamNames[p].endsWith(".isBold") || docStyleParamNames[p].endsWith(".startIsBold")) && checkParamValueClass(docStyleParamNames[p], Boolean.class, false))
				fpDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
			else if (fIsItalics && (docStyleParamNames[p].endsWith(".isItalics") || docStyleParamNames[p].endsWith(".startIsItalics")) && checkParamValueClass(docStyleParamNames[p], Boolean.class, false))
				fpDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
			else if (fIsAllCaps && (docStyleParamNames[p].endsWith(".isAllCaps") || docStyleParamNames[p].endsWith(".startIsAllCaps")) && checkParamValueClass(docStyleParamNames[p], Boolean.class, false))
				fpDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
		}
		
		//	add actions using font style and size
		if (((fMinFontSize <= fMaxFontSize) || fIsBold || fIsItalics || fIsAllCaps) && (fpDocStyleParamGroupNames.size() != 0))
			actions.add(new SelectionAction("styleUseFont", "Use Font Properties", "Use font properties of selected words in document style") {
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					if (fpDocStyleParamGroupNames.size() == 1)
						return super.getMenuItem(invoker);
					
					//	populate sub menu
					JMenu m = new JMenu(this.label + " ...");
					m.setToolTipText(this.tooltip);
					JMenuItem mi;
					for (Iterator pnit = fpDocStyleParamGroupNames.iterator(); pnit.hasNext();) {
						final String pgn = ((String) pnit.next());
						mi = new JMenuItem(pgn);
						if ((docStyleEditor != null) && pgn.equals(docStyleEditor.paramGroupName))
							mi.setText("<HTML><B>" + pgn + "</B></HTML>");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								useFontProperties(idmp.document.docId, docStyle, pgn, docStyleParamNames, fMinFontSize, fMaxFontSize, fIsBold, fIsItalics, fIsAllCaps);
							}
						});
						m.add(mi);
					}
					
					//	finally ...
					return m;
				}
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					useFontProperties(idmp.document.docId, docStyle, ((String) fpDocStyleParamGroupNames.first()), docStyleParamNames, fMinFontSize, fMaxFontSize, fIsBold, fIsItalics, fIsAllCaps);
					return false;
				}
			});
		
		//	collect style parameter group names that use string properties
		final TreeSet sDocStyleParamGroupNames = new TreeSet();
		for (int p = 0; p < docStyleParamNames.length; p++) {
			if (checkParamValueClass(docStyleParamNames[p], String.class, true))
				sDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
		}
		
		//	add actions using word string (patterns, first and foremost, but also fixed values)
		if ((fWordString != null) && (sDocStyleParamGroupNames.size() != 0))
			actions.add(new SelectionAction("styleUseString", "Use String / Pattern", "Use string or pattern based on selected words in document style") {
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					if (sDocStyleParamGroupNames.size() == 1)
						return super.getMenuItem(invoker);
					
					//	populate sub menu
					JMenu m = new JMenu(this.label + " ...");
					m.setToolTipText(this.tooltip);
					JMenuItem mi;
					for (Iterator pnit = sDocStyleParamGroupNames.iterator(); pnit.hasNext();) {
						final String pgn = ((String) pnit.next());
						mi = new JMenuItem(pgn);
						if ((docStyleEditor != null) && pgn.equals(docStyleEditor.paramGroupName))
							mi.setText("<HTML><B>" + pgn + "</B></HTML>");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								useString(idmp.document, docStyle, pgn, docStyleParamNames, fWordString);
							}
						});
						m.add(mi);
					}
					
					//	finally ...
					return m;
				}
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					useString(idmp.document, docStyle, ((String) sDocStyleParamGroupNames.first()), docStyleParamNames, fWordString);
					return false;
				}
			});
		
		//	collect style parameter names that use bounding box properties
		final TreeSet bbDocStyleParamNames = new TreeSet();
		for (int p = 0; p < docStyleParamNames.length; p++) {
			if (checkParamValueClass(docStyleParamNames[p], BoundingBox.class, true))
				bbDocStyleParamNames.add(docStyleParamNames[p]);
		}
		
		//	add actions using bounding box
		if (bbDocStyleParamNames.size() != 0)
			actions.add(new SelectionAction("styleUseBox", "Use Bounding Box", "Use bounding box (rectangular hull) of selected words in document style") {
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					if (bbDocStyleParamNames.size() == 1)
						return super.getMenuItem(invoker);
					
					//	populate sub menu
					JMenu m = new JMenu(this.label + " ...");
					m.setToolTipText(this.tooltip);
					JMenuItem mi;
					for (Iterator pnit = bbDocStyleParamNames.iterator(); pnit.hasNext();) {
						final String pn = ((String) pnit.next());
						mi = new JMenuItem(pn);
						if ((docStyleEditor != null) && (docStyleEditor.paramGroupName != null) && pn.startsWith(docStyleEditor.paramGroupName) && (docStyleEditor.paramGroupName.length() == pn.lastIndexOf('.')))
							mi.setText("<HTML><B>" + pn + "</B></HTML>");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								useBoundingBox(idmp.document.docId, docStyle, pn, fWordBounds);
							}
						});
						m.add(mi);
					}
					
					//	finally ...
					return m;
				}
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					useBoundingBox(idmp.document.docId, docStyle, ((String) bbDocStyleParamNames.first()), fWordBounds);
					return false;
				}
			});
		
		//	collect style parameter names that use integer properties (apart from font sizes)
		final TreeSet mDocStyleParamNames = new TreeSet();
		for (int p = 0; p < docStyleParamNames.length; p++) {
			if (true
					&& !docStyleParamNames[p].endsWith(".margin") && !docStyleParamNames[p].endsWith("Margin")
					&& !docStyleParamNames[p].endsWith(".width") && !docStyleParamNames[p].endsWith("Width")
					&& !docStyleParamNames[p].endsWith(".height") && !docStyleParamNames[p].endsWith("Height")
					&& !docStyleParamNames[p].endsWith(".distance") && !docStyleParamNames[p].endsWith("Distance")
					&& !docStyleParamNames[p].endsWith(".dist") && !docStyleParamNames[p].endsWith("Dist")
					&& !docStyleParamNames[p].endsWith(".gap") && !docStyleParamNames[p].endsWith("Gap")
				) continue;
			if (checkParamValueClass(docStyleParamNames[p], Integer.class, false))
				mDocStyleParamNames.add(docStyleParamNames[p]);
		}
		
		//	if two words on same line, offer using horizontal distance between first and last (e.g. for minimum column margin)
		if ((fHoriMargin != 0) && (mDocStyleParamNames.size() != 0))
			actions.add(new SelectionAction("styleUseMargin", "Use Horizontal Margin", "Use horizontal margin between first and last seleted words in document style") {
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					if (mDocStyleParamNames.size() == 1)
						return super.getMenuItem(invoker);
					
					//	populate sub menu
					JMenu m = new JMenu(this.label + " ...");
					m.setToolTipText(this.tooltip);
					JMenuItem mi;
					for (Iterator pnit = mDocStyleParamNames.iterator(); pnit.hasNext();) {
						final String pn = ((String) pnit.next());
						mi = new JMenuItem(pn);
						if ((docStyleEditor != null) && (docStyleEditor.paramGroupName != null) && pn.startsWith(docStyleEditor.paramGroupName) && (docStyleEditor.paramGroupName.length() == pn.lastIndexOf('.')))
							mi.setText("<HTML><B>" + pn + "</B></HTML>");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								useMargin(idmp.document.docId, docStyle, pn, fHoriMargin);
							}
						});
						m.add(mi);
					}
					
					//	finally ...
					return m;
				}
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					useMargin(idmp.document.docId, docStyle, ((String) mDocStyleParamNames.first()), fHoriMargin);
					return false;
				}
			});
		
		//	if two or more words not on same line, offer using vertical distance between first and last (e.g. for minimum block margin)
		if ((fVertMargin != 0) && (mDocStyleParamNames.size() != 0))
			actions.add(new SelectionAction("styleUseMargin", "Use Vertical Margin", "Use vertical margin between first and last seleted words in document style") {
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					if (mDocStyleParamNames.size() == 1)
						return super.getMenuItem(invoker);
					
					//	populate sub menu
					JMenu m = new JMenu(this.label + " ...");
					m.setToolTipText(this.tooltip);
					JMenuItem mi;
					for (Iterator pnit = mDocStyleParamNames.iterator(); pnit.hasNext();) {
						final String pn = ((String) pnit.next());
						mi = new JMenuItem(pn);
						if ((docStyleEditor != null) && (docStyleEditor.paramGroupName != null) && pn.startsWith(docStyleEditor.paramGroupName) && (docStyleEditor.paramGroupName.length() == pn.lastIndexOf('.')))
							mi.setText("<HTML><B>" + pn + "</B></HTML>");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								useMargin(idmp.document.docId, docStyle, pn, fVertMargin);
							}
						});
						m.add(mi);
					}
					
					//	finally ...
					return m;
				}
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					useMargin(idmp.document.docId, docStyle, ((String) mDocStyleParamNames.first()), fVertMargin);
					return false;
				}
			});
		
		//	collect style parameter group names that use bounding box properties
		final TreeSet bbDocStyleParamGroupNames = new TreeSet();
		for (int p = 0; p < docStyleParamNames.length; p++) {
			if (checkParamValueClass(docStyleParamNames[p], BoundingBox.class, true))
				bbDocStyleParamGroupNames.add(docStyleParamNames[p].substring(0, docStyleParamNames[p].lastIndexOf('.')));
		}
		
		//	combine style parameter names
		final TreeSet selDocStyleParamGroupNames = new TreeSet();
		selDocStyleParamGroupNames.addAll(fpDocStyleParamGroupNames);
		selDocStyleParamGroupNames.addAll(sDocStyleParamGroupNames);
		selDocStyleParamGroupNames.addAll(bbDocStyleParamGroupNames);
		
		//	add prefix for creating anchor (only if string given)
		if (fWordString != null)
			selDocStyleParamGroupNames.add("anchor.<create>");
		
		//	add actions using all properties of selection
		if (selDocStyleParamGroupNames.size() != 0)
			actions.add(new SelectionAction("styleUseAll", "Use Selection", "Use properties and bounds of selected words in document style") {
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					if (selDocStyleParamGroupNames.size() == 1)
						return super.getMenuItem(invoker);
					
					//	populate sub menu
					JMenu m = new JMenu(this.label + " ...");
					m.setToolTipText(this.tooltip);
					JMenuItem mi;
					for (Iterator pnit = selDocStyleParamGroupNames.iterator(); pnit.hasNext();) {
						final String pgn = ((String) pnit.next());
						mi = new JMenuItem(pgn);
						if ((docStyleEditor != null) && pgn.equals(docStyleEditor.paramGroupName))
							mi.setText("<HTML><B>" + ("anchor.<create>".equals(pgn) ? "anchor.&lt;create&gt;" : pgn) + "</B></HTML>");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								useSelection(idmp.document, docStyle, pgn, docStyleParamNames, fMinFontSize, fMaxFontSize, fIsBold, fIsItalics, fIsAllCaps, fWordString, fWordBounds);
							}
						});
						m.add(mi);
					}
					
					//	finally ...
					return m;
				}
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					useSelection(idmp.document, docStyle, ((String) selDocStyleParamGroupNames.first()), docStyleParamNames, fMinFontSize, fMaxFontSize, fIsBold, fIsItalics, fIsAllCaps, fWordString, fWordBounds);
					return false;
				}
			});
		
		//	add action editing document style (open dialog with tree based access to all style parameters)
		if ((this.docStyleEditor == null) || !this.docStyleEditor.isVisible() || !docStyleName.equals(this.docStyleEditor.docStyleName))
			actions.add(this.getEditDocStyleAction(idmp.document, docStyleName, docStyle));
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, final ImDocumentMarkupPanel idmp) {
		
		//	get document style name and style
		String docStyleName = ((String) idmp.document.getAttribute(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE));
		
		//	no document style assigned, offer adding or creating one
		if (docStyleName == null) {
			
			//	no document style editing, offer adding or creating one
			if ((this.docStyleEditor == null) || (this.docStyleEditor.docStyleName == null))
				return this.getAssignDocStyleAction(idmp.document);
			
			//	use editing document style
			else docStyleName = this.docStyleEditor.docStyleName;
		}
		
		//	style already assigned, offer extending or modifying it
		final Settings docStyle = this.getDocStyle(idmp.document.docId, docStyleName);
		if (this.docStyleEditor != null)
			this.docStyleEditor.setDocStyle(idmp.document, docStyleName, docStyle);
		
		//	measure selection
		int left = Math.min(start.x, end.x);
		int right = Math.max(start.x, end.x);
		int top = Math.min(start.y, end.y);
		int bottom = Math.max(start.y, end.y);
		
		//	fix parameter values, scaling bounds and margins to default 72 DPI
		final BoundingBox fWordBounds = DocumentStyle.scaleBox(new BoundingBox(left, right, top, bottom), page.getImageDPI(), 72);
		final int fHoriMargin = DocumentStyle.scaleInt((right - left), page.getImageDPI(), 72);
		final int fVertMargin = DocumentStyle.scaleInt((bottom - top), page.getImageDPI(), 72);
		
		//	get available parameter names, including ones from style proper (anchors !!!)
		TreeSet docStyleParamNameSet = new TreeSet(Arrays.asList(this.parameterValueClassNames.getKeys()));
		String[] dsDocStyleParamNames = docStyle.getKeys();
		for (int p = 0; p < dsDocStyleParamNames.length; p++) {
			if (dsDocStyleParamNames[p].startsWith("anchor."))
				docStyleParamNameSet.add(dsDocStyleParamNames[p]);
		}
		final String[] docStyleParamNames = ((String[]) docStyleParamNameSet.toArray(new String[docStyleParamNameSet.size()]));
		
		//	collect actions
		ArrayList actions = new ArrayList();
		
		//	collect style parameter group names that use bounding box properties
		final TreeSet bbDocStyleParamNames = new TreeSet();
		for (int p = 0; p < docStyleParamNames.length; p++) {
			if (checkParamValueClass(docStyleParamNames[p], BoundingBox.class, true))
				bbDocStyleParamNames.add(docStyleParamNames[p]);
		}
		
		//	add actions using bounding box
		if (bbDocStyleParamNames.size() != 0)
			actions.add(new SelectionAction("styleUseBox", "Use Bounding Box", "Use selected bounding box in document style") {
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					if (bbDocStyleParamNames.size() == 1)
						return super.getMenuItem(invoker);
					
					//	populate sub menu
					JMenu m = new JMenu(this.label + " ...");
					m.setToolTipText(this.tooltip);
					JMenuItem mi;
					for (Iterator pnit = bbDocStyleParamNames.iterator(); pnit.hasNext();) {
						final String pn = ((String) pnit.next());
						mi = new JMenuItem(pn);
						if ((docStyleEditor != null) && (docStyleEditor.paramGroupName != null) && pn.startsWith(docStyleEditor.paramGroupName) && (docStyleEditor.paramGroupName.length() == pn.lastIndexOf('.')))
							mi.setText("<HTML><B>" + pn + "</B></HTML>");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								useBoundingBox(idmp.document.docId, docStyle, pn, fWordBounds);
							}
						});
						m.add(mi);
					}
					
					//	finally ...
					return m;
				}
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					useBoundingBox(idmp.document.docId, docStyle, ((String) bbDocStyleParamNames.first()), fWordBounds);
					return false;
				}
			});
		
		//	collect style parameter names that use integer properties (apart from font sizes)
		final TreeSet mDocStyleParamNames = new TreeSet();
		for (int p = 0; p < docStyleParamNames.length; p++) {
			if (true
					&& !docStyleParamNames[p].endsWith(".margin") && !docStyleParamNames[p].endsWith("Margin")
					&& !docStyleParamNames[p].endsWith(".width") && !docStyleParamNames[p].endsWith("Width")
					&& !docStyleParamNames[p].endsWith(".height") && !docStyleParamNames[p].endsWith("Height")
					&& !docStyleParamNames[p].endsWith(".distance") && !docStyleParamNames[p].endsWith("Distance")
					&& !docStyleParamNames[p].endsWith(".dist") && !docStyleParamNames[p].endsWith("Dist")
					&& !docStyleParamNames[p].endsWith(".gap") && !docStyleParamNames[p].endsWith("Gap")
				) continue;
			if (checkParamValueClass(docStyleParamNames[p], Integer.class, false))
				mDocStyleParamNames.add(docStyleParamNames[p]);
		}
		
		//	if two words on same line, offer using horizontal distance between first and last (e.g. for minimum column margin)
		if ((fHoriMargin != 0) && (mDocStyleParamNames.size() != 0))
			actions.add(new SelectionAction("styleUseMargin", "Use Horizontal Margin", "Use width of selection in document style") {
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					if (mDocStyleParamNames.size() == 1)
						return super.getMenuItem(invoker);
					
					//	populate sub menu
					JMenu m = new JMenu(this.label + " ...");
					m.setToolTipText(this.tooltip);
					JMenuItem mi;
					for (Iterator pnit = mDocStyleParamNames.iterator(); pnit.hasNext();) {
						final String pn = ((String) pnit.next());
						mi = new JMenuItem(pn);
						if ((docStyleEditor != null) && (docStyleEditor.paramGroupName != null) && pn.startsWith(docStyleEditor.paramGroupName) && (docStyleEditor.paramGroupName.length() == pn.lastIndexOf('.')))
							mi.setText("<HTML><B>" + pn + "</B></HTML>");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								useMargin(idmp.document.docId, docStyle, pn, fHoriMargin);
							}
						});
						m.add(mi);
					}
					
					//	finally ...
					return m;
				}
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					useMargin(idmp.document.docId, docStyle, ((String) mDocStyleParamNames.first()), fHoriMargin);
					return false;
				}
			});
		
		//	if two or more words not on same line, offer using vertical distance between first and last (e.g. for minimum block margin)
		if ((fVertMargin != 0) && (mDocStyleParamNames.size() != 0))
			actions.add(new SelectionAction("styleUseMargin", "Use Vertical Margin", "Use height of selection in document style") {
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					if (mDocStyleParamNames.size() == 1)
						return super.getMenuItem(invoker);
					
					//	populate sub menu
					JMenu m = new JMenu(this.label + " ...");
					m.setToolTipText(this.tooltip);
					JMenuItem mi;
					for (Iterator pnit = mDocStyleParamNames.iterator(); pnit.hasNext();) {
						final String pn = ((String) pnit.next());
						mi = new JMenuItem(pn);
						if ((docStyleEditor != null) && (docStyleEditor.paramGroupName != null) && pn.startsWith(docStyleEditor.paramGroupName) && (docStyleEditor.paramGroupName.length() == pn.lastIndexOf('.')))
							mi.setText("<HTML><B>" + pn + "</B></HTML>");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								useMargin(idmp.document.docId, docStyle, pn, fVertMargin);
							}
						});
						m.add(mi);
					}
					
					//	finally ...
					return m;
				}
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					useMargin(idmp.document.docId, docStyle, ((String) mDocStyleParamNames.first()), fVertMargin);
					return false;
				}
			});
		
		//	add action editing document style (open dialog with tree based access to all style parameters)
		if ((this.docStyleEditor == null) || !this.docStyleEditor.isVisible() || !docStyleName.equals(this.docStyleEditor.docStyleName))
			actions.add(this.getEditDocStyleAction(idmp.document, docStyleName, docStyle));
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private void useFontProperties(String docId, Settings docStyle, String docStyleParamGroupName, String[] docStyleParamNames, int minFontSize, int maxFontSize, boolean isBold, boolean isItalics, boolean isAllCaps) {
		
		//	ask for properties to use
		JPanel fpPanel = new JPanel(new GridLayout(0, 1, 0, 0), true);
		UseBooleanPanel useMinFontSize = null;
		UseBooleanPanel useMaxFontSize = null;
		if (minFontSize <= maxFontSize) {
			int eMinFontSize = 72;
			try {
				eMinFontSize = Integer.parseInt(docStyle.getSetting((docStyleParamGroupName + ".minFontSize"), "72"));
			} catch (NumberFormatException nfe) {}
			if (minFontSize < eMinFontSize) {
				useMinFontSize = new UseBooleanPanel((docStyleParamGroupName + ".minFontSize"), ("Use " + minFontSize + " as Minimum Font Size (currently " + eMinFontSize + ")"), true);
				fpPanel.add(useMinFontSize);
			}
			int eMaxFontSize = 0;
			try {
				eMaxFontSize = Integer.parseInt(docStyle.getSetting((docStyleParamGroupName + ".maxFontSize"), "0"));
			} catch (NumberFormatException nfe) {}
			if (eMaxFontSize < maxFontSize) {
				useMaxFontSize = new UseBooleanPanel((docStyleParamGroupName + ".maxFontSize"), ("Use " + maxFontSize + " as Maximum Font Size (currently " + eMaxFontSize + ")"), true);
				fpPanel.add(useMaxFontSize);
			}
		}
		UseBooleanPanel useIsBold = null;
		if (isBold) {
			for (int p = 0; p < docStyleParamNames.length; p++) {
				if (docStyleParamNames[p].equals(docStyleParamGroupName + ".isBold")) {
					useIsBold = new UseBooleanPanel((docStyleParamGroupName + ".isBold"), "Require Values to be Bold", true);
					break;
				}
				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".startIsBold")) {
					useIsBold = new UseBooleanPanel((docStyleParamGroupName + ".startIsBold"), "Require Value Starts to be Bold", true);
					break;
				}
			}
			if (useIsBold != null)
				fpPanel.add(useIsBold);
		}
		UseBooleanPanel useIsItalics = null;
		if (isItalics) {
			for (int p = 0; p < docStyleParamNames.length; p++) {
				if (docStyleParamNames[p].equals(docStyleParamGroupName + ".isItalics")) {
					useIsItalics = new UseBooleanPanel((docStyleParamGroupName + ".isItalics"), "Require Values to be in Italics", true);
					break;
				}
				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".startIsItalics")) {
					useIsItalics = new UseBooleanPanel((docStyleParamGroupName + ".startIsItalics"), "Require Value Starts to be in Italics", true);
					break;
				}
			}
			if (useIsItalics != null)
				fpPanel.add(useIsItalics);
		}
		UseBooleanPanel useIsAllCaps = null;
		if (isAllCaps) {
			for (int p = 0; p < docStyleParamNames.length; p++) {
				if (docStyleParamNames[p].equals(docStyleParamGroupName + ".isAllCaps")) {
					useIsAllCaps = new UseBooleanPanel((docStyleParamGroupName + ".isAllCaps"), "Require Values to be All Caps", true);
					break;
				}
				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".startIsAllCaps")) {
					useIsAllCaps = new UseBooleanPanel((docStyleParamGroupName + ".startIsAllCaps"), "Require Value Starts to be All Caps", true);
					break;
				}
			}
			if (useIsAllCaps != null)
				fpPanel.add(useIsAllCaps);
		}
		
		//	prompt
		int choice = JOptionPane.showConfirmDialog(null, fpPanel, "Select Font Properties to Use", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (choice != JOptionPane.OK_OPTION)
			return;
		
		//	set properties
		if ((useMinFontSize != null) && useMinFontSize.useParam.isSelected()) {
			docStyle.setSetting((docStyleParamGroupName + ".minFontSize"), ("" + minFontSize));
			System.out.println(docStyleParamGroupName + ".minFontSize set to " + minFontSize);
		}
		if ((useMaxFontSize != null) && useMaxFontSize.useParam.isSelected()) {
			docStyle.setSetting((docStyleParamGroupName + ".maxFontSize"), ("" + maxFontSize));
			System.out.println(docStyleParamGroupName + ".maxFontSize set to " + maxFontSize);
		}
		if ((minFontSize == maxFontSize) && (useMinFontSize != null) && useMinFontSize.useParam.isSelected() && (useMaxFontSize != null) && useMaxFontSize.useParam.isSelected()) {
			docStyle.setSetting((docStyleParamGroupName + ".fontSize"), ("" + minFontSize));
			System.out.println(docStyleParamGroupName + ".fontSize set to " + minFontSize);
		}
		if ((useIsBold != null) && useIsBold.useParam.isSelected()) {
			docStyle.setSetting(useIsBold.docStyleParamName, "true");
			System.out.println(useIsBold.docStyleParamName + " set to true");
		}
		if ((useIsItalics != null) && useIsItalics.useParam.isSelected()) {
			docStyle.setSetting(useIsItalics.docStyleParamName, "true");
			System.out.println(useIsItalics.docStyleParamName + " set to true");
		}
		if ((useIsAllCaps != null) && useIsAllCaps.useParam.isSelected()) {
			docStyle.setSetting(useIsAllCaps.docStyleParamName, "true");
			System.out.println(useIsAllCaps.docStyleParamName + " set to true");
		}
		
		//	if style editor open, adjust tree path
		if (this.docStyleEditor != null)
			this.docStyleEditor.setParamGroupName(docStyleParamGroupName);
		
		//	index document style for saving
		this.docStylesByDocId.put(docId, docStyle);
	}
	
	private void useMargin(String docId, Settings docStyle, String docStyleParamName, int margin) {
		Class paramValueClass = this.getParamValueClass(docStyleParamName);
		
		//	single integer, expand or overwrite
		if (Integer.class.getName().equals(paramValueClass.getName())) {
			int eMargin = Integer.parseInt(docStyle.getSetting(docStyleParamName, "-1"));
			if (eMargin == -1) {
				docStyle.setSetting(docStyleParamName, ("" + margin));
				System.out.println(docStyleParamName + " set to " + margin);
			}
			else if (docStyleParamName.startsWith(".min", docStyleParamName.lastIndexOf('.'))) {
				docStyle.setSetting(docStyleParamName, ("" + Math.min(eMargin,  margin)));
				System.out.println(docStyleParamName + " set to " + Math.min(eMargin,  margin));
			}
			else if (docStyleParamName.startsWith(".max", docStyleParamName.lastIndexOf('.'))) {
				docStyle.setSetting(docStyleParamName, ("" + Math.max(eMargin,  margin)));
				System.out.println(docStyleParamName + " set to " + Math.max(eMargin,  margin));
			}
			else {
				docStyle.setSetting(docStyleParamName, ("" + margin));
				System.out.println(docStyleParamName + " set to " + margin);
			}
		}
		
		//	list of integers, add new one and eliminate duplicates
		else if (Integer.class.getName().equals(DocumentStyle.getListElementClass(paramValueClass).getName())) {
			String eMarginStr = docStyle.getSetting(docStyleParamName);
			if ((eMarginStr == null) || (eMarginStr.trim().length() == 0)) {
				docStyle.setSetting(docStyleParamName, ("" + margin));
				System.out.println(docStyleParamName + " set to " + margin);
			}
			else {
				String[] eMarginStrs = eMarginStr.split("[^0-9]+");
				for (int e = 0; e < eMarginStrs.length; e++) {
					if (margin == Integer.parseInt(eMarginStrs[e]))
						return;
				}
				docStyle.setSetting(docStyleParamName, (eMarginStr + " " + margin));
				System.out.println(docStyleParamName + " set to " + (eMarginStr + " " + margin));
			}
		}
		
		//	if style editor open, adjust tree path
		if (this.docStyleEditor != null)
			this.docStyleEditor.setParamGroupName(docStyleParamName.substring(0, docStyleParamName.lastIndexOf('.')));
		
		//	index document style for saving
		this.docStylesByDocId.put(docId, docStyle);
	}
	
	private void useString(final ImDocument doc, Settings docStyle, String docStyleParamGroupName, String[] docStyleParamNames, String string) {
		
		//	collect style parameter names in argument group that use string properties, constructing string usage panels on the fly
		TreeSet sDocStyleParamPanels = new TreeSet();
		final ImTokenSequence[] docTokens = {null};
		for (int p = 0; p < docStyleParamNames.length; p++)
			if (docStyleParamNames[p].startsWith(docStyleParamGroupName + ".")) {
				if (checkParamValueClass(docStyleParamNames[p], String.class, true))
					sDocStyleParamPanels.add(new UseStringPanel(docStyleParamNames[p], true, string, true) {
						ImTokenSequence getTestDocTokens() {
							if (docTokens[0] == null)
								docTokens[0] = new ImTokenSequence(((Tokenizer) doc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER)), doc.getTextStreamHeads(), (ImTokenSequence.NORMALIZATION_LEVEL_PARAGRAPHS | ImTokenSequence.NORMALIZE_CHARACTERS));
							return docTokens[0];
						}
					});
			}
		
		//	nothing to work with
		if (sDocStyleParamPanels.isEmpty())
			return;
		
		//	assemble panel
		JPanel sPanel = new JPanel(new GridLayout(0, 1, 0, 3), true);
		for (Iterator ppit = sDocStyleParamPanels.iterator(); ppit.hasNext();)
			sPanel.add((UseStringPanel) ppit.next());
		
		//	prompt
		int choice = JOptionPane.showConfirmDialog(null, sPanel, ("Select how to Use '" + string + "'"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (choice != JOptionPane.OK_OPTION)
			return;
		
		//	write parameters
		for (Iterator ppit = sDocStyleParamPanels.iterator(); ppit.hasNext();) {
			UseStringPanel usp = ((UseStringPanel) ppit.next());
			if (!usp.useParam.isSelected())
				continue;
			string = usp.string.getText().trim();
			if (string.length() == 0)
				continue;
			Class paramValueClass = this.getParamValueClass(usp.docStyleParamName);
			if (String.class.getName().equals(paramValueClass.getName())) {
				docStyle.setSetting(usp.docStyleParamName, string);
				System.out.println(usp.docStyleParamName + " set to " + string);
			}
			else if (String.class.getName().equals(DocumentStyle.getListElementClass(paramValueClass).getName())) {
				String eString = docStyle.getSetting(usp.docStyleParamName, "").trim();
				if (eString.length() == 0) {
					docStyle.setSetting(usp.docStyleParamName, string);
					System.out.println(usp.docStyleParamName + " set to " + string);
				}
				else {
					TreeSet eStringSet = new TreeSet(Arrays.asList(eString.split("\\s+")));
					eStringSet.add(string);
					StringBuffer eStringsStr = new StringBuffer();
					for (Iterator sit = eStringSet.iterator(); sit.hasNext();) {
						eStringsStr.append((String) sit.next());
						if (sit.hasNext())
							eStringsStr.append(' ');
					}
					docStyle.setSetting(usp.docStyleParamName, eStringsStr.toString());
					System.out.println(usp.docStyleParamName + " set to " + eStringsStr.toString());
				}
			}
		}
		
		//	if style editor open, adjust tree path
		if (this.docStyleEditor != null)
			this.docStyleEditor.setParamGroupName(docStyleParamGroupName);
		
		//	index document style for saving
		this.docStylesByDocId.put(doc.docId, docStyle);
	}
	
	private static abstract class UseParamPanel extends JPanel implements Comparable {
		final String docStyleParamName;
		final JCheckBox useParam;
		UseParamPanel(String docStyleParamName, String label, boolean use) {
			super(new BorderLayout(), true);
			this.docStyleParamName = docStyleParamName;
			this.useParam = new JCheckBox(label, use);
		}
		public int compareTo(Object obj) {
			return this.docStyleParamName.compareTo(((UseParamPanel) obj).docStyleParamName);
		}
		abstract String getValue();
		abstract void setValue(String value);
		boolean verifyValue(String value) {
			return true;
		}
	}
	
	private static class UseBooleanPanel extends UseParamPanel {
		UseBooleanPanel(String docStyleParamName, String label, boolean selected) {
			super(docStyleParamName, label, selected);
			this.add(this.useParam, BorderLayout.CENTER);
		}
		String getValue() {
			return (this.useParam.isSelected() ? "true" : "false");
		}
		void setValue(String value) {
			this.useParam.setSelected("true".equals(value));
		}
	}
	
	private static class UseStringPanel extends UseParamPanel {
		JTextField string;
		UseStringPanel(String docStyleParamName, boolean selected, String string, boolean escapePattern) {
			super(docStyleParamName, (" Use as " + docStyleParamName.substring(docStyleParamName.lastIndexOf('.') + ".".length())), selected);
			String localDspn = this.docStyleParamName.substring(this.docStyleParamName.lastIndexOf('.') + ".".length());
			this.add(this.useParam, BorderLayout.WEST);
			
			if (localDspn.equals("pattern") || localDspn.endsWith("Pattern")) {
				if (escapePattern)
					string = buildPattern(string);
				JButton testButton = new JButton("Test");
				testButton.setBorder(BorderFactory.createRaisedBevelBorder());
				testButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						String pattern = UseStringPanel.this.string.getText().trim();
						if (pattern.length() != 0)
							testPattern(pattern, getTestDocTokens());
					}
				});
				this.add(testButton, BorderLayout.EAST);
			}
			this.string = new JTextField(string);
			this.string.setBorder(BorderFactory.createLoweredBevelBorder());
			this.string.setPreferredSize(new Dimension(Math.max(this.string.getWidth(), (this.string.getFont().getSize() * string.length())), this.string.getHeight()));
			this.string.addFocusListener(new FocusListener() {
				private String oldValue = null;
				public void focusGained(FocusEvent fe) {
					this.oldValue = UseStringPanel.this.string.getText().trim();
				}
				public void focusLost(FocusEvent fe) {
					String value = UseStringPanel.this.string.getText().trim();
					if (!value.equals(this.oldValue))
						stringChanged(value);
					this.oldValue = null;
				}
			});
			
			this.add(this.string, BorderLayout.CENTER);
		}
		ImTokenSequence getTestDocTokens() {
			return null;
		}
		String getValue() {
			return this.string.getText().trim();
		}
		void setValue(String value) {
			this.string.setText(value);
			if (value.length() != 0)
				this.useParam.setSelected(true);
		}
		void stringChanged(String string) {}
	}
	
	private static String buildPattern(String string) {
		StringBuffer pString = new StringBuffer();
		for (int c = 0; c < string.length(); c++) {
			char ch = string.charAt(c);
			if ((ch < 33) || (ch == 160))
				pString.append("\\s*"); // turn all control characters into spaces, along with non-breaking space
			else if (ch < 127)
				pString.append((Character.isLetterOrDigit(ch) ? "" : "\\") + ch); // no need to normalize basic ASCII characters, nor escaping letters and digits
			else if ("-\u00AD\u2010\u2011\u2012\u2013\u2014\u2015\u2212".indexOf(ch) != -1)
				pString.append("\\-"); // normalize dashes right here
			else pString.append(StringUtils.getNormalForm(ch));
		}
		replace(pString, "\\s**", "\\s*");
		replace(pString, "\\s*\\s*", "\\s*");
		string = pString.toString();
		string = string.replaceAll("[1-9][0-9]*", "[1-9][0-9]*");
		return string;
	}
	
	private static void replace(StringBuffer sb, String toReplace, String replacement) {
		for (int s; (s = sb.indexOf(toReplace)) != -1;)
			sb.replace(s, (s + toReplace.length()), replacement);
	}
	
	private static void testPattern(String pattern, ImTokenSequence docTokens) {
		try {
			Annotation[] annotations = Gamta.extractAllMatches(docTokens, pattern, 20);
			if (annotations != null) {
				Window topWindow = DialogPanel.getTopWindow();
				AnnotationDisplayDialog add;
				if (topWindow instanceof JFrame)
					add = new AnnotationDisplayDialog(((JFrame) topWindow), "Matches of Pattern", annotations, true);
				else if (topWindow instanceof JDialog)
					add = new AnnotationDisplayDialog(((JDialog) topWindow), "Matches of Pattern", annotations, true);
				else add = new AnnotationDisplayDialog(((JFrame) null), "Matches of Pattern", annotations, true);
				add.setLocationRelativeTo(topWindow);
				add.setVisible(true);
			}
		}
		catch (PatternSyntaxException pse) {
			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("The pattern is not valid:\n" + pse.getMessage()), "Pattern Validation Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private static abstract class UseListPanel extends UseParamPanel {
		JTextArea list;
		UseListPanel(String docStyleParamName, boolean selected, String string) {
			super(docStyleParamName, (" Use as " + docStyleParamName.substring(docStyleParamName.lastIndexOf('.') + ".".length())), selected);
			String localDspn = this.docStyleParamName.substring(this.docStyleParamName.lastIndexOf('.') + ".".length());
			this.add(this.useParam, BorderLayout.WEST);
			if (localDspn.equals("patterns") || localDspn.endsWith("Patterns")) {
				JButton testButton = new JButton("Test");
				testButton.setBorder(BorderFactory.createRaisedBevelBorder());
				testButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						String pattern = UseListPanel.this.list.getSelectedText();
						if (pattern == null)
							return;
						pattern = pattern.trim();
						if (pattern.length() != 0)
							testPattern(pattern, getTestDocTokens());
					}
				});
				this.add(testButton, BorderLayout.EAST);
			}
			this.list = new JTextArea((string == null) ? "" : string.trim().replaceAll("\\s+", "\r\n"));
			this.list.setBorder(BorderFactory.createLoweredBevelBorder());
			this.list.addFocusListener(new FocusListener() {
				private String oldValue = null;
				public void focusGained(FocusEvent fe) {
					this.oldValue = UseListPanel.this.list.getText().trim();
				}
				public void focusLost(FocusEvent fe) {
					String value = UseListPanel.this.list.getText().trim();
					if (!value.equals(this.oldValue))
						stringChanged(value);
					this.oldValue = null;
				}
			});
			
			this.add(this.list, BorderLayout.CENTER);
		}
		ImTokenSequence getTestDocTokens() {
			return null;
		}
		String getValue() {
			return this.list.getText().trim().replaceAll("\\s+", " ");
		}
		void setValue(String value) {
			this.list.setText(value.replaceAll("\\s+", "\r\n"));
			if (value.length() != 0)
				this.useParam.setSelected(true);
		}
		void stringChanged(String string) {}
	}
	
	private void useBoundingBox(String docId, Settings docStyle, String docStyleParamName, BoundingBox bounds) {
		Class paramValueClass = this.getParamValueClass(docStyleParamName);
		
		//	single bounding box, expand
		if (BoundingBox.class.getName().equals(paramValueClass.getName())) {
			BoundingBox eBounds = BoundingBox.parse(docStyle.getSetting(docStyleParamName));
			if (eBounds == null) {
				docStyle.setSetting(docStyleParamName, bounds.toString());
				System.out.println(docStyleParamName + " set to " + bounds.toString());
			}
			else {
				docStyle.setSetting(docStyleParamName, this.aggregateBoxes(eBounds, bounds).toString());
				System.out.println(docStyleParamName + " set to " + this.aggregateBoxes(eBounds, bounds).toString());
			}
		}
		
		//	list of bounding boxes, add new one and merge ones overlapping at least 90%
		else if (BoundingBox.class.getName().equals(DocumentStyle.getListElementClass(paramValueClass).getName())) {
			String boundsStr = this.getBoxListString(docStyle.getSetting(docStyleParamName), bounds);
			docStyle.setSetting(docStyleParamName, boundsStr);
			System.out.println(docStyleParamName + " set to " + boundsStr);
		}
		
		//	if style editor open, adjust tree path
		if (this.docStyleEditor != null)
			this.docStyleEditor.setParamGroupName(docStyleParamName.substring(0, docStyleParamName.lastIndexOf('.')));
		
		//	index document style for saving
		this.docStylesByDocId.put(docId, docStyle);
	}
	
	private double getBoxOverlap(BoundingBox bb1, BoundingBox bb2) {
		if (bb1.includes(bb2, false) || bb2.includes(bb1, false))
			return 1;
		if (!bb1.overlaps(bb2))
			return 0;
		int iLeft = Math.max(bb1.left, bb2.left);
		int iRight = Math.min(bb1.right, bb2.right);
		int iTop = Math.max(bb1.top, bb2.top);
		int iBottom = Math.min(bb1.bottom, bb2.bottom);
		int iArea = ((iRight - iLeft) * (iBottom - iTop));
		int minBbArea = Math.min(((bb1.right - bb1.left) * (bb1.bottom - bb1.top)), ((bb2.right - bb2.left) * (bb2.bottom - bb2.top)));
		return (((double) iArea) / minBbArea);
	}
	
	private BoundingBox aggregateBoxes(BoundingBox bb1, BoundingBox bb2) {
		int left = Math.min(bb1.left, bb2.left);
		int right = Math.max(bb1.right, bb2.right);
		int top = Math.min(bb1.top, bb2.top);
		int bottom = Math.max(bb1.bottom, bb2.bottom);
		return new BoundingBox(left, right, top, bottom);
	}
	
	private String getBoxListString(String eBoundsStr, BoundingBox bounds) {
		if ((eBoundsStr == null) || (eBoundsStr.trim().length() == 0))
			return bounds.toString();
		
		ArrayList boundsList = new ArrayList();
		boundsList.add(bounds);
		String[] eBoundsStrs = eBoundsStr.split("[^0-9\\,\\[\\]]+");
		for (int b = 0; b < eBoundsStrs.length; b++)
			boundsList.add(BoundingBox.parse(eBoundsStrs[b]));
		
		int boundsCount;
		do {
			boundsCount = boundsList.size();
			BoundingBox bb1 = null;
			BoundingBox bb2 = null;
			double bbOverlap = 0.9; // 90% is minimum overlap for merging
			for (int b = 0; b < boundsList.size(); b++) {
				BoundingBox tbb1 = ((BoundingBox) boundsList.get(b));
				System.out.println("Testing for merger: " + tbb1);
				for (int c = (b+1); c < boundsList.size(); c++) {
					BoundingBox tbb2 = ((BoundingBox) boundsList.get(c));
					double tbbOverlap = this.getBoxOverlap(tbb1, tbb2);
					System.out.println(" - overlap with " + tbb2 + " is " + tbbOverlap);
					if (bbOverlap < tbbOverlap) {
						bbOverlap = tbbOverlap;
						bb1 = tbb1;
						bb2 = tbb2;
						System.out.println(" ==> new best merger");
					}
				}
			}
			if ((bb1 != null) && (bb2 != null)) {
				boundsList.remove(bb1);
				boundsList.remove(bb2);
				boundsList.add(this.aggregateBoxes(bb1, bb2));
			}
		}
		while (boundsList.size() < boundsCount);
		
		StringBuffer boundsStr = new StringBuffer();
		for (int b = 0; b < boundsList.size(); b++) {
			if (b != 0)
				boundsStr.append(' ');
			boundsStr.append(((BoundingBox) boundsList.get(b)).toString());
		}
		return boundsStr.toString();
	}
	
	private void useSelection(final ImDocument doc, Settings docStyle, String docStyleParamGroupName, String[] docStyleParamNames, int minFontSize, int maxFontSize, boolean isBold, boolean isItalics, boolean isAllCaps, String string, BoundingBox bounds) {
		JPanel sPanel = new JPanel(new GridLayout(0, 1, 0, 0), true);
		
		//	if creating anchor, add name field at top, pre-filled with string less all non-letters
		JTextField createAnchorName = null;
		if ("anchor.<create>".equals(docStyleParamGroupName)) {
			createAnchorName = new JTextField(DocumentStyleProvider.normalizeString(string).replaceAll("[^A-Za-z]", ""));
			JPanel caPanel = new JPanel(new BorderLayout(), true);
			caPanel.add(new JLabel(" Anchor Name: "), BorderLayout.WEST);
			caPanel.add(createAnchorName, BorderLayout.CENTER);
			sPanel.add(caPanel);
		}
		
		//	ask for font properties to use
		UseBooleanPanel useMinFontSize = null;
		UseBooleanPanel useMaxFontSize = null;
		if (minFontSize <= maxFontSize) {
			int eMinFontSize = 72;
			try {
				eMinFontSize = Integer.parseInt(docStyle.getSetting((docStyleParamGroupName + ".minFontSize"), "72"));
			} catch (NumberFormatException nfe) {}
			if (minFontSize < eMinFontSize) {
				useMinFontSize = new UseBooleanPanel((docStyleParamGroupName + ".minFontSize"), ("Use " + minFontSize + " as Minimum Font Size (currently " + eMinFontSize + ")"), true);
				sPanel.add(useMinFontSize);
			}
			int eMaxFontSize = 0;
			try {
				eMaxFontSize = Integer.parseInt(docStyle.getSetting((docStyleParamGroupName + ".maxFontSize"), "0"));
			} catch (NumberFormatException nfe) {}
			if (eMaxFontSize < maxFontSize) {
				useMaxFontSize = new UseBooleanPanel((docStyleParamGroupName + ".maxFontSize"), ("Use " + maxFontSize + " as Maximum Font Size (currently " + eMaxFontSize + ")"), true);
				sPanel.add(useMaxFontSize);
			}
		}
		UseBooleanPanel useIsBold = null;
		if (isBold) {
			for (int p = 0; p < docStyleParamNames.length; p++) {
				if (docStyleParamGroupName.startsWith("anchor.")) {
					useIsBold = new UseBooleanPanel((docStyleParamGroupName + ".isBold"), "Require Anchor Value to be Bold", true);
					break;
				}
				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".isBold")) {
					useIsBold = new UseBooleanPanel((docStyleParamGroupName + ".isBold"), "Require Values to be Bold", true);
					break;
				}
				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".startIsBold")) {
					useIsBold = new UseBooleanPanel((docStyleParamGroupName + ".startIsBold"), "Require Value Starts to be Bold", true);
					break;
				}
			}
			if (useIsBold != null)
				sPanel.add(useIsBold);
		}
		UseBooleanPanel useIsItalics = null;
		if (isItalics) {
			for (int p = 0; p < docStyleParamNames.length; p++) {
				if (docStyleParamGroupName.startsWith("anchor.")) {
					useIsItalics = new UseBooleanPanel((docStyleParamGroupName + ".isItalics"), "Require Anchor Value to be in Italics", true);
					break;
				}
				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".isItalics")) {
					useIsItalics = new UseBooleanPanel((docStyleParamGroupName + ".isItalics"), "Require Values to be in Italics", true);
					break;
				}
				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".startIsItalics")) {
					useIsItalics = new UseBooleanPanel((docStyleParamGroupName + ".startIsItalics"), "Require Value Starts to be in Italics", true);
					break;
				}
			}
			if (useIsItalics != null)
				sPanel.add(useIsItalics);
		}
		UseBooleanPanel useIsAllCaps = null;
		if (isAllCaps) {
			for (int p = 0; p < docStyleParamNames.length; p++) {
				if (docStyleParamGroupName.startsWith("anchor.")) {
					useIsAllCaps = new UseBooleanPanel((docStyleParamGroupName + ".isAllCaps"), "Require Anchor Value to be All Caps", true);
					break;
				}
				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".isAllCaps")) {
					useIsAllCaps = new UseBooleanPanel((docStyleParamGroupName + ".isAllCaps"), "Require Values to be All Caps", true);
					break;
				}
				else if (docStyleParamNames[p].equals(docStyleParamGroupName + ".startIsAllCaps")) {
					useIsAllCaps = new UseBooleanPanel((docStyleParamGroupName + ".startIsAllCaps"), "Require Value Starts to be All Caps", true);
					break;
				}
			}
			if (useIsAllCaps != null)
				sPanel.add(useIsAllCaps);
		}
		
		//	collect style parameter names in argument group that use string properties, constructing string usage panels on the fly
		TreeSet sDocStyleParamPanels = new TreeSet();
		final ImTokenSequence[] docTokens = {null};
		for (int p = 0; p < docStyleParamNames.length; p++)
			if (docStyleParamNames[p].startsWith(docStyleParamGroupName + ".")) {
				String localDspn = docStyleParamNames[p].substring(docStyleParamNames[p].lastIndexOf('.') + ".".length());
				if (!docStyleParamNames[p].equals(docStyleParamGroupName + "." + localDspn))
					continue;
				if (checkParamValueClass(docStyleParamNames[p], String.class, true))
					sDocStyleParamPanels.add(new UseStringPanel(docStyleParamNames[p], true, string, true) {
						ImTokenSequence getTestDocTokens() {
							if (docTokens[0] == null)
								docTokens[0] = new ImTokenSequence(((Tokenizer) doc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER)), doc.getTextStreamHeads(), (ImTokenSequence.NORMALIZATION_LEVEL_PARAGRAPHS | ImTokenSequence.NORMALIZE_CHARACTERS));
							return docTokens[0];
						}
					});
			}
		if (docStyleParamGroupName.equals("anchor.<create>") && sDocStyleParamPanels.isEmpty()) {
			sDocStyleParamPanels.add(new UseStringPanel((docStyleParamGroupName + ".pattern"), true, string, true) {
				ImTokenSequence getTestDocTokens() {
					if (docTokens[0] == null)
						docTokens[0] = new ImTokenSequence(((Tokenizer) doc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER)), doc.getTextStreamHeads(), (ImTokenSequence.NORMALIZATION_LEVEL_PARAGRAPHS | ImTokenSequence.NORMALIZE_CHARACTERS));
					return docTokens[0];
				}
			});
		}
		for (Iterator ppit = sDocStyleParamPanels.iterator(); ppit.hasNext();)
			sPanel.add((UseStringPanel) ppit.next());
		
		//	collect style parameter names in argument group that use bounding box properties, constructing checkboxes on the fly
		TreeSet bbDocStyleParamPanels = new TreeSet();
		for (int p = 0; p < docStyleParamNames.length; p++)
			if (docStyleParamNames[p].startsWith(docStyleParamGroupName + ".")) {
				String localDspn = docStyleParamNames[p].substring(docStyleParamNames[p].lastIndexOf('.') + ".".length());
				if (!docStyleParamNames[p].equals(docStyleParamGroupName + "." + localDspn))
					continue;
				if (checkParamValueClass(docStyleParamNames[p], BoundingBox.class, true))
					bbDocStyleParamPanels.add(new UseBooleanPanel(docStyleParamNames[p], ("Use Bounding Box as " + localDspn), true));
			}
		for (Iterator pnit = bbDocStyleParamPanels.iterator(); pnit.hasNext();)
			sPanel.add((UseParamPanel) pnit.next());
		if (docStyleParamGroupName.equals("anchor.<create>") && bbDocStyleParamPanels.isEmpty()) // an anchor always requires a bounding box, so we don't display the checkbox, but simply use it
			bbDocStyleParamPanels.add(new UseBooleanPanel((docStyleParamGroupName + ".area"), ("Use Bounding Box as Area"), true));
		
		//	prompt
		int choice = JOptionPane.showConfirmDialog(null, sPanel, "Select Properties to Use", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (choice != JOptionPane.OK_OPTION)
			return;
		
		//	if we're creating an anchor, determine lowest non-used anchor number and use that as parameter group name
		if ("anchor.<create>".equals(docStyleParamGroupName)) {
			String can = createAnchorName.getText().replaceAll("[^A-Za-z0-9]", "");
			if (can.length() == 0)
				return;
			else docStyleParamGroupName = ("anchor." + can);
		}
		
		//	set font properties
		if ((useMinFontSize != null) && useMinFontSize.useParam.isSelected()) {
			docStyle.setSetting((docStyleParamGroupName + ".minFontSize"), ("" + minFontSize));
			System.out.println(docStyleParamGroupName + ".minFontSize set to " + minFontSize);
		}
		if ((useMaxFontSize != null) && useMaxFontSize.useParam.isSelected()) {
			docStyle.setSetting((docStyleParamGroupName + ".maxFontSize"), ("" + maxFontSize));
			System.out.println(docStyleParamGroupName + ".maxFontSize set to " + maxFontSize);
		}
		if ((minFontSize == maxFontSize) && (useMinFontSize != null) && useMinFontSize.useParam.isSelected() && (useMaxFontSize != null) && useMaxFontSize.useParam.isSelected()) {
			docStyle.setSetting((docStyleParamGroupName + ".fontSize"), ("" + minFontSize));
			System.out.println(docStyleParamGroupName + ".fontSize set to " + minFontSize);
		}
		if ((useIsBold != null) && useIsBold.useParam.isSelected()) {
			docStyle.setSetting((docStyleParamGroupName + useIsBold.docStyleParamName.substring(useIsBold.docStyleParamName.lastIndexOf('.'))), "true");
			System.out.println((docStyleParamGroupName + useIsBold.docStyleParamName.substring(useIsBold.docStyleParamName.lastIndexOf('.'))) + " set to true");
		}
		if ((useIsItalics != null) && useIsItalics.useParam.isSelected()) {
			docStyle.setSetting((docStyleParamGroupName + useIsItalics.docStyleParamName.substring(useIsItalics.docStyleParamName.lastIndexOf('.'))), "true");
			System.out.println((docStyleParamGroupName + useIsItalics.docStyleParamName.substring(useIsItalics.docStyleParamName.lastIndexOf('.'))) + " set to true");
		}
		if ((useIsAllCaps != null) && useIsAllCaps.useParam.isSelected()) {
			docStyle.setSetting((docStyleParamGroupName + useIsAllCaps.docStyleParamName.substring(useIsAllCaps.docStyleParamName.lastIndexOf('.'))), "true");
			System.out.println((docStyleParamGroupName + useIsAllCaps.docStyleParamName.substring(useIsAllCaps.docStyleParamName.lastIndexOf('.'))) + " set to true");
		}
		
		//	set string parameters
		for (Iterator ppit = sDocStyleParamPanels.iterator(); ppit.hasNext();) {
			UseStringPanel usp = ((UseStringPanel) ppit.next());
			if (!usp.useParam.isSelected())
				continue;
			string = usp.string.getText().trim();
			if (string.length() == 0)
				continue;
			
			String docStyleParamName = usp.docStyleParamName;
			if ("anchor.<create>.pattern".equals(usp.docStyleParamName))
				docStyleParamName = (docStyleParamGroupName + ".pattern");
			Class paramValueClass = this.getParamValueClass(docStyleParamName);
			
			if (String.class.getName().equals(paramValueClass.getName())) {
				docStyle.setSetting(docStyleParamName, string);
				System.out.println(docStyleParamName + " set to " + string);
			}
			else if (String.class.getName().equals(DocumentStyle.getListElementClass(paramValueClass).getName())) {
				String eString = docStyle.getSetting(docStyleParamName, "").trim();
				if (eString.length() == 0) {
					docStyle.setSetting(docStyleParamName, string);
					System.out.println(docStyleParamName + " set to " + string);
				}
				else {
					TreeSet eStringSet = new TreeSet(Arrays.asList(eString.split("\\s+")));
					eStringSet.add(string);
					StringBuffer eStringsStr = new StringBuffer();
					for (Iterator sit = eStringSet.iterator(); sit.hasNext();) {
						eStringsStr.append((String) sit.next());
						if (sit.hasNext())
							eStringsStr.append(' ');
					}
					docStyle.setSetting(docStyleParamName, eStringsStr.toString());
					System.out.println(docStyleParamName + " set to " + eStringsStr.toString());
				}
			}
		}
		
		//	set bounding box properties
		for (Iterator bbdspnit = bbDocStyleParamPanels.iterator(); bbdspnit.hasNext();) {
			UseBooleanPanel useBbDsp = ((UseBooleanPanel) bbdspnit.next());
			if (!useBbDsp.useParam.isSelected())
				continue;
			
			String docStyleParamName = useBbDsp.docStyleParamName;
			if ("anchor.<create>.area".equals(docStyleParamName))
				docStyleParamName = (docStyleParamGroupName + ".area");
			Class paramValueClass = this.getParamValueClass(docStyleParamName);
			
			//	single bounding box, expand
			if (BoundingBox.class.getName().equals(paramValueClass.getName())) {
				BoundingBox eBounds = BoundingBox.parse(docStyle.getSetting(docStyleParamName));
				if (eBounds == null) {
					docStyle.setSetting(docStyleParamName, bounds.toString());
					System.out.println(docStyleParamName + " set to " + bounds.toString());
				}
				else {
					docStyle.setSetting(docStyleParamName, this.aggregateBoxes(eBounds, bounds).toString());
					System.out.println(docStyleParamName + " set to " + this.aggregateBoxes(eBounds, bounds).toString());
				}
			}
			
			//	list of bounding boxes, add new one and merge ones overlapping at least 90%
			else if (BoundingBox.class.getName().equals(DocumentStyle.getListElementClass(paramValueClass).getName())) {
				String boundsStr = this.getBoxListString(docStyle.getSetting(docStyleParamName), bounds);
				docStyle.setSetting(docStyleParamName, boundsStr);
				System.out.println(docStyleParamName + " set to " + boundsStr);
			}
		}
		
		//	if style editor open, adjust tree path
		if (this.docStyleEditor != null)
			this.docStyleEditor.setParamGroupName(docStyleParamGroupName);
		
		//	index document style for saving
		this.docStylesByDocId.put(doc.docId, docStyle);
	}
	
	private Settings getDocStyle(String docId, String docStyleName) {
		Settings docStyle = ((Settings) this.docStylesByDocId.get(docId));
		if (docStyle == null)
			docStyle = this.styleProvider.getStyle(docStyleName);
		if (docStyle == null) {
			docStyle = new Settings();
			docStyle.setSetting(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE, docStyleName);
		}
		return docStyle;
	}
	
	private static final String CREATE_DOC_STYLE = "<Create Document Style>";
	
	private SelectionAction[] getAssignDocStyleAction(final ImDocument doc) {
		SelectionAction[] adsa = {new SelectionAction("styleAssign", "Assign Document Style", "Assign a style template to, or create one for this document to help with markup automation") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				
				final JTextField cDocStyleOrigin = new JTextField();
				final JTextField cDocStyleYear = new JTextField();
				final JComboBox cDocStylePubType = new JComboBox(refTypeSystem.getBibRefTypeNames());
				cDocStylePubType.setEditable(false);
				
				final JPanel cDocStylePanel = new JPanel(new GridLayout(0, 2, 3, 3), true);
				cDocStylePanel.setBorder(BorderFactory.createEtchedBorder());
				cDocStylePanel.add(new JLabel(" Journal / Publisher: "));
				cDocStylePanel.add(cDocStyleOrigin);
				cDocStylePanel.add(new JLabel(" From Year: "));
				cDocStylePanel.add(cDocStyleYear);
				cDocStylePanel.add(new JLabel(" Publication Type: "));
				cDocStylePanel.add(cDocStylePubType);
				
				String[] docStyleNames = styleProvider.getResourceNames();
				for (int s = 0; s < docStyleNames.length; s++)
					docStyleNames[s] = docStyleNames[s].substring(0, docStyleNames[s].lastIndexOf('.'));
				
				final JComboBox docStyleSelector = new JComboBox(docStyleNames);
				docStyleSelector.insertItemAt(CREATE_DOC_STYLE, 0);
				docStyleSelector.setSelectedItem(CREATE_DOC_STYLE);
				docStyleSelector.setEditable(false);
				docStyleSelector.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						cDocStylePanel.setEnabled(CREATE_DOC_STYLE.equals(docStyleSelector.getSelectedItem()));
						cDocStyleOrigin.setEnabled(CREATE_DOC_STYLE.equals(docStyleSelector.getSelectedItem()));
						cDocStyleYear.setEnabled(CREATE_DOC_STYLE.equals(docStyleSelector.getSelectedItem()));
						cDocStylePubType.setEnabled(CREATE_DOC_STYLE.equals(docStyleSelector.getSelectedItem()));
					}
				});
				
				JPanel docStyleSelectorPanel = new JPanel(new BorderLayout(), true);
				docStyleSelectorPanel.add(new JLabel("Select Document Style: "), BorderLayout.WEST);
				docStyleSelectorPanel.add(docStyleSelector, BorderLayout.CENTER);
				docStyleSelectorPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 4, 0, docStyleSelectorPanel.getBackground()));
				
				JPanel docStylePanel = new JPanel(new BorderLayout(), true);
				docStylePanel.add(docStyleSelectorPanel, BorderLayout.NORTH);
				docStylePanel.add(cDocStylePanel, BorderLayout.CENTER);
				
				int choice = JOptionPane.showConfirmDialog(DialogPanel.getTopWindow(), docStylePanel, "Select or Create Document Style", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (choice != JOptionPane.OK_OPTION)
					return false;
				
				String selectedDocStyleName = ((String) docStyleSelector.getSelectedItem());
				if (CREATE_DOC_STYLE.equals(selectedDocStyleName)) {
					String origin = cDocStyleOrigin.getText().trim();
					if (origin.length() == 0)
						return false;
					origin = origin.toLowerCase().replaceAll("[^A-Za-z0-9\\-]+", "_");
					String year = cDocStyleYear.getText().trim();
					if ((year.length() == 0) || !year.matches("[0-9]{4}"))
						year = "0000";
					String pubType = ((String) cDocStylePubType.getSelectedItem());
					pubType = pubType.toLowerCase().replaceAll("\\s+", "_");
					selectedDocStyleName = (origin + "." + year + "." + pubType);
				}
				doc.setAttribute(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE, selectedDocStyleName);
				DocumentStyle.getStyleFor(doc);
				
				return true; // we _did_ change the document, if only by assigning attributes
			}
		}};
		return adsa;
	}
	
	private SelectionAction getEditDocStyleAction(final ImDocument doc, final String docStyleName, final Settings docStyle) {
		return new SelectionAction("styleEdit", "Edit Document Style", "Open the style template assigned to this document for editing") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				if (docStyleEditor == null)
					docStyleEditor = new DocStyleEditor();
				docStyleEditor.setDocStyle(doc, docStyleName, docStyle);
				docStyleEditor.setVisible(true);
				return false;
			}
		};
	}
	
	private DocStyleEditor docStyleEditor = null;
	
	private class DocStyleEditor extends DialogPanel {
		private JTree paramTree = new JTree();
		private JPanel paremPanel = new JPanel(new GridBagLayout(), true) {
			public void add(Component comp, Object constraints) {
				super.add(comp, ((GridBagConstraints) constraints).clone());
				((GridBagConstraints) constraints).gridy++;
			}
		};
		
		DocStyleEditor() {
			super("Edit Document Style Template", false);
			
			this.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					docStyleEditor = null;
				}
			});
			
			TreeSelectionModel ptsm = new DefaultTreeSelectionModel();
			ptsm.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			this.paramTree.setSelectionModel(ptsm);
			this.paramTree.addTreeSelectionListener(new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent tse) {
					paramTreeNodeSelected((ParamTreeNode) tse.getPath().getLastPathComponent());
				}
			});
			this.paramTree.setModel(this.paramTreeModel);
			
			JScrollPane paramTreeBox = new JScrollPane(this.paramTree);
			paramTreeBox.getHorizontalScrollBar().setBlockIncrement(20);
			paramTreeBox.getHorizontalScrollBar().setUnitIncrement(20);
			paramTreeBox.getVerticalScrollBar().setBlockIncrement(20);
			paramTreeBox.getVerticalScrollBar().setUnitIncrement(20);
			
			JSplitPane paramSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, paramTreeBox, this.paremPanel);
			
			this.add(paramSplit, BorderLayout.CENTER);
			this.setSize(400, 600);
			this.setLocationRelativeTo(this.getOwner());
		}
		
		String docStyleName = null;
		private Settings docStyle = null;
		
		private ImDocument testDoc;
		private ImTokenSequence testDocTokens;
		
		void setDocStyle(ImDocument doc, String docStyleName, Settings docStyle) {
			
			//	update test document
			if (this.testDoc != doc) {
				this.testDoc = doc;
				this.testDocTokens = null;
			}
			
			//	document style remains, we're done here
			if (docStyleName.equals(this.docStyleName))
				return;
			
			//	update data fields
			this.docStyleName = docStyleName;
			this.docStyle = docStyle;
			
			//	clear index fields
			this.paramGroupName = null;
			this.paramValueFields.clear();
			
			//	update window title
			this.setTitle("Edit Document Style Template '" + this.docStyleName + "'");
			
			//	get available parameter names, including ones from style proper (anchors !!!)
			TreeSet dsParamNameSet = new TreeSet(Arrays.asList(parameterValueClassNames.getKeys()));
			String[] dDsParamNames = docStyle.getKeys();
			for (int p = 0; p < dDsParamNames.length; p++) {
				if (dDsParamNames[p].startsWith("anchor."))
					dsParamNameSet.add(dDsParamNames[p]);
			}
			
			//	make sure we can create anchors
			dsParamNameSet.add("anchor.<create>.dummy");
			
			//	line up parameter names
			String[] dsParamNames = ((String[]) dsParamNameSet.toArray(new String[dsParamNameSet.size()]));
			Arrays.sort(dsParamNames);
			
			//	update parameter tree
			this.paramTreeRoot.clearChildren();
			this.paramTreeNodesByPrefix.clear();
			this.paramTreeNodesByPrefix.put(this.paramTreeRoot.prefix, this.paramTreeRoot);
			LinkedList ptnStack = new LinkedList();
			ptnStack.add(this.paramTreeRoot);
			for (int p = 0; p < dsParamNames.length; p++) {
				
				//	get current parent
				ParamTreeNode pptn = ((ParamTreeNode) ptnStack.getLast());
				
				//	ascend until prefix matches
				while ((pptn != this.paramTreeRoot) && !dsParamNames[p].startsWith(pptn.prefix + ".")) {
					ptnStack.removeLast();
					pptn = ((ParamTreeNode) ptnStack.getLast());
				}
				
				//	add more intermediate nodes for steps of current parameter
				while (pptn.prefix.length() < dsParamNames[p].lastIndexOf('.')) {
					ParamTreeNode ptn = new ParamTreeNode(dsParamNames[p].substring(0, dsParamNames[p].indexOf('.', (pptn.prefix.length() + 1))), pptn);
					pptn.addChild(ptn);
					pptn = ptn;
					ptnStack.addLast(pptn);
				}
				
				//	add parameter to parent tree node
				if (!"anchor.<create>.dummy".equals(dsParamNames[p]))
					pptn.addParamName(dsParamNames[p]);
			}
			
			//	update display
			this.updateParamTree();
		}
		
		private class ParamTreeNode implements Comparable{
			final String prefix;
			final ParamTreeNode parent;
			private ArrayList children = null;
			private TreeSet paramNames = null;
			ParamTreeNode(String prefix, ParamTreeNode parent) {
				this.prefix = prefix;
				this.parent = parent;
				paramTreeNodesByPrefix.put(this.prefix, this);
			}
			int getChildCount() {
				return ((this.children == null) ? 0 : this.children.size());
			}
			int getChildIndex(ParamTreeNode child) {
				return ((this.children == null) ? -1 : this.children.indexOf(child));
			}
			void addChild(ParamTreeNode child) {
				if (this.children == null)
					this.children = new ArrayList(3);
				this.children.add(child);
			}
			void removeChild(ParamTreeNode child) {
				if (this.children != null)
					this.children.remove(child);
			}
			void sortChildren() {
				if (this.children == null)
					return;
				Collections.sort(this.children);
				for (int c = 0; c < this.children.size(); c++)
					((ParamTreeNode) this.children.get(c)).sortChildren();
			}
			void clearChildren() {
				if (this.children != null)
					this.children.clear();
			}
			void addParamName(String paramName) {
				if (this.paramNames == null)
					this.paramNames = new TreeSet();
				this.paramNames.add(paramName);
			}
			
			public String toString() {
				return this.prefix.substring(this.prefix.lastIndexOf('.') + 1);
			}
			
			public int compareTo(Object obj) {
				ParamTreeNode ptn = ((ParamTreeNode) obj);
				if ((this.children == null) != (ptn.children == null))
					return ((this.children == null) ? -1 : 1);
				return this.prefix.compareTo(ptn.prefix);
			}
		}
		
		private TreeMap paramTreeNodesByPrefix = new TreeMap();
		private ParamTreeNode paramTreeRoot = new ParamTreeNode("", null);
		private ArrayList paramTreeModelListeners = new ArrayList(2);
		private TreeModel paramTreeModel = new TreeModel() {
			public Object getRoot() {
				return paramTreeRoot;
			}
			public boolean isLeaf(Object node) {
				return (((ParamTreeNode) node).getChildCount() == 0);
			}
			public int getChildCount(Object parent) {
				return ((ParamTreeNode) parent).getChildCount();
			}
			public Object getChild(Object parent, int index) {
				return ((ParamTreeNode) parent).children.get(index);
			}
			public int getIndexOfChild(Object parent, Object child) {
				return ((ParamTreeNode) parent).getChildIndex((ParamTreeNode) child);
			}
			public void valueForPathChanged(TreePath path, Object newValue) { /* we're not changing the tree */ }
			public void addTreeModelListener(TreeModelListener tml) {
				paramTreeModelListeners.add(tml);
			}
			public void removeTreeModelListener(TreeModelListener tml) {
				paramTreeModelListeners.remove(tml);
			}
		};
		private void updateParamTree() {
			this.paramTreeRoot.sortChildren();
			ArrayList expandedPaths = new ArrayList();
			for (int r = 0; r < this.paramTree.getRowCount(); r++) {
				if (this.paramTree.isExpanded(r))
					expandedPaths.add(this.paramTree.getPathForRow(r));
			}
			TreeModelEvent tme = new TreeModelEvent(this, new TreePath(this.paramTreeRoot));
			for (int l = 0; l < paramTreeModelListeners.size(); l++)
				((TreeModelListener) paramTreeModelListeners.get(l)).treeStructureChanged(tme);
			for (int r = 0; r < expandedPaths.size(); r++)
				this.paramTree.expandPath((TreePath) expandedPaths.get(r));
			this.paramTree.validate();
			this.paramTree.repaint();
		}
		
		void paramTreeNodeSelected(final ParamTreeNode ptn) {
			
			//	remember selected param group
			this.paramGroupName = ptn.prefix;
			
			//	clear param panel
			this.paremPanel.removeAll();
			
			//	update param panel
			if (ptn.paramNames != null) {
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.insets.left = 5;
				gbc.insets.right = 5;
				gbc.insets.top = 2;
				gbc.insets.bottom = 2;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.gridx = 0;
				gbc.weightx = 1;
				gbc.gridwidth = 1;
				gbc.gridy = 0;
				gbc.weighty = 0;
				gbc.gridheight = 1;
				
				gbc.weighty = 1;
				this.paremPanel.add(new JPanel(), gbc);
				gbc.weighty = 0;
				for (Iterator pnit = ptn.paramNames.iterator(); pnit.hasNext();)
					this.paremPanel.add(this.getParamValueField((String) pnit.next()), gbc);
				if (this.paramGroupName.startsWith("anchor.")) {
					JButton testButton = new JButton("Test Anchor");
					testButton.setBorder(BorderFactory.createRaisedBevelBorder());
					testButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							testAnchor(ptn);
						}
					});
					this.paremPanel.add(testButton, gbc);
					JButton removeButton = new JButton("Remove Anchor");
					removeButton.setBorder(BorderFactory.createRaisedBevelBorder());
					removeButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							removeAnchor(ptn);
						}
					});
					this.paremPanel.add(removeButton, gbc);
				}
				gbc.weighty = 1;
				this.paremPanel.add(new JPanel(), gbc);
			}
			
			//	make changes show
			this.paremPanel.validate();
			this.paremPanel.repaint();
		}
		
		void testAnchor(ParamTreeNode ptn) {
			
			//	get anchor settings
			Settings anchorParamList = this.docStyle.getSubset(ptn.prefix);
			
			//	get bounding box
			BoundingBox area = BoundingBox.parse(anchorParamList.getSetting("area"));
			if (area == null)
				return;
			
			//	get pattern
			String pattern = anchorParamList.getSetting("pattern");
			if (pattern == null)
				return;
			
			//	get font sizes and perform test
			try {
				boolean anchorMatch = DocumentStyleProvider.anchorMatches(this.testDoc, 
						area, 
						Integer.parseInt(anchorParamList.getSetting("minFontSize", anchorParamList.getSetting("fontSize", "0"))),
						Integer.parseInt(anchorParamList.getSetting("maxFontSize", anchorParamList.getSetting("fontSize", "72"))),
						"true".equals(anchorParamList.getSetting("isBold")),
						"true".equals(anchorParamList.getSetting("isItalics")),
						"true".equals(anchorParamList.getSetting("isAllCaps")),
						pattern);
				String anchorName = ptn.prefix.substring(ptn.prefix.lastIndexOf('.') + ".".length());
				JOptionPane.showMessageDialog(this, ("This document " + (anchorMatch ? " matches " : " does not match ") + " anchor '" + anchorName + "'"), "Anchor Match Test", (anchorMatch ? JOptionPane.PLAIN_MESSAGE : JOptionPane.ERROR_MESSAGE));
			} catch (NumberFormatException nfe) {}
		}
		
		void removeAnchor(ParamTreeNode ptn) {
			
			//	remove settings
			for (Iterator pnit = ptn.paramNames.iterator(); pnit.hasNext();)
				this.docStyle.removeSetting((String) pnit.next());
			
			//	remove node
			ParamTreeNode pptn = ptn.parent;
			pptn.removeChild(ptn);
			
			//	update param tree
			this.updateParamTree();
			
			//	select path of current tree node
			ArrayList pptnPath = new ArrayList();
			for (;pptn != null; pptn = pptn.parent)
				pptnPath.add(0, pptn);
			if (pptnPath.size() != 0)
				this.paramTree.setSelectionPath(new TreePath(pptnPath.toArray()));
		}
		
		String paramGroupName;
		
		void setParamGroupName(String pgn) {
			
			//	update fields for any parameters in group
			for (Iterator pnit = this.paramValueFields.keySet().iterator(); pnit.hasNext();) {
				String pn = ((String) pnit.next());
				if (pn.lastIndexOf('.') != pgn.length())
					continue;
				if (!pn.startsWith(pgn))
					continue;
				UseParamPanel pvf = ((UseParamPanel) this.paramValueFields.get(pn));
				String pv = this.docStyle.getSetting(pn);
				pvf.setValue((pv == null) ? "" : pv);
				pvf.useParam.setSelected(pv != null);
			}
			
			//	no further updates required
			if (pgn.equals(this.paramGroupName))
				return;
			
			//	set param group name and get corresponding tree node
			this.paramGroupName = pgn;
			ParamTreeNode ptn = ((ParamTreeNode) this.paramTreeNodesByPrefix.get(this.paramGroupName));
			
			//	if we're creating an anchor, and only then, the tree node is null
			if (ptn == null) {
				ParamTreeNode pptn = ((ParamTreeNode) this.paramTreeNodesByPrefix.get("anchor"));
				ptn = new ParamTreeNode(pgn, pptn);
				ptn.addParamName(this.paramGroupName + ".fontSize");
				ptn.addParamName(this.paramGroupName + ".minFontSize");
				ptn.addParamName(this.paramGroupName + ".maxFontSize");
				ptn.addParamName(this.paramGroupName + ".isBold");
				ptn.addParamName(this.paramGroupName + ".isItalics");
				ptn.addParamName(this.paramGroupName + ".isAllCaps");
				ptn.addParamName(this.paramGroupName + ".pattern");
				ptn.addParamName(this.paramGroupName + ".area");
				pptn.addChild(ptn);
				this.updateParamTree();
			}
			
			//	select path of current tree node
			ArrayList ptnPath = new ArrayList();
			for (;ptn != null; ptn = ptn.parent)
				ptnPath.add(0, ptn);
			if (ptnPath.size() != 0)
				this.paramTree.setSelectionPath(new TreePath(ptnPath.toArray()));
		}
		
		private TreeMap paramValueFields = new TreeMap();
		
		private UseParamPanel getParamValueField(final String pn) {
			UseParamPanel pvf = ((UseParamPanel) this.paramValueFields.get(pn));
			if (pvf == null) {
				pvf = this.createParamValueField(pn);
				this.paramValueFields.put(pn, pvf);
			}
			return pvf;
		}
		
		private class ParamToggleListener implements ItemListener {
			private UseParamPanel upp;
			ParamToggleListener(UseParamPanel upp) {
				this.upp = upp;
			}
			public void itemStateChanged(ItemEvent ie) {
				if (this.upp.useParam.isSelected()) {
					String dspv = this.upp.getValue();
					if (this.upp.verifyValue(dspv))
						docStyle.setSetting(this.upp.docStyleParamName, dspv);
				}
				else docStyle.removeSetting(this.upp.docStyleParamName);
			}
		}
		
		private UseParamPanel createParamValueField(final String pn) {
			final Class pvc = getParamValueClass(pn);
			String pv = ((this.docStyle == null) ? null : this.docStyle.getSetting(pn));
			
			//	boolean, use plain checkbox
			if (Boolean.class.getName().equals(pvc.getName())) {
				final UseBooleanPanel pvf = new UseBooleanPanel(pn, paramNamesLabels.getProperty(pn, pn), "true".equals(pv));
				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
				return pvf;
			}
			
			//	number, use string field
			else if (Integer.class.getName().equals(pvc.getName())) {
				final UseStringPanel pvf = new UseStringPanel(pn, (pv != null), ((pv == null) ? "" : pv), false) {
					boolean verifyValue(String value) {
						try {
							Integer.parseInt(this.getValue());
							return true;
						}
						catch (NumberFormatException nfe) {
							return false;
						}
					}
					void stringChanged(String string) {
						if (string.length() == 0)
							this.useParam.setSelected(false);
						else if (this.useParam.isSelected() && this.verifyValue(string))
							docStyle.setSetting(this.docStyleParamName, string);
					}
				};
				pvf.useParam.setText(" " + paramNamesLabels.getProperty(pn, pn));
				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
				return pvf;
			}
			else if (Float.class.getName().equals(pvc.getName())) {
				final UseStringPanel pvf = new UseStringPanel(pn, (pv != null), ((pv == null) ? "" : pv), false) {
					boolean verifyValue(String value) {
						try {
							Float.parseFloat(this.getValue());
							return true;
						}
						catch (NumberFormatException nfe) {
							return false;
						}
					}
					void stringChanged(String string) {
						if (string.length() == 0)
							this.useParam.setSelected(false);
						else if (this.useParam.isSelected() && this.verifyValue(string))
							docStyle.setSetting(this.docStyleParamName, string);
					}
				};
				pvf.useParam.setText(" " + paramNamesLabels.getProperty(pn, pn));
				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
				return pvf;
			}
			else if (Double.class.getName().equals(pvc.getName())) {
				final UseStringPanel pvf = new UseStringPanel(pn, (pv != null), ((pv == null) ? "" : pv), false) {
					boolean verifyValue(String value) {
						try {
							Double.parseDouble(this.getValue());
							return true;
						}
						catch (NumberFormatException nfe) {
							return false;
						}
					}
					void stringChanged(String string) {
						if (string.length() == 0)
							this.useParam.setSelected(false);
						else if (this.useParam.isSelected() && this.verifyValue(string))
							docStyle.setSetting(this.docStyleParamName, string);
					}
				};
				pvf.useParam.setText(" " + paramNamesLabels.getProperty(pn, pn));
				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
				return pvf;
			}
			
			//	bounding box, use string field
			else if (BoundingBox.class.getName().equals(pvc.getName())) {
				final UseStringPanel pvf = new UseStringPanel(pn, (pv != null), ((pv == null) ? "" : pv), false) {
					boolean verifyValue(String value) {
						try {
							return (BoundingBox.parse(this.getValue()) != null);
						}
						catch (RuntimeException re) {
							return false;
						}
					}
					void stringChanged(String string) {
						if (string.length() == 0)
							this.useParam.setSelected(false);
						else if (this.useParam.isSelected() && this.verifyValue(string))
							docStyle.setSetting(this.docStyleParamName, string);
					}
				};
				pvf.useParam.setText(" " + paramNamesLabels.getProperty(pn, pn));
				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
				return pvf;
			}
			
			//	string, use string field
			else if (String.class.getName().equals(pvc.getName())) {
				final UseStringPanel pvf = new UseStringPanel(pn, (pv != null), ((pv == null) ? "" : pv), false) {
					ImTokenSequence getTestDocTokens() {
						if (testDocTokens == null)
							testDocTokens = new ImTokenSequence(((Tokenizer) testDoc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER)), testDoc.getTextStreamHeads(), (ImTokenSequence.NORMALIZATION_LEVEL_PARAGRAPHS | ImTokenSequence.NORMALIZE_CHARACTERS));
						return testDocTokens;
					}
					boolean verifyValue(String value) {
						if (pn.endsWith(".pattern") || pn.endsWith("Pattern")) {
							try {
								Pattern.compile(value);
								return true;
							}
							catch (PatternSyntaxException pse) {
								return false;
							}
						}
						else return true;
					}
					void stringChanged(String string) {
						if (string.length() == 0)
							this.useParam.setSelected(false);
						else if (this.useParam.isSelected() && this.verifyValue(string))
							docStyle.setSetting(this.docStyleParamName, string);
					}
				};
				pvf.useParam.setText(" " + paramNamesLabels.getProperty(pn, pn));
				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
				return pvf;
			}
			
			//	list, use list field
			else if (DocumentStyle.getListElementClass(pvc) != pvc) {
				final Class pvlec = DocumentStyle.getListElementClass(pvc);
				final UseListPanel pvf = new UseListPanel(pn, (pv != null), ((pv == null) ? "" : pv)) {
					ImTokenSequence getTestDocTokens() {
						if (testDocTokens == null)
							testDocTokens = new ImTokenSequence(((Tokenizer) testDoc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER)), testDoc.getTextStreamHeads(), (ImTokenSequence.NORMALIZATION_LEVEL_PARAGRAPHS | ImTokenSequence.NORMALIZE_CHARACTERS));
						return testDocTokens;
					}
					boolean verifyValue(String value) {
						String[] valueParts = value.split("\\s+");
						for (int p = 0; p < valueParts.length; p++) {
							if (!this.verifyValuePart(valueParts[p]))
								return false;
						}
						return true;
					}
					boolean verifyValuePart(String valuePart) {
						if (Boolean.class.getName().equals(pvlec.getName()))
							return ("true".equals(valuePart) || "false".equals(valuePart));
						else if (Integer.class.getName().equals(pvlec.getName())) {
							try {
								Integer.parseInt(valuePart);
								return true;
							}
							catch (NumberFormatException nfe) {
								return false;
							}
						}
						else if (Float.class.getName().equals(pvlec.getName())) {
							try {
								Float.parseFloat(valuePart);
								return true;
							}
							catch (NumberFormatException nfe) {
								return false;
							}
						}
						else if (Double.class.getName().equals(pvlec.getName())) {
							try {
								Double.parseDouble(valuePart);
								return true;
							}
							catch (NumberFormatException nfe) {
								return false;
							}
						}
						else if (BoundingBox.class.getName().equals(pvlec.getName())) {
							try {
								return (BoundingBox.parse(this.getValue()) != null);
							}
							catch (RuntimeException re) {
								return false;
							}
						}
						else if (String.class.getName().equals(pvlec.getName())) {
							if (pn.endsWith(".patterns") || pn.endsWith("Patterns")) {
								try {
									Pattern.compile(valuePart);
									return true;
								}
								catch (PatternSyntaxException pse) {
									return false;
								}
							}
							else return true;
						}
						else return true;
					}
					void stringChanged(String string) {
						if (string.length() == 0)
							this.useParam.setSelected(false);
						else if (this.useParam.isSelected() && this.verifyValue(string))
							docStyle.setSetting(this.docStyleParamName, string.replaceAll("\\s+", " "));
					}
				};
				pvf.useParam.setText(" " + paramNamesLabels.getProperty(pn, pn));
				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
				return pvf;
			}
			
			//	as the ultimate fallback, use string field
			else {
				final UseStringPanel pvf = new UseStringPanel(pn, (pv != null), ((pv == null) ? "" : pv), false) {
					void stringChanged(String string) {
						if (string.length() == 0)
							this.useParam.setSelected(false);
						else if (this.useParam.isSelected() && this.verifyValue(string))
							docStyle.setSetting(this.docStyleParamName, string);
					}
				};
				pvf.useParam.setText(" " + paramNamesLabels.getProperty(pn, pn));
				pvf.useParam.addItemListener(new ParamToggleListener(pvf));
				return pvf;
			}
		}
	}
	
	private static final Properties paramNamesLabels = new Properties() {
		public String getProperty(String key, String defaultValue) {
			return super.getProperty(key.substring(key.lastIndexOf('.') + 1), defaultValue);
		}
	};
	static {
		paramNamesLabels.setProperty("isBold", "Require Values to be Bold");
		paramNamesLabels.setProperty("startIsBold", "Require Value Starts to be Bold");
		paramNamesLabels.setProperty("isItalics", "Require Values to be in Italics");
		paramNamesLabels.setProperty("startIsItalics", "Require Value Starts to be in Italics");
		paramNamesLabels.setProperty("isAllCaps", "Require Values to be All Caps");
		paramNamesLabels.setProperty("startIsAllCaps", "Require Value Starts to be All Caps");
		paramNamesLabels.setProperty("minFontSize", "Use Minimum Font Size");
		paramNamesLabels.setProperty("maxFontSize", "Use Maximum Font Size");
	}
	
	/* TODO 
- when loading document style parameter lists, copy them into genuine Properties object via putAll() instead of handing out view on Settings
- introduce "parent" parameter
  - load parent parameter list ...
  - ... and use it as default in handed out Properties
  ==> facilitates parameter value inheritance, vastly reducing maintenance effort
  	 */
	public static void main(String[] args) throws Exception {
		System.out.println(buildPattern("ZOOTAXA  109:"));
	}
}