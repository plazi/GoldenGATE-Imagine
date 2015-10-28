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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import javax.imageio.ImageIO;

import de.uka.ipd.idaho.gamta.util.ProgressMonitor;
import de.uka.ipd.idaho.gamta.util.imaging.BoundingBox;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.gamta.util.swing.ProgressMonitorDialog;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.analysis.Imaging;
import de.uka.ipd.idaho.im.analysis.Imaging.AnalysisImage;
import de.uka.ipd.idaho.im.analysis.Imaging.ImagePartRectangle;
import de.uka.ipd.idaho.im.analysis.PageImageAnalysis;
import de.uka.ipd.idaho.im.analysis.PageImageAnalysis.Block;
import de.uka.ipd.idaho.im.analysis.PageImageAnalysis.Line;
import de.uka.ipd.idaho.im.analysis.PageImageAnalysis.Region;
import de.uka.ipd.idaho.im.analysis.PageImageAnalysis.Word;
import de.uka.ipd.idaho.im.analysis.WordImageAnalysis;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ImageEditToolProvider;
import de.uka.ipd.idaho.im.ocr.OcrEngine;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.im.util.ImImageEditorPanel;
import de.uka.ipd.idaho.im.util.ImImageEditorPanel.ImImageEditTool;
import de.uka.ipd.idaho.im.util.ImImageEditorPanel.SelectionImageEditTool;
import de.uka.ipd.idaho.im.utilities.ImageDisplayDialog;

/**
 * This plugin provides functionality for running OCR on selected blocks of a
 * page image.
 * 
 * @author sautter
 */
public class BlockOcrProvider extends AbstractImageMarkupToolProvider implements ImageEditToolProvider {
	
	private static final boolean DEBUG_LINE_SPLITTING = false;
	
	private static final int minLineDistance = 5;
	private static final int whiteRgb = Color.WHITE.getRGB();
	
	private ImageBlockOCR imageBlockOcr = null;
	
	//	TODO provide image markup tool running OCR on empty blocks in document
	
