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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.ImagingConstants;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.Imaging;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractImageEditToolProvider;
import de.uka.ipd.idaho.im.ocr.OcrEngine;
import de.uka.ipd.idaho.im.util.ImImageEditorPanel;
import de.uka.ipd.idaho.im.util.ImImageEditorPanel.ImImageEditTool;
import de.uka.ipd.idaho.im.util.ImImageEditorPanel.PatternOverpaintImageEditTool;
import de.uka.ipd.idaho.im.util.ImImageEditorPanel.SelectionImageEditTool;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * @author sautter
 *
 */
public class BasicImageEditToolProvider extends AbstractImageEditToolProvider {
	
	private ImImageEditTool pen1 = new PenX("Pen 1", "1");
	private ImImageEditTool pen3 = new PenX("Pen 3", "111\n111\n111");
	private ImImageEditTool pen5 = new PenX("Pen 5", "01110\n11111\n11111\n11111\n01110");
	private ImImageEditTool eraserBar = new Eraser("Eraser |", "111\n111\n111\n111\n111\n111\n111\n111\n111\n111\n111\n111\n111\n111\n111");
	private ImImageEditTool eraserDash = new Eraser("Eraser -", "111111111111111\n111111111111111\n111111111111111");
	private ImImageEditTool eraserSlash = new Eraser("Eraser /", "00000000011\n00000000111\n00000001110\n00000011100\n00000111000\n00001110000\n00011100000\n00111000000\n01110000000\n11100000000\n11000000000");
	private ImImageEditTool eraserBackslash = new Eraser("Eraser \\", "11000000000\n11100000000\n01110000000\n00111000000\n00011100000\n00001110000\n00000111000\n00000011100\n00000001110\n00000000111\n00000000011");
	private ImImageEditTool eraserFlex = new EraserFlex();
	private ImImageEditTool rotator = new Rotator();
	private ImImageEditTool wordRemover = new WordRemover();
	private ImImageEditTool wordMarker = new WordMarker();
	
	/** public zero-argument constructor for class loading */
	public BasicImageEditToolProvider() {}
	
	//	TODO load pattern based edit tools from config files, maybe even make them GG resources
	//	format: "Label" <HexColor> <Pattern (line break as \)>
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "IM Basic Image Edit Tools";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageEditToolProvider#getImageEditTools()
	 */
	public ImImageEditTool[] getImageEditTools() {
		ImImageEditTool[] imiets = {
			this.pen1,
			this.pen3,
			this.pen5,
			this.eraserBar,
			this.eraserDash,
			this.eraserSlash,
			this.eraserBackslash,
			this.eraserFlex,
			this.rotator,
			this.wordRemover,
			this.wordMarker,
		};
		return imiets;
	}
	
	private class EraserFlex extends SelectionImageEditTool {
		EraserFlex() {
			super("Eraser Flex", null, null, true);
		}
		protected void doEdit(ImImageEditorPanel iiep, int sx, int sy, int ex, int ey) {
			Graphics gr = iiep.getImage().getGraphics();
			Color bc = gr.getColor();
			gr.setColor(Color.WHITE);
			gr.fillRect(Math.min(sx, ex), Math.min(sy, ey), Math.abs(ex - sx), Math.abs(ey - sy));
			gr.setColor(bc);
			ImWord[] words = iiep.getWords();
			//	TODO observe page image edges
			BoundingBox edited = new BoundingBox(Math.min(sx, ex), Math.max(ex, sx), Math.min(sy, ey), Math.max(ey, sy));
			for (int w = 0; w < words.length; w++) {
				if (!edited.includes(words[w].bounds, true))
					continue;
				ImWord pWord = words[w].getPreviousWord();
				ImWord nWord = words[w].getNextWord();
				if ((pWord != null) && (nWord != null))
					pWord.setNextWord(nWord);
				else {
					words[w].setPreviousWord(null);
					words[w].setNextWord(null);
				}
				iiep.removeWord(words[w]);
			}
		}
	}
	
	private class PenX extends PatternOverpaintImageEditTool {
		PenX(String label, String pattern) {
			super(label, null, pattern, Color.BLACK);
		}
	}
	
	private class Eraser extends PatternOverpaintImageEditTool {
		Eraser(String label, String pattern) {
			super(label, null, pattern, Color.WHITE);
		}
	}
	
