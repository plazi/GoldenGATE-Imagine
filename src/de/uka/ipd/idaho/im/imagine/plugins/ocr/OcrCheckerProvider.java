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
package de.uka.ipd.idaho.im.imagine.plugins.ocr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.gamta.util.ParallelJobRunner;
import de.uka.ipd.idaho.gamta.util.ParallelJobRunner.ParallelFor;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.ProgressMonitor.SynchronizedProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.goldenGate.plugins.PluginDataProviderFileBased;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImObject;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImSupplement;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.Imaging;
import de.uka.ipd.idaho.im.analysis.Imaging.AnalysisImage;
import de.uka.ipd.idaho.im.analysis.Imaging.ImagePartRectangle;
import de.uka.ipd.idaho.im.analysis.WordImageAnalysis;
import de.uka.ipd.idaho.im.analysis.WordImageAnalysis.WordImage;
import de.uka.ipd.idaho.im.analysis.WordImageAnalysis.WordImageMatch;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener;
import de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;
import de.uka.ipd.idaho.im.util.ImUtils;
import de.uka.ipd.idaho.im.util.ImfIO;
import de.uka.ipd.idaho.im.util.SymbolTable;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * This plugin provides an OCR sanity checker for scanned documents. The
 * sanity check works by superimposing rendered OCR results over the original
 * word images and subsequently measuring the accuracy of the overlay. It can
 * also be used as a component outside of GoldenGATE Imagine.
 * 
 * @author sautter
 */
public class OcrCheckerProvider extends AbstractImageMarkupToolProvider implements SelectionActionProvider, ReactionProvider, GoldenGateImagineDocumentListener {
	
	/* TODO describe this in class JavaDoc
	 * do cluster merging in two steps:
	 * - first, identify merger candidates:
	 *   - compare all clusters to all (eligible) others ...
	 *   - ... and store similarity and action to take in list of possible mergers
	 *   ==> should parallelize pretty well
	 * - second, do actual merging:
	 *   - start with most similar cluster
	 *   - merge on close matches ...
	 *   - ... and group on more remote ones
	 *     - especially in terms of edit distances
	 *     - and also with lower optical thresholds
	 *     - but still with dictionary catch
	 *   ==> prevents order effects
	 *   ==> should parallelize pretty well ...
	 *       ... at least when using a common set for cluster locking
	 *   ==> far easier to assess which cluster has the best ORC
	 * - third, assess groups as a whole for more merging
	 *   ==> groups are a lot less dangerous that merging
	 */
	
	/* TODO
In correction UI:
- display only cluster groups initially
  ==> single point OCR correction
  - on click on main layer (font, size, face, and style) image, show layers and groups in 2-dimensional table
  - offer editing clusters individually
  - offer removing cluster sets from group for click on table column head
  - offer viewing individual word images for click on individual clusters
- offer editing font styles for cluster groups ...
- ... as well as cluster layers
- offer font style filter (e.g. for viewing italics taxon names only)

Word cluster UI:
- offer "Same Word" option merging one style similar cluster into other, using "OCR A" vs. "OCR B" vs. "Cancel" for selecting merge direction
- offer "View Individual Words" option showing cluster members
  - indivdual words selectable
  - "Different Font Style" option moving words to ((maybe) new) cluster with same OCR (still check dimensions)
  - "Different Word" option moving words to wholly new cluster
	 */
	
	private static final String OCR_CHECKER_IMT_NAME = "OcrChecker";
	private ImageMarkupTool ocrChecker = new OcrChecker();
	
	private TreeSet dictionary = new TreeSet(String.CASE_INSENSITIVE_ORDER);
	
	private boolean debug = false;
	private int debugPage = -1;
	
	/** zero-argument constructor for class loading */
	public OcrCheckerProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "IM OCR Checker";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	load dictionaries
		String[] dataNames = this.dataProvider.getDataNames();
		for (int d = 0; d < dataNames.length; d++) try {
			if (dataNames[d].indexOf(".list.") == -1)
				continue;
			
			//	open file reader from ZIP or plain TXT file
			BufferedReader dictReader = null;
			if (dataNames[d].endsWith(".list.txt")) {
				dictReader = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream(dataNames[d]), "UTF-8"));
			}
			else if (dataNames[d].endsWith(".list.zip")) {
				final ZipInputStream zis = new ZipInputStream(this.dataProvider.getInputStream(dataNames[d]));
				ZipEntry ze = zis.getNextEntry();
				if (ze == null) {
					zis.close();
					continue;
				}
				dictReader = new BufferedReader(new InputStreamReader(zis, "UTF-8")) {
					public void close() throws IOException {
						super.close();
						zis.close();
					}
				};
			}
			else continue;
			
			//	load dictionary
			for (String dictEntry; (dictEntry = dictReader.readLine()) != null;) {
				dictEntry = dictEntry.trim();
				if ((dictEntry.length() != 0) && !dictEntry.startsWith("//"))
					this.dictionary.add(dictEntry);
			}
			
			//	finally ...
			dictReader.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.ImWord, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(final ImWord start, ImWord end, ImDocumentMarkupPanel idmp) {
		
		//	we only offer actions for individual words
		if (start != end)
			return null;
		
		//	we only work if OCR overlay visible
		if (!idmp.areTextStringsPainted())
			return null;
		
		//	get existing word clustering (cannot do without)
		final ImWordClustering wordClustering = this.getWordClustering(idmp.document, false, null);
		if (wordClustering == null)
			return null;
		
		//	get cluster around current word (cannot do without that, either)
		final ImWordCluster wc = wordClustering.getClusterForWord(start);
		if (wc == null)
			return null;
		
		//	do we have any related words?
		final ImWordClusterSimilarity[] wcss = wordClustering.getSimilarClusters(wc, true);
		
		//	anything to work with?
		if ((wc.words.size() < 2) && (wcss.length == 0))
			return null;
		
		//	collect available actions
		LinkedList actions = new LinkedList();
		
		//	edit word string
		actions.add(new SelectionAction("editWords", ("List Similar Words"), ("List words optically similar to '" + start.getString() + "'")) {
			public boolean performAction(ImDocumentMarkupPanel invoker) {
				displayWordCluster(wc, wcss, null);
				return true;
			}
		});
		
		//	finally ...
		return ((SelectionAction[]) actions.toArray(new SelectionAction[actions.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider#getActions(java.awt.Point, java.awt.Point, de.uka.ipd.idaho.im.ImPage, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel)
	 */
	public SelectionAction[] getActions(Point start, Point end, ImPage page, ImDocumentMarkupPanel idmp) { return null; /* we only offer actions for individual words */ }
	
	//	TODO add isUndo flag to notification API instead of blocking notification altogether
	//	==> facilitates keeping clustering in sync on UNDO
	//	==> deny any reaction on UNDO, though, also in other reaction providers
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#typeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void typeChanged(ImObject object, String oldType, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentOpened(de.uka.ipd.idaho.im.ImDocument)
	 */
	public void documentOpened(ImDocument doc) { /* we're not interested in documents opening */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentSaving(de.uka.ipd.idaho.im.ImDocument)
	 */
	public void documentSaving(ImDocument doc) { /* no need to listen for documents saving, as our own little supplement implementation takes care of staying up to date */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentSaved(de.uka.ipd.idaho.im.ImDocument)
	 */
	public void documentSaved(ImDocument doc) { /* we're not interested in documents saving success */ }
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener#documentClosed(java.lang.String)
	 */
	public void documentClosed(String docId) {
		this.docIDsToWordClusterings.remove(docId);
		
//		//	TODO dispose clustering, e.g. clearing collections (recursively !!!) to assist garbage collection
//		ImWordClustering wordClustering = ((ImWordClustering) this.docIDsToWordClusterings.remove(docId));
//		if (wordClustering != null)
//			wordClustering.dispose();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#attributeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, java.lang.Object, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void attributeChanged(ImObject object, String attributeName, Object oldValue, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		
		//	we're only interested in scanned documents
		if (idmp.documentBornDigital)
			return;
		
		//	we're only interested in word edits
		if (!(object instanceof ImWord))
			return;
		
		//	we're only interested in OCR strings and style attributes
		if (!ImWord.STRING_ATTRIBUTE.equals(attributeName) && !ImWord.BOLD_ATTRIBUTE.equals(attributeName) && !ImWord.ITALICS_ATTRIBUTE.equals(attributeName))
			return;
		
		//	get word clustering
		ImWordClustering wordClustering = this.getWordClustering(idmp.document, false, null);
		if (wordClustering == null)
			return;
		
		//	get word cluster
		ImWord imw = ((ImWord) object);
		ImWordCluster wc = wordClustering.getClusterForWord(imw);
		if (wc == null)
			return;
		
		//	let's not react on our own updates
		if (wc.isUpdating)
			return;
		
		//	update word cluster
		if (ImWord.STRING_ATTRIBUTE.equals(attributeName))
			wc.setString(imw.getString());
		else if (ImWord.BOLD_ATTRIBUTE.equals(attributeName))
			wc.setBold(imw.hasAttribute(ImWord.BOLD_ATTRIBUTE));
		else if (ImWord.ITALICS_ATTRIBUTE.equals(attributeName))
			wc.setItalics(imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE));
		
		//	record edit
		if (ImWord.STRING_ATTRIBUTE.equals(attributeName) && (oldValue != null))
			wordClustering.addLetterReplacements(oldValue.toString(), imw.getString(), wc.words.size());
		
		//	not allowed to prompt for similar clusters
		if (!allowPrompt)
			return;
		
		//	get similar clusters
		ImWordClusterSimilarity[] wcss = wordClustering.getSimilarClusters(wc, true);
		ImWordClusterSimilarity[] tgWcss = ((oldValue == null) ? null : getStringSimilarClusters(wc, oldValue.toString(), wordClustering));
		if ((wcss.length == 0) && (tgWcss.length == 0))
			return;
		
		//	open list of clusters for editing
		this.displayWordCluster(wc, wcss, tgWcss);
		
		//	TODO_not (too high risk of error is user plays around) If two optically similar clusters are conflated in terms of OCR, merge them if style flags match as well
		//	TODO_not (too high risk of error is user plays around) If two optically similar clusters are conflated in terms of style flags, merge them if OCR matches as well
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#regionAdded(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionAdded(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		
		//	we're only interested in scanned documents
		if (idmp.documentBornDigital)
			return;
		
		//	we're only interested in word edits
		if (!(region instanceof ImWord))
			return;
		
		//	get word clustering
		ImWordClustering wordClustering = this.getWordClustering(idmp.document, false, null);
		if (wordClustering == null)
			return;
		
		//	get word clusters for word string
		ImWord imw = ((ImWord) region);
		ImWordCluster[] iwcs = wordClustering.getForString(imw.getString());
		
		//	new word, add cluster
		if (iwcs == null) {
			String iwcId = Integer.toString(imw.hashCode(), 16).toUpperCase();
			wordClustering.getClusterForId(iwcId, imw);
			return;
		}
		
		//	seek best-matching string-similar word cluster
		PageImage imwPi = imw.getImage();
		WordImage imwWi = new WordImage(imwPi.image, imw, -1, imwPi.currentDpi);
		float bestSim = 0;
		ImWordCluster bestSimIwc = null;
		for (int c = 0; c < iwcs.length; c++) {
			if (!imw.getString().equals(iwcs[c].str))
				continue;
			if (iwcs[c].isBold != imw.hasAttribute(ImWord.BOLD_ATTRIBUTE))
				continue;
			if (iwcs[c].isItalics != imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE))
				continue;
			
			WordImage iwcWi = new WordImage(iwcs[c].representative, iwcs[c].str, iwcs[c].isItalics, imwPi.currentDpi);
			if (Math.abs(imwWi.boxProportion - iwcWi.boxProportion) > 0.15f)
				continue;
			if ((Math.abs(imwWi.box.getWidth() - iwcWi.box.getWidth()) * 10) > Math.max(imwWi.box.getWidth(), iwcWi.box.getWidth()))
				continue;
			if ((Math.abs(imwWi.box.getHeight() - iwcWi.box.getHeight()) * 5) > Math.max(imwWi.box.getHeight(), iwcWi.box.getHeight()))
				continue;
			
			WordImageMatch wim = WordImageAnalysis.matchWordImages(imwWi, iwcWi, true, true);
			if (bestSim < wim.sim) {
				bestSim = wim.sim;
				bestSimIwc = iwcs[c];
			}
		}
		
		//	no matching cluster found (less than 90% match, let's be safe here)
		if ((bestSim < 0.90) || (bestSimIwc == null)) {
			String iwcId = Integer.toString(imw.hashCode(), 16).toUpperCase();
			wordClustering.getClusterForId(iwcId, imw);
		}
		
		//	found matching cluster
		else bestSimIwc.addWord(imw);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#regionRemoved(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionRemoved(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) {
		
		//	we're only interested in scanned documents
		if (idmp.documentBornDigital)
			return;
		
		//	we're only interested in word edits
		if (!(region instanceof ImWord))
			return;
		
		//	get word clustering
		ImWordClustering wordClustering = this.getWordClustering(idmp.document, false, null);
		if (wordClustering == null)
			return;
		
		//	get word cluster
		ImWord imw = ((ImWord) region);
		ImWordCluster wc = wordClustering.getClusterForWord(imw);
		
		//	remove word from cluster
		if (wc != null)
			wc.removeWord(imw);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationAdded(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationAdded(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationRemoved(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationRemoved(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	private static ImWordClusterSimilarity[] getStringSimilarClusters(ImWordCluster wc, String oldStr, ImWordClustering wordClustering) {
		
		//	also get word clusters containing edited trigrams
		CountingSet simNGramWcSet = new CountingSet();
		WordStringEdit[] wses = getEdits(oldStr, wc.str);
		for (int e = 0; e < wses.length; e++) {
			if (wses[e].replaced.length() == 0)
				continue;
			for (int s = 0; s < wses[e].replaced.length(); s++)
				if (127 < wses[e].replaced.charAt(s)) {
					ImWordCluster[] rIwcs = wordClustering.getForString(wses[e].replaced.substring(s, (s + 1)));
					if (rIwcs != null)
						simNGramWcSet.addAll(Arrays.asList(rIwcs));
				}
			for (int s = 0; s < (wses[e].replaced.length() - 1); s++) {
				ImWordCluster[] rIwcs = wordClustering.getForString(wses[e].replaced.substring(s, (s + 2)));
				if (rIwcs != null)
					simNGramWcSet.addAll(Arrays.asList(rIwcs));
			}
			char bCh = ((wses[e].sPos == 0) ? '_' : oldStr.charAt(wses[e].sPos));
			char aCh = (((wses[e].sPos + wses[e].replaced.length()) == oldStr.length()) ? '_' : oldStr.charAt(wses[e].sPos + wses[e].replaced.length()));
			String tgStr = (bCh + wses[e].replaced + aCh);
			for (int s = 0; s < (tgStr.length() - 2); s++) {
				ImWordCluster[] rIwcs = wordClustering.getForString(tgStr.substring(s, (s + 3)));
				if (rIwcs != null)
					simNGramWcSet.addAll(Arrays.asList(rIwcs));
			}
		}
		simNGramWcSet.removeAll(wc);
		ImWordCluster[] simNGramWcs = ((ImWordCluster[]) simNGramWcSet.toArray(new ImWordCluster[simNGramWcSet.elementCount()]));
		
		//	create cluster similarities
		ArrayList wcsList = new ArrayList();
		int maxSimNGamWcCount = 0;
		for (int c = 0; c < simNGramWcs.length; c++)
			maxSimNGamWcCount = Math.max(maxSimNGamWcCount, simNGramWcSet.getCount(simNGramWcs[c]));
		for (int c = 0; c < simNGramWcs.length; c++) {
			int simNGamWcCount = simNGramWcSet.getCount(simNGramWcs[c]);
			if (simNGamWcCount != 0)
				wcsList.add(new ImWordClusterSimilarity(wc, simNGramWcs[c], (((float) simNGamWcCount) / (maxSimNGamWcCount + 1))));
		}
		Collections.sort(wcsList, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				ImWordClusterSimilarity wcs1 = ((ImWordClusterSimilarity) obj1);
				ImWordClusterSimilarity wcs2 = ((ImWordClusterSimilarity) obj2);
				return Float.compare(wcs2.similarity, wcs1.similarity);
			}
		});
		return ((ImWordClusterSimilarity[]) wcsList.toArray(new ImWordClusterSimilarity[wcsList.size()]));
	}
	
	private void displayWordCluster(ImWordCluster wc, ImWordClusterSimilarity[] wcss, ImWordClusterSimilarity[] tgWcss) {
		ImWordClusterListDialog wcld = new ImWordClusterListDialog(wc, wcss, tgWcss, true);
		wcld.setLocationRelativeTo(null);
		wcld.setVisible(true);
	}
	
	/**
	 * Dialog displaying similar word clusters for merging.
	 * 
	 * @author sautter
	 */
	private static class ImWordClusterListDialog extends DialogPanel {
		private ImWordClusterQueue wcQueue;
		private LinkedList backStack = null;
		private boolean isWordEdit = false;
		private ImWordCluster wc;
		private ImWordClusterPanel wcp;
		private JPanel swcp = new JPanel(new GridBagLayout(), true);
		ImWordClusterListDialog(final ImWordCluster wc, ImWordClusterSimilarity[] wcss, ImWordClusterSimilarity[] tgWcss, final boolean isWordEdit) {
			super((isWordEdit ? ("Words Similar to '" + wc.str + "'") : ("Possible OCR Error '" + wc.str + "' (" + wc.words.size() + " times)")), true);
			this.isWordEdit = isWordEdit;
			
			this.wcp = new ImWordClusterPanel(wc, this, true) {
				protected void wordClusterApproved(ImWordCluster wc, String oldStr) {
					if (oldStr != null)
						enqueueStringSimilarClusters(getStringSimilarClusters(wc, oldStr, wc.root));
					next();
				}
			};
			
			JScrollPane swcpBox = new JScrollPane(this.swcp);
			swcpBox.getVerticalScrollBar().setBlockIncrement(50);
			swcpBox.getVerticalScrollBar().setUnitIncrement(50);
			
			JButton backButton = new JButton(" < ");
			backButton.setFont(new Font("Dialog", Font.BOLD, 24));
			backButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(backButton.getBackground(), 2), BorderFactory.createRaisedBevelBorder()));
			backButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					back();
				}
			});
			
			JPanel topPanel = new JPanel(new BorderLayout(), true);
			topPanel.add(backButton, BorderLayout.WEST);
			topPanel.add(this.wcp, BorderLayout.CENTER);
			this.add(topPanel, BorderLayout.NORTH);
			this.add(swcpBox, BorderLayout.CENTER);
			
			//	set word cluster
			this.setWordCluster(wc, false, wcss);
			
			//	enqueue string-similar clusters
			this.enqueueStringSimilarClusters(tgWcss);
		}
		
		void setWordCluster(ImWordCluster wc, boolean highPriority, ImWordClusterSimilarity[] wcss) {
			if (this.wcQueue != null)
				this.wcQueue.markDequeued(this.wc);
			this.wc = wc;
			this.wcp.setWordCluster(this.wc);
			if (highPriority)
				this.setTitle("Possible OCR Error '" + this.wc.str + "' (" + this.wc.words.size() + " times, contains previously corrected letter combination");
			else this.setTitle(this.isWordEdit ? ("Words Similar to '" + this.wc.str + "'") : ("Possible OCR Error '" + this.wc.str + "' (" + this.wc.words.size() + " times)"));
			int swcc = this.updateSimilarWordClusters(wcss);
			this.setSize((100 + Math.max(400, (2 * wc.representative.getWidth()))), Math.min(600, (100 + (Math.max(60, wc.representative.getHeight()) * swcc))));
		}
		
		void mergeWordCluster(ImWordCluster wc) {
			
			//	always merge small into large, no matter which way they come up
			if ((this.wc.words.size() < wc.words.size()) || ((this.wc.words.size() < (wc.words.size() * 2)) && (this.wc.representative.getWidth() < wc.representative.getWidth()))) {
				ImWordCluster mWc = this.wc;
				this.wc = wc;
				wc = mWc;
			}
			
			//	check if size matches
			boolean sizeMisMatch = false;
			if ((Math.min(this.wc.representative.getWidth(), wc.representative.getWidth()) * 10) < (Math.max(this.wc.representative.getWidth(), wc.representative.getWidth()) * 9))
				sizeMisMatch = true;
			else if ((Math.min(this.wc.representative.getHeight(), wc.representative.getHeight()) * 5) < (Math.max(this.wc.representative.getHeight(), wc.representative.getHeight()) * 4))
				sizeMisMatch = true;
			
			//	remember old string
			String oldStr = (this.wc.str.equals(wc.str) ? null : wc.str);
			
			//	prompt for confirmation in any case of a mismatch
			if (sizeMisMatch || !this.wc.str.equals(wc.str) || (this.wc.isBold != wc.isBold) || (this.wc.isItalics != wc.isItalics)) {
				JPanel crp = new JPanel(new GridLayout(0, 1), true);
				crp.add(new JLabel(new ImageIcon(this.wc.representative)));
				crp.add(new JLabel(new ImageIcon(wc.representative)));
				
				JPanel mdp = new JPanel(new GridLayout(0, 2), true);
				ButtonGroup strBg = new ButtonGroup();
				JRadioButton tWcStr = new JRadioButton(("'" + this.wc.str + "'"), (wc.words.size() <= this.wc.words.size()));
				strBg.add(tWcStr);
				tWcStr.setEnabled(!this.wc.str.equals(wc.str));
				JRadioButton wcStr = new JRadioButton(("'" + wc.str + "'"), (wc.words.size() > this.wc.words.size()));
				wcStr.setEnabled(!this.wc.str.equals(wc.str));
				strBg.add(wcStr);
				mdp.add(tWcStr);
				mdp.add(wcStr);
				JCheckBox bold = new JCheckBox("Bold", ((((this.wc.isBold ? this.wc.words.size() : 0) + (wc.isBold ? wc.words.size() : 0)) * 2) > (this.wc.words.size() + wc.words.size())));
				bold.setEnabled(this.wc.isBold != wc.isBold);
				mdp.add(bold);
				JCheckBox italics = new JCheckBox("Italics", ((((this.wc.isItalics ? this.wc.words.size() : 0) + (wc.isItalics ? wc.words.size() : 0)) * 2) > (this.wc.words.size() + wc.words.size())));
				italics.setEnabled(this.wc.isItalics != wc.isItalics);
				mdp.add(italics);
				
				JPanel mop = new JPanel(new BorderLayout(), true);
				mop.add(new JLabel("<HTML>Please only merge clusters of words that:" +
						"<BR>- represent the same string" +
						"<BR>- in the same stlye" +
						"<BR>- at (approximately) the same font size" +
						"<BR>Resolve any conflicts on the right of the images</HTML>"), BorderLayout.NORTH);
				mop.add(crp, BorderLayout.WEST);
				mop.add(mdp, BorderLayout.CENTER);
				
				if (JOptionPane.showConfirmDialog(null, mop, "Word Cluster Merger", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
					return;
				
				if (tWcStr.isSelected())
					wc.setString(this.wc.str);
				else if (wcStr.isSelected())
					this.wc.setString(wc.str);
				this.wc.setBold(bold.isSelected());
				wc.setBold(bold.isSelected());
				this.wc.setItalics(italics.isSelected());
				wc.setItalics(italics.isSelected());
			}
			
			//	perform merger
			this.wc.mergeWith(wc);
			this.wcp.setWordCluster(this.wc);
			
			//	update layout
			this.wcp.updateRepTooltip();
			this.setTitle(this.isWordEdit ? ("Words Similar to '" + this.wc.str + "'") : ("Possible OCR Error '" + this.wc.str + "' (" + this.wc.words.size() + " times)"));
			
			//	update similar cluster list
			this.updateSimilarWordClusters(null);
			
			//	mark cluster as processed, we might have had a swap
			if (this.wcQueue != null)
				this.wcQueue.markDequeued(this.wc);
			
			//	enqueue string-similar clusters
			if (oldStr != null)
				this.enqueueStringSimilarClusters(getStringSimilarClusters(this.wc, oldStr, this.wc.root));
		}
		
		void markAsArtifact(ImWordCluster wc) {
			
			//	mark cluster words as artifacts
			for (int w = 0; w < wc.words.size(); w++) {
				ImWord imw = ((ImWord) wc.words.get(w));
				ImWord[] imwa = {imw};
				ImUtils.makeStream(imwa, ImWord.TEXT_STREAM_TYPE_ARTIFACT, null);
			}
			
			//	empty cluster & remove it from clustering
			wc.words.clear();
			this.wc.root.removeWordCluster(wc);
			
			//	move on if main cluster marked as artifact
			if (wc == this.wc)
				this.next();
			
			//	update similar cluster list otherwise
			else this.updateSimilarWordClusters(null);
		}
		
		private int updateSimilarWordClusters(ImWordClusterSimilarity[] wcss) {
			this.swcp.removeAll();
			
			if (wcss == null)
				wcss = this.wc.root.getSimilarClusters(this.wc, true);
			if (wcss != null) {
				Arrays.sort(wcss, new Comparator() {
					public int compare(Object obj1, Object obj2) {
						ImWordClusterSimilarity wcs1 = ((ImWordClusterSimilarity) obj1);
						ImWordClusterSimilarity wcs2 = ((ImWordClusterSimilarity) obj2);
						return Float.compare(wcs2.similarity, wcs1.similarity);
					}
				});
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx = 0;
				gbc.gridy = 0;
				gbc.weightx = 1;
				gbc.weighty = 0;
				gbc.insets.top = 5;
				gbc.insets.bottom = 0;
				gbc.fill = GridBagConstraints.BOTH;
				for (int s = 0; s < wcss.length; s++) {
					this.swcp.add(new ImWordClusterPanel(wcss[s].wc2, this, false), gbc.clone());
					gbc.gridy++;
				}
				gbc.weighty = 1;
				this.swcp.add(new JPanel(), gbc.clone());
			}
			
			this.swcp.validate();
			this.swcp.repaint();
			
			return ((wcss == null) ? 0 : wcss.length);
		}
		
		private void enqueueStringSimilarClusters(ImWordClusterSimilarity[] tgWcss) {
			if ((tgWcss == null) || (tgWcss.length == 0))
				return;
			if (this.wcQueue == null)
				this.wcQueue = new ImWordClusterQueue();
			for (int s = 0; s < tgWcss.length; s++)
				this.wcQueue.enqueue(tgWcss[s].wc2, true);
		}
		
		void enqueueWordCluster(ImWordCluster wc) {
			if (this.wcQueue == null)
				this.wcQueue = new ImWordClusterQueue();
			this.wcQueue.enqueue(wc, false);
		}
		
		void next() {
			if (this.wcQueue == null)
				this.dispose();
			ImWordCluster wc = this.wcQueue.dequeue();
			if (wc == null)
				this.dispose();
			else {
				if (this.backStack == null)
					this.backStack = new LinkedList();
				this.backStack.addLast(this.wc);
				this.setWordCluster(wc, this.wcQueue.lastDequeueWasHighPriority(), null);
			}
		}
		
		void back() {
			if ((this.backStack != null) && (this.backStack.size() != 0))
				this.setWordCluster(((ImWordCluster) this.backStack.removeLast()), false, null);
		}
	}
	
	private static class ImWordClusterQueue {
		private LinkedList highPriorityQueue = new LinkedList();
		private LinkedList lowPriorityQueue = new LinkedList();
		private HashSet dequeued = new HashSet();
		private boolean lastDequeuedWasHighPriority = false;
		void enqueue(ImWordCluster wc, boolean highPriority) {
			(highPriority ? this.highPriorityQueue : this.lowPriorityQueue).addLast(wc);
		}
		void markDequeued(ImWordCluster wc) {
			if (wc != null)
				this.dequeued.add(wc);
		}
		ImWordCluster dequeue() {
			ImWordCluster wc = null;
			do {
				if (this.highPriorityQueue.size() != 0) {
					wc = ((ImWordCluster) this.highPriorityQueue.removeFirst());
					this.lastDequeuedWasHighPriority = true;
				}
				else if (this.lowPriorityQueue.size() != 0) {
					wc = ((ImWordCluster) this.lowPriorityQueue.removeFirst());
					this.lastDequeuedWasHighPriority = false;
				}
				if (wc == null)
					return wc; // queue is empty
				else if (wc.words.isEmpty())
					wc = null; // this one has been merged into another cluster or marked as an artifact
				else if (!this.dequeued.add(wc))
					wc = null; // we've seen this one before
				else return wc; // this one's fine
			}
			while (wc == null);
			return wc;
		}
		boolean lastDequeueWasHighPriority() {
			return this.lastDequeuedWasHighPriority;
		}
	}
	
	/* TODO
UI: Make "Edit Page Image & Words" accessible from cluster representative context menu (if only for single-word clusters)
- add optional bounding box argument to respective method in document markup panel
  - on write-through, translate all edits relative to bounding box
- show word plus some context, maybe half an inch left & right, and a quarter inch above & below
- also offer this from "View Cluster Members" list
- offer "light version" of this as "View Context" to help with weird words, e.g. parts of hyphenated words
  - offer "More Context" button in "View Context" dialog to widen page image part shown
  - show word boundaries as (yellow) boxes as in "full version"
  - offer going to "Edit Page Image & Words" from there
	 */
	
	private static class ImWordClusterPanel extends JPanel {
		private ImWordClusterListDialog wcld;
		private SymbolTable symbolTable;
		private ImWordCluster wc;
		JPanel repPanel;
		private JTextField strField;
		private JCheckBox boldField;
		private JCheckBox italicsField;
		ImWordClusterPanel(ImWordCluster wc, ImWordClusterListDialog wcld, boolean isMainWcp) {
			super(new BorderLayout(), true);
			this.wc = wc;
			this.wcld = wcld;
			
			this.repPanel = new JPanel() {
				public void paint(Graphics g) {
					super.paint(g);
					g.drawImage(ImWordClusterPanel.this.wc.representative, 3, 3, null);
				}
				public Dimension getPreferredSize() {
					return new Dimension((ImWordClusterPanel.this.wc.representative.getWidth() + 6), (ImWordClusterPanel.this.wc.representative.getHeight() + 6));
				}
				public Dimension getMaximumSize() {
					return this.getPreferredSize();
				}
				public Dimension getMinimumSize() {
					return this.getPreferredSize();
				}
				public Dimension getSize() {
					return this.getPreferredSize();
				}
			};
			this.repPanel.setBackground(Color.WHITE);
			this.add(this.repPanel, BorderLayout.WEST);
			
			JButton artifactButton = new JButton("Artifact");
			artifactButton.setToolTipText("Mark as Artifact");
			artifactButton.setBorder(BorderFactory.createRaisedBevelBorder());
			artifactButton.setPreferredSize(new Dimension(50, 21));
			artifactButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					ImWordClusterPanel.this.wcld.markAsArtifact(ImWordClusterPanel.this.wc);
				}
			});
			
			if (isMainWcp) {
				this.boldField = new JCheckBox("Bold");
				this.boldField.setToolTipText("Bold");
				this.boldField.setBorder(BorderFactory.createEmptyBorder());
				
				this.italicsField = new JCheckBox("Italics");
				this.italicsField.setToolTipText("Italics");
				this.italicsField.setBorder(BorderFactory.createEmptyBorder());
				
				this.strField = new JTextField();
				this.strField.setFont(new Font("Monospaced", Font.PLAIN, 14));
				this.strField.setBorder(BorderFactory.createLoweredBevelBorder());
				this.strField.addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent ke) {
						if (ke.isControlDown()) {
							if (ke.getKeyCode() == KeyEvent.VK_I)
								italicsField.setSelected(!italicsField.isSelected());
							else if (ke.getKeyCode() == KeyEvent.VK_B)
								boldField.setSelected(!boldField.isSelected());
						}
						if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
							commitChanges();
						}
					}
				});
				
				JButton symbolButton = new JButton("Symbols");
				symbolButton.setToolTipText("Symbol Table");
				symbolButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(symbolButton.getBackground(), 2), BorderFactory.createRaisedBevelBorder()));
				symbolButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (symbolTable == null)
							symbolTable = SymbolTable.getSharedSymbolTable();
							symbolTable.setOwner(new SymbolTable.Owner() {
								public void useSymbol(char symbol) {
									StringBuffer sb = new StringBuffer(ImWordClusterPanel.this.strField.getText());
									int cp = ImWordClusterPanel.this.strField.getCaretPosition();
									sb.insert(cp, symbol);
									ImWordClusterPanel.this.strField.setText(sb.toString());
									ImWordClusterPanel.this.strField.setCaretPosition(++cp);
								}
								public void symbolTableClosed() {
									symbolTable = null;
								}
								public Dimension getSize() {
									return ImWordClusterPanel.this.getSize();
								}
								public Point getLocation() {
									return ImWordClusterPanel.this.getLocationOnScreen();
								}
								public Color getColor() {
									return ImWordClusterPanel.this.getBackground();
								}
							});
						symbolTable.open();
					}
				});
				JPanel strPanel = new JPanel(new BorderLayout(), true);
				strPanel.add(this.strField, BorderLayout.CENTER);
				strPanel.add(symbolButton, BorderLayout.EAST);
				
				JButton approveButton = new JButton("OK");
				approveButton.setFont(new Font("Dialog", Font.BOLD, 20));
				approveButton.setToolTipText("Approve OCR & Style");
				approveButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(approveButton.getBackground(), 2), BorderFactory.createRaisedBevelBorder()));
				approveButton.setPreferredSize(new Dimension(50, 21));
				approveButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						ImWordClusterPanel.this.commitChanges();
					}
				});
				
				JPanel styleButtonPanel = new JPanel(new GridLayout(1, 3, 2, 2));
				styleButtonPanel.setBorder(BorderFactory.createLineBorder(styleButtonPanel.getBackground(), 2));
				styleButtonPanel.add(this.boldField);
				styleButtonPanel.add(this.italicsField);
				styleButtonPanel.add(artifactButton);
				JPanel fieldPanel = new JPanel(new GridLayout(2, 1));
				fieldPanel.add(strPanel);
				fieldPanel.add(styleButtonPanel);
				
				this.add(fieldPanel, BorderLayout.CENTER);
				this.add(approveButton, BorderLayout.EAST);
				
				this.setWordCluster(wc);
			}
			
			else {
				JLabel strLabel = new JLabel(" " + wc.str);
				strLabel.setFont(new Font("Monospaced", Font.PLAIN, 20));
				strLabel.setOpaque(true);
				strLabel.setBackground(Color.WHITE);
				this.add(strLabel, BorderLayout.CENTER);
				
				JButton editButton = new JButton("Edit");
				editButton.setToolTipText("Move to top for Editing");
				editButton.setBorder(BorderFactory.createRaisedBevelBorder());
				editButton.setPreferredSize(new Dimension(50, 21));
				editButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						ImWordClusterPanel.this.wcld.setWordCluster(ImWordClusterPanel.this.wc, false, null);
					}
				});
				JButton mergeButton = new JButton("Merge");
				mergeButton.setToolTipText("Merge into top cluster");
				mergeButton.setBorder(BorderFactory.createRaisedBevelBorder());
				mergeButton.setPreferredSize(new Dimension(50, 21));
				mergeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						ImWordClusterPanel.this.wcld.mergeWordCluster(ImWordClusterPanel.this.wc);
					}
				});
				JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 2, 2));
				buttonPanel.setBorder(BorderFactory.createLineBorder(buttonPanel.getBackground(), 2));
				buttonPanel.add(editButton);
				buttonPanel.add(mergeButton);
				buttonPanel.add(artifactButton);
				JPanel fieldPanel = new JPanel(new GridLayout(2, 1));
				fieldPanel.add(strLabel);
				fieldPanel.add(buttonPanel);
				
				this.add(fieldPanel, BorderLayout.CENTER);
				
				this.updateRepTooltip();
			}
		}
		
		void setWordCluster(ImWordCluster wc) {
			this.wc = wc;
			this.strField.setText(this.wc.str);
			this.strField.setCaretPosition(this.wc.str.length());
			this.strField.setPreferredSize(new Dimension(this.wc.representative.getWidth(), 23));
			this.boldField.setSelected(this.wc.isBold);
			this.italicsField.setSelected(this.wc.isItalics);
			this.updateRepTooltip();
			this.validate();
			this.repaint();
		}
		
		void updateRepTooltip() {
			StringBuffer repToolTip = new StringBuffer("'" + wc.str + "'");
			repToolTip.append("(" + wc.words.size() + " words");
			if (wc.isBold)
				repToolTip.append(", bold");
			if (wc.isItalics)
				repToolTip.append(", italics");
			repToolTip.append(")");
			this.repPanel.setToolTipText(repToolTip.toString());
		}
		
		void commitChanges() {
			if ((this.strField == null) || (this.boldField == null) || (this.italicsField == null))
				return;
			if (this.symbolTable != null)
				this.symbolTable.close();
			String oldStr = this.wc.str;
			String str = this.strField.getText();
			if (!this.wc.str.equals(str))
				this.wc.setString(str);
			if (this.wc.isBold != this.boldField.isSelected())
				this.wc.setBold(this.boldField.isSelected());
			if (this.wc.isItalics != this.italicsField.isSelected())
				this.wc.setItalics(this.italicsField.isSelected());
			this.wordClusterApproved(this.wc, (this.wc.equals(oldStr) ? null : oldStr));
		}
		
		protected void wordClusterApproved(ImWordCluster wc, String oldStr) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		String[] tmins = {OCR_CHECKER_IMT_NAME};
		return tmins;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		if (OCR_CHECKER_IMT_NAME.equals(name))
			return this.ocrChecker;
		else return null;
	}
	
	private class OcrChecker implements ImageMarkupTool {
		public String getLabel() {
			return "Check OCR";
		}
		public String getTooltip() {
			return "'Visually' check OCR result via overlaying with original image, and highlight bad matches";
		}
		public String getHelpText() {
			return null; // not now ...
		}
		public void process(ImDocument doc, ImAnnotation annot, ImDocumentMarkupPanel idmp, ProgressMonitor pm) {
			if (idmp.documentBornDigital)
				pm.setStep("Cannot check OCR on born-digital document");
			else if (annot == null)
				checkOcr(doc, pm);
			else pm.setStep("Cannot check OCR on single annotation");
		}
	}
	
	/**
	 * Runtime representation of word cluster
	 * 
	 * @author sautter
	 */
	private static class ImWordCluster {
		final ImWordClustering root;
		final String id;
		String str;
		boolean isBold;
		boolean isItalics;
		ArrayList words = new ArrayList(1);
		BufferedImage representative;
		boolean isUpdating = false;
		ImWordCluster(ImWordClustering root, String id, ImWord imw) {
			this.root = root;
			this.id = id;
			this.str = imw.getString();
			this.isBold = imw.hasAttribute(ImWord.BOLD_ATTRIBUTE);
			this.isItalics = imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE);
			this.addWord(imw);
			this.root.indexForStringAndTrigrams(this);
		}
		
		void addWord(ImWord imw) {
			this.words.add(imw);
			this.root.wordIDsToClusters.put(imw.getLocalID(), this);
			this.root.modCount++;
		}
		void removeWord(ImWord imw) {
			this.words.remove(imw);
			this.root.wordIDsToClusters.remove(imw.getLocalID());
			this.root.modCount++;
		}
		
		void mergeWith(ImWordCluster iwc) {
			
			//	update OCR string and style attributes in argument cluster (no need for hassling local words, they are consistent already)
			if (this.str.equals(iwc.str))
				this.root.unIndexForStringAndTrigrams(iwc);
			else iwc.setString(this.str, false);
			iwc.setBold(this.isBold);
			iwc.setItalics(this.isItalics);
			
			//	add all words from argument cluster to this one (updates word mappings automatically along the way)
			for (int w = 0; w < iwc.words.size(); w++)
				this.addWord((ImWord) iwc.words.get(w));
			iwc.words.clear();
			
			//	update all similarities of argument cluster to this one
			for (int s = 0; s < this.root.clusterSimilarities.size(); s++) {
				ImWordClusterSimilarity iwcs = ((ImWordClusterSimilarity) this.root.clusterSimilarities.get(s));
				if (iwcs.wc1 == iwc)
					iwcs.wc1 = this;
				else if (iwcs.wc2 == iwc)
					iwcs.wc2 = this;
				if (iwcs.wc1 == iwcs.wc2) // remove all now-reflexive similarities
					this.root.clusterSimilarities.remove(s--);
			}
			
			//	remove merged cluster
			this.root.idsToClusters.remove(iwc.id);
		}
		
		void setString(String str) {
			this.setString(str, true);
		}
		private void setString(String str, boolean reIndex) {
			if (!this.str.equals(str)) try {
				this.root.unIndexForStringAndTrigrams(this);
				this.isUpdating = true;
				for (int w = 0; w < this.words.size(); w++) {
					ImWord imw = ((ImWord) this.words.get(w));
					if (!str.equals(imw.getString()))
						imw.setString(str);
				}
				this.str = str;
				this.root.modCount++;
				if (reIndex)
					this.root.indexForStringAndTrigrams(this);
			}
			finally {
				this.isUpdating = false;
			}
		}
		void setBold(boolean bold) {
			if (this.isBold != bold) try {
				this.isUpdating = true;
				for (int w = 0; w < this.words.size(); w++) {
					ImWord imw = ((ImWord) this.words.get(w));
					if (bold != imw.hasAttribute(ImWord.BOLD_ATTRIBUTE))
						imw.setAttribute(ImWord.BOLD_ATTRIBUTE, (bold ? "true" : null));
				}
				this.isBold = bold;
				this.root.modCount++;
			}
			finally {
				this.isUpdating = false;
			}
		}
		void setItalics(boolean italics) {
			if (this.isItalics != italics) try {
				this.isUpdating = true;
				for (int w = 0; w < this.words.size(); w++) {
					ImWord imw = ((ImWord) this.words.get(w));
					if (italics != imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE))
						imw.setAttribute(ImWord.ITALICS_ATTRIBUTE, (italics ? "true" : null));
				}
				this.isItalics = italics;
				this.root.modCount++;
			}
			finally {
				this.isUpdating = false;
			}
		}
	}
	
	/**
	 * Runtime representation of word cluster similarity
	 * 
	 * @author sautter
	 */
	private static class ImWordClusterSimilarity {
		ImWordCluster wc1;
		ImWordCluster wc2;
		float similarity;
		ImWordClusterSimilarity(ImWordCluster wc1, ImWordCluster wc2, float similarity) {
			this.wc1 = wc1;
			this.wc2 = wc2;
			this.similarity = similarity;
		}
	}
	
	/**
	 * Runtime representation of past letter replacements
	 * 
	 * @author sautter
	 */
	private static class ImLetterReplacement {
		final String replaced;
		final String replacement;
		int frequency;
		ImLetterReplacement(String replaced, String replacement, int frequency) {
			this.replaced = replaced;
			this.replacement = replacement;
			this.frequency = frequency;
		}
	}
	
	/**
	 * Root class of runtime cluster hierarchy
	 * 
	 * @author sautter
	 */
	private static class ImWordClustering {
		final String docId;
		HashMap idsToClusters = new HashMap();
		HashMap wordIDsToClusters = new HashMap();
		ArrayList clusterSimilarities = new ArrayList();
		ArrayList letterReplacements = new ArrayList();
		HashMap stringsToClusters = new HashMap();
		int modCount = 0;
		ImWordClustering(String docId) {
			this.docId = docId;
		}
		
		ImWordCluster getClusterForWord(ImWord imw) {
			return this.getClusterForWordId(imw.getLocalID());
		}
		ImWordCluster getClusterForWordId(String wId) {
			return ((ImWordCluster) this.wordIDsToClusters.get(wId));
		}
		
		ImWordCluster getClusterForId(String wcId, ImWord imw) {
			ImWordCluster iwc = ((ImWordCluster) this.idsToClusters.get(wcId));
			if ((iwc == null) && (imw != null)) {
				iwc = new ImWordCluster(this, wcId, imw);
				this.idsToClusters.put(iwc.id, iwc);
				this.modCount++;
			}
			return iwc;
		}
		
		void removeWordCluster(ImWordCluster iwc) {
			this.idsToClusters.remove(iwc.id);
			for (int w = 0; w < iwc.words.size(); w++)
				this.wordIDsToClusters.remove(((ImWord) iwc.words.get(w)).getLocalID());
			this.unIndexForStringAndTrigrams(iwc);
			for (int s = 0; s < this.clusterSimilarities.size(); s++) {
				ImWordClusterSimilarity iwcs = ((ImWordClusterSimilarity) this.clusterSimilarities.get(s));
				if ((iwcs.wc1 == iwc) || (iwcs.wc2 == iwc))
					this.clusterSimilarities.remove(s--);
			}
		}
		
		void addClusterSimilarity(String wcId1, String wcId2, float similarity) {
			ImWordCluster iwc1 = this.getClusterForId(wcId1, null);
			ImWordCluster iwc2 = this.getClusterForId(wcId2, null);
			if ((iwc1 != null) && (iwc2 != null)) {
				this.clusterSimilarities.add(new ImWordClusterSimilarity(iwc1, iwc2, similarity));
				this.modCount++;
			}
		}
		
		/** first cluster: argument, second cluster: associated cluster */
		ImWordClusterSimilarity[] getSimilarClusters(ImWordCluster iwc, boolean transitive) {
			HashMap simWcs = new HashMap();
			int simWcsCount;
			do {
				simWcsCount = simWcs.size();
				for (int s = 0; s < this.clusterSimilarities.size(); s++) {
					ImWordClusterSimilarity iwcs = ((ImWordClusterSimilarity) this.clusterSimilarities.get(s));
					if ((iwcs.wc1 == iwc) || (simWcs.containsKey(iwcs.wc1) && (iwcs.wc2 != iwc))) {
						ImWordClusterSimilarity wcs = ((ImWordClusterSimilarity) simWcs.get(iwcs.wc1));
						if (!simWcs.containsKey(iwcs.wc2))
							simWcs.put(iwcs.wc2, new ImWordClusterSimilarity(iwc, iwcs.wc2, (iwcs.similarity * ((wcs == null) ? 1 : wcs.similarity))));
					}
					else if ((iwcs.wc2 == iwc) || (simWcs.containsKey(iwcs.wc2) && (iwcs.wc1 != iwc))) {
						ImWordClusterSimilarity wcs = ((ImWordClusterSimilarity) simWcs.get(iwcs.wc2));
						if (!simWcs.containsKey(iwcs.wc1))
							simWcs.put(iwcs.wc1, new ImWordClusterSimilarity(iwc, iwcs.wc1, (iwcs.similarity * ((wcs == null) ? 1 : wcs.similarity))));
					}
				}
			} while (transitive && (simWcsCount < simWcs.size()));
			return ((ImWordClusterSimilarity[]) simWcs.values().toArray(new ImWordClusterSimilarity[simWcs.size()]));
		}
		
		void addLetterReplacement(String replaced, String replacement, int frequency) {
			for (int r = 0; r < this.letterReplacements.size(); r++) {
				ImLetterReplacement ilr = ((ImLetterReplacement) this.letterReplacements.get(r));
				if (ilr.replaced.equals(replaced) && ilr.replacement.equals(replacement)) {
					ilr.frequency += frequency;
					this.modCount++;
					return;
				}
			}
			this.letterReplacements.add(new ImLetterReplacement(replaced, replacement, frequency));
			this.modCount++;
		}
		void addLetterReplacements(String oldStr, String newStr, int frequency) {
			WordStringEdit[] wses = getEdits(oldStr, newStr);
			for (int e = 0; e < wses.length; e++)
				this.addLetterReplacement(wses[e].replaced, wses[e].replacement, frequency);
		}
		
		void indexForStringAndTrigrams(ImWordCluster iwc) {
			this.indexForString(iwc, iwc.str);
			for (int s = 0; s < iwc.str.length(); s++) {
				if (127 < iwc.str.charAt(s))
					this.indexForString(iwc, iwc.str.substring(s, (s + 1)));
			}
			for (int s = 0; s < (iwc.str.length() - 1); s++)
				this.indexForString(iwc, iwc.str.substring(s, (s + 2)));
			String tgStr = ("_" + iwc.str + "_");
			for (int s = 0; s < (tgStr.length() - 2); s++)
				this.indexForString(iwc, tgStr.substring(s, (s + 3)));
		}
		void indexForString(ImWordCluster iwc, String str) {
			HashSet strClusters = ((HashSet) this.stringsToClusters.get(str));
			if (strClusters == null) {
				strClusters = new HashSet(2);
				this.stringsToClusters.put(str, strClusters);
			}
			strClusters.add(iwc);
		}
		
		void unIndexForStringAndTrigrams(ImWordCluster iwc) {
			for (int s = 0; s < iwc.str.length(); s++) {
				if (127 < iwc.str.charAt(s))
					this.unIndexForString(iwc, iwc.str.substring(s, (s + 1)));
			}
			for (int s = 0; s < (iwc.str.length() - 1); s++)
				this.unIndexForString(iwc, iwc.str.substring(s, (s + 2)));
			this.unIndexForString(iwc, iwc.str);
			String tgStr = ("_" + iwc.str + "_");
			for (int s = 0; s < (tgStr.length() - 2); s++)
				this.unIndexForString(iwc, tgStr.substring(s, (s + 3)));
		}
		void unIndexForString(ImWordCluster iwc, String str) {
			HashSet strClusters = ((HashSet) this.stringsToClusters.get(str));
			if (strClusters != null)
				strClusters.remove(iwc);
		}
		
		ImWordCluster[] getForString(String str) {
			HashSet strClusters = ((HashSet) this.stringsToClusters.get(str));
			return ((strClusters == null) ? null : ((ImWordCluster[]) strClusters.toArray(new ImWordCluster[strClusters.size()])));
		}
	}
	
	private HashMap docIDsToWordClusterings = new HashMap(3);
	
	private ImWordClustering getWordClustering(ImDocument doc, boolean create, ProgressMonitor pm) {
		
		//	do cache lookup first
		ImWordClustering wordClustering = ((ImWordClustering) this.docIDsToWordClusterings.get(doc.docId));
		if (wordClustering != null)
			return wordClustering;
		
		//	attempt to load from supplement
		ImSupplement wordClusteringData = doc.getSupplement("ocrWordClustering");
		if (wordClusteringData != null) try {
			wordClustering = readWordClustering(doc, wordClusteringData);
		} catch (IOException ioe) {}
		
		//	create word clustering from scratch
		if ((wordClustering == null) && create)
			wordClustering = this.createWordClustering(doc, pm);
		
		//	what do we have?
		if (wordClustering != null) {
			
			//	cache word clustering
			this.docIDsToWordClusterings.put(wordClustering.docId, wordClustering);
			
			//	overwrite generic supplement implementation with our own, so data is updated on storage
			doc.addSupplement(new ImWordClusteringSupplement(doc, wordClustering));
		}
		
		//	finally ...
		return wordClustering;
	}
	
	private static class ImWordClusteringSupplement extends ImSupplement {
		private ImWordClustering wordClustering;
		private byte[] wordClusteringBytes = null;
		private int wordClusteringBytesModCount = -1;
		ImWordClusteringSupplement(ImDocument doc, ImWordClustering wordClustering) {
			super(doc, "ocrWordClustering", "text/plain");
			this.wordClustering = wordClustering;
		}
		public String getId() {
			return "ocrWordClustering";
		}
		public InputStream getInputStream() throws IOException {
			if ((this.wordClusteringBytes == null) || (this.wordClusteringBytesModCount < this.wordClustering.modCount)) {
				ByteArrayOutputStream wcBytes = new ByteArrayOutputStream();
				writeWordClustering(this.wordClustering, wcBytes);
				this.wordClusteringBytes = wcBytes.toByteArray();
				this.wordClusteringBytesModCount = this.wordClustering.modCount;
			}
			return new ByteArrayInputStream(this.wordClusteringBytes);
		}
	}
	
	/*
	Word Clustering Persistence Format:
	- use "ocrWordClustering" custom supplement,type "text/plain"
	- tab separated two-section TXT file, sections separated by blank line
	- section 1: word clusters
	  - cluster ID (creation time hash code)
	  - OCR string
	  - style (bold?, italics?)
	  - member word IDs
	  - representative width (int)
	  - representative height (int)
	  - representative bytes (hex, or base64)
	- section 2: word cluster similarities
	  - first cluster ID (creation time hash code)
	  - second cluster ID (creation time hash code)
	  - similarity (float)
	 */
	
	private static ImWordClustering readWordClustering(ImDocument doc, ImSupplement wordClusteringData) throws IOException {
		ImWordClustering wordClustering = new ImWordClustering(doc.docId);
		BufferedReader wcBr = new BufferedReader(new InputStreamReader(wordClusteringData.getInputStream(), "UTF-8"));
		
		//	read word clusters (up to separator blank line)
		for (String wcLine; (wcLine = wcBr.readLine()) != null;) {
			wcLine = wcLine.trim();
			if (wcLine.length() == 0)
				break;
			String[] wcData = wcLine.split("\\t");
			if (wcData.length < 7)
				continue;
			
			//	add words, creating cluster on first valid word
			String[] wordIDs = wcData[3].split("\\;");
			ImWordCluster wc = null;
			for (int w = 0; w < wordIDs.length; w++) {
				ImWord imw = doc.getWord(wordIDs[w]);
				if (imw == null)
					continue;
				if (wc == null)
					wc = wordClustering.getClusterForId(wcData[0], imw);
				else wc.addWord(imw);
			}
			
			//	no valid words at all ...
			if (wc == null)
				continue;
			
			//	load representative dimensions
			int wcrWidth = Integer.parseInt(wcData[4]);
			int wcrHeight = Integer.parseInt(wcData[5]);
			
			//	load representative HEX data
			wc.representative = new BufferedImage(wcrWidth, wcrHeight, BufferedImage.TYPE_BYTE_BINARY);
			int r = 0;
			int c = 0;
			for (int b = 0; b < wcData[6].length(); b++) {
				int h = "0123456789ABCDEF".indexOf(wcData[6].charAt(b));
				for (int i = 0; i < 4; i++) {
					if ((c + i) < wcrWidth)
						wc.representative.setRGB((c + i), r, (((h & 8) == 0) ? whiteRgb : blackRgb));
					h <<= 1;
				}
				c += 4;
				if (wcrWidth <= c) {
					c = 0;
					r++;
				}
			}
		}
		
		//	read word cluster similarities (up to separator blank line)
		for (String wcLine; (wcLine = wcBr.readLine()) != null;) {
			wcLine = wcLine.trim();
			if (wcLine.length() == 0)
				break;
			String[] wcsData = wcLine.split("\\t");
			if (wcsData.length == 3)
				wordClustering.addClusterSimilarity(wcsData[0], wcsData[1], Float.parseFloat(wcsData[2]));
		}
		
		//	read replaced letters
		for (String wcLine; (wcLine = wcBr.readLine()) != null;) {
			wcLine = wcLine.trim();
			if (wcLine.length() == 0)
				break;
			String[] wcsData = wcLine.split("\\t");
			if (wcsData.length == 3)
				wordClustering.addLetterReplacement(wcsData[0], wcsData[1], Integer.parseInt(wcsData[2]));
		}
		
		//	finally ...
		return wordClustering;
	}
	
	private static void writeWordClustering(ImWordClustering wordClustering, OutputStream wcOut) throws IOException {
		BufferedWriter wcBw = new BufferedWriter(new OutputStreamWriter(wcOut, "UTF-8"));
		
		//	write word clusters
		for (Iterator wcit = wordClustering.idsToClusters.keySet().iterator(); wcit.hasNext();) {
			ImWordCluster wc = ((ImWordCluster) wordClustering.idsToClusters.get(wcit.next()));
			wcBw.write(wc.id);
			wcBw.write("\t");
			wcBw.write(wc.str);
			wcBw.write("\t");
			wcBw.write("" + ((wc.isBold ? Font.BOLD : Font.PLAIN) | (wc.isItalics ? Font.ITALIC : Font.PLAIN)));
			wcBw.write("\t");
			for (int w = 0; w < wc.words.size(); w++) {
				if (w != 0)
					wcBw.write(";");
				wcBw.write(((ImWord) wc.words.get(w)).getLocalID());
			}
			wcBw.write("\t");
			wcBw.write("" + wc.representative.getWidth());
			wcBw.write("\t");
			wcBw.write("" + wc.representative.getHeight());
			wcBw.write("\t");
			
			//	write representative HEX data (4 pixels per char)
			AnalysisImage wcrAi = Imaging.wrapImage(wc.representative, null);
			byte[][] wcrBrightness = wcrAi.getBrightness();
			for (int r = 0; r < wc.representative.getHeight(); r++)
				for (int c = 0; c < wc.representative.getWidth(); c += 4) {
					int b = 0;
					for (int i = 0; i < 4; i++) {
						if (i != 0)
							b <<= 1;
						if (((c + i) < wc.representative.getWidth()) && (wcrBrightness[c + i][r] < 80))
							b++;
					}
					wcBw.write("0123456789ABCDEF".substring(b, (b+1)));
				}
			wcBw.newLine();
		}
		
		//	write separator blank line
		wcBw.newLine();
		
		//	write word cluster similarities
		for (int s = 0; s < wordClustering.clusterSimilarities.size(); s++) {
			ImWordClusterSimilarity iwcs = ((ImWordClusterSimilarity) wordClustering.clusterSimilarities.get(s));
			wcBw.write(iwcs.wc1.id);
			wcBw.write("\t");
			wcBw.write(iwcs.wc2.id);
			wcBw.write("\t");
			wcBw.write("" + iwcs.similarity);
			wcBw.newLine();
		}
		
		//	write separator blank line
		wcBw.newLine();
		
		//	write letter replacements
		for (int r = 0; r < wordClustering.clusterSimilarities.size(); r++) {
			ImLetterReplacement ilr = ((ImLetterReplacement) wordClustering.letterReplacements.get(r));
			wcBw.write(ilr.replaced);
			wcBw.write("\t");
			wcBw.write(ilr.replacement);
			wcBw.write("\t");
			wcBw.write("" + ilr.frequency);
			wcBw.newLine();
		}
		
		//	finally ...
		wcBw.flush();
	}
	
	/**
	 * Check OCR on a scanned document. In particular, this method renders OCR
	 * results compares them to the words in the original page images.
	 * Afterwards, below-average matches are highlighted for correction. On
	 * top of detecting 'optical' mismatches, this method also detects bold
	 * face. Words whose text stream type is 'artifact' or 'deleted' are ignored.
	 * @param doc the document to process
	 * @param pm a progress monitor to inform on processing
	 */
	public void checkOcr(ImDocument doc, ProgressMonitor pm) {
		
		/* TODO Maybe also try and detect font _face_
		 * - use super-rendering on large clusters in the various available font families
		 * - measure out which font fits best on average ...
		 * - ... and use that for further correction
		 * ==> might help with documents using odd fonts that strongly deviate from Serif
		 * ==> might also help detecting serif vs. sans-serif
		 * ==> might even help with small-caps
		 */
		
		//	get word clustering
		final boolean debug = this.debug;
		this.debug = false;
		final ImWordClustering wordClustering = this.getWordClustering(doc, true, pm);
		this.debug = debug;
		
		//	compute average DPI
		ImPage[] pages = doc.getPages();
		int pageImageDpiSum = 0;
		for (int p = 0; p < pages.length; p++) {
			PageImage pi = pages[p].getPageImage();
			pageImageDpiSum += pi.currentDpi;
		}
		final int pageImageDpi = (pageImageDpiSum / pages.length);
		
		//	line up word clusters
		final ArrayList wordClusters = new ArrayList();
		for (Iterator wcit = wordClustering.idsToClusters.keySet().iterator(); wcit.hasNext();)
			wordClusters.add(wordClustering.idsToClusters.get(wcit.next()));
		
		//	render OCR result over each cluster representative
		final List matches = Collections.synchronizedList(new ArrayList());
		final Map matchesToWordClusters = Collections.synchronizedMap(new HashMap());
		final SimCountingSet nGramSims = new SimCountingSet();
		final SimCountingSet nGramWaSims = new SimCountingSet();
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int c) throws Exception {
				ImWordCluster wc = ((ImWordCluster) wordClusters.get(c));
				
				//	ignore numbers, at least for now, as they are a lot less likely to repeat than words
				if (Gamta.isNumber(wc.str))
					return;
				
				//	ignore Roman numbers, at least for now, as they rarely mis-OCR
				if (Gamta.isRomanNumber(wc.str.replaceAll("[^a-zA-Z]", "")))
					return;
				
				//	measure similarity (in all 8 combinations of bold, italics, and serifs)
				System.out.println("Super-rendering OCR result for cluster '" + wc.str + "'");
				WordImageMatch swim = this.getMatch(wc, "Serif");
				System.out.println(" - serif similarity is " + swim.sim + ", weight adjusted " + swim.waSim);
				WordImageMatch pwim = this.getMatch(wc, "Sans");
				System.out.println(" - sans-serif similarity is " + pwim.sim + ", weight adjusted " + pwim.waSim);
				WordImageMatch wim = ((pwim.sim < swim.sim) ? swim : pwim);
				System.out.println(" --> similarity is " + wim.sim + ", weight adjusted " + wim.waSim);
				
				//	this one's just too bad, likely an artifact
				if (wim.sim < 0.10f)
					return;
				
				//	store better matching version for further processing
				matches.add(wim);
				matchesToWordClusters.put(wim, wc);
				
				//	use '_' to mark word boundaries in bi- and trigrams
				String btgStr = ("_" + wc.str + "_");
				
				//	store n-gram frequencies and similarities
				for (int oc = 0; oc < wc.str.length(); oc++) {
					nGramSims.add(wc.str.substring(oc, (oc+1)), wim.sim, wc.words.size());
					nGramWaSims.add(wc.str.substring(oc, (oc+1)), wim.waSim, wc.words.size());
				}
				for (int bc = 0; bc < (btgStr.length() - 1); bc++) {
					nGramSims.add(btgStr.substring(bc, (bc+2)), wim.sim, wc.words.size());
					nGramWaSims.add(btgStr.substring(bc, (bc+2)), wim.waSim, wc.words.size());
				}
				for (int tc = 0; tc < (btgStr.length() - 2); tc++) {
					nGramSims.add(btgStr.substring(tc, (tc+3)), wim.sim, wc.words.size());
					nGramWaSims.add(btgStr.substring(tc, (tc+3)), wim.waSim, wc.words.size());
				}
			}
			private WordImageMatch getMatch(ImWordCluster wc, String fontName) {
				
				//	render OCR result (both plain and italics, just to be sure)
				WordImage cwi = new WordImage(wc.representative, wc.str, wc.isItalics, pageImageDpi);
				BoundingBox wcrBox = new BoundingBox(0, wc.representative.getWidth(), 0, wc.representative.getHeight());
				
				//	measure non-bold similarity
				WordImage pprwi = WordImageAnalysis.renderWordImage(wc.str, wcrBox, -1, fontName, Font.PLAIN, 10, pageImageDpi, null);
				WordImageMatch ppwim = WordImageAnalysis.matchWordImages(cwi, pprwi, true, true);
				System.out.println(" - plain similarity is " + ppwim.sim + ", weight adjusted " + ppwim.waSim);
				WordImage pirwi = WordImageAnalysis.renderWordImage(wc.str, wcrBox, -1, fontName, Font.ITALIC, 10, pageImageDpi, null);
				WordImageMatch piwim = WordImageAnalysis.matchWordImages(cwi, pirwi, true, true);
				System.out.println(" - italics similarity is " + piwim.sim + ", weight adjusted " + piwim.waSim);
				WordImageMatch pwim = ((ppwim.sim < piwim.sim) ? piwim : ppwim);
				
				//	if cluster is non bold, we're done here
				if (!wc.isBold)
					return pwim;
				
				//	measure bold similarity is cluster is bold
				WordImage bprwi = WordImageAnalysis.renderWordImage(wc.str, wcrBox, -1, fontName, Font.BOLD, 10, pageImageDpi, null);
				WordImageMatch bpwim = WordImageAnalysis.matchWordImages(cwi, bprwi, true, true);
				System.out.println(" - bold similarity is " + bpwim.sim + ", weight adjusted " + bpwim.waSim);
				WordImage birwi = WordImageAnalysis.renderWordImage(wc.str, wcrBox, -1, fontName, (Font.BOLD | Font.ITALIC), 10, pageImageDpi, null);
				WordImageMatch biwim = WordImageAnalysis.matchWordImages(cwi, birwi, true, true);
				System.out.println(" - bold-italics similarity is " + biwim.sim + ", weight adjusted " + biwim.waSim);
				WordImageMatch bwim = ((bpwim.sim < biwim.sim) ? biwim : bpwim);
				
				//	pick best overall match
				return ((pwim.sim < bwim.sim) ? bwim : pwim);
			}
		}, wordClusters.size(), (this.debug ? 1 : -1));
		
		//	compute average similarity
		float simSum = 0;
		float waSimSum = 0;
		for (int m = 0; m < matches.size(); m++) {
			WordImageMatch wim = ((WordImageMatch) matches.get(m));
			simSum += wim.sim;
			waSimSum += wim.waSim;
		}
		float avgSim = (simSum / matches.size());
		float avgWaSim = (waSimSum / matches.size());
		System.out.println("Average similarity from " + matches.size() + " matches is " + avgSim + ", weight adjusted " + avgWaSim);
		
		//	compute word cluster metrics
		ArrayList wordClusterMetrics = new ArrayList();
		for (int m = 0; m < matches.size(); m++) { // TODO parallelize this
			WordImageMatch wim = ((WordImageMatch) matches.get(m));
			ImWordCluster wc = ((ImWordCluster) matchesToWordClusters.get(wim));
			
			ExtendedWordImageMatch ewim = new ExtendedWordImageMatch(wim);
			ewim.computeMatchHistograms();
			ewim.computeMatchHistogramMetrics();
			ewim.computeMatchAreaSurfaces();
			ewim.computeMisMatchDistances();
			
			//	use '_' to mark word boundaries in bi- and trigrams
			String btgStr = ("_" + wc.str + "_");
			float trigramSimSum = 0;
			float minTrigramSim = 1;
			int trigramFreqSum = 0;
			int minTrigramFreq = Integer.MAX_VALUE;
			for (int tc = 0; tc < (btgStr.length() - 2); tc++) {
				String trigram = btgStr.substring(tc, (tc + 3));
				float trigramSim = nGramSims.getSim(trigram);
				trigramSimSum += trigramSim;
				minTrigramSim = Math.min(minTrigramSim, trigramSim);
				int trigramFreq = nGramSims.getCount(trigram);
				trigramFreqSum += trigramFreq;
				minTrigramFreq = Math.min(minTrigramFreq, trigramFreq);
			}
			float avgTrigramSim = (trigramSimSum / (btgStr.length() - 2));
			int avgTrigramFreq = (trigramFreqSum / (btgStr.length() - 2));
			
			ImWordClusterMetrics wcm = new ImWordClusterMetrics(wc, ewim, avgTrigramSim, minTrigramSim, (avgTrigramSim / wim.sim), avgTrigramFreq, minTrigramFreq);
			
			for (int r = 0; r < wordClustering.letterReplacements.size(); r++) {
				ImLetterReplacement lr = ((ImLetterReplacement) wordClustering.letterReplacements.get(r));
				if (lr.replaced.length() == 0)
					continue;
				if (wc.str.indexOf(lr.replaced) == -1)
					continue;
				
				if (wcm.replacedLetterCombs == null)
					wcm.replacedLetterCombs = new LinkedHashSet(3);
				wcm.replacedLetterCombs.add(lr.replaced);
				wcm.replacedLetterCombScore += (lr.frequency * lr.replaced.length() * lr.replaced.length() * lr.replaced.length());
				
				for (int c = (lr.replaced.equalsIgnoreCase(lr.replacement) ? 1 : 0); c < (wc.str.length() - lr.replaced.length() + 1); c++) {
					if (!wc.str.startsWith(lr.replaced, c))
						continue;
					char bCh = ((c == 0) ? '_' : wc.str.charAt(c-1));
					char aCh = (((c + lr.replaced.length()) == wc.str.length()) ? '_' : wc.str.charAt(c + lr.replaced.length()));
					int oTrigramFreq = 0;
					if (lr.replaced.length() == 1)
						oTrigramFreq += nGramSims.getCount(bCh + lr.replaced + aCh);
					else if (lr.replaced.length() >= 2) {
						int boTrigramFreq = nGramSims.getCount(bCh + lr.replaced.substring(0, 2));
						int aoTrigramFreq = nGramSims.getCount(lr.replaced.substring(lr.replaced.length() - 2) + aCh);
						if ((boTrigramFreq != 0) && (aoTrigramFreq != 0))
							oTrigramFreq += ((boTrigramFreq + aoTrigramFreq) / 2);
					}
					int rTrigramFreq = 0;
					if (lr.replacement.length() == 1)
						rTrigramFreq += nGramSims.getCount(bCh + lr.replacement + aCh);
					else if (lr.replacement.length() >= 2) {
						int brTrigramFreq = nGramSims.getCount(bCh + lr.replacement.substring(0, 2));
						int arTrigramFreq = nGramSims.getCount(lr.replacement.substring(lr.replacement.length() - 2) + aCh);
						if ((brTrigramFreq != 0) && (arTrigramFreq != 0))
							rTrigramFreq += ((brTrigramFreq + arTrigramFreq) / 2);
						}
					if (rTrigramFreq > oTrigramFreq) {
						if (wcm.letterCombReplacements == null)
							wcm.letterCombReplacements = new LinkedHashSet(3);
						wcm.letterCombReplacements.add((bCh + lr.replaced + aCh) + " (" + oTrigramFreq + ") --> " + (bCh + lr.replacement + aCh) + " (" + rTrigramFreq + ")");
						wcm.replacedLetterTrigramFreqDiff += (rTrigramFreq - oTrigramFreq);
					}
				}
			}
			
			ImWordClusterSimilarity[] swcs = wordClustering.getSimilarClusters(wc, true);
			for (int s = 0; s < swcs.length; s++) {
				if (wc.str.equals(swcs[s].wc2.str))
					wcm.sameStringWcCount++;
				else wcm.simImageWcCount++;
			}
			
			wordClusterMetrics.add(wcm);
		}
		
		//	score word clusters (kind of k-way-sorting)
		//	- weight adjusted similarity
		scoreWordClusterMetrics(wordClusterMetrics, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				ImWordClusterMetrics wcm1 = ((ImWordClusterMetrics) obj1);
				ImWordClusterMetrics wcm2 = ((ImWordClusterMetrics) obj2);
				return Float.compare(wcm1.ewim.base.waSim, wcm2.ewim.base.waSim);
			}
		}, 1, "WaSim");
		//	- minimum letter similarity
		scoreWordClusterMetrics(wordClusterMetrics, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				ImWordClusterMetrics wcm1 = ((ImWordClusterMetrics) obj1);
				ImWordClusterMetrics wcm2 = ((ImWordClusterMetrics) obj2);
				return Float.compare(wcm1.ewim.minLetterSim, wcm2.ewim.minLetterSim);
			}
		}, 1, "MinLetterSim");
		//	- relative maximum distance (sort descending)
		scoreWordClusterMetrics(wordClusterMetrics, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				ImWordClusterMetrics wcm1 = ((ImWordClusterMetrics) obj1);
				ImWordClusterMetrics wcm2 = ((ImWordClusterMetrics) obj2);
				return Float.compare(wcm2.relMaxDist, wcm1.relMaxDist);
			}
		}, 1, "RelMaxDist");
		//	- square average distance (sort descending)
		scoreWordClusterMetrics(wordClusterMetrics, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				ImWordClusterMetrics wcm1 = ((ImWordClusterMetrics) obj1);
				ImWordClusterMetrics wcm2 = ((ImWordClusterMetrics) obj2);
				return Float.compare(wcm2.avgSqrDist, wcm1.avgSqrDist);
			}
		}, 1, "SqrAvgDist");
