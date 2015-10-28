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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.TwoClickSelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.im.util.ImfIO;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * This class provides basic actions for editing logical text streams.
 * 
 * @author sautter
 */
public class TextStreamActionProvider extends AbstractSelectionActionProvider implements ImageMarkupToolProvider {
	
	/** public zero-argument constructor for class loading */
	public TextStreamActionProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Text Stream Actions";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(final ImWord start, final ImWord end, final ImDocumentMarkupPanel idmp) {
		
		//	get selection actions
		LinkedList actions = new LinkedList();
		
		//	test if paragraphs to merge
		boolean paragraphsToMerge = false;
		if (start.getTextStreamId().equals(end.getTextStreamId()) && (start != end))
			for (ImWord imw = start.getNextWord(); imw != null; imw = imw.getNextWord()) {
				if (imw.getPreviousWord().getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END) {
					paragraphsToMerge = true;
					break;
				}
				if (imw == end)
					break;
			}
		
		//	we're not offering text stream editing if text streams are not visualized, only word relations
		if (!idmp.areTextStreamsPainted()) {
			if ((start == end) && (start.getNextWord() != null))
				actions.add(new SelectionAction("streamWordRelation", "Set Next Word Relation", ("Set Relation between '" + start.getString() + "' and its Successor '" + start.getNextWord().getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return false;
					}
					public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
						JMenu pm = new JMenu("Set Next Word Relation");
						ButtonGroup bg = new ButtonGroup();
						final JMenuItem smi = new JRadioButtonMenuItem("Separate Word", (start.getNextRelation() == ImWord.NEXT_RELATION_SEPARATE));
						smi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (smi.isSelected()) {
									setNextRelation(ImWord.NEXT_RELATION_SEPARATE, invoker);
								}
							}
						});
						pm.add(smi);
						bg.add(smi);
						final JMenuItem pmi = new JRadioButtonMenuItem("Separate Word with Pararaph Break After", (start.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
						pmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (pmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END, invoker);
							}
						});
						pm.add(pmi);
						bg.add(pmi);
						final JMenuItem hmi = new JRadioButtonMenuItem("First Part of Hyphenated Word", (start.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED));
						hmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (hmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_HYPHENATED, invoker);
							}
						});
						pm.add(hmi);
						bg.add(hmi);
						final JMenuItem cmi = new JRadioButtonMenuItem(((start.bounds.left <= start.getNextWord().bounds.left) ? "First Part of Split Word" : "First Part of Line-Broken Word"), (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
						cmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (cmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
							}
						});
						pm.add(cmi);
						bg.add(cmi);
//						ImWord startNext = start.getNextWord();
//						if ((start.centerX < startNext.bounds.left) && (startNext.bounds.top < start.centerY) && (start.centerY < startNext.bounds.bottom)) {
//							final JMenuItem cmi = new JRadioButtonMenuItem("First Part of Split Word", (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
//							cmi.addActionListener(new ActionListener() {
//								public void actionPerformed(ActionEvent ae) {
//									if (cmi.isSelected())
//										setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
//								}
//							});
//							pm.add(cmi);
//							bg.add(cmi);
//						}
						return pm;
					}
					private void setNextRelation(char nextRelation, ImDocumentMarkupPanel invoker) {
						invoker.beginAtomicAction("Set Next Word Relation");
						start.setNextRelation(nextRelation);
						invoker.endAtomicAction();
						invoker.validate();
						invoker.repaint();
					}
				});
			else if (start.getNextWord() == end)
				actions.add(new SelectionAction("streamWordRelation", "Set Word Relation", ("Set word relation between '" + start.getString() + "' and '" + end.getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return false;
					}
					public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
						JMenu pm = new JMenu("Set Word Relation");
						ButtonGroup bg = new ButtonGroup();
						final JMenuItem smi = new JRadioButtonMenuItem("Two Words", (start.getNextRelation() == ImWord.NEXT_RELATION_SEPARATE));
						smi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (smi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_SEPARATE, invoker);
							}
						});
						pm.add(smi);
						bg.add(smi);
						final JMenuItem pmi = new JRadioButtonMenuItem("Two Words with Pararaph Break", (start.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
						pmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (pmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END, invoker);
							}
						});
						pm.add(pmi);
						bg.add(pmi);
						final JMenuItem hmi = new JRadioButtonMenuItem("Hyphenated Word", (start.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED));
						hmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (hmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_HYPHENATED, invoker);
							}
						});
						pm.add(hmi);
						bg.add(hmi);
						final JMenuItem cmi = new JRadioButtonMenuItem(((start.bounds.left <= start.getNextWord().bounds.left) ? "Same Word" : "Line-Broken Word"), (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
						cmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (cmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
							}
						});
						pm.add(cmi);
						bg.add(cmi);
//						if ((start.centerX < end.bounds.left) && (end.bounds.top < start.centerY) && (start.centerY < end.bounds.bottom)) {
//							final JMenuItem cmi = new JRadioButtonMenuItem("Same Word", (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
//							cmi.addActionListener(new ActionListener() {
//								public void actionPerformed(ActionEvent ae) {
//									if (cmi.isSelected())
//										setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
//								}
//							});
//							pm.add(cmi);
//							bg.add(cmi);
//						}
						return pm;
					}
					private void setNextRelation(char nextRelation, ImDocumentMarkupPanel invoker) {
						invoker.beginAtomicAction("Set Word Relation");
						start.setNextRelation(nextRelation);
						invoker.endAtomicAction();
						invoker.validate();
						invoker.repaint();
					}
				});
			else if (start.getTextStreamId().equals(end.getTextStreamId()) && (start.pageId == end.pageId) && (start.bounds.top < end.centerY) && (end.centerY < start.bounds.bottom) && (end.getTextStreamPos() <= (start.getTextStreamPos() + 20))) {
				boolean singleLine = true;
				for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
					if ((imw.centerY < start.bounds.top) || (start.bounds.bottom < imw.centerY)) {
						singleLine = false;
						break;
					}
					if (imw == end)
						break;
				}
				if (singleLine)
					actions.add(new SelectionAction("streamMergeWords", "Merge Words", "Merge selected words into one.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
								if (imw == end)
									break;
								imw.setNextRelation(ImWord.NEXT_RELATION_CONTINUE);
							}
							return true;
						}
					});
			}
			
			//	merge paragraphs
			if (paragraphsToMerge)
				actions.add(new SelectionAction("streamMergeParas", "Merge Paragraphs", "Merge selected paragraphs into one.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
							if (imw == end)
								break;
							if (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
								imw.setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
						}
						return true;
					}
				});
			
			//	copy selected text
			if (start.getTextStreamId().equals(end.getTextStreamId()))
				actions.add(new SelectionAction("copyWordsTXT", "Copy Text", "Copy the selected words to the system clipboard.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						ImUtils.copy(new StringSelection(ImUtils.getString(start, end, false)));
						return false;
					}
				});
			
			//	finally ...
			return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
		}
		
		//	single word selection
		if (start == end) {
			if (start.getNextWord() != null) {
				actions.add(new SelectionAction("streamWordRelation", "Set Next Word Relation", ("Set Relation between '" + start.getString() + "' and its Successor '" + start.getNextWord().getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return false;
					}
					public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
						JMenu pm = new JMenu("Set Next Word Relation");
						ButtonGroup bg = new ButtonGroup();
						final JMenuItem smi = new JRadioButtonMenuItem("Separate Word", (start.getNextRelation() == ImWord.NEXT_RELATION_SEPARATE));
						smi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (smi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_SEPARATE, invoker);
							}
						});
						pm.add(smi);
						bg.add(smi);
						final JMenuItem pmi = new JRadioButtonMenuItem("Separate Word with Pararaph Break After", (start.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
						pmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (pmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END, invoker);
							}
						});
						pm.add(pmi);
						bg.add(pmi);
						final JMenuItem hmi = new JRadioButtonMenuItem("First Part of Hyphenated Word", (start.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED));
						hmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (hmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_HYPHENATED, invoker);
							}
						});
						pm.add(hmi);
						bg.add(hmi);
						final JMenuItem cmi = new JRadioButtonMenuItem(((start.bounds.left <= start.getNextWord().bounds.left) ? "First Part of Split Word" : "First Part of Line-Broken Word"), (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
						cmi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								if (cmi.isSelected())
									setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
							}
						});
						pm.add(cmi);
						bg.add(cmi);
