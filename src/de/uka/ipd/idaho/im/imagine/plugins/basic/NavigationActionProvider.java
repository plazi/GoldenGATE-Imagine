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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractReactionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener;
import de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.im.util.SymbolTable;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * @author sautter
 *
 */
public class NavigationActionProvider extends AbstractReactionProvider implements SelectionActionProvider, ReactionProvider, GoldenGateImagineDocumentListener {
	
	private static final boolean DEBUG_FIND = false;
	
	private static final int MODE_CASE_SENSITIVE = 1;
	private static final int MODE_ACCENT_SENSITIVE = 2;
	private static final int MODE_SPACE_SENSITIVE = 4;
	private static final int MODE_WORD_BOUNDARY_SENSITIVE = 8;
	
	private int matchMode = 0;
	
	/** zero-argument constructor for class loading */
	public NavigationActionProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "IM Document Navigator";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentOpened(de.uka.ipd.idaho.im.ImDocument)
	 */
	public void documentOpened(ImDocument doc) { /* we're not interested in documents being opened, as we create our index on the fly */ }

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentSaving(de.uka.ipd.idaho.im.ImDocument)
	 */
	public void documentSaving(ImDocument doc) { /* we're not interested in documents being saved, as we're not persisting our index */ }

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentSaved(de.uka.ipd.idaho.im.ImDocument)
	 */
	public void documentSaved(ImDocument doc) { /* we're not interested in documents having been saved */ }

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentClosed(java.lang.String)
	 */
	public void documentClosed(String docId) {
		ImDocumentIndex docIndex = ((ImDocumentIndex) this.docIndexesByDocID.get(docId));
		if (docIndex != null)
			docIndex.invalidateIndex();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractReactionProvider#attributeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, java.lang.Object, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void attributeChanged(ImObject object, String attributeName, Object oldValue, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		if ((object instanceof ImWord) && (ImWord.STRING_ATTRIBUTE.equals(attributeName) || ImWord.NEXT_RELATION_ATTRIBUTE.equals(attributeName) || ImWord.PREVIOUS_WORD_ATTRIBUTE.equals(attributeName) || ImWord.NEXT_WORD_ATTRIBUTE.equals(attributeName)))
			this.invalidatePageIndex((ImWord) object);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractReactionProvider#regionAdded(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionAdded(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		if (region instanceof ImWord)
			this.invalidatePageIndex((ImWord) region);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractReactionProvider#regionRemoved(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionRemoved(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		if (region instanceof ImWord)
			this.invalidatePageIndex((ImWord) region);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(final ImWord start, final ImWord end, final ImDocumentMarkupPanel idmp) {
		
		//	searching only makes sense in a single logical text stream
		if (!start.getTextStreamId().equals(end.getTextStreamId()))
			return null;
		
		//	generate word string and label
		final String wsStr = ImUtils.getString(start, end, true);
		String wsLabelStr = wsStr;
		if ((wsLabelStr.length() > 20) && (start != end) && (start.getNextWord() != end))
			wsLabelStr = (start.getString() + " ... " + end.getString());
		
		//	collect actions
		LinkedList actions = new LinkedList();
		
		//	find next forward
		actions.add(new SelectionAction("navFindNext", ("Find Next '" + wsLabelStr + "'"), ("Find (and move to) next occurrence of '" + wsLabelStr + "'")) {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				final ImWord[] next = find(wsStr, matchMode, start, false, idmp);
				if (next != null)
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							idmp.setWordSelection(next[0], next[1]);
							if (DEBUG_FIND) System.out.println(" ==> selection set to " + next[0].getLocalID());
						}
					});
				return false; // we're not changing anything
			}
		});
		
		//	find next backward
		actions.add(new SelectionAction("navFindPrev", ("Find Previous '" + wsLabelStr + "'"), ("Find (and move to) previous occurrence of '" + wsLabelStr + "'")) {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				final ImWord[] prev = find(wsStr, matchMode, start, true, idmp);
				if (prev != null)
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							idmp.setWordSelection(prev[0], prev[1]);
							if (DEBUG_FIND) System.out.println(" ==> selection set to " + prev[0].getLocalID());
						}
					});
				return false; // we're not changing anything
			}
		});
		
		//	provide 'Find <selected> ...' function, opening modal dialog with match options
		actions.add(new SelectionAction("navFind", ("Find '" + wsLabelStr + "' ..."), ("Find (and move to) occurrences of '" + wsLabelStr + "', customizing match mode")) {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				
				//	TODO maybe add tools menu entry to open dialog without word clicks
				
				FindDialog fd = new FindDialog(idmp, wsStr, start);
				fd.setVisible(true);
				return false; // we're not changing anything
			}
		});
		
		//	provide 'List All <selected>' function, opening non-modal navigation dialog
		actions.add(new SelectionAction("navList", ("List '" + wsLabelStr + "' ..."), ("List all occurrences of '" + wsLabelStr + "'")) {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				if (DEBUG_FIND) System.out.println("Listing occurrences of '" + wsStr + "' from " + start.getLocalID());
				ImWord[] match = {start, end};
				ArrayList matchList = new ArrayList();
				ListMatch matched = new ListMatch(match[0], match[1]);
				matchList.add(matched);
				while (true) {
					match = find(wsStr, matchMode, match[0], false, idmp);
					if (match == null)
						break;
					if (match[0] == start)
						break;
					matchList.add(new ListMatch(match[0], match[1]));
					if (DEBUG_FIND) System.out.println(" - found occurrence at " + match[0].getLocalID());
				}
				
				//	line up and sort matches
				ListMatch[] matches = ((ListMatch[]) matchList.toArray(new ListMatch[matchList.size()]));
				Arrays.sort(matches, listMatchOrder);
				
				//	TODO maybe add tools menu entry to open dialog without word clicks
				
				//	show matches in JList
				ListDialog ld = new ListDialog(idmp, wsStr, matches, matched);
				ld.setVisible(true);
				return false; // we're not changing anything
			}
		});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private class FindDataPanel extends JPanel {
		final JTextField toFindField;
		private SymbolTable symbolTable = null;
		
		private JCheckBox caseSensitive = new JCheckBox("Case Sensitive?", ((matchMode & MODE_CASE_SENSITIVE) != 0));
		private JCheckBox ignoreAccents = new JCheckBox("Ignore Accents?", ((matchMode & MODE_ACCENT_SENSITIVE) == 0));
		private JCheckBox ignoreSpace = new JCheckBox("Ignore Spaces?", ((matchMode & MODE_SPACE_SENSITIVE) == 0));
		private JCheckBox wordBoundarySensitive = new JCheckBox("Match Whole Word?", ((matchMode & MODE_WORD_BOUNDARY_SENSITIVE) != 0));
		
		FindDataPanel(final JDialog parent, String toFind, JComponent south) {
			super(new BorderLayout(), true);
			
			this.toFindField = new JTextField(toFind);
			this.toFindField.setFont(this.toFindField.getFont().deriveFont(12f));
			this.toFindField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createMatteBorder(1, 3, 1, 3, this.toFindField.getBackground())));
			JButton sButton = new JButton("Symbol");
			sButton.setToolTipText("Open symbol table for special characters");
			sButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 3, 1, 3, this.getBackground()), BorderFactory.createRaisedBevelBorder()));
			sButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (symbolTable == null)
						symbolTable = SymbolTable.getSharedSymbolTable();
					symbolTable.setOwner(new SymbolTable.Owner() {
						public void useSymbol(char symbol) {
							StringBuffer sb = new StringBuffer(toFindField.getText());
							int cp = toFindField.getCaretPosition();
							sb.insert(cp, symbol);
							toFindField.setText(sb.toString());
							toFindField.setCaretPosition(++cp);
						}
						public void symbolTableClosed() {
							symbolTable = null;
						}
						public Dimension getSize() {
							return parent.getSize();
						}
						public Point getLocation() {
							return parent.getLocation();
						}
						public Color getColor() {
							return parent.getBackground();
						}
					});
					symbolTable.open();
				}
			});
			JPanel toFindPanel = new JPanel(new BorderLayout(), true);
			toFindPanel.add(new JLabel("Find: "), BorderLayout.WEST);
			toFindPanel.add(this.toFindField, BorderLayout.CENTER);
			toFindPanel.add(sButton, BorderLayout.EAST);
			
			this.caseSensitive.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					updateMatchMode();
				}
			});
			this.ignoreAccents.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					updateMatchMode();
				}
			});
			this.ignoreSpace.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					updateMatchMode();
				}
			});
			this.wordBoundarySensitive.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					updateMatchMode();
				}
			});
			JPanel optPanel = new JPanel(new GridLayout(2, 2, 4, 4), true);
			optPanel.add(this.caseSensitive);
			optPanel.add(this.ignoreSpace);
			optPanel.add(this.ignoreAccents);
			optPanel.add(this.wordBoundarySensitive);
			
			this.add(toFindPanel, BorderLayout.NORTH);
			this.add(optPanel, BorderLayout.CENTER);
			if (south != null)
				this.add(south, BorderLayout.SOUTH);
		}
		private void updateMatchMode() {
			matchMode = ((this.caseSensitive.isSelected() ? MODE_CASE_SENSITIVE : 0) | (this.ignoreAccents.isSelected() ? 0 : MODE_ACCENT_SENSITIVE) | (this.ignoreSpace.isSelected() ? 0 : MODE_SPACE_SENSITIVE) | (this.wordBoundarySensitive.isSelected() ? MODE_WORD_BOUNDARY_SENSITIVE : 0));
		}
	}
	
	private Point listDialogLocation = null;
	private Dimension listDialogSize = new Dimension(400, 400);
	private class ListDialog extends DialogPanel implements WindowFocusListener {
		private ImDocumentMarkupPanel target;
		private ImWord pivot;
		private FindDataPanel dataPanel;
		private JList occList = new JList();
		ListDialog(ImDocumentMarkupPanel target, String toFind, ListMatch[] matches, ListMatch matched) {
			super(("Occurrences of '" + matched.string + "' (" + matches.length + ")"), false);
			this.target = target;
			
			this.occList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.occList.setSelectedValue(matched, true);
			this.occList.addListSelectionListener(new ListSelectionListener() {
				int selected = occList.getSelectedIndex();
				public void valueChanged(ListSelectionEvent lse) {
					int selected = occList.getSelectedIndex();
					if (selected != this.selected){
						this.selected = selected;
						ListMatch lm = ((ListMatch) occList.getSelectedValue());
						if (lm != null) {
							pivot = lm.start;
							ListDialog.this.target.setWordSelection(lm.start, lm.end);
						}
					}
				}
			});
			JScrollPane occListBox = new JScrollPane(this.occList);
			occListBox.getVerticalScrollBar().setUnitIncrement(50);
			occListBox.getVerticalScrollBar().setBlockIncrement(50);
			
			JButton uButton = new JButton("Update List");
			uButton.setBorder(BorderFactory.createRaisedBevelBorder());
			uButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					updateContent();
				}
			});
			
			this.dataPanel = new FindDataPanel(this.getDialog(), toFind, uButton);
			this.dataPanel.setBorder(BorderFactory.createLineBorder(this.dataPanel.getBackground(), 3));
			this.dataPanel.toFindField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					updateContent();
				}
			});
			
			JButton cButton = new JButton("Close");
			cButton.setBorder(BorderFactory.createRaisedBevelBorder());
			cButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dispose();
				}
			});
			
			this.add(this.dataPanel, BorderLayout.NORTH);
			this.add(occListBox, BorderLayout.CENTER);
			this.add(cButton, BorderLayout.SOUTH);
			
			this.getDialog().getOwner().addWindowFocusListener(this); // listen to focus going back to main window
			
			this.setSize(listDialogSize);
			if (listDialogLocation == null)
				this.setLocationRelativeTo(DialogFactory.getTopWindow());
			else this.setLocation(listDialogLocation);
			
			this.setContent(matches, matched);
		}
		public void windowGainedFocus(WindowEvent we) {
			this.getDialog().getOwner().removeWindowFocusListener(this); // we don't want to receive notifications after we're done with
			this.dispose(); // focus back to main window, we're done with
		}
		public void windowLostFocus(WindowEvent we) {}
		public void dispose() {
			listDialogLocation = this.getLocation();
			listDialogSize = this.getSize();
			super.dispose();
		}
		void updateContent() {
			String toFind = this.dataPanel.toFindField.getText().trim();
			if (toFind.length() == 0)
				return;
			
			//	make sure to stay in place if current match stays in list (e.g. because only options have changed)
			if (this.pivot.getPreviousWord() != null)
				this.pivot = this.pivot.getPreviousWord();
			
			//	find first match, and update pivot if found
			ImWord[] match = find(toFind, matchMode, this.pivot, false, this.target);
			if (match == null)
				return;
			this.pivot = match[0];
			ListMatch matched = new ListMatch(match[0], match[1]);
			
			//	collect further matches
			ArrayList matchList = new ArrayList();
			matchList.add(matched);
			while (true) {
				match = find(toFind, matchMode, match[0], false, this.target);
				if (match == null)
					break;
				if (match[0] == this.pivot)
					break;
				matchList.add(new ListMatch(match[0], match[1]));
				if (DEBUG_FIND) System.out.println(" - found occurrence at " + match[0].getLocalID());
			}
			
			//	line up and sort matches
			ListMatch[] matches = ((ListMatch[]) matchList.toArray(new ListMatch[matchList.size()]));
			Arrays.sort(matches,listMatchOrder);
			
			//	show what we got
			this.setContent(matches, matched);
			this.setTitle("Occurrences of '" + toFind + "' (" + matches.length + ")");
		}
		private void setContent(final ListMatch[] matches, ListMatch matched) {
			this.occList.setModel(new AbstractListModel() {
				public int getSize() {
					return matches.length;
				}
				public Object getElementAt(int index) {
					return matches[index];
				}
			});
			this.occList.validate();
			this.occList.repaint();
			this.pivot = matched.start;
			this.occList.setSelectedValue(matched, true);
		}
	}
	
	private class ListMatch {
		final ImWord start;
		final ImWord end;
		final String string;
		final String toString;
		ListMatch(ImWord start, ImWord end) {
			this.start = start;
			this.end = end;
			if (this.start == this.end) {
				if (this.start.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED)
					this.string = this.start.getString().substring(0, (this.start.getString().length()-1));
				else this.string = this.start.getString();
			}
			else {
				StringBuffer string = new StringBuffer(this.start.getString());
				if (this.start.getNextWord() != this.end)
					string.append(" ... ");
				else if (this.start.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED)
					string.delete((string.length() - 1), string.length());
				else if ((this.start.getNextRelation() != ImWord.NEXT_RELATION_CONTINUE) && Gamta.insertSpace(this.start.getString(), this.end.getString()))
					string.append(" ");
				string.append(this.end.getString());
				this.string = string.toString();
			}
			StringBuffer toString = new StringBuffer();
			int lContext = 0;
			for (ImWord imw = this.start.getPreviousWord(); imw != null; imw = imw.getPreviousWord()) {
				if (imw.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE);
				else if (imw.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED);
				else if (Gamta.insertSpace(imw.getString(), imw.getNextWord().getString())) {
					toString.insert(0, " ");
					lContext++;
				}
				String imwStr = imw.getString();
				if (imw.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED)
					imwStr = imwStr.substring(0, (imwStr.length() - 1));
				toString.insert(0, imwStr);
				lContext += (imwStr.length());
				if (lContext >= 20) {
					if (imw.getPreviousWord() != null)
						toString.insert(0, "... ");
					break;
				}
			}
			toString.append("<B>");
			toString.append(this.string);
			toString.append("</B>");
			int rContext = 0;
			for (ImWord imw = this.end.getNextWord(); imw != null; imw = imw.getNextWord()) {
				if (imw.getPreviousWord().getNextRelation() == ImWord.NEXT_RELATION_CONTINUE);
				else if (imw.getPreviousWord().getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED);
				else if (Gamta.insertSpace(imw.getPreviousWord().getString(), imw.getString())) {
					toString.append(" ");
					rContext++;
				}
				String imwStr = imw.getString();
				if (imw.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED)
					imwStr = imwStr.substring(0, (imwStr.length() - 1));
				toString.append(imwStr);
				rContext += (imwStr.length());
				if (rContext >= 20) {
					if (imw.getNextWord() != null)
						toString.append(" ...");
					break;
				}
			}
			toString.append(" (page " + (this.start.pageId + 1) + ")");
			toString.insert(0, "<HTML>");
			toString.append("</HTML>");
			this.toString = toString.toString();
		}
		public String toString() {
			return this.toString;
		}
	}
	
	private static final Comparator listMatchOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			ImWord imw1 = ((ListMatch) obj1).start;
			ImWord imw2 = ((ListMatch) obj2).start;
			if (imw1.pageId == imw2.pageId)
				return ImUtils.textStreamOrder.compare(imw1, imw2);
			else return (imw1.pageId - imw2.pageId);
		}
	};
	
	private Point findDialogLocation = null;
	private Dimension findDialogSize = new Dimension(300, 120);
	private class FindDialog extends DialogPanel implements WindowFocusListener {
		private ImDocumentMarkupPanel target;
		private ImWord pivot;
		
		private FindDataPanel dataPanel;
		
		private JButton fpButton = new JButton("Previous");
		private JButton fnButton = new JButton("Next");
		
		FindDialog(ImDocumentMarkupPanel target, String toFind, ImWord pivot) {
			super("Find ...", false);
			this.target = target;
			this.pivot = pivot;
			
			this.fpButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.fpButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					find(true);
				}
			});
			JButton cButton = new JButton("Close");
			cButton.setBorder(BorderFactory.createRaisedBevelBorder());
			cButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dispose();
				}
			});
			this.fnButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.fnButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					find(false);
				}
			});
			JPanel butPanel = new JPanel(new GridLayout(1, 0, 10, 0));
			butPanel.add(this.fpButton);
			butPanel.add(cButton);
			butPanel.add(this.fnButton);
			
			this.dataPanel = new FindDataPanel(this.getDialog(), toFind, butPanel);
			this.dataPanel.toFindField.getDocument().addDocumentListener(new DocumentListener() {
				public void insertUpdate(DocumentEvent de) {
					updateButtonTooltips();
				}
				public void removeUpdate(DocumentEvent de) {
					updateButtonTooltips();
				}
				public void changedUpdate(DocumentEvent de) {}
			});
			this.dataPanel.toFindField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					find(false);
				}
			});
			this.add(this.dataPanel, BorderLayout.CENTER);
			
			this.getDialog().getOwner().addWindowFocusListener(this); // listen to focus going back to main window
			
			this.setSize(findDialogSize);
			if (findDialogLocation == null)
				this.setLocationRelativeTo(DialogFactory.getTopWindow());
			else this.setLocation(findDialogLocation);
			
			this.updateButtonTooltips();
		}
		public void windowGainedFocus(WindowEvent we) {
			this.getDialog().getOwner().removeWindowFocusListener(this); // we don't want to receive notifications after we're done with
			this.dispose(); // focus back to main window, we're done with
		}
		public void windowLostFocus(WindowEvent we) {}
		public void dispose() {
			findDialogLocation = this.getLocation();
			findDialogSize = this.getSize();
			super.dispose();
		}
		void updateButtonTooltips() {
			String toFindStr = this.dataPanel.toFindField.getText().trim();
			this.fpButton.setToolTipText("Find previous occurrence of '" + toFindStr + "'");
			this.fnButton.setToolTipText("Find next occurrence of '" + toFindStr + "'");
		}
		void find(boolean backward) {
			String toFind = this.dataPanel.toFindField.getText().trim();
			if (toFind.length() == 0)
				return;
			ImWord[] match = NavigationActionProvider.this.find(toFind, matchMode, this.pivot, backward, this.target);
			if (match != null) {
				this.pivot = match[0];
				this.target.setWordSelection(match[0], match[1]);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, ImDocumentMarkupPanel idmp) { return null; /* we're only working on word selections */ }
	
	private void invalidatePageIndex(ImWord imw) {
		ImDocumentIndex docIndex = this.getDocumentIndex(imw.getDocument(), false);
		if (docIndex != null)
			docIndex.invalidatePageIndexFor(imw);
	}
	
	private ImWord[] find(String toFind, int matchMode, ImWord pivot, boolean backward, ImDocumentMarkupPanel idmp) {
		if (DEBUG_FIND) System.out.println("Seeking '" + toFind + "' from " + pivot.getLocalID() + " " + (backward ? "backward" : "forward"));
		
		//	normalize search string
		StringBuffer nToFindBuilder = new StringBuffer();
		for (int c = 0; c < toFind.length(); c++) {
			char ch = toFind.charAt(c);
			if (32 < ch) {
				if (ch < 127)
					nToFindBuilder.append(Character.toLowerCase(ch));
				else nToFindBuilder.append(StringUtils.getBaseChar(ch));
			}
		}
		String nToFind = nToFindBuilder.toString();
		if (DEBUG_FIND) System.out.println(" - normalized to '" + nToFind + "'");
		
		//	get document index
		ImDocumentIndex docIndex = this.getDocumentIndex(pivot.getDocument(), true);
		if (DEBUG_FIND) System.out.println(" - got document index");
		
		//	search next occurrence in specified direction
		ImDocument doc = pivot.getDocument();
		int pageId = pivot.pageId;
		boolean wrapped = false;
		while (true) {
			
			//	search visible page
			if (idmp.isPageVisible(pageId)) {
				if (DEBUG_FIND) System.out.println(" - searching page " + pageId);
				ImWord[] match = docIndex.find(toFind, matchMode, nToFind, pivot, backward, wrapped, doc.getPage(pageId));
				if (match != null) {
					if (DEBUG_FIND) System.out.println(" ==> got match at " + match[0].getLocalID());
					return match;
				}
			}
			
			//	initially skipped over part of page of origin searched, we're done
			if (wrapped && (pageId == pivot.pageId)) {
				if (DEBUG_FIND) System.out.println(" ==> no match found");
				return null;
			}
			
			//	backward search, switch to previous page, and wrap at document start
			if (backward) {
				pageId--;
				if (pageId < 0) {
					pageId = (doc.getPageCount() - 1);
					wrapped = true;
				}
			}
			
			//	forward search, switch to next page, and wrap at document end
			else {
				pageId++;
				if (doc.getPageCount() <= pageId) {
					pageId = 0;
					wrapped = true;
				}
			}
		}
	}
	
	private Map docIndexesByDocID = Collections.synchronizedMap(new HashMap());
	
	private ImDocumentIndex getDocumentIndex(ImDocument doc, boolean create) {
		ImDocumentIndex docIndex = ((ImDocumentIndex) this.docIndexesByDocID.get(doc.docId));
		if ((docIndex == null) && create) {
			docIndex = new ImDocumentIndex(doc);
			this.docIndexesByDocID.put(doc.docId, docIndex);
		}
		return docIndex;
	}
	
	private static class ImDocumentIndex {
		final ImPageIndex[] pageIndexes;
		
		ImDocumentIndex(ImDocument doc) {
			this.pageIndexes = new ImPageIndex[doc.getPageCount()];
		}
		
		void invalidateIndex() {
			for (int p = 0; p < this.pageIndexes.length; p++) {
				if (this.pageIndexes[p] != null)
					this.pageIndexes[p].invalidate();
			}
		}
		
		void invalidatePageIndexFor(ImWord imw) {
			if (this.pageIndexes[imw.pageId] == null)
				return;
			this.pageIndexes[imw.pageId].invalidate();
			if ((imw.pageId == 0) || (this.pageIndexes[imw.pageId - 1] == null))
				return;
			this.pageIndexes[imw.pageId - 1].invalidateIfContains(imw);
		}
		
		ImWord[] find(String toFind, int matchMode, String nToFind, ImWord pivot, boolean backward, boolean wrapped, ImPage inPage) {
			
			//	make sure page index exists
			if (this.pageIndexes[inPage.pageId] == null)
				this.pageIndexes[inPage.pageId] = new ImPageIndex(inPage);
			
			//	use page index
			return this.pageIndexes[inPage.pageId].find(toFind, matchMode, nToFind, pivot, backward, wrapped);
		}
	}
	
	private static class ImPageIndex {
		final ImPage page;
		private ImPageTextStreamIndex[] tsIndexes;
		
		ImPageIndex(ImPage page) {
			this.page = page;
		}
		
		void invalidate() {
			this.tsIndexes = null;
		}
		
		void invalidateIfContains(ImWord imw) {
			if (this.tsIndexes == null)
				return;
			for (int s = 0; s < this.tsIndexes.length; s++)
				if (imw.getTextStreamId().equals(this.tsIndexes[s].textStreamWords[0].getLocalID()) && (ImUtils.textStreamOrder.compare(imw, this.tsIndexes[s].textStreamWords[this.tsIndexes[s].textStream.length() - 1]) <= 0)) {
					this.tsIndexes = null;
					break;
				}
		}
		
		void validate() {
			
			//	get text stream heads
			ImWord[] tshs = this.page.getTextStreamHeads();
			
			//	sort text stream heads by stream IDs
			Arrays.sort(tshs, ImUtils.textStreamOrder);
			
			//	build index for each text stream
			this.tsIndexes = new ImPageTextStreamIndex[tshs.length];
			for (int h = 0; h < tshs.length; h++)
				this.tsIndexes[h] = new ImPageTextStreamIndex(tshs[h]);
		}
		
		ImWord[] find(String toFind, int matchMode, String nToFind, ImWord pivot, boolean backward, boolean wrapped) {
			this.validate();
			
			//	backward search
			if (backward) {
				for (int s = (this.tsIndexes.length - 1); s >= 0; s--) {
					
					//	same page as pivot word, need to observe wrapping and text stream order
					if (pivot.pageId == this.page.pageId) {
						
						//	pivot before first word of text stream, skip stream in non-wrapped search
						if (!wrapped && (ImUtils.textStreamOrder.compare(pivot, this.tsIndexes[s].textStreamWords[0]) < 0))
							continue;
						
						//	pivot after last word of text stream, skip stream in wrapped search
						if (wrapped && (ImUtils.textStreamOrder.compare(this.tsIndexes[s].textStreamWords[this.tsIndexes[s].textStreamWords.length-1], pivot) < 0))
							continue;
					}
					
					//	perform match in eligible text stream
					ImWord[] match = this.tsIndexes[s].find(toFind, matchMode, nToFind, pivot, backward, wrapped);
					if (match != null)
						return match;
				}
			}
			
			//	forward search
			else {
				for (int s = 0; s < this.tsIndexes.length; s++) {
					
					//	same page as pivot word, need to observe wrapping and text stream order
					if (pivot.pageId == this.page.pageId) {
						
						//	pivot before first word of text stream, skip stream in wrapped search
						if (wrapped && (ImUtils.textStreamOrder.compare(pivot, this.tsIndexes[s].textStreamWords[0]) < 0))
							continue;
						
						//	pivot after last word of text stream, skip stream in non-wrapped search
						if (!wrapped && (ImUtils.textStreamOrder.compare(this.tsIndexes[s].textStreamWords[this.tsIndexes[s].textStreamWords.length-1], pivot) < 0))
							continue;
					}
					
					//	perform match in eligible text stream
					ImWord[] match = this.tsIndexes[s].find(toFind, matchMode, nToFind, pivot, backward, wrapped);
					if (match != null)
						return match;
				}
			}
			
			//	nothing found
			return null;
		}
	}
	
	private static class ImPageTextStreamIndex {
		final String textStream;
		final ImWord[] textStreamWords;
		
		ImPageTextStreamIndex(ImWord tsh) {
			StringBuffer ts = new StringBuffer();
			ArrayList tsWords = new ArrayList();
			int nextPageOverlap = 32;
			for (ImWord imw = tsh; imw != null; imw = imw.getNextWord()) {
				String imwStr = imw.getString().toLowerCase();
				if (imwStr.length() == 0)
					continue;
				for (int c = 0; c < (imwStr.length() - ((imw.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED) ? 1 : 0)); c++) {
					char ch = imwStr.charAt(c);
					if (32 < ch) {
						if (ch < 127)
							ts.append(Character.toLowerCase(ch));
						else ts.append(StringUtils.getBaseChar(ch));
						tsWords.add(imw);
					}
				}
				if (imw.pageId != tsh.pageId) {
					nextPageOverlap -= imwStr.length();
					if (nextPageOverlap <= 0)
						break;
				}
			}
			this.textStream = ts.toString();
			this.textStreamWords = ((ImWord[]) tsWords.toArray(new ImWord[tsWords.size()]));
		}
		
		ImWord[] find(String toFind, int matchMode, String nToFind, ImWord pivot, boolean backward, boolean wrapped) {
			
			//	test if text stream contains pivot
			boolean pivotInTextStream = ((ImUtils.textStreamOrder.compare(this.textStreamWords[0], pivot) <= 0) && (ImUtils.textStreamOrder.compare(pivot, this.textStreamWords[this.textStreamWords.length-1]) <= 0));
			
			//	backward search
			if (backward) {
				int matchIndex = this.textStream.length();
				while (true) {
					
					//	get next match
					matchIndex = this.textStream.lastIndexOf(nToFind, matchIndex);
					
					//	no use searching any further
					if (matchIndex == -1)
						return null;
					if (DEBUG_FIND) System.out.println(" - match found at " + matchIndex + " in " + this.textStream);
					
					//	reject match in next page overlap
					if (this.textStreamWords[matchIndex].pageId != this.textStreamWords[0].pageId) {
						if (DEBUG_FIND) System.out.println(" ==> only in next page overlap, giving up");
						return null;
					}
					
					//	check position relative to pivot if latter lies in text stream
					if (pivotInTextStream) {
						
						//	wrapped search, break if match start before pivot
						if (wrapped && (ImUtils.textStreamOrder.compare(this.textStreamWords[matchIndex], pivot) < 0)) {
							if (DEBUG_FIND) System.out.println(" ==> before pivot after wrapping, giving up");
							return null;
						}
						
						//	non-wrapped search, reject match if start at or after pivot
						if (!wrapped && (ImUtils.textStreamOrder.compare(pivot, this.textStreamWords[matchIndex]) <= 0)) {
							if (DEBUG_FIND) System.out.println(" --> at or after pivot before wrapping, skipping");
							matchIndex--;
							continue;
						}
					}
					
					//	if strict match works as well, we have a match
					if (this.verifyMatch(toFind, matchMode, nToFind, matchIndex)) {
						ImWord[] match = {this.textStreamWords[matchIndex], this.textStreamWords[matchIndex + nToFind.length() - 1]};
						return match;
					}
					
					//	start next match one character left of last match
					matchIndex--;
				}
			}
			
			//	forward search
			else {
				int matchIndex = 0;
				while (true) {
					
					//	get next match
					matchIndex = this.textStream.indexOf(nToFind, matchIndex);
					
					//	no use searching any further
					if (matchIndex == -1)
						return null;
					if (DEBUG_FIND) System.out.println(" - match found at " + matchIndex + " in " + this.textStream);
					
					//	reject match in next page overlap
					if (this.textStreamWords[matchIndex].pageId != this.textStreamWords[0].pageId) {
						if (DEBUG_FIND) System.out.println(" ==> only in next page overlap, giving up");
						return null;
					}
					
					//	check position relative to pivot if latter lies in text stream
					if (pivotInTextStream) {
						
						//	wrapped search, break if match start after pivot
						if (wrapped && (ImUtils.textStreamOrder.compare(pivot, this.textStreamWords[matchIndex]) < 0)) {
							if (DEBUG_FIND) System.out.println(" ==> after pivot after wrapping, giving up");
							return null;
						}
						
						//	non-wrapped search, reject match if start before or at pivot
						if (!wrapped && (ImUtils.textStreamOrder.compare(this.textStreamWords[matchIndex], pivot) <= 0)) {
							if (DEBUG_FIND) System.out.println(" --> before or at pivot before wrapping, skipping");
							matchIndex++;
							continue;
						}
					}
					
					//	if strict match works as well, we have a match
					if (this.verifyMatch(toFind, matchMode, nToFind, matchIndex)) {
						ImWord[] match = {this.textStreamWords[matchIndex], this.textStreamWords[matchIndex + nToFind.length() - 1]};
						return match;
					}
					
					//	start next match one character right of last match
					matchIndex++;
				}
			}
		}
		
		private boolean verifyMatch(String toFind, int matchMode, String nToFind, int matchIndex) {
			
			//	check word boundaries first
			if ((matchMode & MODE_WORD_BOUNDARY_SENSITIVE) != 0) {
				ImWord preMatchWord = ((matchIndex == 0) ? this.textStreamWords[0].getPreviousWord() : this.textStreamWords[matchIndex - 1]);
				if (preMatchWord != null) {
					if (this.textStreamWords[matchIndex] == preMatchWord) {
						if (DEBUG_FIND) System.out.println(" --> verification failed: start not at word boundary");
						return false;
					}
					if (preMatchWord.getNextRelation() == ImWord.NEXT_RELATION_CONTINUE) {
						if (DEBUG_FIND) System.out.println(" --> verification failed: start continues previous word");
						return false;
					}
					if (preMatchWord.getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED) {
						if (DEBUG_FIND) System.out.println(" --> verification failed: start continues previous word (hyphenated)");
						return false;
					}
				}
				ImWord postMatchWord = (((matchIndex + nToFind.length()) == this.textStream.length()) ? this.textStreamWords[this.textStream.length() - 1].getNextWord() : this.textStreamWords[matchIndex + nToFind.length()]);
				if (postMatchWord != null) {
					if (this.textStreamWords[matchIndex + nToFind.length() - 1] == postMatchWord) {
						if (DEBUG_FIND) System.out.println(" --> verification failed: end not at word boundary");
						return false;
					}
					if (this.textStreamWords[matchIndex + nToFind.length() - 1].getNextRelation() == ImWord.NEXT_RELATION_CONTINUE) {
						if (DEBUG_FIND) System.out.println(" --> verification failed: word continues after match");
						return false;
					}
					if (this.textStreamWords[matchIndex + nToFind.length() - 1].getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED) {
						if (DEBUG_FIND) System.out.println(" --> verification failed: word continues after match (hyphenated)");
						return false;
					}
				}
			}
			
			//	ignoring all other details ==> no further comparison required
			if ((matchMode & (MODE_SPACE_SENSITIVE | MODE_CASE_SENSITIVE | MODE_ACCENT_SENSITIVE)) == 0)
				return true;
			
			//	get match mode
			boolean ignoreSpace = ((matchMode & MODE_SPACE_SENSITIVE) == 0);
			boolean ignoreCase = ((matchMode & MODE_CASE_SENSITIVE) == 0);
			boolean ignoreAccents = ((matchMode & MODE_ACCENT_SENSITIVE) == 0);
			
			//	compute offset of match start in first word
			int wordCharOffset = 0;
			while ((wordCharOffset < matchIndex) && (this.textStreamWords[matchIndex] == this.textStreamWords[matchIndex - wordCharOffset - 1]))
				wordCharOffset++;
			
			//	check character by character
			char lCh = ((char) 0);
			for (int c = 0, n = 0; c < toFind.length(); c++) {
				
				//	get next match character
				char ch = toFind.charAt(c);
				
				//	space ==> require word boundary and non-merging next word relation if space sensitive
				if (ch < 33) {
					
					//	jump leading space, or ignore space altogether
					if ((n == 0) || ignoreSpace) {
						lCh = ch;
						continue;
					}
					
					//	test word relation
					if (this.textStreamWords[matchIndex + n - 1] == this.textStreamWords[matchIndex + n]) {
						if (DEBUG_FIND) System.out.println(" --> verification failed: word continues over space");
						return false;
					}
					else if (this.textStreamWords[matchIndex + n - 1].getNextRelation() == ImWord.NEXT_RELATION_CONTINUE) {
						if (DEBUG_FIND) System.out.println(" --> verification failed: word continues over space (concatenated)");
						return false;
					}
					else if (this.textStreamWords[matchIndex + n - 1].getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED) {
						if (DEBUG_FIND) System.out.println(" --> verification failed: word continues over space (hyphenated)");
						return false;
					}
					
					//	move on to next character
					else {
						lCh = ch;
						continue;
					}
				}
				
				//	get next text stream character
				if ((n != 0) && (this.textStreamWords[matchIndex + n] != this.textStreamWords[matchIndex + n - 1]))
					wordCharOffset = 0;
				char tsCh = this.textStreamWords[matchIndex + n].getString().charAt(wordCharOffset++);
				
				//	check if normalization even required
				if ((ch != tsCh) || (127 < ch) || (127 < tsCh)) {
					
					//	normalize current character
					if (ignoreAccents && (127 < ch))
						ch = StringUtils.getBaseChar(ch);
					if (ignoreCase)
						ch = Character.toLowerCase(ch);
					
					//	normalize current text stream character
					if (ignoreAccents && (127 < tsCh))
						tsCh = StringUtils.getBaseChar(tsCh);
					if (ignoreCase)
						tsCh = Character.toLowerCase(tsCh);
					
					//	compare again
					if (ch != tsCh) {
						if (DEBUG_FIND) System.out.println(" --> verification failed: character mismatch at " + c + "('" + ch + "' vs. '" + tsCh + "')");
						return false;
					}
				}
				
				//	we're ignoring spaces, or we're right after one (counting a virtual leading space) ==> no need for checking character relation
				if (ignoreSpace || (n == 0) || (lCh < 33)) {
					lCh = ch;
					n++; // move to next text stream character
					continue;
				}
				
				//	two letters, or two digits ==> require same word or merging next word relation
				if ((Character.isLetter(lCh) && Character.isLetter(ch)) || (Character.isDigit(lCh) && Character.isDigit(ch))) {
					if (this.textStreamWords[matchIndex + n - 1] == this.textStreamWords[matchIndex + n]);
					else if (this.textStreamWords[matchIndex + n - 1].getNextRelation() == ImWord.NEXT_RELATION_CONTINUE);
					else if (this.textStreamWords[matchIndex + n - 1].getNextRelation() == ImWord.NEXT_RELATION_HYPHENATED);
					else {
						if (DEBUG_FIND) System.out.println(" --> verification failed: word broken at " + c + "(between '" + lCh + "' and '" + ch + "')");
						return false;
					}
				}
				
				//	move to next text stream character
				lCh = ch;
				n++;
			}
			
			//	no counter indication found
			return true;
		}
	}
}