//		//	- min trigram frequencies
//		scoreWordClusterMetrics(wordClusterMetrics, new Comparator() {
//			public int compare(Object obj1, Object obj2) {
//				ImWordClusterMetrics wcm1 = ((ImWordClusterMetrics) obj1);
//				ImWordClusterMetrics wcm2 = ((ImWordClusterMetrics) obj2);
//				return Float.compare(wcm1.minTrigramFreq, wcm2.minTrigramFreq);
//			}
//		}, 1, "MinTrigramFreq");
		//	- avg trigram frequencies
		scoreWordClusterMetrics(wordClusterMetrics, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				ImWordClusterMetrics wcm1 = ((ImWordClusterMetrics) obj1);
				ImWordClusterMetrics wcm2 = ((ImWordClusterMetrics) obj2);
				return Float.compare(wcm1.avgTrigramFreq, wcm2.avgTrigramFreq);
			}
		}, 1, "AvgTrigramFreq");
		//	- avg trigram similarity relations (sort descending)
		scoreWordClusterMetrics(wordClusterMetrics, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				ImWordClusterMetrics wcm1 = ((ImWordClusterMetrics) obj1);
				ImWordClusterMetrics wcm2 = ((ImWordClusterMetrics) obj2);
				return Float.compare(wcm2.avgTrigramSimRel, wcm1.avgTrigramSimRel);
			}
		}, 1, "AvgTrigramSimRel");
		//	- number of frequently corrected letter combinations (sort descending)
		scoreWordClusterMetrics(wordClusterMetrics, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				ImWordClusterMetrics wcm1 = ((ImWordClusterMetrics) obj1);
				ImWordClusterMetrics wcm2 = ((ImWordClusterMetrics) obj2);
				return (wcm2.replacedLetterCombScore - wcm1.replacedLetterCombScore);
			}
		}, 1, "LetterReplacements");
		
		//	sort matches by similarity
		Collections.sort(wordClusterMetrics, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				ImWordClusterMetrics wcm1 = ((ImWordClusterMetrics) obj1);
				ImWordClusterMetrics wcm2 = ((ImWordClusterMetrics) obj2);
				return (wcm2.errorPosScore - wcm1.errorPosScore);
			}
		});
