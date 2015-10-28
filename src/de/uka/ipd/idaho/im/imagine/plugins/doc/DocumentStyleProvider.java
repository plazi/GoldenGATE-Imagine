/*
< * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.DocumentStyle;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.imagine.GoldenGateImagine;
import de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefConstants;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils;
import de.uka.ipd.idaho.plugins.bibRefs.BibRefUtils.RefData;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * This plug-in provides document style parameter lists to others.
 * 
 * @author sautter
 */
public class DocumentStyleProvider extends AbstractResourceManager implements GoldenGateImaginePlugin, DocumentStyle.Provider, BibRefConstants {
	private Map docStylesByName = Collections.synchronizedMap(new TreeMap());
	
	/** zero-argument constructor for class loading */
	public DocumentStyleProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Document Style Provider";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Document Style";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin#setImagineParent(de.uka.ipd.idaho.im.imagine.GoldenGateImagine)
	 */
	public void setImagineParent(GoldenGateImagine ggImagine) { /* we don't really need GG Imagine, at least for now */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	register as document style provider
		DocumentStyle.addProvider(this);
		
		//	load and index document styles
		String[] docStyleNames = this.getResourceNames();
		for (int s = 0; s < docStyleNames.length; s++) {
			Settings dsParamList = this.loadSettingsResource(docStyleNames[s]);
			if ((dsParamList != null) && (dsParamList.size() != 0)) {
				dsParamList.setSetting(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE, docStyleNames[s].substring(0, docStyleNames[s].lastIndexOf('.')));
				this.docStylesByName.put(docStyleNames[s], new DocStyle(dsParamList));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin#initImagine()
	 */
	public void initImagine() { /* nothing to initialize */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return ".docStyle";
	}
	
	Settings getStyle(String dsName) {
		if (!dsName.endsWith(".docStyle"))
			dsName += ".docStyle";
		
		//	return pre-loaded style first, so updates go to (possibly already shared) instance
		DocStyle docStyle = ((DocStyle) this.docStylesByName.get(dsName));
		if (docStyle != null)
			return docStyle.paramList;
		
		//	try and load new style not currently held in memory
		Settings dsParamList = this.loadSettingsResource(dsName);
		if (dsParamList != null) {
			dsParamList.setSetting(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE, dsName.substring(0,dsName.lastIndexOf('.')));
			this.docStylesByName.put(dsName, new DocStyle(dsParamList));
		}
		return dsParamList;
	}
	
	void storeStyle(String dsName, Settings dsParamList) {
		if (!dsName.endsWith(".docStyle"))
			dsName += ".docStyle";
		try {
			this.storeSettingsResource(dsName, dsParamList);
			//	update rather than replace, so changes become available wherever existing style object is in use
			DocStyle docStyle = ((DocStyle) this.docStylesByName.get(dsName));
			if (docStyle == null)
				this.docStylesByName.put(dsName, new DocStyle(dsParamList));
			else docStyle.setParamList(dsParamList);
		}
		catch (IOException ioe) {
			System.out.println("Error storing document style '" + dsName + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.plugins.docStyle.DocumentStyle.Provider#getStyleFor(de.uka.ipd.idaho.gamta.Attributed)
	 */
	public Properties getStyleFor(Attributed doc) {
		String docStyleName = ((String) doc.getAttribute(DocumentStyle.DOCUMENT_STYLE_NAME_ATTRIBUTE));
		
		//	no style clues at all, have to match
		if (docStyleName == null) {
			
			//	try anchor matches if argument is ImDocument
			if (doc instanceof ImDocument) {
				DocStyle docStyle = this.findStyleFor((ImDocument) doc);
				if (docStyle != null)
					return docStyle.toProperties();
			}
//			
//			//	TODO use this for tests
//			ResourceDialog dssd = ResourceDialog.getResourceDialog(this, "Select Document Style", "OK");
//			dssd.setLocationRelativeTo(dssd.getOwner());
//			dssd.setVisible(true);
//			if (dssd.isCommitted()) {
//				docStyleName = dssd.getSelectedResourceName();
//				if (docStyleName == null)
//					return null;
//				DocStyle docStyle = ((DocStyle) this.docStylesByName.get(docStyleName + ".docStyle"));
//				if (docStyle != null)
//					return docStyle.toProperties();
//			}
			
			return null;
		}
		
		//	try loading style directly by name first
//		Settings docStyle = this.loadSettingsResource(docStyleName + ".docStyle");
		DocStyle docStyle = ((DocStyle) this.docStylesByName.get(docStyleName + ".docStyle"));
		if (docStyle != null)
			return docStyle.toProperties();
		
		//	get bibliographic meta data if available
		RefData ref = BibRefUtils.modsAttributesToRefData(doc);
		String docYear = ref.getAttribute(YEAR_ANNOTATION_TYPE);
		if ((docYear != null) && (docStyleName.indexOf(docYear) != -1))
			docYear = null;
		String docType = ref.getAttribute(PUBLICATION_TYPE_ATTRIBUTE);
		if (docType != null)
			docType = docType.replaceAll("[^a-zA-Z]+", "_");
		if ((docType != null) && (docStyleName.indexOf(docType) != -1))
			docType = null;
		
		//	try and match name and year against available styles (sort descending to find closest style on year match)
//		String[] docStyleNames = this.dataProvider.getDataNames();
		String[] docStyleNames = ((String[]) this.docStylesByName.keySet().toArray(new String[this.docStylesByName.size()]));
		Arrays.sort(docStyleNames, Collections.reverseOrder());
		String bestDocStyleName = null;
		for (int n = 0; n < docStyleNames.length; n++) {
			if (!docStyleNames[n].startsWith(docStyleName + "."))
				continue;
			String dsn = docStyleNames[n].substring((docStyleName + ".").length());
			
			if (dsn.matches("[0-9]{4}\\..+")) {
				if ((docYear != null) && (docYear.compareTo(dsn.substring(0, 4)) < 0))
					continue;
				dsn = dsn.substring("0000.".length());
			}
			
			if ((docType != null) && dsn.startsWith(docType + ".")) {
//				docStyle = this.loadSettingsResource(docStyleNames[n]);
				docStyle = ((DocStyle) this.docStylesByName.get(docStyleNames[n]));
				if (docStyle != null)
					return docStyle.toProperties();
			}
			
			if ((bestDocStyleName == null) || (docStyleNames[n].length() < bestDocStyleName.length()))
				bestDocStyleName = docStyleNames[n];
		}
		
		//	do we have a match?
		if (bestDocStyleName != null) {
//			docStyle = this.loadSettingsResource(bestDocStyleName);
			docStyle = ((DocStyle) this.docStylesByName.get(bestDocStyleName));
			if (docStyle != null)
				return docStyle.toProperties();
		}
		
		//	no style found
		return null;
	}
	
	private DocStyle findStyleFor(ImDocument doc) {
		System.out.println("Searching style for document " + doc.docId);
		DocStyle bestDocStyle = null;
		int bestDocStyleAnchorMatchCount = 0;
		float bestDocStyleAnchorMatchScore = 0.66f; // require at least two thirds of anchors to match to prevent erroneous style assignments
		for (Iterator dsnit = this.docStylesByName.keySet().iterator(); dsnit.hasNext();) {
			String dsn = ((String) dsnit.next());
			System.out.println(" - testing style " + dsn);
			DocStyle ds = ((DocStyle) this.docStylesByName.get(dsn));
			if (ds.anchors.length == 0) {
				System.out.println(" ==> no anchors");
				continue;
			}
			System.out.println(" - got " + ds.anchors.length + " anchors");
			int dsAnchorMatchCount = 0;
			for (int a = 0; a < ds.anchors.length; a++) {
				System.out.println("   - testing anchor " + ds.anchors[a].pattern + " at " + ds.anchors[a].area);
				if (anchorMatches(doc, ds.anchors[a].area, ds.anchors[a].minFontSize, ds.anchors[a].maxFontSize, ds.anchors[a].isBold, ds.anchors[a].isItalics, ds.anchors[a].isAllCaps, ds.anchors[a].pattern)) {
					dsAnchorMatchCount++;
					System.out.println("   --> match");
				}
				else System.out.println("   --> no match");
			}
			if (dsAnchorMatchCount == 0)
				continue;
			float dsAnchorMatchScore = (((float) dsAnchorMatchCount) / ds.anchors.length);
			System.out.println(" - match score is " + dsAnchorMatchScore);
			if (dsAnchorMatchScore > bestDocStyleAnchorMatchScore) {
				bestDocStyle = ds;
				bestDocStyleAnchorMatchCount = dsAnchorMatchCount;
				bestDocStyleAnchorMatchScore = dsAnchorMatchScore;
				System.out.println(" ==> new best match (" + dsAnchorMatchScore + " for " + dsAnchorMatchCount + " out of " + ds.anchors.length + ")");
			}
			else if ((dsAnchorMatchScore == bestDocStyleAnchorMatchScore) && (bestDocStyleAnchorMatchCount < dsAnchorMatchCount)) {
				bestDocStyle = ds;
				bestDocStyleAnchorMatchCount = dsAnchorMatchCount;
				bestDocStyleAnchorMatchScore = dsAnchorMatchScore;
				System.out.println(" ==> new best-founded match (" + dsAnchorMatchScore + " for " + dsAnchorMatchCount + " out of " + ds.anchors.length + ")");
			}
			else System.out.println(" ==> worse than best match");
		}
		return bestDocStyle;
	}
	
	static boolean anchorMatches(ImDocument doc, BoundingBox area, int minFontSize, int maxFontSize, boolean isBold, boolean isItalics, boolean isAllCaps, String pattern) {
		
		//	get page and scale bounding box
		ImPage page = doc.getPage(0);
		area = DocumentStyle.scaleBox(area, 72, page.getImageDPI());
		
		//	get words in area
		ImWord[] words = doc.getPage(0).getWordsInside(area);
		if (words.length == 0)
			return false;
		
		//	filter words by font properties
		ArrayList wordList = new ArrayList();
		for (int w = 0; w < words.length; w++) {
			if (isBold && !words[w].hasAttribute(ImWord.BOLD_ATTRIBUTE))
				continue;
			if (isItalics && !words[w].hasAttribute(ImWord.ITALICS_ATTRIBUTE))
				continue;
			if (isAllCaps && !words[w].getString().equals(words[w].getString().toUpperCase()))
				continue;
			try {
				int wfs = Integer.parseInt((String) words[w].getAttribute(ImWord.FONT_SIZE_ATTRIBUTE, "0"));
				if (wfs < minFontSize)
					continue;
				if (maxFontSize < wfs)
					continue;
			} catch (NumberFormatException nfe) {}
			wordList.add(words[w]);
		}
		if (wordList.isEmpty())
			return false;
		else if (wordList.size() < words.length)
			words = ((ImWord[]) wordList.toArray(new ImWord[wordList.size()]));
		
		//	sort words and create normalized string
		ImUtils.sortLeftRightTopDown(words);
		StringBuffer wordStr = new StringBuffer();
		for (int w = 0; w < words.length; w++)
			wordStr.append(normalizeString(words[w].getString()));
		
		//	test against pattern
		return Pattern.compile(pattern).matcher(wordStr).matches();
	}
	
	static String normalizeString(String string) {
		StringBuffer nString = new StringBuffer();
		for (int c = 0; c < string.length(); c++) {
			char ch = string.charAt(c);
			if ((ch < 33) || (ch == 160))
				nString.append(" "); // turn all control characters into spaces, along with non-breaking space
			else if (ch < 127)
				nString.append(ch); // no need to normalize basic ASCII characters
			else if ("-\u00AD\u2010\u2011\u2012\u2013\u2014\u2015\u2212".indexOf(ch) != -1)
				nString.append("-"); // normalize dashes right here
			else nString.append(StringUtils.getNormalForm(ch));
		}
		return nString.toString();
	}
	
	private static class DocStyle {
		DocStyleAnchor[] anchors;
		Settings paramList;
		DocStyle(Settings paramList) {
			this.paramList = paramList;
			this.updateAnchors();
		}
		void setParamList(Settings paramList) {
			String[] eParamNames = this.paramList.getKeys();
			for (int p = 0; p < eParamNames.length; p++) {
				if (!paramList.containsKey(eParamNames[p]))
					this.paramList.removeSetting(eParamNames[p]);
			}
			this.paramList.setSettings(paramList);
			this.updateAnchors();
		}
		private void updateAnchors() {
			Settings anchorParamLists = this.paramList.getSubset("anchor");
			String[] anchorNames = anchorParamLists.getSubsetPrefixes();
			ArrayList anchorList = new ArrayList();
			for (int a = 0; a < anchorNames.length; a++) {
				Settings anchorParamList = anchorParamLists.getSubset(anchorNames[a]);
				BoundingBox area = BoundingBox.parse(anchorParamList.getSetting("area"));
				if (area == null)
					continue;
				String pattern = anchorParamList.getSetting("pattern");
				if (pattern == null)
					continue;
				try {
					anchorList.add(new DocStyleAnchor(
							area,
							Integer.parseInt(anchorParamList.getSetting("minFontSize", anchorParamList.getSetting("fontSize", "0"))),
							Integer.parseInt(anchorParamList.getSetting("maxFontSize", anchorParamList.getSetting("fontSize", "72"))),
							"true".equals(anchorParamList.getSetting("isBold")),
							"true".equals(anchorParamList.getSetting("isItalics")),
							"true".equals(anchorParamList.getSetting("isAllCaps")),
							pattern
						));
				} catch (NumberFormatException nfe) {}
			}
			this.anchors = ((DocStyleAnchor[]) anchorList.toArray(new DocStyleAnchor[anchorList.size()]));
		}
		Properties toProperties() {
			return this.paramList.toProperties();
		}
	}
	
	private static class DocStyleAnchor {
		final BoundingBox area;
		final int minFontSize;
		final int maxFontSize;
		final boolean isBold;
		final boolean isItalics;
		final boolean isAllCaps;
		final String pattern;
		DocStyleAnchor(BoundingBox area, int minFontSize, int maxFontSize, boolean isBold, boolean isItalics, boolean isAllCaps, String pattern) {
			this.area = area;
			this.minFontSize = minFontSize;
			this.maxFontSize = maxFontSize;
			this.isBold = isBold;
			this.isItalics = isItalics;
			this.isAllCaps = isAllCaps;
			this.pattern = pattern;
		}
	}
}