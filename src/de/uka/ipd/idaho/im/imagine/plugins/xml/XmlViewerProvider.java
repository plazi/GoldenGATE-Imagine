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
package de.uka.ipd.idaho.im.imagine.plugins.xml;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.GenericMutableAnnotationWrapper;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.goldenGate.DocumentEditorDialog;
import de.uka.ipd.idaho.goldenGate.GoldenGATE;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;

/**
 * This plugin provides XML views of Image Markup documents, which simplifies
 * inspection of nested annotation structures. Annotations are editable in
 * these views, the document text to some degree.
 * 
 * @author sautter
 */
public class XmlViewerProvider extends AbstractImageMarkupToolProvider implements SelectionActionProvider {
	private static final String XML_VIEWER_IMT_NAME = "XmlViewer";
	
	private ImageMarkupTool xmlViewer = new XmlViewer();
	
	/** zero-argument constructor for class loading */
	public XmlViewerProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "IM XML Viewer";
	}
	
	private class XmlViewer implements ImageMarkupTool {
		public String getLabel() {
			return "View Document as XML";
		}
		public String getTooltip() {
			return "Show an XML based view of the document.";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			showXmlView(doc, annot, idmp);
		}
	}
	
	private Dimension idedSize = null;
	
	private class ImDocumentEditorDialog extends DocumentEditorDialog {
		private boolean changesCommitted = false;
		private int contentFlags;
		private int cContentFlags = -1;
		ImDocumentEditorDialog(GoldenGATE host, String title, MutableAnnotation content, int contentFlags) {
			super(host, title, content);
			
			this.contentFlags = contentFlags;
			
			JButton ok = new JButton("OK");
			ok.setBorder(BorderFactory.createRaisedBevelBorder());
			ok.setPreferredSize(new Dimension(100, 21));
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dispose(isContentModified());
				}
			});
			this.mainButtonPanel.add(ok);
			
			JButton cancel = new JButton("Cancel");
			cancel.setBorder(BorderFactory.createRaisedBevelBorder());
			cancel.setPreferredSize(new Dimension(100, 21));
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dispose(false);
				}
			});
			this.mainButtonPanel.add(cancel);
			
			JButton customize = new JButton("Customize");
			customize.setBorder(BorderFactory.createRaisedBevelBorder());
			customize.setPreferredSize(new Dimension(100, 21));
			customize.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					customizeContent();
				}
			});
			this.mainButtonPanel.add(customize);
		}
		
		public void dispose(boolean commit) {
			if (commit) {
				this.writeChanges();
				this.changesCommitted = true;
			}
			this.dispose();
		}
		
		void customizeContent() {
			JComboBox cNormalizationLevel = new JComboBox(NORMALIZATION_LEVELS);
			int normalizationLevel = (this.contentFlags & ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS);
			if (normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_RAW)
				cNormalizationLevel.setSelectedItem(RAW_NORMALIZATION_LEVEL);
			else if (normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_WORDS)
				cNormalizationLevel.setSelectedItem(WORD_NORMALIZATION_LEVEL);
			else if (normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS)
				cNormalizationLevel.setSelectedItem(STREAM_NORMALIZATION_LEVEL);
			else cNormalizationLevel.setSelectedItem(PARAGRAPH_NORMALIZATION_LEVEL);
			
			JCheckBox cNormalizeChars = new JCheckBox("Normalize Chars");
			cNormalizeChars.setSelected((this.contentFlags & ImDocumentRoot.NORMALIZE_CHARACTERS) != 0);
			JCheckBox cExcludeTables = new JCheckBox("Exclude Tables");
			cExcludeTables.setSelected((this.contentFlags & ImDocumentRoot.EXCLUDE_TABLES) != 0);
			JCheckBox cExcludeCaptionsFootnotes = new JCheckBox("Exclude Captions & Footnotes");
			cExcludeCaptionsFootnotes.setSelected((this.contentFlags & ImDocumentRoot.EXCLUDE_CAPTIONS_AND_FOOTNOTES) != 0);
			
			JPanel cPanel = new JPanel(new GridLayout(0, 1), true);
			cPanel.add(cNormalizationLevel);
			cPanel.add(cNormalizeChars);
			cPanel.add(cExcludeTables);
			cPanel.add(cExcludeCaptionsFootnotes);
			int cOptionType;
			if (this.isContentModified()) {
				cPanel.add(new JLabel("<HTML>The content of this XML View has been modified.<BR>Retain these modifications?</HTML>"));
				cOptionType = JOptionPane.YES_NO_CANCEL_OPTION;
			}
			else cOptionType = JOptionPane.OK_CANCEL_OPTION;
			
			int cChoice = JOptionPane.showConfirmDialog(this, cPanel, "Customize XML View", cOptionType, JOptionPane.PLAIN_MESSAGE);
			if ((cChoice == JOptionPane.CANCEL_OPTION) || (cChoice == JOptionPane.CLOSED_OPTION))
				return;
			
			if (RAW_NORMALIZATION_LEVEL.equals(cNormalizationLevel.getSelectedItem()))
				this.cContentFlags = ImDocumentRoot.NORMALIZATION_LEVEL_RAW;
			else if (WORD_NORMALIZATION_LEVEL.equals(cNormalizationLevel.getSelectedItem()))
				this.cContentFlags = ImDocumentRoot.NORMALIZATION_LEVEL_WORDS;
			else if (STREAM_NORMALIZATION_LEVEL.equals(cNormalizationLevel.getSelectedItem()))
				this.cContentFlags = ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS;
			else this.cContentFlags = ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS;
			
			if (cNormalizeChars.isSelected())
				this.cContentFlags |= ImDocumentRoot.NORMALIZE_CHARACTERS;
			if (cExcludeTables.isSelected())
				this.cContentFlags |= ImDocumentRoot.EXCLUDE_TABLES;
			if (cExcludeCaptionsFootnotes.isSelected())
				this.cContentFlags |= ImDocumentRoot.EXCLUDE_CAPTIONS_AND_FOOTNOTES;
			
			if (this.contentFlags != this.cContentFlags) // let's not recurse for nothing
				this.dispose(this.isContentModified() && (cChoice == JOptionPane.YES_OPTION));
		}
		
		boolean areChangesCommitted() {
			return this.changesCommitted;
		}
	}
	
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
	
	public boolean showXmlView(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp) {
		return this.showXmlView(doc, annot, ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS, idmp);
	}
	
	private boolean showXmlView(ImDocument doc, ImAnnotation annot, int xmlWrapperFlags, ImDocumentMarkupPanel idmp) {
		
		//	wrap document (we must not expose a GAMTA DocumentRoot directly, as then changes go right through)
		MutableAnnotation xmlDoc;
		if (annot == null)
			xmlDoc = new GenericMutableAnnotationWrapper(new ImDocumentRoot(doc, xmlWrapperFlags));
		else xmlDoc = new GenericMutableAnnotationWrapper(new ImDocumentRoot(annot, xmlWrapperFlags));
		
		//	find main window (we don't want to size after splash screen ...)
		Window topWindow = DialogPanel.getTopWindow();
		while (topWindow != null) {
			if (topWindow instanceof JFrame)
				break;
			Window topWindowOwner = topWindow.getOwner();
			if (topWindowOwner == null)
				break;
			topWindow = topWindowOwner;
		}
		
		//	initialize window size
		if ((this.idedSize == null) && (topWindow != null))
			this.idedSize = topWindow.getSize();
		
		//	use custom title, indicating wrapper flags
		StringBuffer title = new StringBuffer("XML View (");
		int normalizationLevel = (xmlWrapperFlags & ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS);
		if (normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS)
			title.append("text streams together");
		else if (normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS)
			title.append("paragraphs together");
		else if (normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_WORDS)
			title.append("words de-hyphenated");
		else if (normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_RAW)
			title.append("raw words");
		if ((xmlWrapperFlags & ImDocumentRoot.NORMALIZE_CHARACTERS) != 0)
			title.append(", special characters normalized");
		if ((xmlWrapperFlags & ImDocumentRoot.EXCLUDE_TABLES) != 0)
			title.append(", tables filtered");
		if ((xmlWrapperFlags & ImDocumentRoot.EXCLUDE_CAPTIONS_AND_FOOTNOTES) != 0)
			title.append(", captions & footnotes filtered");
		title.append(")");
		
		//	create, configure, and show editor dialog
		ImDocumentEditorDialog ided = new ImDocumentEditorDialog(this.parent, title.toString(), xmlDoc, xmlWrapperFlags);
		if (this.idedSize == null)
			ided.setSize(800, 600);
		else ided.setSize(this.idedSize);
		ided.setLocationRelativeTo(topWindow);
		ided.setVisible(true);
		
		//	not in a view customization recursion, indicate changes right away
		if (ided.cContentFlags == -1)
			return ided.areChangesCommitted();
		
		//	in a view customization recursion, indicate changes disjunctively
		else return (showXmlView(doc, annot, ided.cContentFlags, idmp) || ided.areChangesCommitted());
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (XML_VIEWER_IMT_NAME.equals(name))
			return this.xmlViewer;
		else return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider#getEditMenuItemNames()
	 */
	public String[] getEditMenuItemNames() {
		String[] emins = {XML_VIEWER_IMT_NAME};
		return emins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(ImWord start, ImWord end, final ImDocumentMarkupPanel idmp) {
		
		//	we only work on individual text streams
		if (!start.getTextStreamId().equals(end.getTextStreamId()))
			return null;
		
		//	collect painted annotations spanning or overlapping whole selection
		ImAnnotation[] allOverlappingAnnots = idmp.document.getAnnotationsOverlapping(start, end);
		LinkedList spanningAnnotList = new LinkedList();
		for (int a = 0; a < allOverlappingAnnots.length; a++) {
			if (!idmp.areAnnotationsPainted(allOverlappingAnnots[a].getType()))
				continue;
			if (ImUtils.textStreamOrder.compare(start, allOverlappingAnnots[a].getFirstWord()) < 0)
				continue;
			if (ImUtils.textStreamOrder.compare(allOverlappingAnnots[a].getLastWord(), end) < 0)
				continue;
			spanningAnnotList.add(allOverlappingAnnots[a]);
		}
		
		//	anything to work with?
		if (spanningAnnotList.isEmpty())
			return null;
		final ImAnnotation[] spanningAnnots = ((ImAnnotation[]) spanningAnnotList.toArray(new ImAnnotation[spanningAnnotList.size()]));
		
		//	collect available actions
		LinkedList actions = new LinkedList();
		
		//	show XML view of existing annotations
		if (spanningAnnots.length == 1)
			actions.add(new SelectionAction("showXmlView", ("Show XML View of " + spanningAnnots[0].getType()), ("Show XML view of '" + spanningAnnots[0].getType() + "' annotation.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return showXmlView(idmp.document, spanningAnnots[0], idmp);
				}
			});
		
		//	show XML view of existing annotations
		else actions.add(new SelectionAction("showXmlView", "Show XML View ...", "Show XML view of selected annotations.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				return false;
			}
			public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
				JMenu pm = new JMenu("Show XML View ...");
				JMenuItem mi;
				for (int a = 0; a < spanningAnnots.length; a++) {
					final ImAnnotation spanningAnnot = spanningAnnots[a];
					mi = new JMenuItem("- " + spanningAnnot.getType() + " '" + getAnnotationShortValue(spanningAnnot.getFirstWord(), spanningAnnot.getLastWord()) + "'");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (showXmlView(idmp.document, spanningAnnot, idmp)) {
								invoker.validate();
								invoker.repaint();
							}
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
	
	//	TODO move this to ImUtils (also from AnnotationActionProvider)
	private String getAnnotationShortValue(ImWord start, ImWord end) {
		if (start == end)
			return start.getString();
		else if (start.getNextWord() == end)
			return (start.getString() + (Gamta.insertSpace(start.getString(), end.getString()) ? " " : "") + end.getString());
		else return (start.getString() + " ... " + end.getString());
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, ImDocumentMarkupPanel idmp) {
		return null;
	}
}