//		Collections.sort(matches, new Comparator() {
//			public int compare(Object obj1, Object obj2) {
//				WordImageMatch wim1 = ((WordImageMatch) obj1);
//				WordImageMatch wim2 = ((WordImageMatch) obj2);
//				return Float.compare(wim1.waSim, wim2.waSim);
//			}
//		});
		
		//	display words worst match first
		if (this.debug) {
			for (int m = 0; m < wordClusterMetrics.size(); m++) {
				ImWordClusterMetrics wcm = ((ImWordClusterMetrics) wordClusterMetrics.get(m));
				ImWordCluster wc = wcm.wc;
				ExtendedWordImageMatch ewim = wcm.ewim;
				WordImageMatch wim = ewim.base;
				if (wc.str.matches("[A-ZÄÖÜ\\-\\']{3,}"))
					continue;
				System.out.println("Bad OCR match '" + wc.str + "' (" + wc.words.size() + " words" + (wc.isBold ? ", bold" : "") + (wc.isItalics ? ", italics" : "") + ") at " + m + " of " + matches.size() + ": similarity is " + wim.sim + ", weight adjusted " + wim.waSim);
				System.out.println(" - shifts are: left " + wim.leftShift + ", right " + wim.rightShift + ", top " + wim.topShift + ", bottom " + wim.bottomShift);
				System.out.println(" - area proportion distance is " + Math.abs(wim.scanned.boxProportion - wim.rendered.boxProportion));
				System.out.println(" - similarity is " + wim.sim + " (" + wim.precision + "/" + wim.recall + ")");
				System.out.println(" - weight adjusted similarity is " + wim.waSim + " (" + wim.waPrecision + "/" + wim.waRecall + ")");
				System.out.println(" - min letter similarity is " + ewim.minLetterSim + ((ewim.minLetterSimBucket == -1) ? "" : (" at " + ewim.minLetterSimBucket + " in " + Arrays.toString(ewim.minLetterSimBuckets))));
				System.out.println(" - max split similarity contrast is " + ewim.maxSimBucketContrast + ", max deviation is " + ewim.maxSimBucketDeviation);
				System.out.println(" - avg split similarity contrast is " + ewim.simBucketContrast + ", avg deviation is " + ewim.simBucketDeviation);
				System.out.println(" - sqr-avg split similarity contrast is " + ewim.simBucketContrastSqr + ", sqr-avg deviation is " + ewim.simBucketDeviationSqr);
				System.out.println(" - matched " + wim.matched + ", surface " + ewim.matchedSurface);
				System.out.println(" - spurious " + wim.scannedOnly + ", surface " + ewim.scannedOnlySurface + ", avg distance " + ewim.scannedOnlyAvgDist + ", avg sqr distance " + ewim.scannedOnlyAvgSqrDist + ", max distance " + ewim.scannedOnlyMaxDist);
				System.out.println(" - missed " + wim.renderedOnly + ", surface " + ewim.renderedOnlySurface + ", avg distance " + ewim.renderedOnlyAvgDist + ", avg sqr distance " + ewim.renderedOnlyAvgSqrDist + ", max distance " + ewim.renderedOnlyMaxDist);
				
				ImWordClusterSimilarity[] wcss = wordClustering.getSimilarClusters(wc, true);
				int sameStringCount = 0;
				System.out.print(" - similar words:");
				if (wcss.length == 0)
					System.out.println(" none");
				else {
					for (int s = 0; s < wcss.length; s++) {
						if (wc.str.equals(wcss[s].wc2.str))
							sameStringCount++; 
						System.out.print(" '" + wcss[s].wc2.str + "' (" + wcss[s].wc2.words.size() + " words, at " + wcss[s].similarity + ")");
					}
					System.out.println("");
				}
				
				int wordFontSizeSum = 0;
				int wordFontSizeCount = 0;
				for (int w = 0; w < wc.words.size(); w++) {
					ImWord imw = ((ImWord) wc.words.get(w));
					if (imw.hasAttribute(ImWord.FONT_SIZE_ATTRIBUTE)) {
						wordFontSizeSum += Integer.parseInt((String) imw.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE));
						wordFontSizeCount++;
					}
				}
				int fontSizeDiff = Math.abs(wim.rendered.fontSize - ((wordFontSizeSum + (wordFontSizeCount / 2)) / wordFontSizeCount));
				
				System.out.println(" - avg trigram count is " + wcm.avgTrigramFreq + ", minimum is " + wcm.minTrigramFreq);
				System.out.println(" - avg trigram sim is " + wcm.avgTrigramSim + ", minimum is " + wcm.minTrigramSim);
				System.out.println(" - trigram sim relation is " + wcm.avgTrigramSimRel);
				System.out.println(" - letter replacement score is " + wcm.replacedLetterCombScore + " for " + wcm.replacedLetterCombs);
				System.out.println(" - letter replacement trigram frequency difference is " + wcm.replacedLetterTrigramFreqDiff + " for " + wcm.letterCombReplacements);
				System.out.println(" - error position score is " + wcm.errorPosScore + " for " + wcm.errorReasons);
				
				int v = verifyWordOCR(ewim, "Bad OCR match");
				if (v == JOptionPane.YES_OPTION) {
					this.goodOcrCount++;
					if (wcm.errorReasons != null)
						this.goodOcrErrorReasons.addAll(wcm.errorReasons);
					
					this.goodOcrClusterSizes.add(new Integer(wc.words.size()));
					this.goodOcrClusterSims.add(new Integer(sameStringCount));
					this.goodOcrFontSizeDiffs.add(new Integer(fontSizeDiff));
					
					this.goodOcrLetterReplacementScores.add(new Integer(wcm.replacedLetterCombScore));
					this.goodOcrLetterReplacementTrigramFreqDiffs.add(new Integer(wcm.replacedLetterTrigramFreqDiff));
					
					this.goodOcrSims.add(new Integer((int) (wim.sim * 100)));
					this.goodOcrWaSims.add(new Integer((int) (wim.waSim * 100)));
					this.goodOcrMinLetterSims.add(new Integer((int) (ewim.minLetterSim * 100)));
//					this.goodOcrTopSims.add(new Integer((int) (topSim * 100)));
//					this.goodOcrTopMinLetterSims.add(new Integer((int) (topMinLetterSim * 100)));
//					this.goodOcrTopSimsBySims.add(new Integer((int) ((topSim / wim.base.sim) * 100)));
//					this.goodOcrMaxLetterErrorToError.add(new Integer((wim.base.sim == 1) ? 100 : ((int) (((1 - wim.minLetterSim) / (1 - wim.base.sim)) * 100))));
					this.goodOcrMaxContrasts.add(new Integer((int) (ewim.maxSimBucketContrast * 100)));
					this.goodOcrAvgContrasts.add(new Integer((int) (ewim.simBucketContrast * 100)));
					this.goodOcrAvgDists.add(new Integer((int) ((ewim.scannedOnlyAvgDist + ewim.renderedOnlyAvgDist) * 50)));
					this.goodOcrAvgSqrDists.add(new Integer((int) ((ewim.scannedOnlyAvgSqrDist + ewim.renderedOnlyAvgSqrDist) * 50)));
					this.goodOcrMaxDists.add(new Integer(Math.max(ewim.scannedOnlyMaxDist, ewim.renderedOnlyMaxDist)));
					this.goodOcrRelMaxDists.add(new Integer((Math.max(ewim.scannedOnlyMaxDist, ewim.renderedOnlyMaxDist) * 100) / ewim.matchedHist.length));
					this.goodOcrMinTrigramCounts.add(new Integer(wcm.minTrigramFreq));
					this.goodOcrAvgTrigramCounts.add(new Integer(wcm.avgTrigramFreq));
					this.goodOcrMinTrigramSims.add(new Integer((int) (wcm.minTrigramSim * 100)));
					this.goodOcrAvgTrigramSims.add(new Integer((int) (wcm.avgTrigramSim * 100)));
					this.goodOcrAvgTrigramSimRels.add(new Integer((int) (wcm.avgTrigramSimRel * 100)));
					this.goodOcrErrorPosScores.add(new Integer(wcm.errorPosScore));
				}
				else if (v == JOptionPane.NO_OPTION) {
					this.badOcrCount++;
					if (wcm.errorReasons != null)
						this.badOcrErrorReasons.addAll(wcm.errorReasons);
					
					this.badOcrClusterSizes.add(new Integer(wc.words.size()));
					this.badOcrClusterSims.add(new Integer(sameStringCount));
					this.badOcrFontSizeDiffs.add(new Integer(fontSizeDiff));
					
					this.badOcrLetterReplacementScores.add(new Integer(wcm.replacedLetterCombScore));
					this.badOcrLetterReplacementTrigramFreqDiffs.add(new Integer(wcm.replacedLetterTrigramFreqDiff));
					
					this.badOcrSims.add(new Integer((int) (wim.sim * 100)));
					this.badOcrWaSims.add(new Integer((int) (wim.waSim * 100)));
					this.badOcrMinLetterSims.add(new Integer((int) (ewim.minLetterSim * 100)));
//					this.badOcrTopSims.add(new Integer((int) (topSim * 100)));
//					this.badOcrTopMinLetterSims.add(new Integer((int) (topMinLetterSim * 100)));
//					this.badOcrTopSimsBySims.add(new Integer((int) ((topSim / wim.base.sim) * 100)));
//					this.badOcrMaxLetterErrorToError.add(new Integer((wim.base.sim == 1) ? 100 : ((int) (((1 - wim.minLetterSim) / (1 - wim.base.sim)) * 100))));
					this.badOcrMaxContrasts.add(new Integer((int) (ewim.maxSimBucketContrast * 100)));
					this.badOcrAvgContrasts.add(new Integer((int) (ewim.simBucketContrast * 100)));
					this.badOcrAvgDists.add(new Integer((int) ((ewim.scannedOnlyAvgDist + ewim.renderedOnlyAvgDist) * 50)));
					this.badOcrAvgSqrDists.add(new Integer((int) ((ewim.scannedOnlyAvgSqrDist + ewim.renderedOnlyAvgSqrDist) * 50)));
					this.badOcrMaxDists.add(new Integer(Math.max(ewim.scannedOnlyMaxDist, ewim.renderedOnlyMaxDist)));
					this.badOcrRelMaxDists.add(new Integer((Math.max(ewim.scannedOnlyMaxDist, ewim.renderedOnlyMaxDist) * 100) / ewim.matchedHist.length));
					this.badOcrMinTrigramCounts.add(new Integer(wcm.minTrigramFreq));
					this.badOcrAvgTrigramCounts.add(new Integer(wcm.avgTrigramFreq));
					this.badOcrMinTrigramSims.add(new Integer((int) (wcm.minTrigramSim * 100)));
					this.badOcrAvgTrigramSims.add(new Integer((int) (wcm.avgTrigramSim * 100)));
					this.badOcrAvgTrigramSimRels.add(new Integer((int) (wcm.avgTrigramSimRel * 100)));
					this.badOcrErrorPosScores.add(new Integer(wcm.errorPosScore));
				}
				else if (v != JOptionPane.CANCEL_OPTION)
					break;
			}
		}
		else {
			ArrayList errorWordClusters = new ArrayList();
			for (int m = 0; m < wordClusterMetrics.size(); m++)
				errorWordClusters.add(((ImWordClusterMetrics) wordClusterMetrics.get(m)).wc);
			HashSet handledWordClusters = new HashSet();
			handleErrorWordClusters(errorWordClusters, handledWordClusters, null);
		}
		
		//	TODO also use patterns, e.g. to catch 'I' vs. 'l' vs. '1'
		
		/* TODO Still, also use originally conceived OCR super-rendering similarity for OCR assessment
		- list word clusters with worst-matching similarity at the top
		- still, clustering vastly decreases correction effort
		- compute document average (maybe separately for each font style) ...
		- ... and use that as a cutoff
		
		Do only super-rendering here, though
		 */
	}
	
	//	TODO make this a protected instance method, so it can be overwritten to use ReCAPTCHA for server side deployment & crowdsourcing
	
	private void handleErrorWordClusters(ArrayList errorWordClusters, final HashSet handledWordClusters, String similarTo) {
		if (errorWordClusters.size() == 0)
			return;
		ImWordClusterListDialog wcld = new ImWordClusterListDialog(((ImWordCluster) errorWordClusters.get(0)), null, null, false);
		for (int c = 1; c < errorWordClusters.size(); c++)
			wcld.enqueueWordCluster((ImWordCluster) errorWordClusters.get(c));
		wcld.setLocationRelativeTo(null);
		wcld.setVisible(true);
	}
//	private static void handleErrorWordClusters(ArrayList errorWordClusters, final HashSet handledWordClusters, String similarTo) {
//		HashSet hWordClusters = ((similarTo == null) ? null : new HashSet());
//		for (int c = 0; c < errorWordClusters.size(); c++) {
//			ImWordCluster wc = ((ImWordCluster) errorWordClusters.get(c));
//			if (handledWordClusters.contains(wc))
//				continue;
//			
//			//	remember old string for correction based priorization
//			String oldStr = wc.str;
//			
//			//	get similar word clusters
//			ImWordClusterSimilarity[] wcss = getSimilarClusters(wc, null, wc.root);
//			
//			//	display word cluster
//			ImWordClusterListDialog wcld = new ImWordClusterListDialog(wc, wcss, false, ((similarTo == null) ? null : ("(similar to '" + similarTo + "')"))) {
//				protected void wordClusterApproved(ImWordCluster wc) {
//					handledWordClusters.add(wc);
//				}
//				protected void wordClusterDissolved(ImWordCluster wc) {
//					handledWordClusters.add(wc);
//				}
//			};
//			wcld.setSize(Math.max(400, (2 * wc.representative.getWidth())), Math.min(600, (100 + (Math.max(45, wc.representative.getHeight()) * ((wcss == null) ? 0 : wcss.length)))));
//			wcld.setLocationRelativeTo(null);
//			wcld.setVisible(true);
//			if ((wcld.listMoveDirection != 0) && (hWordClusters != null))
//				hWordClusters.add(wc);
//			
//			//	handle words with corrected trigrams right away
//			if (!oldStr.equals(wc.str)) {
//				ImWordClusterSimilarity[] tgWcss = getSimilarClusters(wc, oldStr, wc.root);
//				if (tgWcss != null) {
//					ArrayList tgSimWordClusters = new ArrayList();
//					for (int s = 0; s < tgWcss.length; s++) {
//						if (!handledWordClusters.contains(tgWcss[s].wc2) && !wc.str.equals(tgWcss[s].wc2.str))
//							tgSimWordClusters.add(tgWcss[s].wc2);
//					}
//					if (tgSimWordClusters.size() != 0)
//						handleErrorWordClusters(tgSimWordClusters, handledWordClusters, oldStr);
//				}
//			}
//			
//			//	move up or down list, or break
//			if (wcld.listMoveDirection == 0)
//				break;
//			else if ((c != 0) && (wcld.listMoveDirection == -1))
//				c -= 2; // also compensate loop increment
//		}
//		if (hWordClusters != null)
//			handledWordClusters.addAll(hWordClusters);
//	}
	
	private static void scoreWordClusterMetrics(ArrayList wordClusterMetrics, Comparator comp, int factor, String metricName) {
		Collections.sort(wordClusterMetrics, comp);
		
//		for (int m = 0; m < (wordClusterMetrics.size() / 2); m++)
//			((ImWordClusterMetrics) wordClusterMetrics.get(m)).errorPosScore += ((wordClusterMetrics.size() / 2) - m);
		
		//	puts extreme emphasis on very top of list
		ImWordClusterMetrics lastWcm = ((ImWordClusterMetrics) wordClusterMetrics.get(wordClusterMetrics.size()-1));
		int score = (wordClusterMetrics.size() / 5);
		int lastDecrPos = 0;
//		int step = 1;
		for (int m = 0; m < wordClusterMetrics.size(); m++) {
//			if (m == step) {
//				score >>= 1;
//				step <<= 1;
//			}
			ImWordClusterMetrics wcm = ((ImWordClusterMetrics) wordClusterMetrics.get(m));
			if ((m != 0) && (comp.compare(wordClusterMetrics.get(m-1), wcm) != 0)) {
				score -= (m - lastDecrPos);
				lastDecrPos = m;
			}
			if (score < 0)
				break;
			if (comp.compare(lastWcm, wcm) == 0)
				break;
			wcm.errorPosScore += (score * factor);
			if (wcm.errorReasons == null)
				wcm.errorReasons = new HashSet();
			wcm.errorReasons.add(metricName);
		}
	}
	
	private static class ImWordClusterMetrics {
		int errorPosScore = 0;
		HashSet errorReasons = null;
		
		ImWordCluster wc;
		
		ExtendedWordImageMatch ewim;
		float avgDist;
		float avgSqrDist;
		float relMaxDist;
		
		float avgTrigramSim;
		float minTrigramSim;
		float avgTrigramSimRel;
		int avgTrigramFreq;
		int minTrigramFreq;
		
		LinkedHashSet replacedLetterCombs = null;
		int replacedLetterCombScore = 0;
		LinkedHashSet letterCombReplacements = null;
		int replacedLetterTrigramFreqDiff = 0;
		
		int sameStringWcCount = 0;
		int simImageWcCount = 0;
		
		ImWordClusterMetrics(ImWordCluster wc, ExtendedWordImageMatch ewim, float avgTrigramSim, float minTrigramSim, float avgTrigramSimRel, int avgTrigramFreq, int minTrigramFreq) {
			this.wc = wc;
			this.ewim = ewim;
			this.relMaxDist = (((float) Math.max(ewim.scannedOnlyMaxDist, ewim.renderedOnlyMaxDist)) / ewim.matchedHist.length);
			this.avgDist = ((ewim.scannedOnlyAvgDist + ewim.renderedOnlyAvgDist) / 2);
			this.avgSqrDist = ((ewim.scannedOnlyAvgSqrDist + ewim.renderedOnlyAvgSqrDist) / 2);
			this.avgTrigramSim = avgTrigramSim;
			this.minTrigramSim = minTrigramSim;
			this.avgTrigramSimRel = avgTrigramSimRel;
			this.avgTrigramFreq = avgTrigramFreq;
			this.minTrigramFreq = minTrigramFreq;
		}
	}
	
	private static class SimCountingSet {
		private static class Entry {
			double simSum = 0;
			int simCount = 0;
			void add(float sim) {
				this.add(sim, 1);
			}
			void add(float sim, int count) {
				this.simSum += (sim * count);
				this.simCount += count;
			}
			float getSim() {
				return ((this.simCount == 0) ? 0f : ((float) (this.simSum / this.simCount)));
			}
		}
		HashMap entries = new HashMap();
		private Entry getEntry(String str, boolean create) {
			Entry en = ((Entry) this.entries.get(str));
			if ((en == null) && create) {
				en = new Entry();
				this.entries.put(str, en);
			}
			return en;
		}
		void add(String str, float sim) {
			this.add(str, sim, 1);
		}
		synchronized void add(String str, float sim, int count) {
			this.getEntry(str, true).add(sim, count);
		}
		float getSim(String str) {
			Entry en = this.getEntry(str, false);
			return ((en == null) ? 0f : en.getSim());
		}
		int getCount(String str) {
			Entry en = this.getEntry(str, false);
			return ((en == null) ? 0 : en.simCount);
		}
		Iterator getIterator() {
			return this.entries.keySet().iterator();
		}
	}
	
	private int goodOcrCount = 0;
	private int badOcrCount = 0;
	
	private CountingSet goodOcrErrorReasons = new CountingSet(new TreeMap());
	private CountingSet badOcrErrorReasons = new CountingSet(new TreeMap());
	
	private CountingSet goodOcrClusterSizes = new CountingSet(new TreeMap());
	private CountingSet badOcrClusterSizes = new CountingSet(new TreeMap());
	
	private CountingSet goodOcrClusterSims = new CountingSet(new TreeMap());
	private CountingSet badOcrClusterSims = new CountingSet(new TreeMap());
	
	private CountingSet goodOcrFontSizeDiffs = new CountingSet(new TreeMap());
	private CountingSet badOcrFontSizeDiffs = new CountingSet(new TreeMap());
	
	private CountingSet goodOcrLetterReplacementScores = new CountingSet(new TreeMap());
	private CountingSet badOcrLetterReplacementScores = new CountingSet(new TreeMap());
	private CountingSet goodOcrLetterReplacementTrigramFreqDiffs = new CountingSet(new TreeMap());
	private CountingSet badOcrLetterReplacementTrigramFreqDiffs = new CountingSet(new TreeMap());
	
	private CountingSet goodOcrSims = new CountingSet(new TreeMap());
	private CountingSet badOcrSims = new CountingSet(new TreeMap());
	private CountingSet goodOcrWaSims = new CountingSet(new TreeMap());
	private CountingSet badOcrWaSims = new CountingSet(new TreeMap());
	private CountingSet goodOcrMinLetterSims = new CountingSet(new TreeMap());
	private CountingSet badOcrMinLetterSims = new CountingSet(new TreeMap());