	/** public zero-argument constructor for class loading */
	public BlockOcrProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "IM Block OCR Provider";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractGoldenGateImaginePlugin#initImagine()
	 */
	public void initImagine() {
		OcrEngine ocrEngine = this.imagineParent.getOcrEngine();
		if (ocrEngine != null)
			this.imageBlockOcr = new ImageBlockOCR(ocrEngine);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.AbstractImageMarkupToolProvider#getToolsMenuItemNames()
	 */
	public String[] getToolsMenuItemNames() {
		return super.getToolsMenuItemNames();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider#getImageMarkupTool(java.lang.String)
	 */
	public ImageMarkupTool getImageMarkupTool(String name) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageEditToolProvider#getImageEditTools()
	 */
	public ImImageEditTool[] getImageEditTools() {
		if (this.imageBlockOcr == null)
			return new ImImageEditTool[0];
		ImImageEditTool[] iiets = {this.imageBlockOcr};
		return iiets;
	}
	
	private class ImageBlockOCR extends SelectionImageEditTool {
		private OcrEngine ocrEngine;
		ImageBlockOCR(OcrEngine ocrEngine) {
			super("Detect Words", "Run OCR on a text block to detect words", null, true);
			this.ocrEngine = ocrEngine;
		}
		
		protected void doEdit(final ImImageEditorPanel iiep, final int sx, final int sy, final int ex, final int ey) {
			
			//	use progress monitor splash screen
			final ProgressMonitorDialog pmd = new ProgressMonitorDialog(DialogFactory.getTopWindow(), "Detecting Words ...");
			pmd.setSize(400, 130);
			pmd.setLocationRelativeTo(DialogFactory.getTopWindow());
			
			//	do the job
			Thread dwt = new Thread() {
				public void run() {
					try {
						while (!pmd.getWindow().isVisible()) try {
							Thread.sleep(10);
						} catch (InterruptedException ie) {}
						detectWords(iiep, sx, sy, ex, ey, pmd);
					}
					
					//	make sure to close progress monitor
					finally {
						pmd.close();
					}
				}
			};
			dwt.start();
			pmd.popUp(true);
		}
		
		protected void detectWords(ImImageEditorPanel iiep, int sx, int sy, int ex, int ey, ProgressMonitor pm) {
			
			//	compute bounds
			int left = Math.min(sx, ex);
			int right = Math.max(sx, ex);
			int top = Math.min(sy, ey);
			int bottom = Math.max(sy, ey);
			
			//	get selected image block
			BufferedImage bi = iiep.getImage().getSubimage(left, top, (right - left), (bottom - top));
			
			//	detect lines in block image
			pm.setStep("Computing line split");
			AnalysisImage bai = Imaging.wrapImage(bi, null);
			ImagePartRectangle bRect = Imaging.getContentBox(bai);
			ImagePartRectangle[] bLines = getBlockLines(bRect, iiep.getPageImage().currentDpi, pm);
			
			//	compute line offsets
			int[] bLineOffsets = new int[bLines.length];
			bLineOffsets[0] = 0;
			for (int l = 1; l < bLines.length; l++) {
				if ((bLines[l].getTopRow() - bLines[l-1].getBottomRow()) >= minLineDistance)
					bLineOffsets[l] = bLineOffsets[l-1];
				else bLineOffsets[l] = bLineOffsets[l-1] + (minLineDistance - (bLines[l].getTopRow() - bLines[l-1].getBottomRow()));
			}
			
			//	compute region coloring
			pm.setStep("Computing region coloring");
			int[][] bRegionColors = Imaging.getRegionColoring(bai, Imaging.computeAverageBrightness(bai), false);
			int bMaxRegionColor = getMaxRegionColor(bRegionColors);
			
			//	compute presence of each region color in each line
			pm.setStep("Computing region color distribution");
			int[] bRegionColorFrequencies = new int[bMaxRegionColor + 1];
			Arrays.fill(bRegionColorFrequencies, 0);
			int[] bRegionColorMinRows = new int[bMaxRegionColor + 1];
			Arrays.fill(bRegionColorMinRows, Integer.MAX_VALUE);
			int[] bRegionColorMaxRows = new int[bMaxRegionColor + 1];
			Arrays.fill(bRegionColorMaxRows, 0);
			for (int c = 0; c < bRegionColors.length; c++)
				for (int r = 0; r < bRegionColors[c].length; r++) {
					bRegionColorFrequencies[bRegionColors[c][r]]++;
					bRegionColorMinRows[bRegionColors[c][r]] = Math.min(bRegionColorMinRows[bRegionColors[c][r]], r);
					bRegionColorMaxRows[bRegionColors[c][r]] = Math.max(bRegionColorMaxRows[bRegionColors[c][r]], r);
				}
			int[][] bLineRegionColorFrequencies = new int[bLines.length][];
			for (int l = 0; l < bLines.length; l++) {
				bLineRegionColorFrequencies[l] = new int[bMaxRegionColor + 1];
				Arrays.fill(bLineRegionColorFrequencies[l], 0);
				for (int c = bLines[l].getLeftCol(); c < bLines[l].getRightCol(); c++) {
					for (int r = bLines[l].getTopRow(); r < bLines[l].getBottomRow(); r++)
						bLineRegionColorFrequencies[l][bRegionColors[c][r]]++;
				}
			}
			
			//	generate block image, with line offsets
			pm.setStep("Generating expanded block image");
			BufferedImage ocrBi = new BufferedImage(bRect.getWidth(), (bRect.getHeight() + (bLineOffsets[bLineOffsets.length-1])), BufferedImage.TYPE_BYTE_GRAY);
			Graphics ocrg = ocrBi.createGraphics();
			ocrg.setColor(Color.WHITE);
			ocrg.fillRect(0, 0, ocrBi.getWidth(), ocrBi.getHeight());
			for (int l = 0; l < bLines.length; l++) {
				ocrg.drawImage(bi.getSubimage(bLines[l].getLeftCol(), bLines[l].getTopRow(), bLines[l].getWidth(), bLines[l].getHeight()), (bLines[l].getLeftCol() - bRect.getLeftCol()), (bLines[l].getTopRow() - bRect.getTopRow() + bLineOffsets[l]), null);
				for (int c = bLines[l].getLeftCol(); c < bLines[l].getRightCol(); c++)
					for (int r = bLines[l].getTopRow(); r < bLines[l].getBottomRow(); r++) {
						int bLineRegionColorFrequency = bLineRegionColorFrequencies[l][bRegionColors[c][r]];
						
						//	check upward shift of lower descender tips if region color does not occur below line center
						if ((l != 0) && (bRegionColorMaxRows[bRegionColors[c][r]] < ((bLines[l].getTopRow() + bLines[l].getBottomRow()) / 2)) && (bLineRegionColorFrequencies[l-1][bRegionColors[c][r]] > (3 * bLineRegionColorFrequency))) {
							int rgb = ocrBi.getRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l]));
							ocrBi.setRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l-1]), rgb);
							ocrBi.setRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l]), whiteRgb);
						}
						
						//	check downward shift of upper ascender tips if region color does not occur above line center
						if (((l+1) < bLines.length) && bRegionColorMinRows[bRegionColors[c][r]] > ((bLines[l].getTopRow() + bLines[l].getBottomRow()) / 2) && (bLineRegionColorFrequencies[l+1][bRegionColors[c][r]] > (3 * bLineRegionColorFrequency))) {
							int rgb = ocrBi.getRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l]));
							ocrBi.setRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l+1]), rgb);
							ocrBi.setRGB((c - bRect.getLeftCol()), (r - bRect.getTopRow() + bLineOffsets[l]), whiteRgb);
						}
					}
			}
			
			//	wrap region
			pm.setStep("Wrapping block image");
			final PageImage ocrPi = new PageImage(ocrBi, iiep.getPageImage().currentDpi, null);
			ImDocument ocrDoc = new ImDocument("OcrTemp");
			ImPage ocrPage = new ImPage(ocrDoc, 0, new BoundingBox(0, ocrBi.getWidth(), 0, ocrBi.getHeight())) {
				public PageImage getImage() {
					return ocrPi;
				}
			};
			ImRegion ocrBlock = new ImRegion(ocrPage, ocrPage.bounds, ImRegion.BLOCK_ANNOTATION_TYPE);
			
			if (DEBUG_LINE_SPLITTING)
				showImage(ocrBi);
			
			//	run block OCR
			else try {
				pm.setStep("Doing OCR");
				this.ocrEngine.doBlockOcr(ocrBlock, ocrPi, pm);
			}
			catch (IOException ioe) {
				ioe.printStackTrace(System.out);
				return;
			}
			ImWord[] ocrWords = ocrBlock.getWords();
			if (ocrWords.length == 0)
				return;
			
			//	detect word properties (font size, bold, italics)
			pm.setStep("Detecting word properties");
			AnalysisImage bsAi = Imaging.wrapImage(ocrBi, null);
			Region bsReg = PageImageAnalysis.getPageRegion(bsAi, ocrPi.currentDpi, false, pm);
			Block bsBlock = bsReg.getBlock();
			if (bsBlock != null) {
				BoundingBox[] ocrWordBounds = new BoundingBox[ocrWords.length];
				HashMap ocrWordsByBounds = new HashMap();
				for (int w = 0; w < ocrWords.length; w++) {
					ocrWordBounds[w] = ocrWords[w].bounds;
					ocrWordsByBounds.put(ocrWords[w].bounds.toString(), ocrWords[w]);
				}
				PageImageAnalysis.getBlockStructure(bsBlock, ocrPi.currentDpi, ocrWordBounds, pm);
				PageImageAnalysis.computeLineBaselines(bsBlock, ocrPi.currentDpi);
				Line[] bsLines = bsBlock.getLines();
				for (int l = 0; l < bsLines.length; l++) {
					Word[] bsWords = bsLines[l].getWords();
					for (int w = 0; w < bsWords.length; w++) {
						ImWord ocrWord = ((ImWord) ocrWordsByBounds.get(bsWords[w].getBoundingBox()));
						if ((ocrWord != null) && (bsWords[w].getBaseline() != -1))
							ocrWord.setAttribute(ImWord.BASELINE_ATTRIBUTE, ("" + bsWords[w].getBaseline()));
					}
				}
			}
			
			//	analyze font metrics
			WordImageAnalysis.analyzeFontMetrics(ocrBlock, pm);
			
			//	remove existing words TODO observe page image edges
			pm.setStep("Removing spurious words");
			BoundingBox bBounds = new BoundingBox(left, right, top, bottom);
			ImWord[] eWords = iiep.getWords();
			for (int w = 0; w < eWords.length; w++) {
				if (eWords[w].bounds.liesIn(bBounds, true))
					iiep.removeWord(eWords[w]);
			}
			
			//	add words to document, line by line, subtracting vertical offsets
			pm.setStep("Adding detected words");
			for (int l = 0; l < bLines.length; l++)
				for (int w = 0; w < ocrWords.length; w++) {
					if (ocrWords[w] == null)
						continue;
					if ((ocrWords[w].centerY > (bLines[l].getTopRow() - bRect.getTopRow() + bLineOffsets[l])) && (ocrWords[w].centerY < (bLines[l].getBottomRow() - bRect.getTopRow() + bLineOffsets[l]))) {
						//	TODO observe page image edges
						ImWord bWord = new ImWord(iiep.getPage().getDocument(), iiep.getPage().pageId, new BoundingBox(
								(ocrWords[w].bounds.left + left + bRect.getLeftCol()),
								(ocrWords[w].bounds.right + left + bRect.getLeftCol()),
								(ocrWords[w].bounds.top + top + bRect.getTopRow() - bLineOffsets[l]),
								(ocrWords[w].bounds.bottom + top + bRect.getTopRow() - bLineOffsets[l])
							), ocrWords[w].getString());
						bWord.copyAttributes(ocrWords[w]);
						if (ocrWords[w].hasAttribute(ImWord.BASELINE_ATTRIBUTE)) {
							int ocrWordBl = Integer.parseInt((String) ocrWords[w].getAttribute(ImWord.BASELINE_ATTRIBUTE));
							bWord.setAttribute(ImWord.BASELINE_ATTRIBUTE, ("" + (ocrWordBl + top + bRect.getTopRow() - bLineOffsets[l])));
						}
						iiep.addWord(bWord);
						ocrWords[w] = null;
					}
				}
		}
	}
	
	private static int getMaxRegionColor(int[][] regionColors) {
		int maxRegionColor = 0;
		for (int c = 0; c < regionColors.length; c++) {
			for (int r = 0; r < regionColors[c].length; r++)
				maxRegionColor = Math.max(maxRegionColor, regionColors[c][r]);
		}
		return maxRegionColor;
	}
	
	private static ImagePartRectangle[] getBlockLines(ImagePartRectangle block, int dpi, ProgressMonitor pm) {
		
		//	try plain whitespace split first
		ImagePartRectangle[] lines = Imaging.splitIntoRows(block, 1);
		
		//	analyze line height distribution
		int minLineHeight = Integer.MAX_VALUE;
		int maxLineHeight = 0;
		int lineHeightSum = 0;
		for (int l = 0; l < lines.length; l++) {
			minLineHeight = Math.min(lines[l].getHeight(), minLineHeight);
			maxLineHeight = Math.max(lines[l].getHeight(), maxLineHeight);
			lineHeightSum += lines[l].getHeight();
		}
		
		//	this looks OK (min and max within two thirds of one another)
		if ((lines.length > 2) && ((minLineHeight * 3) > (maxLineHeight * 2)))
			return lines;
		
		//	compute row brightness
		byte[][] brightness = block.getImage().getBrightness();
		int[] rowBrightness = new int[block.getHeight()];
		for (int c = block.getLeftCol(); c < block.getRightCol(); c++) {
			for (int r = block.getTopRow(); r < block.getBottomRow(); r++)
				rowBrightness[r - block.getTopRow()] += brightness[c][r];
		}
		for (int r = 0; r < rowBrightness.length; r++)
			rowBrightness[r] /= block.getWidth();
		for (int r = 0; r < rowBrightness.length; r++)
			System.out.println((r + block.getTopRow()) + ": " + rowBrightness[r]);
		
		//	compute average row brightness
		int avgRowBrightness = 0;
		for (int r = 0; r < rowBrightness.length; r++)
			avgRowBrightness += rowBrightness[r];
		avgRowBrightness /= rowBrightness.length;
		pm.setInfo("avg: " + avgRowBrightness);
		
		//	try estimating line count based on 'black bars' caused by letters within x-height
		int xLineCount = 0;
		int xStart = -1;
		int avgxStartDist = 0;
		int avgxHeight = 0;
		int lxStart = -1;
		int lxEnd = 0;
		boolean[] isxRow = new boolean[rowBrightness.length];
		Arrays.fill(isxRow, false);
		for (int r = 2; r < rowBrightness.length; r++) {
			if ((xStart == -1) && (rowBrightness[r-2] < avgRowBrightness) && (rowBrightness[r-1] < avgRowBrightness) && (rowBrightness[r] < avgRowBrightness)) {
				xLineCount++;
				xStart = (r-2);
				if (lxStart != -1)
					avgxStartDist += (xStart - lxStart);
				lxStart = xStart;
			}
			if (xStart != -1)
				isxRow[r-2] = true;
			if ((xStart != -1) && (rowBrightness[r-1] >= avgRowBrightness) && (rowBrightness[r] >= avgRowBrightness)) {
				lxEnd = (r-2);
				avgxHeight += (lxEnd - xStart + 1);
				xStart = -1;
			}
		}
		pm.setInfo("x lines: " + xLineCount);
		if (xLineCount > 0)
			avgxHeight /= xLineCount;
		pm.setInfo("avg x-height: " + avgxHeight);
		if (xLineCount > 1)
			avgxStartDist = ((avgxStartDist + (xLineCount / 2)) / (xLineCount - 1));
		else avgxStartDist = -1;
		pm.setInfo("avg x start dist: " + avgxStartDist);
		
		//	compute line count
		int lineCount = xLineCount;
		//	- last x-row way further back than avg line height ==> increment line count by one to amend for short (and thus light) last line
		if ((avgxStartDist != -1) && ((block.getHeight() - lxEnd) > avgxStartDist)) {
			lineCount++;
			for (int r = (lxStart + avgxStartDist); r <= (lxEnd + avgxStartDist); r++)
				isxRow[r] = true;
		}
		//	- last x-end way further from block end than x-start from block start ==> increment line count by one to amend for short (and thus light) last line
		else if ((avgxStartDist == -1) && ((lxStart * 3) < (block.getHeight() - lxEnd)))
			lineCount++;
		pm.setInfo("lines: " + lineCount);
		pm.setInfo("avg line height: " + (block.getHeight() / lineCount));
		
		//	split block (about) equidistantly
		int[] lineBounds = new int[lineCount+1];
		lineBounds[0] = block.getTopRow();
		lineBounds[lineCount] = block.getBottomRow();
		for (int l = 1; l < lineCount; l++) {
			lineBounds[l] = (block.getTopRow() + ((l * block.getHeight()) / lineCount));
			if (avgxStartDist != -1) {
				int topDist = 0;
				while ((topDist < (lineBounds[l] - block.getTopRow())) && !isxRow[lineBounds[l]-block.getTopRow()-topDist-1])
					topDist++;
				int bottomDist = 0;
				while ((bottomDist < (block.getBottomRow() - lineBounds[l])) && !isxRow[lineBounds[l]-block.getTopRow()+bottomDist+1])
					bottomDist++;
				while (topDist > bottomDist) {
					lineBounds[l]--;
					topDist--;
					bottomDist++;
				}
				while (topDist < bottomDist) {
					lineBounds[l]++;
					topDist++;
					bottomDist--;
				}
			}
			while ((lineBounds[l] > block.getTopRow()) && (rowBrightness[lineBounds[l]-block.getTopRow()] < rowBrightness[lineBounds[l]-block.getTopRow()-1]))
				lineBounds[l]--;
			while ((lineBounds[l] < block.getBottomRow()) && (rowBrightness[lineBounds[l]-block.getTopRow()] < rowBrightness[lineBounds[l]-block.getTopRow()+1]))
				lineBounds[l]++;
		}
		
		//	create lines
		lines = new ImagePartRectangle[lineCount];
		for (int l = 0; l < lineCount; l++) {
			lines[l] = block.getSubRectangle(block.getLeftCol(), block.getRightCol(), lineBounds[l], lineBounds[l+1]);
			Imaging.narrowTopAndBottom(lines[l]);
			Imaging.narrowLeftAndRight(lines[l]);
			pm.setInfo("line: " + lines[l].getId());
		}
		
		//	finally ...
		return lines;
	}
	
	private static void showImage(BufferedImage bi) {
		ImageDisplayDialog idd = new ImageDisplayDialog("");
		idd.setSize((bi.getWidth() + 100), (bi.getHeight() + 100));
		idd.setLocationRelativeTo(null);
		idd.addImage(bi, "");
		idd.setVisible(true);
	}
	
	//	!!! TEST ONLY !!!
	public static void main(String[] args) throws Exception {
		BufferedImage bi = ImageIO.read(new File("E:/Testdaten/PdfExtract/OcrTestBlock.png"));
		int dpi = 150;
		AnalysisImage bai = Imaging.wrapImage(bi, null);
		ImagePartRectangle bRect = Imaging.getContentBox(bai);
		ImagePartRectangle[] bLines = getBlockLines(bRect, dpi, ProgressMonitor.dummy);
		
		//	compute line offsets
		int[] bLineOffsets = new int[bLines.length];
		bLineOffsets[0] = 0;
		for (int l = 1; l < bLines.length; l++) {
			if ((bLines[l].getTopRow() - bLines[l-1].getBottomRow()) >= minLineDistance)
				bLineOffsets[l] = bLineOffsets[l-1];
			else bLineOffsets[l] = bLineOffsets[l-1] + (minLineDistance - (bLines[l].getTopRow() - bLines[l-1].getBottomRow()));
		}
		
		//	generate block image, with line offsets
		BufferedImage ocrBi = new BufferedImage(bRect.getWidth(), (bRect.getHeight() + (bLineOffsets[bLineOffsets.length-1])), BufferedImage.TYPE_BYTE_GRAY);
		Graphics ocrg = ocrBi.createGraphics();
		ocrg.setColor(Color.WHITE);
		ocrg.fillRect(0, 0, ocrBi.getWidth(), ocrBi.getHeight());
		for (int l = 0; l < bLines.length; l++)
			ocrg.drawImage(bi.getSubimage(bLines[l].getLeftCol(), bLines[l].getTopRow(), bLines[l].getWidth(), bLines[l].getHeight()), (bLines[l].getLeftCol() - bRect.getLeftCol()), (bLines[l].getTopRow() - bRect.getTopRow() + bLineOffsets[l]), null);
		showImage(ocrBi);
	}
}