//						if (start.bounds.right <= start.getNextWord().bounds.left) {
//							final JMenuItem cmi = new JRadioButtonMenuItem("First Part of Split Word", (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
//							cmi.addActionListener(new ActionListener() {
//								public void actionPerformed(ActionEvent ae) {
//									if (cmi.isSelected())
//										setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
//								}
//							});
//							pm.add(cmi);
//							bg.add(cmi);
//						}
						return pm;
					}
					private void setNextRelation(char nextRelation, ImDocumentMarkupPanel invoker) {
						invoker.beginAtomicAction("Set Next Word Relation");
						start.setNextRelation(nextRelation);
						invoker.endAtomicAction();
						invoker.validate();
						invoker.repaint();
					}
				});
			}
			if (start.getPreviousWord() != null)
				actions.add(new SelectionAction("streamCutBefore", "Cut Stream Before", ("Cut text stream before '" + start.getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						start.setPreviousWord(null);
						return true;
					}
				});
			if (start.getNextWord() != null)
				actions.add(new SelectionAction("streamCutAfter", "Cut Stream After", ("Cut text stream after '" + start.getString() + "'")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						start.setNextWord(null);
						return true;
					}
				});
			actions.add(new TwoClickSelectionAction("streamMergeBackward", "Click Predeccessor", "Mark words, and set its predeccessor by clicking another word") {
				public boolean performAction(ImWord secondWord) {
					if (start.getTextStreamId().equals(secondWord.getTextStreamId()) && ((secondWord.pageId > start.pageId) || ((secondWord.pageId == start.pageId) && (secondWord.getTextStreamPos() >= start.getTextStreamPos())))) {
						DialogFactory.alert(("'" + secondWord.getString() + "' cannot be the predecessor of '" + start.getString() + "'\nThey belong to the same logical text stream,\nand '" + start.getString() + "' is a treansitive predecessor of '" + secondWord.getString() + "'"), "Cannot Set Predecessor", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					else if (start.getPreviousWord() == secondWord)
						return false;
					else {
						start.setPreviousWord(secondWord);
						return true;
					}
				}
				public ImWord getFirstWord() {
					return start;
				}
				public String getActiveLabel() {
					return ("Click predecessor of '" + start.getString() + "'");
				}
			});
			actions.add(new TwoClickSelectionAction("streamMergeForward", "Click Successor", "Mark words, and set its successor by clicking another word") {
				public boolean performAction(ImWord secondWord) {
					if (start.getTextStreamId().equals(secondWord.getTextStreamId()) && ((secondWord.pageId < start.pageId) || ((secondWord.pageId == start.pageId) && (secondWord.getTextStreamPos() <= start.getTextStreamPos())))) {
						DialogFactory.alert(("'" + secondWord.getString() + "' cannot be the successor of '" + start.getString() + "'\nThey belong to the same logical text stream,\nand '" + start.getString() + "' is a treansitive successor of '" + secondWord.getString() + "'"), "Cannot Set Successor", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					else if (start.getNextWord() == secondWord)
						return false;
					else {
						start.setNextWord(secondWord);
						return true;
					}
				}
				public ImWord getFirstWord() {
					return start;
				}
				public String getActiveLabel() {
					return ("Click successor of '" + start.getString() + "'");
				}
			});
		}
		
		//	two words, different streams
		else if (!start.getTextStreamId().equals(end.getTextStreamId())) {
			actions.add(new SelectionAction("streamMergeForward", "Make Successor", ("Make '" + end.getString() + "' successor of '" + start.getString() + "'")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					System.out.println("Setting successor of " + start + " to " + end);
					ImWord oldStartNext = start.getNextWord();
					System.out.println("Old start next is " + oldStartNext);
					start.setNextWord(end);
					if (oldStartNext == null)
						return true;
					ImWord imw = end;
					while (imw != null) {
						if (imw.getNextWord() == null) {
							oldStartNext.setPreviousWord(imw);
							System.out.println("Setting predecessor of " + oldStartNext + " to " + imw + " at " + imw.getLocalID());
							break;
						}
						imw = imw.getNextWord();
					}
					return true;
				}
			});
		}
		
		//	two words, same stream
		else if (start.getNextWord() == end) {
			actions.add(new SelectionAction("streamWordRelation", "Set Word Relation", ("Set word relation between '" + start.getString() + "' and '" + end.getString() + "'")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Set Word Relation");
					ButtonGroup bg = new ButtonGroup();
					final JMenuItem smi = new JRadioButtonMenuItem("Two Words", (start.getNextRelation() == ImWord.NEXT_RELATION_SEPARATE));
					smi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (smi.isSelected())
								setNextRelation(ImWord.NEXT_RELATION_SEPARATE, invoker);
						}
					});
					pm.add(smi);
					bg.add(smi);
					final JMenuItem pmi = new JRadioButtonMenuItem("Two Words with Pararaph Break", (start.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END));
					pmi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (pmi.isSelected())
								setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END, invoker);
						}
					});
					pm.add(pmi);
					bg.add(pmi);
					final JMenuItem hmi = new JRadioButtonMenuItem("Hyphenated Word", (start.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED));
					hmi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (hmi.isSelected())
								setNextRelation(ImWord.NEXT_RELATION_HYPHENATED, invoker);
						}
					});
					pm.add(hmi);
					bg.add(hmi);
					final JMenuItem cmi = new JRadioButtonMenuItem(((start.bounds.left <= end.bounds.left) ? "Same Word" : "Line-Broken Word"), (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
					cmi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							if (cmi.isSelected())
								setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
						}
					});
					pm.add(cmi);
					bg.add(cmi);