//	private CountingSet goodOcrTopSims = new CountingSet(new TreeMap());
//	private CountingSet badOcrTopSims = new CountingSet(new TreeMap());
//	private CountingSet goodOcrTopMinLetterSims = new CountingSet(new TreeMap());
//	private CountingSet badOcrTopMinLetterSims = new CountingSet(new TreeMap());
//	private CountingSet goodOcrTopSimsBySims = new CountingSet(new TreeMap());
//	private CountingSet badOcrTopSimsBySims = new CountingSet(new TreeMap());
//	private CountingSet goodOcrMaxLetterErrorToError = new CountingSet(new TreeMap());
//	private CountingSet badOcrMaxLetterErrorToError = new CountingSet(new TreeMap());
	
	private CountingSet goodOcrMaxContrasts = new CountingSet(new TreeMap());
	private CountingSet badOcrMaxContrasts = new CountingSet(new TreeMap());
	private CountingSet goodOcrAvgContrasts = new CountingSet(new TreeMap());
	private CountingSet badOcrAvgContrasts = new CountingSet(new TreeMap());
	private CountingSet goodOcrAvgDists = new CountingSet(new TreeMap());
	private CountingSet badOcrAvgDists = new CountingSet(new TreeMap());
	private CountingSet goodOcrAvgSqrDists = new CountingSet(new TreeMap());
	private CountingSet badOcrAvgSqrDists = new CountingSet(new TreeMap());
	private CountingSet goodOcrMaxDists = new CountingSet(new TreeMap());
	private CountingSet badOcrMaxDists = new CountingSet(new TreeMap());
	private CountingSet goodOcrRelMaxDists = new CountingSet(new TreeMap());
	private CountingSet badOcrRelMaxDists = new CountingSet(new TreeMap());
	
	private CountingSet goodOcrMinTrigramCounts = new CountingSet(new TreeMap());
	private CountingSet badOcrMinTrigramCounts = new CountingSet(new TreeMap());
	private CountingSet goodOcrAvgTrigramCounts = new CountingSet(new TreeMap());
	private CountingSet badOcrAvgTrigramCounts = new CountingSet(new TreeMap());
	private CountingSet goodOcrMinTrigramSims = new CountingSet(new TreeMap());
	private CountingSet badOcrMinTrigramSims = new CountingSet(new TreeMap());
	private CountingSet goodOcrAvgTrigramSims = new CountingSet(new TreeMap());
	private CountingSet badOcrAvgTrigramSims = new CountingSet(new TreeMap());
	private CountingSet goodOcrAvgTrigramSimRels = new CountingSet(new TreeMap());
	private CountingSet badOcrAvgTrigramSimRels = new CountingSet(new TreeMap());
	
	private CountingSet goodOcrCapCounts = new CountingSet(new TreeMap());
	private CountingSet badOcrCapCounts = new CountingSet(new TreeMap());
	
	private CountingSet goodOcrErrorPosScores = new CountingSet(new TreeMap());
	private CountingSet badOcrErrorPosScores = new CountingSet(new TreeMap());
	
	/* TODO
For single letters, bigrams, and trigrams, average out how well super-rendering matches
==> words with low average matches on their letters, bigrams, and trigrams might have higher probability of containing errors
==> words matching worse than their letters, bigrams, and trigrams might have higher probability of containing errors

Many optically similar clusters (similarity > 0) indicate potential OCR errors

Many OCR similar clusters (similarity == 0) indicate good OCR
	 */
	
	/* TODO
- Errors more likely in small clusters (OCR errors are at least somewhat random, so less likely to happen the same way more than a few times)

- Errors more likely in clusters with no or few string-similar clusters (OCR errors are at least somewhat random, so less likely to happen the same way across font sizes and, even less likely, font styles)

- Difference of rendered font size and measured (paragraph) font size might indicate errors like 'parts' vs. 'pans'

- Average out word image match metrics like mis-match pixel distance measures across document, and use above-average values to identify OCR errors

- Rare trigrams (measure across document to adapt to language & terminology) might indicate errors

Dottless lower case 'I's and 'J's do indicate errors (they exist for use with combining accents)

OCR string not being in dictionary indicates errors

Errors more likely in lower case (caps usually OCR pretty well ... mind 'SHARAR' vs. 'SHARAF.' error, though)

Capital letters in middle of word indicate errors ...
... as do lower case letters in words majorly consisting of capitals

Presence of frequently corrected letter combinations indicates errors ==> Learn frequency of edits (as well as base string edits):
- during cluster mergers, across documents
- from user input
- seed that table from experience, basically with what optical edit compensation uses now ...
- ... and add dictionary lookups with reverse-edited strings
==> also try and use that in OCR part of cluster matching, namely in optically compensated edit distance
==> also try and use that in picking OCR result on cluster merging (might help prevent 'Myrrnicinae' from being selected over 'Myrmicinae' by close-as-hell super rendering comparison)

Create dictionary mapping each word to optically similar ones ...
... and use that for identifying alternative OCR strings ...
... even ones to try if not occurring in (small) cluster

Measure x-height vs. cap-height to assess propability of 't' to be mistaken for 'r' (the higher the x-height, the more likely the error)

Cut histogram alignment some more slack with maximum pixel row shifts (should help with small-caps, as well as words that lost their I dot only ascender ... like that one case of 'species')
	 */
	
	private ImWordClustering createWordClustering(ImDocument doc, ProgressMonitor pm) {
		
		//	TODO use progress monitor instead of System.out for major updates
		
		//	TODO make any other System.out usage dependent on debug flag
		
		final SynchronizedProgressMonitor spm = ((pm instanceof SynchronizedProgressMonitor) ? ((SynchronizedProgressMonitor) pm) : new SynchronizedProgressMonitor(pm));
		
		//	collect word images for later collective assessment
		final ArrayList wordImages = new ArrayList();
		
		//	get word images
		spm.setStep("Extracting word images");
		spm.setBaseProgress(0);
		spm.setMaxProgress(20);
		final ImPage[] pages = doc.getPages();
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int p) throws Exception {
				
				//	update progress
				spm.setInfo("Extracting word images from page " + p);
				spm.setProgress((p * 100) / pages.length);
				
				//	get word images for current page
				WordImage[] pageWordImages = WordImageAnalysis.getWordImages(pages[p]);
				spm.setInfo(" ==> got " + pageWordImages.length + " word images");
				
				//	add word images to list (order is not really relevant for our analyses)
				synchronized (wordImages) {
					wordImages.addAll(Arrays.asList(pageWordImages));
				}
			}
		}, pages.length, -1);
		
		/* cluster words by OCR result first (as OCR errors are more or less
		 * random, they are a lot more likely to split clusters (which we can
		 * merge later) than to conflate clusters or result in a word being
		 * assigned to the wrong cluster) */
		final ArrayList wordClusters = new ArrayList();
		final HashMap wordClusterIndex = new HashMap();
		for (int w = 0; w < wordImages.size(); w++) { // TODO parallelize this (but how ???)
			WordImage wi = ((WordImage) wordImages.get(w));
			System.out.println("Assigning word '" + wi.word.getString() + "' at " + wi.word.bounds + " to cluster");
			
			//	ignore baseline punctuation marks (for now, at least)
			if ((wi.box.getWidth() < (wi.pageImageDpi / 30)) || (wi.box.getHeight() < (wi.pageImageDpi / 20))) {
				System.out.println(" ==> punctuation mark, ignored");
				continue;
			}
			
			//	ignore baseline punctuation marks
			if (Gamta.isPunctuation(wi.str)) {
				System.out.println(" ==> punctuation mark, ignored");
				continue;
			}
			
			//	add word to cluster
			addWordToCluster(wi, wordClusterIndex, wordClusters);
		}
		
		//	assess average cluster internal similarity (non-weighted and weighted by cluster size)
		int clusterSimCount = 0;
		double clusterBoxPropSum = 0;
		double clusterSimSum = 0;
		double clusterWaSimSum = 0;
		int mlClusterSimCount = 0;
		double clusterMinLetterSimSum = 0;
		double clusterMaxContrastSum = 0;
		double clusterMaxDeviationSum = 0;
		double clusterAvgContrastSum = 0;
		double clusterAvgDeviationSum = 0;
		double clusterAvgSqrContrastSum = 0;
		double clusterAvgSqrDeviationSum = 0;
		double clusterAvgDistanceSum = 0;
		double clusterAvgSqrDistanceSum = 0;
		double clusterMaxDistanceSum = 0;
		double clusterRelMaxDistanceSum = 0;
		
		int wordSimCount = 0;
		double wordBoxPropSum = 0;
		double wordSimSum = 0;
		double wordWaSimSum = 0;
		int mlWordSimCount = 0;
		double wordMinLetterSimSum = 0;
		double wordMaxContrastSum = 0;
		double wordMaxDeviationSum = 0;
		double wordAvgContrastSum = 0;
		double wordAvgDeviationSum = 0;
		double wordAvgSqrContrastSum = 0;
		double wordAvgSqrDeviationSum = 0;
		double wordAvgDistanceSum = 0;
		double wordAvgSqrDistanceSum = 0;
		double wordMaxDistanceSum = 0;
		double wordRelMaxDistanceSum = 0;
		
		for (int c = 0; c < wordClusters.size(); c++) { // TODO parallelize this
			WordCluster wc = ((WordCluster) wordClusters.get(c));
			
			//	little to do here ...
			if (wc.wordImages.size() < 2)
				continue;
			System.out.println("Assessing internal similarity of cluster '" + wc.name + "'");
			
			//	compare words in cluster to representative
			wc.compileRepresentative();
			int clusterWordSimCount = 0;
			double clusterWordBoxPropSum = 0;
			double clusterWordSimSum = 0;
			double clusterWordWaSimSum = 0;
			int mlClusterWordSimCount = 0;
			double clusterWordMinLetterSimSum = 0;
			double clusterWordMaxContrastSum = 0;
			double clusterWordMaxDeviationSum = 0;
			double clusterWordAvgContrastSum = 0;
			double clusterWordAvgDeviationSum = 0;
			double clusterWordAvgSqrContrastSum = 0;
			double clusterWordAvgSqrDeviationSum = 0;
			double clusterWordAvgDistanceSum = 0;
			double clusterWordAvgSqrDistanceSum = 0;
			double clusterWordMaxDistanceSum = 0;
			double clusterWordRelMaxDistanceSum = 0;
			System.out.println(" - comparing " + wc.wordImages.size() + " word images to representative");
			System.out.print("   ");
			for (int w = 0; w < wc.wordImages.size(); w++) {
				WordImage wi = ((WordImage) wc.wordImages.get(w));
				ExtendedWordImageMatch wim = matchWordImages(wi, wc.representative, true, true, 1);
				
				wc.minWordImageSim = Math.min(wc.minWordImageSim, wim.base.sim);
				
				clusterWordSimCount++;
				clusterWordBoxPropSum += Math.abs(wc.avgBoxProportion - wi.boxProportion);
				clusterWordSimSum += wim.base.sim;
				clusterWordWaSimSum += wim.base.waSim;
				
				wordSimCount++;
				wordBoxPropSum += Math.abs(wc.avgBoxProportion - wi.boxProportion);
				wordSimSum += wim.base.sim;
				wordWaSimSum += wim.base.waSim;
				
				//	compute match histograms
				wim.computeMatchHistograms();
				
				//	compute metrics for non-matching areas
				wim.computeMisMatchDistances();
				
				clusterWordAvgDistanceSum += wim.scannedOnlyAvgDist;
				clusterWordAvgSqrDistanceSum += wim.scannedOnlyAvgSqrDist;
				clusterWordMaxDistanceSum += wim.scannedOnlyMaxDist;
				clusterWordRelMaxDistanceSum += (((float) wim.scannedOnlyMaxDist) / wim.matchedHist.length);
				
				wordAvgDistanceSum += wim.scannedOnlyAvgDist;
				wordAvgSqrDistanceSum += wim.scannedOnlyAvgSqrDist;
				wordMaxDistanceSum += wim.scannedOnlyMaxDist;
				wordRelMaxDistanceSum += (((float) wim.scannedOnlyMaxDist) / wim.matchedHist.length);
				
				//	little use doing histogram metrics on single letters
				if (Math.max(wim.base.scanned.str.length(), wim.base.rendered.str.length()) < 2)
					continue;
				
				//	compute match histogram metrics
				wim.computeMatchHistogramMetrics();
				
				mlClusterWordSimCount++;
				clusterWordMinLetterSimSum += wim.minLetterSim;
				clusterWordMaxContrastSum += wim.maxSimBucketContrast;
				clusterWordMaxDeviationSum += wim.maxSimBucketDeviation;
				clusterWordAvgContrastSum += wim.simBucketContrast;
				clusterWordAvgDeviationSum += wim.simBucketDeviation;
				clusterWordAvgSqrContrastSum += wim.simBucketContrastSqr;
				clusterWordAvgSqrDeviationSum += wim.simBucketDeviationSqr;
				
				mlWordSimCount++;
				wordMinLetterSimSum += wim.minLetterSim;
				wordMaxContrastSum += wim.maxSimBucketContrast;
				wordMaxDeviationSum += wim.maxSimBucketDeviation;
				wordAvgContrastSum += wim.simBucketContrast;
				wordAvgDeviationSum += wim.simBucketDeviation;
				wordAvgSqrContrastSum += wim.simBucketContrastSqr;
				wordAvgSqrDeviationSum += wim.simBucketDeviationSqr;
				
				System.out.print(".");
			}
			System.out.println();
			
			wc.avgWordImageSim = ((float) ((clusterWordSimCount == 0) ? 0 : (clusterWordSimSum / clusterWordSimCount)));
			
			double clusterWordBoxProp = ((clusterWordSimCount == 0) ? 0 : (clusterWordBoxPropSum / clusterWordSimCount));
			double clusterWordSim = ((clusterWordSimCount == 0) ? 0 : (clusterWordSimSum / clusterWordSimCount));
			double clusterWordWaSim = ((clusterWordSimCount == 0) ? 0 : (clusterWordWaSimSum / clusterWordSimCount));
			double clusterWordMinLetterSim = ((mlClusterWordSimCount == 0) ? 0 : (clusterWordMinLetterSimSum / mlClusterWordSimCount));
			double clusterWordMaxContrast = ((mlClusterWordSimCount == 0) ? 0 : (clusterWordMaxContrastSum / mlClusterWordSimCount));
			double clusterWordMaxDeviation = ((mlClusterWordSimCount == 0) ? 0 : (clusterWordMaxDeviationSum / mlClusterWordSimCount));
			double clusterWordAvgContrast = ((mlClusterWordSimCount == 0) ? 0 : (clusterWordAvgContrastSum / mlClusterWordSimCount));
			double clusterWordAvgDeviation = ((mlClusterWordSimCount == 0) ? 0 : (clusterWordAvgDeviationSum / mlClusterWordSimCount));
			double clusterWordAvgSqrContrast = ((mlClusterWordSimCount == 0) ? 0 : (clusterWordAvgSqrContrastSum / mlClusterWordSimCount));
			double clusterWordAvgSqrDeviation = ((mlClusterWordSimCount == 0) ? 0 : (clusterWordAvgSqrDeviationSum / mlClusterWordSimCount));
			double clusterWordAvgDistance = ((clusterWordSimCount == 0) ? 0 : (clusterWordAvgDistanceSum / clusterWordSimCount));
			double clusterWordAvgSqrDistance = ((clusterWordSimCount == 0) ? 0 : (clusterWordAvgSqrDistanceSum / clusterWordSimCount));
			double clusterWordMaxDistance = ((clusterWordSimCount == 0) ? 0 : (clusterWordMaxDistanceSum / clusterWordSimCount));
			double clusterWordRelMaxDistance = ((clusterWordSimCount == 0) ? 0 : (clusterWordRelMaxDistanceSum / clusterWordSimCount));
			System.out.println(" - box proportion is " + clusterWordBoxProp);
			System.out.println(" - similarity is " + clusterWordSim);
			System.out.println(" - weight adjusted similarity is " + clusterWordWaSim);
			if (wc.name.length() > 1) {
				System.out.println(" - min letter similarity is " + clusterWordMinLetterSim);
				System.out.println(" - max split similarity contrast is " + clusterWordMaxContrast + ", max deviation is " + clusterWordMaxDeviation);
				System.out.println(" - avg split similarity contrast is " + clusterWordAvgContrast + ", avg deviation is " + clusterWordAvgDeviation);
				System.out.println(" - sqr-avg split similarity contrast is " + clusterWordAvgSqrContrast + ", sqr-avg deviation is " + clusterWordAvgSqrDeviation);
			}
			System.out.println(" - avg distance " + clusterWordAvgDistance + ", avg sqr distance " + clusterWordAvgSqrDistance + ", max distance " + clusterWordMaxDistance + ", relative max distance " + clusterWordRelMaxDistance);
			
			clusterSimCount++;
			clusterBoxPropSum += clusterWordBoxProp;
			clusterSimSum += clusterWordSim;
			clusterWaSimSum += clusterWordWaSim;
			
			if (wc.name.length() > 1) {
				mlClusterSimCount++;
				clusterMinLetterSimSum += clusterWordMinLetterSim;
				clusterMaxContrastSum += clusterWordMaxContrast;
				clusterMaxDeviationSum += clusterWordMaxDeviation;
				clusterAvgContrastSum += clusterWordAvgContrast;
				clusterAvgDeviationSum += clusterWordAvgDeviation;
				clusterAvgSqrContrastSum += clusterWordAvgSqrContrast;
				clusterAvgSqrDeviationSum += clusterWordAvgSqrDeviation;
			}
			
			clusterAvgDistanceSum += clusterWordAvgDistance;
			clusterAvgSqrDistanceSum += clusterWordAvgSqrDistance;
			clusterMaxDistanceSum += clusterWordMaxDistance;
			clusterRelMaxDistanceSum += clusterWordRelMaxDistance;
		}
		
		final WordImageMatchStats clusterMatchStats = new WordImageMatchStats();
		clusterMatchStats.avgBoxProp = ((clusterSimCount == 0) ? 0 : (clusterBoxPropSum / clusterSimCount));
		clusterMatchStats.avgSim = ((clusterSimCount == 0) ? 0 : (clusterSimSum / clusterSimCount));
		clusterMatchStats.avgWaSim = ((clusterSimCount == 0) ? 0 : (clusterWaSimSum / clusterSimCount));
		clusterMatchStats.avgMinLetterSim = ((mlClusterSimCount == 0) ? 0 : (clusterMinLetterSimSum / mlClusterSimCount));
		clusterMatchStats.avgMaxContrast = ((mlClusterSimCount == 0) ? 0 : (clusterMaxContrastSum / mlClusterSimCount));
		clusterMatchStats.avgMaxDeviation = ((mlClusterSimCount == 0) ? 0 : (clusterMaxDeviationSum / mlClusterSimCount));
		clusterMatchStats.avgAvgContrast = ((mlClusterSimCount == 0) ? 0 : (clusterAvgContrastSum / mlClusterSimCount));
		clusterMatchStats.avgAvgDeviation = ((mlClusterSimCount == 0) ? 0 : (clusterAvgDeviationSum / mlClusterSimCount));
		clusterMatchStats.avgAvgSqrContrast = ((mlClusterSimCount == 0) ? 0 : (clusterAvgSqrContrastSum / mlClusterSimCount));
		clusterMatchStats.avgAvgSqrDeviation = ((mlClusterSimCount == 0) ? 0 : (clusterAvgSqrDeviationSum / mlClusterSimCount));
		clusterMatchStats.avgAvgDistance = ((clusterSimCount == 0) ? 0 : (clusterAvgDistanceSum / clusterSimCount));
		clusterMatchStats.avgAvgSqrDistance = ((clusterSimCount == 0) ? 0 : (clusterAvgSqrDistanceSum / clusterSimCount));
		clusterMatchStats.avgMaxDistance = ((clusterSimCount == 0) ? 0 : (clusterMaxDistanceSum / clusterSimCount));
		clusterMatchStats.avgRelMaxDistance = ((clusterSimCount == 0) ? 0 : (clusterRelMaxDistanceSum / clusterSimCount));
		System.out.println("Average cluster-internal similarity metrics based on " + clusterSimCount + " clusters, " + mlClusterSimCount + " multi-letter clusters:");
		System.out.println(" - box proportion is " + clusterMatchStats.avgBoxProp);
		System.out.println(" - similarity is " + clusterMatchStats.avgSim);
		System.out.println(" - weight adjusted similarity is " + clusterMatchStats.avgWaSim);
		System.out.println(" - min letter similarity is " + clusterMatchStats.avgMinLetterSim);
		System.out.println(" - max split similarity contrast is " + clusterMatchStats.avgMaxContrast + ", max deviation is " + clusterMatchStats.avgMaxDeviation);
		System.out.println(" - avg split similarity contrast is " + clusterMatchStats.avgAvgContrast + ", avg deviation is " + clusterMatchStats.avgAvgDeviation);
		System.out.println(" - sqr-avg split similarity contrast is " + clusterMatchStats.avgAvgSqrContrast + ", sqr-avg deviation is " + clusterMatchStats.avgAvgSqrDeviation);
		System.out.println(" - avg distance " + clusterMatchStats.avgAvgDistance + ", avg sqr distance " + clusterMatchStats.avgAvgSqrDistance + ", max distance " + clusterMatchStats.avgMaxDistance + ", relative max distance " + clusterMatchStats.avgRelMaxDistance);
		
		final WordImageMatchStats wordMatchStats = new WordImageMatchStats();
		wordMatchStats.avgBoxProp = ((wordSimCount == 0) ? 0 : (wordBoxPropSum / wordSimCount));
		wordMatchStats.avgSim = ((wordSimCount == 0) ? 0 : (wordSimSum / wordSimCount));
		wordMatchStats.avgWaSim = ((wordSimCount == 0) ? 0 : (wordWaSimSum / wordSimCount));
		wordMatchStats.avgMinLetterSim = ((mlWordSimCount == 0) ? 0 : (wordMinLetterSimSum / mlWordSimCount));
		wordMatchStats.avgMaxContrast = ((mlWordSimCount == 0) ? 0 : (wordMaxContrastSum / mlWordSimCount));
		wordMatchStats.avgMaxDeviation = ((mlWordSimCount == 0) ? 0 : (wordMaxDeviationSum / mlWordSimCount));
		wordMatchStats.avgAvgContrast = ((mlWordSimCount == 0) ? 0 : (wordAvgContrastSum / mlWordSimCount));
		wordMatchStats.avgAvgDeviation = ((mlWordSimCount == 0) ? 0 : (wordAvgDeviationSum / mlWordSimCount));
		wordMatchStats.avgAvgSqrContrast = ((mlWordSimCount == 0) ? 0 : (wordAvgSqrContrastSum / mlWordSimCount));
		wordMatchStats.avgAvgSqrDeviation = ((mlWordSimCount == 0) ? 0 : (wordAvgSqrDeviationSum / mlWordSimCount));
		wordMatchStats.avgAvgDistance = ((wordSimCount == 0) ? 0 : (wordAvgDistanceSum / wordSimCount));
		wordMatchStats.avgAvgSqrDistance = ((wordSimCount == 0) ? 0 : (wordAvgSqrDistanceSum / wordSimCount));
		wordMatchStats.avgMaxDistance = ((wordSimCount == 0) ? 0 : (wordMaxDistanceSum / wordSimCount));
		wordMatchStats.avgRelMaxDistance = ((wordSimCount == 0) ? 0 : (wordRelMaxDistanceSum / wordSimCount));
		System.out.println("Weighted average cluster-internal word similarity metrics based on " + wordSimCount + " words, " + mlWordSimCount + " multi-letter words:");
		System.out.println(" - box proportion is " + wordMatchStats.avgBoxProp);
		System.out.println(" - similarity is " + wordMatchStats.avgSim);
		System.out.println(" - weight adjusted similarity is " + wordMatchStats.avgWaSim);
		System.out.println(" - min letter similarity is " + wordMatchStats.avgMinLetterSim);
		System.out.println(" - max split similarity contrast is " + wordMatchStats.avgMaxContrast + ", max deviation is " + wordMatchStats.avgMaxDeviation);
		System.out.println(" - avg split similarity contrast is " + wordMatchStats.avgAvgContrast + ", avg deviation is " + wordMatchStats.avgAvgDeviation);
		System.out.println(" - sqr-avg split similarity contrast is " + wordMatchStats.avgAvgSqrContrast + ", sqr-avg deviation is " + wordMatchStats.avgAvgSqrDeviation);
		System.out.println(" - avg distance " + wordMatchStats.avgAvgDistance + ", avg sqr distance " + wordMatchStats.avgAvgSqrDistance + ", max distance " + wordMatchStats.avgMaxDistance + ", relative max distance " + wordMatchStats.avgRelMaxDistance);
		
		//	sort clusters by size
		Collections.sort(wordClusters, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				WordCluster wc1 = ((WordCluster) obj1);
				WordCluster wc2 = ((WordCluster) obj2);
				return (wc2.wordImages.size() - wc1.wordImages.size());
			}
		});
		
		//	record letter replacements
		final CountingSet letterReplacements = new CountingSet(new HashMap());
		
		//	re-assess clusters whose members are far less similar to centroid than average
		for (int c = 0; c < wordClusters.size(); c++) { // TODO parallelize this
			WordCluster wc = ((WordCluster) wordClusters.get(c));
			
			//	little to do here ...
			if (wc.wordImages.size() < 2) {
//				System.out.println(" - single-word cluster, ignored");
				continue;
			}
			System.out.println("Assessing consistency of cluster '" + wc.name + "' with " + wc.wordImages.size() + " words");
			
			//	ignore numbers and punctuation marks, at least for now
			if (Gamta.isNumber(wc.name) || Gamta.isPunctuation(wc.name)) {
				System.out.println(" - number or punctuation mark, ignored");
				continue;
			}
			
			//	this one looks OK (maximum error less than 50% above average error)
			if (((1 - wc.minWordImageSim) * 1) < ((1 - wordMatchStats.avgSim) * 2)) {
				System.out.println(" - looks consistent at min similarity " + wc.minWordImageSim);
				continue;
			}
			System.out.println(" - min similarity is " + wc.minWordImageSim + ", re-assessing words");
			
			//	remove worst matching word image until cluster consistent
			final int clusterSize = wc.wordImages.size();
			String rwcKey = null;
			WordCluster rwc = null;
			while ((wc.wordImages.size() > 1) && (((1 - wc.minWordImageSim) * 1) >= ((1 - wordMatchStats.avgSim) * 2))) {
				
				//	order cluster members by similarity to representative
				wc.orderWordImages();
				
				//	worst match is at end of list now !!!
				WordImage rcwi = ((WordImage) wc.wordImages.get(wc.wordImages.size()-1));
				ExtendedWordImageMatch rcwim = ((ExtendedWordImageMatch) wc.wordImageMatches.get(wc.wordImageMatches.size()-1));
				rcwim.computeMatchHistograms();
				System.out.println("Bad cluster word match:");
				System.out.println(" - shifts are: left " + rcwim.base.leftShift + ", right " + rcwim.base.rightShift + ", top " + rcwim.base.topShift + ", bottom " + rcwim.base.bottomShift);
				System.out.println(" - area proportion distance is " + Math.abs(rcwim.base.scanned.boxProportion - rcwim.base.rendered.boxProportion));
				System.out.println(" - similarity is " + rcwim.base.sim + " (" + rcwim.base.precision + "/" + rcwim.base.recall + ") against average " + wordMatchStats.avgSim);
				System.out.println(" - weight adjusted similarity is " + rcwim.base.waSim + " (" + rcwim.base.waPrecision + "/" + rcwim.base.waRecall + ") against average " + wordMatchStats.avgWaSim);
				
				//	compare to best matches for removal verification
				int rvc = ((int) Math.floor(Math.sqrt(wc.wordImages.size())));
				float rvSimSum = 0;
				float rvWaSimSum = 0;
				float rvMinLetterSimSum = 0;
				for (int w = 0; w < rvc; w++) {
					ExtendedWordImageMatch rvwim = matchWordImages(rcwi, ((WordImage) wc.wordImages.get(w)), true, true, 1);
					rvSimSum += rvwim.base.sim;
					rvWaSimSum += rvwim.base.waSim;
					rvwim.computeMatchHistogramMetrics();
					rvMinLetterSimSum += rvwim.minLetterSim;
				}
				float rvSim = (rvSimSum / rvc);
				float rvWaSim = (rvWaSimSum / rvc);
				float rvMinLetterSim = (rvMinLetterSimSum / rvc);
				System.out.println(" - similarity to top " + rvc + " cluster words is " + rvSim + ", weight adjusted " + rvWaSim + ", min letters similarity is " + rvMinLetterSim);
				
				//	this image in fact does look OK
				if ((((1 - rvMinLetterSim) * 2) < ((1 - wordMatchStats.avgMinLetterSim) * 5)) && ((((1 - rvSim) * 1) < ((1 - wordMatchStats.avgSim) * 2)) || (((1 - rvWaSim) * 1) < ((1 - wordMatchStats.avgWaSim) * 2)))) {
					System.out.println(" ==> retained in cluster");
//					if (this.debug) displayWordMatch(rcwim, "Retaining in cluster '" + wc.name + "'");
//					if (clusterSize == wc.wordImages.size())
//						wc.compileRepresentative(true);
					break;
				}
				
				//	remove worst match and update representative
//				if (this.debug) displayWordMatch(rcwim, "Removing from cluster '" + wc.name + "'");
				WordImage rwi = wc.removeWordImage(wc.wordImages.size()-1);
				
				//	recompute internal similarity for remaining cluster
				wc.computeInternalSimilarity();
				
				/* if the first letter is one with lower case easily confused
				 * with upper case even when part of a word (COSVWXZ, but not
				 * P, which has a descender in lower case), try to match to
				 * lower case cluster right away */
				
				//	assess upper case first letter
				if (stripAccents(rwi.word.getString()).matches("[COPSVWXZ][a-z\\-\\']+")) {
					WordImage ucrwi = WordImageAnalysis.renderWordImage(rwi.word, -1, (rwi.word.hasAttribute(ImWord.ITALICS_ATTRIBUTE) ? Font.ITALIC : Font.PLAIN), 9, rwi.pageImageDpi, null);
					ExtendedWordImageMatch ucwim = matchWordImages(rwi, ucrwi, true, true, 1);
					WordImage lcrwi = WordImageAnalysis.renderWordImage(rwi.word.getString().toLowerCase(), rwi.word.bounds, -1, (rwi.word.hasAttribute(ImWord.ITALICS_ATTRIBUTE) ? Font.ITALIC : Font.PLAIN), 9, rwi.pageImageDpi, null);
					ExtendedWordImageMatch lcwim = matchWordImages(rwi, lcrwi, true, true, 1);
					System.out.println("Upper case verification:");
					System.out.println(" - plain rendering similarities are " + ucwim.base.sim + " and " + lcwim.base.sim);
					System.out.println(" - weight adjusted rendering similarities are " + ucwim.base.waSim + " and " + lcwim.base.waSim);
					if (lcwim.base.sim > ucwim.base.sim) {
						letterReplacements.add(rwi.word.getString().substring(0, 1) + " " + rwi.word.getString().substring(0, 1).toLowerCase());
						rwi.word.setString(rwi.word.getString().toLowerCase());
						addWordToCluster(rwi, wordClusterIndex, wordClusters);
						System.out.println(" ==> moved to cluster '" + rwi.word.getString() + "' with key " + getClusterKey(rwi.word));
						continue;
					}
				}
				
				//	assess lower case first letter
				else if (stripAccents(rwi.word.getString()).matches("[copsvwxz][a-z\\-\\']+")) {
					WordImage ucrwi = WordImageAnalysis.renderWordImage((rwi.word.getString().substring(0, 1).toUpperCase() + rwi.word.getString().substring(1)), rwi.word.bounds, -1, (rwi.word.hasAttribute(ImWord.ITALICS_ATTRIBUTE) ? Font.ITALIC : Font.PLAIN), 9, rwi.pageImageDpi, null);
					ExtendedWordImageMatch ucwim = matchWordImages(rwi, ucrwi, true, true, 1);
					WordImage lcrwi = WordImageAnalysis.renderWordImage(rwi.word, -1, (rwi.word.hasAttribute(ImWord.ITALICS_ATTRIBUTE) ? Font.ITALIC : Font.PLAIN), 9, rwi.pageImageDpi, null);
					ExtendedWordImageMatch lcwim = matchWordImages(rwi, lcrwi, true, true, 1);
					System.out.println("Lower case verification:");
					System.out.println(" - plain rendering similarities are " + ucwim.base.sim + " and " + lcwim.base.sim);
					System.out.println(" - weight adjusted rendering similarities are " + ucwim.base.waSim + " and " + lcwim.base.waSim);
					if (ucwim.base.sim > lcwim.base.sim) {
						letterReplacements.add(rwi.word.getString().substring(0, 1) + " " + rwi.word.getString().substring(0, 1).toUpperCase());
						rwi.word.setString(rwi.word.getString().substring(0, 1).toUpperCase() + rwi.word.getString().substring(1));
						addWordToCluster(rwi, wordClusterIndex, wordClusters);
						System.out.println(" ==> moved to cluster '" + rwi.word.getString() + "' with key " + getClusterKey(rwi.word));
						continue;
					}
				}
				
				//	test if removed word fits into cluster with others
				if (rwc != null) {
					rwc.compileRepresentative();
					ExtendedWordImageMatch rwim = matchWordImages(rwi, rwc.representative, true, true, 1);
					if ((((1 - rwim.base.sim) * 1) < ((1 - wordMatchStats.avgSim) * 2)) || (((1 - rwim.base.waSim) * 1) < ((1 - wordMatchStats.avgWaSim) * 2))) {
						rwc.addWordImage(rwi);
						System.out.println(" ==> moved to cluster '" + rwi.word.getString() + "' with key " + rwcKey + " at similarity " + rwim.base.sim + ", weight adjusted " + rwim.base.waSim);
						continue;
					}
				}
				
				//	start new cluster otherwise
				rwc = new WordCluster(rwi, rwi.word);
				wordClusters.add(rwc);
				
				//	properly index new cluster
				rwcKey = getClusterKey(rwi.word);
				ArrayList rwcList = ((ArrayList) wordClusterIndex.get(rwcKey));
				if (rwcList == null) {
					rwcList = new ArrayList(1);
					wordClusterIndex.put(rwcKey, rwcList);
				}
				rwcList.add(rwc);
				System.out.println(" ==> moved to new cluster '" + rwi.word.getString() + "' with key " + rwcKey);
			}
			
			//	modified, re-assess internal similarity
			if (wc.wordImages.size() < clusterSize) {
				wc.computeInternalSimilarity();
				System.out.println(" - removed " + (clusterSize - wc.wordImages.size()) + " words");
//				if (this.debug) JOptionPane.showMessageDialog(null, ("Clusters '" + wc.name + "' cleaned up"), "Cluster Cleanup Result", JOptionPane.PLAIN_MESSAGE, new ImageIcon(wc.representative.img));
			}
			else System.out.println(" - cluster unmodified");
		}
		
		//	group word clusters into sets
		final ArrayList wordClusterSets = new ArrayList();
		HashMap wordClusterSetIndex = new HashMap();
		for (int c = 0; c < wordClusters.size(); c++) {
			WordCluster wc = ((WordCluster) wordClusters.get(c));
			String wcsKey = stripAccents(wc.name);
			WordClusterSet wcs = ((WordClusterSet) wordClusterSetIndex.get(wcsKey));
			if (wcs == null) {
				wcs = new WordClusterSet(wc);
				wordClusterSets.add(wcs);
				wordClusterSetIndex.put(wcsKey, wcs);
			}
			else wcs.addWordCluster(wc);
		}
		
		//	sort word cluster sets by size (gets us to the beef a lot faster)
		Collections.sort(wordClusterSets, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				WordClusterSet wcs1 = ((WordClusterSet) obj1);
				WordClusterSet wcs2 = ((WordClusterSet) obj2);
				return (wcs2.wordCount - wcs1.wordCount);
			}
		});
		
		//	try merging clusters within sets first
		final Map renderedWordImageCache = Collections.synchronizedMap(new HashMap());
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int s) throws Exception {
				WordClusterSet wcs = ((WordClusterSet) wordClusterSets.get(s));
				
				//	nothing to merge in this one
				if (wcs.wordClusters.size() == 1)
					return;
				
				//	ignore numbers, at least for now, as they are a lot less likely to repeat than words
				if (Gamta.isNumber(wcs.name))
					return;
				
				//	ignore (month-range) Roman numbers, at least for now, as they rarely mis-OCR
				if (wcs.name.replaceAll("[^a-zA-Z]", "").matches("[IVXivx]+"))
					return;
				
				//	compare word clusters
				for (int c = 0; c < wcs.wordClusters.size(); c++) {
					WordCluster wc = ((WordCluster) wcs.wordClusters.get(c));
					wc.compileRepresentative();
					
					//	compare to smaller clusters
					for (int cc = (c+1); cc < wcs.wordClusters.size(); cc++) {
						WordCluster cwc = ((WordCluster) wcs.wordClusters.get(cc));
						cwc.compileRepresentative();
						
						//	don't try to match clusters with all too different font sizes
						if (Math.abs(wc.fontSize - cwc.fontSize) > 1)
							continue;
						
						//	try and match clusters if (a) font size difference at most 1, (b) either of bold or italics properties match, and (c) box proportion distance is very low
						int matchScore = 0;
						if (wc.fontSize == cwc.fontSize)
							matchScore++;
						if (wc.isBold == cwc.isBold)
							matchScore++;
						if (wc.isItalics == cwc.isItalics)
							matchScore++;
						if (Math.abs(wc.avgBoxProportion - cwc.avgBoxProportion) <= 0.05)
							matchScore++;
						
						//	too little overlapping properties
						if (matchScore < 3)
							continue;
						
						//	compare representatives
						System.out.println("Matching cluster '" + wc.name + "' (" + wc.wordImages.size() + " words) to cluster '" + cwc.name + "' (" + cwc.wordImages.size() + " words)");
						ExtendedWordImageMatch cwim = matchWordImages(wc.representative, cwc.representative, true, false, 1);
						System.out.println(" - similarity is " + cwim.base.sim);
						
						//	do we have a match?
						int action = assessMatch(cwim, wc, cwc, true, clusterMatchStats, wordMatchStats, renderedWordImageCache, true);
						
						//	swap word clusters
						if (action == CLUSTER_MATCH_SWAP_MERGE) {
							wcs.wordClusters.set(c, wcs.wordClusters.set(cc, wcs.wordClusters.get(c)));
							wc = ((WordCluster) wcs.wordClusters.get(c));
							cwc = ((WordCluster) wcs.wordClusters.get(cc));
							action = CLUSTER_MATCH_MERGE;
							System.out.println(" - clusters swapped");
						}
						
						//	perform merger
						if (action == CLUSTER_MATCH_MERGE) {
							for (int i = 0; i < cwc.wordImages.size(); i++)
								wc.addWordImage((WordImage) cwc.wordImages.get(i));
							cwc.wordImages.clear();
							wc.compileRepresentative();
							wcs.wordClusters.remove(cc--);
							System.out.println(" ==> merged");
//							if (debug) JOptionPane.showMessageDialog(null, ("Cluster '" + cwc.name + "' merged into cluster '" + wc.name + "'"), "Cluster Merge Result", JOptionPane.PLAIN_MESSAGE, new ImageIcon(wc.representative.img));
						}
					}
//					if (debug && wc.name.matches("[A-Z\\-]{3,}")) JOptionPane.showMessageDialog(null, "Continue with mergers", "Cluster Mergers", JOptionPane.PLAIN_MESSAGE, new ImageIcon(wc.representative.img));
				}
			}
		}, wordClusterSets.size(), (this.debug ? 1 : -1));
//		if (this.debug) JOptionPane.showMessageDialog(null, "Continue with mergers", "Cluster Mergers", JOptionPane.PLAIN_MESSAGE);
		
		//	sort out empty clusters
		for (int c = 0; c < wordClusters.size(); c++) {
			WordCluster wc = ((WordCluster) wordClusters.get(c));
			if (wc.wordImages.isEmpty())
				wordClusters.remove(c--);
		}
		
		//	index sets by clusters
		final Map wordClustersToSets = Collections.synchronizedMap(new HashMap());
		for (int s = 0; s < wordClusterSets.size(); s++) {
			WordClusterSet wcs = ((WordClusterSet) wordClusterSets.get(s));
			for (int c = 0; c < wcs.wordClusters.size(); c++)
				wordClustersToSets.put(wcs.wordClusters.get(c), wcs);
		}
		
		//	sort clusters by width
		Collections.sort(wordClusters, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				WordCluster wc1 = ((WordCluster) obj1);
				WordCluster wc2 = ((WordCluster) obj2);
				return (wc2.avgBoxWidth - wc1.avgBoxWidth);
			}
		});
		
		//	assess which word clusters to merge or group
		final List wordClusterMatches = Collections.synchronizedList(new ArrayList());
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int c) throws Exception {
				WordCluster wc = ((WordCluster) wordClusters.get(c));
				wc.compileRepresentative();
				
				//	ignore numbers, at least for now, as they are a lot less likely to repeat than words
				if (Gamta.isNumber(wc.name))
					return;
				
				//	ignore (month-range) Roman numbers, at least for now, as they rarely mis-OCR
				if (wc.name.replaceAll("[^a-zA-Z]", "").matches("[IVXivx]+"))
					return;
				
				//	get cluster set
				WordClusterSet wcs = ((WordClusterSet) wordClustersToSets.get(wc));
				
				//	compare to smaller clusters
				for (int cc = (c+1); cc < wordClusters.size(); cc++) {
					WordCluster cwc = ((WordCluster) wordClusters.get(cc));
					cwc.compileRepresentative();
					
					//	get cluster set
					WordClusterSet cwcs = ((WordClusterSet) wordClustersToSets.get(cwc));
					
					//	we've already compared these two
					if (wcs == cwcs)
						continue;
					
					//	ignore numbers, at least for now, as they are a lot less likely to repeat than words
					if (Gamta.isNumber(cwc.name))
						continue;
					
					//	ignore (month-range) Roman numbers, at least for now, as they rarely mis-OCR
					if (cwc.name.replaceAll("[^a-zA-Z]", "").matches("[IVXivx]+"))
						continue;
					System.out.println("Matching cluster '" + wc.name + "' (" + wc.wordImages.size() + " words) to cluster '" + cwc.name + "' (" + cwc.wordImages.size() + " words)");
					
					//	check basics
					/* TODO_ne quite likely, we can use a way lower threshold than sqrt(2) either way
					 * - 21330.pdf.boldTest.imf --> 0.04
					 * - RSZ117_121.pdf.boldTest.imf --> 0.06
					 * - melander_1907.boldTest.pdf.imf --> 0.07
					 * - hesse_1974.pdf.boldTest.imf --> 0.08
					 * - ZM1967042005.pdf.raw.imf --> 0.10
					 * - paramonov_1950.pdf.imf --> 0.06
					 * - Kullander_ramirezi_1980.pdf.imf --> 0.09
					 * - 1104.pdf.test.imf --> 0.12
					 * - */
					if (Math.abs(wc.avgBoxProportion - cwc.avgBoxProportion) > 0.15f) {
						System.out.println(" ==> out of proportion, distance is " + Math.abs(wc.avgBoxProportion - cwc.avgBoxProportion));
						continue;
					}
					if ((Math.abs(wc.avgBoxHeigth - cwc.avgBoxHeigth) * 5) > Math.max(wc.avgBoxHeigth, cwc.avgBoxHeigth)) {
						System.out.println(" ==> height out of range, distance is " + Math.abs(wc.avgBoxHeigth - cwc.avgBoxHeigth));
						continue;
					}
					if ((Math.abs(wc.avgBoxWidth - cwc.avgBoxWidth) * 10) > Math.max(wc.avgBoxWidth, cwc.avgBoxWidth)) {
						System.out.println(" ==> width out of range, distance is " + Math.abs(wc.avgBoxWidth - cwc.avgBoxWidth));
						break; // with decreasing-width sort order, we know there is no better one coming
					}
					
					//	don't try to match clusters with all too different font sizes
					if (Math.abs(wc.fontSize - cwc.fontSize) > 1) {
						System.out.println(" ==> font size out of range, difference is " + Math.abs(wc.fontSize - cwc.fontSize));
						continue;
					}
					
					//	try and match clusters if (a) font size difference at most 1, (b) either of bold or italics properties match, and (c) box proportion distance is very low
					int styleMatchScore = 0;
					if (wc.fontSize == cwc.fontSize)
						styleMatchScore++;
					if (wc.isBold == cwc.isBold)
						styleMatchScore++;
					if (wc.isItalics == cwc.isItalics)
						styleMatchScore++;
					if (Math.abs(wc.avgBoxProportion - cwc.avgBoxProportion) <= 0.05)
						styleMatchScore++;
					
					//	too little overlapping properties
					if (styleMatchScore < 3) {
						System.out.println(" ==> style mismatch");
						continue;
					}
					
					//	compare representatives
					ExtendedWordImageMatch cwim = matchWordImages(wc.representative, cwc.representative, true, false, 1);
					System.out.println(" - similarity is " + cwim.base.sim);
					
					//	do we have a match?
					int action = assessMatch(cwim, wc, cwc, false, clusterMatchStats, wordMatchStats, renderedWordImageCache, true);
					if (action == CLUSTER_MATCH_MERGE) {
						System.out.println(" ==> merged");
						wordClusterMatches.add(new WordClusterMatch(wc, cwc, cwim, false));
					}
					else if (action == CLUSTER_MATCH_SWAP_MERGE) {
						System.out.println(" ==> swapped and merged");
						wordClusterMatches.add(new WordClusterMatch(cwc, wc, cwim, false));
					}
					else if (action == CLUSTER_MATCH_GROUP) {
						System.out.println(" ==> grouped for similarity, but not merged");
						wordClusterMatches.add(new WordClusterMatch(wc, cwc, cwim, true));
					}
					else System.out.println(" ==> image mismatch");
				}
//				if (debug && wc.name.endsWith("ae")) JOptionPane.showMessageDialog(null, "Continue with mergers", "Cluster Mergers", JOptionPane.PLAIN_MESSAGE, new ImageIcon(wc.representative.img));
			}
		}, wordClusters.size(), (this.debug ? 1 : -1));
		
		//	sort word cluster matches by similarity
		Collections.sort(wordClusterMatches, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				WordClusterMatch wcm1 = ((WordClusterMatch) obj1);
				WordClusterMatch wcm2 = ((WordClusterMatch) obj2);
				return Float.compare(wcm2.wim.base.sim, wcm1.wim.base.sim);
			}
		});
		
		//	what do we have?
		System.out.println("Got " + wordClusterMatches.size() + " word cluster matches:");
		for (int m = 0; m < wordClusterMatches.size(); m++) {
			WordClusterMatch wcm = ((WordClusterMatch) wordClusterMatches.get(m));
			System.out.println(" - " + (wcm.isGrouping ? "grouping" : "merger") + ": '" + wcm.swc.name + "' (" + wcm.swc.wordImages.size() + " words) and '" + wcm.rwc.name + "' (" + wcm.rwc.wordImages.size() + " words)");
		}