	private class WordRemover extends SelectionImageEditTool {
		WordRemover() {
			super("Remove Words", null, null, true);
		}
		protected void doEdit(ImImageEditorPanel iiep, int sx, int sy, int ex, int ey) {
			ImWord[] words = iiep.getWords();
			//	TODO observe page image edges
			BoundingBox edited = new BoundingBox(Math.min(sx, ex), Math.max(ex, sx), Math.min(sy, ey), Math.max(ey, sy));
			for (int w = 0; w < words.length; w++) {
				if (!edited.includes(words[w].bounds, true))
					continue;
				ImWord pWord = words[w].getPreviousWord();
				ImWord nWord = words[w].getNextWord();
				if ((pWord != null) && (nWord != null))
					pWord.setNextWord(nWord);
				else {
					words[w].setPreviousWord(null);
					words[w].setNextWord(null);
				}
				iiep.removeWord(words[w]);
			}
		}
	}
	
	private class WordMarker extends SelectionImageEditTool implements ImagingConstants {
		WordMarker() {
			super("Mark Words", null, null, true);
		}
		protected void doEdit(ImImageEditorPanel iiep, int sx, int sy, int ex, int ey) {
			
			//	normalize coordinates
			int sMinX = Math.min(sx, ex);
			int sMinY = Math.min(sy, ey);
			int sMaxX = Math.max(sx, ex);
			int sMaxY = Math.max(sy, ey);
			
			//	start with original selection
			int eMinX = sMinX;
			int eMinY = sMinY;
			int eMaxX = sMaxX;
			int eMaxY = sMaxY;
			
			//	get region coloring of selected rectangle
			boolean changed;
			boolean[] originalSelectionRegionInCol;
			boolean[] originalSelectionRegionInRow;
			do {
				
				//	get region coloring for current rectangle
				int[][] regionColors = Imaging.getRegionColoring(Imaging.wrapImage(iiep.getImage().getSubimage(eMinX, eMinY, (eMaxX - eMinX), (eMaxY - eMinY)), null), ((byte) 120), true);
				
				//	check which region colors occur in original selection
				boolean[] regionColorInOriginalSelection = new boolean[this.getMaxRegionColor(regionColors)];
				Arrays.fill(regionColorInOriginalSelection, false);
				for (int c = Math.max((sMinX - eMinX), 0); c < Math.min((sMaxX - eMinX), regionColors.length); c++)
					for (int r = Math.max((sMinY - eMinY), 0); r < Math.min((sMaxY - eMinY), regionColors[c].length); r++) {
						if (regionColors[c][r] != 0)
							regionColorInOriginalSelection[regionColors[c][r]-1] = true;
					}
				
				//	assess which columns and rows contain regions that overlap original selection
				originalSelectionRegionInCol = new boolean[regionColors.length];
				Arrays.fill(originalSelectionRegionInCol, false);
				originalSelectionRegionInRow = new boolean[regionColors[0].length];
				Arrays.fill(originalSelectionRegionInRow, false);
				for (int c = 0; c < regionColors.length; c++) {
					for (int r = 0; r < regionColors[c].length; r++)
						if (regionColors[c][r] != 0) {
							originalSelectionRegionInCol[c] = (originalSelectionRegionInCol[c] || regionColorInOriginalSelection[regionColors[c][r]-1]);
							originalSelectionRegionInRow[r] = (originalSelectionRegionInRow[r] || regionColorInOriginalSelection[regionColors[c][r]-1]);
						}
				}
				
				//	adjust boundaries
				changed = false;
				if (originalSelectionRegionInCol[0] && (eMinX != 0)) {
					eMinX--;
					changed = true;
				}
				else for (int c = 0; (c+1) < originalSelectionRegionInCol.length; c++) {
					if (originalSelectionRegionInCol[c+1])
						break;
					else {
						eMinX++;
						changed = true;
					}
				}
				if (originalSelectionRegionInCol[originalSelectionRegionInCol.length-1] && (eMaxX != (iiep.getImage().getWidth()-1))) {
					eMaxX++;
					changed = true;
				}
				else for (int c = (originalSelectionRegionInCol.length-1); c != 0; c--) {
					if (originalSelectionRegionInCol[c-1])
						break;
					else {
						eMaxX--;
						changed = true;
					}
				}
				if (originalSelectionRegionInRow[0] && (eMinY != 0)) {
					eMinY--;
					changed = true;
				}
				else for (int r = 0; (r+1) < originalSelectionRegionInRow.length; r++) {
					if (originalSelectionRegionInRow[r+1])
						break;
					else {
						eMinY++;
						changed = true;
					}
				}
				if (originalSelectionRegionInRow[originalSelectionRegionInRow.length-1] && (eMaxY != (iiep.getImage().getHeight()-1))) {
					eMaxY++;
					changed = true;
				}
				else for (int r = (originalSelectionRegionInRow.length-1); r != 0; r--) {
					if (originalSelectionRegionInRow[r-1])
						break;
					else {
						eMaxY--;
						changed = true;
					}
				}
				
				//	check if we still have something to work with
				if ((eMaxX <= eMinX) || (eMaxY <= eMinY))
					return;
			}
			
			//	keep going while there is adjustments
			while (changed);
			
			//	cut white edge
			if (!originalSelectionRegionInCol[0])
				eMinX++;
			if (!originalSelectionRegionInCol[originalSelectionRegionInCol.length-1])
				eMaxX--;
			if (!originalSelectionRegionInRow[0])
				eMinY++;
			if (!originalSelectionRegionInRow[originalSelectionRegionInRow.length-1])
				eMaxY--;
			
			//	make undo management know eventual selection
			this.addUndoPoint(eMinX, eMinY);
			this.addUndoPoint(eMaxX, eMaxY);
			
			//	collect strings and bold and italic properties from words in selection, and find predecessor and successor
			ImWord[] words = iiep.getWords();
			StringBuffer wordString = new StringBuffer();
			int boldCharCount = 0;
			int italicsCharCount = 0;
			//	TODO observe page image edges
			BoundingBox wordBox = new BoundingBox(eMinX, eMaxX, eMinY, eMaxY);
			ImWord prevWord = null;
			ImWord nextWord = null;
			for (int w = 0; w < words.length; w++) {
				if (!wordBox.includes(words[w].bounds, true) && !words[w].bounds.includes(wordBox, true))
					continue;
				if (prevWord == null) {
					prevWord = words[w].getPreviousWord();
					if (prevWord == null)
						prevWord = ((ImWord) words[w].getAttribute(ImWord.PREVIOUS_WORD_ATTRIBUTE + "Temp"));
				}
				nextWord = words[w].getNextWord();
				if (nextWord == null)
					nextWord = ((ImWord) words[w].getAttribute(ImWord.NEXT_WORD_ATTRIBUTE + "Temp"));
				wordString.append(words[w].getString());
				if (words[w].hasAttribute(BOLD_ATTRIBUTE))
					boldCharCount += words[w].getString().length();
				if (words[w].hasAttribute(ITALICS_ATTRIBUTE))
					italicsCharCount += words[w].getString().length();
			}
			
			//	use OCR if no words there
			if (wordString.length() == 0) {
				OcrEngine ocr = imagineParent.getOcrEngine();
				if (ocr != null) try {
					ImDocument ocrDoc = new ImDocument(iiep.getPage().getDocument().docId);
					ImPage ocrPage = new ImPage(ocrDoc, iiep.getPage().pageId, iiep.getPage().bounds);
					ImRegion ocrRegion = new ImRegion(ocrPage, wordBox, ImagingConstants.REGION_ANNOTATION_TYPE);
					ocr.doBlockOcr(ocrRegion, iiep.getPageImage(), null);
					ImWord[] ocrWords = ocrRegion.getWords();
					for (int w = 0; w < ocrWords.length; w++) {
						wordString.append(ocrWords[w].getString());
						if (ocrWords[w].hasAttribute(BOLD_ATTRIBUTE))
							boldCharCount += ocrWords[w].getString().length();
						if (ocrWords[w].hasAttribute(ITALICS_ATTRIBUTE))
							italicsCharCount += ocrWords[w].getString().length();
					}
				}
				catch (IOException ioe) {
					ioe.printStackTrace(System.out);
				}
			}
			
			//	create word
			ImWord word = new ImWord(iiep.getPage().getDocument(), iiep.getPage().pageId, wordBox, wordString.toString());
			if (wordString.length() < (boldCharCount * 2))
				word.setAttribute(BOLD_ATTRIBUTE);
			if (wordString.length() < (italicsCharCount * 2))
				word.setAttribute(ITALICS_ATTRIBUTE);
			
			//	prompt user
			if (!iiep.editWord(word) || (word.getString().trim().length() == 0))
				return;
			
			//	compute baseline
			boolean gotAscent = false;
			boolean gotDescent = false;
			for (int c = 0; c < word.getString().length(); c++) {
				char ch = word.getString().charAt(c);
				char bch = StringUtils.getBaseChar(ch);
				gotAscent = (gotAscent || Character.isUpperCase(bch) || Character.isDigit(bch) || ("bdfhijklt".indexOf(bch) != -1) || ("°!§$%&/(){}[]?#'|\\\"*€".indexOf(bch) != -1) || (ch != bch));
				gotDescent = (gotDescent || ("gjqy".indexOf(bch) != -1));
			}
			int baseline = word.bounds.bottom;
			if (gotDescent)
				baseline -= ((word.bounds.bottom - word.bounds.top) / 4);
			word.setAttribute(BASELINE_ATTRIBUTE, ("" + baseline));
			
			//	remove overwritten words
			for (int w = 0; w < words.length; w++) {
				if (wordBox.includes(words[w].bounds, true) || words[w].bounds.includes(wordBox, true))
					iiep.removeWord(words[w]);
			}
			
			//	get tokenizer to check and split words with
			Tokenizer tokenizer = ((Tokenizer) iiep.getPage().getDocument().getAttribute(ImDocument.TOKENIZER_ATTRIBUTE, Gamta.INNER_PUNCTUATION_TOKENIZER));
			ArrayList wordList = new ArrayList();
			TokenSequence wordTokens = tokenizer.tokenize(word.getString());
			
			if (wordTokens.size() == 1)
				wordList.add(word);
			else {
				System.out.println("   - splitting " + word.getString() + " at " + word.bounds + " into " + wordTokens.size() + " parts");
				
				//	get width for each token at word font size
				String[] splitTokens = new String[wordTokens.size()];
				float[] splitTokenWidths = new float[wordTokens.size()];
				Font wordFont = new Font("Serif", Font.BOLD, 24);
				float splitTokenWidthSum = 0;
				for (int s = 0; s < splitTokens.length; s++) {
					splitTokens[s] = wordTokens.valueAt(s);
					TextLayout tl = new TextLayout(splitTokens[s], wordFont, new FontRenderContext(null, false, true));
					splitTokenWidths[s] = ((float) tl.getBounds().getWidth());
					splitTokenWidthSum += splitTokenWidths[s];
				}
				
				//	store split result, splitting word bounds accordingly
				int wordWidth = (word.bounds.right - word.bounds.left);
				int splitTokenStart = word.bounds.left;
				for (int s = 0; s < splitTokens.length; s++) {
					int splitTokenWidth = Math.round((wordWidth * splitTokenWidths[s]) / splitTokenWidthSum);
					boolean cutLeft = ((s != 0) && (splitTokenWidths[s-1] < splitTokenWidths[s]));
					boolean cutRight = (((s + 1) != splitTokens.length) && (splitTokenWidths[s+1] < splitTokenWidths[s]));
					BoundingBox sWordBox = new BoundingBox(
							(splitTokenStart + (cutLeft ? 1 : 0)),
							Math.min((splitTokenStart + splitTokenWidth - (cutRight ? 1 : 0)), word.bounds.right),
							word.bounds.top,
							word.bounds.bottom
						);
					ImWord sWord = new ImWord(iiep.getPage().getDocument(), iiep.getPage().pageId, sWordBox, wordTokens.valueAt(s));
					sWord.copyAttributes(word);
					wordList.add(sWord);
					System.out.println("     - part " + sWord.getString() + " at " + sWord.bounds);
					splitTokenStart += splitTokenWidth;
				}
			}
			
			//	add word(s)
			for (int w = 0; w < wordList.size(); w++) {
				iiep.addWord((ImWord) wordList.get(w));
				if (w != 0)
					((ImWord) wordList.get(w-1)).setNextWord((ImWord) wordList.get(w));
			}
			
			//	prepare text stream integration
			if (prevWord != null)
				((ImWord) wordList.get(0)).setAttribute((ImWord.PREVIOUS_WORD_ATTRIBUTE + "Temp"), prevWord);
			if (nextWord != null)
				((ImWord) wordList.get(wordList.size() - 1)).setAttribute((ImWord.NEXT_WORD_ATTRIBUTE + "Temp"), nextWord);
		}
		