//					if (start.bounds.right <= end.bounds.left) {
//						final JMenuItem cmi = new JRadioButtonMenuItem("Same Word", (start.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE));
//						cmi.addActionListener(new ActionListener() {
//							public void actionPerformed(ActionEvent ae) {
//								if (cmi.isSelected())
//									setNextRelation(ImWord.NEXT_RELATION_CONTINUE, invoker);
//							}
//						});
//						pm.add(cmi);
//						bg.add(cmi);
//					}
					return pm;
				}
				private void setNextRelation(char nextRelation, ImDocumentMarkupPanel invoker) {
					invoker.beginAtomicAction("Set Word Relation");
					start.setNextRelation(nextRelation);
					invoker.endAtomicAction();
					invoker.validate();
					invoker.repaint();
				}
			});
			actions.add(new SelectionAction("streamCut", "Cut Stream", ("Cut text stream between '" + start.getString() + "' and '" + end.getString() + "'")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					start.setNextWord(null);
					return true;
				}
			});
		}
		
		//	multiple words, same stream
		else if (start.getTextStreamId().equals(end.getTextStreamId()) && (start.pageId == end.pageId) && (start.bounds.top < end.centerY) && (end.centerY < start.bounds.bottom) && (end.getTextStreamPos() <= (start.getTextStreamPos() + 20))) {
			boolean singleLine = true;
			for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
				if ((imw.centerY < start.bounds.top) || (start.bounds.bottom < imw.centerY)) {
					singleLine = false;
					break;
				}
				if (imw == end)
					break;
			}
			if (singleLine)
				actions.add(new SelectionAction("streamMergeWords", "Merge Words", "Merge selected words into one.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
							if (imw == end)
								break;
							imw.setNextRelation(ImWord.NEXT_RELATION_CONTINUE);
						}
						return true;
					}
				});
		}
		
		//	merge paragraphs
		if (paragraphsToMerge)
			actions.add(new SelectionAction("streamMergeParas", "Merge Paragraphs", "Merge selected paragraphs into one.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
						if (imw == end)
							break;
						if (imw.getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
							imw.setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
					}
					return true;
				}
			});
		
		//	one or more words from same stream
		if (start.getTextStreamId().equals(end.getTextStreamId())) {
			actions.add(new SelectionAction("streamCutOut", "Make Stream", "Make selected words a separate text stream.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					ImWord oldStartPrev = start.getPreviousWord();
					ImWord oldEndNext = end.getNextWord();
					if ((oldStartPrev != null) && (oldEndNext != null))
						oldStartPrev.setNextWord(oldEndNext);
					else {
						start.setPreviousWord(null);
						end.setNextWord(null);
					}
					return true;
				}
			});
			actions.add(new SelectionAction("streamSetType", "Set Text Stream Type", ("Set Type of Text Stream '" + start.getString() + "' Belongs to")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Set Text Stream Type");
					ButtonGroup bg = new ButtonGroup();
					String[] textStreamTypes = idmp.getTextStreamTypes();
					for (int t = 0; t < textStreamTypes.length; t++) {
						final String textStreamType = textStreamTypes[t];
						final JMenuItem smi = new JRadioButtonMenuItem(textStreamType, textStreamType.equals(start.getTextStreamType()));
						smi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								invoker.beginAtomicAction("Set Text Stream Type");
								if (smi.isSelected())
									start.setTextStreamType(textStreamType);
								invoker.endAtomicAction();
								invoker.validate();
								invoker.repaint();
							}
						});
						pm.add(smi);
						bg.add(smi);
					}
					return pm;
				}
			});
			actions.add(new SelectionAction("copyWordsTXT", "Copy Text", "Copy the selected words to the system clipboard.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					ImUtils.copy(new StringSelection(ImUtils.getString(start, end, false)));
					return false;
				}
			});
		}
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, final ImDocumentMarkupPanel idmp) {
		
		//	we're not offering text stream editing if text streams are not visualized
		if (!idmp.areTextStreamsPainted())
			return null;
		
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
		for (int w = 1; w < selectedWords.length; w++) {
			
			//	not continuous
			if (selectedWords[w].getPreviousWord() != selectedWords[w-1]) {
				singleContinuousStream = false;
				break;
			}
			
			//	above predecessor
			if (selectedWords[w].centerY < selectedWords[w-1].bounds.top) {
				singleContinuousStream = false;
				break;
			}
			
			//	same line as predecessor, and to the left
			if ((selectedWords[w].centerY < selectedWords[w-1].bounds.bottom) && (selectedWords[w].centerX < selectedWords[w-1].bounds.right)) {
				singleContinuousStream = false;
				break;
			}
		}
		
		LinkedList actions = new LinkedList();
		
		//	if we have a single continuous selection, we can handle it like a word selection
		if (singleContinuousStream)
			actions.addAll(Arrays.asList(this.getActions(selectedWords[0], selectedWords[selectedWords.length - 1], idmp)));
		
		//	generically make selected words a separate text stream
		actions.add(new SelectionAction("streamCutOut", "Make Stream", "Make selected words a separate text stream.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.makeStream(selectedWords, null, null);
				return true;
			}
		});
		
		//	order selected words into a text stream
		actions.add(new SelectionAction("streamOrder", "Order Stream", "Order selected words in a text stream left to right and top to bottom.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.orderStream(selectedWords, ImUtils.leftRightTopDownOrder);
				return true;
			}
		});
		
		//	TODO maybe offer word merging for selection as well
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getEditMenuItemNames()
	 */
	public String[] getEditMenuItemNames() {
		return new String[0];
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		String[] tmins = {WORD_MERGER_IMT_NAME};
		return tmins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (WORD_MERGER_IMT_NAME.equals(name))
			return new WordMerger();
		else return null;
	}
	
	private static final String WORD_MERGER_IMT_NAME = "WordMerger";
	
	private static class WordMerger implements ImageMarkupTool {
		public String getLabel() {
			return "Merge Shattered Words";
		}
		public String getTooltip() {
			return "Analyze word gaps, and merge words shattered into multiple pieces due to font decoding errors - USE WITH CARE";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we only process documents as a whole
			if (annot != null)
				return;
			
			//	little use processing scanned documents
			if (!idmp.documentBornDigital)
				return;
			
			//	get lines
			ArrayList docLines = new ArrayList();
			for (int p = 0; p < doc.getPageCount(); p++) {
				ImPage page = doc.getPage(p);
				ImRegion[] pageLines = page.getRegions(ImagingConstants.LINE_ANNOTATION_TYPE);
				Arrays.sort(pageLines, ImUtils.topDownOrder);
				docLines.addAll(Arrays.asList(pageLines));
			}
			ImRegion[] lines = ((ImRegion[]) docLines.toArray(new ImRegion[docLines.size()]));
			
			//	get tokenizer
			Tokenizer tokenizer = ((Tokenizer) doc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.NO_INNER_PUNCTUATION_TOKENIZER));
			
			//	process lines
			mergeWords(lines, tokenizer, pm);
		}
	}
	
	private static class LineData {
		ImWord[] words;
		int lineHeight;
		int fontSize;
		int maxWordGapJump;
		int minWordGap;
		int maxNonWordGap;
		int avgWordGap;
		int minSpaceWordGap;
		int maxNonSpaceWordGap;
		int mergeMinWordGap = -1;
		LineData(ImWord[] words, int lineHeight, int fontSize) {
			this.words = words;
			this.lineHeight = lineHeight;
			this.fontSize = fontSize;
		}
	}
	
	/* TODO consider replacing words altogether instead of connecting them
	 * - HAVE TO preserve char code string for future font edits
	 * - benefit: words behave normally in other analyses
	 * - benefit: document stores more compact (fewer words and text stream connections)
	 */
	
	private static final boolean DEBUG_WORD_MERGING = false;
	
	private static void mergeWords(ImRegion[] lines, Tokenizer tokenizer, ProgressMonitor pm) {
		LineData[] lineData = new LineData[lines.length];
		
		//	get line words, and measure font size
		pm.setStep("Collecting line words");
		pm.setBaseProgress(0);
		pm.setMaxProgress(5);
		int minFontSize = Integer.MAX_VALUE;
		int maxFontSize = 0;
		for (int l = 0; l < lines.length; l++) {
			pm.setProgress((l * 100) / lines.length);
			
			ImWord[] lWords = lines[l].getWords();
			int lFontSizeSum = 0;
			int lFontSizeCount = 0;
			for (int w = 1; w < lWords.length; w++) try {
				int wfs = Integer.parseInt((String) lWords[w].getAttribute(ImWord.FONT_SIZE_ATTRIBUTE));
				minFontSize = Math.min(minFontSize, wfs);
				maxFontSize = Math.max(maxFontSize, wfs);
				lFontSizeSum += wfs;
				lFontSizeCount++;
			} catch (RuntimeException re) {}
			int lFontSize = ((lFontSizeCount == 0) ? -1 : ((lFontSizeSum + (lFontSizeCount / 2)) / lFontSizeCount));
			if (1 < lWords.length)
				Arrays.sort(lWords, ImUtils.leftRightOrder);
			lineData[l] = new LineData(lWords, (lines[l].bounds.bottom - lines[l].bounds.top), lFontSize);
		}
		
		//	split lines up at gaps larger than twice line height TODO threshold low enough?
		pm.setStep("Splitting table row lines");
		pm.setBaseProgress(5);
		pm.setMaxProgress(10);
		ArrayList lineDataList = new ArrayList();
		for (int l = 0; l < lineData.length; l++) {
			pm.setProgress((l * 100) / lines.length);
			pm.setInfo("Line " + l + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ", max word gap jump " + lineData[l].maxWordGapJump + ")");
			
			if (DEBUG_WORD_MERGING) {
				System.out.println("Line " + l + " of " + lineData.length + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ", max word gap jump " + lineData[l].maxWordGapJump + ")");
				System.out.print("  words: " + lineData[l].words[0].getString());
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
					System.out.print(((gap < 100) ? "" : " ") + ((gap < 10) ? "" : " ") + " " + lineData[l].words[w].getString());
				}
				System.out.println();
				System.out.print("   gaps: " + lineData[l].words[0].getString().replaceAll(".", " "));
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
					System.out.print(gap + "" + lineData[l].words[w].getString().replaceAll(".", " "));
				}
				System.out.println();
			}
			for (int w = 1; w < lineData[l].words.length; w++) {
				int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
				if ((lineData[l].lineHeight * 2) < gap) {
					ImWord[] lWords = new ImWord[w];
					System.arraycopy(lineData[l].words, 0, lWords, 0, lWords.length);
					lineDataList.add(new LineData(lWords, lineData[l].lineHeight, lineData[l].fontSize));
					ImWord[] rWords = new ImWord[lineData[l].words.length - w];
					System.arraycopy(lineData[l].words, w, rWords, 0, rWords.length);
					lineData[l] = new LineData(rWords, lineData[l].lineHeight, lineData[l].fontSize);
					if (DEBUG_WORD_MERGING) System.out.println("  split at " + w + " for " + gap + " gap");
					w = 0;
				}
			}
			lineDataList.add(lineData[l]);
		}
		if (lineData.length < lineDataList.size())
			lineData = ((LineData[]) lineDataList.toArray(new LineData[lineDataList.size()]));
		
		//	analyze word gap structure for all lines together
		pm.setStep("Computing word gap distributions");
		pm.setBaseProgress(10);
		pm.setMaxProgress(15);
		CountingSet pageWordGaps = new CountingSet(new TreeMap());
		CountingSet[] fsPageWordGaps = ((minFontSize < maxFontSize) ? new CountingSet[maxFontSize - minFontSize + 1] : null);
		CountingSet pageWordGapQuots = new CountingSet(new TreeMap());
		for (int l = 0; l < lineData.length; l++) {
			pm.setProgress((l * 100) / lines.length);
			pm.setInfo("Line " + l + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ", max word gap jump " + lineData[l].maxWordGapJump + ")");
			
			CountingSet lWordGaps = new CountingSet(new TreeMap());
			CountingSet lWordGapQuots = new CountingSet(new TreeMap());
			for (int w = 1; w < lineData[l].words.length; w++) {
				int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
				lWordGaps.add(new Integer(gap));
				lWordGapQuots.add(new Integer(((gap * 10) + (lineData[l].fontSize / 2)) / lineData[l].fontSize));
			}
			
			pageWordGaps.addAll(lWordGaps);
			if ((0 < lineData[l].fontSize) && (fsPageWordGaps != null)) {
				if (fsPageWordGaps[lineData[l].fontSize - minFontSize] == null)
					fsPageWordGaps[lineData[l].fontSize - minFontSize] = new CountingSet(new TreeMap());
				fsPageWordGaps[lineData[l].fontSize - minFontSize].addAll(lWordGaps);
			}
			pageWordGapQuots.addAll(lWordGapQuots);
		}
		
		pm.setStep("Computing word gap accumulation points");
		pm.setBaseProgress(15);
		pm.setMaxProgress(20);
		int[] allPageWordGaps = getElementArray(pageWordGaps);
		if (DEBUG_WORD_MERGING) {
			System.out.println("Computing word gap accumulation points from " + Arrays.toString(allPageWordGaps));
			printCountingSet(pageWordGaps);
		}
		int pageInWordGap = -1;
		for (int g = 0; g < allPageWordGaps.length; g++) {
			int gc = pageWordGaps.getCount(new Integer(allPageWordGaps[g]));
			if ((g != 0) && (gc < pageWordGaps.getCount(new Integer(allPageWordGaps[g-1]))))
				continue;
			if (((g+1) < allPageWordGaps.length) && (gc < pageWordGaps.getCount(new Integer(allPageWordGaps[g+1]))))
				continue;
			if (pageWordGaps.getCount(new Integer(pageInWordGap)) < gc)
				pageInWordGap = allPageWordGaps[g];
		}
		int pageWordGap = -1;
		for (int g = 0; g < allPageWordGaps.length; g++) {
			if (allPageWordGaps[g] <= pageInWordGap)
				continue;
			int gc = pageWordGaps.getCount(new Integer(allPageWordGaps[g]));
			if ((g != 0) && (gc < pageWordGaps.getCount(new Integer(allPageWordGaps[g-1]))))
				continue;
			if (((g+1) < allPageWordGaps.length) && (gc < pageWordGaps.getCount(new Integer(allPageWordGaps[g+1]))))
				continue;
			if (pageWordGaps.getCount(new Integer(pageWordGap)) < gc)
				pageWordGap = allPageWordGaps[g];
		}
		pm.setInfo("Found best gap pair " + pageInWordGap + " (" + pageWordGaps.getCount(new Integer(pageInWordGap)) + ") / " + pageWordGap + " (" + pageWordGaps.getCount(new Integer(pageWordGap)) + ")");
		
		int[] allPageWordGapQuots = getElementArray(pageWordGapQuots);
		if (DEBUG_WORD_MERGING) {
			System.out.println("Computing word gap quotient accumulation points from " + Arrays.toString(allPageWordGapQuots));
			printCountingSet(pageWordGapQuots);
		}
		int pageInWordGapQuot = -1;
		for (int g = 0; g < allPageWordGapQuots.length; g++) {
			int gc = pageWordGapQuots.getCount(new Integer(allPageWordGapQuots[g]));
			if ((g != 0) && (gc < pageWordGapQuots.getCount(new Integer(allPageWordGapQuots[g-1]))))
				continue;
			if (((g+1) < allPageWordGapQuots.length) && (gc < pageWordGapQuots.getCount(new Integer(allPageWordGapQuots[g+1]))))
				continue;
			if (pageWordGapQuots.getCount(new Integer(pageInWordGapQuot)) < gc)
				pageInWordGapQuot = allPageWordGapQuots[g];
		}
		int pageWordGapQuot = -1;
		for (int g = 0; g < allPageWordGapQuots.length; g++) {
			if (allPageWordGapQuots[g] <= pageInWordGapQuot)
				continue;
			int gc = pageWordGapQuots.getCount(new Integer(allPageWordGapQuots[g]));
			if ((g != 0) && (gc < pageWordGapQuots.getCount(new Integer(allPageWordGapQuots[g-1]))))
				continue;
			if (((g+1) < allPageWordGapQuots.length) && (gc < pageWordGapQuots.getCount(new Integer(allPageWordGapQuots[g+1]))))
				continue;
			if (pageWordGapQuots.getCount(new Integer(pageWordGapQuot)) < gc)
				pageWordGapQuot = allPageWordGapQuots[g];
		}
		pm.setInfo("Found best gap quotient pair " + pageInWordGapQuot + " (" + pageWordGapQuots.getCount(new Integer(pageInWordGapQuot)) + ") / " + pageWordGapQuot + " (" + pageWordGapQuots.getCount(new Integer(pageWordGapQuot)) + ")");
		
		//	analyze word gap structure for each line
		pm.setStep("Analyzing word gaps for individual lines");
		pm.setBaseProgress(20);
		pm.setMaxProgress(25);
		CountingSet maxWordGapJumps = new CountingSet(new TreeMap());
		CountingSet[] fsMaxWordGapJumps = ((minFontSize < maxFontSize) ? new CountingSet[maxFontSize - minFontSize + 1] : null);
		CountingSet minWordGaps = new CountingSet(new TreeMap());
		CountingSet[] fsMinWordGaps = ((minFontSize < maxFontSize) ? new CountingSet[maxFontSize - minFontSize + 1] : null);
		CountingSet maxNonWordGaps = new CountingSet(new TreeMap());
		CountingSet[] fsMaxNonWordGaps = ((minFontSize < maxFontSize) ? new CountingSet[maxFontSize - minFontSize + 1] : null);
		CountingSet minSpaceWordGaps = new CountingSet(new TreeMap());
		CountingSet[] fsMinSpaceWordGaps = ((minFontSize < maxFontSize) ? new CountingSet[maxFontSize - minFontSize + 1] : null);
		CountingSet maxNonSpaceWordGaps = new CountingSet(new TreeMap());
		CountingSet[] fsMaxNonSpaceWordGaps = ((minFontSize < maxFontSize) ? new CountingSet[maxFontSize - minFontSize + 1] : null);
		for (int l = 0; l < lineData.length; l++) {
			pm.setProgress((l * 100) / lines.length);
			
			CountingSet lWordGaps = new CountingSet(new TreeMap());
			int lWordGapSum = 0;
			for (int w = 1; w < lineData[l].words.length; w++) {
				int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
				lWordGaps.add(new Integer(gap));
				lWordGapSum += gap;
			}
			lineData[l].avgWordGap = ((lineData[l].words.length < 2) ? 0 : ((lWordGapSum + ((lineData[l].words.length - 1) / 2)) / (lineData[l].words.length - 1)));
			if (DEBUG_WORD_MERGING) {
				System.out.println("Word gaps in line " + l + " (" + lWordGaps.size() + " in total, font size " + lineData[l].fontSize + ")");
				printCountingSet(lWordGaps);
			}
			if (DEBUG_WORD_MERGING) System.out.println(" - line at 0 gap threshold: " + getWordString(lineData[l].words, 1));
			
			//	search largest jump in gap size 
			int lastGap = -1;
			for (Iterator git = lWordGaps.iterator(); git.hasNext();) {
				Integer gap = ((Integer) git.next());
				if (lastGap == -1) {
					lastGap = gap.intValue();
					continue;
				}
				if ((gap.intValue() - lastGap) >= lineData[l].maxWordGapJump) {
					lineData[l].maxWordGapJump = (gap.intValue() - lastGap);
					lineData[l].minWordGap = gap.intValue();
					lineData[l].maxNonWordGap = lastGap;
				}
				
				//	break if lower edge closer to larger accumulation point (likely caused by extremely large outlier, e.g. table column gap)
				int lGapDist = Math.abs(pageInWordGap - gap.intValue());
				int rGapDist = Math.abs(pageWordGap - gap.intValue());
				int gapQuot = ((gap * 10) / lineData[l].fontSize);
				int lGapQuotDist = Math.abs(pageInWordGapQuot - gapQuot);
				int rGapQuotDist = Math.abs(pageWordGapQuot - gapQuot);
				if ((rGapDist < lGapDist) && (rGapQuotDist < lGapQuotDist))
					break;
				lastGap = Math.max(gap.intValue(), 1);
			}
			
			if (DEBUG_WORD_MERGING) System.out.println(" - line at max jump gap threshold (" + lineData[l].minWordGap + "): " + getWordString(lineData[l].words, lineData[l].minWordGap));
			maxWordGapJumps.add(new Integer(lineData[l].maxWordGapJump));
			if ((0 < lineData[l].fontSize) && (fsMaxWordGapJumps != null)) {
				if (fsMaxWordGapJumps[lineData[l].fontSize - minFontSize] == null)
					fsMaxWordGapJumps[lineData[l].fontSize - minFontSize] = new CountingSet(new TreeMap());
				fsMaxWordGapJumps[lineData[l].fontSize - minFontSize].add(new Integer(lineData[l].maxWordGapJump));
			}
			
			minWordGaps.add(new Integer(lineData[l].minWordGap), lineData[l].maxWordGapJump);
			if ((0 < lineData[l].fontSize) && (fsMinWordGaps != null)) {
				if (fsMinWordGaps[lineData[l].fontSize - minFontSize] == null)
					fsMinWordGaps[lineData[l].fontSize - minFontSize] = new CountingSet(new TreeMap());
				fsMinWordGaps[lineData[l].fontSize - minFontSize].add(new Integer(lineData[l].minWordGap), lineData[l].maxWordGapJump);
			}
			maxNonWordGaps.add(new Integer(lineData[l].maxNonWordGap), lineData[l].maxWordGapJump);
			if ((0 < lineData[l].fontSize) && (fsMaxNonWordGaps != null)) {
				if (fsMaxNonWordGaps[lineData[l].fontSize - minFontSize] == null)
					fsMaxNonWordGaps[lineData[l].fontSize - minFontSize] = new CountingSet(new TreeMap());
				fsMaxNonWordGaps[lineData[l].fontSize - minFontSize].add(new Integer(lineData[l].maxNonWordGap), lineData[l].maxWordGapJump);
			}
			
			
			lineData[l].minSpaceWordGap = getMinSpaceGap(lineData[l].words, tokenizer);
			if (lineData[l].minSpaceWordGap < Integer.MAX_VALUE) {
				minSpaceWordGaps.add(new Integer(lineData[l].minSpaceWordGap));
				if ((0 < lineData[l].fontSize) && (fsMinSpaceWordGaps != null)) {
					if (fsMinSpaceWordGaps[lineData[l].fontSize - minFontSize] == null)
						fsMinSpaceWordGaps[lineData[l].fontSize - minFontSize] = new CountingSet(new TreeMap());
					fsMinSpaceWordGaps[lineData[l].fontSize - minFontSize].add(new Integer(lineData[l].minSpaceWordGap));
				}
			}
			lineData[l].maxNonSpaceWordGap = getMaxNonSpaceGap(lineData[l].words);
			if (lineData[l].maxNonSpaceWordGap > 0) {
				maxNonSpaceWordGaps.add(new Integer(lineData[l].maxNonSpaceWordGap));
				if ((0 < lineData[l].fontSize) && (fsMaxNonSpaceWordGaps != null)) {
					if (fsMaxNonSpaceWordGaps[lineData[l].fontSize - minFontSize] == null)
						fsMaxNonSpaceWordGaps[lineData[l].fontSize - minFontSize] = new CountingSet(new TreeMap());
					fsMaxNonSpaceWordGaps[lineData[l].fontSize - minFontSize].add(new Integer(lineData[l].maxNonSpaceWordGap));
				}
			}
		}
		
		//	perform word mergers starting from most certain lines
		pm.setStep("Merging words in lines with clear gap structure");
		pm.setBaseProgress(25);
		pm.setMaxProgress(40);
		TreeSet dictionary = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		for (int l = 0; l < lineData.length; l++) {
			pm.setProgress((l * 100) / lines.length);
			pm.setInfo("Line " + l + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ", max word gap jump " + lineData[l].maxWordGapJump + ")");
			
			if (DEBUG_WORD_MERGING) System.out.println("Line " + l + " of " + lineData.length + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ", max word gap jump " + lineData[l].maxWordGapJump + ")");
			if (DEBUG_WORD_MERGING) {
				int leftGapSum = 0;
				int rightGapSum = 0;
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
					if (w < (lineData[l].words.length / 2))
						leftGapSum += gap;
					else rightGapSum += gap;
				}
				System.out.print("  words: " + lineData[l].words[0].getString());
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
					System.out.print(((gap < 100) ? "" : " ") + ((gap < 10) ? "" : " ") + " " + lineData[l].words[w].getString());
				}
				System.out.println();
				System.out.print("   gaps: " + lineData[l].words[0].getString().replaceAll(".", " "));
				for (int w = 1; w < lineData[l].words.length; w++) {
					int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
					System.out.print(gap + "" + lineData[l].words[w].getString().replaceAll(".", " "));
				}
				System.out.println();
				System.out.println("  left gap sum is " + leftGapSum + ", right gap sum is " + rightGapSum);
				System.out.println("  at 0 gap threshold: " + getWordString(lineData[l].words, 1));
				
				//	show word gap distribution based thresholds
				System.out.println("  at local max non-gap + 1 threshold (" + (lineData[l].maxNonWordGap + 1) + "): " + getWordString(lineData[l].words, (lineData[l].maxNonWordGap + 1)));
				System.out.println("  at local min gap threshold (" + lineData[l].minWordGap + "): " + getWordString(lineData[l].words, lineData[l].minWordGap));
				System.out.println("  at local avg gap threshold (" + lineData[l].avgWordGap + "): " + getWordString(lineData[l].words, lineData[l].avgWordGap));
			}
			
			//	this one's clear from gap jumps, can use word gap
			boolean gapJumpSecure = ((lineData[l].maxWordGapJump * 5) > lineData[l].lineHeight);
			if (gapJumpSecure) {
				lineData[l].mergeMinWordGap = lineData[l].minWordGap;
				lineData[l].words = performWordMergers(lineData[l].words, lineData[l].mergeMinWordGap, tokenizer, dictionary);
				pm.setInfo("  securely merged at local min word gap (" + lineData[l].mergeMinWordGap + "): " + ImUtils.getString(lineData[l].words[0], lineData[l].words[lineData[l].words.length-1], true));
				continue;
			}
			
			//	show space and tokenization based thresholds
			if (DEBUG_WORD_MERGING) {
				if (lineData[l].maxNonSpaceWordGap > 0)
					System.out.println("  at maximum local non-space gap + 1 threshold (" + (lineData[l].maxNonSpaceWordGap + 1) + "): " + getWordString(lineData[l].words, (lineData[l].maxNonSpaceWordGap + 1)));
				if (lineData[l].minSpaceWordGap < Integer.MAX_VALUE)
					System.out.println("  at minimum local space gap threshold (" + lineData[l].minSpaceWordGap + "): " + getWordString(lineData[l].words, lineData[l].minSpaceWordGap));
				if ((lineData[l].maxNonSpaceWordGap > 0) && (lineData[l].minSpaceWordGap < Integer.MAX_VALUE)) {
					int spaceWordGap = ((lineData[l].minSpaceWordGap + lineData[l].maxNonSpaceWordGap + 1) / 2);
					System.out.println("  at avg local space gap threshold (" + spaceWordGap + "): " + getWordString(lineData[l].words, spaceWordGap));
				}
			}
			
			//	this one's clear from spaces and tokenization, can use space based word gap
			boolean nonSpaceSecure = ((lineData[l].maxNonSpaceWordGap * 5) > lineData[l].lineHeight);
			if (nonSpaceSecure) {
				lineData[l].mergeMinWordGap = (lineData[l].maxNonSpaceWordGap + 1);
				lineData[l].words = performWordMergers(lineData[l].words, lineData[l].mergeMinWordGap, tokenizer, dictionary);
				pm.setInfo("  securely merged at local non-space word gap + 1 (" + lineData[l].mergeMinWordGap + "): " + ImUtils.getString(lineData[l].words[0], lineData[l].words[lineData[l].words.length-1], true));
				continue;
			}
		}
		
		//	try and increase min word gap from one fifth of line height upward, or from previous threshold, and use gap with most dictionary hits
		pm.setStep("Merging words in remaining lines");
		pm.setBaseProgress(40);
		pm.setMaxProgress(100);
		for (int r = 0, nml = 0;; r++, nml = 0) {
			pm.setStep("Merging words in remaining lines, round " + (r+1));
			pm.setBaseProgress(40 + ((60 * r) / (r+1)));
			for (int l = 0; l < lineData.length; l++) {
				pm.setProgress((l * 100) / lines.length);
				pm.setInfo("Line " + l + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ", max word gap jump " + lineData[l].maxWordGapJump + ")");
				
				if (DEBUG_WORD_MERGING) System.out.println("Round " + r + ": Line " + l + " of " + lineData.length + " at " + lineData[l].words[0].getLocalID() + " (font size " + lineData[l].fontSize + ", height " + lineData[l].lineHeight + ", max word gap jump " + lineData[l].maxWordGapJump + ")");
				if (DEBUG_WORD_MERGING) {
					System.out.print("  words: " + lineData[l].words[0].getString());
					for (int w = 1; w < lineData[l].words.length; w++) {
						int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
						System.out.print(((gap < 100) ? "" : " ") + ((gap < 10) ? "" : " ") + " " + lineData[l].words[w].getString());
					}
					System.out.println();
					System.out.print("   gaps: " + lineData[l].words[0].getString().replaceAll(".", " "));
					for (int w = 1; w < lineData[l].words.length; w++) {
						int gap = getGap(lineData[l].words[w-1], lineData[l].words[w]);
						System.out.print(gap + "" + lineData[l].words[w].getString().replaceAll(".", " "));
					}
					System.out.println();
					System.out.println("  at 0 gap threshold: " + getWordString(lineData[l].words, 1));
				}
				
				//	keep track of dictionary hits for each char
				int lineCharCount = 0;
				for (int w = 0; w < lineData[l].words.length; w++)
					lineCharCount += lineData[l].words[w].getString().length();
				char[] lineChars = new char[lineCharCount];
				char[] lineCharDictHitStatus = new char[lineCharCount];
				ImWord[] lineCharWords = new ImWord[lineCharCount];
				for (int lco = 0, w = 0; w < lineData[l].words.length; w++)
					for (int c = 0; c < lineData[l].words[w].getString().length(); c++) {
						lineChars[lco] = lineData[l].words[w].getString().charAt(c);
						lineCharDictHitStatus[lco] = 'O';
						lineCharWords[lco] = lineData[l].words[w];
						lco++;
					}
				
				//	count dictionary hits, starting at one fifth of line height
				int maxDictHitCount = -1;
				int maxDictHitCharCount = -1;
//				int maxDictHitTokenCount = 0;
//				int maxDictHitLength = Integer.MIN_VALUE;
//				int minKillSpaceCount = 0;
				int minNumberTokenCount = 0;
				int minNonNumberTokenCount = 0;
				for (int twg = ((lineData[l].mergeMinWordGap == -1) ? ((lineData[l].lineHeight + 2) / 5) : lineData[l].mergeMinWordGap); twg < lineData[l].lineHeight; twg++) {
					String twgString = getWordString(lineData[l].words, twg);
					if (DEBUG_WORD_MERGING) System.out.println("  at test word gap threshold (" + twg + "): " + twgString);
					TokenSequence twgTokens = tokenizer.tokenize(twgString);
					int twgDictHitCount = 0;
					int twgDictHitCharCount = 0;
					int twgKillSpaceCount = 0;
					int twgNonNumberTokenCount = 0;
					int twgNumberTokenCount = 0;
					for (int lco = 0, t = 0; t < twgTokens.size(); lco += twgTokens.valueAt(t++).length()) {
						String twgToken = twgTokens.valueAt(t);
						if (twgToken.matches("[0-9]+"))
							twgNumberTokenCount++;
						else twgNonNumberTokenCount++;
						if (twgToken.length() < 2)
							continue;
						if (dictionary.contains(twgToken) && (twgToken.matches("[12][0-9]{3}") || !Gamta.isNumber(twgToken))) {
							twgDictHitCount++;
							twgDictHitCharCount += twgToken.length();
							lineCharDictHitStatus[lco] = 'S';
							for (int c = 1; c < twgToken.length(); c++)
								lineCharDictHitStatus[lco + c] = 'C';
						}
						if ((t != 0) && Gamta.insertSpace(twgTokens.valueAt(t-1), twgToken) && (twgTokens.getWhitespaceAfter(t-1).length() == 0))
							twgKillSpaceCount++;
					}
					if (DEBUG_WORD_MERGING) {
						System.out.println("  ==> " + twgTokens.size() + " tokens, " + twgDictHitCount + " in dictionary (" + twgDictHitCharCount + " chars), " + twgKillSpaceCount + " spaces killed, " + twgNumberTokenCount + " number tokens, " + twgNonNumberTokenCount + " other tokens");
						System.out.println("  ==> dict hit status: " + Arrays.toString(lineChars));
						System.out.println("                       " + Arrays.toString(lineCharDictHitStatus));
					}
					
					//	do we have a new optimum?
					if ((maxDictHitCharCount < twgDictHitCharCount) || ((maxDictHitCharCount == twgDictHitCharCount) && (twgDictHitCount < maxDictHitCount))) {
						pm.setInfo("  ==> new best word gap " + twg + " for dictionary hits");
						maxDictHitCount = twgDictHitCount;
						maxDictHitCharCount = twgDictHitCharCount;
//						maxDictHitTokenCount = twgTokens.size();
//						maxDictHitLength = twgString.length();
//						minKillSpaceCount = twgKillSpaceCount;
						minNumberTokenCount = twgNumberTokenCount;
						minNonNumberTokenCount = twgNonNumberTokenCount;
						if (lineData[l].mergeMinWordGap < twg)
							nml++;
						lineData[l].mergeMinWordGap = twg;
						if (twgString.indexOf(' ') == -1)
							break;
						else continue;
					}
					
					if ((twgDictHitCount + twgDictHitCharCount) < (maxDictHitCount + maxDictHitCharCount)) {
						if (twgString.indexOf(' ') == -1)
							break;
						else continue;
					}
//					
//					if ((maxDictHitTokenCount <= twgTokens.size()) && (twgString.length() < maxDictHitLength)) {
//						pm.setInfo("  ==> new best word gap " + twg + " for overall length");
//						maxDictHitCount = twgDictHitCount;
//						maxDictHitCharCount = twgDictHitCharCount;
//						maxDictHitTokenCount = twgTokens.size();
//						maxDictHitLength = twgString.length();
//						minKillSpaceCount = twgKillSpaceCount;
//						minNumberTokenCount = twgNumberTokenCount;
//						minNonNumberTokenCount = twgNonNumberTokenCount;
//						if (lineData[l].mergeMinWordGap < twg)
//							nml++;
//						lineData[l].mergeMinWordGap = twg;
//						continue;
//					}
					
					if ((minNonNumberTokenCount <= twgNonNumberTokenCount) && (twgNumberTokenCount < minNumberTokenCount)) {
						pm.setInfo("  ==> new best word gap " + twg + " for fewer number tokens");
						maxDictHitCount = twgDictHitCount;
						maxDictHitCharCount = twgDictHitCharCount;
//						maxDictHitTokenCount = twgTokens.size();
//						maxDictHitLength = twgString.length();
//						minKillSpaceCount = twgKillSpaceCount;
						minNumberTokenCount = twgNumberTokenCount;
						minNonNumberTokenCount = twgNonNumberTokenCount;
						if (lineData[l].mergeMinWordGap < twg)
							nml++;
						lineData[l].mergeMinWordGap = twg;
						continue;
					}
					
					//	no use increasing minimum gap any further ...
					if (twgString.indexOf(' ') == -1)
						break;
				}
//				
//				//	no dictionary hits at all, be safe
//				if (maxDictHitCount == 0)
//					lineData[l].mergeMinWordGap = -1;
				
				//	nothing found on this one, at least merge at minimum one fifth 
				if (lineData[l].mergeMinWordGap == -1)
					lineData[l].mergeMinWordGap = ((lineData[l].lineHeight + 2) / 5);
				
				//	perform mergers (we need to amend dictionary after round only, so to not reinforce partial mergers)
				lineData[l].words = performWordMergers(lineData[l].words, lineData[l].mergeMinWordGap, tokenizer, null);
				pm.setInfo("  merged at dictionary hit optimized min word gap (" + lineData[l].mergeMinWordGap + "): " + ImUtils.getString(lineData[l].words[0], lineData[l].words[lineData[l].words.length-1], true));
				if (DEBUG_WORD_MERGING) System.out.println("  ==> " + getWordString(lineData[l].words, lineData[l].mergeMinWordGap));
//				
//				//	recover dictionary hits that disappeared due to mergers
//				boolean dictHitResplit = false;
//				for (int c = 1; c < lineCharDictHitStatus.length; c++) {
//					if (lineCharWords[c-1] == lineCharWords[c])
//						continue;
//					if (lineCharWords[c-1].getNextRelation() != ImWord.NEXT_RELATION_CONTINUE)
//						continue;
//					if ((lineCharDictHitStatus[c-1] != 'O') && (lineCharDictHitStatus[c] == 'S')) {
//						lineCharWords[c-1].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
//						dictHitResplit = true;
//					}
////					TOO AGGRESSIVE WITH FREQUENT WORD PREFIXES, INFIXES, AND SUFFIXES
////					else if ((lineCharDictHitStatus[c-1] != 'O') && (lineCharDictHitStatus[c] == 'O')) {
////						lineCharWords[c-1].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
////						dictHitResplit = true;
////					}
//				}
//				if (dictHitResplit)
//					System.out.println("  re-split merged-away dictionary hits: " + ImUtils.getString(lineData[l].words[0], lineData[l].words[lineData[l].words.length-1], true));
			}
			
			//	nothing new this round, we're done
			if (nml == 0)
				break;
			
			/* extend dictionary (we need to amend dictionary after round only,
			 * so to not reinforce partial mergers)
			 * - only add words to dictionary that don't merge away if min word
			 *   gap increases by 2 or 3 to prevent adding results of incomplete
			 *   mergers to dictionary (which would reinforce these mergers)
			 * - collect line tokens at each threshold, retaining only those
			 *   that remain at min word gap increase, and adding only the
			 *   latter to the main dictionary */
			for (int l = 0; l < lineData.length; l++) {
				TreeSet lDictionary = getWordTokens(lineData[l].words, lineData[l].mergeMinWordGap, tokenizer);
				for (int g = (lineData[l].mergeMinWordGap + 1); g < (lineData[l].mergeMinWordGap + 3); g++)
					lDictionary.retainAll(getWordTokens(lineData[l].words, g, tokenizer));
				dictionary.addAll(lDictionary);
				if (DEBUG_WORD_MERGING && (lDictionary.size() != 0))
					System.out.println("Line " + l + " of " + lineData.length + " at (" + lineData[l].mergeMinWordGap + "): dictionary extended by " + lDictionary);
			}
			
			//	TODO consider case sensitive dictionary
		}
		
		if (DEBUG_WORD_MERGING) {
			for (int l = 0; l < lineData.length; l++)
				System.out.println("Line " + l + " of " + lineData.length + " at " + lineData[l].words[0].getLocalID() + " (" + lineData[l].mergeMinWordGap + "): " + ImUtils.getString(lineData[l].words[0], lineData[l].words[lineData[l].words.length-1], true));
		}
	}
	
	private static ImWord[] performWordMergers(ImWord[] words, int minWordGap, Tokenizer tokenizer, Set dictionary) {
		StringBuffer wStr = new StringBuffer(words[0].getString());
		ArrayList wStrCharWords = new ArrayList();
		for (int c = 0; c < words[0].getString().length(); c++)
			wStrCharWords.add(words[0]);
		for (int w = 1; w < words.length; w++) {
			
			//	append space if required
			if (space(words, w, minWordGap)) {
				wStr.append(' ');
				wStrCharWords.add(null);
			}
			
			//	append word proper
			wStr.append(words[w].getString());
			for (int c = 0; c < words[w].getString().length(); c++)
				wStrCharWords.add(words[w]);
		}
		
		//	merge away combinable accents
		for (int c = 0; c < wStr.length(); c++) {
			char ch = wStr.charAt(c);
			if (COMBINABLE_ACCENTS.indexOf(ch) == -1)
				continue;
//			System.out.println("Char is " + COMBINABLE_ACCENT_MAPPINGS.get(new Character(ch)).toString());
			if (c != 0) {
				char lCh = wStr.charAt(c-1);
				char cCh = StringUtils.getCharForName(lCh + COMBINABLE_ACCENT_MAPPINGS.get(new Character(ch)).toString());
				if (cCh != 0) {
					wStr.deleteCharAt(c);
					wStrCharWords.remove(c);
					wStr.setCharAt(--c, cCh);
					continue;
				}
			}
			if ((c+1) < wStr.length()) {
				char rCh = wStr.charAt(c+1);
				char cCh = StringUtils.getCharForName(rCh + COMBINABLE_ACCENT_MAPPINGS.get(new Character(ch)).toString());
				if (cCh != 0) {
					wStr.deleteCharAt(c);
					wStrCharWords.remove(c);
					wStr.setCharAt(c, cCh);
					continue;
				}
			}
		}
		
		//	tokenize string
		TokenSequence wStrTokens = tokenizer.tokenize(wStr);
		
		//	update word strings
		ArrayList mWords = new ArrayList();
		for (int t = 0; t < wStrTokens.size(); t++) {
			Token wStrToken = wStrTokens.tokenAt(t);
			for (int c = wStrToken.getStartOffset(); c < wStrToken.getEndOffset(); c++) {
				ImWord imw1 = ((c == 0) ? null : ((ImWord) wStrCharWords.get(c-1)));
				ImWord imw2 = ((ImWord) wStrCharWords.get(c));
				if (imw1 == imw2)
					imw2.setString(imw2.getString() + wStr.charAt(c));
				else imw2.setString("" + wStr.charAt(c));
			}
		}
		
		//	cut out empty words
		for (int w = 0; w < words.length; w++) {
			if (wStrCharWords.contains(words[w]))
				mWords.add(words[w]);
			else {
				ImWord[] dWord = {words[w]};
				ImUtils.makeStream(dWord, ImWord.TEXT_STREAM_TYPE_DELETED, null);
			}
		}
		
		//	set word relations
		for (int t = 0; t < wStrTokens.size(); t++) {
			Token wStrToken = wStrTokens.tokenAt(t);
			for (int c = wStrToken.getStartOffset(); c < (wStrToken.getEndOffset() - 1); c++) {
				ImWord imw1 = ((ImWord) wStrCharWords.get(c));
				ImWord imw2 = ((ImWord) wStrCharWords.get(c+1));
				if (imw1 != imw2)
					imw1.setNextRelation(ImWord.NEXT_RELATION_CONTINUE);
			}
			if (dictionary != null)
				dictionary.add(wStrToken.getValue());
		}
		
		//	indicate whether or not words have changed
		return ((mWords.size() < words.length) ? ((ImWord[]) mWords.toArray(new ImWord[mWords.size()])) : words);
	}
	
	private static TreeSet getWordTokens(ImWord[] words, int minWordGap, Tokenizer tokenizer) {
		TreeSet dictionary = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		String wStr = getWordString(words, minWordGap);
		TokenSequence wStrTokens = tokenizer.tokenize(wStr);
		for (int t = 0; t < wStrTokens.size(); t++)
			dictionary.add(wStrTokens.valueAt(t));
		return dictionary;
	}
	
	private static int getMinSpaceGap(ImWord[] words, Tokenizer tokenizer) {
		int minSpaceGap = Integer.MAX_VALUE;
		for (int w = 1; w < words.length; w++) {
			if (Gamta.insertSpace(words[w-1].getString(), words[w].getString()) && (tokenizer.tokenize(words[w-1].getString() + words[w].getString()).size() > 1))
				minSpaceGap = Math.min(minSpaceGap, getGap(words[w-1], words[w]));
		}
		return Math.max(minSpaceGap, 0);
	}
	
	private static int getMaxNonSpaceGap(ImWord[] words) {
		int maxNonSpaceGap = -1;
		for (int w = 1; w < words.length; w++) {
			if (!Gamta.insertSpace(words[w-1].getString(), words[w].getString()))
				maxNonSpaceGap = Math.max(maxNonSpaceGap, getGap(words[w-1], words[w]));
		}
		return maxNonSpaceGap;
	}
	
	private static void printCountingSet(CountingSet cs) {
		for (Iterator iit = cs.iterator(); iit.hasNext();) {
			Integer i = ((Integer) iit.next());
			System.out.println(" - " + i.intValue() + " (" + cs.getCount(i) + " times)");
		}
	}
	
	private static String getWordString(ImWord[] words, int minWordGap) {
		if (words.length == 0)
			return "";
		StringBuffer wStr = new StringBuffer(words[0].getString());
		for (int w = 1; w < words.length; w++) {
			
			//	append space if required
			if (space(words, w, minWordGap))
				wStr.append(' ');
			
			//	append word proper
			wStr.append(words[w].getString());
		}
		
		//	merge away combinable accents
		for (int c = 0; c < wStr.length(); c++) {
			char ch = wStr.charAt(c);
			if (COMBINABLE_ACCENTS.indexOf(ch) == -1)
				continue;
//			System.out.println("Char is " + COMBINABLE_ACCENT_MAPPINGS.get(new Character(ch)).toString());
			if (c != 0) {
				char lCh = wStr.charAt(c-1);
				char cCh = StringUtils.getCharForName(lCh + COMBINABLE_ACCENT_MAPPINGS.get(new Character(ch)).toString());
				if (cCh != 0) {
					wStr.deleteCharAt(c);
					wStr.setCharAt(--c, cCh);
					continue;
				}
			}
			if ((c+1) < wStr.length()) {
				char rCh = wStr.charAt(c+1);
				char cCh = StringUtils.getCharForName(rCh + COMBINABLE_ACCENT_MAPPINGS.get(new Character(ch)).toString());
				if (cCh != 0) {
					wStr.deleteCharAt(c);
					wStr.setCharAt(c, cCh);
					continue;
				}
			}
		}
		
		//	finally ...
		return wStr.toString();
	}
	
	private static int getGap(ImWord lWord, ImWord rWord) {
		int wordGap = Math.max(0, (rWord.bounds.left - lWord.bounds.right));
		
		//	cap gap at line height to deal with extremely large (column) gaps in tables
		int lineHeight = ((lWord.bounds.bottom - lWord.bounds.top + rWord.bounds.bottom - rWord.bounds.top) / 2);
		
		//	investigate left tail char and right lead char
		String lStr = lWord.getString();
		char lTailCh = lStr.charAt(lStr.length()-1);
		String rStr = rWord.getString();
		char rLeadCh = rStr.charAt(0);
		
		//	add some anti-kerning for left tailing 'f' if right lead char doesn't have left ascender (maybe 10% of line height)
		boolean antiKern = false;
		if (("fFPTVWY7'".indexOf(lTailCh) != -1) && ("acdegijmnopqrstuvwxyzAJ.,;:+/-".indexOf(rLeadCh) != -1))
			antiKern = true;
		else if (("rvwyDOSU09".indexOf(lTailCh) != -1) && ("A.,".indexOf(rLeadCh) != -1))
			antiKern = true;
		else if ((".,".indexOf(lTailCh) != -1) && ("vwyCOQTVW01".indexOf(rLeadCh) != -1))
			antiKern = true;
		
		//	subtract some kerning for left tailing or right leading 'i', 'l', '1', etc. (maybe 5% of line height)
		boolean kern = false;
		if (("([{".indexOf(lTailCh) != -1) || (")]}".indexOf(rLeadCh) != -1))
			kern = true;
		
		if (antiKern)
			wordGap += (lineHeight / 10);
		else if (kern)
			wordGap -= (lineHeight / 20);
		wordGap = Math.max(wordGap, 0);
		
		//	finally ...
		return wordGap;
	}
	
	private static boolean space(ImWord[] words, int w, int minWordGap) {
		int gap = getGap(words[w-1], words[w]);
		
		//	font mismatch ==> don't merge
		if (!words[w-1].getAttribute(ImWord.FONT_NAME_ATTRIBUTE, "").equals(words[w].getAttribute(ImWord.FONT_NAME_ATTRIBUTE, "")))
			return true;
		
		//	compute adjusted gap for case to left and right
		int aGap = gap;
		String lStr = words[w-1].getString();
		char lTailCh = lStr.charAt(lStr.length()-1);
		String rStr = words[w].getString();
		char rLeadCh = rStr.charAt(0);
		if (Character.isLetter(lTailCh) && Character.isLetter(rLeadCh)) {
			
			//	left side ends in lower case and right side starts in upper case ==> only merge if gap at most half the threshold
			if ((lTailCh == Character.toLowerCase(lTailCh)) && (rLeadCh == Character.toUpperCase(rLeadCh)))
				aGap = (aGap * 2);
			
			//	left is single upper case letter, right side starts in lower case ==> even merge if gap is a little above threshold
			else if ((lStr.length() == 1) && (lTailCh == Character.toUpperCase(lTailCh)) && (rLeadCh == Character.toLowerCase(rLeadCh)))
				aGap = ((aGap * 2) / 3);
		}
		
		//	gap above threshold ==> don't merge
		if (minWordGap <= aGap)
			return true;
		
		//	gap at least four times as large as smaller of adjacent gaps (looks like we're in a dense area), and closer to threshold than to average of adjacent gaps ==> don't merge
		int lGap = Math.max(1, ((w < 2) ? gap : getGap(words[w-2], words[w-1])));
		int rGap = Math.max(1, (((w+1) == words.length) ? gap : getGap(words[w], words[w+1])));
		int avgLrGap = ((lGap + rGap + 1) / 2);
		if (false) {}
		else if ((gap - avgLrGap) < (minWordGap - gap)) {}
		else if ((Math.min(lGap, rGap) * 4) <= gap)
			return true;
		else if ((avgLrGap * 3) <= gap)
			return true;
		
		//	found no reason for a space
		return false;
	}