//		if (this.debug) JOptionPane.showMessageDialog(null, "Continue with mergers", "Cluster Mergers", JOptionPane.PLAIN_MESSAGE);
		
		//	perform actual mergers
		for (int m = 0; m < wordClusterMatches.size(); m++) {
			WordClusterMatch wcm = ((WordClusterMatch) wordClusterMatches.get(m));
			
			//	either of these two has been merged otherwise before
			if ((wcm.swc.wordImages.size() == 0) || (wcm.rwc.wordImages.size() == 0))
				continue;
			
			//	this one's a grouping, we'll save that later on
			if (wcm.isGrouping) {
				if (this.debug) displayWordMatch(wcm.wim, "Cluster grouping");
				continue;
			}
			
			/* if similarity is low, re-compute match (super-rendering
			 * assessment might have been off due to OCR errors in both
			 * clusters, which are hopefully merged away by higher similarity
			 * previous mergers) */
			if (wcm.wim.base.sim < wordMatchStats.avgSim) {
				System.out.println("Re-assessing low-similarity merger of cluster '" + wcm.swc.name + "' (" + wcm.swc.wordImages.size() + " words) and cluster '" + wcm.rwc.name + "' (" + wcm.rwc.wordImages.size() + " words) at similarity " + wcm.wim.base.sim);
				ExtendedWordImageMatch vwim = matchWordImages(wcm.swc.representative, wcm.rwc.representative, true, false, 1);
				int vAction = assessMatch(vwim, wcm.swc, wcm.rwc, false, clusterMatchStats, wordMatchStats, renderedWordImageCache, !this.debug);
				if (vAction == CLUSTER_MATCH_GROUP) {
					wordClusterMatches.set(m, new WordClusterMatch(wcm.swc, wcm.rwc, vwim, true));
					continue;
				}
				else if (vAction == CLUSTER_MATCH_MERGE) {}
				else if (vAction == CLUSTER_MATCH_SWAP_MERGE) {
					WordCluster swc = wcm.swc;
					wcm.swc = wcm.rwc;
					wcm.rwc = swc;
					vAction = CLUSTER_MATCH_MERGE;
				}
				else {
					wordClusterMatches.remove(m--);
					continue;
				}
			}
			
			//	decide which way to merge
			WordCluster wc = wcm.swc;
			WordCluster mwc = wcm.rwc;
			System.out.println("Merging cluster '" + mwc.name + "' (" + mwc.wordImages.size() + " words) into cluster '" + wc.name + "' (" + wc.wordImages.size() + " words) at similarity " + wcm.wim.base.sim);
			
			//	perform merge
			for (int i = 0; i < mwc.wordImages.size(); i++)
				wc.addWordImage((WordImage) mwc.wordImages.get(i));
			wc.compileRepresentative();
			mwc.wordImages.clear();
			wordClustersToSets.remove(mwc);
//			if (this.debug) JOptionPane.showMessageDialog(null, ("Cluster '" + mwc.name + "' merged into cluster '" + wc.name + "'"), "Cluster Merge Result", JOptionPane.PLAIN_MESSAGE, new ImageIcon(wc.representative.img));
			
			//	clean up to-come mergers
			for (int um = (m+1); um < wordClusterMatches.size(); um++) {
				WordClusterMatch uwcm = ((WordClusterMatch) wordClusterMatches.get(um));
				if (uwcm.swc == mwc)
					uwcm.swc = wc;
				if (uwcm.rwc == mwc)
					uwcm.rwc = wc;
				if (uwcm.swc == uwcm.rwc) // this one has been handled transitively
					wordClusterMatches.remove(um--);
			}
			
			//	clean up merger just worked off
			wordClusterMatches.remove(m--);
		}
		
		//	sort out empty clusters
		for (int c = 0; c < wordClusters.size(); c++) {
			WordCluster wc = ((WordCluster) wordClusters.get(c));
			if (wc.wordImages.isEmpty())
				wordClusters.remove(c--);
		}
		
		//	sort out empty cluster sets
		for (int s = 0; s < wordClusterSets.size(); s++) {
			WordClusterSet wcs = ((WordClusterSet) wordClusterSets.get(s));
			wcs.cleanup();
			if (wcs.wordCount == 0)
				wordClusterSets.remove(s--);
		}
		
		//	sort clusters by size
		Collections.sort(wordClusters, new Comparator() {
			public int compare(Object obj1, Object obj2) {
				WordCluster wc1 = ((WordCluster) obj1);
				WordCluster wc2 = ((WordCluster) obj2);
				return (wc2.wordImages.size() - wc1.wordImages.size());
			}
		});
		
		//	what do we have?
		System.out.println("Got " + wordClusters.size() + " word clusters:");
		for (int c = 0; c < wordClusters.size(); c++) {
			WordCluster wc = ((WordCluster) wordClusters.get(c));
			System.out.println(" - '" + wc.name + "' with " + wc.wordImages.size() + " words");
		}
		
		//	count OCR word frequencies across clusters to help with smaller clusters
		final CountingSet docRawOcrStrings = new CountingSet(Collections.synchronizedMap(new TreeMap()));
		final CountingSet docNormOcrStrings = new CountingSet(Collections.synchronizedMap(new TreeMap()));
		final CountingSet docCiRawOcrStrings = new CountingSet(Collections.synchronizedMap(new TreeMap(String.CASE_INSENSITIVE_ORDER)));
		final CountingSet docCiNormOcrStrings = new CountingSet(Collections.synchronizedMap(new TreeMap(String.CASE_INSENSITIVE_ORDER)));
		for (int c = 0; c < wordClusters.size(); c++) {
			WordCluster wc = ((WordCluster) wordClusters.get(c));
			
			//	ignore numbers, at least for now, as they are a lot less likely to repeat than words
			if (Gamta.isNumber(wc.name))
				continue;
			
			//	ignore Roman numbers, at least for now, as they rarely mis-OCR
			if (Gamta.isRomanNumber(wc.name.replaceAll("[^a-zA-Z]", "")))
				continue;
			
			//	add OCR words to global counts
			for (int w = 0; w < wc.wordImages.size(); w++) {
				WordImage wi = ((WordImage) wc.wordImages.get(w));
				String rawString = splitLigatures(wi.word.getString());
				docRawOcrStrings.add(rawString);
				docCiRawOcrStrings.add(rawString);
				String normString = stripAccents(rawString);
				docNormOcrStrings.add(normString);
				docCiNormOcrStrings.add(normString);
			}
		}
		
		//	inside clusters, do majority vote on OCR results
		ParallelJobRunner.runParallelFor(new ParallelFor() {
			public void doFor(int c) throws Exception {
				WordCluster wc = ((WordCluster) wordClusters.get(c));
				
				//	ignore numbers, at least for now, as they are a lot less likely to repeat than words
				if (Gamta.isNumber(wc.name)) {
					this.updateWords(wc, null);
					return;
				}
				
				//	ignore Roman numbers, at least for now, as they rarely mis-OCR
				if (Gamta.isRomanNumber(wc.name.replaceAll("[^a-zA-Z]", ""))) {
					this.updateWords(wc, null);
					return;
				}
				
				//	little to vote over in this one
				if (wc.wordImages.size() == 1) {
					WordImage wi = ((WordImage) wc.wordImages.get(0));
					String ocrString = splitLigatures(wi.word.getString());
					if (!ocrString.equals(wi.word.getString())) {
						recordLetterReplacements(wi.word.getString(), ocrString, letterReplacements);
						wi.word.setString(ocrString);
					}
					return;
				}
				
				//	run majority vote on cluster members, also regarding italics
				CountingSet rawOcrStrings = new CountingSet(new TreeMap());
				CountingSet normOcrStrings = new CountingSet(new TreeMap());
				Properties rawStringsToNormStrings = new Properties();
				for (int w = 0; w < wc.wordImages.size(); w++) {
					WordImage wi = ((WordImage) wc.wordImages.get(w));
					String rawString = splitLigatures(wi.word.getString());
					rawOcrStrings.add(rawString);
					String normString = stripAccents(rawString);
					normOcrStrings.add(normString);
					rawStringsToNormStrings.setProperty(rawString, normString);
				}
				
				//	this one's consistent, just the ligatures left to deal with
				if (rawOcrStrings.elementCount() == 1) {
					String ocrString = ((String) rawOcrStrings.first());
					this.updateWords(wc, ocrString);
					return;
				}
				
				//	what do we have?
				System.out.println("Got " + rawOcrStrings.elementCount() + " raw OCR strings in cluster '" + wc.name + "' with " + wc.wordImages.size() + " words:");
				for (Iterator sit = rawOcrStrings.iterator(); sit.hasNext();) {
					String str = ((String) sit.next());
					System.out.println(" - '" + str + "' (" + rawOcrStrings.getCount(str) + " times in cluster, " + docRawOcrStrings.getCount(str) + " times in document)");
				}
				System.out.println("Got " + normOcrStrings.elementCount() + " normalized OCR strings in cluster '" + wc.name + "' with " + wc.wordImages.size() + " words:");
				for (Iterator sit = normOcrStrings.iterator(); sit.hasNext();) {
					String str = ((String) sit.next());
					System.out.println(" - '" + str + "' (" + normOcrStrings.getCount(str) + " times in cluster, " + docNormOcrStrings.getCount(str) + " times in document)");
				}
				
				//	get most likely OCR result
				String ocrString = this.getOcrString(wc, rawOcrStrings, normOcrStrings, rawStringsToNormStrings);
				this.updateWords(wc, ocrString);
			}
			
			private void updateWords(WordCluster wc, String ocrString) {
				int boldCount = 0;
				int italicsCount = 0;
				for (int w = 0; w < wc.wordImages.size(); w++) {
					WordImage wi = ((WordImage) wc.wordImages.get(w));
					if (wi.word.hasAttribute(ImWord.BOLD_ATTRIBUTE))
						boldCount++;
					if (wi.word.hasAttribute(ImWord.ITALICS_ATTRIBUTE))
						italicsCount++;
				}
				boolean isBold = ((boldCount * 2) > wc.wordImages.size());
				boolean isItalics = ((italicsCount * 2) > wc.wordImages.size());
				for (int w = 0; w < wc.wordImages.size(); w++) {
					WordImage wi = ((WordImage) wc.wordImages.get(w));
					if ((ocrString != null) && !ocrString.equals(wi.word.getString())) {
						recordLetterReplacements(wi.word.getString(), ocrString, letterReplacements);
						wi.word.setString(ocrString);
					}
					if (wi.word.hasAttribute(ImWord.BOLD_ATTRIBUTE) != isBold)
						wi.word.setAttribute(ImWord.BOLD_ATTRIBUTE, (isBold ? "true" : null));
					if (wi.word.hasAttribute(ImWord.ITALICS_ATTRIBUTE) != isItalics)
						wi.word.setAttribute(ImWord.ITALICS_ATTRIBUTE, (isItalics ? "true" : null));
				}
			}
			
			private String getOcrString(WordCluster wc, CountingSet rawOcrStrings, CountingSet normOcrStrings, Properties rawStringsToNormStrings) {
				
				//	get most frequent OCR result
				String rawString = null;
				int rawStringFreq = 0;
				for (Iterator sit = rawOcrStrings.iterator(); sit.hasNext();) {
					String str = ((String) sit.next());
					int strFreq = rawOcrStrings.getCount(str);
					if (strFreq > rawStringFreq) {
						rawString = str;
						rawStringFreq = strFreq;
					}
					else if ((strFreq == rawStringFreq) && (docRawOcrStrings.getCount(str) > docRawOcrStrings.getCount(rawString))) {
						rawString = str;
						rawStringFreq = strFreq;
					}
					else if ((strFreq == rawStringFreq) && (docCiRawOcrStrings.getCount(str) > docCiRawOcrStrings.getCount(rawString))) {
						rawString = str;
						rawStringFreq = strFreq;
					}
				}
				
				//	we have a clear winner, done here
				if ((rawStringFreq * 2) > rawOcrStrings.size()) {
					System.out.println(" ==> '" + rawString + "' selected for local frequency");
					return rawString;
				}
				
				//	we have a winner from a global point of view
				if (docRawOcrStrings.getCount(rawString) > rawOcrStrings.size()) {
					System.out.println(" ==> '" + rawString + "' selected for global frequency");
					return rawString;
				}
				
				//	see if we have a winner in abstraction from accents
				String normString = null;
				int normStringFreq = 0;
				for (Iterator sit = normOcrStrings.iterator(); sit.hasNext();) {
					String str = ((String) sit.next());
					int strFreq = normOcrStrings.getCount(str);
					if (strFreq > normStringFreq) {
						normString = str;
						normStringFreq = strFreq;
					}
					else if ((strFreq == normStringFreq) && (docNormOcrStrings.getCount(str) > docNormOcrStrings.getCount(normString))) {
						normString = str;
						normStringFreq = strFreq;
					}
				}
				
				//	we have a clear winner at least in terms of base letters, try and find most frequent accented form of it
				if (((normStringFreq * 2) > normOcrStrings.size()) || (docNormOcrStrings.getCount(normString) > normOcrStrings.size())) {
					if ((normStringFreq * 2) > normOcrStrings.size())
						System.out.println(" --> '" + normString + "' selected as seed for local frequency");
					else System.out.println(" --> '" + normString + "' selected as seed for global frequency");
					rawString = null;
					rawStringFreq = 0;
					for (Iterator sit = rawOcrStrings.iterator(); sit.hasNext();) {
						String str = ((String) sit.next());
						if (!normString.equals(rawStringsToNormStrings.getProperty(str)))
							continue;
						if (rawOcrStrings.getCount(str) > rawStringFreq) {
							rawString = str;
							rawStringFreq = rawOcrStrings.getCount(str);
						}
					}
					
					//	we have a clear winner, done here
					if ((rawStringFreq * 2) > normOcrStrings.getCount(normString)) {
						System.out.println(" ==> '" + rawString + "' selected for accent-insensitive frequency");
						return rawString;
					}
				}
				
				//	try and use dictionary
				ArrayList rawDictStrings = new ArrayList();
				for (Iterator sit = rawOcrStrings.iterator(); sit.hasNext();) {
					String str = ((String) sit.next());
					int strFreq = rawOcrStrings.getCount(str);
					if (((strFreq * 2) > rawStringFreq) && dictionary.contains(str))
						rawDictStrings.add(str);
				}
				if (rawDictStrings.size() == 1) {
					rawString = ((String) rawDictStrings.get(0));
					System.out.println(" ==> '" + rawString + "' selected for sole dictionary membership");
					return rawString;
				}
				
				//	we still have a winner from a global point of view
				if (docCiRawOcrStrings.getCount(rawString) > rawOcrStrings.size()) {
					System.out.println(" ==> '" + rawString + "' selected for global case-insensitive frequency");
					return rawString;
				}
				
				//	use super-rendering to select winner
				System.out.println(" - rendering OCR results");
				float bestSim = 0;
				wc.compileRepresentative();
				for (Iterator sit = rawOcrStrings.iterator(); sit.hasNext();) {
					String str = ((String) sit.next());
					int strFreq = rawOcrStrings.getCount(str);
					if ((strFreq * 2) < rawStringFreq)
						continue;
					//	TODO observe previously-recorded letter replacement in selection
					WordImage prwi;
					WordImage irwi;
					synchronized (renderedWordImageCache) {
						prwi = WordImageAnalysis.renderWordImage(str, getBoundingBox(wc.representative.box), -1, Font.PLAIN, 10, wc.representative.pageImageDpi, renderedWordImageCache);
						irwi = WordImageAnalysis.renderWordImage(str, getBoundingBox(wc.representative.box), -1, Font.ITALIC, 10, wc.representative.pageImageDpi, renderedWordImageCache);
					}
					WordImageMatch pwim = WordImageAnalysis.matchWordImages(wc.representative, prwi, true, true);
					WordImageMatch iwim = WordImageAnalysis.matchWordImages(wc.representative, irwi, true, true);
					WordImageMatch wim = ((pwim.sim < iwim.sim) ? iwim : pwim);
					System.out.println(" - '" + str + "' rendered to similarity " + wim.sim + ", weight adjusted " + wim.waSim);
					if (bestSim < wim.sim) {
						System.out.println(" --> new best match");
						rawString = str;
						bestSim = wim.sim;
					}
					else System.out.println(" --> worse than '" + rawString + "' at " + bestSim);
					if (debug) {
						ExtendedWordImageMatch ewim = new ExtendedWordImageMatch(wim);
						ewim.computeMatchHistograms();
						ewim.computeMatchHistogramMetrics();
						displayWordMatch(ewim, "Super-rendering match");
					}
				}
				
				//	finally ...
				System.out.println(" ==> '" + rawString + "' selected for super-rendering similarity " + bestSim);
				return rawString;
			}
		}, wordClusters.size(), (this.debug ? 1 : -1));
		
		//	re-assess groupings, as representatives might have grown more similar from side mergers, and OCR correction might do yet more
		for (int m = 0; m < wordClusterMatches.size(); m++) {
			WordClusterMatch wcm = ((WordClusterMatch) wordClusterMatches.get(m));
			
			//	either of these two has been merged otherwise before
			if ((wcm.swc.wordImages.size() == 0) || (wcm.rwc.wordImages.size() == 0))
				continue;
			
			//	we're down to all groupings now, so we have to re-asses in any case
			wcm.swc.compileRepresentative();
			wcm.rwc.compileRepresentative();
			System.out.println("Re-assessing low-similarity merger of cluster '" + wcm.swc.name + "' (" + wcm.swc.wordImages.size() + " words) and cluster '" + wcm.rwc.name + "' (" + wcm.rwc.wordImages.size() + " words) at similarity " + wcm.wim.base.sim);
			ExtendedWordImageMatch vwim = matchWordImages(wcm.swc.representative, wcm.rwc.representative, true, false, 1);
			int vAction = assessMatch(vwim, wcm.swc, wcm.rwc, false, clusterMatchStats, wordMatchStats, renderedWordImageCache, !this.debug);
			if (vAction == CLUSTER_MATCH_GROUP)
				continue;
			else if (vAction == CLUSTER_MATCH_MERGE) {}
			else if (vAction == CLUSTER_MATCH_SWAP_MERGE) {
				WordCluster swc = wcm.swc;
				wcm.swc = wcm.rwc;
				wcm.rwc = swc;
				vAction = CLUSTER_MATCH_MERGE;
			}
			else {
				wordClusterMatches.remove(m--);
				continue;
			}
			
			//	decide which way to merge
			WordCluster wc = wcm.swc;
			WordCluster mwc = wcm.rwc;
			System.out.println("Merging cluster '" + mwc.name + "' (" + mwc.wordImages.size() + " words) into cluster '" + wc.name + "' (" + wc.wordImages.size() + " words) at similarity " + wcm.wim.base.sim);
			
			//	perform merge
			for (int i = 0; i < mwc.wordImages.size(); i++)
				wc.addWordImage((WordImage) mwc.wordImages.get(i));
			wc.compileRepresentative();
			mwc.wordImages.clear();
			wordClustersToSets.remove(mwc);
//			if (this.debug) JOptionPane.showMessageDialog(null, ("Cluster '" + mwc.name + "' group-merged into cluster '" + wc.name + "'"), "Cluster Merge Result", JOptionPane.PLAIN_MESSAGE, new ImageIcon(wc.representative.img));
			
			//	clean up to-come mergers
			for (int um = (m+1); um < wordClusterMatches.size(); um++) {
				WordClusterMatch uwcm = ((WordClusterMatch) wordClusterMatches.get(um));
				if (uwcm.swc == mwc)
					uwcm.swc = wc;
				if (uwcm.rwc == mwc)
					uwcm.rwc = wc;
				if (uwcm.swc == uwcm.rwc) // this one has been handled transitively
					wordClusterMatches.remove(um--);
			}
			
			//	clean up merger just worked off
			wordClusterMatches.remove(m--);
		}
		
		//	sort out empty clusters
		for (int c = 0; c < wordClusters.size(); c++) {
			WordCluster wc = ((WordCluster) wordClusters.get(c));
			if (wc.wordImages.size() == 0)
				wordClusters.remove(c--);
		}
		
		//	output this again so we have it for comparison
		System.out.println("Average cluster-internal similarity metrics based on " + clusterSimCount + " clusters, " + mlClusterSimCount + " multi-letter clusters:");
		System.out.println(" - box proportion is " + clusterMatchStats.avgBoxProp);
		System.out.println(" - similarity is " + clusterMatchStats.avgSim);
		System.out.println(" - weight adjusted similarity is " + clusterMatchStats.avgWaSim);
		System.out.println(" - min letter similarity is " + clusterMatchStats.avgMinLetterSim);
		System.out.println(" - max split similarity contrast is " + clusterMatchStats.avgMaxContrast + ", max deviation is " + clusterMatchStats.avgMaxDeviation);
		System.out.println(" - avg split similarity contrast is " + clusterMatchStats.avgAvgContrast + ", avg deviation is " + clusterMatchStats.avgAvgDeviation);
		System.out.println(" - sqr-avg split similarity contrast is " + clusterMatchStats.avgAvgSqrContrast + ", sqr-avg deviation is " + clusterMatchStats.avgAvgSqrDeviation);
		System.out.println(" - avg distance " + clusterMatchStats.avgAvgDistance + ", avg sqr distance " + clusterMatchStats.avgAvgSqrDistance + ", max distance " + clusterMatchStats.avgMaxDistance + ", relative max distance " + clusterMatchStats.avgRelMaxDistance);
		System.out.println("Weighted average cluster-internal word similarity metrics based on " + wordSimCount + " words, " + mlWordSimCount + " multi-letter words:");
		System.out.println(" - box proportion is " + wordMatchStats.avgBoxProp);
		System.out.println(" - similarity is " + wordMatchStats.avgSim);
		System.out.println(" - weight adjusted similarity is " + wordMatchStats.avgWaSim);
		System.out.println(" - min letter similarity is " + wordMatchStats.avgMinLetterSim);
		System.out.println(" - max split similarity contrast is " + wordMatchStats.avgMaxContrast + ", max deviation is " + wordMatchStats.avgMaxDeviation);
		System.out.println(" - avg split similarity contrast is " + wordMatchStats.avgAvgContrast + ", avg deviation is " + wordMatchStats.avgAvgDeviation);
		System.out.println(" - sqr-avg split similarity contrast is " + wordMatchStats.avgAvgSqrContrast + ", sqr-avg deviation is " + wordMatchStats.avgAvgSqrDeviation);
		System.out.println(" - avg distance " + wordMatchStats.avgAvgDistance + ", avg sqr distance " + wordMatchStats.avgAvgSqrDistance + ", max distance " + wordMatchStats.avgMaxDistance + ", relative max distance " + wordMatchStats.avgRelMaxDistance);
		
		//	re-group word clusters into sets
		wordClusterSets.clear();
		wordClusterSetIndex.clear();
		for (int c = 0; c < wordClusters.size(); c++) {
			WordCluster wc = ((WordCluster) wordClusters.get(c));
			String wcsKey = stripAccents(wc.name);
			WordClusterSet wcs = ((WordClusterSet) wordClusterSetIndex.get(wcsKey));
			if (wcs == null) {
				wcs = new WordClusterSet(wc);
				wordClusterSets.add(wcs);
				wordClusterSetIndex.put(wcsKey, wcs);
			}
			else wcs.addWordCluster(wc);
		}
		
		//	create runtime word clustering
		ImWordClustering wordClustering = new ImWordClustering(doc.docId);
		
		//	add word clusters, and put them into sets along the way
		for (int c = 0; c < wordClusters.size(); c++) {
			WordCluster wc = ((WordCluster) wordClusters.get(c));
			
			//	create runtime word cluster around first word
			String iwcId = Integer.toString(wc.hashCode(), 16).toUpperCase();
			ImWordCluster iwc = wordClustering.getClusterForId(iwcId, ((WordImage) wc.wordImages.get(0)).word);
			
			//	add subsequent words
			for (int w = 1; w < wc.wordImages.size(); w++)
				iwc.addWord(((WordImage) wc.wordImages.get(w)).word);
			
			//	add representative
			wc.compileRepresentative();
			iwc.representative = wc.representative.img;
		}
		
		//	add word cluster groupings (create from remaining mergers)
		for (int m = 0; m < wordClusterMatches.size(); m++) {
			WordClusterMatch wcm = ((WordClusterMatch) wordClusterMatches.get(m));
			
			//	either of these two must have been merged away before
			if ((wcm.swc.wordImages.size() == 0) || (wcm.rwc.wordImages.size() == 0) || (wcm.swc == wcm.rwc))
				continue;
			
			//	add cluster similarity
			String iwcId1 = Integer.toString(wcm.swc.hashCode(), 16).toUpperCase();
			String iwcId2 = Integer.toString(wcm.rwc.hashCode(), 16).toUpperCase();
			wordClustering.addClusterSimilarity(iwcId1, iwcId2, wcm.wim.base.sim);
		}
		
		//	add OCR wise word cluster similarities
		for (int s = 0; s < wordClusterSets.size(); s++) {
			WordClusterSet wcs = ((WordClusterSet) wordClusterSets.get(s));
			
			//	little to group here
			if (wcs.wordClusters.size() < 2)
				continue;
			
			//	do that loop join
			for (int c1 = 0; c1 < wcs.wordClusters.size(); c1++) {
				WordCluster wc1 = ((WordCluster) wcs.wordClusters.get(c1));
				String iwcId1 = Integer.toString(wc1.hashCode(), 16).toUpperCase();
				for (int c2 = (c1+1); c2 < wcs.wordClusters.size(); c2++) {
					WordCluster wc2 = ((WordCluster) wcs.wordClusters.get(c2));
					String iwcId2 = Integer.toString(wc2.hashCode(), 16).toUpperCase();
					wordClustering.addClusterSimilarity(iwcId1, iwcId2, 0);
				}
			}
		}
		
		//	store letter replacements
		for (Iterator lrit = letterReplacements.iterator(); lrit.hasNext();) {
			String lr = ((String) lrit.next());
			String[] lrData = lr.split("\\s+");
			if (lrData.length == 2)
				wordClustering.addLetterReplacement(lrData[0], lrData[1], letterReplacements.getCount(lr));
		}
		
		//	finally ...
		return wordClustering;
	}
	
	private static String splitLigatures(String str) {
		StringBuffer slStr = new StringBuffer();
		for (int c = 0; c < str.length(); c++) {
			if (str.charAt(c) == '\u0192')
				slStr.append("f"); // handle italics 'f' separately
			else {
				String nCh = StringUtils.getNormalForm(str.charAt(c));
				if (nCh.length() == 1)
					slStr.append(str.charAt(c));
				else slStr.append(nCh);
			}
		}
		return slStr.toString();
	}
	
	private static String stripAccents(String str) {
		StringBuffer asStr = new StringBuffer();
		for (int c = 0; c < str.length(); c++) {
//			ck.append(StringUtils.getBaseChar(str.charAt(c)));
			if (str.charAt(c) == '\u0192')
				asStr.append("f"); // handle italics 'f' separately
			else asStr.append(StringUtils.getNormalForm(str.charAt(c)));
		}
		return asStr.toString();
	}
	
	private static String getClusterKey(ImWord imw) {
		return (((String) imw.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE, "-1")) + "_" + (imw.hasAttribute(ImWord.BOLD_ATTRIBUTE) ? "bold_" : "") + (imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE) ? "italics_" : "") + stripAccents(imw.getString()));
	}
	
	private static final int CLUSTER_MATCH_MISMATCH = 0;
	private static final int CLUSTER_MATCH_MERGE = 1;
	private static final int CLUSTER_MATCH_SWAP_MERGE = -1;
	private static final int CLUSTER_MATCH_GROUP = 2;
	
	private int assessMatch(ExtendedWordImageMatch wim, WordCluster swc, WordCluster rwc, boolean clusterSetMatch, WordImageMatchStats clusterMatchStats, WordImageMatchStats wordMatchStats, Map renderedWordImageCache, boolean auto) {
		if (wim.base.sim < (clusterSetMatch? 0.60 : 0.50))
			return CLUSTER_MATCH_MISMATCH; // just too bad
		
		System.out.println("Word box match stats:");
		System.out.println(" - shifts are: left " + wim.base.leftShift + ", right " + wim.base.rightShift + ", top " + wim.base.topShift + ", bottom " + wim.base.bottomShift);
		System.out.println(" - area proportion distance is " + Math.abs(wim.base.scanned.boxProportion - wim.base.rendered.boxProportion));
		System.out.println(" - similarity is " + wim.base.sim + " (" + wim.base.precision + "/" + wim.base.recall + ")");
		System.out.println(" - weight adjusted similarity is " + wim.base.waSim + " (" + wim.base.waPrecision + "/" + wim.base.waRecall + ")");
		
		//	compute match column histograms
		wim.computeMatchHistograms();
		
		//	compute bucketized histograms
		wim.computeMatchHistogramMetrics();
		System.out.println(" - min letter similarity is " + wim.minLetterSim + ((wim.minLetterSimBucket == -1) ? "" : (" at " + wim.minLetterSimBucket + " in " + Arrays.toString(wim.minLetterSimBuckets))));
		System.out.println(" - max split similarity contrast is " + wim.maxSimBucketContrast + ", max deviation is " + wim.maxSimBucketDeviation);
		System.out.println(" - avg split similarity contrast is " + wim.simBucketContrast + ", avg deviation is " + wim.simBucketDeviation);
		System.out.println(" - sqr-avg split similarity contrast is " + wim.simBucketContrastSqr + ", sqr-avg deviation is " + wim.simBucketDeviationSqr);
		
		//	average contrast >=0.20 ==> mis-match
		if (wim.simBucketContrast >= 0.20)
			return CLUSTER_MATCH_MISMATCH;
		
		//	maximum contrast >=0.20 ==> mis-match
		if (wim.maxSimBucketContrast >= 0.25)
			return CLUSTER_MATCH_MISMATCH;
		
		//	minimum letter similarity <=0.50 ==> mis-match
		if (wim.minLetterSim <= 0.50)
			return CLUSTER_MATCH_MISMATCH;
		
		//	compute metrics for non-matching areas
		wim.computeMatchAreaSurfaces();
		wim.computeMisMatchDistances();
		System.out.println(" - matched " + wim.base.matched + ", surface " + wim.matchedSurface);
		System.out.println(" - spurious " + wim.base.scannedOnly + ", surface " + wim.scannedOnlySurface + ", avg distance " + wim.scannedOnlyAvgDist + ", avg sqr distance " + wim.scannedOnlyAvgSqrDist + ", max distance " + wim.scannedOnlyMaxDist);
		System.out.println(" - missed " + wim.base.renderedOnly + ", surface " + wim.renderedOnlySurface + ", avg distance " + wim.renderedOnlyAvgDist + ", avg sqr distance " + wim.renderedOnlyAvgSqrDist + ", max distance " + wim.renderedOnlyMaxDist);
		
		//	average distance >=2.0 ==> mis-match
		if (((wim.scannedOnlyAvgDist + wim.renderedOnlyAvgDist) / 2) >= 2.0)
			return CLUSTER_MATCH_MISMATCH;
		
		//	average square distance >=2.5 ==> mis-match
		if (((wim.scannedOnlyAvgSqrDist + wim.renderedOnlyAvgSqrDist) / 2) >= 2.5)
			return CLUSTER_MATCH_MISMATCH;
		
		//	max distance >=(DPI / 20) ==> mis-match (little more than a mm)
		if (Math.max(wim.scannedOnlyMaxDist, wim.renderedOnlyMaxDist) >= (Math.max(wim.base.scanned.pageImageDpi, wim.base.rendered.pageImageDpi) / 20))
			return CLUSTER_MATCH_MISMATCH;
		
		//	width-relative max distance >=0.1 ==> mis-match (cut italics a little more slack, as they might have missed punctuation marks attached)
		float maxDist = ((float) Math.max(wim.scannedOnlyMaxDist, wim.renderedOnlyMaxDist));
		int relMaxDistDenom = Math.max((Math.max(wim.base.scanned.pageImageDpi, wim.base.rendered.pageImageDpi) / ((wim.base.scanned.isItalics && wim.base.rendered.isItalics) ? 4 : 6)), wim.matchedHist.length);
		float relMaxDist = (maxDist / relMaxDistDenom);
		System.out.println(" - max relative distance is " + relMaxDist);
		if (relMaxDist >= 0.1)
			return CLUSTER_MATCH_MISMATCH;
		
		//	measure Levenshtein distance and edits ...
		int sIndex = 0;
		int rIndex = 0;
		StringBuffer replaced = null;
		StringBuffer replacement = null;
		
		//	... for the raw OCR result ...
		int[] editSequence = StringUtils.getLevenshteinEditSequence(wim.base.scanned.str, wim.base.rendered.str, true);
		int absEditDist = 0;
		int absCiEditDist = 0;
		ArrayList edits = new ArrayList(3);
		sIndex = 0;
		rIndex = 0;
		replaced = null;
		replacement = null;
		int absMaxEditLength = 0;
		for (int e = 0; e < editSequence.length; e++) {
			if (editSequence[e] == StringUtils.LEVENSHTEIN_KEEP) {
				if ((replaced != null) && (replacement != null)) {
					if (absMaxEditLength == 0)
						System.out.println(" - character replacements:");
					System.out.println("   - '" + replaced + "' at " + (sIndex - replaced.length()) + " replaced by '" + replacement + "' at " + (rIndex - replacement.length()));
					absMaxEditLength = Math.max(absMaxEditLength, Math.max(replaced.length(), replacement.length()));
					edits.add(new WordStringEdit(replaced.toString(), (sIndex - replaced.length()), replacement.toString(), (rIndex - replacement.length())));
				}
				sIndex++;
				rIndex++;
				replaced = null;
				replacement = null;
				continue;
			}
			if ((replaced == null) && (replacement == null)) {
				replaced = new StringBuffer();
				replacement = new StringBuffer();
			}
			if (editSequence[e] == StringUtils.LEVENSHTEIN_REPLACE) {
				absEditDist++;
				if (Character.toLowerCase(wim.base.scanned.str.charAt(sIndex)) != Character.toLowerCase(wim.base.rendered.str.charAt(rIndex)))
					absCiEditDist++;
				replaced.append(wim.base.scanned.str.charAt(sIndex++));
				replacement.append(wim.base.rendered.str.charAt(rIndex++));
			}
			else if (editSequence[e] == StringUtils.LEVENSHTEIN_DELETE) {
				absEditDist++;
				absCiEditDist++;
				replaced.append(wim.base.scanned.str.charAt(sIndex++));
			}
			else if (editSequence[e] == StringUtils.LEVENSHTEIN_INSERT) {
				absEditDist++;
				absCiEditDist++;
				replacement.append(wim.base.rendered.str.charAt(rIndex++));
			}
		}
		if ((replaced != null) && (replacement != null)) {
			if (absMaxEditLength == 0)
				System.out.println(" - character replacements:");
			System.out.println("   - '" + replaced + "' at " + (sIndex - replaced.length()) + " replaced by '" + replacement + "' at " + (rIndex - replacement.length()));
			absMaxEditLength = Math.max(absMaxEditLength, Math.max(replaced.length(), replacement.length()));
			edits.add(new WordStringEdit(replaced.toString(), (sIndex - replaced.length()), replacement.toString(), (rIndex - replacement.length())));
		}
		float relEditDist = (((float) absEditDist) / Math.max(wim.base.scanned.str.length(), wim.base.rendered.str.length()));
		float relMaxEditLength = (((float) absMaxEditLength) / Math.max(wim.base.scanned.str.length(), wim.base.rendered.str.length()));
		System.out.println(" - full relative edit distance is " + relEditDist + ", maximum continuous edit is " + relMaxEditLength);
		
		//	... as well as abstracted from accents and other diacritic markers
		String sBaseStr = stripAccents(wim.base.scanned.str);
		String rBaseStr = stripAccents(wim.base.rendered.str);
		int[] baseEditSequence = StringUtils.getLevenshteinEditSequence(sBaseStr, rBaseStr, true);
		int absBaseEditDist = 0;
		int absCiBaseEditDist = 0;
		ArrayList baseEdits = new ArrayList(3);
		sIndex = 0;
		rIndex = 0;
		replaced = null;
		replacement = null;
		int absMaxBaseEditLength = 0;
		for (int e = 0; e < baseEditSequence.length; e++) {
			if (baseEditSequence[e] == StringUtils.LEVENSHTEIN_KEEP) {
				if ((replaced != null) && (replacement != null)) {
					if (absMaxBaseEditLength == 0)
						System.out.println(" - character replacements:");
					System.out.println("   - '" + replaced + "' at " + (sIndex - replaced.length()) + " replaced by '" + replacement + "' at " + (rIndex - replacement.length()));
					absMaxBaseEditLength = Math.max(absMaxBaseEditLength, Math.max(replaced.length(), replacement.length()));
					baseEdits.add(new WordStringEdit(replaced.toString(), (sIndex - replaced.length()), replacement.toString(), (rIndex - replacement.length())));
				}
				sIndex++;
				rIndex++;
				replaced = null;
				replacement = null;
				continue;
			}
			if ((replaced == null) && (replacement == null)) {
				replaced = new StringBuffer();
				replacement = new StringBuffer();
			}
			if (baseEditSequence[e] == StringUtils.LEVENSHTEIN_REPLACE) {
				absBaseEditDist++;
				if (Character.toLowerCase(sBaseStr.charAt(sIndex)) != Character.toLowerCase(rBaseStr.charAt(rIndex)))
					absCiBaseEditDist++;
				replaced.append(sBaseStr.charAt(sIndex++));
				replacement.append(rBaseStr.charAt(rIndex++));
			}
			else if (baseEditSequence[e] == StringUtils.LEVENSHTEIN_DELETE) {
				absBaseEditDist++;
				absCiBaseEditDist++;
				replaced.append(sBaseStr.charAt(sIndex++));
			}
			else if (baseEditSequence[e] == StringUtils.LEVENSHTEIN_INSERT) {
				absBaseEditDist++;
				absCiBaseEditDist++;
				replacement.append(rBaseStr.charAt(rIndex++));
			}
		}
		if ((replaced != null) && (replacement != null)) {
			if (absMaxBaseEditLength == 0)
				System.out.println(" - character replacements:");
			System.out.println("   - '" + replaced + "' at " + (sIndex - replaced.length()) + " replaced by '" + replacement + "' at " + (rIndex - replacement.length()));
			absMaxBaseEditLength = Math.max(absMaxBaseEditLength, Math.max(replaced.length(), replacement.length()));
			baseEdits.add(new WordStringEdit(replaced.toString(), (sIndex - replaced.length()), replacement.toString(), (rIndex - replacement.length())));
		}
		float relBaseEditDist = (((float) absBaseEditDist) / Math.max(sBaseStr.length(), rBaseStr.length()));
		float relMaxBaseEditLength = (((float) absMaxBaseEditLength) / Math.max(sBaseStr.length(), rBaseStr.length()));
		System.out.println(" - base relative edit distance is " + relBaseEditDist + ", maximum continuous edit is " + relMaxBaseEditLength);
		
		//	test if edits overlap with min letter similarity
		boolean minLetterSimOutsideEdit;
		boolean minLetterSimOutsideBaseEdit;
		if (wim.minLetterSimBucket != -1) {
			int sMinSimLetterIndex = ((wim.minLetterSimBucket * wim.base.scanned.str.length()) / wim.minLetterSimBuckets.length);
			int rMinSimLetterIndex = ((wim.minLetterSimBucket * wim.base.rendered.str.length()) / wim.minLetterSimBuckets.length);
			minLetterSimOutsideEdit = ((absEditDist != 0) && (wim.base.scanned.str.charAt(sMinSimLetterIndex) == wim.base.rendered.str.charAt(rMinSimLetterIndex)));
			minLetterSimOutsideBaseEdit = ((absBaseEditDist != 0) && (sBaseStr.charAt(sMinSimLetterIndex) == rBaseStr.charAt(rMinSimLetterIndex)));
		}
		else {
			minLetterSimOutsideEdit = false;
			minLetterSimOutsideBaseEdit = false;
		}
//		
//		//	measure weight distance LOOKS PRETTY USELESS
//		float scannedWeight = (((float) wim.base.scanned.pixelCount) / (wim.base.scanned.box.getWidth() * wim.base.scanned.box.getHeight()));
//		float renderedWeight = (((float) wim.base.rendered.pixelCount) / (wim.base.rendered.box.getWidth() * wim.base.rendered.box.getHeight()));
//		float weightRelation = (Math.min(scannedWeight, renderedWeight) / Math.max(scannedWeight, renderedWeight));
//		System.out.println(" - weight relation is " + weightRelation);
		
		//	compare top members of multi-word clusters to one another
		float topSim = 0;
		float topWaSim = 0;
		float topMinLetterSim = 0;
		if ((swc.wordImages.size() > 1) || (rwc.wordImages.size() > 1)) {
			
			//	order cluster members by similarity to representative
			swc.orderWordImages();
			rwc.orderWordImages();
			
			//	compare to best matches for removal verification
			int stwic = ((int) Math.floor(Math.sqrt(swc.wordImages.size())));
			int rtwic = ((int) Math.floor(Math.sqrt(rwc.wordImages.size())));
			float wiSimSum = 0;
			float wiWaSimSum = 0;
			float wiMinLetterSimSum = 0;
			for (int sw = 0; sw < stwic; sw++) {
				WordImage swi = ((WordImage) swc.wordImages.get(sw));
				for (int rw = 0; rw < rtwic; rw++) {
					WordImage rwi = ((WordImage) rwc.wordImages.get(rw));
					ExtendedWordImageMatch twim = matchWordImages(swi, rwi, true, true, 1);
					wiSimSum += twim.base.sim;
					wiWaSimSum += twim.base.waSim;
					twim.computeMatchHistogramMetrics();
					wiMinLetterSimSum += twim.minLetterSim;
				}
			}
			topSim = (wiSimSum / (stwic * rtwic));
			topWaSim = (wiWaSimSum / (stwic * rtwic));
			topMinLetterSim = (wiMinLetterSimSum / (stwic * rtwic));
			System.out.println(" - similarity of top " + (stwic * rtwic) + " cluster words is " + topSim + ", weight adjusted " + topWaSim + ", min letters similarity is " + topMinLetterSim);
		}
		
		if (clusterSetMatch && ((wim.base.sim > wordMatchStats.avgSim) || (wim.base.waSim > wordMatchStats.avgWaSim)))
			return ((swc.wordImages.size() < rwc.wordImages.size()) ? CLUSTER_MATCH_SWAP_MERGE : CLUSTER_MATCH_MERGE);
		else if (clusterSetMatch && ((Math.max(wim.scannedOnlyMaxDist, wim.renderedOnlyMaxDist) * 9) > wim.matchedHist.length))
			return CLUSTER_MATCH_MISMATCH;
		else if (clusterSetMatch && ((topSim > wim.base.sim) || (topWaSim > wim.base.waSim)))
			return ((swc.wordImages.size() < rwc.wordImages.size()) ? CLUSTER_MATCH_SWAP_MERGE : CLUSTER_MATCH_MERGE);
		else if (clusterSetMatch) // TODO amend this with other criteria (if there are any with matching OCR)
			return CLUSTER_MATCH_MISMATCH;
		
		//	call mis-match if top members of of multi-word clusters don't match
		if (((swc.wordImages.size() > 1) || (rwc.wordImages.size() > 1)) && ((1 - topSim) > ((1 - wordMatchStats.avgSim) * 2)))
			return CLUSTER_MATCH_MISMATCH;
		
		//	call mismatch if edit distance too large (case insensitive, though, as small-caps usually come in numbers)
		if (!sBaseStr.equalsIgnoreCase(rBaseStr) && (absMaxBaseEditLength > 3))
			return CLUSTER_MATCH_MISMATCH;
		
		//	compute match score based on document averages
		int matchPoints = 0;
		int maxMatchPoints = 0;
		String zeroPointReason = null;
		
		//	figure in similarity and minimum letter similarity
		maxMatchPoints += 2;
		if ((1 - wim.base.sim) < (1 - wordMatchStats.avgSim))
			matchPoints += 2;
		else if ((1 - wim.base.sim) < ((1 - wordMatchStats.avgSim) * 2))
			matchPoints++;
		if (wim.maxContrastSimBuckets != -1) {
			maxMatchPoints += 2;
			if ((1 - wim.minLetterSim) < (1 - wordMatchStats.avgMinLetterSim))
				matchPoints += 2;
			else if ((1 - wim.minLetterSim) < ((1 - wordMatchStats.avgMinLetterSim) * 2))
				matchPoints++;
		}
		
		//	figure in average and maximum contrast (if string longer than 1)
		if (wim.maxContrastSimBuckets != -1) {
			maxMatchPoints += 2;
			if (wim.maxSimBucketContrast < wordMatchStats.avgMaxContrast)
				matchPoints += 2;
			else if (wim.maxSimBucketContrast < (wordMatchStats.avgMaxContrast * 1.5))
				matchPoints++;
			else if (wim.maxSimBucketContrast > Math.max(0.15, (wordMatchStats.avgMaxContrast * 2.5)))
				zeroPointReason = "MaxContrast";
			maxMatchPoints += 2;
			if (wim.simBucketContrast < wordMatchStats.avgAvgContrast)
				matchPoints += 2;
			else if (wim.simBucketContrast < (wordMatchStats.avgAvgContrast * 1.5))
				matchPoints++;
			else if (wim.simBucketContrast > Math.max(0.11, (wordMatchStats.avgAvgContrast * 2.5)))
				zeroPointReason = "AvgContrast";
		}
		
		//	figure in average distance and/or average square distance
		maxMatchPoints += 2;
		if (((wim.scannedOnlyAvgDist + wim.renderedOnlyAvgDist) / 2) < wordMatchStats.avgAvgDistance)
			matchPoints += 2;
		else if (((wim.scannedOnlyAvgDist + wim.renderedOnlyAvgDist) / 2) < (wordMatchStats.avgAvgDistance * 2))
			matchPoints++;
		maxMatchPoints += 2;
		if (((wim.scannedOnlyAvgSqrDist + wim.renderedOnlyAvgSqrDist) / 2) < wordMatchStats.avgAvgSqrDistance)
			matchPoints += 2;
		else if (((wim.scannedOnlyAvgSqrDist + wim.renderedOnlyAvgSqrDist) / 2) < (wordMatchStats.avgAvgSqrDistance * 2))
			matchPoints++;
		
		//	figure in maximum distance and relative maximum distance
		maxMatchPoints += 2;
		if (maxDist < wordMatchStats.avgMaxDistance)
			matchPoints += 2;
		else if (maxDist < (wordMatchStats.avgMaxDistance * 2))
			matchPoints++;
		maxMatchPoints += 2;
		if (relMaxDist < wordMatchStats.avgRelMaxDistance)
			matchPoints += 2;
		else if (relMaxDist < (wordMatchStats.avgRelMaxDistance * 2))
			matchPoints++;
		else if (relMaxDist > Math.max(0.1, (wordMatchStats.avgRelMaxDistance * 5)))
			zeroPointReason = "MaxRelDist";
		
		//	flag for box proportion distance
		if (Math.abs(wim.base.scanned.boxProportion - wim.base.rendered.boxProportion) > Math.max(0.1, (wordMatchStats.avgBoxProp * 5)))
			zeroPointReason = "BoxProportion";
		
		//	compute combined match score
		float matchScore = (((float) ((zeroPointReason == null) ? matchPoints : 0)) / maxMatchPoints);
		System.out.println(" - match score is " + matchScore + " (" + matchPoints + " out of " + maxMatchPoints + ((zeroPointReason == null) ? "" : (", but flagged for " + zeroPointReason)) + ")");
		
		//	this one's just too bad
		if (matchScore < 0.15)
			return CLUSTER_MATCH_MISMATCH;
		
		//	check dictionary
		int dictHitCount = 0;
		if (this.dictionary.contains(wim.base.scanned.str) || this.dictionary.contains(sBaseStr))
			dictHitCount++;
		if (this.dictionary.contains(wim.base.rendered.str) || this.dictionary.contains(rBaseStr))
			dictHitCount++;
		System.out.println(" - dictionary hit count is " + dictHitCount);
		
		//	compare super-rendered OCR result, both ways
		WordImage srwi;
		WordImage rrwi;
		synchronized (renderedWordImageCache) {
			srwi = WordImageAnalysis.renderWordImage(wim.base.scanned.str, getBoundingBox(wim.base.scanned.box), -1, (wim.base.scanned.isItalics ? Font.ITALIC : Font.PLAIN), 10, wim.base.scanned.pageImageDpi, renderedWordImageCache);
			rrwi = WordImageAnalysis.renderWordImage(wim.base.rendered.str, getBoundingBox(wim.base.rendered.box), -1, (wim.base.rendered.isItalics ? Font.ITALIC : Font.PLAIN), 10, wim.base.rendered.pageImageDpi, renderedWordImageCache);
		}
//		WordImage srwi = WordImageAnalysis.renderWordImage(wim.base.scanned.str, getBoundingBox(wim.base.scanned.box), -1, (wim.base.scanned.isItalics ? Font.ITALIC : Font.PLAIN), 10, wim.base.scanned.pageImageDpi, renderedWordImageCache);
//		WordImage rrwi = WordImageAnalysis.renderWordImage(wim.base.rendered.str, getBoundingBox(wim.base.rendered.box), -1, (wim.base.rendered.isItalics ? Font.ITALIC : Font.PLAIN), 10, wim.base.rendered.pageImageDpi, renderedWordImageCache);
		WordImageMatch swim = WordImageAnalysis.matchWordImages(wim.base.scanned, srwi, true, true);
		WordImageMatch rwim = WordImageAnalysis.matchWordImages(wim.base.rendered, rrwi, true, true);
		System.out.println(" - rendering similarities are " + swim.sim + " and " + rwim.sim);
		WordImageMatch xswim = WordImageAnalysis.matchWordImages(wim.base.scanned, rrwi, true, true);
		WordImageMatch xrwim = WordImageAnalysis.matchWordImages(wim.base.rendered, srwi, true, true);
		System.out.println(" - rendering cross similarities are " + xswim.sim + " and " + xrwim.sim);
		float renderingSimRelation = ((swim.sim * rwim.sim) / (xswim.sim * xrwim.sim));
		float renderingWaSimRelation = ((swim.waSim * rwim.waSim) / (xswim.waSim * xrwim.waSim));
		System.out.println(" - rendering similarity relation is " + renderingSimRelation + ", weight adjusted " + renderingWaSimRelation);
		float renderingCrossSimRelation = ((swim.sim * xrwim.sim) / (xswim.sim * rwim.sim));
		float renderingCrossWaSimRelation = ((swim.waSim * xrwim.waSim) / (xswim.waSim * rwim.waSim));
		boolean swapOnMerge;
		if ((renderingCrossSimRelation < 1) && (renderingCrossWaSimRelation < 1))
			swapOnMerge = true;
		else if ((renderingCrossSimRelation > 1) && (renderingCrossWaSimRelation > 1))
			swapOnMerge = false;
		else swapOnMerge = (swc.wordImages.size() < rwc.wordImages.size());
		if (renderingCrossSimRelation < 1)
			renderingCrossSimRelation = (1.0f / renderingCrossSimRelation);
		if (renderingCrossWaSimRelation < 1)
			renderingCrossWaSimRelation = (1.0f / renderingCrossWaSimRelation);
		System.out.println(" - rendering cross similarity relation is " + renderingCrossSimRelation + ", weight adjusted " + renderingCrossWaSimRelation);
		System.out.println(" - rendering similarity quotient is " + (renderingSimRelation / renderingCrossSimRelation) + ", weight adjusted " + (renderingWaSimRelation / renderingCrossWaSimRelation));
		System.out.println(" - merge order is " + (swapOnMerge ? "reverse" : "straight"));
		
		//	make prediction
		final int pv;
		int baseEditDist = 16;
		int optBaseEditDist = 16;
		int stemBaseEditDist = 16;
		boolean suffixEdit = false;
		
		/* TODO
OCR-group clusters whose optically compensated OCR edit distance is half (maybe
67% if either word not in dictionary) their OCR edit distance or less, or at
most 1, and stem edit distance at most 1
==> more "similar words" to show
==> might help identify more errors
==> but then, don't we already do this?
		 */
		
		/* if we have a mis-match only in terms of upper or lower case, base
		 * decision on super-rendering
		 * do we need a dictionary hit for additional verification? */
		if (sBaseStr.equalsIgnoreCase(rBaseStr)) {
			dictHitCount--;
			
			//	one rendering matching a lot better than the other
			if ((renderingSimRelation / renderingCrossSimRelation) < 0.50) {
				System.out.println(" ==> match (one of '" + sBaseStr + "' and '" + rBaseStr + "' appears to be an OCR case error)");
				pv = (swapOnMerge ? CLUSTER_MATCH_SWAP_MERGE : CLUSTER_MATCH_MERGE);
			}
			
			//	first letter easily mistaken for upper case in fonts with low ascenders, and rendering comparison shows clear tendency
			else if (sBaseStr.substring(1).equals(rBaseStr.substring(1)) && ("COPSVWXZcopsvwxz".indexOf(sBaseStr.charAt(0)) != -1) && ((renderingSimRelation / renderingCrossSimRelation) < 0.75)) {
				System.out.println(" ==> match (one of '" + sBaseStr + "' and '" + rBaseStr + "' appears to be an OCR case error)");
				pv = (swapOnMerge ? CLUSTER_MATCH_SWAP_MERGE : CLUSTER_MATCH_MERGE);
			}
			
			//	these two are the same (can happen on merge verification lookups)
			else if (sBaseStr.equals(rBaseStr)) {
				System.out.println(" ==> match (both words are the same: '" + sBaseStr + "')");
				pv = (swapOnMerge ? CLUSTER_MATCH_SWAP_MERGE : CLUSTER_MATCH_MERGE);
			}
			
			//	catch through-the-roof matches to own super-renderings
			else if ((renderingSimRelation / renderingCrossSimRelation) > 1.50) {
				System.out.println(" ==> mis-match (both of '" + sBaseStr + "' and '" + rBaseStr + "' appear to be OK in terms of case)");
				pv = CLUSTER_MATCH_MISMATCH;
			}
			
			//	this one's just too close to tell, call grouping match to avoid false positive
			else {
				System.out.println(" ==> group-match (both '" + sBaseStr + "' and '" + rBaseStr + "' seem to be OK in terms of case, but might not be)");
				pv = CLUSTER_MATCH_GROUP;
			}
		}
		
		//	if we have exactly one dictionary hit, so the other word looks like an OCR error
		else if (dictHitCount == 1) {
			
			/* extremely high similarity of top cluster members, match ...
			 * rationale: heaviness of representative doesn't improve
			 * similarity, but decrease it ==> extra pixels are off-target
			 * ones, not core word pixels, indicating a match */
			if (((1 - topWaSim) < ((1 - wordMatchStats.avgSim) * 2)) && (topWaSim > wim.base.sim) && (topMinLetterSim > wim.minLetterSim)) {
				System.out.println(" ==> match ('" + rBaseStr + "' appears to be an OCR error of '" + sBaseStr + "')");
				pv = (swapOnMerge ? CLUSTER_MATCH_SWAP_MERGE : CLUSTER_MATCH_MERGE);
			}
			
			//	these look sufficiently close
			else if ((renderingSimRelation / renderingCrossSimRelation) < 0.75) {
				System.out.println(" ==> match (one of '" + sBaseStr + "' and '" + rBaseStr + "' appears to be an OCR error)");
				pv = (swapOnMerge ? CLUSTER_MATCH_SWAP_MERGE : CLUSTER_MATCH_MERGE);
			}
			
			//	catch through-the-roof matches to own super-renderings
			else if ((renderingSimRelation / renderingCrossSimRelation) > 1.5) {
				System.out.println(" ==> mis-match (both of '" + sBaseStr + "' and '" + rBaseStr + "' appear to be valid words)");
				pv = CLUSTER_MATCH_MISMATCH;
			}
			
			//	call match if only dash or high comma replaced
			else if (sBaseStr.replaceAll("[^A-Za-z]", "").equals(rBaseStr.replaceAll("[^A-Za-z]", ""))) {
				System.out.println(" ==> match (one of '" + sBaseStr + "' and '" + rBaseStr + "' appears to have an embedded OCR artifact)");
				pv = (swapOnMerge ? CLUSTER_MATCH_SWAP_MERGE : CLUSTER_MATCH_MERGE);
			}
			
			//	compare edit distance
			else {
				baseEditDist = 0;
				optBaseEditDist = 0;
				stemBaseEditDist = 0;
				for (int e = 0; e < baseEdits.size(); e++) {
					WordStringEdit wse = ((WordStringEdit) baseEdits.get(e));
					baseEditDist += Math.max(wse.replaced.length(), wse.replacement.length());
					optBaseEditDist += (Math.max(wse.replaced.length(), wse.replacement.length()) / (wse.isOpticalyClose() ? 2 : 1));
					stemBaseEditDist += wse.getStemDiff();
					if (((sBaseStr.length() - 3) < (wse.sPos + wse.replaced.length())) && (rBaseStr.length() - 3) < (wse.rPos + wse.replacement.length()))
						suffixEdit = (suffixEdit || (wse.involvesVowel(true) && !wse.isOpticalyClose()));
				}
				System.out.println(" - difference is " + baseEditDist + ", optically compensated difference is " + optBaseEditDist + ", stem difference is " + stemBaseEditDist);
				
				//	these two look close, and with neither word in dictionary, they are likely the same, as longer out-of-dictionary words are a lot less densely distributed than short in-dictionary words
				if (!suffixEdit && (8 <= Math.min(wim.base.scanned.str.length(), wim.base.scanned.str.length())) && (baseEditDist <= 1)) {
					System.out.println(" ==> match (one of '" + sBaseStr + "' and '" + rBaseStr + "' appears to be an OCR error)");
					pv = (swapOnMerge ? CLUSTER_MATCH_SWAP_MERGE : CLUSTER_MATCH_MERGE);
				}
				else if (!suffixEdit && (10 <= Math.min(wim.base.scanned.str.length(), wim.base.scanned.str.length())) && (baseEditDist <= 2)) {
					System.out.println(" ==> match (one of '" + sBaseStr + "' and '" + rBaseStr + "' appears to be an OCR error)");
					pv = (swapOnMerge ? CLUSTER_MATCH_SWAP_MERGE : CLUSTER_MATCH_MERGE);
				}
				
				//	this one's too close to tell, call grouping match to avoid false positive
				else {
					System.out.println(" ==> group-match (one of '" + sBaseStr + "' and '" + rBaseStr + "' seems to be an OCR error, but might not be)");
					pv = CLUSTER_MATCH_GROUP;
				}
			}
		}
		
		//	we have two dictionary hits and a mis-match beyond case
		else if (dictHitCount == 2) {
			
			/* extremely high similarity of top cluster members, at least group
			 * ... rationale: heaviness of representative doesn't improve
			 * similarity, but decrease it ==> extra pixels are off-target
			 * ones, not core word pixels, indicating a grouping match even 
			 * though both words are in dictionary */
			if (((1 - topWaSim) < ((1 - wordMatchStats.avgSim) * 2)) && (topWaSim > wim.base.sim) && (topMinLetterSim > wim.minLetterSim)) {
				System.out.println(" ==> group-match ('" + rBaseStr + "' appears to be an OCR error of '" + sBaseStr + "')");
				pv = CLUSTER_MATCH_GROUP;
			}
			
			//	super-rendering inconclusive, call mis-match
			else if ((renderingSimRelation / renderingCrossSimRelation) > 1.0) {
				System.out.println(" ==> mis-match (both of '" + sBaseStr + "' and '" + rBaseStr + "' appear to be valid words)");
				pv = CLUSTER_MATCH_MISMATCH;
			}
			
			//	compare edit distance
			else {
				baseEditDist = 0;
				optBaseEditDist = 0;
				stemBaseEditDist = 0;
				for (int e = 0; e < baseEdits.size(); e++) {
					WordStringEdit wse = ((WordStringEdit) baseEdits.get(e));
					baseEditDist += Math.max(wse.replaced.length(), wse.replacement.length());
					optBaseEditDist += (Math.max(wse.replaced.length(), wse.replacement.length()) / (wse.isOpticalyClose() ? 2 : 1));
					stemBaseEditDist += wse.getStemDiff();
					if (((sBaseStr.length() - 3) < (wse.sPos + wse.replaced.length())) && (rBaseStr.length() - 3) < (wse.rPos + wse.replacement.length()))
						suffixEdit = (suffixEdit || (wse.involvesVowel(true) && !wse.isOpticalyClose()));
				}
				System.out.println(" - difference is " + baseEditDist + ", optically compensated difference is " + optBaseEditDist + ", stem difference is " + stemBaseEditDist);
				
				//	these two look close, call grouping match to avoid false positive
				if (!suffixEdit && (6 <= Math.min(wim.base.scanned.str.length(), wim.base.scanned.str.length())) && (baseEditDist <= 1)) {
					System.out.println(" ==> group-match (one of '" + sBaseStr + "' and '" + rBaseStr + "' seems to be an OCR error, but might not be)");
					pv = CLUSTER_MATCH_GROUP;
				}
				else if (!suffixEdit && (8 <= Math.min(wim.base.scanned.str.length(), wim.base.scanned.str.length())) && (baseEditDist <= 2)) {
					System.out.println(" ==> group-match (one of '" + sBaseStr + "' and '" + rBaseStr + "' seems to be an OCR error, but might not be)");
					pv = CLUSTER_MATCH_GROUP;
				}
				else {
					System.out.println(" ==> group-match (one of '" + sBaseStr + "' and '" + rBaseStr + "' might be an OCR error)");
					pv = CLUSTER_MATCH_GROUP;
				}
			}
		}
		
		//	we have no dictionary hits at all
		else {
			baseEditDist = 0;
			optBaseEditDist = 0;
			stemBaseEditDist = 0;
			for (int e = 0; e < baseEdits.size(); e++) {
				WordStringEdit wse = ((WordStringEdit) baseEdits.get(e));
				baseEditDist += Math.max(wse.replaced.length(), wse.replacement.length());
				optBaseEditDist += (Math.max(wse.replaced.length(), wse.replacement.length()) / (wse.isOpticalyClose() ? 2 : 1));
				stemBaseEditDist += wse.getStemDiff();
				if (((sBaseStr.length() - 3) < (wse.sPos + wse.replaced.length())) && (rBaseStr.length() - 3) < (wse.rPos + wse.replacement.length()))
					suffixEdit = (suffixEdit || (wse.involvesVowel(true) && !wse.isOpticalyClose()));
			}
			System.out.println(" - difference is " + baseEditDist + ", optically compensated difference is " + optBaseEditDist + ", stem difference is " + stemBaseEditDist);
			
			//	these two look close, and with neither word in dictionary, they are likely the same, as longer out-of-dictionary words are a lot less densely distributed than short in-dictionary words
			if (!suffixEdit && (6 <= Math.min(wim.base.scanned.str.length(), wim.base.scanned.str.length())) && (baseEditDist <= 1)) {
				System.out.println(" ==> match (one of '" + sBaseStr + "' and '" + rBaseStr + "' appears to be an OCR error)");
				pv = (swapOnMerge ? CLUSTER_MATCH_SWAP_MERGE : CLUSTER_MATCH_MERGE);
			}
			else if (!suffixEdit && (8 <= Math.min(wim.base.scanned.str.length(), wim.base.scanned.str.length())) && (baseEditDist <= 2)) {
				System.out.println(" ==> match (one of '" + sBaseStr + "' and '" + rBaseStr + "' appears to be an OCR error)");
				pv = (swapOnMerge ? CLUSTER_MATCH_SWAP_MERGE : CLUSTER_MATCH_MERGE);
			}
			
			//	these two look somewhat close, call grouping match to avoid false positive
			else {
				System.out.println(" ==> group-match (one of '" + sBaseStr + "' and '" + rBaseStr + "' might to be an OCR error)");
				pv = CLUSTER_MATCH_GROUP;
			}
		}
		
		//	use prompts only in development mode
		if (auto)
			return pv;
		synchronized (this) {
			int v = verifyWordMatch(wim, "Cluster match");
			if ((pv == CLUSTER_MATCH_MERGE) || (pv == CLUSTER_MATCH_SWAP_MERGE))
				this.matchPredictionCount++;
			else if (pv == CLUSTER_MATCH_MISMATCH)
				this.misMatchPredictionCount++;
			else this.goodMisMatches.add(wim);
			if (v == JOptionPane.YES_OPTION) {
				if ((pv == CLUSTER_MATCH_MERGE) || (pv == CLUSTER_MATCH_SWAP_MERGE))
					this.matchPredictionCorrect++;
				if (wim.base.sim > clusterMatchStats.avgSim)
					this.matchSimAboveClusterDiameter++;
				if (wim.base.sim > wordMatchStats.avgSim)
					this.matchSimAboveWeightedClusterDiameter++;
				if (wim.base.waSim > clusterMatchStats.avgSim)
					this.matchWaSimAboveClusterDiameter++;
				if (wim.base.waSim > wordMatchStats.avgSim)
					this.matchWaSimAboveWeightedClusterDiameter++;
				if (wim.minLetterSim > clusterMatchStats.avgSim)
					this.matchMinLetterSimAboveClusterDiameter++;
				if (wim.minLetterSim > wordMatchStats.avgSim)
					this.matchMinLetterSimAboveWeightedClusterDiameter++;
				if (wim.minLetterSim > wim.base.sim)
					this.matchMinLetterSimAboveSim++;
				if (wim.minLetterSim > clusterMatchStats.avgMinLetterSim)
					this.matchMinLetterSimAboveAvg++;
				if (wim.minLetterSim > wordMatchStats.avgMinLetterSim)
					this.matchMinLetterSimAboveWeightedAvg++;
				if (minLetterSimOutsideEdit)
					this.matchMinLetterSimOutsideEdit++;
				if (minLetterSimOutsideBaseEdit)
					this.matchMinLetterSimOutsideBaseEdit++;
				if (wim.base.scanned.str.equals(wim.base.rendered.str) && (wim.base.scanned.isItalics == wim.base.rendered.isItalics))
					this.matchOcrMatches++;
				else this.matchOcrMisMatches++;
				this.matchMatchScores.add(new Integer((int) (matchScore * 100)));
				if (Math.min(wim.base.scanned.str.length(), wim.base.scanned.str.length()) > 1)
					this.matchDictHitCounts.add(new Integer(dictHitCount));
				this.matchRenderingSimRelations.add(new Integer((int) (renderingSimRelation * 100)));
				this.matchRenderingCrossSimRelations.add(new Integer((int) (renderingCrossSimRelation * 100)));
				this.matchRenderingSimQuots.add(new Integer((int) ((renderingSimRelation / renderingCrossSimRelation) * 100)));
//				this.matchFontSizeRels.add(new Integer((int) (fontSizeRelation * 100)));
				for (int e = 0; e < baseEdits.size(); e++)
					this.matchEdits.add(((WordStringEdit) baseEdits.get(e)).toString());
				this.matchEditSignatures.add(dictHitCount + "-" + Math.min(wim.base.scanned.str.length(), wim.base.scanned.str.length()) + "-" + baseEditDist + "-" + optBaseEditDist);
				this.matchEditDists.add(new Integer(baseEditDist));
				this.matchOptEditDists.add(new Integer(optBaseEditDist));
				this.matchStemEditDists.add(new Integer(stemBaseEditDist));
//				this.matchEditDists.add(new Integer((int) (relEditDist * 100)));
				this.matchMaxEditLengths.add(new Integer(absMaxEditLength));
				this.matchMaxRelEditLengths.add(new Integer((int) (relMaxEditLength * 100)));
				this.matchBaseEditDists.add(new Integer((int) (relBaseEditDist * 100)));
				this.matchMaxBaseEditLengths.add(new Integer(absMaxBaseEditLength));
				this.matchMaxRelBaseEditLengths.add(new Integer((int) (relMaxBaseEditLength * 100)));
				if (Math.max(wim.base.scanned.str.length(), wim.base.rendered.str.length()) > 3) {
					this.matchEditDists4.add(new Integer((int) (relEditDist * 100)));
					this.matchMaxEditLengths4.add(new Integer(absMaxEditLength));
					this.matchMaxRelEditLengths4.add(new Integer((int) (relMaxEditLength * 100)));
					this.matchBaseEditDists4.add(new Integer((int) (relBaseEditDist * 100)));
					this.matchMaxBaseEditLengths4.add(new Integer(absMaxBaseEditLength));
					this.matchMaxRelBaseEditLengths4.add(new Integer((int) (relMaxBaseEditLength * 100)));
				}
				this.matchBoxPropDists.add(new Integer((int) (Math.abs(wim.base.scanned.boxProportion - wim.base.rendered.boxProportion) * 100)));
				this.matchSims.add(new Integer((int) (wim.base.sim * 100)));
				this.matchMinLetterSims.add(new Integer((int) (wim.minLetterSim * 100)));
				this.matchTopSims.add(new Integer((int) (topSim * 100)));
				this.matchTopMinLetterSims.add(new Integer((int) (topMinLetterSim * 100)));
				this.matchTopSimsBySims.add(new Integer((int) ((topSim / wim.base.sim) * 100)));
				this.matchMaxLetterErrorToError.add(new Integer((wim.base.sim == 1) ? 100 : ((int) (((1 - wim.minLetterSim) / (1 - wim.base.sim)) * 100))));
				this.matchMaxContrasts.add(new Integer((int) (wim.maxSimBucketContrast * 100)));
				this.matchAvgContrasts.add(new Integer((int) (wim.simBucketContrast * 100)));
				this.matchAvgDists.add(new Integer((int) ((wim.scannedOnlyAvgDist + wim.renderedOnlyAvgDist) * 50)));
				this.matchAvgSqrDists.add(new Integer((int) ((wim.scannedOnlyAvgSqrDist + wim.renderedOnlyAvgSqrDist) * 50)));
				this.matchMaxDists.add(new Integer(Math.max(wim.scannedOnlyMaxDist, wim.renderedOnlyMaxDist)));
				this.matchRelMaxDists.add(new Integer((Math.max(wim.scannedOnlyMaxDist, wim.renderedOnlyMaxDist) * 100) / wim.matchedHist.length));
				return (swapOnMerge ? CLUSTER_MATCH_SWAP_MERGE : CLUSTER_MATCH_MERGE);
			}
			else if (v == JOptionPane.NO_OPTION) {
				if (pv == CLUSTER_MATCH_MISMATCH)
					this.misMatchPredictionCorrect++;
//				if (matchScore >= .25)
//					this.goodMisMatches.add(wim);
//				if (optBaseEditDist < 2)
//					this.goodMisMatches.add(wim);
				if ((pv == CLUSTER_MATCH_MERGE) || (pv == CLUSTER_MATCH_SWAP_MERGE))
					this.goodMisMatches.add(wim);
				if (wim.base.sim > clusterMatchStats.avgSim) {
					this.misMatchSimAboveClusterDiameter++;
//					this.goodMisMatches.add(wim);
				}
				if (wim.base.sim > wordMatchStats.avgSim) {
					this.misMatchSimAboveWeightedClusterDiameter++;
//					this.goodMisMatches.add(wim);
				}
				if (wim.base.waSim > clusterMatchStats.avgSim) {
					this.misMatchWaSimAboveClusterDiameter++;
//					this.goodMisMatches.add(wim);
				}
				if (wim.base.waSim > wordMatchStats.avgSim) {
					this.misMatchWaSimAboveWeightedClusterDiameter++;
//					this.goodMisMatches.add(wim);
				}
				if (wim.minLetterSim > clusterMatchStats.avgSim) {
					this.misMatchMinLetterSimAboveClusterDiameter++;
//					this.goodMisMatches.add(wim);
				}
				if (wim.minLetterSim > wordMatchStats.avgSim) {
					this.misMatchMinLetterSimAboveWeightedClusterDiameter++;
//					this.goodMisMatches.add(wim);
				}
				if (wim.minLetterSim > wim.base.sim) {
					this.misMatchMinLetterSimAboveSim++;
//					this.goodMisMatches.add(wim);
				}
				if (wim.minLetterSim > clusterMatchStats.avgMinLetterSim) {
					this.misMatchMinLetterSimAboveAvg++;
//					this.goodMisMatches.add(wim);
				}
				if (wim.minLetterSim > wordMatchStats.avgMinLetterSim) {
					this.misMatchMinLetterSimAboveWeightedAvg++;
//					this.goodMisMatches.add(wim);
				}
				if (minLetterSimOutsideEdit)
					this.misMatchMinLetterSimOutsideEdit++;
				if (minLetterSimOutsideBaseEdit)
					this.misMatchMinLetterSimOutsideBaseEdit++;
				if (wim.base.scanned.str.equals(wim.base.rendered.str) && (wim.base.scanned.isItalics == wim.base.rendered.isItalics))
					this.misMatchOcrMatches++;
				else this.misMatchOcrMisMatches++;
				this.misMatchMatchScores.add(new Integer((int) (matchScore * 100)));
				if (Math.min(wim.base.scanned.str.length(), wim.base.scanned.str.length()) > 1)
					this.misMatchDictHitCounts.add(new Integer(dictHitCount));
				this.misMatchRenderingSimRelations.add(new Integer((int) (renderingSimRelation * 100)));
				this.misMatchRenderingCrossSimRelations.add(new Integer((int) (renderingCrossSimRelation * 100)));
				this.misMatchRenderingSimQuots.add(new Integer((int) ((renderingSimRelation / renderingCrossSimRelation) * 100)));
//				this.misMatchFontSizeRels.add(new Integer((int) (fontSizeRelation * 100)));
				for (int e = 0; e < baseEdits.size(); e++)
					this.misMatchEdits.add(((WordStringEdit) baseEdits.get(e)).toString());
				this.misMatchEditSignatures.add(dictHitCount + "-" + Math.min(wim.base.scanned.str.length(), wim.base.scanned.str.length()) + "-" + baseEditDist + "-" + optBaseEditDist);
				this.misMatchEditDists.add(new Integer(baseEditDist));
				this.misMatchOptEditDists.add(new Integer(optBaseEditDist));
				this.misMatchStemEditDists.add(new Integer(stemBaseEditDist));
//				this.misMatchEditDists.add(new Integer((int) (relEditDist * 100)));
				this.misMatchMaxEditLengths.add(new Integer(absMaxEditLength));
				this.misMatchMaxRelEditLengths.add(new Integer((int) (relMaxEditLength * 100)));
				this.misMatchBaseEditDists.add(new Integer((int) (relBaseEditDist * 100)));
				this.misMatchMaxBaseEditLengths.add(new Integer(absMaxBaseEditLength));
				this.misMatchMaxRelBaseEditLengths.add(new Integer((int) (relMaxBaseEditLength * 100)));
				if (Math.max(wim.base.scanned.str.length(), wim.base.rendered.str.length()) > 3) {
					this.misMatchEditDists4.add(new Integer((int) (relEditDist * 100)));
					this.misMatchMaxEditLengths4.add(new Integer(absMaxEditLength));
					this.misMatchMaxRelEditLengths4.add(new Integer((int) (relMaxEditLength * 100)));
					this.misMatchBaseEditDists4.add(new Integer((int) (relBaseEditDist * 100)));
					this.misMatchMaxBaseEditLengths4.add(new Integer(absMaxBaseEditLength));
					this.misMatchMaxRelBaseEditLengths4.add(new Integer((int) (relMaxBaseEditLength * 100)));
				}
				this.misMatchBoxPropDists.add(new Integer((int) (Math.abs(wim.base.scanned.boxProportion - wim.base.rendered.boxProportion) * 100)));
				this.misMatchSims.add(new Integer((int) (wim.base.sim * 100)));
				this.misMatchMinLetterSims.add(new Integer((int) (wim.minLetterSim * 100)));
				this.misMatchTopSims.add(new Integer((int) (topSim * 100)));
				this.misMatchTopMinLetterSims.add(new Integer((int) (topMinLetterSim * 100)));
				this.misMatchTopSimsBySims.add(new Integer((int) ((topSim / wim.base.sim) * 100)));
				this.misMatchMaxLetterErrorToError.add(new Integer((wim.base.sim == 1) ? 100 : ((int) (((1 - wim.minLetterSim) / (1 - wim.base.sim)) * 100))));
				this.misMatchMaxContrasts.add(new Integer((int) (wim.maxSimBucketContrast * 100)));
				this.misMatchAvgContrasts.add(new Integer((int) (wim.simBucketContrast * 100)));
				this.misMatchAvgDists.add(new Integer((int) ((wim.scannedOnlyAvgDist + wim.renderedOnlyAvgDist) * 50)));
				this.misMatchAvgSqrDists.add(new Integer((int) ((wim.scannedOnlyAvgSqrDist + wim.renderedOnlyAvgSqrDist) * 50)));
				this.misMatchMaxDists.add(new Integer(Math.max(wim.scannedOnlyMaxDist, wim.renderedOnlyMaxDist)));
				this.misMatchRelMaxDists.add(new Integer((Math.max(wim.scannedOnlyMaxDist, wim.renderedOnlyMaxDist) * 100) / wim.matchedHist.length));
				return CLUSTER_MATCH_MISMATCH;
			}
			else return CLUSTER_MATCH_GROUP;
		}
	}
	
	private static BoundingBox getBoundingBox(ImagePartRectangle rect) {
		return new BoundingBox(rect.getLeftCol(), rect.getRightCol(), rect.getTopRow(), rect.getBottomRow());
	}
	
	private static class WordStringEdit {
		final String replaced;
		final int sPos;
		final String replacement;
		final int rPos;
		WordStringEdit(String replaced, int sPos, String replacement, int rPos) {
			this.replaced = replaced;
			this.sPos = sPos;
			this.replacement = replacement;
			this.rPos = rPos;
		}
		public String toString() {
			return (this.replaced + " -> " + this.replacement);
		}
		int getStemDiff() {
			return Math.abs(countStems(this.replaced) - countStems(this.replacement));
		}
		private static int countStems(String str) {
			int stems = 0;
			for (int c = 0; c < str.length(); c++) {
				char ch = str.charAt(c);
				if (128 < ch)
					stems += countStems(StringUtils.getNormalForm(ch));
				else if ("mw".indexOf(ch) != -1)
					stems += 3;
				else if ("abdghknopquvy".indexOf(ch) != -1)
					stems += 2;
				else stems++;
			}
			return stems;
		}
		boolean involvesVowel(boolean countI) {
			for (int c = 0; c < this.replaced.length(); c++) {
				if ("AEOUaeou".indexOf(this.replaced.charAt(c)) != -1)
					return true;
				else if (countI && "Ii".indexOf(this.replaced.charAt(c)) != -1)
					return true;
			}
			for (int c = 0; c < this.replacement.length(); c++) {
				if ("AEOUaeou".indexOf(this.replacement.charAt(c)) != -1)
					return true;
				else if (countI && "Ii".indexOf(this.replacement.charAt(c)) != -1)
					return true;
			}
			return false;
		}
		boolean isDeterioration() {
			return (("" +
					" b-h" +
					" e-c" +
					" f-i f-l f-t" +
					" h-n" +
					" k-x" +
					" l-i" +
					" o-c" +
					" p-y" +
					" t-r" +
					" y-v" +
					" ").indexOf(" " + this.replaced + "-" + this.replacement + " ") != -1);
		}
		boolean isOpticalyClose() {
			return (("" +
					" an-cm" +
					" b-h" +
					" c-e c-o" +
					" e-c" +
					" f-t" +
					" ii-n ii-u im-nn im-un in-m" +
					" m-in m-ni m-rn" +
					" n-ii n-ri n-rz nn-rm" +
					" o-c o-e" +
					" ra-m rm-nn rn-m" +
					" t-i t-r tt-rr" +
					" u-ii u-n un-im un-mi un-rm un-tm un-zm" +
					" ").indexOf(" " + this.replaced + "-" + this.replacement + " ") != -1);
		}
	}
	
	private int matchPredictionCount = 0;
	private int matchPredictionCorrect = 0;
	private int misMatchPredictionCount = 0;
	private int misMatchPredictionCorrect = 0;
	
	private int matchSimAboveClusterDiameter = 0;
	private int misMatchSimAboveClusterDiameter = 0;
	private int matchSimAboveWeightedClusterDiameter = 0;
	private int misMatchSimAboveWeightedClusterDiameter = 0;
	private int matchWaSimAboveClusterDiameter = 0;
	private int misMatchWaSimAboveClusterDiameter = 0;
	private int matchWaSimAboveWeightedClusterDiameter = 0;
	private int misMatchWaSimAboveWeightedClusterDiameter = 0;
	private int matchMinLetterSimAboveClusterDiameter = 0;
	private int misMatchMinLetterSimAboveClusterDiameter = 0;
	private int matchMinLetterSimAboveWeightedClusterDiameter = 0;
	private int misMatchMinLetterSimAboveWeightedClusterDiameter = 0;
	private int matchMinLetterSimAboveSim = 0;
	private int misMatchMinLetterSimAboveSim = 0;
	private int matchMinLetterSimAboveAvg = 0;
	private int misMatchMinLetterSimAboveAvg = 0;
	private int matchMinLetterSimAboveWeightedAvg = 0;
	private int misMatchMinLetterSimAboveWeightedAvg = 0;
	private int matchMinLetterSimOutsideEdit = 0;
	private int misMatchMinLetterSimOutsideEdit = 0;
	private int matchMinLetterSimOutsideBaseEdit = 0;
	private int misMatchMinLetterSimOutsideBaseEdit = 0;
	private int matchOcrMatches = 0;
	private int misMatchOcrMatches = 0;
	private int matchOcrMisMatches = 0;
	private int misMatchOcrMisMatches = 0;
	private LinkedHashSet goodMisMatches = new LinkedHashSet();
	
	private CountingSet matchDictHitCounts = new CountingSet(new TreeMap());
	private CountingSet misMatchDictHitCounts = new CountingSet(new TreeMap());
	private CountingSet matchMatchScores = new CountingSet(new TreeMap());
	private CountingSet misMatchMatchScores = new CountingSet(new TreeMap());
	private CountingSet matchRenderingSimRelations = new CountingSet(new TreeMap());
	private CountingSet misMatchRenderingSimRelations = new CountingSet(new TreeMap());
	private CountingSet matchRenderingCrossSimRelations = new CountingSet(new TreeMap());
	private CountingSet misMatchRenderingCrossSimRelations = new CountingSet(new TreeMap());
	private CountingSet matchRenderingSimQuots = new CountingSet(new TreeMap());
	private CountingSet misMatchRenderingSimQuots = new CountingSet(new TreeMap());
	
	private CountingSet matchFontSizeRels = new CountingSet(new TreeMap());
	private CountingSet misMatchFontSizeRels = new CountingSet(new TreeMap());
	
	private CountingSet matchEdits = new CountingSet(new TreeMap());
	private CountingSet misMatchEdits = new CountingSet(new TreeMap());
	private CountingSet matchEditSignatures = new CountingSet(new TreeMap());
	private CountingSet misMatchEditSignatures = new CountingSet(new TreeMap());
	
	private CountingSet matchEditDists = new CountingSet(new TreeMap());
	private CountingSet misMatchEditDists = new CountingSet(new TreeMap());
	private CountingSet matchOptEditDists = new CountingSet(new TreeMap());
	private CountingSet misMatchOptEditDists = new CountingSet(new TreeMap());
	private CountingSet matchStemEditDists = new CountingSet(new TreeMap());
	private CountingSet misMatchStemEditDists = new CountingSet(new TreeMap());
	private CountingSet matchMaxEditLengths = new CountingSet(new TreeMap());
	private CountingSet misMatchMaxEditLengths = new CountingSet(new TreeMap());
	private CountingSet matchMaxRelEditLengths = new CountingSet(new TreeMap());
	private CountingSet misMatchMaxRelEditLengths = new CountingSet(new TreeMap());
	private CountingSet matchEditDists4 = new CountingSet(new TreeMap());
	private CountingSet misMatchEditDists4 = new CountingSet(new TreeMap());
	private CountingSet matchMaxEditLengths4 = new CountingSet(new TreeMap());
	private CountingSet misMatchMaxEditLengths4 = new CountingSet(new TreeMap());
	private CountingSet matchMaxRelEditLengths4 = new CountingSet(new TreeMap());
	private CountingSet misMatchMaxRelEditLengths4 = new CountingSet(new TreeMap());
	
	private CountingSet matchBaseEditDists = new CountingSet(new TreeMap());
	private CountingSet misMatchBaseEditDists = new CountingSet(new TreeMap());
	private CountingSet matchMaxBaseEditLengths = new CountingSet(new TreeMap());
	private CountingSet misMatchMaxBaseEditLengths = new CountingSet(new TreeMap());
	private CountingSet matchMaxRelBaseEditLengths = new CountingSet(new TreeMap());
	private CountingSet misMatchMaxRelBaseEditLengths = new CountingSet(new TreeMap());
	private CountingSet matchBaseEditDists4 = new CountingSet(new TreeMap());
	private CountingSet misMatchBaseEditDists4 = new CountingSet(new TreeMap());
	private CountingSet matchMaxBaseEditLengths4 = new CountingSet(new TreeMap());
	private CountingSet misMatchMaxBaseEditLengths4 = new CountingSet(new TreeMap());
	private CountingSet matchMaxRelBaseEditLengths4 = new CountingSet(new TreeMap());
	private CountingSet misMatchMaxRelBaseEditLengths4 = new CountingSet(new TreeMap());
	
	private CountingSet matchBoxPropDists = new CountingSet(new TreeMap());
	private CountingSet misMatchBoxPropDists = new CountingSet(new TreeMap());
	private CountingSet matchSims = new CountingSet(new TreeMap());
	private CountingSet misMatchSims = new CountingSet(new TreeMap());
	private CountingSet matchMinLetterSims = new CountingSet(new TreeMap());
	private CountingSet misMatchMinLetterSims = new CountingSet(new TreeMap());
	private CountingSet matchTopSims = new CountingSet(new TreeMap());
	private CountingSet misMatchTopSims = new CountingSet(new TreeMap());
	private CountingSet matchTopMinLetterSims = new CountingSet(new TreeMap());
	private CountingSet misMatchTopMinLetterSims = new CountingSet(new TreeMap());
	private CountingSet matchTopSimsBySims = new CountingSet(new TreeMap());
	private CountingSet misMatchTopSimsBySims = new CountingSet(new TreeMap());
	private CountingSet matchMaxLetterErrorToError = new CountingSet(new TreeMap());
	private CountingSet misMatchMaxLetterErrorToError = new CountingSet(new TreeMap());
	
	private CountingSet matchMaxContrasts = new CountingSet(new TreeMap());
	private CountingSet misMatchMaxContrasts = new CountingSet(new TreeMap());
	private CountingSet matchAvgContrasts = new CountingSet(new TreeMap());
	private CountingSet misMatchAvgContrasts = new CountingSet(new TreeMap());
	private CountingSet matchAvgDists = new CountingSet(new TreeMap());
	private CountingSet misMatchAvgDists = new CountingSet(new TreeMap());
	private CountingSet matchAvgSqrDists = new CountingSet(new TreeMap());
	private CountingSet misMatchAvgSqrDists = new CountingSet(new TreeMap());
	private CountingSet matchMaxDists = new CountingSet(new TreeMap());
	private CountingSet misMatchMaxDists = new CountingSet(new TreeMap());
	private CountingSet matchRelMaxDists = new CountingSet(new TreeMap());
	private CountingSet misMatchRelMaxDists = new CountingSet(new TreeMap());
	
	private static void addWordToCluster(WordImage wi, Map wordClusterIndex, List wordClusters) {
		
		//	get existing clusters
		String wcKey = getClusterKey(wi.word);
		ArrayList wcList = ((ArrayList) wordClusterIndex.get(wcKey));
		WordCluster wc = null;
		
		//	no clusters at so far for this key
		if (wcList == null) {
			wcList = new ArrayList(1);
			wordClusterIndex.put(wcKey, wcList);
		}
		
		//	try and find matching cluster among existing ones
		else for (int c = 0; c < wcList.size(); c++) {
			WordCluster twc = ((WordCluster) wcList.get(c));
			
			//	don't even try to conflate styles
			if (!twc.isStyleCompatible(wi))
				continue;
			
			//	check basics
			if (Math.abs(wi.boxProportion - twc.avgBoxProportion) > 0.15f)
				continue;
			if ((Math.abs(wi.box.getWidth() - twc.avgBoxWidth) * 10) > Math.max(wi.box.getWidth(), twc.avgBoxWidth))
				continue;
			if ((Math.abs(wi.box.getHeight() - twc.avgBoxHeigth) * 5) > Math.max(wi.box.getHeight(), twc.avgBoxHeigth))
				continue;
			
			//	this one looks OK (we've already checked OCR via the map lookup)
			wc = twc;
			break;
		}
		
		//	found a cluster for this one?
		if (wc == null) {
			System.out.println(" ==> started new cluster '" + wi.word.getString() + "' with key " + wcKey);
			wc = new WordCluster(wi, wi.word);
			wcList.add(wc);
			wordClusters.add(wc);
		}
		else {
			System.out.println(" ==> assigned to cluster '" + wc.name + "' with key " + wcKey);
			wc.addWordImage(wi);
		}
	}
	
	private static class WordCluster {
		final String name;
		final int fontSize;
		final boolean isBold;
		final boolean isItalics;
		
		WordImage representative;
		ArrayList wordImages = new ArrayList(2);
		ArrayList wordImageMatches = new ArrayList(2);
		
		double boxProportionSum = 0;
		double avgBoxProportion = 0;
		int boxWidthSum = 0;
		int avgBoxWidth = 0;
		int boxHeightSum = 0;
		int avgBoxHeigth = 0;
		
		int wordPixelCountSum = 0;
		int avgWordPixelCount = 0;
		int wordAreaSum = 0;
		float avgWordWeight = 0;
		
		float avgWordImageSim = 1;
		float minWordImageSim = 1;
		
		WordCluster(WordImage wi, ImWord imw) {
			this.addWordImage(wi);
			this.name = imw.getString();
			this.fontSize = Integer.parseInt((String) imw.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE, "-1"));
			this.isBold = imw.hasAttribute(ImWord.BOLD_ATTRIBUTE);
			this.isItalics = imw.hasAttribute(ImWord.ITALICS_ATTRIBUTE);
		}
		
		void addWordImage(WordImage wi) {
			this.wordImages.add(wi);
			this.wordImageMatches.clear();
			this.boxProportionSum += wi.boxProportion;
			this.boxWidthSum += wi.box.getWidth();
			this.boxHeightSum += wi.box.getHeight();
			this.avgBoxProportion = (this.boxProportionSum / this.wordImages.size());
			this.avgBoxWidth = ((this.boxWidthSum + (this.wordImages.size() / 2)) / this.wordImages.size());
			this.avgBoxHeigth = ((this.boxHeightSum + (this.wordImages.size() / 2)) / this.wordImages.size());
			this.wordPixelCountSum += wi.pixelCount;
			this.avgWordPixelCount = ((this.wordPixelCountSum + (this.wordImages.size() / 2)) / this.wordImages.size());
			this.wordAreaSum += (wi.box.getWidth() * wi.box.getHeight());
			this.avgWordWeight = (((float) this.wordPixelCountSum) / this.wordAreaSum);
			this.representative = ((WordImage) this.wordImages.get(0));
		}
		
		WordImage removeWordImage(int index) {
			WordImage wi = ((WordImage) this.wordImages.remove(index));
			this.wordImageMatches.clear();
			this.boxProportionSum -= wi.boxProportion;
			this.boxWidthSum -= wi.box.getWidth();
			this.boxHeightSum -= wi.box.getHeight();
			this.avgBoxProportion = (this.boxProportionSum / this.wordImages.size());
			this.avgBoxWidth = ((this.boxWidthSum + (this.wordImages.size() / 2)) / this.wordImages.size());
			this.avgBoxHeigth = ((this.boxHeightSum + (this.wordImages.size() / 2)) / this.wordImages.size());
			this.wordPixelCountSum -= wi.pixelCount;
			this.avgWordPixelCount = ((this.wordPixelCountSum + (this.wordImages.size() / 2)) / this.wordImages.size());
			this.wordAreaSum -= (wi.box.getWidth() * wi.box.getHeight());
			this.avgWordWeight = (((float) this.wordPixelCountSum) / this.wordAreaSum);
			this.representative = ((WordImage) this.wordImages.get(0));
			return wi;
		}
		
		synchronized void compileRepresentative() {
			this.compileRepresentative(false);
		}
		synchronized void compileRepresentative(boolean explain) {
			if (!explain && ((this.wordImages.size() < 2) || (this.representative != this.wordImages.get(0))))
				return;
			
			//	clear up ordered images, as this has to be re-done after change to representative
			this.wordImageMatches.clear();
			
			//	order word images by width (so narrower ones can shift to avoid non-OCRed punctuation appendages)
			ArrayList wordImages = new ArrayList(this.wordImages);
			Collections.sort(wordImages, new Comparator() {
				public int compare(Object obj1, Object obj2) {
					WordImage wi1 = ((WordImage) obj1);
					WordImage wi2 = ((WordImage) obj2);
					return (wi2.box.getWidth() - wi1.box.getWidth());
				}
			});
			
			//	compute maximum width and height
			int rWidth = 0;
			int rHeight = 0;
			int rDpi = 0;
			int[] rWidthWordColHist = null;
			int[] rHeightWordRowHist = null;
			for (int i = 0; i < wordImages.size(); i++) {
				WordImage wi = ((WordImage) wordImages.get(i));
				if (rWidth < wi.box.getWidth()) {
					rWidth = wi.box.getWidth();
					rWidthWordColHist = wi.colBrightnessHist;
				}
				if (rHeight < wi.box.getHeight()) {
					rHeight = wi.box.getHeight();
					rHeightWordRowHist = wi.rowBrightnessHist;
				}
				rDpi = Math.max(rDpi, wi.pageImageDpi);
			}
			
			//	copy histogram
			int[] rColHist = new int[rWidthWordColHist.length];
			System.arraycopy(rWidthWordColHist, 0, rColHist, 0, rColHist.length);
			int[] rRowHist = new int[rHeightWordRowHist.length];
			System.arraycopy(rHeightWordRowHist, 0, rRowHist, 0, rRowHist.length);
			
			//	compute bounds of representative (might have white edges as well ...)
			int rLeft = -1;
			int rRight = 0;
			for (int c = 0; c < rWidthWordColHist.length; c++)
				if (rWidthWordColHist[c] != 0) {
					if (rLeft == -1)
						rLeft = c;
					rRight = (c+1);
				}
			int rTop = -1;
			int rBottom = 0;
			for (int r = 0; r < rHeightWordRowHist.length; r++)
				if (rHeightWordRowHist[r] != 0) {
					if (rTop == -1)
						rTop = r;
					rBottom = (r+1);
				}
			
			//	do majority vote between all word images zoomed to these dimensions
			int[][] rPixels = new int[rWidth][rHeight];
			for (int c = 0; c < rPixels.length; c++)
				Arrays.fill(rPixels[c], 0);
			for (int i = 0; i < wordImages.size(); i++) {
				WordImage wi = ((WordImage) wordImages.get(i));
				int[] colShifts = WordImageAnalysis.getHistogramAlignmentShifts(rColHist, wi.colBrightnessHist, (rWidth / 10));
				int leftShift = Math.max(colShifts[0], 0); // only shift current word
				int rightShift = Math.max(colShifts[1], 0); // only shift current word
				int[] rowShifts = WordImageAnalysis.getHistogramAlignmentShifts(rRowHist, wi.rowBrightnessHist, (rHeight / 10));
				int topShift = Math.max(rowShifts[0], 0); // only shift current word
				int bottomShift = Math.max(rowShifts[1], 0); // only shift current word
				int wLeft = wi.box.getLeftCol();
				int wRight = wi.box.getRightCol();
				int wTop = wi.box.getTopRow();
				int wBottom = wi.box.getBottomRow();
				int wWidth = (wRight - wLeft);
				int wHeight = wi.box.getHeight();
				if (explain) {
					System.out.println("Adding word image sized " + wWidth + "x" + wHeight + ":");
					System.out.println(" - rendering offsets are: left " + wLeft + ", top " + wTop);
					System.out.println(" - reColHist: " + Arrays.toString(rColHist));
					System.out.println(" - wiColHist: " + Arrays.toString(wi.colBrightnessHist));
					System.out.println(" - reRowHist: " + Arrays.toString(rRowHist));
					System.out.println(" - wiRowHist: " + Arrays.toString(wi.rowBrightnessHist));
					System.out.println(" - rendering shifts are: left " + leftShift + ", right " + rightShift + ", top " + topShift + ", bottom " + bottomShift);
					WordImage rWi = wi;
					if (i != 0) {
						int rBlackThreshold = ((int) Math.floor(Math.sqrt(i)));
						BufferedImage rBi = this.buildRepresentative(rPixels, rBlackThreshold);
						rWi = new WordImage(rBi, this.name, this.isItalics, rDpi);
					}
					ExtendedWordImageMatch rWim = matchWordImages(rWi, wi, true, true, 1);
					System.out.println(" - match shifts are: left " + rWim.base.leftShift + ", right " + rWim.base.rightShift + ", top " + rWim.base.topShift + ", bottom " + rWim.base.bottomShift);
					rWim.computeMatchHistograms();
					displayWordMatch(rWim, "Adding word image");
				}
				for (int rCol = 0; rCol < rWidth; rCol++) {
//					int wCol = (wLeft - leftShift + ((rCol * (wWidth + leftShift + rightShift)) / rWidth));
					int wCol = (wLeft - ((i == 0) ? 0 : rLeft) - leftShift + ((rCol * (wWidth + leftShift + rightShift)) / (rRight - rLeft)));
					if (wCol < 0)
						continue;
					if (wRight <= wCol)
						break;
					for (int rRow = 0; rRow < rHeight; rRow++) {
//						int wRow = (wTop - topShift + ((rRow * (wHeight + topShift + bottomShift)) / rHeight));
						int wRow = (wTop - ((i == 0) ? 0 : rTop) - topShift + ((rRow * (wHeight + topShift + bottomShift)) / (rBottom - rTop)));
						if (wRow < 0)
							continue;
						if (wBottom <= wRow)
							break;
						if (wi.brightness[wCol][wRow] < 96)
							rPixels[rCol][rRow]++;
//						if (rCol == 0)
//							rRowHist[rRow] = Math.max(rRowHist[rRow], wi.rowBrightnessHist[wRow]);
					}
//					rColHist[rCol] = Math.max(rColHist[rCol], wi.colBrightnessHist[wCol]);
				}
				
				//	re-compute representative histograms
				int rBlackThreshold = ((int) Math.floor(Math.sqrt(i+1)));
				Arrays.fill(rColHist, 0);
				Arrays.fill(rRowHist, 0);
				for (int rCol = 0; rCol < rWidth; rCol++) {
					for (int rRow = 0; rRow < rHeight; rRow++)
						if (rBlackThreshold <= rPixels[rCol][rRow]) {
							rColHist[rCol]++;
							rRowHist[rRow]++;
						}
				}
				
				//	re-compute bounds of representative (might have white edges as well ...)
				rLeft = -1;
				rRight = 0;
				for (int c = 0; c < rColHist.length; c++)
					if (rColHist[c] != 0) {
						if (rLeft == -1)
							rLeft = c;
						rRight = (c+1);
					}
				rTop = -1;
				rBottom = 0;
				for (int r = 0; r < rRowHist.length; r++)
					if (rRowHist[r] != 0) {
						if (rTop == -1)
							rTop = r;
						rBottom = (r+1);
					}
			}
			if (explain) {
				int rBlackThreshold = ((int) Math.floor(Math.sqrt(wordImages.size())));
				BufferedImage rBi = this.buildRepresentative(rPixels, rBlackThreshold);
				WordImage rWi = new WordImage(rBi, this.name, this.isItalics, rDpi);
				ExtendedWordImageMatch rWim = matchWordImages(rWi, rWi, true, true, 1);
				rWim.computeMatchHistograms();
				displayWordMatch(rWim, "Representative generated");
			}
			
			//	create representative from overlay result
			int rBlackThreshold = ((int) Math.floor(Math.sqrt(wordImages.size())));
			BufferedImage rBi = this.buildRepresentative(rPixels, rBlackThreshold);
			
			//	set representative
			this.representative = new WordImage(rBi, this.name, this.isItalics, rDpi);
		}
		private BufferedImage buildRepresentative(int[][] rPixels, int rBlackThreshold) {
			BufferedImage rBi = new BufferedImage(rPixels.length, rPixels[0].length, this.representative.img.getType());
			Graphics rGr = rBi.createGraphics();
			rGr.setColor(Color.WHITE);
			rGr.fillRect(0, 0, rPixels.length, rPixels[0].length);
			for (int c = 0; c < rPixels.length; c++)
				for (int r = 0; r < rPixels[c].length; r++) {
					if (rBlackThreshold <= rPixels[c][r])
						rBi.setRGB(c, r, blackRgb);
				}
			return rBi;
		}
		
		synchronized void orderWordImages() {
			if (this.wordImageMatches.size() != 0)
				return;
			
			//	make sure we have an up-to-date representative
			this.compileRepresentative();
			
			//	create list of similarities for all word images
			for (int w = 0; w < this.wordImages.size(); w++)
				this.wordImageMatches.add(matchWordImages(((WordImage) this.wordImages.get(w)), this.representative, true, true, 1));
			
			//	order word images by similarities
			Collections.sort(this.wordImageMatches, new Comparator() {
				public int compare(Object obj1, Object obj2) {
					ExtendedWordImageMatch ewim1 = ((ExtendedWordImageMatch) obj1);
					ExtendedWordImageMatch ewim2 = ((ExtendedWordImageMatch) obj2);
					return Float.compare(ewim2.base.sim, ewim1.base.sim);
				}
			});
			
			//	store ordered word images
			for (int w = 0; w < this.wordImageMatches.size(); w++)
				this.wordImages.set(w, ((ExtendedWordImageMatch) this.wordImageMatches.get(w)).base.scanned);
		}
		
		synchronized void computeInternalSimilarity() {
			
			//	make sure we have an up-to-date representative
			this.compileRepresentative();
			
			//	reset minimum
			this.minWordImageSim = 1;
			
			//	assess word images
			int clusterWordSimCount = 0;
			double clusterWordSimSum = 0;
			for (int w = 0; w < this.wordImages.size(); w++) {
				WordImage wi = ((WordImage) this.wordImages.get(w));
				ExtendedWordImageMatch wim = matchWordImages(wi, this.representative, true, true, 1);
				this.minWordImageSim = Math.min(this.minWordImageSim, wim.base.sim);
				clusterWordSimCount++;
				clusterWordSimSum += wim.base.sim;
			}
			this.avgWordImageSim = ((float) ((clusterWordSimCount == 0) ? 0 : (clusterWordSimSum / clusterWordSimCount)));
		}
		
		boolean isStyleCompatible(WordImage wi) {
			return (("" + this.fontSize).equals(wi.word.getAttribute(ImWord.FONT_SIZE_ATTRIBUTE, "-1")) && (this.isBold == wi.word.hasAttribute(ImWord.BOLD_ATTRIBUTE)) && (this.isItalics == wi.word.hasAttribute(ImWord.ITALICS_ATTRIBUTE)));
		}
		
		boolean areStylesCompatible(WordCluster wc) {
			return this.areStylesCompatible(wc, 0);
		}
		boolean areStylesCompatible(WordCluster wc, int maxFontSizeDiff) {
			return ((Math.abs(this.fontSize - wc.fontSize) <= maxFontSizeDiff) && (this.isBold == wc.isBold) && (this.isItalics == wc.isItalics));
		}
	}
	
	private static class WordClusterSet {
		final String name;
		ArrayList wordClusters = new ArrayList(2);
		int wordCount = 0;
		WordClusterSet(WordCluster wc) {
			this.name = wc.name;
			this.addWordCluster(wc);
		}
		void addWordCluster(WordCluster wc) {
			this.wordClusters.add(wc);
			this.wordCount += wc.wordImages.size();
		}
		void cleanup() {
			this.wordCount = 0;
			for (int c = 0; c < this.wordClusters.size(); c++) {
				WordCluster wc = ((WordCluster) this.wordClusters.get(c));
				if (wc.wordImages.isEmpty())
					this.wordClusters.remove(c--);
				else this.wordCount += wc.wordImages.size();
			}
		}
	}
	
	private static class WordClusterMatch {
		WordCluster swc;
		WordCluster rwc;
		ExtendedWordImageMatch wim;
		boolean isGrouping;
		WordClusterMatch(WordCluster swc, WordCluster rwc, ExtendedWordImageMatch wim, boolean isGrouping) {
			this.swc = swc;
			this.rwc = rwc;
			this.wim = wim;
			this.isGrouping = isGrouping;
		}
	}
	
	//	use Levenshtein edit sequence to extract letter replacements, and add them to counting set
	private static void recordLetterReplacements(String oldStr, String newStr, CountingSet letterReplacements) {
		WordStringEdit[] wses = getEdits(oldStr, newStr);
		synchronized (letterReplacements) {
			for (int e = 0; e < wses.length; e++)
				letterReplacements.add(wses[e].replaced + " " + wses[e].replacement);
		}
	}
	
	private static WordStringEdit[] getEdits(String oldStr, String newStr) {
		int[] editSequence = StringUtils.getLevenshteinEditSequence(oldStr, newStr, true);
		int sIndex = 0;
		int rIndex = 0;
		StringBuffer replaced = null;
		StringBuffer replacement = null;
		ArrayList edits = new ArrayList(3);
		for (int e = 0; e < editSequence.length; e++) {
			if (editSequence[e] == StringUtils.LEVENSHTEIN_KEEP) {
				if ((replaced != null) && (replacement != null))
					edits.add(new WordStringEdit(replaced.toString(), (sIndex - replaced.length()), replacement.toString(), (rIndex - replacement.length())));
				sIndex++;
				rIndex++;
				replaced = null;
				replacement = null;
				continue;
			}
			if ((replaced == null) && (replacement == null)) {
				replaced = new StringBuffer();
				replacement = new StringBuffer();
			}
			if (editSequence[e] == StringUtils.LEVENSHTEIN_REPLACE) {
				replaced.append(oldStr.charAt(sIndex++));
				replacement.append(newStr.charAt(rIndex++));
			}
			else if (editSequence[e] == StringUtils.LEVENSHTEIN_DELETE)
				replaced.append(oldStr.charAt(sIndex++));
			else if (editSequence[e] == StringUtils.LEVENSHTEIN_INSERT)
				replacement.append(newStr.charAt(rIndex++));
		}
		if ((replaced != null) && (replacement != null))
			edits.add(new WordStringEdit(replaced.toString(), (sIndex - replaced.length()), replacement.toString(), (rIndex - replacement.length())));
		return ((WordStringEdit[]) edits.toArray(new WordStringEdit[edits.size()]));
	}
	
	private static class WordImageMatchStats {
		double avgBoxProp = 0;
		double avgSim = 0;
		double avgWaSim = 0;
		double avgMinLetterSim = 0;
		double avgMaxContrast = 0;
		double avgMaxDeviation = 0;
		double avgAvgContrast = 0;
		double avgAvgDeviation = 0;
		double avgAvgSqrContrast = 0;
		double avgAvgSqrDeviation = 0;
		double avgAvgDistance = 0;
		double avgAvgSqrDistance = 0;
		double avgMaxDistance = 0;
		double avgRelMaxDistance = 0;
		WordImageMatchStats() {}
	}
	
	private static class ExtendedWordImageMatch implements Comparable {
		final WordImageMatch base;
		
		ExtendedWordImageMatch(WordImageMatch wim) {
			this.base = wim;
		}
		
		public int compareTo(Object obj) {
			return this.base.compareTo(((ExtendedWordImageMatch) obj).base);
		}
		
		int[] matchedHist = null;
		int[] scannedOnlyHist = null;
		int[] renderedOnlyHist = null;
		void computeMatchHistograms() {
			this.computeMatchHistograms(Math.max(2, Math.max((this.base.scanned.pageImageDpi / 75), (this.base.rendered.pageImageDpi / 75))));
		}
		void computeMatchHistograms(int smoothRadius) {
			if (this.matchedHist != null)
				return;
			int[] matchedHist = new int[this.base.matchData.length];
			Arrays.fill(matchedHist, 0);
			int[] scannedOnlyHist = new int[this.base.matchData.length];
			Arrays.fill(scannedOnlyHist, 0);
			int[] renderedOnlyHist = new int[this.base.matchData.length];
			Arrays.fill(renderedOnlyHist, 0);
			for (int c = 0; c < this.base.matchData.length; c++)
				for (int r = 0; r < this.base.matchData[c].length; r++) {
					if (this.base.matchData[c][r] == WordImageAnalysis.WORD_IMAGE_MATCH_MATCHED)
						matchedHist[c]++;
					else if (this.base.matchData[c][r] == WordImageAnalysis.WORD_IMAGE_MATCH_SCANNED_ONLY)
						scannedOnlyHist[c]++;
					else if (this.base.matchData[c][r] == WordImageAnalysis.WORD_IMAGE_MATCH_RENDERED_ONLY)
						renderedOnlyHist[c]++;
				}
			this.matchedHist = ((smoothRadius < 1) ? matchedHist : smoothHist(matchedHist, smoothRadius));
			this.scannedOnlyHist = ((smoothRadius < 1) ? scannedOnlyHist : smoothHist(scannedOnlyHist, smoothRadius));
			this.renderedOnlyHist = ((smoothRadius < 1) ? renderedOnlyHist : smoothHist(renderedOnlyHist, smoothRadius));
		}
		
		/* TODO Try and use similarity buckets on single-letter words:
		 * - 'E' and 'B' are very similar in left half, but less so in right half
		 * - as are 'F' and 'R' ...
		 * ==> bucketize down to _twice_ number of OCR letters
		 * ==> might add more accuracy to assessMatch()
		 */
		
		int maxContrastSimBuckets = -1;
		float maxSimBucketContrast = 0;
		int maxDeviationSimBuckets = -1;
		float maxSimBucketDeviation = 0;
		float simBucketContrast = -1;
		float simBucketContrastSqr = -1;
		float simBucketDeviation = -1;
		float simBucketDeviationSqr = -1;
		float minLetterSim = 1;
		float[] minLetterSimBuckets = null;
		int minLetterSimBucket = -1;
		void computeMatchHistogramMetrics() {
			int minHistAggregateBuckets = 2;
			int maxHistAggregateBuckets = Math.max(this.base.scanned.str.length(), this.base.rendered.str.length());
			this.computeMatchHistogramMetrics(minHistAggregateBuckets, maxHistAggregateBuckets);
		}
		void computeMatchHistogramMetrics(int minBuckets, int maxBuckets) {
			if ((this.maxContrastSimBuckets != -1) || (this.maxDeviationSimBuckets != -1))
				return;
			this.computeMatchHistograms();
			float simBucketContrastSum = 0;
			float simBucketContrastSqrSum = 0;
			float simBucketDeviationSum = 0;
			float simBucketDeviationSqrSum = 0;
			for (int nb = minBuckets; nb <= maxBuckets; nb++) {
				float[] simBuckets = getSimBuckets(this, nb);
				float simBucketContrast = getSimBucketContrast(simBuckets);
				simBucketContrastSum += simBucketContrast;
				simBucketContrastSqrSum += (simBucketContrast * simBucketContrast);
				if (this.maxSimBucketContrast < simBucketContrast) {
					this.maxSimBucketContrast = simBucketContrast;
					this.maxContrastSimBuckets = nb;
				}
				float simBucketDeviation = getSimBucketDeviation(simBuckets, this.base.sim);
				simBucketDeviationSum += simBucketDeviation;
				simBucketDeviationSqrSum += (simBucketDeviation * simBucketDeviation);
				if (this.maxSimBucketDeviation < simBucketDeviation) {
					this.maxSimBucketDeviation = simBucketDeviation;
					this.maxDeviationSimBuckets = nb;
				}
				for (int b = 0; b < simBuckets.length; b++)
					if (simBuckets[b] < this.minLetterSim) {
						this.minLetterSim = simBuckets[b];
						this.minLetterSimBuckets = simBuckets;
						this.minLetterSimBucket = b;
					}
//				System.out.println(" - " + nb + "-split similarity is " + Arrays.toString(simBuckets) + " with contrast " + simBucketContrast + " and deviation " + simBucketDeviation);
			}
			this.simBucketContrast = (simBucketContrastSum / (maxBuckets - minBuckets + 1));
			this.simBucketContrastSqr = ((float) Math.sqrt(simBucketContrastSqrSum / (maxBuckets - minBuckets + 1)));
			this.simBucketDeviation = (simBucketDeviationSum / (maxBuckets - minBuckets + 1));
			this.simBucketDeviationSqr = ((float) Math.sqrt(simBucketDeviationSqrSum / (maxBuckets - minBuckets + 1)));
		}
		
		int matchedSurface = -1;
		int scannedOnlySurface = -1;
		int renderedOnlySurface = -1;
		void computeMatchAreaSurfaces() {
			if (this.matchedSurface >= 0)
				return;
			this.matchedSurface = getSurface(this.base.matchData, WordImageAnalysis.WORD_IMAGE_MATCH_MATCHED);
			this.scannedOnlySurface = getSurface(this.base.matchData, WordImageAnalysis.WORD_IMAGE_MATCH_SCANNED_ONLY);
			this.renderedOnlySurface = getSurface(this.base.matchData, WordImageAnalysis.WORD_IMAGE_MATCH_RENDERED_ONLY);
		}
		
		float scannedOnlyAvgDist = -1;
		float scannedOnlyAvgSqrDist = -1;
		int scannedOnlyMaxDist = -1;
		float renderedOnlyAvgDist = -1;
		float renderedOnlyAvgSqrDist = -1;
		int renderedOnlyMaxDist = -1;
		void computeMisMatchDistances() {
			if ((this.scannedOnlyMaxDist + this.renderedOnlyMaxDist) >= 0)
				return;
			byte[][] sDistData = new byte[this.base.matchData.length][this.base.matchData[0].length];
			byte[][] rDistData = new byte[this.base.matchData.length][this.base.matchData[0].length];
			for (int c = 0; c < this.base.matchData.length; c++)
				for (int r = 0; r < this.base.matchData[c].length; r++) {
					sDistData[c][r] = ((byte) (((this.base.matchData[c][r] == WordImageAnalysis.WORD_IMAGE_MATCH_MATCHED) || (this.base.matchData[c][r] == WordImageAnalysis.WORD_IMAGE_MATCH_SCANNED_ONLY)) ? 1 : 0));
					rDistData[c][r] = ((byte) (((this.base.matchData[c][r] == WordImageAnalysis.WORD_IMAGE_MATCH_MATCHED) || (this.base.matchData[c][r] == WordImageAnalysis.WORD_IMAGE_MATCH_RENDERED_ONLY)) ? 1 : 0));;
				}
			fillDistData(sDistData);
			fillDistData(rDistData);
			this.scannedOnlyAvgDist = ((float) getAvgDist(this.base.matchData, WordImageAnalysis.WORD_IMAGE_MATCH_SCANNED_ONLY, rDistData));
			this.scannedOnlyAvgSqrDist = ((float) getAvgSqrDist(this.base.matchData, WordImageAnalysis.WORD_IMAGE_MATCH_SCANNED_ONLY, rDistData));
			this.scannedOnlyMaxDist = getMaxDist(this.base.matchData, WordImageAnalysis.WORD_IMAGE_MATCH_SCANNED_ONLY, rDistData);
			this.renderedOnlyAvgDist = ((float) getAvgDist(this.base.matchData, WordImageAnalysis.WORD_IMAGE_MATCH_RENDERED_ONLY, sDistData));
			this.renderedOnlyAvgSqrDist = ((float) getAvgSqrDist(this.base.matchData, WordImageAnalysis.WORD_IMAGE_MATCH_RENDERED_ONLY, sDistData));
			this.renderedOnlyMaxDist = getMaxDist(this.base.matchData, WordImageAnalysis.WORD_IMAGE_MATCH_RENDERED_ONLY, sDistData);
		}
	}
	
	private static ExtendedWordImageMatch matchWordImages(WordImage scanned, WordImage rendered, boolean wordBoxMatch, boolean isVerificationMatch, float displaySim) {
		ExtendedWordImageMatch nswim = matchWordImages(scanned, rendered, true, true, false, 1);
		ExtendedWordImageMatch swim = matchWordImages(scanned, rendered, true, true, true, 1);
		return ((nswim.base.sim > swim.base.sim) ? nswim : swim);
	}
	
	private static ExtendedWordImageMatch matchWordImages(WordImage scanned, WordImage rendered, boolean wordBoxMatch, boolean isVerificationMatch, boolean allowShift, float displaySim) {
		ExtendedWordImageMatch wim = new ExtendedWordImageMatch(WordImageAnalysis.matchWordImages(scanned, rendered, wordBoxMatch, isVerificationMatch, allowShift));
		if (wim.base.sim > displaySim) {
			System.out.println("Word box match stats:");
			System.out.println(" - shifts are: left " + wim.base.leftShift + ", right " + wim.base.rightShift + ", top " + wim.base.topShift + ", bottom " + wim.base.bottomShift);
			System.out.println(" - area proportion distance is " + Math.abs(scanned.boxProportion - rendered.boxProportion));
			System.out.println(" - similarity is " + wim.base.sim + " (" + wim.base.precision + "/" + wim.base.recall + ")");
			System.out.println(" - weight adjusted similarity is " + wim.base.waSim + " (" + wim.base.waPrecision + "/" + wim.base.waRecall + ")");
			
			int histSmoothRad = 4;
			wim.computeMatchHistograms(histSmoothRad);
			
			int minHistAggregateBuckets = 2;
			int maxHistAggregateBuckets = 7;
			wim.computeMatchHistogramMetrics(minHistAggregateBuckets, maxHistAggregateBuckets);
			System.out.println(" - max split similarity contrast is " + wim.maxSimBucketContrast + ", max deviation is " + wim.maxSimBucketDeviation);
			System.out.println(" - avg split similarity contrast is " + wim.simBucketContrast + ", avg deviation is " + wim.simBucketDeviation);
			System.out.println(" - sqr-avg split similarity contrast is " + wim.simBucketContrastSqr + ", sqr-avg deviation is " + wim.simBucketDeviationSqr);
			
			wim.computeMatchAreaSurfaces();
			wim.computeMisMatchDistances();
			System.out.println(" - matched " + wim.base.matched + ", surface " + wim.matchedSurface);
			System.out.println(" - spurious " + wim.base.scannedOnly + ", surface " + wim.scannedOnlySurface + ", avg distance " + wim.scannedOnlyAvgDist + ", avg sqr distance " + wim.scannedOnlyAvgSqrDist + ", max distance " + wim.scannedOnlyMaxDist);
			System.out.println(" - missed " + wim.base.renderedOnly + ", surface " + wim.renderedOnlySurface + ", avg distance " + wim.renderedOnlyAvgDist + ", avg sqr distance " + wim.renderedOnlyAvgSqrDist + ", max distance " + wim.renderedOnlyMaxDist);
			displayWordMatch(wim, "Word box match");
		}
		return wim;
	}
	