		private int getMaxRegionColor(int[][] regionColors) {
			int maxRegionColor = 0;
			for (int c = 0; c < regionColors.length; c++) {
				for (int r = 0; r < regionColors[c].length; r++)
					maxRegionColor = Math.max(maxRegionColor, regionColors[c][r]);
			}
			return maxRegionColor;
		}
	}
	
	private class Rotator extends SelectionImageEditTool implements ImagingConstants {
		Rotator() {
			super("Rotate", "Rotate parts of the page image", null, true);
		}
		
		protected void doEdit(ImImageEditorPanel iiep, int sx, int sy, int ex, int ey) {
			
			//	compute bounds
			int left = Math.min(sx, ex);
			int right = Math.max(sx, ex);
			int top = Math.min(sy, ey);
			int bottom = Math.max(sy, ey);
			
			//	get selected image block
			final BufferedImage bi = iiep.getImage().getSubimage(left, top, (right - left), (bottom - top));
			
			final JTextField angleField = new JTextField("0.0");
			final double[] angle = {Double.NaN};
			
			final JPanel bip = new JPanel() {
				public void paint(Graphics g) {
					super.paint(g);
					
					AffineTransform at = null;
					if ((angle[0] != Double.NaN) && (g instanceof Graphics2D)) {
						at = ((Graphics2D) g).getTransform();
						((Graphics2D) g).rotate(angle[0], (bi.getWidth() / 2), (bi.getHeight() / 2));
					}
					g.drawImage(bi, ((this.getWidth() - bi.getWidth()) / 2), ((this.getHeight() - bi.getHeight()) / 2), this);
					if (at != null)
						((Graphics2D) g).setTransform(at);
					
					Color preLineColor = g.getColor();
					g.setColor(Color.RED);
					for (int y = 10; y < this.getHeight(); y += 10)
						g.drawLine(0, y, this.getWidth(), y);
					g.setColor(preLineColor);
				}
			};
			bip.setBackground(Color.WHITE);
			bip.validate();
			bip.repaint();
			
			angleField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					try {
						angle[0] = Double.parseDouble(angleField.getText().trim());
						bip.validate();
						bip.repaint();
					} catch (Exception e) {}
				}
			});
			
			final DialogPanel dp = new DialogPanel("Rotate Image Part", true);
			
			JPanel ap = new JPanel(new BorderLayout(), true);
			ap.add(new JLabel("Rotation Angle (hit 'Enter' to apply): "), BorderLayout.WEST);
			ap.add(angleField, BorderLayout.CENTER);
			
			JButton ok = new JButton("Rotate");
			ok.setBorder(BorderFactory.createRaisedBevelBorder());
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					try {
						angle[0] = Double.parseDouble(angleField.getText().trim());
						dp.dispose();
					}
					catch (Exception e) {
						DialogFactory.alert("Invalid Rotation Angle", ("'" + angleField.getText() + "' is not a valid angle."), JOptionPane.ERROR_MESSAGE);
					}
				}
			});
			JButton cancel = new JButton("Cancel");
			cancel.setBorder(BorderFactory.createRaisedBevelBorder());
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					angle[0] = Double.NaN;
					dp.dispose();
				}
			});
			JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			bp.add(ok);
			bp.add(cancel);
			
			dp.add(ap, BorderLayout.NORTH);
			dp.add(bip, BorderLayout.CENTER);
			dp.add(bp, BorderLayout.SOUTH);
			
			dp.setSize((bi.getWidth() + 100), (bi.getHeight() + 100));
			dp.setLocationRelativeTo(dp.getOwner());
			dp.setVisible(true);
			
			if ((angle[0] == Double.NaN) || (angle[0] == 0))
				return;
			
			BufferedImage cbi = new BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());
			Graphics cg = cbi.getGraphics();
			cg.setColor(Color.WHITE);
			cg.fillRect(0, 0, cbi.getWidth(), cbi.getHeight());
			cg.drawImage(bi, 0, 0, null);
			cg.dispose();
			
			BufferedImage tbi = iiep.getImage();
			Graphics2D tg = tbi.createGraphics();
			tg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			tg.rotate(angle[0], ((left + right) / 2), ((top + bottom) / 2));
			tg.drawImage(cbi, left, top, null);
			tg.dispose();
		}
	}
}