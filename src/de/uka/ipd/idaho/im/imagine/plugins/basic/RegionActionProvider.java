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
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImSupplement;
import de.uka.ipd.idaho.im.ImSupplement.Figure;
import de.uka.ipd.idaho.im.ImSupplement.Scan;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.Imaging;
import de.uka.ipd.idaho.im.analysis.Imaging.AnalysisImage;
import de.uka.ipd.idaho.im.analysis.Imaging.ImagePartRectangle;
import de.uka.ipd.idaho.im.analysis.PageAnalysis;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.TwoClickSelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.im.util.ImUtils.StringPair;

/**
 * This class provides basic actions for handling regions.
 * 
 * @author sautter
 */
public class RegionActionProvider extends AbstractSelectionActionProvider implements ImageMarkupToolProvider, LiteratureConstants, ReactionProvider {
	
	private static final String REGION_CONVERTER_IMT_NAME = "ConvertRegions";
	private static final String REGION_RETYPER_IMT_NAME = "RetypeRegions";
	private static final String REGION_REMOVER_IMT_NAME = "RemoveRegions";
	
	private ImageMarkupTool regionConverter = new RegionConverter();
	private ImageMarkupTool regionRetyper = new RegionRetyper();
	private ImageMarkupTool regionRemover = new RegionRemover();
	
	/** public zero-argument constructor for class loading */
	public RegionActionProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Region Actions";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getEditMenuItemNames()
	 */
	public String[] getEditMenuItemNames() {
		String[] emins = {REGION_CONVERTER_IMT_NAME, REGION_RETYPER_IMT_NAME, REGION_REMOVER_IMT_NAME};
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
		if (REGION_CONVERTER_IMT_NAME.equals(name))
			return this.regionConverter;
		else if (REGION_RETYPER_IMT_NAME.equals(name))
			return this.regionRetyper;
		else if (REGION_REMOVER_IMT_NAME.equals(name))
			return this.regionRemover;
		else return null;
	}
	
	private abstract class RegionMarkupTool implements ImageMarkupTool {
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	get region types
			ImPage[] pages = doc.getPages();
			TreeSet allRegionTypes = new TreeSet();
			TreeSet paintedRegionTypes = new TreeSet();
			for (int p = 0; p < pages.length; p++) {
				String[] pageRegionTypes = pages[p].getRegionTypes();
				for (int t = 0; t < pageRegionTypes.length; t++) {
					allRegionTypes.add(pageRegionTypes[t]);
					if (idmp.areRegionsPainted(pageRegionTypes[t]))
						paintedRegionTypes.add(pageRegionTypes[t]);
				}
			}
			
			//	nothing to work with
			if (allRegionTypes.isEmpty()) {
				DialogFactory.alert("There are no regions in this document.", "No Regions", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			//	get region types
			String[] regionTypes = (paintedRegionTypes.isEmpty() ? ((String[]) allRegionTypes.toArray(new String[allRegionTypes.size()])) : ((String[]) paintedRegionTypes.toArray(new String[paintedRegionTypes.size()])));
			String regionType = ImUtils.promptForObjectType("Select Region Type", "Select type of regions", regionTypes, regionTypes[0], false);
			if (regionType == null)
				return;
			
			//	process regions of selected type
			this.processRegions(doc, pages, regionType);
		}
		abstract void processRegions(ImDocument doc, ImPage[] pages, String regionType);
	}
	
	private class RegionConverter extends RegionMarkupTool {
		public String getLabel() {
			return "Regions To Annotations";
		}
		public String getTooltip() {
			return "Convert regions into annotations";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		void processRegions(ImDocument doc, ImPage[] pages, String regionType) {
			for (int p = 0; p < pages.length; p++) {
				ImRegion[] pageRegions = pages[p].getRegions(regionType);
				for (int r = 0; r < pageRegions.length; r++) {
					ImWord[] regionWords = pageRegions[r].getWords();
					if (regionWords.length == 0)
						continue;
					Arrays.sort(regionWords, ImUtils.textStreamOrder);
					
					ImWord firstWord = regionWords[0];
					ImWord lastWord = regionWords[0];
					for (int w = 1; w < regionWords.length; w++) {
						if (firstWord.getTextStreamId().equals(regionWords[w].getTextStreamId()))
							lastWord = regionWords[w];
						else {
							ImAnnotation regionAnnot = doc.addAnnotation(firstWord, lastWord, regionType);
							regionAnnot.copyAttributes(pageRegions[r]);
							firstWord = regionWords[w];
							lastWord = regionWords[w];
						}
					}
					ImAnnotation regionAnnot = doc.addAnnotation(firstWord, lastWord, regionType);
					regionAnnot.copyAttributes(pageRegions[r]);
					
					pages[p].removeRegion(pageRegions[r]);
				}
			}
		}
	}
	
