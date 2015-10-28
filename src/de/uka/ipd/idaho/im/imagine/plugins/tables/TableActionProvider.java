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
package de.uka.ipd.idaho.im.imagine.plugins.tables;

import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.TwoClickSelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This plugin provides actions for marking tables in Image Markup documents,
 * and for very simple data extraction from tables.
 * 
 * @author sautter
 */
public class TableActionProvider extends AbstractSelectionActionProvider implements ImageMarkupToolProvider, ReactionProvider {
	private static final String TABLE_CLEANER_IMT_NAME = "TableAnnotCleaner";
	private ImageMarkupTool tableCleaner = new TableCleaner();
	
	private static final String SPLIT_OPERATION = "split";
	private static final String REMOVE_OPERATION = "remove";
	private static final String CUT_TO_START_OPERATION = "cutToStart";
	private static final String CUT_TO_END_OPERATION = "cutToEnd";
	private Properties annotTypeOperations = new Properties();
	
	//	example with multi-line cells (in-cell line margin about 5 pixels, row margin about 15 pixels): zt00904.pdf, page 6
	
	//	example without multi-line cells, very tight row margin (2 pixels): Milleretal2014Anthracotheres.pdf, page 4
	
	//	example without multi-line cells, normal row margin: zt00619.o.pdf, page 3
	