//	/**
//	 * negative shift: shift first argument inward<br>
//	 * positive shift: shift second argument inward<br>
//	 * returns <code>[leftOrTopShift, rightOrBottomShift]</code>
//	 */
//	private static int[] getHistAlignmentShifts(int[] sHist, int[] rHist, int maxShift) {
//		int sLeft = 0;
//		while ((sLeft < sHist.length) && (sHist[sLeft] == 0))
//			sLeft++;
//		int sRight = sHist.length;
//		while ((sRight != 0) && (sHist[sRight-1] == 0))
//			sRight--;
//		int rLeft = 0;
//		while ((rLeft < rHist.length) && (rHist[rLeft] == 0))
//			rLeft++;
//		int rRight = rHist.length;
//		while ((rRight != 0) && (rHist[rRight-1] == 0))
//			rRight--;
//		
//		if ((sRight <= sLeft) || (rRight <= rLeft)) {
//			int[] shifts = {0, 0};
//			return shifts;
//		}
//		
//		int sWidth = (sRight - sLeft);
//		int rWidth = (rRight - rLeft);
//		
//		int leftShift = 0;
//		int rightShift = 0;
//		int bestShiftDistSquareSum = Integer.MAX_VALUE;
//		for (int ls = -maxShift; ls <= maxShift; ls++)
//			for (int rs = -maxShift; rs <= maxShift; rs++) {
//				int shiftDistSquareSum = 0;
//				int mWidth = Math.max((sWidth + Math.max(-ls, 0) + Math.max(-rs, 0)), (rWidth + Math.max(ls, 0) + Math.max(rs, 0)));
//				for (int c = 0; c < mWidth; c++) {
////					//	THIS IS NOT PUSHING INWARD, THIS IS JUMPING INWARD, AKA PULING OUTWARD !!!
////					int sCol = ((sLeft + Math.max(-ls, 0)) + ((c * (sWidth - Math.max(-ls, 0) - Math.max(-rs, 0))) / (mWidth - Math.max(-ls, 0) - Math.max(-rs, 0))));
////					int rCol = (rLeft + Math.max(ls, 0) + ((c * (rWidth - Math.max(ls, 0) - Math.max(rs, 0))) / (mWidth - Math.max(ls, 0) - Math.max(rs, 0))));
//					//	PUSH INWARD
////					int sCol = (0 - Math.max(-ls, 0) + ((c * (sHist.length + Math.max(-ls, 0) + Math.max(-rs, 0))) / mWidth));
////					int rCol = (0 - Math.max(ls, 0) + ((c * (rHist.length + Math.max(ls, 0) + Math.max(rs, 0))) / mWidth));
//					int sCol = (sLeft - Math.max(-ls, 0) + ((c * (sWidth + Math.max(-ls, 0) + Math.max(-rs, 0))) / mWidth));
//					int rCol = (rLeft - Math.max(ls, 0) + ((c * (rWidth + Math.max(ls, 0) + Math.max(rs, 0))) / mWidth));
//					
//					int sps = (((0 <= sCol) && (sCol < sHist.length)) ? sHist[sCol] : 0);
//					int rps = (((0 <= rCol) && (rCol < rHist.length)) ? rHist[rCol] : 0);
//					shiftDistSquareSum += ((sps - rps) * (sps - rps));
//				}
//				if (shiftDistSquareSum < bestShiftDistSquareSum) {
//					bestShiftDistSquareSum = shiftDistSquareSum;
//					leftShift = ls;
//					rightShift = rs;
//				}
//			}
//		int[] shifts = {leftShift, rightShift};
//		return shifts;
//	}
	
	private static int[] smoothHist(int[] hist, int radius) {
		int[] sHist = new int[hist.length];
		for (int c = 0; c < hist.length; c++) {
			sHist[c] = 0;
			for (int r = (c - radius); r <= (c + radius); r++)
				sHist[c] += (((r < 0) || (r >= hist.length)) ? 0 : hist[r]);
			sHist[c] /= (radius + 1 + radius);
		}
		return sHist;
	}
	
	private static void fillDistData(byte[][] distData) {
		for (int d = 1;; d++) {
			boolean allMatched = true;
			boolean newMatch = false;
			for (int x = 0; x < distData.length; x++)
				for (int y = 0; y < distData[x].length; y++) {
					if (distData[x][y] != 0)
						continue;
					byte dist = Byte.MAX_VALUE;
					if ((x != 0) && (distData[x-1][y] == d))
						dist = ((byte) (d+1));
					if ((y != 0) && (distData[x][y-1] == d))
						dist = ((byte) (d+1));
					if (((x+1) < distData.length) && (distData[x+1][y] == d))
						dist = ((byte) (d+1));
					if (((y+1) < distData[x].length) && (distData[x][y+1] == d))
						dist = ((byte) (d+1));
					if (dist < Byte.MAX_VALUE) {
						distData[x][y] = dist;
						newMatch = true;
					}
					else allMatched = false;
				}
			
			if (allMatched)
				break;
			
			if (newMatch)
				continue;
			
			for (int x = 0; x < distData.length; x++)
				for (int y = 0; y < distData[x].length; y++) {
					if (distData[x][y] == 0)
						distData[x][y] = Byte.MAX_VALUE;
				}
			break;
		}
	}
	
	private static double getAvgDist(byte[][] cimData, byte t, byte[][] distData) {
		int tCount = 0;
		double distSum = 0;
		for (int x = 0; x < cimData.length; x++)
			for (int y = 0; y < cimData[x].length; y++) {
				if (cimData[x][y] != t)
					continue;
				tCount++;
				distSum += (distData[x][y]-1);
			}
		return ((tCount == 0) ? 0 : (distSum / tCount));
	}
	
	private static double getAvgSqrDist(byte[][] cimData, byte t, byte[][] distData) {
		int tCount = 0;
		double distSum = 0;
		for (int x = 0; x < cimData.length; x++)
			for (int y = 0; y < cimData[x].length; y++) {
				if (cimData[x][y] != t)
					continue;
				tCount++;
				distSum += ((distData[x][y]-1) * (distData[x][y]-1));
			}
		return ((tCount == 0) ? 0 : Math.sqrt(distSum / tCount));
	}
	
	private static int getMaxDist(byte[][] cimData, byte t, byte[][] distData) {
		int maxDist = 0;
		for (int x = 0; x < cimData.length; x++)
			for (int y = 0; y < cimData[x].length; y++) {
				if (cimData[x][y] != t)
					continue;
				maxDist = Math.max(maxDist, (distData[x][y]-1));
			}
		return maxDist;
	}
	
	private static final float getSimBucketContrast(float[] simBuckets) {
		if (simBuckets.length < 2)
			return 0;
		double simBucketDistSquareSum = 0;
		for (int b = 1; b < simBuckets.length; b++)
			simBucketDistSquareSum += ((simBuckets[b-1] - simBuckets[b]) * (simBuckets[b-1] - simBuckets[b]));
		return ((float) Math.sqrt(simBucketDistSquareSum / (simBuckets.length - 1)));
	}
	
	private static final float getSimBucketDeviation(float[] simBuckets, float sim) {
		if (simBuckets.length < 1)
			return 0;
		double simBucketDevSquareSum = 0;
		for (int b = 0; b < simBuckets.length; b++)
			simBucketDevSquareSum += ((simBuckets[b] - sim) * (simBuckets[b] - sim));
		return ((float) Math.sqrt(simBucketDevSquareSum / simBuckets.length));
	}
	
	private static float[] getSimBuckets(ExtendedWordImageMatch wim, int numBuckets) {
		return getSimBuckets(wim.matchedHist, wim.scannedOnlyHist, wim.renderedOnlyHist, numBuckets);
	}
	
	private static float[] getSimBuckets(int[] matchedHist, int[] scannedOnlyHist, int[] renderedOnlyHist, int numBuckets) {
		float[] simBuckets = new float[numBuckets];
		for (int b = 0; b < numBuckets; b++) {
			int bMatched = 0;
			int bScannedOnly = 0;
			int bRenderedOnly = 0;
			for (int c = (b * (matchedHist.length / numBuckets)); c < ((b+1) * (matchedHist.length / numBuckets)); c++) {
				bMatched += matchedHist[c];
				bScannedOnly += scannedOnlyHist[c];
				bRenderedOnly += renderedOnlyHist[c];
			}
			if (bMatched == 0)
				simBuckets[b] = 0;
			else {
				float precision = (((float) bMatched) / (bScannedOnly + bMatched));
				float recall = (((float) bMatched) / (bRenderedOnly + bMatched));
				simBuckets[b] = ((precision * recall * 2) / (precision + recall));
			}
		}
		return simBuckets;
	}
	
	private static int getSurface(byte[][] cimData, byte t) {
		int tSurface = 0;
		for (int x = 0; x < cimData.length; x++)
			for (int y = 0; y < cimData[x].length; y++) {
				if (cimData[x][y] == t)
					continue;
				if ((x != 0) && (cimData[x-1][y] == t))
					tSurface++;
				else if ((y != 0) && (cimData[x][y-1] == t))
					tSurface++;
				else if (((x+1) < cimData.length) && (cimData[x+1][y] == t))
					tSurface++;
				else if (((y+1) < cimData[x].length) && (cimData[x][y+1] == t))
					tSurface++;
			}
		return tSurface;
	}
	
	private static final int whiteRgb = Color.WHITE.getRGB();
	private static final int blackRgb = Color.BLACK.getRGB();
	
	private static final int wordImageMargin = 10;
	
	private static BufferedImage getWordMatchImage(ExtendedWordImageMatch wim) {
		BufferedImage bi = new BufferedImage(
				(wim.base.scanned.img.getWidth() + wordImageMargin + wim.base.rendered.img.getWidth() + wordImageMargin + Math.max(wim.matchedHist.length, wim.base.matchData.length)),
				(Math.max(wim.base.scanned.img.getHeight(), wim.base.rendered.img.getHeight()) + wordImageMargin + Math.max(wim.base.scanned.img.getHeight(), wim.base.rendered.img.getHeight())),
				BufferedImage.TYPE_INT_RGB
			);
		Graphics g = bi.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		g.drawImage(wim.base.scanned.img, 0, 0, null);
		g.drawImage(wim.base.rendered.img, (wim.base.scanned.img.getWidth() + wordImageMargin), 0, null);
		g.setColor(Color.BLACK);
		if (0 < wim.base.scanned.baseline)
			g.drawLine(0, wim.base.scanned.baseline, wim.base.scanned.img.getWidth(), wim.base.scanned.baseline);
		if (0 < wim.base.rendered.baseline)
			g.drawLine((wim.base.scanned.img.getWidth() + wordImageMargin), wim.base.rendered.baseline, ((wim.base.scanned.img.getWidth() + wordImageMargin) + wim.base.rendered.img.getWidth()), wim.base.rendered.baseline);
		
		for (int x = 0; x < wim.base.matchData.length; x++) {
			for (int y = 0; y < wim.base.matchData[x].length; y++) {
				Color c = null;
				if (wim.base.matchData[x][y] == WordImageAnalysis.WORD_IMAGE_MATCH_MATCHED)
					c = Color.BLACK;
				else if (wim.base.matchData[x][y] == WordImageAnalysis.WORD_IMAGE_MATCH_SCANNED_ONLY)
					c = Color.GREEN;
				else if (wim.base.matchData[x][y] == WordImageAnalysis.WORD_IMAGE_MATCH_RENDERED_ONLY)
					c = Color.RED;
				if (c != null)
					bi.setRGB(
							(wim.base.scanned.img.getWidth() + wordImageMargin + wim.base.rendered.img.getWidth() + wordImageMargin + x),
							y,
							c.getRGB()
						);
			}
			
			for (int c = 0; c < wim.base.scanned.colBrightnessHist.length; c++) {
				int y = bi.getHeight();
				for (int h = 0; h < wim.base.scanned.colBrightnessHist[c]; h++)
					bi.setRGB(
							c,
							(--y),
							Color.BLACK.getRGB()
						);
			}
			
			for (int c = 0; c < wim.base.rendered.colBrightnessHist.length; c++) {
				int y = bi.getHeight();
				for (int r = 0; r < wim.base.rendered.colBrightnessHist[c]; r++)
					bi.setRGB(
							(wim.base.scanned.img.getWidth() + wordImageMargin + c),
							(--y),
							Color.BLACK.getRGB()
						);
			}
			
			for (int c = 0; c < wim.matchedHist.length; c++) {
				int y = bi.getHeight();
				for (int r = 0; r < wim.renderedOnlyHist[c]; r++)
					bi.setRGB(
							(wim.base.scanned.img.getWidth() + wordImageMargin + wim.base.rendered.img.getWidth() + wordImageMargin + c),
							(--y),
							Color.RED.getRGB()
						);
				for (int s = 0; s < wim.scannedOnlyHist[c]; s++)
					bi.setRGB(
							(wim.base.scanned.img.getWidth() + wordImageMargin + wim.base.rendered.img.getWidth() + wordImageMargin + c),
							(--y),
							Color.GREEN.getRGB()
						);
				for (int m = 0; m < wim.matchedHist[c]; m++)
					bi.setRGB(
							(wim.base.scanned.img.getWidth() + wordImageMargin + wim.base.rendered.img.getWidth() + wordImageMargin + c),
							(--y),
							Color.BLACK.getRGB()
						);
			}
		}
		
		return bi;
	}
	
	private static void displayWordMatch(ExtendedWordImageMatch wim, String message) {
		JOptionPane.showMessageDialog(null, (message + ": '" + wim.base.scanned.str + "'/'" + wim.base.rendered.str + "', similarity is " + wim.base.sim + "\n" + wim.base.matched + "-" + wim.base.scannedOnly + "-" + wim.base.renderedOnly), "Comparison Image", JOptionPane.PLAIN_MESSAGE, new ImageIcon(getWordMatchImage(wim)));
	}
	
	private static int verifyWordMatch(ExtendedWordImageMatch wim, String message) {
		return JOptionPane.showConfirmDialog(null, (message + ": '" + wim.base.scanned.str + "'/'" + wim.base.rendered.str + "', similarity is " + wim.base.sim + "\n" + wim.base.matched + "-" + wim.base.scannedOnly + "-" + wim.base.renderedOnly + "\n\nUse 'Cancel' for grouping"), "Comparison Image", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, new ImageIcon(getWordMatchImage(wim)));
	}
	
	private static int verifyWordOCR(ExtendedWordImageMatch wim, String message) {
		return JOptionPane.showConfirmDialog(null, (message + ": '" + wim.base.scanned.str + "'/'" + wim.base.rendered.str + "', similarity is " + wim.base.sim + "\n" + wim.base.matched + "-" + wim.base.scannedOnly + "-" + wim.base.renderedOnly + "\n\nUse 'Cancel' to ignore"), "Comparison Image", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, new ImageIcon(getWordMatchImage(wim)));
	}
