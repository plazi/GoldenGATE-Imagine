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
import java.util.Arrays;
import java.util.LinkedList;

import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImLayoutObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.Imaging;
import de.uka.ipd.idaho.im.analysis.Imaging.AnalysisImage;
import de.uka.ipd.idaho.im.analysis.Imaging.ImagePartRectangle;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;

/**
 * This class provides basic actions for working with text blocks.
 * 
 * @author sautter
 */
public class TextBlockActionProvider extends AbstractSelectionActionProvider implements LiteratureConstants {
	
	/** public zero-argument constructor for class loading */
	public TextBlockActionProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Text Block Actions";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(final ImWord start, final ImWord end, ImDocumentMarkupPanel idmp) {
		
		//	we strictly work on one page at a time
		if (start.pageId != end.pageId)
			return null;
		
		//	we also work on individual text streams only
		if (!start.getTextStreamId().equals(end.getTextStreamId()))
			return null;
		
		//	line up words
		LinkedList words = new LinkedList();
		for (ImWord imw = start; imw != null; imw = imw.getNextWord()) {
			words.addLast(imw);
			if (imw == end)
				break;
		}
		
		//	return actions
		return this.getActions(((ImWord[]) words.toArray(new ImWord[words.size()])), null, null);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractSelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, ImDocumentMarkupPanel idmp) {
		
		//	mark selection
		BoundingBox selectedBox = new BoundingBox(Math.min(start.x, end.x), Math.max(start.x, end.x), Math.min(start.y, end.y), Math.max(start.y, end.y));
		
		//	get selected words
		ImWord[] selectedWords = page.getWordsInside(selectedBox);
		
		//	return actions if selection not empty
		return ((selectedWords.length == 0) ? null : this.getActions(selectedWords, page, selectedBox));
	}
	
	private SelectionAction[] getActions(final ImWord[] words, final ImPage page, final BoundingBox selectedBox) {
		LinkedList actions = new LinkedList();
		
		//	mark selected words as a caption
		actions.add(new SelectionAction("markRegionCaption", "Mark Caption", "Mark selected words as a caption.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				
				//	cut out caption
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_CAPTION, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				
				//	annotate caption
				ImAnnotation caption = words[0].getDocument().addAnnotation(words[0], words[words.length-1], CAPTION_TYPE);
				caption.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				invoker.setAnnotationsPainted(CAPTION_TYPE, true);
				BoundingBox captionBox = ImLayoutObject.getAggregateBox(words);
				
				//	do we have a table caption or a figure caption?
				boolean isTableCaption = words[0].getString().toLowerCase().startsWith("tab"); // covers most Latin based languages
				
				//	find possible targets
				ImRegion[] targets;
				if (isTableCaption)
					targets = page.getRegions(ImRegion.TABLE_TYPE);
				else {
					targets = page.getRegions(ImRegion.IMAGE_TYPE);
					if (targets.length == 0)
						targets = page.getRegions(ImRegion.BLOCK_ANNOTATION_TYPE);
				}
				if (targets.length == 0)
					return true;
				
				//	assign target
				Arrays.sort(targets, ImUtils.topDownOrder);
				PageImage pi = page.getImage();
				for (int i = 0; i < targets.length; i++) {
					
					//	check vertical alignment
					if (!isTableCaption && (captionBox.top < targets[i].bounds.bottom))
						break; // due to top-down sort order, we won't find any matches from here onward
					
					//	TODO also search left and right if no images at all above and below
					
					//	check general alignment
					if (!ImUtils.isCaptionBelowTargetMatch(captionBox, targets[i].bounds, pi.currentDpi) && (!isTableCaption || !ImUtils.isCaptionAboveTargetMatch(captionBox, targets[i].bounds, pi.currentDpi)))
						continue;
					
					//	check size and words if using block fallback
					if (ImRegion.BLOCK_ANNOTATION_TYPE.equals(targets[i].getType())) {
						if ((targets[i].bounds.right - targets[i].bounds.left) < pi.currentDpi)
							continue;
						if ((targets[i].bounds.bottom - targets[i].bounds.top) < pi.currentDpi)
							continue;
						ImWord[] imageWords = targets[i].getWords();
						if (imageWords.length != 0)
							continue;
					}
					
					//	link image to caption
					caption.setAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE, ("" + targets[i].pageId));
					caption.setAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE, targets[i].bounds.toString());
					if (isTableCaption)
						caption.setAttribute("targetIsTable");
					break;
				}
				
				//	finally ...
				return true;
			}
		});
		
		//	mark selected words as a footnote
		actions.add(new SelectionAction("markRegionFootnote", "Mark Footnote", "Mark selected words as a footnote.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_FOOTNOTE, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				ImAnnotation footnote = words[0].getDocument().addAnnotation(words[0], words[words.length-1], FOOTNOTE_TYPE);
				footnote.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				invoker.setAnnotationsPainted(FOOTNOTE_TYPE, true);
				return true;
			}
		});
		
		//	mark selected words as a page header
		actions.add(new SelectionAction("markRegionPageHeader", "Mark Page Header", "Mark selected words as a page header or footer.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_PAGE_TITLE, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				ImAnnotation pageTitle = words[0].getDocument().addAnnotation(words[0], words[words.length-1], PAGE_TITLE_TYPE);
				pageTitle.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				invoker.setAnnotationsPainted(PAGE_TITLE_TYPE, true);
				return true;
			}
		});
		
		//	mark selected words as a page header
		actions.add(new SelectionAction("markRegionParenthesis", "Mark Parenthesis", "Mark selected words as a parenthesis, e.g. a standalone text box or a note.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_MAIN_TEXT, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				ImAnnotation pageTitle = words[0].getDocument().addAnnotation(words[0], words[words.length-1], PARENTHESIS_TYPE);
				pageTitle.getLastWord().setNextRelation(ImWord.NEXT_RELATION_PARAGRAPH_END);
				invoker.setAnnotationsPainted(PARENTHESIS_TYPE, true);
				return true;
			}
		});
		
		//	mark selected words as an artifact
		actions.add(new SelectionAction("markRegionArtifact", "Mark Artifact", "Mark selected words as an OCR or layout artifact.") {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_ARTIFACT, null);
				ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
				for (ImWord imw = words[words.length-1]; imw != null; imw = imw.getPreviousWord())
					imw.setNextWord(null);
				return true;
			}
		});
		
		//	mark selected non-white area as image (case without words comes from region actions)
		if ((page != null) && (selectedBox != null))
			actions.add(new SelectionAction("markRegionImage", "Mark Image", "Mark selected region as an image.") {
				public boolean performAction(ImDocumentMarkupPanel invoker) {
					
					//	remove words
					ImUtils.makeStream(words, ImWord.TEXT_STREAM_TYPE_ARTIFACT, null);
					ImUtils.orderStream(words, ImUtils.leftRightTopDownOrder);
					for (ImWord imw = words[words.length-1]; imw != null; imw = imw.getPreviousWord())
						imw.setNextWord(null);
					for (int w = 0; w < words.length; w++)
						page.removeWord(words[w], true);
					
					//	shrink selection
					PageImage pi = page.getImage();
					AnalysisImage ai = Imaging.wrapImage(pi.image, null);
					ImagePartRectangle ipr = Imaging.getContentBox(ai);
					ImagePartRectangle selectedIpr = ipr.getSubRectangle(Math.max(selectedBox.left, page.bounds.left), Math.min(selectedBox.right, page.bounds.right), Math.max(selectedBox.top, page.bounds.top), Math.min(selectedBox.bottom, page.bounds.bottom));
					selectedIpr = Imaging.narrowLeftAndRight(selectedIpr);
					selectedIpr = Imaging.narrowTopAndBottom(selectedIpr);
					BoundingBox imageBox = new BoundingBox(selectedIpr.getLeftCol(), selectedIpr.getRightCol(), selectedIpr.getTopRow(), selectedIpr.getBottomRow());
					
					//	clean up nested regions
					ImRegion[] selectedRegions = page.getRegionsInside(selectedBox, true);
					for (int r = 0; r < selectedRegions.length; r++) {
						if (!imageBox.liesIn(selectedRegions[r].bounds, false))
							page.removeRegion(selectedRegions[r]);
					}
					
					//	mark image
					ImRegion image = new ImRegion(page, imageBox, ImRegion.IMAGE_TYPE);
					invoker.setRegionsPainted(ImRegion.IMAGE_TYPE, true);
					
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
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
}