	/** public zero-argument constructor for class loading */
	public TableActionProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Table Actions";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	read operations for individual annotation types
		try {
			Reader tor = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream("tableCleanupOptions.cnfg")));
			StringVector typeOperationList = StringVector.loadList(tor);
			tor.close();
			for (int t = 0; t < typeOperationList.size(); t++) {
				String typeOperation = typeOperationList.get(t).trim();
				if (typeOperation.length() == 0)
					continue;
				if (typeOperation.startsWith("//"))
					continue;
				String[] typeAndOperation = typeOperation.split("\\s+");
				if (typeAndOperation.length == 2)
					this.annotTypeOperations.setProperty(typeAndOperation[0], typeAndOperation[1]);
			}
		} catch (IOException ioe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getEditMenuItemNames()
	 */
	public String[] getEditMenuItemNames() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		String[] tmins = {TABLE_CLEANER_IMT_NAME};
		return tmins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (TABLE_CLEANER_IMT_NAME.equals(name))
			return this.tableCleaner;
		else return null;
	}
	
	private class TableCleaner implements ImageMarkupTool {
		public String getLabel() {
			return "Clean Table Annotations";
		}
		public String getTooltip() {
			return "Clean annotations in tables, specifically ones spanning column breaks";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	we're only processing documents as a whole
			if (annot == null)
				cleanupTableAnnotations(doc, pm);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(final ImWord start, ImWord end, final ImDocumentMarkupPanel idmp) {
		
		//	anything to work with?
		if (!ImWord.TEXT_STREAM_TYPE_TABLE.equals(start.getTextStreamType()) || !start.getTextStreamId().equals(end.getTextStreamId()) || (start.pageId != end.pageId))
			return null;
		if (!idmp.areRegionsPainted(ImRegion.TABLE_TYPE))
			return null;
		
		//	get start table
		final ImRegion startTable = this.getTableAt(start);
		if (startTable == null) {
			System.out.println("Enclosing table not found");
			return null;
		}
		
		//	collect actions
		LinkedList actions = new LinkedList();
		
		//	offer merging rows across tables
		if (idmp.areRegionsPainted(ImRegion.TABLE_ROW_TYPE) && !startTable.hasAttribute("rowsContinueIn"))
			actions.add(new TwoClickSelectionAction("tableExtendRows", "Connect Table Rows", "Connect the rows in this table to those in another table, merging the tables left to right.") {
				public boolean performAction(ImWord secondWord) {
					if (start.getTextStreamId().equals(secondWord.getTextStreamId())) {
						DialogFactory.alert(("'" + secondWord.getString() + "' belongs to the same table as '" + start.getString() + "'\nA table cannot be merged with itself"), "Cannot Merge Rows", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					
					ImRegion secondWordTable = getTableAt(secondWord);
					if (!ImWord.TEXT_STREAM_TYPE_TABLE.equals(secondWord.getTextStreamType()) || (secondWordTable == null)) {
						DialogFactory.alert(("'" + secondWord.getString() + "' does not belong to a table"), "Cannot Merge Rows", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					
					if (!ImUtils.areTableRowsCompatible(startTable, secondWordTable, true)) {
						DialogFactory.alert("The two tables are not compatible; in order to merge rows, two tables have to have\n- the same number of rows\n- the same label on each one but the first (column header) row", "Cannot Merge Rows", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					
					ImRegion[] startTables = ImUtils.getColumnConnectedTables(startTable);
					ImRegion[] secondWordTables = ImUtils.getColumnConnectedTables(secondWordTable);
					if (startTables.length != secondWordTables.length) {
						DialogFactory.alert("The two tables are connected to other tables, and the rows not compatible;\nin order to merge rows, two tables have to have\n- the same number of rows\n- the same label on each one but the first (column header) row\nTry establishing all column extension relations first", "Cannot Merge Rows", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					
					if (startTables.length > 1) {
						for (int t = 0; t < startTables.length; t++)
							if (!ImUtils.areTableRowsCompatible(startTables[t], secondWordTables[t], true)) {
								DialogFactory.alert("The two tables are connected to other tables, and the rows not compatible;\nin order to merge rows, two tables have to have\n- the same number of rows\n- the same label on each one but the first (column header) row\nTry establishing all column extension relations first", "Cannot Merge Rows", JOptionPane.ERROR_MESSAGE);
								return false;
							}
					}
					
					for (int t = 0; t < startTables.length; t++) {
						startTables[t].setAttribute("rowsContinueIn", (secondWordTables[t].pageId + "." + secondWordTables[t].bounds));
						secondWordTables[t].setAttribute("rowsContinueFrom", (startTables[t].pageId + "." + startTables[t].bounds));
					}
					DialogFactory.alert("Table rows merged successfully; you can copy the whole grid of connected tables via 'Copy Table Grid Data'", "Table Rows Merged", JOptionPane.INFORMATION_MESSAGE);
					return true;
				}
				public ImWord getFirstWord() {
					return start;
				}
				public String getActiveLabel() {
					return ("Click some word in the table to connect rows to");
				}
			});
		
		//	offer merging columns across tables
		if (idmp.areRegionsPainted(ImRegion.TABLE_COL_TYPE) && !startTable.hasAttribute("colsContinueIn"))
			actions.add(new TwoClickSelectionAction("tableExtendCols", "Connect Table Columns", "Connect the columns in this table to those in another table, merging the tables top to bottom.") {
				public boolean performAction(ImWord secondWord) {
					if (start.getTextStreamId().equals(secondWord.getTextStreamId())) {
						DialogFactory.alert(("'" + secondWord.getString() + "' belongs to the same table as '" + start.getString() + "'\nA table cannot be merged with itself"), "Cannot Merge Columns", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					
					ImRegion secondWordTable = getTableAt(secondWord);
					if (!ImWord.TEXT_STREAM_TYPE_TABLE.equals(secondWord.getTextStreamType()) || (secondWordTable == null)) {
						DialogFactory.alert(("'" + secondWord.getString() + "' does not belong to a table"), "Cannot Merge Columns", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					
					if (!ImUtils.areTableColumnsCompatible(startTable, secondWordTable, true)) {
						DialogFactory.alert("The two tables are not compatible; in order to merge columns, two tables have to have\n- the same number of coumns\n- the same header on each one but the first (row label) column", "Cannot Merge Columns", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					
					ImRegion[] startTables = ImUtils.getRowConnectedTables(startTable);
					ImRegion[] secondWordTables = ImUtils.getRowConnectedTables(secondWordTable);
					if (startTables.length != secondWordTables.length) {
						DialogFactory.alert("The two tables are connected to other tables, and the columns not compatible;\nin order to merge columns, two tables have to have\n- the same number of columns\n- the same label on each one but the first (row label) column\nTry establishing all row extension relations first", "Cannot Merge Columns", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					
					if (startTables.length > 1) {
						for (int t = 0; t < startTables.length; t++)
							if (!ImUtils.areTableColumnsCompatible(startTables[t], secondWordTables[t], true)) {
								DialogFactory.alert("The two tables are connected to other tables, and the columns not compatible;\nin order to merge columns, two tables have to have\n- the same number of columns\n- the same label on each one but the first (row label) column\nTry establishing all row extension relations first", "Cannot Merge Columns", JOptionPane.ERROR_MESSAGE);
								return false;
							}
					}
					
					for (int t = 0; t < startTables.length; t++) {
						startTables[t].setAttribute("colsContinueIn", (secondWordTables[t].pageId + "." + secondWordTables[t].bounds));
						secondWordTables[t].setAttribute("colsContinueFrom", (startTables[t].pageId + "." + startTables[t].bounds));
					}
					DialogFactory.alert("Table columns merged successfully; you can copy the whole grid of connected tables via 'Copy Table Grid Data'", "Table Columns Merged", JOptionPane.INFORMATION_MESSAGE);
					return true;
				}
				public ImWord getFirstWord() {
					return start;
				}
				public String getActiveLabel() {
					return ("Click some word in the table to connect columns to");
				}
			});
		
		//	offer dissecting as rows and columns
		if (startTable.hasAttribute("rowsContinueFrom") || startTable.hasAttribute("rowsContinueIn"))
			actions.add(new SelectionAction("tableCutRows", "Disconnect Table Rows", "Disconnect the rows in this table from the other tables they are connected to.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					ImRegion[] tables = ImUtils.getColumnConnectedTables(startTable);
					ImRegion leftTable = ImUtils.getTableForId(idmp.document, ((String) (startTable.getAttribute("rowsContinueFrom"))));
					ImRegion[] leftTables = null;
					if (leftTable != null) {
						leftTables = ImUtils.getColumnConnectedTables(leftTable);
						for (int t = 0; t < leftTables.length; t++)
							leftTables[t].removeAttribute("rowsContinueIn");
						for (int t = 0; t < tables.length; t++)
							tables[t].removeAttribute("rowsContinueFrom");
					}
					ImRegion rightTable = ImUtils.getTableForId(idmp.document, ((String) (startTable.getAttribute("rowsContinueIn"))));
					ImRegion[] rightTables = null;
					if (rightTable != null) {
						rightTables = ImUtils.getColumnConnectedTables(rightTable);
						for (int t = 0; t < tables.length; t++)
							tables[t].removeAttribute("rowsContinueIn");
						for (int t = 0; t < rightTables.length; t++)
							rightTables[t].removeAttribute("rowsContinueFrom");
					}
					if ((leftTable != null) && (rightTable != null))
						ImUtils.connectTableRows(leftTable, rightTable);
					DialogFactory.alert("Table rows dissected successfully", "Table Rows Dissected", JOptionPane.INFORMATION_MESSAGE);
					return true;
				}
			});
		if (startTable.hasAttribute("colsContinueFrom") || startTable.hasAttribute("colsContinueIn"))
			actions.add(new SelectionAction("tableCutCols", "Disconnect Table Columns", "Disconnect the columns in this table from the other tables they are connected to.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					ImRegion[] tables = ImUtils.getColumnConnectedTables(startTable);
					ImRegion topTable = ImUtils.getTableForId(idmp.document, ((String) (startTable.getAttribute("colsContinueFrom"))));
					ImRegion[] topTables = null;
					if (topTable != null) {
						topTables = ImUtils.getRowConnectedTables(topTable);
						for (int t = 0; t < topTables.length; t++)
							topTables[t].removeAttribute("colsContinueIn");
						for (int t = 0; t < tables.length; t++)
							tables[t].removeAttribute("colsContinueFrom");
					}
					ImRegion bottomTable = ImUtils.getTableForId(idmp.document, ((String) (startTable.getAttribute("colsContinueIn"))));
					ImRegion[] bottomTables = null;
					if (bottomTable != null) {
						bottomTables = ImUtils.getRowConnectedTables(bottomTable);
						for (int t = 0; t < tables.length; t++)
							tables[t].removeAttribute("colsContinueIn");
						for (int t = 0; t < bottomTables.length; t++)
							bottomTables[t].removeAttribute("colsContinueFrom");
					}
					if ((topTable != null) && (bottomTable != null))
						ImUtils.connectTableColumns(topTable, bottomTable);
					DialogFactory.alert("Table columns dissected successfully", "Table Columns Dissected", JOptionPane.INFORMATION_MESSAGE);
					return true;
				}
			});
		
		//	offer assigning caption with second click
		actions.add(new TwoClickSelectionAction("assignCaptionTable", "Assign Caption", "Assign a caption to this table with a second click.") {
			private ImWord artificialStartWord = null;
			public boolean performAction(ImWord secondWord) {
				if (!ImWord.TEXT_STREAM_TYPE_CAPTION.equals(secondWord.getTextStreamType()))
					return false;
				
				//	find affected caption
				ImAnnotation[] wordAnnots = idmp.document.getAnnotationsSpanning(secondWord);
				ArrayList wordCaptions = new ArrayList(2);
				for (int a = 0; a < wordAnnots.length; a++) {
					if (ImAnnotation.CAPTION_TYPE.equals(wordAnnots[a].getType()))
						wordCaptions.add(wordAnnots[a]);
				}
				if (wordCaptions.size() != 1)
					return false;
				ImAnnotation wordCaption = ((ImAnnotation) wordCaptions.get(0));
				
				//	does this caption match?
				if (!wordCaption.getFirstWord().getString().toLowerCase().startsWith("tab"))
					return false;
				
				//	set attributes
				wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, startTable.bounds.toString());
				wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + startTable.pageId));
				wordCaption.setAttribute("targetIsTable");
				return true;
			}
			public ImWord getFirstWord() {
				if (this.artificialStartWord == null)
					this.artificialStartWord = new ImWord(startTable.getDocument(), startTable.pageId, startTable.bounds, "TABLE");
				return this.artificialStartWord;
			}
			public String getActiveLabel() {
				return ("Click on a caption to assign it to the table at " + startTable.bounds.toString() + " on page " + (startTable.pageId + 1));
			}
		});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private ImRegion getTableAt(ImWord imw) {
		ImRegion[] imwPageTables = imw.getPage().getRegions(ImRegion.TABLE_TYPE);
		if (imwPageTables.length == 0)
			return null;
		for (int t = 0; t < imwPageTables.length; t++) {
			if (imwPageTables[t].bounds.includes(imw.bounds, false))
				return imwPageTables[t];
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#typeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void typeChanged(ImObject object, String oldType, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#attributeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, java.lang.Object, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void attributeChanged(ImObject object, String attributeName, Object oldValue, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#regionAdded(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionAdded(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		if (!ImRegion.TABLE_TYPE.equals(region.getType()))
			return;
		
		//	get potential captions
		ImAnnotation[] captionAnnots = ImUtils.findCaptions(region, true, true, true);
		
		//	try setting attributes in unassigned captions first
		for (int a = 0; a < captionAnnots.length; a++) {
			if (captionAnnots[a].hasAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE) || captionAnnots[a].hasAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE))
				continue;
			captionAnnots[a].setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + region.pageId));
			captionAnnots[a].setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, region.bounds.toString());
			captionAnnots[a].setAttribute("targetIsTable");
			return;
		}
		
		//	set attributes in any caption (happens if user corrects, for instance)
		if (captionAnnots.length != 0) {
			captionAnnots[0].setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + region.pageId));
			captionAnnots[0].setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, region.bounds.toString());
			captionAnnots[0].setAttribute("targetIsTable");
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#regionRemoved(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionRemoved(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		if (!ImRegion.TABLE_TYPE.equals(region.getType()))
			return;
		ImPage page = idmp.document.getPage(region.pageId);
		ImRegion[] subRegions = page.getRegionsInside(region.bounds, true);
		for (int r = 0; r < subRegions.length; r++) {
			if (ImRegion.TABLE_CELL_TYPE.equals(subRegions[r].getType()) || ImRegion.TABLE_COL_TYPE.equals(subRegions[r].getType()) || ImRegion.TABLE_ROW_TYPE.equals(subRegions[r].getType()))
				page.removeRegion(subRegions[r]);
		}
		ImWord[] words = page.getWordsInside(region.bounds);
		if (words.length != 0) {
			ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
			Arrays.sort(words, ImUtils.textStreamOrder);
			words[0].setTextStreamType(ImWord.TEXT_STREAM_TYPE_MAIN_TEXT);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationAdded(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationAdded(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		
		//	we're only interested in tables
		if (!ImWord.TEXT_STREAM_TYPE_TABLE.equals(annotation.getFirstWord().getTextStreamType()))
			return;
		
		//	single-word annotation cannot span multiple table cells
		if (annotation.getFirstWord() == annotation.getLastWord())
			return;
		
		//	annotation spanning two pages, none of out business here
		if (annotation.getFirstWord().pageId != annotation.getLastWord().pageId)
			return;
		
		//	get document and page
		ImDocument doc = annotation.getDocument();
		if (doc == null)
			return;
		ImPage page = doc.getPage(annotation.getFirstWord().pageId);
		if (page == null)
			return;
		
		//	get and sort table cells
		ImRegion[] cells = page.getRegions(ImRegion.TABLE_CELL_TYPE);
		Arrays.sort(cells, ImUtils.leftRightOrder);
		Arrays.sort(cells, ImUtils.topDownOrder);
		
		//	index cells by words
		Map wordsToCells = new HashMap();
		ImRegion wordCell = null;
		for (ImWord imw = annotation.getFirstWord(); imw != null; imw = imw.getNextWord()) {
			if ((wordCell != null) && (wordCell.bounds.includes(imw.bounds, true)))
				wordsToCells.put(imw, wordCell);
			else for (int c = 0; c < cells.length; c++)
				if (cells[c].bounds.includes(imw.bounds, true)) {
					wordCell = cells[c];
					wordsToCells.put(imw, wordCell);
					break;
				}
			if (imw == annotation.getLastWord())
				break;
		}
		
		//	mark parts of original annotation in individual cells
		this.cleanupTableAnnotation(doc, annotation, wordsToCells);
		
		//	perform document annotation cleanup to deal with whatever duplicates we might have incurred
		doc.cleanupAnnotations();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationRemoved(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationRemoved(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	private void cleanupTableAnnotations(ImDocument doc, ProgressMonitor pm) {
		
		//	clean up tables page by page
		ImPage[] pages = doc.getPages();
		for (int p = 0; p < pages.length; p++) {
			pm.setProgress((p * 100) / pages.length);
			ImRegion[] pageTables = pages[p].getRegions(ImRegion.TABLE_TYPE);
			if (pageTables.length == 0)
				continue;
			for (int t = 0; t < pageTables.length; t++)
				this.cleanupTableAnnotations(doc, pageTables[t]);
		}
		
		//	perform document annotation cleanup to deal with whatever duplicates we might have incurred
		doc.cleanupAnnotations();
	}
	
	private void cleanupTableAnnotations(ImDocument doc, ImRegion table) {
		
		//	get annotations lying inside table
		ImAnnotation[] annots = doc.getAnnotations(table.pageId);
		ArrayList annotList = new ArrayList();
		for (int a = 0; a < annots.length; a++) {
			if (!ImWord.TEXT_STREAM_TYPE_TABLE.equals(annots[a].getFirstWord().getTextStreamType()))
				continue;
			if (table.pageId != annots[a].getFirstWord().pageId)
				continue;
			if (table.pageId != annots[a].getLastWord().pageId)
				continue;
			if (!table.bounds.includes(annots[a].getFirstWord().bounds, true))
				continue;
			if (!table.bounds.includes(annots[a].getLastWord().bounds, true))
				continue;
			annotList.add(annots[a]);
		}
		if (annotList.isEmpty())
			return;
		
		//	get and sort table cells
		ImRegion[] cells = table.getRegions(ImRegion.TABLE_CELL_TYPE);
		if (cells.length == 0)
			return;
		Arrays.sort(cells, ImUtils.leftRightOrder);
		Arrays.sort(cells, ImUtils.topDownOrder);
		
		//	get table words
		ImWord[] words = table.getWords();
		if (words.length == 0)
			return;
		Arrays.sort(words, ImUtils.textStreamOrder);
		
		//	index cells by words
		Map wordsToCells = new HashMap();
		ImRegion wordCell = null;
		for (int w = 0; w < words.length; w++) {
			if ((wordCell != null) && (wordCell.bounds.includes(words[w].bounds, true)))
				wordsToCells.put(words[w], wordCell);
			else for (int c = 0; c < cells.length; c++)
				if (cells[c].bounds.includes(words[w].bounds, true)) {
					wordCell = cells[c];
					wordsToCells.put(words[w], wordCell);
					break;
				}
		}
		
		//	mark parts of original annotation in individual cells
		for (int a = 0; a < annotList.size(); a++)
			this.cleanupTableAnnotation(doc, ((ImAnnotation) annotList.get(a)), wordsToCells);
		
		//	perform document annotation cleanup to deal with whatever duplicates we might have incurred
		doc.cleanupAnnotations();
	}
	
	private void cleanupTableAnnotation(ImDocument doc, ImAnnotation annotation, Map wordsToCells) {
		
		//	single-cell annotation, nothing to chop
		if (wordsToCells.get(annotation.getFirstWord()) == wordsToCells.get(annotation.getLastWord()))
			return;
		
		//	get cleanup operation, and behave accordingly
		String annotTypeOperation = this.annotTypeOperations.getProperty(annotation.getType(), SPLIT_OPERATION);
		
		//	remove annotation (happens below)
		if (REMOVE_OPERATION.equals(annotTypeOperation)) {}
		
		//	cut at end of start cell
		else if (CUT_TO_START_OPERATION.equals(annotTypeOperation)) {
			ImRegion annotStartCell = ((ImRegion) wordsToCells.get(annotation.getFirstWord()));
			for (ImWord imw = annotation.getFirstWord(); imw != null; imw = imw.getNextWord())
				if (wordsToCells.get(imw) != annotStartCell) {
					annotation.setLastWord(imw.getPreviousWord());
					break;
				}
		}
		
		//	cut at start of end cell
		else if (CUT_TO_END_OPERATION.equals(annotTypeOperation)) {
			ImRegion annotEndCell = ((ImRegion) wordsToCells.get(annotation.getLastWord()));
			for (ImWord imw = annotation.getLastWord(); imw != null; imw = imw.getPreviousWord())
				if (wordsToCells.get(imw) != annotEndCell) {
					annotation.setFirstWord(imw.getNextWord());
					break;
				}
		}
		
		//	split up to individual cells
		else if (SPLIT_OPERATION.equals(annotTypeOperation)) {
			ImRegion annotCell = ((ImRegion) wordsToCells.get(annotation.getFirstWord()));
			ImAnnotation annot = doc.addAnnotation(annotation.getFirstWord(), annotation.getType());
			annot.copyAttributes(annotation);
			for (ImWord imw = annotation.getFirstWord(); imw != null; imw = imw.getNextWord()) {
				if (wordsToCells.get(imw) != annotCell) {
					annot.setLastWord(imw.getPreviousWord());
					annot = doc.addAnnotation(imw, annotation.getType());
					annot.copyAttributes(annotation);
					annotCell = ((ImRegion) wordsToCells.get(imw));
				}
				if (imw == annotation.getLastWord()) {
					annot.setLastWord(imw);
					break;
				}
			}
		}
		
		//	clean up original annotation
		doc.removeAnnotation(annotation);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, ImDocumentMarkupPanel idmp) {
		
		//	mark selection
		BoundingBox selectedBox = new BoundingBox(Math.min(start.x, end.x), Math.max(start.x, end.x), Math.min(start.y, end.y), Math.max(start.y, end.y));
		
		//	get selected words
		ImWord[] selectedWords = page.getWordsInside(selectedBox);
		
		//	shrink bounding box to words
		if (selectedWords.length != 0) {
			int tbLeft = selectedBox.right;
			int tbRight = selectedBox.left;
			int tbTop = selectedBox.bottom;
			int tbBottom = selectedBox.top;
			for (int w = 0; w < selectedWords.length; w++) {
				tbLeft = Math.min(tbLeft, selectedWords[w].bounds.left);
				tbRight = Math.max(tbRight, selectedWords[w].bounds.right);
				tbTop = Math.min(tbTop, selectedWords[w].bounds.top);
				tbBottom = Math.max(tbBottom, selectedWords[w].bounds.bottom);
			}
			selectedBox = new BoundingBox(tbLeft, tbRight, tbTop, tbBottom);
		}
		
		//	return actions if selection not empty
		return this.getActions(page, selectedWords, selectedBox, idmp);
	}
	
	private SelectionAction[] getActions(final ImPage page, final ImWord[] words, final BoundingBox wordsBox, final ImDocumentMarkupPanel idmp) {
		LinkedList actions = new LinkedList();
		
		//	get selected table
		final ImRegion[] tables = this.getRegionsIncluding(page, wordsBox, ImRegion.TABLE_TYPE, true);
		
		//	no table selected, offer marking selected words as a table and analyze table structure
		if ((tables.length == 0) && (words.length != 0))
			actions.add(new SelectionAction("markRegionTable", "Mark Table", "Mark selected words as a table and analyze table structure.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return markTable(page, words, wordsBox, invoker);
				}
			});
		
		//	one table selected, offer editing structure
		else if (tables.length == 1) {
			
			//	working on table rows
			if (idmp.areRegionsPainted(ImRegion.TABLE_ROW_TYPE) && (words.length != 0)) {
				final ImRegion[] selectedRows = this.getRegionsInside(page, wordsBox, ImRegion.TABLE_ROW_TYPE, true);
				final ImRegion[] partSelectedRows = this.getRegionsIncluding(page, wordsBox, ImRegion.TABLE_ROW_TYPE, true);
				
				//	if multiple table row regions selected, offer merging them, and cells along the way
				if (selectedRows.length > 1)
					actions.add(new SelectionAction("tableMergeRows", "Merge Table Rows", "Merge selected table rows.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return mergeTableRows(tables[0], selectedRows);
						}
					});
				
				//	if only part of one row selected, offer splitting row
				else if (partSelectedRows.length == 1) {
					actions.add(new SelectionAction("tableSplitRow", "Split Table Row", "Split selected table row.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return splitTableRow(tables[0], partSelectedRows[0], wordsBox);
						}
					});
					if (!tables[0].hasAttribute("rowsContinueFrom") && !tables[0].hasAttribute("rowsContinueIn"))
						actions.add(new SelectionAction("tableSplitRow", "Split Table Row to Lines", "Split selected table row into individual lines.") {
							public boolean performAction(ImDocumentMarkupPanel invoker) {
								return splitTableRowToLines(tables[0], partSelectedRows[0]);
							}
						});
				}
			}
			
			//	working on table columns
			if (idmp.areRegionsPainted(ImRegion.TABLE_COL_TYPE) && (words.length != 0)) {
				final ImRegion[] selectedCols = this.getRegionsInside(page, wordsBox, ImRegion.TABLE_COL_TYPE, true);
				final ImRegion[] partSelectedCols = this.getRegionsIncluding(page, wordsBox, ImRegion.TABLE_COL_TYPE, true);
				
				//	if multiple table column regions selected, offer merging them, and cells along the way
				if (selectedCols.length > 1)
					actions.add(new SelectionAction("tableMergeColumns", "Merge Table Columns", "Merge selected table columns.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return mergeTableCols(tables[0], selectedCols);
						}
					});
				
				//	if only part of one column selected, offer splitting column
				else if (partSelectedCols.length == 1)
					actions.add(new SelectionAction("tableSplitColumns", "Split Table Column", "Split selected table column.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return splitTableCol(tables[0], partSelectedCols[0], wordsBox);
						}
					});
			}
			
			//	update table structure after manual modifications
			actions.add(new SelectionAction("tableUpdate", "Update Table", "Update table to reflect manual modifications.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					ImRegion[][] tableCells = markTableCells(page, tables[0]);
					ImUtils.orderTableWords(tableCells);
					cleanupTableAnnotations(page.getDocument(), tables[0]);
					return true;
				}
			});
			
			//	offer assigning caption with second click
			actions.add(new TwoClickSelectionAction("assignCaptionTable", "Assign Caption", "Assign a caption to this table with a second click.") {
				private ImWord artificialStartWord = null;
				public boolean performAction(ImWord secondWord) {
					if (!ImWord.TEXT_STREAM_TYPE_CAPTION.equals(secondWord.getTextStreamType()))
						return false;
					
					//	find affected caption
					ImAnnotation[] wordAnnots = idmp.document.getAnnotationsSpanning(secondWord);
					ArrayList wordCaptions = new ArrayList(2);
					for (int a = 0; a < wordAnnots.length; a++) {
						if (ImAnnotation.CAPTION_TYPE.equals(wordAnnots[a].getType()))
							wordCaptions.add(wordAnnots[a]);
					}
					if (wordCaptions.size() != 1)
						return false;
					ImAnnotation wordCaption = ((ImAnnotation) wordCaptions.get(0));
					
					//	does this caption match?
					if (!wordCaption.getFirstWord().getString().toLowerCase().startsWith("tab"))
						return false;
					
					//	set attributes
					wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, tables[0].bounds.toString());
					wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + tables[0].pageId));
					wordCaption.setAttribute("targetIsTable");
					return true;
				}
				public ImWord getFirstWord() {
					if (this.artificialStartWord == null)
						this.artificialStartWord = new ImWord(tables[0].getDocument(), tables[0].pageId, tables[0].bounds, "TABLE");
					return this.artificialStartWord;
				}
				public String getActiveLabel() {
					return ("Click on a caption to assign it to the table at " + tables[0].bounds.toString() + " on page " + (tables[0].pageId + 1));
				}
			});
			
			//	offer exporting tables as CSV
			actions.add(new SelectionAction("tableCopySingle", "Copy Table Data", "Copy the data in the table to the clipboard.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Copy Table Data ...");
					JMenuItem mi;
					mi = new JMenuItem("- CSV (comma separated)");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							copyTableData(tables[0], ',');
						}
					});
					pm.add(mi);
					mi = new JMenuItem("- Excel (semicolon separated)");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							copyTableData(tables[0], ';');
						}
					});
					pm.add(mi);
					mi = new JMenuItem("- Text (tab separated)");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							copyTableData(tables[0], '\t');
						}
					});
					pm.add(mi);
					return pm;
				}
			});
			
			//	offer exporting grid of connected tables as CSV
			if (tables[0].hasAttribute("rowsContinueIn") || tables[0].hasAttribute("rowsContinueFrom") || tables[0].hasAttribute("colsContinueIn") || tables[0].hasAttribute("colsContinueFrom"))
				actions.add(new SelectionAction("tableCopyGrid", "Copy Table Grid Data", "Copy the data in the table and all connected tables to the clipboard.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return false;
					}
					public JMenuItem getMenuItem(ImDocumentMarkupPanel invoker) {
						JMenu pm = new JMenu("Copy Table Grid Data ...");
						JMenuItem mi;
						mi = new JMenuItem("- CSV (comma separated)");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								copyTableGridData(tables[0], ',');
							}
						});
						pm.add(mi);
						mi = new JMenuItem("- Excel (semicolon separated)");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								copyTableGridData(tables[0], ';');
							}
						});
						pm.add(mi);
						mi = new JMenuItem("- Text (tab separated)");
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								copyTableGridData(tables[0], '\t');
							}
						});
						pm.add(mi);
						return pm;
					}
				});
		}
		
		//	TODO_once_example_available if multiple table cell regions selected, offer merging them, setting colspan and rowspan
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private boolean mergeTableRows(ImRegion table, ImRegion[] mergeRows) {
		ImRegion[] tables = ImUtils.getRowConnectedTables(table);
		
		//	cut effort short in basic case
		if (tables.length == 1) {
			this.doMergeTableRows(table, mergeRows);
			return true;
		}
		
		//	compute merge area
		ImRegion[] tableRows = ImUtils.getTableRows(table);
		HashSet mergeRowSet = new HashSet(Arrays.asList(mergeRows));
		Arrays.sort(tableRows, ImUtils.topDownOrder);
		int firstMergeRowIndex = -1;
		for (int r = 0; r < tableRows.length; r++)
			if (mergeRowSet.contains(tableRows[r])) {
				firstMergeRowIndex = r;
				break;
			}
		int tableRowCount = tableRows.length;
		
		//	get corresponding rows in connected tables
		System.out.println("Collecting merge row sets ...");
		System.out.println(" - merge area is rows " + firstMergeRowIndex + " to " + (firstMergeRowIndex + mergeRows.length - 1));
		ImRegion[][] mergeTableRows = new ImRegion[tables.length][];
		for (int t = 0; t < tables.length; t++) {
			System.out.println(" - from " + tables[t].pageId + "." + tables[t].bounds);
			
			//	this one's clear
			if (tables[t] == table) {
				System.out.println(" --> original selection");
				mergeTableRows[t] = mergeRows;
				continue;
			}
			
			//	collect rows inside merge area 
			tableRows = ImUtils.getTableRows(tables[t]);
			if (tableRows.length != tableRowCount) {
				System.out.println(" ==> row count mismatch, merge not possible");
				return false;
			}
			Arrays.sort(tableRows, ImUtils.topDownOrder);
			mergeTableRows[t] = new ImRegion[mergeRows.length];
			System.arraycopy(tableRows, firstMergeRowIndex, mergeTableRows[t], 0, mergeRows.length);
		}
		System.out.println(" ==> found all merge row sets");
		
		//	perform actual merge
		for (int t = 0; t < tables.length; t++)
			this.doMergeTableRows(tables[t], mergeTableRows[t]);
		return true;
	}
	
	private void doMergeTableRows(ImRegion table, ImRegion[] mergeRows) {
		Arrays.sort(mergeRows, ImUtils.topDownOrder);
		new ImRegion(table.getPage(), ImLayoutObject.getAggregateBox(mergeRows), ImRegion.TABLE_ROW_TYPE);
		for (int r = 0; r < mergeRows.length; r++)
			table.getPage().removeRegion(mergeRows[r]);
		ImRegion[][] tableCells = markTableCells(table.getPage(), table);
		ImUtils.orderTableWords(tableCells);
		this.cleanupTableAnnotations(table.getDocument(), table);
	}
	
	private boolean splitTableRow(ImRegion table, ImRegion toSplitRow, BoundingBox splitBox) {
		ImRegion[] tables = ImUtils.getRowConnectedTables(table);
		ImWord[] toSplitRowWords = toSplitRow.getWords();
		
		//	compute relative split bounds
		int relAboveSplitBottom = 0;
		int relSplitTop = (splitBox.bottom - toSplitRow.bounds.top);
		int relSplitBottom = (splitBox.top - toSplitRow.bounds.top);
		int relBelowSplitTop = (toSplitRow.bounds.bottom - toSplitRow.bounds.top);
		
		//	order words above, inside, and below selection
		for (int w = 0; w < toSplitRowWords.length; w++) {
			if (toSplitRowWords[w].centerY < splitBox.top)
				relAboveSplitBottom = Math.max(relAboveSplitBottom, (toSplitRowWords[w].bounds.bottom - toSplitRow.bounds.top));
			else if (toSplitRowWords[w].centerY < splitBox.bottom) {
				relSplitTop = Math.min(relSplitTop, (toSplitRowWords[w].bounds.top - toSplitRow.bounds.top));
				relSplitBottom = Math.max(relSplitBottom, (toSplitRowWords[w].bounds.bottom - toSplitRow.bounds.top));
			}
			else relBelowSplitTop = Math.min(relBelowSplitTop, (toSplitRowWords[w].bounds.top - toSplitRow.bounds.top));
		}
		
		//	anything to split at all?
		int emptySplitRows = 0;
		if (relAboveSplitBottom == 0)
			emptySplitRows++;
		if (relSplitBottom <= relSplitTop)
			emptySplitRows++;
		if (relBelowSplitTop == (toSplitRow.bounds.bottom - toSplitRow.bounds.top))
			emptySplitRows++;
		if (emptySplitRows >= 2)
			return false;
		
		//	cut effort short in basic case
		if (tables.length == 1) {
			this.doSplitTableRow(table, toSplitRow, relAboveSplitBottom, relSplitTop, relSplitBottom, relBelowSplitTop);
			return true;
		}
		
		//	compute table-relative split area
		int relToSplitRowTop = (toSplitRow.bounds.top - table.bounds.top);
		int relToSplitRowBottom = (toSplitRow.bounds.bottom - table.bounds.top);
		
		//	get corresponding rows in connected tables
		System.out.println("Collecting to-split rows ...");
		System.out.println(" - relative split area is " + relToSplitRowTop + " to " + relToSplitRowBottom);
		ImRegion[] toSplitTableRows = new ImRegion[tables.length];
		for (int t = 0; t < tables.length; t++) {
			System.out.println(" - from " + tables[t].pageId + "." + tables[t].bounds);
			
			//	this one's clear
			if (tables[t] == table) {
				System.out.println(" --> original selection");
				toSplitTableRows[t] = toSplitRow;
				continue;
			}
			
			//	collect rows inside table-relative merge area
			ImRegion[] tableRows = ImUtils.getTableRows(tables[t]);
			for (int r = 0; r < tableRows.length; r++) {
				int relRowCenter = (((tableRows[r].bounds.top + tableRows[r].bounds.bottom) / 2) - tables[t].bounds.top);
				System.out.println("   - row " + tableRows[r].bounds + ", relative center is " + relRowCenter);
				if ((relToSplitRowTop < relRowCenter) && (relRowCenter < relToSplitRowBottom)) {
					System.out.println("   --> inside selection");
					if (toSplitTableRows[t] == null)
						toSplitTableRows[t] = tableRows[r];
					else {
						System.out.println(" ==> duplicate match, split not possible");
						return false;
					}
				}
				else System.out.println("   --> outside selection");
			}
		}
		System.out.println(" ==> found all to-split rows");
		
		//	perform split
		float relTopMiddleSplit = (((float) (relAboveSplitBottom + relSplitTop)) / (2 * (toSplitRow.bounds.bottom - toSplitRow.bounds.top)));
		float relMiddleBottomSplit = (((float) (relSplitBottom + relBelowSplitTop)) / (2 * (toSplitRow.bounds.bottom - toSplitRow.bounds.top)));
		for (int t = 0; t < tables.length; t++) {
			if (tables[t] == table)
				this.doSplitTableRow(tables[t], toSplitTableRows[t], relAboveSplitBottom, relSplitTop, relSplitBottom, relBelowSplitTop);
			else this.doSplitTableRow(tables[t], toSplitTableRows[t], relTopMiddleSplit, relMiddleBottomSplit);
		}
		return true;
	}
	
	private void doSplitTableRow(ImRegion table, ImRegion toSplitRow, float relTopMiddleSplit, float relMiddleBottomSplit) {
		
		//	compute relative split box
		int splitBoxTop = (((int) (relTopMiddleSplit * (toSplitRow.bounds.bottom - toSplitRow.bounds.top))) + toSplitRow.bounds.top);
		int splitBoxBottom = (((int) (relMiddleBottomSplit * (toSplitRow.bounds.bottom - toSplitRow.bounds.top))) + toSplitRow.bounds.top);
		
		//	compute relative split bounds
		int relAboveSplitBottom = 0;
		int relSplitTop = (splitBoxBottom - toSplitRow.bounds.top);
		int relSplitBottom = (splitBoxTop - toSplitRow.bounds.top);
		int relBelowSplitTop = (toSplitRow.bounds.bottom - toSplitRow.bounds.top);
		
		//	order words above, inside, and below selection
		ImWord[] toSplitRowWords = toSplitRow.getWords();
		for (int w = 0; w < toSplitRowWords.length; w++) {
			if (toSplitRowWords[w].centerY < splitBoxTop)
				relAboveSplitBottom = Math.max(relAboveSplitBottom, (toSplitRowWords[w].bounds.bottom - toSplitRow.bounds.top));
			else if (toSplitRowWords[w].centerY < splitBoxBottom) {
				relSplitTop = Math.min(relSplitTop, (toSplitRowWords[w].bounds.top - toSplitRow.bounds.top));
				relSplitBottom = Math.max(relSplitBottom, (toSplitRowWords[w].bounds.bottom - toSplitRow.bounds.top));
			}
			else relBelowSplitTop = Math.min(relBelowSplitTop, (toSplitRowWords[w].bounds.top - toSplitRow.bounds.top));
		}
		
		//	do split with case adjusted absolute numbers
		this.doSplitTableRow(table, toSplitRow, relAboveSplitBottom, relSplitTop, relSplitBottom, relBelowSplitTop);
	}
	
	private void doSplitTableRow(ImRegion table, ImRegion toSplitRow, int relAboveSplitBottom, int relSplitTop, int relSplitBottom, int relBelowSplitTop) {
		
		//	create two or three new rows
		if (0 < relAboveSplitBottom) {
			BoundingBox arBox = new BoundingBox(table.bounds.left, table.bounds.right, toSplitRow.bounds.top, (toSplitRow.bounds.top + relAboveSplitBottom));
			new ImRegion(table.getPage(), arBox, ImRegion.TABLE_ROW_TYPE);
		}
		if (relSplitTop < relSplitBottom) {
			BoundingBox irBox = new BoundingBox(table.bounds.left, table.bounds.right, (toSplitRow.bounds.top + relSplitTop), (toSplitRow.bounds.top + relSplitBottom));
			new ImRegion(table.getPage(), irBox, ImRegion.TABLE_ROW_TYPE);
		}
		if (relBelowSplitTop < (toSplitRow.bounds.bottom - toSplitRow.bounds.top)) {
			BoundingBox brBox = new BoundingBox(table.bounds.left, table.bounds.right, (toSplitRow.bounds.top + relBelowSplitTop), toSplitRow.bounds.bottom);
			new ImRegion(table.getPage(), brBox, ImRegion.TABLE_ROW_TYPE);
		}
		
		//	remove selected row
		table.getPage().removeRegion(toSplitRow);
		
		//	clean up table structure
		ImRegion[][] tableCells = markTableCells(table.getPage(), table);
		ImUtils.orderTableWords(tableCells);
		this.cleanupTableAnnotations(table.getDocument(), table);
	}
	
	private boolean splitTableRowToLines(ImRegion table, ImRegion toSplitRow) {
		
		//	get table row words
		ImWord[] toSplitRowWords = toSplitRow.getWords();
		ImUtils.sortLeftRightTopDown(toSplitRowWords);
		
		//	sort words into rows
		int rowStartWordIndex = 0;
		for (int w = 0; w < toSplitRowWords.length; w++)
			if (((w+1) == toSplitRowWords.length) || (toSplitRowWords[w+1].centerY > toSplitRowWords[w].bounds.bottom)) {
				BoundingBox rowWordBox = ImLayoutObject.getAggregateBox(toSplitRowWords, rowStartWordIndex, (w+1));
				new ImRegion(table.getPage(), new BoundingBox(table.bounds.left, table.bounds.right, rowWordBox.top, rowWordBox.bottom), ImRegion.TABLE_ROW_TYPE);
				rowStartWordIndex = (w+1);
			}
		
		//	remove selected row
		table.getPage().removeRegion(toSplitRow);
		
		//	clean up table structure
		ImRegion[][] tableCells = markTableCells(table.getPage(), table);
		ImUtils.orderTableWords(tableCells);
		this.cleanupTableAnnotations(table.getDocument(), table);
		
		//	finally ...
		return true;
	}
	
	private boolean mergeTableCols(ImRegion table, ImRegion[] mergeCols) {
		ImRegion[] tables = ImUtils.getColumnConnectedTables(table);
		
		//	cut effort short in basic case
		if (tables.length == 1) {
			this.doMergeTableCols(table, mergeCols);
			return true;
		}
		
		//	compute merge area
		ImRegion[] tableCols = ImUtils.getTableColumns(table);
		Arrays.sort(tableCols, ImUtils.leftRightOrder);
		HashSet mergeRowSet = new HashSet(Arrays.asList(mergeCols));
		int firstMergeColIndex = -1;
		for (int c = 0; c < tableCols.length; c++)
			if (mergeRowSet.contains(tableCols[c])) {
				firstMergeColIndex = c;
				break;
			}
		int tableColCount = tableCols.length;
		
		//	get corresponding rows in connected tables
		System.out.println("Collecting merge column sets ...");
		System.out.println(" - merge area is columns " + firstMergeColIndex + " to " + (firstMergeColIndex + mergeCols.length - 1));
		ImRegion[][] mergeTableCols = new ImRegion[tables.length][];
		for (int t = 0; t < tables.length; t++) {
			System.out.println(" - from " + tables[t].pageId + "." + tables[t].bounds);
			
			//	this one's clear
			if (tables[t] == table) {
				System.out.println(" --> original selection");
				mergeTableCols[t] = mergeCols;
				continue;
			}
			
			//	collect columns inside merge area 
			tableCols = ImUtils.getTableColumns(tables[t]);
			if (tableCols.length != tableColCount) {
				System.out.println(" ==> column count mismatch, merge not possible");
				return false;
			}
			Arrays.sort(tableCols, ImUtils.leftRightOrder);
			mergeTableCols[t] = new ImRegion[mergeCols.length];
			System.arraycopy(tableCols, firstMergeColIndex, mergeTableCols[t], 0, mergeCols.length);
		}
		System.out.println(" ==> found all merge column sets");
		
		//	perform actual merge
		for (int t = 0; t < tables.length; t++)
			this.doMergeTableCols(tables[t], mergeTableCols[t]);
		return true;
	}
	
	private void doMergeTableCols(ImRegion table, ImRegion[] mergeCols) {
		Arrays.sort(mergeCols, ImUtils.leftRightOrder);
		new ImRegion(table.getPage(), ImLayoutObject.getAggregateBox(mergeCols), ImRegion.TABLE_COL_TYPE);
		for (int c = 0; c < mergeCols.length; c++)
			table.getPage().removeRegion(mergeCols[c]);
		ImRegion[][] tableCells = markTableCells(table.getPage(), table);
		ImUtils.orderTableWords(tableCells);
		this.cleanupTableAnnotations(table.getDocument(), table);
	}
	
	private boolean splitTableCol(ImRegion table, ImRegion toSplitCol, BoundingBox splitBox) {
		ImRegion[] tables = ImUtils.getColumnConnectedTables(table);
		ImWord[] toSplitColWords = toSplitCol.getWords();
		
		//	compute relative split bounds
		int relLeftSplitRight = 0;
		int relSplitLeft = (splitBox.right - toSplitCol.bounds.left);
		int relSplitRight = (splitBox.left - toSplitCol.bounds.left);
		int relRightSplitLeft = (toSplitCol.bounds.right - toSplitCol.bounds.left);
		
		//	order words left of, inside, and right of selection
		for (int w = 0; w < toSplitColWords.length; w++) {
			if (toSplitColWords[w].centerX < splitBox.left)
				relLeftSplitRight = Math.max(relLeftSplitRight, (toSplitColWords[w].bounds.right - toSplitCol.bounds.left));
			else if (toSplitColWords[w].centerX < splitBox.right) {
				relSplitLeft = Math.min(relSplitLeft, (toSplitColWords[w].bounds.left - toSplitCol.bounds.left));
				relSplitRight = Math.max(relSplitRight, (toSplitColWords[w].bounds.right - toSplitCol.bounds.left));
			}
			else relRightSplitLeft = Math.min(relRightSplitLeft, (toSplitColWords[w].bounds.left - toSplitCol.bounds.left));
		}
		
		//	anything to split at all?
		int emptySplitCols = 0;
		if (relLeftSplitRight == 0)
			emptySplitCols++;
		if (relSplitRight <= relSplitLeft)
			emptySplitCols++;
		if (relRightSplitLeft == (toSplitCol.bounds.right - toSplitCol.bounds.left))
			emptySplitCols++;
		if (emptySplitCols >= 2)
			return false;
		
		//	cut effort short in basic case
		if (tables.length == 1) {
			this.doSplitTableCol(table, toSplitCol, relLeftSplitRight, relSplitLeft, relSplitRight, relRightSplitLeft);
			return true;
		}
		
		//	compute table-relative split area
		int relToSplitColLeft = (toSplitCol.bounds.left - table.bounds.left);
		int relToSplitColRight = (toSplitCol.bounds.right - table.bounds.left);
		
		//	get corresponding columns in connected tables
		System.out.println("Collecting to-split columns ...");
		System.out.println(" - relative split area is " + relToSplitColLeft + " to " + relToSplitColRight);
		ImRegion[] toSplitTableCols = new ImRegion[tables.length];
		for (int t = 0; t < tables.length; t++) {
			System.out.println(" - from " + tables[t].pageId + "." + tables[t].bounds);
			
			//	this one's clear
			if (tables[t] == table) {
				System.out.println(" --> original selection");
				toSplitTableCols[t] = toSplitCol;
				continue;
			}
			
			//	collect rows inside table-relative merge area
			ImRegion[] tableCols = ImUtils.getTableColumns(tables[t]);
			for (int c = 0; c < tableCols.length; c++) {
				int relColCenter = (((tableCols[c].bounds.left + tableCols[c].bounds.right) / 2) - tables[t].bounds.left);
				System.out.println("   - row " + tableCols[c].bounds + ", relative center is " + relColCenter);
				if ((relToSplitColLeft < relColCenter) && (relColCenter < relToSplitColRight)) {
					System.out.println("   --> inside selection");
					if (toSplitTableCols[t] == null)
						toSplitTableCols[t] = tableCols[c];
					else {
						System.out.println(" ==> duplicate match, split not possible");
						return false;
					}
				}
				else System.out.println("   --> outside selection");
			}
		}
		System.out.println(" ==> found all to-split columns");
		
		//	perform split
		float relLeftMiddleSplit = (((float) (relLeftSplitRight + relSplitLeft)) / (2 * (toSplitCol.bounds.right - toSplitCol.bounds.left)));
		float relMiddleRightSplit = (((float) (relSplitRight + relRightSplitLeft)) / (2 * (toSplitCol.bounds.right - toSplitCol.bounds.left)));
		for (int t = 0; t < tables.length; t++) {
			if (tables[t] == table)
				this.doSplitTableCol(tables[t], toSplitTableCols[t], relLeftSplitRight, relSplitLeft, relSplitRight, relRightSplitLeft);
			else this.doSplitTableCol(tables[t], toSplitTableCols[t], relLeftMiddleSplit, relMiddleRightSplit);
		}
		return true;
	}
	
	private void doSplitTableCol(ImRegion table, ImRegion toSplitCol, float relLeftMiddleSplit, float relMiddleRightSplit) {
		
		//	compute relative split box
		int splitBoxLeft = (((int) (relLeftMiddleSplit * (toSplitCol.bounds.right - toSplitCol.bounds.left))) + toSplitCol.bounds.left);
		int splitBoxRight = (((int) (relMiddleRightSplit * (toSplitCol.bounds.right - toSplitCol.bounds.left))) + toSplitCol.bounds.left);
		
		//	compute relative split bounds
		int relLeftSplitRight = 0;
		int relSplitLeft = (splitBoxRight - toSplitCol.bounds.left);
		int relSplitRight = (splitBoxLeft - toSplitCol.bounds.left);
		int relRightSplitLeft = (toSplitCol.bounds.right - toSplitCol.bounds.left);
		
		//	order words left of, inside, and right of selection
		ImWord[] toSplitColWords = toSplitCol.getWords();
		for (int w = 0; w < toSplitColWords.length; w++) {
			if (toSplitColWords[w].centerX < splitBoxLeft)
				relLeftSplitRight = Math.max(relLeftSplitRight, (toSplitColWords[w].bounds.right - toSplitCol.bounds.left));
			else if (toSplitColWords[w].centerX < splitBoxRight) {
				relSplitLeft = Math.min(relSplitLeft, (toSplitColWords[w].bounds.left - toSplitCol.bounds.left));
				relSplitRight = Math.max(relSplitRight, (toSplitColWords[w].bounds.right - toSplitCol.bounds.left));
			}
			else relRightSplitLeft = Math.min(relRightSplitLeft, (toSplitColWords[w].bounds.left - toSplitCol.bounds.left));
		}
		
		//	do split with case adjusted absolute numbers
		this.doSplitTableCol(table, toSplitCol, relLeftSplitRight, relSplitLeft, relSplitRight, relRightSplitLeft);
	}
	
	private void doSplitTableCol(ImRegion table, ImRegion toSplitCol, int relLeftSplitRight, int relSplitLeft, int relSplitRight, int relRightSplitLeft) {
		
		//	create two or three new columns
		if (0 < relLeftSplitRight) {
			BoundingBox arBox = new BoundingBox(toSplitCol.bounds.left, (toSplitCol.bounds.left + relLeftSplitRight), table.bounds.top, table.bounds.bottom);
			new ImRegion(table.getPage(), arBox, ImRegion.TABLE_COL_TYPE);
		}
		if (relSplitLeft < relSplitRight) {
			BoundingBox irBox = new BoundingBox((toSplitCol.bounds.left + relSplitLeft), (toSplitCol.bounds.left + relSplitRight), table.bounds.top, table.bounds.bottom);
			new ImRegion(table.getPage(), irBox, ImRegion.TABLE_COL_TYPE);
		}
		if (relRightSplitLeft < (toSplitCol.bounds.right - toSplitCol.bounds.left)) {
			BoundingBox brBox = new BoundingBox((toSplitCol.bounds.left + relRightSplitLeft), toSplitCol.bounds.right, table.bounds.top, table.bounds.bottom);
			new ImRegion(table.getPage(), brBox, ImRegion.TABLE_COL_TYPE);
		}
		
		//	remove selected column
		table.getPage().removeRegion(toSplitCol);
		
		//	clean up table structure
		ImRegion[][] tableCells = markTableCells(table.getPage(), table);
		ImUtils.orderTableWords(tableCells);
		this.cleanupTableAnnotations(table.getDocument(), table);
	}
	
	private boolean markTable(ImPage page, ImWord[] words, BoundingBox tableBox, ImDocumentMarkupPanel idmp) {
		
		//	wrap region around words
		ImRegion tableRegion = new ImRegion(page.getDocument(), page.pageId, tableBox, ImRegion.TABLE_TYPE);
		
		//	get rows and columns
		ImRegion[] tableRows = ImUtils.getTableRows(tableRegion);
		if (tableRows == null)
			return false;
		ImRegion[] tableCols = ImUtils.getTableColumns(tableRegion);
		if (tableCols == null)
			return false;
		
		//	mark cells as intersection of columns and rows
		ImRegion[][] tableCells = ImUtils.getTableCells(tableRegion, tableRows, tableCols);
		if (tableCells == null)
			return false;
		
		//	add regions to page
		page.addRegion(tableRegion);
		for (int r = 0; r < tableRows.length; r++) {
			if (tableRows[r].getPage() == null)
				page.addRegion(tableRows[r]);
		}
		for (int c = 0; c < tableCols.length; c++) {
			if (tableCols[c].getPage() == null)
				page.addRegion(tableCols[c]);
		}
		for (int r = 0; r < tableCells.length; r++)
			for (int c = 0; c < tableCells[r].length; c++) {
				if (tableCells[r][c].getPage() == null)
					page.addRegion(tableCells[r][c]);
			}
		
		//	cut table out of main text
		ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_TABLE, null);
		
		//	flatten out table content
		for (int w = 0; w < words.length; w++) {
			if (words[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
				words[w].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
		}
		
		//	remove all regions not related to table
		ImRegion[] tableRegions = page.getRegionsInside(tableBox, false);
		for (int r = 0; r < tableRegions.length; r++) {
			if (ImRegion.LINE_ANNOTATION_TYPE.equals(tableRegions[r].getType()))
				page.removeRegion(tableRegions[r]);
			else if (ImRegion.PARAGRAPH_TYPE.equals(tableRegions[r].getType()))
				page.removeRegion(tableRegions[r]);
			else if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(tableRegions[r].getType()))
				page.removeRegion(tableRegions[r]);
			else if (ImRegion.COLUMN_ANNOTATION_TYPE.equals(tableRegions[r].getType()))
				page.removeRegion(tableRegions[r]);
			else if (ImRegion.REGION_ANNOTATION_TYPE.equals(tableRegions[r].getType()))
				page.removeRegion(tableRegions[r]);
		}
		
		//	order words from each cell as a text stream
		ImUtils.orderTableWords(tableCells);
		this.cleanupTableAnnotations(page.getDocument(), tableRegion);
		
		//	show regions in invoker
		idmp.setRegionsPainted(ImRegion.TABLE_TYPE, true);
		idmp.setRegionsPainted(ImRegion.TABLE_ROW_TYPE, true);
		idmp.setRegionsPainted(ImRegion.TABLE_COL_TYPE, true);
		idmp.setRegionsPainted(ImRegion.TABLE_CELL_TYPE, true);
		
		//	finally ...
		return true;
	}
	
	private ImRegion[][] markTableCells(ImPage page, ImRegion table) {
		
		//	get cells
		ImRegion[][] cells = ImUtils.getTableCells(table, null, null);
		
		//	make sure cells are attached to page
		for (int r = 0; r < cells.length; r++)
			for (int c = 0; c < cells[r].length; c++) {
				if (cells[r][c].getPage() == null)
					page.addRegion(cells[r][c]);
			}
		
		//	finally ...
		return cells;
	}
	
	private ImRegion[] getRegionsInside(ImPage page, BoundingBox box, String type, boolean fuzzy) {
		ImRegion[] regions = page.getRegionsInside(box, fuzzy);
		ArrayList regionList = new ArrayList();
		for (int r = 0; r < regions.length; r++) {
			if (type.equals(regions[r].getType()))
				regionList.add(regions[r]);
		}
		return ((ImRegion[]) regionList.toArray(new ImRegion[regionList.size()]));
	}
	
	private ImRegion[] getRegionsIncluding(ImPage page, BoundingBox box, String type, boolean fuzzy) {
		ImRegion[] regions = page.getRegionsIncluding(box, fuzzy);
		ArrayList regionList = new ArrayList();
		for (int r = 0; r < regions.length; r++) {
			if (type.equals(regions[r].getType()))
				regionList.add(regions[r]);
		}
		return ((ImRegion[]) regionList.toArray(new ImRegion[regionList.size()]));
	}
	
	private void copyTableData(ImRegion table, char separator) {
		ImUtils.copy(new StringSelection(ImUtils.getTableData(table, separator)));
	}
	
	private void copyTableGridData(ImRegion table, char separator) {
		ImUtils.copy(new StringSelection(ImUtils.getTableGridData(table, separator)));
	}
}