//	
//	private static void displayWordMatches(WordMatchResult wmr, String message) {
//		displayWordMatches(wmr.plain, wmr.bold, message);
//	}
	
	private static void displayWordMatches(ExtendedWordImageMatch pwim, ExtendedWordImageMatch bwim, String message) {
		BufferedImage pmi = getWordMatchImage(pwim);
		BufferedImage bmi = getWordMatchImage(bwim);
		BufferedImage mi = new BufferedImage(Math.max(pmi.getWidth(), bmi.getWidth()), (pmi.getHeight() + 2 + bmi.getHeight()), BufferedImage.TYPE_INT_ARGB);
		Graphics mig = mi.createGraphics();
		mig.drawImage(pmi, 0, 0, null);
		mig.drawImage(bmi, 0, (pmi.getHeight() + 2), null);
		JOptionPane.showMessageDialog(null, (message + ": '" + pwim.base.rendered.str + "'\nplain similarity is " + pwim.base.sim + "\n" + pwim.base.matched + "-" + pwim.base.scannedOnly + "-" + pwim.base.renderedOnly + "\nbold similarity is " + bwim.base.sim + "\n" + bwim.base.matched + "-" + bwim.base.scannedOnly + "-" + bwim.base.renderedOnly), "Comparison Image", JOptionPane.PLAIN_MESSAGE, new ImageIcon(mi));
	}
	
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		final String testFileName;
		testFileName = "21330.pdf.boldTest.imf";