//	
//	private static int getMaxFreqElement(CountingSet cs) {
//		int maxFreqElement = -1;
//		int maxElementFreq = 0;
//		for (Iterator iit = cs.iterator(); iit.hasNext();) {
//			Integer i = ((Integer) iit.next());
//			if (cs.getCount(i) > maxElementFreq) {
//				maxFreqElement = i.intValue();
//				maxElementFreq = cs.getCount(i);
//			}
//		}
//		return maxFreqElement;
//	}
	
	private static final String COMBINABLE_ACCENTS;
	private static final HashMap COMBINABLE_ACCENT_MAPPINGS = new HashMap();
	
	static {
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0300'), "grave");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0301'), "acute");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0302'), "circumflex");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0303'), "tilde");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0304'), "macron");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0306'), "breve");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0307'), "dot");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0308'), "dieresis");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0309'), "hook");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u030A'), "ring");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u030B'), "dblacute");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u030F'), "dblgrave");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u030C'), "caron");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0323'), "dotbelow");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0327'), "cedilla");
		COMBINABLE_ACCENT_MAPPINGS.put(new Character('\u0328'), "ogonek");
		
		StringBuffer combinableAccentCollector = new StringBuffer();
		ArrayList combinableAccents = new ArrayList(COMBINABLE_ACCENT_MAPPINGS.keySet());
		for (int c = 0; c < combinableAccents.size(); c++) {
			Character combiningChar = ((Character) combinableAccents.get(c));
			combinableAccentCollector.append(combiningChar.charValue());
			String charName = ((String) COMBINABLE_ACCENT_MAPPINGS.get(combiningChar));
			char baseChar = StringUtils.getCharForName(charName);
			if ((baseChar > 0) && (baseChar != combiningChar.charValue())) {
				combinableAccentCollector.append(baseChar);
				COMBINABLE_ACCENT_MAPPINGS.put(new Character(baseChar), charName);
			}
		}
		COMBINABLE_ACCENTS = combinableAccentCollector.toString();
	}
	
	private static int[] getElementArray(CountingSet cs) {
		int[] csElements = new int[cs.elementCount()];
		int csIndex = 0;
		for (Iterator git = cs.iterator(); git.hasNext();)
			csElements[csIndex++] = ((Integer) git.next()).intValue();
		return csElements;
	}
	
	/* Tooling for re-assessing words
	 * - helps merging single-letter "words" into actual words ...
	 * - ... vastly mitigating effort for correcting adverse effects of nasty obfuscation fonts
	 * 
	 * - mechanism:
	 *   - analyze distribution of word gap widths for each line (rationale: justification expands actual inter-word whitespace equally)
	 *   - set word relation to "same word" for below-average word gaps
	 *   - use absolute upper gap width bound for merging as well, for safety ...
	 *   - ... and only merge if words don't tokenize apart
	 *   ==> perform this test for potential merger blocks as a whole
	 * 
	 * - provide for:
	 *   - individual text blocks / regions via context menu
	 *   - whole document via "Tools" menu
	 * 
	 * TEST WITH Lopez_et_al.pdf (words torn into individual letters due to combination of char decoding problems and obfuscation-aimed word rendering)
	 */
	public static void main(String[] args) throws Exception {
		InputStream docIn = new BufferedInputStream(new FileInputStream(new File("E:/Eigene Daten/Plazi Workshop - Heraklion 2015/Lopez et al.pdf.charsOk.imf")));
		ImDocument doc = ImfIO.loadDocument(docIn);
		docIn.close();
		Tokenizer tokenizer = ((Tokenizer) doc.getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.NO_INNER_PUNCTUATION_TOKENIZER));
		
		//	get lines
		ArrayList docLines = new ArrayList();
		for (int p = 0; p < doc.getPageCount(); p++) {
			ImPage page = doc.getPage(p);
			ImRegion[] pageLines = page.getRegions(ImagingConstants.LINE_ANNOTATION_TYPE);
			Arrays.sort(pageLines, ImUtils.topDownOrder);
//			if (p == 0)
			docLines.addAll(Arrays.asList(pageLines));
		}
		ImRegion[] lines = ((ImRegion[]) docLines.toArray(new ImRegion[docLines.size()]));
		mergeWords(lines, tokenizer, ProgressMonitor.dummy);
	}
}