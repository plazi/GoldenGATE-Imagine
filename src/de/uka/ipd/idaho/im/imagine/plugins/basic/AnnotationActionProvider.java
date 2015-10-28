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
package de.uka.ipd.idaho.im.imagine.plugins.basic;

import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
import de.uka.ipd.idaho.im.gamta.ImTokenSequence;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.TwoClickSelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.im.util.ImUtils.StringPair;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This class provides basic actions for editing annotations.
 * 
 * @author sautter
 */
public class AnnotationActionProvider extends AbstractSelectionActionProvider implements ImageMarkupToolProvider {
	
	private static final String ANNOT_RETYPER_IMT_NAME = "RetypeAnnots";
	private static final String ANNOT_REMOVER_IMT_NAME = "RemoveAnnots";
	private static final String ANNOT_DUPLICATE_REMOVER_IMT_NAME = "RemoveDuplicateAnnots";
	
	private ImageMarkupTool annotRetyper = new AnnotRetyper();
	private ImageMarkupTool annotRemover = new AnnotRemover();
	private ImageMarkupTool annotDuplicateRemover = new AnnotDuplicateRemover();
	
	private TreeSet annotTypes = new TreeSet(String.CASE_INSENSITIVE_ORDER);
	
	/** public zero-argument constructor for class loading */
	public AnnotationActionProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Annotation Actions";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	load pre-configured annotation types from config file
		try {
			StringVector annotTypes = StringVector.loadList(new InputStreamReader(this.dataProvider.getInputStream("annotationTypes.cnfg"), "UTF-8"));
			for (int t = 0; t < annotTypes.size(); t++) {
				String annotType = annotTypes.get(t).trim();
				if ((annotType.length() != 0) && !annotType.startsWith("//"))
					this.annotTypes.add(annotType);
			}
		} catch (IOException ioe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getEditMenuItemNames()
	 */
	public String[] getEditMenuItemNames() {
		String[] emins = {ANNOT_RETYPER_IMT_NAME, ANNOT_REMOVER_IMT_NAME, ANNOT_DUPLICATE_REMOVER_IMT_NAME};
		return emins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (ANNOT_RETYPER_IMT_NAME.equals(name))
			return this.annotRetyper;
		else if (ANNOT_REMOVER_IMT_NAME.equals(name))
			return this.annotRemover;
		else if (ANNOT_DUPLICATE_REMOVER_IMT_NAME.equals(name))
			return this.annotDuplicateRemover;
		else return null;
	}
	
	private class AnnotRetyper implements ImageMarkupTool {
		public String getLabel() {
			return "Change Annotation Type";
		}
		public String getTooltip() {
			return "Replace an annotation type with another one";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	get region types
			TreeSet allAnnotTypes = new TreeSet();
			TreeSet paintedAnnotTypes = new TreeSet();
			String[] annotTypes = doc.getAnnotationTypes();
			for (int t = 0; t < annotTypes.length; t++) {
				allAnnotTypes.add(annotTypes[t]);
				if (idmp.areAnnotationsPainted(annotTypes[t]))
					paintedAnnotTypes.add(annotTypes[t]);
			}
			
			//	nothing to work with
			if (allAnnotTypes.isEmpty()) {
				DialogFactory.alert("There are no annotations in this document.", "No Annotations", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			//	get annotation types
			annotTypes = (paintedAnnotTypes.isEmpty() ? ((String[]) allAnnotTypes.toArray(new String[allAnnotTypes.size()])) : ((String[]) paintedAnnotTypes.toArray(new String[paintedAnnotTypes.size()])));
			StringPair annotTypeChange = ImUtils.promptForObjectTypeChange("Select Annotation Type", "Select type of annotation to change", "Select or enter new annotation type", annotTypes, annotTypes[0], true);
			if (annotTypeChange == null)
				return;
			
			//	process annotations of selected type
			ImAnnotation[] annots = doc.getAnnotations(annotTypeChange.strOld);
			for (int a = 0; a < annots.length; a++)
				annots[a].setType(annotTypeChange.strNew);
			
			//	perform document annotation cleanup to deal with whatever duplicates we might have incurred
			doc.cleanupAnnotations();
		}
	}
	
	private class AnnotRemover implements ImageMarkupTool {
		public String getLabel() {
			return "Remove Annotations";
		}
		public String getTooltip() {
			return "Remove annotations from document";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	get annotations types
			TreeSet allAnnotTypes = new TreeSet();
			TreeSet paintedAnnotTypes = new TreeSet();
			String[] annotTypes = doc.getAnnotationTypes();
			for (int t = 0; t < annotTypes.length; t++) {
				allAnnotTypes.add(annotTypes[t]);
				if (idmp.areAnnotationsPainted(annotTypes[t]))
					paintedAnnotTypes.add(annotTypes[t]);
			}
			
			//	nothing to work with
			if (allAnnotTypes.isEmpty()) {
				DialogFactory.alert("There are no annotations in this document.", "No Annotations", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			//	get annotation types
			annotTypes = (paintedAnnotTypes.isEmpty() ? ((String[]) allAnnotTypes.toArray(new String[allAnnotTypes.size()])) : ((String[]) paintedAnnotTypes.toArray(new String[paintedAnnotTypes.size()])));
			String annotType = ImUtils.promptForObjectType("Select Annotation Type", "Select type of annotations", annotTypes, annotTypes[0], false);
			if (annotType == null)
				return;
			
			//	remove annotations of selected type
			ImAnnotation[] annots = doc.getAnnotations(annotType);
			for (int a = 0; a < annots.length; a++)
				doc.removeAnnotation(annots[a]);
		}
	}
	
	private class AnnotDuplicateRemover implements ImageMarkupTool {
		public String getLabel() {
			return "Remove Duplicate Annotations";
		}
		public String getTooltip() {
			return "Remove duplicate annotations from document";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			doc.cleanupAnnotations();
		}
	}
	
	private static final Comparator annotationOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			ImAnnotation annot1 = ((ImAnnotation) obj1);
			ImAnnotation annot2 = ((ImAnnotation) obj2);
			int c = ImUtils.textStreamOrder.compare(annot1.getFirstWord(), annot2.getFirstWord());
			return ((c == 0) ? ImUtils.textStreamOrder.compare(annot2.getLastWord(), annot1.getLastWord()) : c);
		}
	};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(final ImWord start, final ImWord end, final ImDocumentMarkupPanel idmp) {
		
		//	we only work on individual text streams
		if (!start.getTextStreamId().equals(end.getTextStreamId()))
			return null;
		
		//	collect painted annotations spanning or overlapping whole selection
		ImAnnotation[] allOverlappingAnnots = idmp.document.getAnnotationsOverlapping(start, end);
		LinkedList spanningAnnotList = new LinkedList();
		LinkedList overlappingAnnotList = new LinkedList();
		for (int a = 0; a < allOverlappingAnnots.length; a++) {
			if (!idmp.areAnnotationsPainted(allOverlappingAnnots[a].getType()))
				continue;
			overlappingAnnotList.add(allOverlappingAnnots[a]);
			if (ImUtils.textStreamOrder.compare(start, allOverlappingAnnots[a].getFirstWord()) < 0)
				continue;
			if (ImUtils.textStreamOrder.compare(allOverlappingAnnots[a].getLastWord(), end) < 0)
				continue;
			spanningAnnotList.add(allOverlappingAnnots[a]);
		}
		final ImAnnotation[] spanningAnnots = ((ImAnnotation[]) spanningAnnotList.toArray(new ImAnnotation[spanningAnnotList.size()]));
		final ImAnnotation[] overlappingAnnots = ((ImAnnotation[]) overlappingAnnotList.toArray(new ImAnnotation[overlappingAnnotList.size()]));
		
		//	sort annotations to reflect nesting order
		Arrays.sort(spanningAnnots, annotationOrder);
		Arrays.sort(overlappingAnnots, annotationOrder);
		
		//	index overlapping annotations by type to identify merger groups
		final TreeMap annotMergerGroups = new TreeMap();
		for (int a = 0; a < overlappingAnnots.length; a++) {
			LinkedList annotMergerGroup = ((LinkedList) annotMergerGroups.get(overlappingAnnots[a].getType()));
			if (annotMergerGroup == null) {
				annotMergerGroup = new LinkedList();
				annotMergerGroups.put(overlappingAnnots[a].getType(), annotMergerGroup);
			}
			annotMergerGroup.add(overlappingAnnots[a]);
		}
		for (Iterator atit = annotMergerGroups.keySet().iterator(); atit.hasNext();) {
			LinkedList annotMergerGroup = ((LinkedList) annotMergerGroups.get(atit.next()));
			if (annotMergerGroup.size() < 2)
				atit.remove();
		}
		
		//	get available annotation types
		final String[] annotTypes = idmp.document.getAnnotationTypes();
		final TreeSet paintedAnnotTypes = new TreeSet();
		for (int t = 0; t < annotTypes.length; t++) {
			if (idmp.areAnnotationsPainted(annotTypes[t]))
				paintedAnnotTypes.add(annotTypes[t]);
		}
		
		//	collect annotation types
		final TreeSet createAnnotTypes = new TreeSet();
		createAnnotTypes.addAll(paintedAnnotTypes);
		createAnnotTypes.addAll(this.annotTypes);
		for (int a = 0; a < overlappingAnnots.length; a++)
			createAnnotTypes.remove(overlappingAnnots[a].getType());
		
		//	collect available actions
		LinkedList actions = new LinkedList();
		
		//	create an annotation
		if (createAnnotTypes.isEmpty()) {
			actions.add(new SelectionAction("annotate", "Annotate", "Annotate selected words") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					String annotType = ImUtils.promptForObjectType("Enter Annotation Type", "Enter or select type of annotation to create", annotTypes, null, true);
					if (annotType == null)
						return false;
					ImWord fw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? start : end);
					ImWord lw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? end : start);
					idmp.document.addAnnotation(fw, lw, annotType);
					idmp.setAnnotationsPainted(annotType, true);
					return true;
				}
			});
			actions.add(new SelectionAction("annotateAll", "Annotate All", "Annotate all occurrences of selected words") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					String annotType = ImUtils.promptForObjectType("Enter Annotation Type", "Enter or select type of annotation to create", annotTypes, null, true);
					if (annotType == null)
						return false;
					annotateAll(idmp.document, start, end, annotType);
					idmp.setAnnotationsPainted(annotType, true);
					return true;
				}
			});
		}
		else {
			actions.add(new SelectionAction("annotate", "Annotate", "Annotate selected words") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Annotate");
					JMenuItem mi = new JMenuItem("Annotate ...");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							String annotType = ImUtils.promptForObjectType("Enter Annotation Type", "Enter or select type of annotation to create", annotTypes, null, true);
							if (annotType != null) {
								invoker.beginAtomicAction("Add '" + annotType + "' Annotation");
								ImWord fw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? start : end);
								ImWord lw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? end : start);
								idmp.document.addAnnotation(fw, lw, annotType);
								invoker.endAtomicAction();
								invoker.setAnnotationsPainted(annotType, true);
								invoker.validate();
								invoker.repaint();
							}
						}
					});
					pm.add(mi);
					for (Iterator atit = createAnnotTypes.iterator(); atit.hasNext();) {
						final String annotType = ((String) atit.next());
						mi = new JMenuItem("- " + annotType);
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								invoker.beginAtomicAction("Add '" + annotType + "' Annotation");
								ImWord fw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? start : end);
								ImWord lw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? end : start);
								ImAnnotation imAnnot = idmp.document.addAnnotation(fw, lw, annotType);
								invoker.endAtomicAction();
								invoker.setAnnotationsPainted(imAnnot.getType(), true);
								invoker.validate();
								invoker.repaint();
							}
						});
						pm.add(mi);
					}
					return pm;
				}
			});
			actions.add(new SelectionAction("annotateAll", "Annotate All", "Annotate all occurrences of selected words") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Annotate All");
					JMenuItem mi = new JMenuItem("Annotate All ...");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							String annotType = ImUtils.promptForObjectType("Enter Annotation Type", "Enter or select type of annotations to create", annotTypes, null, true);
							if (annotType != null) {
								invoker.beginAtomicAction("Annotate All");
								annotateAll(idmp.document, start, end, annotType);
								invoker.endAtomicAction();
								invoker.setAnnotationsPainted(annotType, true);
								invoker.validate();
								invoker.repaint();
							}
						}
					});
					pm.add(mi);
					for (Iterator atit = createAnnotTypes.iterator(); atit.hasNext();) {
						final String annotType = ((String) atit.next());
						mi = new JMenuItem("- " + annotType);
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								invoker.beginAtomicAction("Annotate All");
								annotateAll(idmp.document, start, end, annotType);
								invoker.setAnnotationsPainted(annotType, true);
								invoker.endAtomicAction();
								invoker.validate();
								invoker.repaint();
							}
						});
						pm.add(mi);
					}
					return pm;
				}
			});
		}
		
		//	single word selection (offer editing word attributes above same for other annotations)
		if (start == end)
			actions.add(new SelectionAction("editAttributesWord", "Edit Word Attributes", ("Edit attributes of '" + start.getString() + "'.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					idmp.editAttributes(start, start.getType(), start.getString());
					return true;
				}
			});
		
		//	single annotation selected
		if (spanningAnnots.length == 1) {
			
			//	edit attributes of existing annotations
			actions.add(new SelectionAction("editAttributesAnnot", ("Edit " + spanningAnnots[0].getType() + " Attributes"), ("Edit attributes of '" + spanningAnnots[0].getType() + "' annotation.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					StringBuffer spanningAnnotValue = new StringBuffer();
					for (ImWord imw = spanningAnnots[0].getFirstWord(); imw != null; imw = imw.getNextWord()) {
						if (imw.pageId != spanningAnnots[0].getFirstWord().pageId)
							break;
						spanningAnnotValue.append(imw.toString());
						if (imw == spanningAnnots[0].getLastWord())
							break;
					}
					idmp.editAttributes(spanningAnnots[0], spanningAnnots[0].getType(), spanningAnnotValue.toString());
					return true;
				}
			});
			
			//	remove existing annotation
			actions.add(new SelectionAction("removeAnnot", ("Remove " + spanningAnnots[0].getType() + " Annotation"), ("Remove '" + spanningAnnots[0].getType() + "' annotation.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					idmp.document.removeAnnotation(spanningAnnots[0]);
					return true;
				}
			});
			
			//	remove all existing annotation with selected value
			actions.add(new SelectionAction("removeAllAnnot", ("Remove All '" + getAnnotationShortValue(start, end) + "' " + spanningAnnots[0].getType() + " Annotations"), ("Remove all '" + spanningAnnots[0].getType() + "' annotations with value '" + getAnnotationShortValue(start, end) + "'.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					idmp.beginAtomicAction("Remove All");
					removeAll(idmp.document, spanningAnnots[0]);
					idmp.endAtomicAction();
					return true;
				}
			});
			
			//	change type of existing annotation
			actions.add(new SelectionAction("changeTypeAnnot", ("Change Annotation Type"), ("Change type of '" + spanningAnnots[0].getType() + "' annotation.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					String annotType = ImUtils.promptForObjectType("Enter Annotation Type", "Enter or select new annotation type", annotTypes, spanningAnnots[0].getType(), true);
					if (annotType == null)
						return false;
					spanningAnnots[0].setType(annotType);
					idmp.setAnnotationsPainted(annotType, true);
					return true;
				}
			});
		}
		
		/* multiple annotations selected; offering attribute editing, removal,
		 * and type change only if we have a single word selection or if there
		 * are no merger groups, i.e., if selection purpose is unlikely to be
		 * merging annotations
		 */
		else if ((spanningAnnots.length > 1) && (annotMergerGroups.isEmpty() || (start == end))) {
			
			//	edit attributes of existing annotations
			actions.add(new SelectionAction("editAttributesAnnot", "Edit Annotation Attributes ...", "Edit attributes of selected annotations.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Edit Annotation Attributes ...");
					JMenuItem mi;
					for (int a = 0; a < spanningAnnots.length; a++) {
						final ImAnnotation spanningAnnot = spanningAnnots[a];
						mi = new JMenuItem("- " + spanningAnnot.getType() + " '" + getAnnotationShortValue(spanningAnnot.getFirstWord(), spanningAnnot.getLastWord()) + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								StringBuffer spanningAnnotValue = new StringBuffer();
								for (ImWord imw = spanningAnnot.getFirstWord(); imw != null; imw = imw.getNextWord()) {
									if (imw.pageId != spanningAnnot.getFirstWord().pageId)
										break;
									spanningAnnotValue.append(imw.toString());
									if (imw == spanningAnnot.getLastWord())
										break;
								}
								idmp.editAttributes(spanningAnnot, spanningAnnot.getType(), spanningAnnotValue.toString());
								invoker.validate();
								invoker.repaint();
							}
						});
						pm.add(mi);
					}
					return pm;
				}
			});
			
			//	remove existing annotation
			actions.add(new SelectionAction("removeAnnot", "Remove Annotation ...", "Remove selected annotations.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Remove Annotation ...");
					JMenuItem mi;
					for (int a = 0; a < spanningAnnots.length; a++) {
						final ImAnnotation spanningAnnot = spanningAnnots[a];
						mi = new JMenuItem("- " + spanningAnnot.getType() + " '" + getAnnotationShortValue(spanningAnnot.getFirstWord(), spanningAnnot.getLastWord()) + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								invoker.beginAtomicAction("Remove Annotation");
								idmp.document.removeAnnotation(spanningAnnot);
								invoker.endAtomicAction();
								invoker.validate();
								invoker.repaint();
							}
						});
						pm.add(mi);
					}
					return pm;
				}
			});
			
			//	remove all existing annotation with selected value
			actions.add(new SelectionAction("removeAllAnnot", "Remove All Annotations ...", "Remove all annotations with selected value.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Remove All Annotations ...");
					JMenuItem mi;
					for (int a = 0; a < spanningAnnots.length; a++) {
						final ImAnnotation selectedAnnot = spanningAnnots[a];
						mi = new JMenuItem("- " + selectedAnnot.getType() + " '" + getAnnotationShortValue(start, end) + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								invoker.beginAtomicAction("Remove All");
								removeAll(idmp.document, selectedAnnot);
								invoker.endAtomicAction();
								invoker.validate();
								invoker.repaint();
							}
						});
						pm.add(mi);
					}
					return pm;
				}
			});
			
			//	change type of existing annotation
			actions.add(new SelectionAction("changeTypeAnnot", "Change Annotation Type ...", "Change the type of selected annotations.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Change Annotation Type ...");
					JMenuItem mi;
					for (int a = 0; a < spanningAnnots.length; a++) {
						final ImAnnotation spanningAnnot = spanningAnnots[a];
						mi = new JMenuItem("- " + spanningAnnot.getType() + " '" + spanningAnnot.getFirstWord().getString() + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								String annotType = ImUtils.promptForObjectType("Enter Annotation Type", "Enter or select new annotation type", annotTypes, spanningAnnot.getType(), true);
								if (annotType != null) {
									invoker.beginAtomicAction("Change Annotation Type");
									spanningAnnot.setType(annotType);
									idmp.setAnnotationsPainted(annotType, true);
									invoker.endAtomicAction();
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
		}
		
		//	single word selection
		if (start == end) {
			
			//	start annotation
			actions.add(new TwoClickSelectionAction("annotateTwoClick", "Start Annotation", ("Start annotation from '" + start.getString() + "'")) {
				public boolean performAction(ImWord secondWord) {
					if (!start.getTextStreamId().equals(secondWord.getTextStreamId())) {
						DialogFactory.alert(("Cannot annotate from '" + start.getString() + "' to '" + secondWord.getString() + "', they belong to different text streams:\r\n- '" + start.getString() + "': " + start.getTextStreamId() + ", type '" + start.getTextStreamType() + "'\r\n- '" + secondWord.getString() + "': " + secondWord.getTextStreamId() + ", type '" + secondWord.getTextStreamType() + "'"), "Cannot Annotate Across Text Streams", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					String annotType = ImUtils.promptForObjectType("Enter Annotation Type", "Enter or select type of annotation to create", ((String[]) createAnnotTypes.toArray(new String[createAnnotTypes.size()])), null, true);
					if (annotType == null)
						return false;
					ImWord fw = ((ImUtils.textStreamOrder.compare(start, secondWord) < 0) ? start : secondWord);
					ImWord lw = ((ImUtils.textStreamOrder.compare(start, secondWord) < 0) ? secondWord : start);
					ImAnnotation imAnnot = idmp.document.addAnnotation(fw, lw, annotType.toString().trim());
					idmp.setAnnotationsPainted(imAnnot.getType(), true);
					return true;
				}
				public ImWord getFirstWord() {
					return start;
				}
				public String getActiveLabel() {
					return ("Add annotation starting from '" + start.getString() + "'");
				}
			});
			
			//	split annotation before selected word
			for (int a = 0; a < spanningAnnots.length; a++) {
				if ((spanningAnnots[a].getFirstWord() == start) || (start.getPreviousWord() == null))
					continue;
				final ImAnnotation spanningAnnot = spanningAnnots[a];
				actions.add(new SelectionAction("splitAnnotBefore", ("Split " + spanningAnnot.getType() + " Before"), ("Split the '" + spanningAnnot.getType() + "' anotation before '" + start.getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						ImAnnotation imAnnot = idmp.document.addAnnotation(start, spanningAnnot.getLastWord(), spanningAnnot.getType());
						imAnnot.copyAttributes(spanningAnnot);
						spanningAnnot.setLastWord(start.getPreviousWord());
						idmp.document.cleanupAnnotations();
						return true;
					}
				});
			}
			
			//	split annotation after selected word
			for (int a = 0; a < spanningAnnots.length; a++) {
				if ((spanningAnnots[a].getLastWord() == start) || (start.getNextWord() == null))
					continue;
				final ImAnnotation spanningAnnot = spanningAnnots[a];
				actions.add(new SelectionAction("splitAnnotAfter", ("Split " + spanningAnnot.getType() + " After"), ("Split the '" + spanningAnnot.getType() + "' anotation after '" + start.getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						ImAnnotation imAnnot = idmp.document.addAnnotation(start.getNextWord(), spanningAnnot.getLastWord(), spanningAnnot.getType());
						imAnnot.copyAttributes(spanningAnnot);
						spanningAnnot.setLastWord(start);
						idmp.document.cleanupAnnotations();
						return true;
					}
				});
			}
		}
		
		//	merge annotations (can also occur with single word selection if there are overlapping annotations)
		for (Iterator atit = annotMergerGroups.keySet().iterator(); atit.hasNext();) {
			final String type = ((String) atit.next());
			final LinkedList annotMergerGroup = ((LinkedList) annotMergerGroups.get(type));
			actions.add(new SelectionAction("mergeAnnots", ("Merge " + type + "s"), ("Merge selected '" + type + "' annotations")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					ImAnnotation mergedAnnot = ((ImAnnotation) annotMergerGroup.removeFirst());
					ImWord mergedLastWord = mergedAnnot.getLastWord();
					for (Iterator ait = annotMergerGroup.iterator(); ait.hasNext();) {
						ImAnnotation annot = ((ImAnnotation) ait.next());
						ImWord lastWord = annot.getLastWord();
						if (ImUtils.textStreamOrder.compare(mergedLastWord, lastWord) < 0)
							mergedLastWord = lastWord;
						AttributeUtils.copyAttributes(annot, mergedAnnot, AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
						idmp.document.removeAnnotation(annot);
					}
					if (mergedLastWord != mergedAnnot.getLastWord())
						mergedAnnot.setLastWord(mergedLastWord);
					idmp.document.cleanupAnnotations();
					return true;
				}
			});
		}
		
		//	offer extending overlapping annotations that are not in a merger group
		for (int a = 0; a < overlappingAnnots.length; a++) {
			if (annotMergerGroups.containsKey(overlappingAnnots[a].getType()))
				continue;
			final boolean extendStart = (ImUtils.textStreamOrder.compare(start, overlappingAnnots[a].getFirstWord()) < 0);
			final boolean extendEnd = (ImUtils.textStreamOrder.compare(overlappingAnnots[a].getLastWord(), end) < 0);
			if (!extendStart && !extendEnd)
				continue;
			final ImAnnotation overlappingAnnot = overlappingAnnots[a];
			actions.add(new SelectionAction("extendAnnot", ("Extend " + overlappingAnnot.getType()), ("Extend selected '" + overlappingAnnot.getType() + "' annotation to " + (extendStart ? ("'" + start.getString() + "'") : "") + (extendEnd ? ((extendStart ? " and " : "") + ("'" + end.getString() + "'")) : ""))) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					if (extendStart)
						overlappingAnnot.setFirstWord(start);
					if (extendEnd)
						overlappingAnnot.setLastWord(end);
					idmp.document.cleanupAnnotations();
					return true;
				}
			});
		}
		
		//	copy spanning annotations (a) as plain text and (b) as XML
		if (spanningAnnots.length == 1) {
			actions.add(new SelectionAction("copyAnnotTXT", ("Copy " + spanningAnnots[0].getType() + " Text"), ("Copy the text annotated as '" + spanningAnnots[0].getType() + "' to the system clipboard")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					ImUtils.copy(new StringSelection(ImUtils.getString(spanningAnnots[0].getFirstWord(), spanningAnnots[0].getLastWord(), true)));
					return false;
				}
			});
			actions.add(new SelectionAction("copyAnnotXML", ("Copy " + spanningAnnots[0].getType() + " XML"), ("Copy the '" + spanningAnnots[0].getType() + "' annotation to the system clipboard as XML")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					copyAnnotationXML(spanningAnnots[0]);
					return false;
				}
			});
		}
		else if (spanningAnnots.length > 1) {
			actions.add(new SelectionAction("copyAnnotTXT", "Copy Annotation Text", "Copy the text of annnotations to the system clipboard") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Copy Annotation Text ...");
					JMenuItem mi;
					for (int a = 0; a < spanningAnnots.length; a++) {
						final ImAnnotation spanningAnnot = spanningAnnots[a];
						mi = new JMenuItem("- " + spanningAnnot.getType() + " '" + spanningAnnot.getFirstWord().getString() + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								ImUtils.copy(new StringSelection(ImUtils.getString(spanningAnnot.getFirstWord(), spanningAnnot.getLastWord(), true)));
							}
						});
						pm.add(mi);
					}
					return pm;
				}
			});
			actions.add(new SelectionAction("copyAnnotXML", "Copy Annotation XML", "Copy annnotations to the system clipboard as XML") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Copy Annotation XML ...");
					JMenuItem mi;
					for (int a = 0; a < spanningAnnots.length; a++) {
						final ImAnnotation spanningAnnot = spanningAnnots[a];
						mi = new JMenuItem("- " + spanningAnnot.getType() + " '" + spanningAnnot.getFirstWord().getString() + "'");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								copyAnnotationXML(spanningAnnot);
							}
						});
						pm.add(mi);
					}
					return pm;
				}
			});
		}
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private String getAnnotationShortValue(ImWord start, ImWord end) {
		if (start == end)
			return start.getString();
		else if (start.getNextWord() == end)
			return (start.getString() + (Gamta.insertSpace(start.getString(), end.getString()) ? " " : "") + end.getString());
		else return (start.getString() + " ... " + end.getString());
	}
	
	private void copyAnnotationXML(final ImAnnotation annot) {
		
		//	wrap annotation
		ImDocumentRoot wrappedAnnot = new ImDocumentRoot(annot, ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS);
		
		//	write annotation as XML (filter out implicit paragraphs, though)
		StringWriter annotData = new StringWriter();
		try {
			AnnotationUtils.writeXML(wrappedAnnot, annotData, true, new HashSet() {
				public boolean contains(Object obj) {
					return (MutableAnnotation.PARAGRAPH_TYPE.equals(annot.getType()) || !MutableAnnotation.PARAGRAPH_TYPE.equals(obj));
				}
			});
		} catch (IOException ioe) {}
		
		//	put data in clipboard
		ImUtils.copy(new StringSelection(annotData.toString()));
	}
	
	private void annotateAll(ImDocument doc, ImWord start, ImWord end, String annotType) {
		
		//	annotate selected occurrence
		ImWord fw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? start : end);
		ImWord lw = ((ImUtils.textStreamOrder.compare(start, end) < 0) ? end : start);
		ImAnnotation annot = doc.addAnnotation(fw, lw, annotType);
		
		//	wrap document (we can normalize all the way, as this is fastest, and annotations refer to a single text stream anyway)
		ImDocumentRoot wrapperDoc = new ImDocumentRoot(doc, ImTokenSequence.NORMALIZATION_LEVEL_STREAMS);
		
		//	extract all occurrences of selected text
		StringVector dictionary = new StringVector();
		dictionary.addElement(TokenSequenceUtils.concatTokens(new ImTokenSequence(annot.getFirstWord(), annot.getLastWord()), true, true));
		Annotation[] annots = Gamta.extractAllContained(wrapperDoc, dictionary);
		
		//	add annotations (we can simply add them all, as ImDocument prevents duplicates internally)
		for (int a = 0; a < annots.length; a++)
			wrapperDoc.addAnnotation(annotType, annots[a].getStartIndex(), annots[a].size());
	}
	
	private void removeAll(ImDocument doc, ImAnnotation annot) {
		
		//	get all annotations of selected type
		ImAnnotation[] annots = doc.getAnnotations(annot.getType());
		
		//	get annotation value
		String removeValue = TokenSequenceUtils.concatTokens(new ImTokenSequence(annot.getFirstWord(), annot.getLastWord()), true, true);
		
		//	remove annotations
		for (int a = 0; a < annots.length; a++) {
			String annotValue = TokenSequenceUtils.concatTokens(new ImTokenSequence(annots[a].getFirstWord(), annots[a].getLastWord()), true, true);
			if (annotValue.equals(removeValue))
				doc.removeAnnotation(annots[a]);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, final ImDocumentMarkupPanel idmp) {
		
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
}