	private class RegionRetyper implements ImageMarkupTool {
		public String getLabel() {
			return "Change Region Type";
		}
		public String getTooltip() {
			return "Replace a region type with another one";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			
			//	get region types
			ImPage[] pages = doc.getPages();
			TreeSet allRegionTypes = new TreeSet();
			TreeSet paintedRegionTypes = new TreeSet();
			for (int p = 0; p < pages.length; p++) {
				String[] pageRegionTypes = pages[p].getRegionTypes();
				for (int t = 0; t < pageRegionTypes.length; t++) {
					allRegionTypes.add(pageRegionTypes[t]);
					if (idmp.areRegionsPainted(pageRegionTypes[t]))
						paintedRegionTypes.add(pageRegionTypes[t]);
				}
			}
			
			//	nothing to work with
			if (allRegionTypes.isEmpty()) {
				DialogFactory.alert("There are no regions in this document.", "No Regions", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			
			//	get region types
			String[] regionTypes = (paintedRegionTypes.isEmpty() ? ((String[]) allRegionTypes.toArray(new String[allRegionTypes.size()])) : ((String[]) paintedRegionTypes.toArray(new String[paintedRegionTypes.size()])));
			StringPair regionTypeChange = ImUtils.promptForObjectTypeChange("Select Region Type", "Select type of regions to change", "Select or enter new region type", regionTypes, regionTypes[0], true);
			if (regionTypeChange == null)
				return;
			
			//	process regions of selected type
			for (int p = 0; p < pages.length; p++) {
				ImRegion[] pageRegions = pages[p].getRegions(regionTypeChange.strOld);
				for (int r = 0; r < pageRegions.length; r++)
					pageRegions[r].setType(regionTypeChange.strNew);
			}
		}
	}
	
	private class RegionRemover extends RegionMarkupTool {
		public String getLabel() {
			return "Remove Regions";
		}
		public String getTooltip() {
			return "Remove regions from document";
		}
		public String getHelpText() {
			return null; // for now ...
		}
		void processRegions(ImDocument doc, ImPage[] pages, String regionType) {
			for (int p = 0; p < pages.length; p++) {
				ImRegion[] pageRegions = pages[p].getRegions(regionType);
				for (int r = 0; r < pageRegions.length; r++)
					pages[p].removeRegion(pageRegions[r]);
			}
		}
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
		
		//	we're only interested in paragraphs
		if (!ImRegion.PARAGRAPH_TYPE.equals(region.getType()))
			return;
		
		//	get words
		ImWord[] pWords = region.getWords();
		if (pWords.length == 0)
			return;
		
		//	remove artifacts and deleted words
		ArrayList pWordList = new ArrayList();
		for (int w = 0; w < pWords.length; w++) {
			if (!ImWord.TEXT_STREAM_TYPE_ARTIFACT.equals(pWords[w].getTextStreamType()) && !ImWord.TEXT_STREAM_TYPE_DELETED.equals(pWords[w].getTextStreamType()))
				pWordList.add(pWords[w]);
		}
		if (pWordList.isEmpty())
			return;
		if (pWordList.size() < pWords.length)
			pWords = ((ImWord[]) pWordList.toArray(new ImWord[pWordList.size()]));
		
		//	check text stream order
		ImUtils.orderStream(pWords, ImUtils.leftRightTopDownOrder);
		
		//	if predecessor of first word is further up on same page, mark logical paragraph break if nothing in between
		ImWord startWordPrev = pWords[0].getPreviousWord();
		if ((startWordPrev != null) && (startWordPrev.pageId == pWords[0].pageId) && (startWordPrev.centerY < pWords[0].bounds.top) && (startWordPrev.getNextRelation() != ImWord.NEXT_RELATION_PARAGRAPH_END)) {
			BoundingBox between = new BoundingBox(region.bounds.left, region.bounds.right, startWordPrev.bounds.bottom, pWords[0].bounds.top);
			ImWord[] betweenWords = region.getPage().getWordsInside(between);
			if (betweenWords.length == 0)
				startWordPrev.setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
		}
		
		//	if successor of last word is further down on same page, mark logical paragraph break if nothing in between
		ImWord endWordNext = pWords[pWords.length-1].getNextWord();
		if ((endWordNext != null) && (endWordNext.pageId == pWords[pWords.length-1].pageId) && (endWordNext.centerY > pWords[pWords.length-1].bounds.bottom) && (pWords[pWords.length-1].getNextRelation() != ImWord.NEXT_RELATION_PARAGRAPH_END)) {
			BoundingBox between = new BoundingBox(region.bounds.left, region.bounds.right, pWords[pWords.length-1].bounds.bottom, endWordNext.bounds.top);
			ImWord[] betweenWords = region.getPage().getWordsInside(between);
			if (betweenWords.length == 0)
				pWords[pWords.length-1].setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#regionRemoved(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionRemoved(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationAdded(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationAdded(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationRemoved(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationRemoved(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, final ImPage page, final ImDocumentMarkupPanel idmp) {
		
		//	mark selection
		BoundingBox selectedBox = new BoundingBox(Math.min(start.x, end.x), Math.max(start.x, end.x), Math.min(start.y, end.y), Math.max(start.y, end.y));
		
		//	get selected and context regions
		ImRegion[] pageRegions = page.getRegions();
		LinkedList selectedRegionList = new LinkedList();
		LinkedList contextRegionList = new LinkedList();
		for (int r = 0; r < pageRegions.length; r++) {
			if (!idmp.areRegionsPainted(pageRegions[r].getType()))
				continue;
			if (pageRegions[r].bounds.includes(selectedBox, true))
				contextRegionList.add(pageRegions[r]);
			if (pageRegions[r].bounds.right < selectedBox.left)
				continue;
			if (selectedBox.right < pageRegions[r].bounds.left)
				continue;
			if (pageRegions[r].bounds.bottom < selectedBox.top)
				continue;
			if (selectedBox.bottom < pageRegions[r].bounds.top)
				continue;
			selectedRegionList.add(pageRegions[r]);
		}
		final ImRegion[] selectedRegions = ((ImRegion[]) selectedRegionList.toArray(new ImRegion[selectedRegionList.size()]));
		final ImRegion[] contextRegions = ((ImRegion[]) contextRegionList.toArray(new ImRegion[contextRegionList.size()]));
		
		//	index selected regions by type for group removal
		final TreeMap selectedRegionsByType = new TreeMap();
		final TreeMap multiSelectedRegionsByType = new TreeMap();
		for (int r = 0; r < selectedRegions.length; r++) {
			LinkedList typeRegions = ((LinkedList) selectedRegionsByType.get(selectedRegions[r].getType()));
			if (typeRegions == null) {
				typeRegions = new LinkedList();
				selectedRegionsByType.put(selectedRegions[r].getType(), typeRegions);
			}
			typeRegions.add(selectedRegions[r]);
			if (typeRegions.size() > 1)
				multiSelectedRegionsByType.put(selectedRegions[r].getType(), typeRegions);
		}
		
		//	index context regions by type
		final TreeMap contextRegionsByType = new TreeMap();
		for (int r = 0; r < contextRegions.length; r++) {
			LinkedList typeRegions = ((LinkedList) contextRegionsByType.get(contextRegions[r].getType()));
			if (typeRegions == null) {
				typeRegions = new LinkedList();
				contextRegionsByType.put(contextRegions[r].getType(), typeRegions);
			}
			typeRegions.add(contextRegions[r]);
		}
		
		//	get selected words
		ImWord[] selectedWords = page.getWordsInside(selectedBox);
		
		//	collect actions
		LinkedList actions = new LinkedList();
		
		//	get region types
		final String[] regionTypes = page.getRegionTypes();
		final TreeSet paintedRegionTypes = new TreeSet();
		for (int t = 0; t < regionTypes.length; t++) {
			if (idmp.areRegionsPainted(regionTypes[t]))
				paintedRegionTypes.add(regionTypes[t]);
		}
		
		//	get bounding box surrounding selected words
		final BoundingBox selectedBounds;
		if (selectedWords.length == 0) {
			int sLeft = Math.max(page.bounds.left, selectedBox.left);
			int sRight = Math.min(page.bounds.right, selectedBox.right);
			int sTop = Math.max(page.bounds.top, selectedBox.top);
			int sBottom = Math.min(page.bounds.bottom, selectedBox.bottom);
			selectedBounds = new BoundingBox(sLeft, sRight, sTop, sBottom);
		}
		else {
			int sLeft = page.bounds.right;
			int sRight = page.bounds.left;
			int sTop = page.bounds.bottom;
			int sBottom = page.bounds.top;
			for (int w = 0; w < selectedWords.length; w++) {
				sLeft = Math.min(sLeft, selectedWords[w].bounds.left);
				sRight = Math.max(sRight, selectedWords[w].bounds.right);
				sTop = Math.min(sTop, selectedWords[w].bounds.top);
				sBottom = Math.max(sBottom, selectedWords[w].bounds.bottom);
			}
			selectedBounds = new BoundingBox(sLeft, sRight, sTop, sBottom);
		}
		
		//	mark region
		if (paintedRegionTypes.isEmpty())
			actions.add(new SelectionAction("markRegion", "Mark Region", ("Mark a region from the selection")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					String regionType = ImUtils.promptForObjectType("Enter Region Type", "Enter or select type of region to create", regionTypes, null, true);
					if (regionType == null)
						return false;
					ImRegion region = new ImRegion(page, selectedBounds, regionType);
					if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(regionType))
						ensureBlockStructure(region);
					idmp.setRegionsPainted(regionType, true);
					return true;
				}
			});
		else actions.add(new SelectionAction("markRegion", "Mark Region", ("Mark a region from the selection")) {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				return false;
			}
			public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
				JMenu pm = new JMenu("Mark Region");
				JMenuItem mi = new JMenuItem("Mark Region ...");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						String regionType = ImUtils.promptForObjectType("Enter Region Type", "Enter or select type of region to create", regionTypes, null, true);
						if (regionType != null) {
							invoker.beginAtomicAction("Mark '" + regionType + "' Region");
							ImRegion region = new ImRegion(page, selectedBounds, regionType);
							if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(regionType))
								ensureBlockStructure(region);
							invoker.endAtomicAction();
							invoker.setRegionsPainted(regionType, true);
							invoker.validate();
							invoker.repaint();
						}
					}
				});
				pm.add(mi);
				for (Iterator rtit = paintedRegionTypes.iterator(); rtit.hasNext();) {
					final String regionType = ((String) rtit.next());
					mi = new JMenuItem("- " + regionType);
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							invoker.beginAtomicAction("Mark '" + regionType + "' Region");
							ImRegion region = new ImRegion(page, selectedBounds, regionType);
							if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(regionType))
								ensureBlockStructure(region);
							invoker.endAtomicAction();
							invoker.setRegionsPainted(regionType, true);
							invoker.validate();
							invoker.repaint();
						}
					});
					pm.add(mi);
				}
				return pm;
			}
		});
		
		//	no words selected
		if (selectedWords.length == 0) {
			
			//	no region selected, either
			if (selectedRegions.length == 0) {
				actions.add(new SelectionAction("editAttributesPage", ("Edit Page Attributes"), ("Edit attributes of page.")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						idmp.editAttributes(page, PAGE_TYPE, "");
						return true;
					}
				});
				if (!idmp.documentBornDigital)
					actions.add(new SelectionAction("editPage", ("Edit Page Image & Words"), ("Edit page image and words recognized in page.")) {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return idmp.editPage(page.pageId);
						}
					});
			}
			
			//	offer marking image (case with artifact words comes from text block actions)
			actions.add(new SelectionAction("annotateRegionImage", "Mark Image", "Mark selected region as an image.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					
					//	shrink selection
					PageImage pi = page.getImage();
					AnalysisImage ai = Imaging.wrapImage(pi.image, null);
					ImagePartRectangle ipr = Imaging.getContentBox(ai);
					ImagePartRectangle selectedIpr = ipr.getSubRectangle(selectedBounds.left, selectedBounds.right, selectedBounds.top, selectedBounds.bottom);
					selectedIpr = Imaging.narrowLeftAndRight(selectedIpr);
					selectedIpr = Imaging.narrowTopAndBottom(selectedIpr);
					
					//	anything selected at all (too much effort to check in advance)
					if (Imaging.computeAverageBrightness(selectedIpr) > 125) {
						DialogFactory.alert("The selection appears to be completely empty and thus cannot be marked as an image.", "Cannot Mark Image", JOptionPane.ERROR_MESSAGE);
						return false;
					}
					
					//	clean up nested regions
					for (int r = 0; r < selectedRegions.length; r++)
						page.removeRegion(selectedRegions[r]);
					
					//	mark image
					ImRegion image = new ImRegion(page, new BoundingBox(selectedIpr.getLeftCol(), selectedIpr.getRightCol(), selectedIpr.getTopRow(), selectedIpr.getBottomRow()), ImRegion.IMAGE_TYPE);
					idmp.setRegionsPainted(ImRegion.IMAGE_TYPE, true);
					
					//	get potential captions
					ImAnnotation[] captionAnnots = ImUtils.findCaptions(image, false, true, true);
					
					//	try setting attributes in unassigned captions first
					for (int a = 0; a < captionAnnots.length; a++) {
						if (captionAnnots[a].hasAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE) || captionAnnots[a].hasAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE))
							continue;
						captionAnnots[a].setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + image.pageId));
						captionAnnots[a].setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, image.bounds.toString());
						return true;
					}
					
					//	set attributes in any caption (happens if user corrects, for instance)
					if (captionAnnots.length != 0) {
						captionAnnots[0].setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + image.pageId));
						captionAnnots[0].setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, image.bounds.toString());
					}
					
					//	finally ...
					return true;
				}
			});
		}
		
		//	single region selected
		if (selectedRegions.length == 1) {
			
			//	edit attributes of existing region
			actions.add(new SelectionAction("editAttributesRegion", ("Edit " + selectedRegions[0].getType() + " Attributes"), ("Edit attributes of '" + selectedRegions[0].getType() + "' region.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					idmp.editAttributes(selectedRegions[0], selectedRegions[0].getType(), "");
					return true;
				}
			});
			
			//	remove existing annotation
			actions.add(new SelectionAction("removeRegion", ("Remove " + selectedRegions[0].getType() + " Region"), ("Remove '" + selectedRegions[0].getType() + "' region.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					page.removeRegion(selectedRegions[0]);
					return true;
				}
			});
			
			//	change type of existing annotation
			actions.add(new SelectionAction("changeTypeRegion", ("Change Region Type"), ("Change type of '" + selectedRegions[0].getType() + "' region.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					String regionType = ImUtils.promptForObjectType("Enter Region Type", "Enter or select new region type", page.getRegionTypes(), selectedRegions[0].getType(), true);
					if (regionType == null)
						return false;
					selectedRegions[0].setType(regionType);
					idmp.setRegionsPainted(regionType, true);
					return true;
				}
			});
		}
		
		//	multiple regions selected
		else if (selectedRegions.length > 1) {
			
			//	edit region attributes
			actions.add(new SelectionAction("editAttributesRegion", "Edit Region Attributes ...", "Edit attributes of selected regions.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Edit Region Attributes ...");
					JMenuItem mi;
					for (int t = 0; t < selectedRegions.length; t++) {
						final ImRegion selectedRegion = selectedRegions[t];
						mi = new JMenuItem("- " + selectedRegion.getType() + " at " + selectedRegion.bounds.toString());
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								idmp.editAttributes(selectedRegion, selectedRegion.getType(), "");
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
			actions.add(new SelectionAction("removeRegion", "Remove Region ...", "Remove selected regions.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Remove Region ...");
					JMenuItem mi;
					for (int t = 0; t < selectedRegions.length; t++) {
						final ImRegion selectedRegion = selectedRegions[t];
						mi = new JMenuItem("- " + selectedRegion.getType() + " at " + selectedRegion.bounds.toString());
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								invoker.beginAtomicAction("Remove '" + selectedRegion.getType() + "' Region");
								page.removeRegion(selectedRegion);
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
			
			//	remove regions of some type together
			if (multiSelectedRegionsByType.size() == 1)
				actions.add(new SelectionAction("removeTypeRegion", ("Remove " + multiSelectedRegionsByType.firstKey() + " Regions"), ("Remove all selected '" + multiSelectedRegionsByType.firstKey() + "' regions.")) {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						LinkedList typeRegs = ((LinkedList) multiSelectedRegionsByType.get(multiSelectedRegionsByType.firstKey()));
						while (typeRegs.size() != 0)
							page.removeRegion((ImRegion) typeRegs.removeFirst());
						return true;
					}
				});
			else if (multiSelectedRegionsByType.size() > 1)
				actions.add(new SelectionAction("removeTypeRegion", "Remove Regions ...", "Remove selected regions by type.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						return false;
					}
					public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
						JMenu pm = new JMenu("Remove Regions ...");
						JMenuItem mi;
						for (Iterator rtit = multiSelectedRegionsByType.keySet().iterator(); rtit.hasNext();) {
							final String regType = ((String) rtit.next());
							final LinkedList typeRegs = ((LinkedList) multiSelectedRegionsByType.get(regType));
							mi = new JMenuItem("- " + regType + " (" + typeRegs.size() + ")");
							mi.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent ae) {
									invoker.beginAtomicAction("Remove '" + regType + "' Regions");
									while (typeRegs.size() != 0)
										page.removeRegion((ImRegion) typeRegs.removeFirst());
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
			actions.add(new SelectionAction("changeTypeRegion", "Change Region Type ...", "Change the type of selected regions.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					return false;
				}
				public JMenuItem getMenuItem(final ImDocumentMarkupPanel invoker) {
					JMenu pm = new JMenu("Change Region Type ...");
					JMenuItem mi;
					for (int t = 0; t < selectedRegions.length; t++) {
						final ImRegion selectedRegion = selectedRegions[t];
						mi = new JMenuItem("- " + selectedRegion.getType() + " " + selectedRegion.bounds.toString());
						mi.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								String regionType = ImUtils.promptForObjectType("Enter Region Type", "Enter or select new region type", page.getRegionTypes(), selectedRegion.getType(), true);
								if (regionType != null) {
									invoker.beginAtomicAction("Chage Region Type");
									selectedRegion.setType(regionType);
									invoker.endAtomicAction();
									invoker.setAnnotationsPainted(regionType, true);
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
		
		//	change region into annotation if eligible and likely
		for (int r = 0; r < selectedRegions.length; r++) {
			
			//	this one is not alone, selection looks like having other purpose
			if (multiSelectedRegionsByType.containsKey(selectedRegions[r].getType()))
				continue;
			
			//	get region words
			ImWord[] regionWords = page.getWordsInside(selectedRegions[r].bounds);
			if (regionWords.length == 0)
				continue;
			
			//	order words
			Arrays.sort(regionWords, ImUtils.textStreamOrder);
			
			//	check if we have a single continuous stream
			boolean streamBroken = false;
			for (int w = 1; w < selectedWords.length; w++)
				if (selectedWords[w].getPreviousWord() != selectedWords[w-1]) {
					streamBroken = true;
					break;
				}
			
			//	can we work with this one?
			if (streamBroken)
				continue;
			
			//	offer transforming region into annotation
			final ImWord firstWord = regionWords[0];
			final ImWord lastWord = regionWords[regionWords.length-1];
			final String regionType = selectedRegions[r].getType();
			final ImRegion selectedRegion = selectedRegions[r];
			actions.add(new SelectionAction("annotateRegion", ("Annotate '" + regionType + "' Region"), ("Create an annotation from '" + regionType + "' region.")) {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					page.removeRegion(selectedRegion);
					idmp.document.addAnnotation(firstWord, lastWord, regionType);
					idmp.setAnnotationsPainted(regionType, true);
					return true;
				}
			});
		}
		
		//	offer paragraph actions
		if (idmp.areRegionsPainted(ImRegion.PARAGRAPH_TYPE)) {
			
			//	if part of a paragraph is selected, offer splitting
			if (contextRegionsByType.containsKey(ImRegion.PARAGRAPH_TYPE) && !multiSelectedRegionsByType.containsKey(ImRegion.PARAGRAPH_TYPE)) {
				final ImRegion paragraph = ((ImRegion) ((LinkedList) contextRegionsByType.get(ImRegion.PARAGRAPH_TYPE)).getFirst());
				final ImRegion[] paragraphLines = paragraph.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
				final ImRegion[] selectedLines = page.getRegionsInside(selectedBox, true);
				if ((selectedLines.length != 0) && (selectedLines.length < paragraphLines.length))
					actions.add(new SelectionAction("paragraphsSplit", "Split 'paragraph' Region", "Split the selected paragraph.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							if (sortLinesIntoParagraphs(page, paragraphLines, selectedBounds)) {
								page.removeRegion(paragraph);
								return true;
							}
							else return false;
						}
					});
			}
			
			//	if two or more paragraphs are selected, offer merging
			if (multiSelectedRegionsByType.containsKey(ImRegion.PARAGRAPH_TYPE)) {
				final LinkedList paragraphList = ((LinkedList) multiSelectedRegionsByType.get(ImRegion.PARAGRAPH_TYPE));
				actions.add(new SelectionAction("paragraphsMerge", "Merge 'paragraph' Regions", "Merge the selected paragraphs.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						ImRegion[] paragraphs = ((ImRegion[]) paragraphList.toArray(new ImRegion[paragraphList.size()]));
						ImRegion mergedParagraph = new ImRegion(page, ImLayoutObject.getAggregateBox(paragraphs), ImRegion.PARAGRAPH_TYPE);
						mergedParagraph.copyAttributes(paragraphs[0]);
						for (int p = 0; p < paragraphs.length; p++)
							page.removeRegion(paragraphs[p]);
						ImWord[] mergedParagraphWords = mergedParagraph.getWords();
						if (mergedParagraphWords.length != 0) {
							Arrays.sort(mergedParagraphWords, ImUtils.textStreamOrder);
							for (int w = 0; w < (mergedParagraphWords.length-1); w++) {
								if (mergedParagraphWords[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
									mergedParagraphWords[w].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
							}
						}
						return true;
					}
				});
			}
			
			//	if blocks and paragraphs visible and click inside block or block selected, offer re-detecting paragraphs
			if (idmp.areRegionsPainted(ImRegion.BLOCK_ANNOTATION_TYPE) && (selectedRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE) || contextRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE)) && !multiSelectedRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE)) {
				
				//	get block
				final ImRegion block;
				if (selectedRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE))
					block = ((ImRegion) ((LinkedList) selectedRegionsByType.get(ImRegion.BLOCK_ANNOTATION_TYPE)).getFirst());
				else block = ((ImRegion) ((LinkedList) contextRegionsByType.get(ImRegion.BLOCK_ANNOTATION_TYPE)).getFirst());
				
				//	get lines
				final ImRegion[] blockLines = block.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
				if (blockLines.length > 1)
					actions.add(new SelectionAction("paragraphsInBlock", "Revise Block Paragraphs", "Revise the grouping of the lines in the block into paragraphs.") {
						public boolean performAction(ImDocumentMarkupPanel invoker) {
							return splitBlock(page, block, block.getRegions(ImRegion.PARAGRAPH_TYPE), blockLines, idmp.getMaxPageImageDpi());
						}
					});
			}
			
			//	if blocks and paragraphs visible and multiple blocks selected, offer merging blocks and re-detecting paragraphs and lines
			if (idmp.areRegionsPainted(ImRegion.BLOCK_ANNOTATION_TYPE) && multiSelectedRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE))
				actions.add(new SelectionAction("paragraphsInBlock", "Merge Blocks", "Merge selected blocks, re-detect lines, and group them into paragraphs.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						
						//	get & merge blocks
						LinkedList blockList = ((LinkedList) multiSelectedRegionsByType.get(ImRegion.BLOCK_ANNOTATION_TYPE));
						ImRegion[] blocks = ((ImRegion[]) blockList.toArray(new ImRegion[blockList.size()]));
						for (int b = 0; b < blocks.length; b++)
							page.removeRegion(blocks[b]);
						ImRegion block = new ImRegion(page, ImLayoutObject.getAggregateBox(blocks), ImRegion.BLOCK_ANNOTATION_TYPE);
						
						//	remove old lines and paragraphs
						ImRegion[] blockParagraphs = block.getRegions(ImRegion.PARAGRAPH_TYPE);
						for (int p = 0; p < blockParagraphs.length; p++)
							page.removeRegion(blockParagraphs[p]);
						ImRegion[] blockLines = block.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
						for (int l = 0; l < blockLines.length; l++)
							page.removeRegion(blockLines[l]);
						
						//	get block words
						ImWord[] blockWords = block.getWords();
						sortIntoLines(page, blockWords);
						
						//	re-detect paragraphs
						PageAnalysis.splitIntoParagraphs(block, page.getImageDPI(), null);
						
						//	update text stream structure
						updateBlockTextStream(block);
						
						//	finally ...
						return true;
					}
				});
			
			//	if one block partially selected, offer splitting
			if (idmp.areRegionsPainted(ImRegion.BLOCK_ANNOTATION_TYPE) && contextRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE) && !multiSelectedRegionsByType.containsKey(ImRegion.BLOCK_ANNOTATION_TYPE)) {
				final ImRegion block = ((ImRegion) ((LinkedList) contextRegionsByType.get(ImRegion.BLOCK_ANNOTATION_TYPE)).getFirst());
				actions.add(new SelectionAction("paragraphsInBlock", "Split Block", "Split selected block, re-detect lines, and group them into paragraphs.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						
						//	sort block words into top, left, right, and bottom of as well as inside selection
						ImWord[] blockWords = block.getWords();
						ArrayList aboveWords = new ArrayList();
						ArrayList belowWords = new ArrayList();
						ArrayList leftWords = new ArrayList();
						ArrayList rightWords = new ArrayList();
						ArrayList selectedWords = new ArrayList();
						for (int w = 0; w < blockWords.length; w++) {
							if (blockWords[w].centerY < selectedBounds.top)
								aboveWords.add(blockWords[w]);
							else if (selectedBounds.bottom <= blockWords[w].centerY)
								belowWords.add(blockWords[w]);
							else if (blockWords[w].centerX < selectedBounds.left)
								leftWords.add(blockWords[w]);
							else if (selectedBounds.right <= blockWords[w].centerX)
								rightWords.add(blockWords[w]);
							else selectedWords.add(blockWords[w]);
						}
						
						//	anything selected, everything selected?
						if (selectedWords.isEmpty() || (selectedWords.size() == blockWords.length))
							return false;
						
						//	get existing paragraphs and lines
						ImRegion[] blockParagraphs = block.getRegions(ImRegion.PARAGRAPH_TYPE);
						ImRegion[] blockLines = block.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
						
						//	if we have a plain top-bottom split, we can retain lines and word order, and likely most paragraphs
						if (leftWords.isEmpty() && rightWords.isEmpty()) {
							
							//	split up paragraphs that intersect with the selection
							for (int p = 0; p < blockParagraphs.length; p++) {
								if (!blockParagraphs[p].bounds.overlaps(selectedBounds))
									continue;
								else if (blockParagraphs[p].bounds.liesIn(selectedBounds, false))
									continue;
								ImRegion[] paragraphLines = blockParagraphs[p].getRegions(ImRegion.LINE_ANNOTATION_TYPE);
								if (sortLinesIntoParagraphs(page, paragraphLines, selectedBounds))
									page.removeRegion(blockParagraphs[p]);
							}
							
							//	split the block proper
							if (aboveWords.size() != 0) {
								ImRegion aboveBlock = new ImRegion(page, ImLayoutObject.getAggregateBox((ImWord[]) aboveWords.toArray(new ImWord[aboveWords.size()])), ImRegion.BLOCK_ANNOTATION_TYPE);
								updateBlockTextStream(aboveBlock);
							}
							ImRegion selectedBlock = new ImRegion(page, ImLayoutObject.getAggregateBox((ImWord[]) selectedWords.toArray(new ImWord[selectedWords.size()])), ImRegion.BLOCK_ANNOTATION_TYPE);
							updateBlockTextStream(selectedBlock);
							if (belowWords.size() != 0) {
								ImRegion belowBlock = new ImRegion(page, ImLayoutObject.getAggregateBox((ImWord[]) belowWords.toArray(new ImWord[belowWords.size()])), ImRegion.BLOCK_ANNOTATION_TYPE);
								updateBlockTextStream(belowBlock);
							}
							page.removeRegion(block);
							
							//	indicate we've done something
							return true;
						}
						
						//	remove old lines and paragraphs
						for (int p = 0; p < blockParagraphs.length; p++)
							page.removeRegion(blockParagraphs[p]);
						for (int l = 0; l < blockLines.length; l++)
							page.removeRegion(blockLines[l]);
						
						//	get last external predecessor and first external successor
						Arrays.sort(blockWords, ImUtils.textStreamOrder);
						ImWord blockPrevWord = blockWords[0].getPreviousWord();
						ImWord blockNextWord = blockWords[blockWords.length-1].getNextWord();
						
						//	cut out block words to avoid conflicts while re-chaining
						if (blockPrevWord != null)
							blockPrevWord.setNextWord(null);
						if (blockNextWord != null)
							blockNextWord.setPreviousWord(null);
						
						//	collect block words in order of chaining
						ArrayList blockWordLists = new ArrayList();
						
						//	if we have a plain left-right split, chain blocks left to right
						if (aboveWords.isEmpty() && belowWords.isEmpty()) {
							if (leftWords.size() != 0)
								blockWordLists.add(this.markBlock(page, leftWords));
							if (selectedWords.size() != 0)
								blockWordLists.add(this.markBlock(page, selectedWords));
							if (rightWords.size() != 0)
								blockWordLists.add(this.markBlock(page, rightWords));
						}
						
						//	selection cut out on top left corner
						else if (aboveWords.isEmpty() && leftWords.isEmpty()) {
							if (selectedWords.size() != 0)
								blockWordLists.add(this.markBlock(page, selectedWords));
							if (rightWords.size() != 0)
								blockWordLists.add(this.markBlock(page, rightWords));
							if (belowWords.size() != 0)
								blockWordLists.add(this.markBlock(page, belowWords));
						}
						
						//	selection cut out in some other place
						else {
							if (aboveWords.size() != 0)
								blockWordLists.add(this.markBlock(page, aboveWords));
							if (leftWords.size() != 0)
								blockWordLists.add(this.markBlock(page, leftWords));
							if (rightWords.size() != 0)
								blockWordLists.add(this.markBlock(page, rightWords));
							if (belowWords.size() != 0)
								blockWordLists.add(this.markBlock(page, belowWords));
							if (selectedWords.size() != 0)
								blockWordLists.add(this.markBlock(page, selectedWords));
						}
						
						//	chain block text streams (only if text stream types match, however)
						for (int b = 0; b < blockWordLists.size(); b++) {
							ImWord[] imws = ((ImWord[]) blockWordLists.get(b));
							if (imws.length == 0)
								continue;
							if ((blockPrevWord != null) && !blockPrevWord.getTextStreamType().endsWith(imws[0].getTextStreamType()))
								continue;
							if (blockPrevWord != null)
								blockPrevWord.setNextWord(imws[0]);
							blockPrevWord = imws[imws.length - 1];
						}
						if ((blockPrevWord != null) && (blockNextWord != null))
							blockPrevWord.setNextWord(blockNextWord);
						
						//	remove the split block
						page.removeRegion(block);
						
						//	indicate we've done something
						return true;
					}
					private ImWord[] markBlock(ImPage page, ArrayList wordList) {
						ImWord[] words = ((ImWord[]) wordList.toArray(new ImWord[wordList.size()]));
						if (words.length == 0)
							return words;
						
						//	mark lines
						ImUtils.makeStream(words, words[0].getTextStreamType(), null);
						sortIntoLines(page, words);
						
						//	mark block proper
						ImRegion block = new ImRegion(page, ImLayoutObject.getAggregateBox(words), ImRegion.BLOCK_ANNOTATION_TYPE);
						
						//	re-detect paragraphs
						PageAnalysis.splitIntoParagraphs(block, page.getImageDPI(), null);
						
						//	finally ...
						Arrays.sort(words, ImUtils.textStreamOrder);
						return words;
					}
				});
			}
		}
		
		//	offer copying selected figure to clipboard, as well as assigning caption
		if (idmp.areRegionsPainted(ImRegion.IMAGE_TYPE)) {
			LinkedHashSet selectedImages = new LinkedHashSet();
			for (int r = 0; r < contextRegions.length; r++) {
				if (ImRegion.IMAGE_TYPE.equals(contextRegions[r].getType()))
					selectedImages.add(contextRegions[r]);
			}
			for (int r = 0; r < selectedRegions.length; r++) {
				if (ImRegion.IMAGE_TYPE.equals(selectedRegions[r].getType()))
					selectedImages.add(selectedRegions[r]);
			}
			if (selectedImages.size() == 1) {
				final ImRegion selectedImage = ((ImRegion) selectedImages.iterator().next());
				System.out.println("Assign caption to image at " + selectedImage.bounds.toString());
				actions.add(new TwoClickSelectionAction("assignCaptionImage", "Assign Caption", "Assign a caption to this image with a second click.") {
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
						if (wordCaption.getFirstWord().getString().toLowerCase().startsWith("tab"))
							return false;
						
						//	set attributes
						wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, selectedImage.bounds.toString());
						wordCaption.setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + selectedImage.pageId));
						return true;
					}
					public ImWord getFirstWord() {
						if (this.artificialStartWord == null)
							this.artificialStartWord = new ImWord(selectedImage.getDocument(), selectedImage.pageId, selectedImage.bounds, "IMAGE");
						return this.artificialStartWord;
					}
					public String getActiveLabel() {
						return ("Click on a caption to assign it to the image at " + selectedImage.bounds.toString() + " on page " + (selectedImage.pageId + 1));
					}
				});
				actions.add(new SelectionAction("copyImage", "Copy Image", "Copy the selected image to the system clipboard.") {
					public boolean performAction(ImDocumentMarkupPanel invoker) {
						Image image = null;
						ImSupplement[] pageSupplements = page.getSupplements();
						for (int s = 0; s < pageSupplements.length; s++) {
							if (idmp.documentBornDigital && (pageSupplements[s] instanceof Figure) && ((Figure) pageSupplements[s]).getBounds().includes(selectedImage.bounds, true)) try {
								image = ImageIO.read(((Figure) pageSupplements[s]).getInputStream());
								break;
							} catch (Exception e) {}
							if (!idmp.documentBornDigital && (pageSupplements[s] instanceof Scan)) try {
								BufferedImage scan = ImageIO.read(((Scan) pageSupplements[s]).getInputStream());
								image = scan.getSubimage(selectedImage.bounds.left, selectedImage.bounds.top, (selectedImage.bounds.right - selectedImage.bounds.left), (selectedImage.bounds.bottom - selectedImage.bounds.top));
								break;
							} catch (Exception e) {}
						}
						if (image == null) {
							PageImage pageImage = page.getPageImage();
							if (pageImage != null)
								image = pageImage.image.getSubimage(selectedImage.bounds.left, selectedImage.bounds.top, (selectedImage.bounds.right - selectedImage.bounds.left), (selectedImage.bounds.bottom - selectedImage.bounds.top));
						}
						if (image != null)
							ImUtils.copy(new ImageSelection(image));
						return false;
					}
				});
			}
		}
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	private void sortIntoLines(ImPage page, ImWord[] words) {
		
		//	order text stream
		ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
		Arrays.sort(words, ImUtils.textStreamOrder);
		
		//	re-detect lines
//		int lineStartWordIndex = 0;
//		for (int w = 0; w < words.length; w++)
//			if (((w+1) == words.length) || (words[w+1].centerY > words[w].bounds.bottom)) {
//				new ImRegion(page, ImLayoutObject.getAggregateBox(words, lineStartWordIndex, (w+1)), ImRegion.LINE_ANNOTATION_TYPE);
//				lineStartWordIndex = (w+1);
//			}
		this.markLines(page, words, null);
	}
	
	private void markLines(ImPage page, ImWord[] words, Map existingLines) {
		int lineStartWordIndex = 0;
		for (int w = 1; w <= words.length; w++)
			
			//	end line at downward or leftward jump, and at last word
			if ((w == words.length) || (words[lineStartWordIndex].bounds.bottom <= words[w].bounds.top) || (words[w].centerX < words[w-1].centerX)) {
				BoundingBox lBounds = ImLayoutObject.getAggregateBox(words, lineStartWordIndex, w);
				if (existingLines == null)
					new ImRegion(page, lBounds, ImRegion.LINE_ANNOTATION_TYPE);
				else {
					ImRegion bLine = ((ImRegion) existingLines.remove(lBounds));
					if (bLine == null)
						bLine = new ImRegion(page, lBounds, ImRegion.LINE_ANNOTATION_TYPE);
				}
				lineStartWordIndex = w;
			}
		
		//	remove now-spurious lines (if any)
		if (existingLines != null)
			for (Iterator lbit = existingLines.keySet().iterator(); lbit.hasNext();)
				page.removeRegion((ImRegion) existingLines.get(lbit.next()));
	}
	
	private void updateBlockTextStream(ImRegion block) {
		ImRegion[] blockParagraphs = block.getRegions(ImRegion.PARAGRAPH_TYPE);
		for (int p = 0; p < blockParagraphs.length; p++) {
			ImWord[] paragraphWords = blockParagraphs[p].getWords();
			Arrays.sort(paragraphWords, ImUtils.textStreamOrder);
			for (int w = 0; w < paragraphWords.length; w++) {
				if ((w+1) == paragraphWords.length) {
					if (paragraphWords[w].getNextRelation() != ImWord.NEXT_RELATION_PARAGRAPH_END)
						paragraphWords[w].setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				}
				else if (paragraphWords[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
					paragraphWords[w].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
			}
		}
	}
	
	private boolean sortLinesIntoParagraphs(ImPage page, ImRegion[] lines, BoundingBox selectedBounds) {
		Arrays.sort(lines, ImUtils.topDownOrder);
		
		//	find boundaries between above, inside, and below selection
		int firstSelectedLineIndex = 0;
		int firstNonSelectedLineIndex = lines.length;
		for (int l = 0; l < lines.length; l++) {
			int lineCenterY = ((lines[l].bounds.top + lines[l].bounds.bottom) / 2);
			if (lineCenterY < selectedBounds.top)
				firstSelectedLineIndex = (l+1);
			if (lineCenterY < selectedBounds.bottom)
				firstNonSelectedLineIndex = (l+1);
		}
		
		//	anything to work with?
		if (firstSelectedLineIndex == firstNonSelectedLineIndex)
			return false;
		
		//	mark parts, and update word relations
		if (firstSelectedLineIndex > 0) {
			ImRegion aboveSelectionParagraph = new ImRegion(page, ImLayoutObject.getAggregateBox(lines, 0, firstSelectedLineIndex), ImRegion.PARAGRAPH_TYPE);
			ImWord[] aboveSelectionParagraphWords = aboveSelectionParagraph.getWords();
			if (aboveSelectionParagraphWords.length != 0) {
				Arrays.sort(aboveSelectionParagraphWords, ImUtils.textStreamOrder);
				aboveSelectionParagraphWords[aboveSelectionParagraphWords.length-1].setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
			}
		}
		ImRegion selectionParagraph = new ImRegion(page, ImLayoutObject.getAggregateBox(lines, firstSelectedLineIndex, firstNonSelectedLineIndex), ImRegion.PARAGRAPH_TYPE);
		if (firstNonSelectedLineIndex < lines.length) {
			ImWord[] selectionParagraphWords = selectionParagraph.getWords();
			if (selectionParagraphWords.length != 0) {
				Arrays.sort(selectionParagraphWords, ImUtils.textStreamOrder);
				selectionParagraphWords[selectionParagraphWords.length-1].setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
			}
			new ImRegion(page, ImLayoutObject.getAggregateBox(lines, firstNonSelectedLineIndex, lines.length), ImRegion.PARAGRAPH_TYPE);
		}
		return true;
	}
	
	private class ImageSelection implements Transferable {
		private Image image;
		ImageSelection(Image image) {
			this.image = image;
		}
		public DataFlavor[] getTransferDataFlavors() {
			DataFlavor[] dfs = {DataFlavor.imageFlavor};
			return dfs;
		}
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return DataFlavor.imageFlavor.equals(flavor);
		}
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (DataFlavor.imageFlavor.equals(flavor))
				return image;
			else throw new UnsupportedFlavorException(flavor);
		}
	}
	
	private void ensureBlockStructure(ImRegion block) {
		
		//	get words
		ImWord[] bWords = block.getWords();
		if (bWords.length == 0)
			return;
		
		//	check if all (non-artifact) words inside lines
		ImRegion[] bLines = block.getRegions(ImRegion.LINE_ANNOTATION_TYPE); 
		int lWordCount = 0;
		for (int l = 0; l < bLines.length; l++) {
			ImWord[] lWords = bLines[l].getWords();
			lWordCount += lWords.length;
		}
		
		//	check if all (non-artifact) words inside paragraphs
		ImRegion[] bParagraphs = block.getRegions(ImRegion.PARAGRAPH_TYPE); 
		int pWordCount = 0;
		for (int p = 0; p < bParagraphs.length; p++) {
			ImWord[] pWords = bParagraphs[p].getWords();
			pWordCount += pWords.length;
		}
		
		//	all words nested properly, we're done here
		if ((bWords.length <= lWordCount) && (bWords.length <= pWordCount))
			return;
		
		//	get page
		ImPage page = block.getPage();
		
		//	repair line nesting
		if (lWordCount < bWords.length) {
			
			//	index lines by bounding boxes
			HashMap exLines = new HashMap();
			for (int l = 0; l < bLines.length; l++)
				exLines.put(bLines[l].bounds, bLines[l]);
			
			//	order words and mark lines
			ImUtils.sortLeftRightTopDown(bWords);
			int lStartIndex = 0;
			for (int w = 1; w <= bWords.length; w++)
				
				//	end line at downward or leftward jump, and at last word
				if ((w == bWords.length) || (bWords[lStartIndex].bounds.bottom <= bWords[w].bounds.top) || (bWords[w].centerX < bWords[w-1].centerX)) {
					BoundingBox lBounds = ImLayoutObject.getAggregateBox(bWords, lStartIndex, w);
					ImRegion bLine = ((ImRegion) exLines.remove(lBounds));
					if (bLine == null)
						bLine = new ImRegion(page, lBounds, ImRegion.LINE_ANNOTATION_TYPE);
					lStartIndex = w;
				}
			
			//	remove now-spurious lines
			for (Iterator lbit = exLines.keySet().iterator(); lbit.hasNext();)
				page.removeRegion((ImRegion) exLines.get(lbit.next()));
			
			//	re-get lines
			bLines = block.getRegions(ImRegion.LINE_ANNOTATION_TYPE);
		}
		
		//	repair paragraph nesting
		if (pWordCount < bWords.length) {
			
			//	clean up existing paragraphs
			for (int p = 0; p < bParagraphs.length; p++)
				page.removeRegion(bParagraphs[p]);
			
			//	re-mark paragraphs
			PageAnalysis.splitIntoParagraphs(block, page.getImageDPI(), ProgressMonitor.dummy);
		}
	}
	
	private boolean splitBlock(ImPage page, ImRegion block, ImRegion[] blockParagraphs, ImRegion[] blockLines, int dpi) {
		
		//	make sure block lines come top-down
		Arrays.sort(blockLines, ImUtils.topDownOrder);
		
		//	index paragraphs and their start lines
		HashSet paragraphStartLineBounds = new HashSet();
		HashMap paragraphsByBounds = new HashMap();
		for (int p = 0; p < blockParagraphs.length; p++) {
			ImRegion[] paragraphLines = blockParagraphs[p].getRegions(ImRegion.LINE_ANNOTATION_TYPE);
			if (paragraphLines.length == 0)
				continue;
			Arrays.sort(paragraphLines, ImUtils.topDownOrder);
			paragraphsByBounds.put(blockParagraphs[p].bounds, blockParagraphs[p]);
			paragraphStartLineBounds.add(paragraphLines[0].bounds);
		}
		
		//	assess line starts
		int pslLeftDistSum = 0;
		int leftDistSum = 0;
		boolean[] isIndentedLine = new boolean[blockLines.length];
		boolean[] isShortLine = new boolean[blockLines.length];
		for (int l = 0; l < blockLines.length; l++) {
			int leftDist = (blockLines[l].bounds.left - block.bounds.left);
			if (paragraphStartLineBounds.contains(blockLines[l].bounds))
				pslLeftDistSum += leftDist;
			leftDistSum += leftDist;
			isIndentedLine[l] = ((dpi / 12) /* about 2mm */ < leftDist); // at least 2mm shy of left block edge
			int rightDist = (block.bounds.right - blockLines[l].bounds.right);
			isShortLine[l] = ((block.bounds.right - block.bounds.left) < (rightDist * 20)); // at least 5% shy of right block edge
		}
		int avgLeftDist = (leftDistSum / blockLines.length);
		int avgPslLeftDist = ((paragraphStartLineBounds.isEmpty()) ? avgLeftDist : (pslLeftDistSum / paragraphStartLineBounds.size()));
		
		//	assemble split option panel
		final JRadioButton pslIndent = new JRadioButton("indented", ((avgLeftDist + (dpi / 12)) < avgPslLeftDist)); // at least 2mm difference
		final JRadioButton pslOutdent = new JRadioButton("outdented", ((avgPslLeftDist + (dpi / 12)) < avgLeftDist)); // at least 2mm difference
		final JRadioButton pslFlush = new JRadioButton("neither", (!pslIndent.isSelected() && !pslOutdent.isSelected()));
		ButtonGroup pslButtonGroup = new ButtonGroup();
		pslButtonGroup.add(pslIndent);
		pslButtonGroup.add(pslOutdent);
		pslButtonGroup.add(pslFlush);
		JPanel pslButtonPanel = new JPanel(new GridLayout(1, 0), true);
		pslButtonPanel.add(pslIndent);
		pslButtonPanel.add(pslOutdent);
		pslButtonPanel.add(pslFlush);
		JPanel pslPanel = new JPanel(new BorderLayout(), true);
		pslPanel.add(new JLabel("Paragraph start lines are "), BorderLayout.WEST);
		pslPanel.add(pslButtonPanel, BorderLayout.CENTER);
		
		final JCheckBox shortLineEndsParagraph = new JCheckBox("Lines short of right block edge end paragraphs", true);
		final JCheckBox singleLineParagraphs = new JCheckBox("Make each line a separate paragraph", false);
		singleLineParagraphs.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ie) {
				pslIndent.setEnabled(!singleLineParagraphs.isSelected());
				pslOutdent.setEnabled(!singleLineParagraphs.isSelected());
				pslFlush.setEnabled(!singleLineParagraphs.isSelected());
				shortLineEndsParagraph.setEnabled(!singleLineParagraphs.isSelected());
			}
		});
		
		JPanel blockSplitOptionPanel = new JPanel(new GridLayout(0, 1), true);
		blockSplitOptionPanel.add(pslPanel);
		blockSplitOptionPanel.add(shortLineEndsParagraph);
		blockSplitOptionPanel.add(singleLineParagraphs);
		
		//	prompt user
		if (DialogFactory.confirm(blockSplitOptionPanel, "Select Block Splitting Options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
			return false;
		
		//	keep track of logical paragraphs at block boundaries
		boolean firstParagraphHasStart = (singleLineParagraphs.isSelected() || (isIndentedLine[0] && pslIndent.isSelected()) || (!isIndentedLine[0] && pslOutdent.isSelected()));
		boolean lastParagraphHasEnd = (singleLineParagraphs.isSelected() || (isShortLine[blockLines.length-1] && shortLineEndsParagraph.isSelected()));
		
		//	do the splitting
		int paragraphStartLineIndex = 0;
		for (int l = 0; l < blockLines.length; l++) {
			
			//	assess whether or not to split after current line
			boolean lineEndsParagraph = false;
			if (singleLineParagraphs.isSelected()) // just split after each line
				lineEndsParagraph = true;
			else if (shortLineEndsParagraph.isSelected() && isShortLine[l]) // split after short line
				lineEndsParagraph = true;
			else if (((l+1) < blockLines.length) && isIndentedLine[l+1] && pslIndent.isSelected()) // split before indented line
				lineEndsParagraph = true;
			else if (((l+1) < blockLines.length) && !isIndentedLine[l+1] && pslOutdent.isSelected()) // split before outdented line
				lineEndsParagraph = true;
			
			//	perform split
			if (lineEndsParagraph) {
				BoundingBox paragraphBounds = ImLayoutObject.getAggregateBox(blockLines, paragraphStartLineIndex, (l+1));
				ImRegion paragraph = ((ImRegion) paragraphsByBounds.remove(paragraphBounds));
				if (paragraph == null)
					paragraph = new ImRegion(page, paragraphBounds, ImRegion.PARAGRAPH_TYPE);
				paragraphStartLineIndex = (l+1);
			}
		}
		if (paragraphStartLineIndex < blockLines.length) {
			BoundingBox paragraphBounds = ImLayoutObject.getAggregateBox(blockLines, paragraphStartLineIndex, blockLines.length);
			ImRegion paragraph = ((ImRegion) paragraphsByBounds.remove(paragraphBounds));
			if (paragraph == null)
				paragraph = new ImRegion(page, paragraphBounds, ImRegion.PARAGRAPH_TYPE);
		}
		
		//	clean up now-spurious paragraphs
		for (Iterator pbit = paragraphsByBounds.keySet().iterator(); pbit.hasNext();)
			page.removeRegion((ImRegion) paragraphsByBounds.get(pbit.next()));
		
		//	update word relations
		blockParagraphs = block.getRegions(ImRegion.PARAGRAPH_TYPE);
		for (int p = 0; p < blockParagraphs.length; p++) {
			ImWord[] paragraphWords = blockParagraphs[p].getWords();
			if (paragraphWords.length == 0)
				continue;
			Arrays.sort(paragraphWords, ImUtils.textStreamOrder);
			for (int w = 0; w < (paragraphWords.length-1); w++) {
				if (paragraphWords[w].getNextRelation() == ImWord.NEXT_RELATION_PARAGRAPH_END)
					paragraphWords[w].setNextRelation(ImWord.NEXT_RELATION_SEPARATE);
			}
			if (((p != 0) || firstParagraphHasStart) && (paragraphWords[0].getPreviousWord() != null))
				paragraphWords[0].getPreviousWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
			if (((p+1) != blockParagraphs.length) || lastParagraphHasEnd)
				paragraphWords[paragraphWords.length-1].setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
		}
		
		//	finally ...
		return true;
	}
}