//		testFileName = "RSZ117_121.pdf.boldTest.imf";
//		testFileName = "melander_1907.boldTest.pdf.imf";
//		testFileName = "hesse_1974.pdf.boldTest.imf"; // many mis-OCRed male and female symbols on page 1, TODO good test for image clustering
//		testFileName = "ZM1967042005.pdf.raw.imf"; // some OCR errors on page 2, if in italics
//		testFileName = "paramonov_1950.pdf.imf"; // some OCR errors on page 1, if in italics
//		testFileName = "Kullander_ramirezi_1980.pdf.imf"; // some OCR errors on page 9, NOT in italics, but due to stains
//		testFileName = "1104.pdf.test.imf"; // some OCR errors, NOT in italics, and no stains, use page 4 for clustering, several occurrences of 'with'
		FileInputStream fis = new FileInputStream(new File("E:/Testdaten/PdfExtract/" + testFileName));
		ImDocument doc = ImfIO.loadDocument(fis);
		fis.close();
		
		WordImageAnalysis.analyzeFontMetrics(doc, ProgressMonitor.dummy);
		
		OcrCheckerProvider ocp = new OcrCheckerProvider();
		ocp.setDataProvider(new PluginDataProviderFileBased(new File("E:/GoldenGATEv3/Plugins/OcrCheckerData/")));
		ocp.init();
//		ocp.debug = true;
//		ocp.debugPage = -1;
		
//		if (true) {
//			ImWordClustering iwc = ocp.createWordClustering(doc, ProgressMonitor.dummy);
//			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			writeWordClustering(iwc, baos);
//			System.out.println("Got " + baos.size() + " bytes from " + iwc.wordIDsToClusters.size() + " words in " + iwc.idsToClusters.size() + " clusters and " + iwc.clusterSimilarities.size() + " similarities");
//			iwc = readWordClustering(doc, new ImSupplement(doc, "ocrWordClustering", "text/plain") {
//				public String getId() {
//					return "ocrWordClustering";
//				}
//				public InputStream getInputStream() throws IOException {
//					return new ByteArrayInputStream(baos.toByteArray());
//				}
//			});
//			System.out.println("Restored " + iwc.wordIDsToClusters.size() + " words in " + iwc.idsToClusters.size() + " clusters and " + iwc.clusterSimilarities.size() + " similarities");
//			for (Iterator wcit = iwc.idsToClusters.keySet().iterator(); wcit.hasNext();) {
//				ImWordCluster wc = iwc.getClusterForId(((String) wcit.next()), null);
//				if (wc.words.size() > 1)
//					JOptionPane.showMessageDialog(null, ("Representative of cluster '" + wc.str + "' (" + wc.words.size() + " words)"), "Cluster Representative", JOptionPane.PLAIN_MESSAGE, new ImageIcon(wc.representative));
//			}
//			return;
//		}
		ocp.checkOcr(doc, ProgressMonitor.dummy);
		if (!ocp.debug)
			return;
		
		System.out.println("Error resons for good OCR:");
		for (Iterator erit = ocp.goodOcrErrorReasons.iterator(); erit.hasNext();) {
			String er = ((String) erit.next());
			System.out.println(" - " + er + " (" + ocp.goodOcrErrorReasons.getCount(er) + " times out of " + ocp.goodOcrCount + ")");
		}
		System.out.println("Error resons for bad OCR:");
		for (Iterator erit = ocp.badOcrErrorReasons.iterator(); erit.hasNext();) {
			String er = ((String) erit.next());
			System.out.println(" - " + er + " (" + ocp.badOcrErrorReasons.getCount(er) + " times out of " + ocp.badOcrCount + ")");
		}
		
		System.out.println("Cluster sizes for good OCR:");
		printCountingSet(ocp.goodOcrClusterSizes, false);
		System.out.println("Cluster sizes for bad OCR:");
		printCountingSet(ocp.badOcrClusterSizes, false);
		System.out.println("Cluster sims for good OCR:");
		printCountingSet(ocp.goodOcrClusterSims, false);
		System.out.println("Cluster sims for bad OCR:");
		printCountingSet(ocp.badOcrClusterSims, false);
		
		System.out.println("Letter replacement scores for good OCR:");
		printCountingSet(ocp.goodOcrLetterReplacementScores, false);
		System.out.println("Letter replacement scores for bad OCR:");
		printCountingSet(ocp.badOcrLetterReplacementScores, false);
		System.out.println("Letter replacement trigram frequency gains for good OCR:");
		printCountingSet(ocp.goodOcrLetterReplacementTrigramFreqDiffs, false);
		System.out.println("Letter replacement trigram frequency gains for bad OCR:");
		printCountingSet(ocp.badOcrLetterReplacementTrigramFreqDiffs, false);
		
		System.out.println("Font size diffs for good OCR:");
		printCountingSet(ocp.goodOcrFontSizeDiffs, false);
		System.out.println("Font size diffs for bad OCR:");
		printCountingSet(ocp.badOcrFontSizeDiffs, false);
		
		System.out.println("Similarities for good OCR:");
		printCountingSet(ocp.goodOcrSims, true);
		System.out.println("Similarities for bad OCR:");
		printCountingSet(ocp.badOcrSims, true);
		System.out.println("Weight-adjusted similarities for good OCR:");
		printCountingSet(ocp.goodOcrWaSims, true);
		System.out.println("Weight-adjusted similarities for bad OCR:");
		printCountingSet(ocp.badOcrWaSims, true);
		System.out.println("Min letter similarities for good OCR:");
		printCountingSet(ocp.goodOcrMinLetterSims, true);
		System.out.println("Min letter similarities for bad OCR:");
		printCountingSet(ocp.badOcrMinLetterSims, true);
//		System.out.println("Top-n similarities for good OCR:");
//		printCountingSet(ocp.goodOcrTopSims, true);
//		System.out.println("Top-n similarities for bad OCR:");
//		printCountingSet(ocp.badOcrTopSims, true);
//		System.out.println("Top-n min letter similarities for good OCR:");
//		printCountingSet(ocp.goodOcrTopMinLetterSims, true);
//		System.out.println("Top-n min letter similarities for bad OCR:");
//		printCountingSet(ocp.badOcrTopMinLetterSims, true);
//		System.out.println("Top-n similarities by similarities for good OCR:");
//		printCountingSet(ocp.goodOcrTopSimsBySims, true);
//		System.out.println("Top-n similarities by similarities for bad OCR:");
//		printCountingSet(ocp.badOcrTopSimsBySims, true);
//		System.out.println("Max letter error by error for good OCR:");
//		printCountingSet(ocp.goodOcrMaxLetterErrorToError, true);
//		System.out.println("Max letter error by error for bad OCR:");
//		printCountingSet(ocp.badOcrMaxLetterErrorToError, true);
		
		System.out.println("Max contrasts for good OCR:");
		printCountingSet(ocp.goodOcrMaxContrasts, true);
		System.out.println("Max contrasts for bad OCR:");
		printCountingSet(ocp.badOcrMaxContrasts, true);
		System.out.println("Avg contrasts for good OCR:");
		printCountingSet(ocp.goodOcrAvgContrasts, true);
		System.out.println("Avg contrasts for bad OCR:");
		printCountingSet(ocp.badOcrAvgContrasts, true);
		System.out.println("Average distances for good OCR:");
		printCountingSet(ocp.goodOcrAvgDists, true);
		System.out.println("Average distances for bad OCR:");
		printCountingSet(ocp.badOcrAvgDists, true);
		System.out.println("Average square distances for good OCR:");
		printCountingSet(ocp.goodOcrAvgSqrDists, true);
		System.out.println("Average square distances for bad OCR:");
		printCountingSet(ocp.badOcrAvgSqrDists, true);
		System.out.println("Max distances for good OCR:");
		printCountingSet(ocp.goodOcrMaxDists, false);
		System.out.println("Max distances for bad OCR:");
		printCountingSet(ocp.badOcrMaxDists, false);
		System.out.println("Relative max distances for good OCR:");
		printCountingSet(ocp.goodOcrMaxDists, true);
		System.out.println("Relative max distances for bad OCR:");
		printCountingSet(ocp.badOcrRelMaxDists, true);
		
		System.out.println("Min trigram counts for good OCR:");
		printCountingSet(ocp.goodOcrMinTrigramCounts, false);
		System.out.println("Min trigram counts for bad OCR:");
		printCountingSet(ocp.badOcrMinTrigramCounts, false);
		System.out.println("Avg trigram counts for good OCR:");
		printCountingSet(ocp.goodOcrAvgTrigramCounts, false);
		System.out.println("Avg trigram counts for bad OCR:");
		printCountingSet(ocp.badOcrAvgTrigramCounts, false);
		
		System.out.println("Min trigram similarities for good OCR:");
		printCountingSet(ocp.goodOcrMinTrigramSims, true);
		System.out.println("Min trigram similarities for bad OCR:");
		printCountingSet(ocp.badOcrMinTrigramSims, true);
		System.out.println("Avg trigram similarities for good OCR:");
		printCountingSet(ocp.goodOcrAvgTrigramSims, true);
		System.out.println("Avg trigram similarities for bad OCR:");
		printCountingSet(ocp.badOcrAvgTrigramSims, true);
		System.out.println("Avg trigram similarity / actual similarity relations for good OCR:");
		printCountingSet(ocp.goodOcrAvgTrigramSimRels, true);
		System.out.println("Avg trigram similarity / actual similarity relations for bad OCR:");
		printCountingSet(ocp.badOcrAvgTrigramSimRels, true);
		
		System.out.println("Error position scores for good OCR:");
		printCountingSet(ocp.goodOcrErrorPosScores, false);
		System.out.println("Error position scores for bad OCR:");
		printCountingSet(ocp.badOcrErrorPosScores, false);
		
//		System.out.println("Similarity > avg cluster diameter: " + ocp.matchSimAboveClusterDiameter + " in matches, " + ocp.misMatchSimAboveClusterDiameter + " in mis-matches");
//		System.out.println("Similarity > weighted avg cluster diameter: " + ocp.matchSimAboveWeightedClusterDiameter + " in matches, " + ocp.misMatchSimAboveWeightedClusterDiameter + " in mis-matches");
//		System.out.println("Weight adjusted similarity > avg cluster diameter: " + ocp.matchWaSimAboveClusterDiameter + " in matches, " + ocp.misMatchWaSimAboveClusterDiameter + " in mis-matches");
//		System.out.println("Weight adjusted similarity > weighted avg cluster diameter: " + ocp.matchWaSimAboveWeightedClusterDiameter + " in matches, " + ocp.misMatchWaSimAboveWeightedClusterDiameter + " in mis-matches");
//		System.out.println("Min letter similarity > avg cluster diameter: " + ocp.matchMinLetterSimAboveClusterDiameter + " in matches, " + ocp.misMatchMinLetterSimAboveClusterDiameter + " in mis-matches");
//		System.out.println("Min letter similarity > weighted avg cluster diameter: " + ocp.matchMinLetterSimAboveWeightedClusterDiameter + " in matches, " + ocp.misMatchMinLetterSimAboveWeightedClusterDiameter + " in mis-matches");
//		System.out.println("Min letter similarity > similarity: " + ocp.matchMinLetterSimAboveSim + " in matches, " + ocp.misMatchMinLetterSimAboveSim + " in mis-matches");
//		System.out.println("Min letter similarity > average: " + ocp.matchMinLetterSimAboveAvg + " in matches, " + ocp.misMatchMinLetterSimAboveAvg + " in mis-matches");
//		System.out.println("Min letter similarity > weighted average: " + ocp.matchMinLetterSimAboveWeightedAvg + " in matches, " + ocp.misMatchMinLetterSimAboveWeightedAvg + " in mis-matches");
//		System.out.println("Min letter similarity outside edit: " + ocp.matchMinLetterSimOutsideEdit + " in matches, " + ocp.misMatchMinLetterSimOutsideEdit + " in mis-matches");
//		System.out.println("Min letter similarity outside base edit: " + ocp.matchMinLetterSimOutsideBaseEdit + " in matches, " + ocp.misMatchMinLetterSimOutsideBaseEdit + " in mis-matches");
//		System.out.println("OCR matches for matches: " + ocp.matchOcrMatches + " / " + ocp.matchOcrMisMatches);
//		System.out.println("OCR matches for mis-matches: " + ocp.misMatchOcrMatches + " / " + ocp.misMatchOcrMisMatches);
//		System.out.println("Correct predictions of matches: " + ocp.matchPredictionCorrect + " of " + ocp.matchPredictionCount);
//		System.out.println("Correct predictions of mis-matches: " + ocp.misMatchPredictionCorrect + " of " + ocp.misMatchPredictionCount);
//		if (ocp.goodMisMatches.size() != 0) {
//			System.out.println("Exteremely cose mis-matches:");
//			for (Iterator mmit = ocp.goodMisMatches.iterator(); mmit.hasNext();) {
//				ExtendedWordImageMatch wim = ((ExtendedWordImageMatch) mmit.next());
//				System.out.println(" - '" + wim.base.scanned.str + "' vs. '" + wim.base.rendered.str + "':");
//				System.out.println("Word box match stats:");
//				System.out.println(" - shifts are: left " + wim.base.leftShift + ", right " + wim.base.rightShift + ", top " + wim.base.topShift + ", bottom " + wim.base.bottomShift);
//				System.out.println(" - area proportion distance is " + Math.abs(wim.base.scanned.boxProportion - wim.base.rendered.boxProportion));
//				System.out.println(" - similarity is " + wim.base.sim + " (" + wim.base.precision + "/" + wim.base.recall + ")");
//				System.out.println(" - weight adjusted similarity is " + wim.base.waSim + " (" + wim.base.waPrecision + "/" + wim.base.waRecall + ")");
//				
//				int histSmoothRad = 4;
//				wim.computeMatchHistograms(histSmoothRad);
//				
//				int minHistAggregateBuckets = 2;
//				int maxHistAggregateBuckets = Math.max(wim.base.scanned.str.length(), wim.base.rendered.str.length());
//				wim.computeMatchHistogramMetrics(minHistAggregateBuckets, maxHistAggregateBuckets);
//				System.out.println(" - min letter similarity is " + wim.minLetterSim);
//				System.out.println(" - max split similarity contrast is " + wim.maxSimBucketContrast + ", max deviation is " + wim.maxSimBucketDeviation);
//				System.out.println(" - avg split similarity contrast is " + wim.simBucketContrast + ", avg deviation is " + wim.simBucketDeviation);
//				System.out.println(" - sqr-avg split similarity contrast is " + wim.simBucketContrastSqr + ", sqr-avg deviation is " + wim.simBucketDeviationSqr);
//				
//				wim.computeMatchAreaSurfaces();
//				wim.computeMisMatchDistances();
//				System.out.println(" - matched " + wim.base.matched + ", surface " + wim.matchedSurface);
//				System.out.println(" - spurious " + wim.base.scannedOnly + ", surface " + wim.scannedOnlySurface + ", avg distance " + wim.scannedOnlyAvgDist + ", avg sqr distance " + wim.scannedOnlyAvgSqrDist + ", max distance " + wim.scannedOnlyMaxDist);
//				System.out.println(" - missed " + wim.base.renderedOnly + ", surface " + wim.renderedOnlySurface + ", avg distance " + wim.renderedOnlyAvgDist + ", avg sqr distance " + wim.renderedOnlyAvgSqrDist + ", max distance " + wim.renderedOnlyMaxDist);
//				displayWordMatch(wim, "Close mis-match");
//			}
//		}
//		System.out.println("Match scores for matches:");
//		printCountingSet(ocp.matchMatchScores, true);
//		System.out.println("Match scores for mis-matches:");
//		printCountingSet(ocp.misMatchMatchScores, true);
//		System.out.println("Dictionary hit counts for matches:");
//		printCountingSet(ocp.matchDictHitCounts, false);
//		System.out.println("Dictionary hit counts for mis-matches:");
//		printCountingSet(ocp.misMatchDictHitCounts, false);
//		
//		System.out.println("Rendering similarity relations for matches:");
//		printCountingSet(ocp.matchRenderingSimRelations, true);
//		System.out.println("Rendering similarity relations for mis-matches:");
//		printCountingSet(ocp.misMatchRenderingSimRelations, true);
//		
//		System.out.println("Rendering cross-similarity relations for matches:");
//		printCountingSet(ocp.matchRenderingCrossSimRelations, true);
//		System.out.println("Rendering cross-similarity relations for mis-matches:");
//		printCountingSet(ocp.misMatchRenderingCrossSimRelations, true);
//		System.out.println("Rendering similarity quotient for matches:");
//		printCountingSet(ocp.matchRenderingSimQuots, true);
//		System.out.println("Rendering similarity quotient for mis-matches:");
//		printCountingSet(ocp.misMatchRenderingSimQuots, true);
//		System.out.println("Edits for matches:");
//		for (Iterator eit = ocp.matchEdits.iterator(); eit.hasNext();) {
//			String e = ((String) eit.next());
//			System.out.println(" - " + e + " (" + ocp.matchEdits.getCount(e) + " times)");
//		}
//		System.out.println("Edits for mis-matches:");
//		for (Iterator eit = ocp.misMatchEdits.iterator(); eit.hasNext();) {
//			String e = ((String) eit.next());
//			System.out.println(" - " + e + " (" + ocp.misMatchEdits.getCount(e) + " times)");
//		}
//		System.out.println("Edit signatures for matches:");
//		for (Iterator sit = ocp.matchEditSignatures.iterator(); sit.hasNext();) {
//			String s = ((String) sit.next());
//			System.out.println(" - " + s + " (" + ocp.matchEditSignatures.getCount(s) + " times)");
//		}
//		System.out.println("Edit signatures for mis-matches:");
//		for (Iterator sit = ocp.misMatchEditSignatures.iterator(); sit.hasNext();) {
//			String s = ((String) sit.next());
//			System.out.println(" - " + s + " (" + ocp.misMatchEditSignatures.getCount(s) + " times)");
//		}
//		System.out.println("Edit distaces for matches:");
//		printCountingSet(ocp.matchEditDists, false);
//		System.out.println("Edit distaces for mis-matches:");
//		printCountingSet(ocp.misMatchEditDists, false);
//		System.out.println("Optically compensated edit distaces for matches:");
//		printCountingSet(ocp.matchOptEditDists, false);
//		System.out.println("Optically compensated edit distaces for mis-matches:");
//		printCountingSet(ocp.misMatchOptEditDists, false);
//		System.out.println("Stem edit distaces for matches:");
//		printCountingSet(ocp.matchStemEditDists, false);
//		System.out.println("Stem edit distaces for mis-matches:");
//		printCountingSet(ocp.misMatchStemEditDists, false);
//		
//		System.out.println("Font size relations for matches:");
//		printCountingSet(ocp.matchFontSizeRels, true);
//		System.out.println("Font size relations for mis-matches:");
//		printCountingSet(ocp.misMatchFontSizeRels, true);
//		
//		System.out.println("Edit distaces for matches:");
//		printCountingSet(ocp.matchEditDists, true);
//		System.out.println("Edit distaces for mis-matches:");
//		printCountingSet(ocp.misMatchEditDists, true);
//		System.out.println("Max edit lengths for matches:");
//		printCountingSet(ocp.matchMaxEditLengths, false);
//		System.out.println("Max edit lengths for mis-matches:");
//		printCountingSet(ocp.misMatchMaxEditLengths, false);
//		System.out.println("Max relative edit lengths for matches:");
//		printCountingSet(ocp.matchMaxRelEditLengths, true);
//		System.out.println("Max relative edit lengths for mis-matches:");
//		printCountingSet(ocp.misMatchMaxRelEditLengths, true);
//		System.out.println("Edit distaces for matches >3:");
//		printCountingSet(ocp.matchEditDists4, true);
//		System.out.println("Edit distaces for mis-matches >3:");
//		printCountingSet(ocp.misMatchEditDists4, true);
//		System.out.println("Max edit lengths for matches >3:");
//		printCountingSet(ocp.matchMaxEditLengths4, false);
//		System.out.println("Max edit lengths for mis-matches >3:");
//		printCountingSet(ocp.misMatchMaxEditLengths4, false);
//		System.out.println("Max relative edit lengths for matches >3:");
//		printCountingSet(ocp.matchMaxRelEditLengths4, true);
//		System.out.println("Max relative edit lengths for mis-matches >3:");
//		printCountingSet(ocp.misMatchMaxRelEditLengths4, true);
//		
//		System.out.println("Base edit distaces for matches:");
//		printCountingSet(ocp.matchBaseEditDists, true);
//		System.out.println("Base edit distaces for mis-matches:");
//		printCountingSet(ocp.misMatchBaseEditDists, true);
//		System.out.println("Max base edit lengths for matches:");
//		printCountingSet(ocp.matchMaxBaseEditLengths, false);
//		System.out.println("Max base edit lengths for mis-matches:");
//		printCountingSet(ocp.misMatchMaxBaseEditLengths, false);
//		System.out.println("Max relative base edit lengths for matches:");
//		printCountingSet(ocp.matchMaxRelBaseEditLengths, true);
//		System.out.println("Max relative base edit lengths for mis-matches:");
//		printCountingSet(ocp.misMatchMaxRelBaseEditLengths, true);
//		System.out.println("Base edit distaces for matches >3:");
//		printCountingSet(ocp.matchBaseEditDists4, true);
//		System.out.println("Base edit distaces for mis-matches >3:");
//		printCountingSet(ocp.misMatchBaseEditDists4, true);
//		System.out.println("Max base edit lengths for matches >3:");
//		printCountingSet(ocp.matchMaxBaseEditLengths4, false);
//		System.out.println("Max base edit lengths for mis-matches >3:");
//		printCountingSet(ocp.misMatchMaxBaseEditLengths4, false);
//		System.out.println("Max relative base edit lengths for matches >3:");
//		printCountingSet(ocp.matchMaxRelBaseEditLengths4, true);
//		System.out.println("Max relative base edit lengths for mis-matches >3:");
//		printCountingSet(ocp.misMatchMaxRelBaseEditLengths4, true);
//		
//		System.out.println("Box proportion distaces for matches:");
//		printCountingSet(ocp.matchBoxPropDists, true);
//		System.out.println("Box proportion distaces for mis-matches:");
//		printCountingSet(ocp.misMatchBoxPropDists, true);
//		System.out.println("Similarities for matches:");
//		printCountingSet(ocp.matchSims, true);
//		System.out.println("Similarities for mis-matches:");
//		printCountingSet(ocp.misMatchSims, true);
//		System.out.println("Min letter similarities for matches:");
//		printCountingSet(ocp.matchMinLetterSims, true);
//		System.out.println("Min letter similarities for mis-matches:");
//		printCountingSet(ocp.misMatchMinLetterSims, true);
//		System.out.println("Top-n similarities for matches:");
//		printCountingSet(ocp.matchTopSims, true);
//		System.out.println("Top-n similarities for mis-matches:");
//		printCountingSet(ocp.misMatchTopSims, true);
//		System.out.println("Top-n min letter similarities for matches:");
//		printCountingSet(ocp.matchTopMinLetterSims, true);
//		System.out.println("Top-n min letter similarities for mis-matches:");
//		printCountingSet(ocp.misMatchTopMinLetterSims, true);
//		System.out.println("Top-n similarities by similarities for matches:");
//		printCountingSet(ocp.matchTopSimsBySims, true);
//		System.out.println("Top-n similarities by similarities for mis-matches:");
//		printCountingSet(ocp.misMatchTopSimsBySims, true);
//		System.out.println("Max letter error by error for matches:");
//		printCountingSet(ocp.matchMaxLetterErrorToError, true);
//		System.out.println("Max letter error by error for mis-matches:");
//		printCountingSet(ocp.misMatchMaxLetterErrorToError, true);
//		
//		System.out.println("Max contrasts for matches:");
//		printCountingSet(ocp.matchMaxContrasts, true);
//		System.out.println("Max contrasts for mis-matches:");
//		printCountingSet(ocp.misMatchMaxContrasts, true);
//		System.out.println("Avg contrasts for matches:");
//		printCountingSet(ocp.matchAvgContrasts, true);
//		System.out.println("Avg contrasts for mis-matches:");
//		printCountingSet(ocp.misMatchAvgContrasts, true);
//		System.out.println("Average distances for matches:");
//		printCountingSet(ocp.matchAvgDists, true);
//		System.out.println("Average distances for mis-matches:");
//		printCountingSet(ocp.misMatchAvgDists, true);
//		System.out.println("Average square distances for matches:");
//		printCountingSet(ocp.matchAvgSqrDists, true);
//		System.out.println("Average square distances for mis-matches:");
//		printCountingSet(ocp.misMatchAvgSqrDists, true);
//		System.out.println("Max distances for matches:");
//		printCountingSet(ocp.matchMaxDists, false);
//		System.out.println("Max distances for mis-matches:");
//		printCountingSet(ocp.misMatchMaxDists, false);
//		System.out.println("Relative max distances for matches:");
//		printCountingSet(ocp.matchMaxDists, true);
//		System.out.println("Relative max distances for mis-matches:");
//		printCountingSet(ocp.misMatchRelMaxDists, true);
	}
	
	private static void printCountingSet(CountingSet cs, boolean isPercent) {
		for (Iterator iit = cs.iterator(); iit.hasNext();) {
			Integer i = ((Integer) iit.next());
			System.out.println(" - " + i.intValue() + (isPercent ? "%" : "") + " (" + cs.getCount(i) + " times)");
		}
	}
}