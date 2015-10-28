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
package de.uka.ipd.idaho.im.imagine.plugins.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImSupplement;
import de.uka.ipd.idaho.im.ImSupplement.Figure;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractReactionProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImUtils;

/**
 * This plugin listens to changes to captions marked in a document, marks
 * their citations, and keeps the attributes of the latter in sync with the
 * referenced captions.
 * 
 * @author sautter
 */
public class CaptionCitationHandler extends AbstractReactionProvider implements ImagingConstants {
	
	/** public zero-argument constructor for class loading */
	public CaptionCitationHandler() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Caption Citation Handler";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#typeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void typeChanged(ImObject object, String oldType, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#attributeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, java.lang.Object, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void attributeChanged(ImObject object, String attributeName, Object oldValue, ImDocumentMarkupPanel idmpg, boolean allowPrompt) {
		
		//	write through modifications of a caption annotation to figure and table citations
		if ((object instanceof ImAnnotation) && CAPTION_TYPE.equals(object.getType())) {
			
			//	are we interested in this update?
			if (true 
				&& !CAPTION_TARGET_BOX_ATTRIBUTE.equals(attributeName)
				&& !CAPTION_TARGET_PAGE_ID_ATTRIBUTE.equals(attributeName)
				&& !ImAnnotation.FIRST_WORD_ATTRIBUTE.equals(attributeName)
				&& !ImAnnotation.LAST_WORD_ATTRIBUTE.equals(attributeName)
				&& !"httpUri".equals(attributeName)
				)
				return;
			
			//	get document
			ImDocument doc = object.getDocument();
			if (doc == null)
				return;
			
			//	get caption attributes
			ImAnnotation caption = ((ImAnnotation) object);
			ImWord captionStartIdWord = (((oldValue instanceof ImWord) && ImAnnotation.FIRST_WORD_ATTRIBUTE.equals(attributeName)) ? ((ImWord) oldValue) : caption.getFirstWord());
			boolean captionStartIdWordIsTable = captionStartIdWord.getString().toLowerCase().startsWith("tab");
			
			//	get affected caption citations
			Map captionCitations = this.findCaptionCitations(captionStartIdWord, captionStartIdWordIsTable, doc);
			if (captionCitations.isEmpty())
				return;
			
			//	produce update attribute values
			String captionStartId = caption.getFirstWord().getLocalID();
			String captionStart = this.getCaptionStart(caption);
			String captionText = ImUtils.getString(caption.getFirstWord(), caption.getLastWord(), true);
			String captionTargetPageId = ((String) caption.getAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE));
			if (captionTargetPageId == null)
				return;
			String captionTargetBoxString = ((String) caption.getAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE));
			if (captionTargetBoxString == null)
				return;
			String captionTargetId = this.getCaptionTargetSupplementId(doc.getPage(Integer.parseInt(captionTargetPageId)), BoundingBox.parse(captionTargetBoxString));
			boolean captionStartWordIsTable = caption.getFirstWord().getString().toLowerCase().startsWith("tab");
			
			//	update caption citations
			for (Iterator ccit = captionCitations.keySet().iterator(); ccit.hasNext();) {
				ImAnnotation captionCitation = ((ImAnnotation) ccit.next());
				String attributeNameSuffix = ((String) captionCitations.get(captionCitation));
				
				//	modify attribute caption target attributes
				if (CAPTION_TARGET_BOX_ATTRIBUTE.equals(attributeName)) {
					captionCitation.setAttribute(("captionTargetBox" + attributeNameSuffix), captionTargetBoxString);
					if (captionTargetId != null)
						captionCitation.setAttribute(("captionTargetId" + attributeNameSuffix), captionTargetId);
				}
				else if (CAPTION_TARGET_PAGE_ID_ATTRIBUTE.equals(attributeName)) {
					captionCitation.setAttribute(("captionTargetPageId" + attributeNameSuffix), captionTargetPageId);
					if (captionTargetId != null)
						captionCitation.setAttribute(("captionTargetId" + attributeNameSuffix), captionTargetId);
				}
				else if ("httpUri".equals(attributeName))
					captionCitation.setAttribute(("httpUri" + attributeNameSuffix), object.getAttribute(attributeName));
				
				//	modify attributes changing with caption start and end
				else if (ImAnnotation.FIRST_WORD_ATTRIBUTE.equals(attributeName)) {
					captionCitation.setAttribute(("captionStartId" + attributeNameSuffix), captionStartId);
					captionCitation.setAttribute(("captionStart" + attributeNameSuffix), captionStart);
					captionCitation.setAttribute(("captionText" + attributeNameSuffix), captionText);
					if (captionStartIdWordIsTable != captionStartWordIsTable)
						captionCitation.setType((captionStartWordIsTable ? ImRegion.TABLE_TYPE : "figure") + CITATION_TYPE_SUFFIX);
				}
				else if (ImAnnotation.LAST_WORD_ATTRIBUTE.equals(attributeName)) {
					captionCitation.setAttribute(("captionStart" + attributeNameSuffix), captionStart);
					captionCitation.setAttribute(("captionText" + attributeNameSuffix), captionText);
				}
			}
		}
		
		//	write through modifications of a caption word to figure and table citations
		else if ((object instanceof ImWord) && ImWord.TEXT_STREAM_TYPE_CAPTION.equals(((ImWord) object).getTextStreamType()) && ImWord.STRING_ATTRIBUTE.equals(attributeName)) {
			
			//	get document
			ImDocument doc = object.getDocument();
			if (doc == null)
				return;
			
			//	find affected caption
			ImAnnotation[] wordCaptions = doc.getAnnotationsSpanning((ImWord) object);
			if (wordCaptions.length != 1)
				return;
			
			//	get caption attributes
			ImAnnotation caption = wordCaptions[0];
			boolean captionStartIdWordIsTable = ((caption.getFirstWord() == ((ImWord) object)) ? ((String) oldValue) : caption.getFirstWord().getString()).toLowerCase().startsWith("tab");
			
			//	get affected caption citations
			Map captionCitations = this.findCaptionCitations(caption.getFirstWord(), captionStartIdWordIsTable, doc);
			if (captionCitations.isEmpty())
				return;
			
			//	produce update attribute values
			String captionStart = this.getCaptionStart(caption);
			String captionText = ImUtils.getString(caption.getFirstWord(), caption.getLastWord(), true);
			boolean captionStartWordIsTable = caption.getFirstWord().getString().toLowerCase().startsWith("tab");
			
			//	update caption citations
			for (Iterator ccit = captionCitations.keySet().iterator(); ccit.hasNext();) {
				ImAnnotation captionCitation = ((ImAnnotation) ccit.next());
				String attributeNameSuffix = ((String) captionCitations.get(captionCitation));
				
				//	modify attributes changing with caption start and end
				captionCitation.setAttribute(("captionStart" + attributeNameSuffix), captionStart);
				captionCitation.setAttribute(("captionText" + attributeNameSuffix), captionText);
				if (captionStartIdWordIsTable != captionStartWordIsTable)
					captionCitation.setType((captionStartWordIsTable ? ImRegion.TABLE_TYPE : "figure") + CITATION_TYPE_SUFFIX);
			}
		}
		
		if (!CAPTION_TARGET_BOX_ATTRIBUTE.equals(attributeName) && !CAPTION_TARGET_PAGE_ID_ATTRIBUTE.equals(attributeName))
			return;
		if (!object.hasAttribute(attributeName))
			return;
		
		//	get document
		ImDocument doc = object.getDocument();
		if (doc == null)
			return;
		
		//	get caption, identifying start string, and caption citations
		ImAnnotation caption = ((ImAnnotation) object);
		ImAnnotation[] captionCitations = doc.getAnnotations((caption.getFirstWord().getString().toLowerCase().startsWith("tab") ? ImRegion.TABLE_TYPE : "figure") + CITATION_TYPE_SUFFIX);
		
		//	work off citations one by one
		for (int cc = 0; cc < captionCitations.length; cc++) {
			String attributeNameSuffix = null;
			
			//	find affected attribute group
			if (captionCitations[cc].hasAttribute("captionStartId-0"))
				for (int s = 0;; s++) {
					if (!captionCitations[cc].hasAttribute("captionStartId-" + s))
						break;
					if (caption.getFirstWord().getLocalID().equals(captionCitations[cc].getAttribute("captionStartId-" + s))) {
						attributeNameSuffix = ("-" + s);
						break;
					}
				}
			if (captionCitations[cc].hasAttribute("captionStartId") && caption.getFirstWord().getLocalID().equals(captionCitations[cc].getAttribute("captionStartId")))
				attributeNameSuffix = "";
			
			//	no caption start match
			if (attributeNameSuffix == null)
				continue;
			
			//	modify attribute
			if (CAPTION_TARGET_BOX_ATTRIBUTE.equals(attributeName))
				captionCitations[cc].setAttribute(("captionTargetBox" + attributeNameSuffix), caption.getAttribute(CAPTION_TARGET_BOX_ATTRIBUTE));
			else if (CAPTION_TARGET_PAGE_ID_ATTRIBUTE.equals(attributeName))
				captionCitations[cc].setAttribute(("captionTargetPageId" + attributeNameSuffix), caption.getAttribute(CAPTION_TARGET_PAGE_ID_ATTRIBUTE));
		}
	}
	
	private Map findCaptionCitations(ImWord captionStart, boolean isTableCaption, ImDocument doc) {
		ImAnnotation[] allCaptionCitations = doc.getAnnotations((isTableCaption ? ImRegion.TABLE_TYPE : "figure") + CITATION_TYPE_SUFFIX);
		HashMap captionCitationsToAttributeNameSuffixes = new HashMap();
		for (int cc = 0; cc < allCaptionCitations.length; cc++) {
			String attributeNameSuffix = null;
			
			//	find affected attribute group
			if (allCaptionCitations[cc].hasAttribute("captionStartId-0"))
				for (int s = 0;; s++) {
					if (!allCaptionCitations[cc].hasAttribute("captionStartId-" + s))
						break;
					if (captionStart.getLocalID().equals(allCaptionCitations[cc].getAttribute("captionStartId-" + s))) {
						attributeNameSuffix = ("-" + s);
						break;
					}
				}
			if (captionStart.getLocalID().equals(allCaptionCitations[cc].getAttribute("captionStartId")))
				attributeNameSuffix = "";
			
			//	map caption citation to attribute suffix for further handling
			if (attributeNameSuffix != null)
				captionCitationsToAttributeNameSuffixes.put(allCaptionCitations[cc], attributeNameSuffix);
		}
		
		//	return what we got
		return captionCitationsToAttributeNameSuffixes;
	}
	
	private String getCaptionStart(ImAnnotation caption) {
		ImWord captionStartEnd = null;
		boolean lastWasIndex = false;
		for (ImWord imw = caption.getFirstWord().getNextWord(); imw != null; imw = imw.getNextWord()) {
			String imwString = imw.getString();
			if ((imwString == null) || (imwString.trim().length() == 0))
				continue;
			
			//	index number or letter
			if (imwString.matches("([0-9]+(\\s*[a-z])?)|[IiVvXxLl]+|[a-zA-Z]")) {
				captionStartEnd = imw;
				lastWasIndex = true;
			}
			
			//	enumeration separator or range marker
			else if (imwString.equals(",") || imwString.equals("&") || (";and;und;et;y;".indexOf(";" + imwString.toLowerCase() + ";") != -1) || imwString.matches("[\\-\\u00AD\\u2010-\\u2015\\u2212]+")) {
				if (!lastWasIndex) // no enumeration or range open, we're done here
					break;
				lastWasIndex = false;
			}
			
			//	ignore dots
			else if (!".".equals(imwString))
				break;
		}
		
		//	finally ...
		return ImUtils.getString(caption.getFirstWord(), ((captionStartEnd == null) ? caption.getLastWord() : captionStartEnd), true);
	}
	
	private String getCaptionTargetSupplementId(ImPage targetPage, BoundingBox captionTargetBox) {
		ImSupplement[] targetPageSupplements = targetPage.getSupplements();
		for (int s = 0; s < targetPageSupplements.length; s++) {
			if (!(targetPageSupplements[s] instanceof Figure))
				continue;
			BoundingBox pageSupplementBox = ((Figure) targetPageSupplements[s]).getBounds();
			if (pageSupplementBox == null)
				continue;
			if (captionTargetBox.liesIn(pageSupplementBox, true) && pageSupplementBox.liesIn(captionTargetBox, true))
				return targetPageSupplements[s].getId();
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#regionAdded(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionAdded(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#regionRemoved(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionRemoved(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationAdded(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationAdded(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		
		//	we're only interested in captions
		if (!CAPTION_TYPE.equals(annotation.getType()))
			return;
		
		//	get document
		ImDocument doc = annotation.getDocument();
		if (doc == null)
			return;
		
		//	collect caption start words, and generate all their prefixes
		TreeMap captionStartsToNumbers = this.getCaptionStartNumberIndex(doc);
		
		//	tag and connect caption citations TODO use caption citation properties from document style (patterns, first and foremost)
		ImWord[] textStreamHeads = doc.getTextStreamHeads();
		for (int h = 0; h < textStreamHeads.length; h++) {
			if (ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(textStreamHeads[h].getTextStreamType()))
				this.markCaptionCitations(doc, textStreamHeads[h], captionStartsToNumbers);
		}
		
		//	remove nested caption citations
		this.removeNestedAnnotations(doc, doc.getAnnotations("figure" + CITATION_TYPE_SUFFIX));
		this.removeNestedAnnotations(doc, doc.getAnnotations(ImRegion.TABLE_TYPE + CITATION_TYPE_SUFFIX));
	}
	
	private void removeNestedAnnotations(ImDocument doc, ImAnnotation[] annots) {
		if (annots.length < 2)
			return;
		Arrays.sort(annots, annotationNestingOrder);
		ImAnnotation lastAnnot = annots[0];
		for (int a = 1; a < annots.length; a++) {
			
			//	different text streams, cannot be nested
			if (!lastAnnot.getFirstWord().getTextStreamId().equals(annots[a].getFirstWord().getTextStreamId())) {
				lastAnnot = annots[a];
				continue;
			}
			
			//	no overlap
			if (ImUtils.textStreamOrder.compare(lastAnnot.getLastWord(), annots[a].getFirstWord()) < 0) {
				lastAnnot = annots[a];
				continue;
			}
			
			//	non-including overlap, merge
			if (ImUtils.textStreamOrder.compare(lastAnnot.getLastWord(), annots[a].getLastWord()) < 0) {
				lastAnnot.setLastWord(annots[a].getLastWord());
				AttributeUtils.copyAttributes(lastAnnot, annots[a], AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
			}
			
			//	remove nested annotation
			doc.removeAnnotation(annots[a]);
		}
	}
	
	private static final Comparator annotationNestingOrder = new Comparator() {
		public int compare(Object obj1, Object obj2) {
			ImAnnotation annot1 = ((ImAnnotation) obj1);
			ImAnnotation annot2 = ((ImAnnotation) obj2);
			int c = ImUtils.textStreamOrder.compare(annot1.getFirstWord(), annot2.getFirstWord());
			return ((c == 0) ? ImUtils.textStreamOrder.compare(annot2.getLastWord(), annot1.getLastWord()) : c);
		}
	};
	
	/**
	 * Mark caption citations in a document. This method expects captions to be
	 * already marked, seeking only citations of theirs.
	 * @param doc the document to process
	 */
	public void markCaptionCitations(ImDocument doc) {
		this.markCaptionCitations(doc, doc.getTextStreamHeads());
	}
	
	/**
	 * Mark caption citations in a document. This method expects captions to be
	 * already marked, seeking only citations of theirs.
	 * @param doc the document to process
	 * @param textStreamHeads the heads of the logical text streams to analyze
	 */
	public void markCaptionCitations(ImDocument doc, ImWord[] textStreamHeads) {
		
		//	collect caption start words, and generate all their prefixes
		TreeMap captionStartsToNumbers = this.getCaptionStartNumberIndex(doc);
		
		//	tag and connect caption citations TODO use caption citation properties from document style (patterns, first and foremost)
		for (int h = 0; h < textStreamHeads.length; h++) {
			if (ImWord.TEXT_STREAM_TYPE_MAIN_TEXT.equals(textStreamHeads[h].getTextStreamType()))
				this.markCaptionCitations(doc, textStreamHeads[h], captionStartsToNumbers);
		}
	}
	
	private TreeMap getCaptionStartNumberIndex(ImDocument doc) {
		TreeMap captionStartsToNumbers = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		
		//	go caption by caption
		ImAnnotation[] captions = doc.getAnnotations(CAPTION_TYPE);
		for (int c = 0; c < captions.length; c++) {
			System.out.println("Analyzing caption (page " + captions[c].getFirstWord().pageId + ") '" + ImUtils.getString(captions[c].getFirstWord(), captions[c].getLastWord(), true) + "'");
			String captionStart = null;
			int minNumber = -1;
			int maxNumber = -1;
			boolean numberRange = false;
			int minLetter = -1;
			int maxLetter = -1;
			int letterBase = 0;
			boolean letterRange = false;
			for (ImWord imw = captions[c].getFirstWord(); imw != null; imw = imw.getNextWord()) {
				String imwString = imw.getString();
				if (imwString == null)
					continue;
				
				//	mark caption start on first word, quit at next word
				if ((imwString.length() >= 3) && Gamta.isWord(imwString)) {
					if (captionStart == null) {
						captionStart = imwString;
						continue;
					}
					else break;
				}
				
				//	nothing to work with yet
				if (captionStart == null)
					continue;
				
				//	number
				if (imwString.matches("[1-9][0-9]{0,2}")) {
					if (minNumber == -1) // start of range
						minNumber = Integer.parseInt(imwString);
					else if (!numberRange) // no range expected, quit
						break;
					else if (maxNumber == -1) // end of range
						maxNumber = Integer.parseInt(imwString);
					else break; // we've had enough numbers here
				}
				
				//	lower case index letter
				else if (imwString.matches("[a-z]")) {
					if (minLetter == -1) { // start of range
						minLetter = (Character.toLowerCase(imwString.charAt(0)) - 'a');
						letterBase = 'a';
					}
					else if (!letterRange) // no range expected, quit
						break;
					else if (maxLetter == -1) // end of range
						maxLetter = Character.toLowerCase(imwString.charAt(0)) - 'a';
					else break; // we've had enough index letters here
				}
				
				//	upper case index letter
				else if (imwString.matches("[A-Z]")) {
					if (minLetter == -1) { // start of range
						minLetter = (Character.toLowerCase(imwString.charAt(0)) - 'A');
						letterBase = 'A';
					}
					else if (!letterRange) // no range expected, quit
						break;
					else if (maxLetter == -1) // end of range
						maxLetter = Character.toLowerCase(imwString.charAt(0)) - 'A';
					else break; // we've had enough index letters here
				}
				
				//	separator
				else if (imwString.matches("[\\,\\&\\-\\u00AD\\u2010-\\u2015\\u2212]+")) {
					if ((minLetter != -1) && (maxLetter == -1)) // indicator of letter range
						letterRange = true;
					else if ((minNumber != -1) && (maxNumber == -1)) // indicator of number range
						numberRange = true;
					else break; // other punctuation, quit
				}
				
				//	ignore punctuation mark
				else if (".:,;".indexOf(imwString) == -1)
					break;
			}
			System.out.println(" - start is '" + captionStart + "'");
			System.out.println(" - min number is " + minNumber);
			System.out.println(" - max number is " + maxNumber);
			System.out.println(" - min letter is " + ((char) (minLetter + letterBase)));
			System.out.println(" - max letter is " + ((char) (maxLetter + letterBase)));
			
			//	do we have anything to work with?
			if (captionStart == null)
				continue;
			if ((minNumber == -1) && (minLetter == -1))
				continue;
			
			//	generate set of numbers associated with caption start
			TreeMap numbers = new TreeMap(String.CASE_INSENSITIVE_ORDER);
			
			//	index letters only
			if (minNumber == -1) {
				if (maxLetter == -1)
					numbers.put(("" + ((char) (minLetter + 'a'))), captions[c]);
				else for (int l = minLetter; l <= maxLetter; l++)
					numbers.put(("" + ((char) (l + 'a'))), captions[c]);
			}
			
			//	single number, possibly with index letters
			else if (maxNumber == -1) {
				numbers.put(("" + minNumber), captions[c]);
				if (maxLetter != -1) {
					for (int l = minLetter; l <= maxLetter; l++)
						numbers.put(("" + minNumber + "" + ((char) (l + letterBase))), captions[c]);
				}
				else if (minLetter != -1)
					numbers.put(("" + minNumber + "" + ((char) (minLetter + letterBase))), captions[c]);
			}
			
			//	number range
			else for (int n = minNumber; n <= maxNumber; n++)
				numbers.put(("" + n), captions[c]);
			
			//	generate caption start prefixes
			TreeSet captionStartPrefixes = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			for (int e = 3; e <= (captionStart.length() - (captionStart.toLowerCase().endsWith("s") ? 1 : 0)); e++) {
				captionStartPrefixes.add(captionStart.substring(0, e));
				captionStartPrefixes.add(captionStart.substring(0, e) + "s");
			}
			
			//	map caption start prefixes to numbers
			for (Iterator cspit = captionStartPrefixes.iterator(); cspit.hasNext();) {
				String captionStartPrefix = ((String) cspit.next());
				TreeMap captionStartNumbers = ((TreeMap) captionStartsToNumbers.get(captionStartPrefix));
				if (captionStartNumbers == null)
					captionStartsToNumbers.put(captionStartPrefix, numbers);
				else for (Iterator cnit = numbers.keySet().iterator(); cnit.hasNext();) {
					String number = ((String) cnit.next());
					if (!captionStartNumbers.containsKey(number))
						captionStartNumbers.put(number, captions[c]);
				}
			}
			System.out.println(" - got " + numbers.size() + " numbers:");
			for (Iterator cnit = numbers.keySet().iterator(); cnit.hasNext();)
				System.out.println("   - " + cnit.next());
		}
		System.out.println("Got captions:");
		for (Iterator csit = captionStartsToNumbers.keySet().iterator(); csit.hasNext();) {
			String captionStart = ((String) csit.next());
			System.out.print(" - " + captionStart);
			TreeMap captionStartNumbers = ((TreeMap) captionStartsToNumbers.get(captionStart));
			for (Iterator cnit = captionStartNumbers.keySet().iterator(); cnit.hasNext();)
				System.out.print(" " + cnit.next());
			System.out.println();
		}
		
		//	finally ...
		return captionStartsToNumbers;
	}
	
	private static final boolean DEBUG_MARK_CAPTION_CITATIONS = false;
	private void markCaptionCitations(ImDocument doc, ImWord textStreamHead, TreeMap captionStartsToNumbers) {
		for (ImWord imw = textStreamHead; imw.getNextWord() != null; imw = imw.getNextWord()) {
			String imwString = imw.getString();
			if (imwString == null)
				continue;
			if (Character.toTitleCase(imwString.charAt(0)) != imwString.charAt(0))
				continue;
			TreeMap captionStartNumbers = ((TreeMap) captionStartsToNumbers.get(imwString));
			if (captionStartNumbers == null)
				continue;
			if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println("Starting caption citation with " + imwString + " on page " + imw.pageId + " at " + imw.bounds);
			imw = this.markCaptionCitation(doc, imw, captionStartNumbers);
		}
	}
	
	private ImWord markCaptionCitation(ImDocument doc, ImWord ccStart, TreeMap captionStartNumbers) {
		int lastNumber = -1;
		int lastLetter = -1;
		boolean lastWasLetter = false;
		CaptionCitation cc = null;
		ArrayList ccList = new ArrayList();
		
		//	try and find numbers, starting from successor of start word
		for (ImWord imw = ccStart.getNextWord(); imw != null; imw = imw.getNextWord()) {
			String imwString = imw.getString();
			if (imwString == null)
				continue;
			
			//	number
			if (imwString.matches("[1-9][0-9]{0,2}") && captionStartNumbers.containsKey(imwString)) {
				if ((lastNumber == -1) || (lastLetter != -1) || lastWasLetter) {
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got number " + imwString);
					cc = new CaptionCitation((ccList.isEmpty() ? ccStart : imw), imw);
					ccList.add(cc);
					ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get(imwString));
					if (citedCaption != null)
						cc.citedCaptions.add(citedCaption);
				}
				else {
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got number range " + lastNumber + "-" + imwString);
					cc.lastWord = imw;
					for (int n = lastNumber; n <= Integer.parseInt(imwString); n++) {
						ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get("" + n));
						if (citedCaption != null)
							cc.citedCaptions.add(citedCaption);
					}
				}
				lastNumber = Integer.parseInt(imwString);
				lastLetter = -1;
				lastWasLetter = false;
			}
			
			//	lower case index letter
			else if (imwString.matches("[a-z]") && (captionStartNumbers.containsKey(imwString) || captionStartNumbers.containsKey("" + lastNumber + imwString))) {
				if (lastLetter == -1) {
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got letter " + imwString);
					if (cc == null) { // letters only, no caption citation open
						cc = new CaptionCitation((ccList.isEmpty() ? ccStart : imw), imw);
						ccList.add(cc);
					}
					else cc.lastWord = imw; // combination of numbers and letters, don't overwrite number start
					ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get(imwString));
					if (citedCaption == null)
						citedCaption = ((ImAnnotation) captionStartNumbers.get("" + lastNumber + imwString));
					if (citedCaption != null)
						cc.citedCaptions.add(citedCaption);
				}
				else {
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got letter range " + ((char) (lastLetter + 'a')) + "-" + imwString);
					cc.lastWord = imw;
					for (int l = lastLetter; l <= (imwString.charAt(0) - 'a'); l++) {
						ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get("" + ((char) (l + 'a'))));
						if (citedCaption == null)
							citedCaption = ((ImAnnotation) captionStartNumbers.get("" + lastNumber + ((char) (l + 'a'))));
						if (citedCaption != null)
							cc.citedCaptions.add(citedCaption);
					}
				}
				lastLetter = (imwString.charAt(0) - 'a');
				lastWasLetter = true;
			}
			
			//	upper case index letter
			else if (imwString.matches("[A-Z]") && (captionStartNumbers.containsKey(imwString) || captionStartNumbers.containsKey("" + lastNumber + imwString))) {
				if (lastLetter == -1) {
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got letter " + imwString);
					if (cc == null) { // letters only, no caption citation open
						cc = new CaptionCitation((ccList.isEmpty() ? ccStart : imw), imw);
						ccList.add(cc);
					}
					else cc.lastWord = imw; // combination of numbers and letters, don't overwrite number start
					ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get(imwString));
					if (citedCaption == null)
						citedCaption = ((ImAnnotation) captionStartNumbers.get("" + lastNumber + imwString));
					if (citedCaption != null)
						cc.citedCaptions.add(citedCaption);
				}
				else {
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got letter range " + ((char) (lastLetter + 'A')) + "-" + imwString);
					cc.lastWord = imw;
					for (int l = lastLetter; l <= (imwString.charAt(0) - 'A'); l++) {
						ImAnnotation citedCaption = ((ImAnnotation) captionStartNumbers.get("" + ((char) (l + 'A'))));
						if (citedCaption == null)
							citedCaption = ((ImAnnotation) captionStartNumbers.get("" + lastNumber + ((char) (l + 'A'))));
						if (citedCaption != null)
							cc.citedCaptions.add(citedCaption);
					}
				}
				lastLetter = (imwString.charAt(0) - 'A');
				lastWasLetter = true;
			}
			
			//	enumeration separator
			else if (imwString.equals(",") || imwString.equals("&") || (";and;und;et;y;".indexOf(";" + imwString.toLowerCase() + ";") != -1)) {
				cc = null;
				if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got enumeration separator " + imwString);
				if (lastLetter != -1) {// clear last letter
					lastLetter = -1;
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println("   ==> letter range start reset");
				}
				else if (lastNumber != -1) {// clear last number
					lastNumber = -1;
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println("   ==> number range start reset");
				}
				else {
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println("   ==> no enumeration open, end of caption citation sequence");
					break; // nothing to clear, we're done here
				}
			}
			
			//	range marker
			else if (imwString.matches("[\\-\\u00AD\\u2010-\\u2015\\u2212]+")) {
				if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - got range separator " + imwString);
				if ((lastNumber == -1) && (lastLetter == -1)) {// no range open, we're done here
					if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println("   ==> no range open, end of caption citation sequence");
					break;
				}
			}
			
			//	ignore dots
			else if (!".".equals(imwString)) {
				if (DEBUG_MARK_CAPTION_CITATIONS) System.out.println(" - end of caption citation sequence at " + imwString);
				break;
			}
		}
		
		//	anything to work with at all?
		if (ccList.isEmpty())
			return ccStart;
		if (DEBUG_MARK_CAPTION_CITATIONS) {
			System.out.println(" ==> found caption citation sequence '" + ImUtils.getString(ccStart, ((CaptionCitation) ccList.get(ccList.size()-1)).lastWord, true) + "' with " + ccList.size() + " caption citations:");
			for (int c = 0; c < ccList.size(); c++) {
				cc = ((CaptionCitation) ccList.get(c));
				System.out.println("   - " + ImUtils.getString(cc.firstWord, cc.lastWord, true) + " (" + cc.citedCaptions.size() + " cited captions)");
			}
		}
		
		//	merge caption citations if cited captions overlap or are empty on either side
		for (int c = 0; c < (ccList.size() - 1); c++) {
			cc = ((CaptionCitation) ccList.get(c));
			CaptionCitation ncc = ((CaptionCitation) ccList.get(c+1));
			boolean mergeCcs = false;
			if (cc.citedCaptions.isEmpty())
				mergeCcs = true;
			else if (ncc.citedCaptions.isEmpty())
				mergeCcs = true;
			else for (Iterator nccit = ncc.citedCaptions.iterator(); nccit.hasNext();)
				if (cc.citedCaptions.contains(nccit.next())) {
					mergeCcs = true;
					break;
				}
			if (mergeCcs) {
				cc.lastWord = ncc.lastWord;
				cc.citedCaptions.addAll(ncc.citedCaptions);
				ccList.remove(c+1);
				c--;
			}
		}
		
		//	check if any cited caption found to link to
		for (int c = 0; c < ccList.size(); c++) {
			cc = ((CaptionCitation) ccList.get(c));
			if (cc.citedCaptions.isEmpty())
				ccList.remove(c--);
		}
		
		//	anything to reference?
		if (ccList.isEmpty())
			return ccStart;
		if (DEBUG_MARK_CAPTION_CITATIONS) {
			System.out.println(" ==> got " + ccList.size() + " caption citations after merging and cleanup:");
			for (int c = 0; c < ccList.size(); c++) {
				cc = ((CaptionCitation) ccList.get(c));
				System.out.println("   - " + ImUtils.getString(cc.firstWord, cc.lastWord, true) + " (" + cc.citedCaptions.size() + " cited captions)");
			}
		}
		
		//	mark caption citation, distinguishing table citations from figure citations
		for (int c = 0; c < ccList.size(); c++) {
			cc = ((CaptionCitation) ccList.get(c));
			ImAnnotation captionCitation = doc.addAnnotation(cc.firstWord, cc.lastWord, ((ccStart.getString().toLowerCase().startsWith("tab") ? ImRegion.TABLE_TYPE : "figure") + CITATION_TYPE_SUFFIX));
			int ccIndex = 0;
			for (Iterator ccit = cc.citedCaptions.iterator(); ccit.hasNext();) {
				ImAnnotation citedCaption = ((ImAnnotation) ccit.next());
				String captionStart = this.getCaptionStart(citedCaption);
				String captionText = ImUtils.getString(citedCaption.getFirstWord(), citedCaption.getLastWord(), true);
				String captionTargetPageId = ((String) citedCaption.getAttribute(ImAnnotation.CAPTION_TARGET_PAGE_ID_ATTRIBUTE));
				BoundingBox captionTargetBox = BoundingBox.parse((String) citedCaption.getAttribute(ImAnnotation.CAPTION_TARGET_BOX_ATTRIBUTE));
				String captionHttpUri = ((String) citedCaption.getAttribute("httpUri"));
				if (cc.citedCaptions.size() == 1) {
					captionCitation.setAttribute("captionStartId", citedCaption.getFirstWord().getLocalID());
					captionCitation.setAttribute("captionStart", captionStart);
					captionCitation.setAttribute("captionText", captionText);
					if ((captionTargetPageId != null) && (captionTargetBox != null)) {
						captionCitation.setAttribute("captionTargetPageId", captionTargetPageId);
						captionCitation.setAttribute("captionTargetBox", captionTargetBox.toString());
						String captionTargetId = this.getCaptionTargetSupplementId(citedCaption.getDocument().getPage(Integer.parseInt(captionTargetPageId)), captionTargetBox);
						if (captionTargetId != null)
							captionCitation.setAttribute("captionTargetId", captionTargetId);
					}
					if (captionHttpUri != null)
						captionCitation.setAttribute("httpUri", captionHttpUri);
				}
				else {
					int cci = ccIndex++;
					captionCitation.setAttribute(("captionStartId-" + cci), citedCaption.getFirstWord().getLocalID());
					captionCitation.setAttribute(("captionStart-" + cci), captionStart);
					captionCitation.setAttribute(("captionText-" + cci), captionText);
					if ((captionTargetPageId != null) && (captionTargetBox != null)) {
						captionCitation.setAttribute(("captionTargetPageId-" + cci), captionTargetPageId);
						captionCitation.setAttribute(("captionTargetBox-" + cci), captionTargetBox.toString());
						String captionTargetId = this.getCaptionTargetSupplementId(citedCaption.getDocument().getPage(Integer.parseInt(captionTargetPageId)), captionTargetBox);
						if (captionTargetId != null)
							captionCitation.setAttribute(("captionTargetId-" + cci), captionTargetId);
					}
					if (captionHttpUri != null)
						captionCitation.setAttribute(("httpUri-" + cci), captionHttpUri);
				}
			}
			if (DEBUG_MARK_CAPTION_CITATIONS) {
				ImDocumentRoot ccDoc = new ImDocumentRoot(captionCitation, ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS);
				Annotation[] ccAnnot = ccDoc.getAnnotations(captionCitation.getType());
				if (ccAnnot.length != 0)
					System.out.println(" - " + ccAnnot[0].toXML());
			}
		}
		
		//	return last word of last caption citation
		return ((CaptionCitation) ccList.get(ccList.size()-1)).lastWord;
	}
	
	private class CaptionCitation {
		ImWord firstWord;
		ImWord lastWord;
		LinkedHashSet citedCaptions = new LinkedHashSet();
		CaptionCitation(ImWord firstWord, ImWord lastWord) {
			this.firstWord = firstWord;
			this.lastWord = lastWord;
		}
	}
}