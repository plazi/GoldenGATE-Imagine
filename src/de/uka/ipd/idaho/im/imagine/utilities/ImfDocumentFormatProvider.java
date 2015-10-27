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
package de.uka.ipd.idaho.im.imagine.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentFormatProvider;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceSplashScreen;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.gamta.ImDocumentRoot;
import de.uka.ipd.idaho.im.util.ImfIO;

/**
 * This document format provider allows loading IMF documents into GoldenGATE
 * Document Editor, in a read-only fashion. The functionality is intended to
 * simplify testing of markup tools, rather than a production environment.
 * 
 * @author sautter
 */
public class ImfDocumentFormatProvider extends AbstractDocumentFormatProvider {
	
	private static final String RAW_LOAD_FORMAT_NAME = "imfRaw";
	private static final String WORD_LOAD_FORMAT_NAME = "imfWord";
	private static final String PARAGRAPH_LOAD_FORMAT_NAME = "imfParagraph";
	private static final String STREAM_LOAD_FORMAT_NAME = "imfStream";
	
	private DocumentFormat rawImfDocumentFormat = new ImfDocumentFormat(RAW_LOAD_FORMAT_NAME, "un-normalized", ImDocumentRoot.NORMALIZATION_LEVEL_RAW);
	private DocumentFormat wordImfDocumentFormat = new ImfDocumentFormat(WORD_LOAD_FORMAT_NAME, "word-normalized", ImDocumentRoot.NORMALIZATION_LEVEL_WORDS);
	private DocumentFormat paragraphImfDocumentFormat = new ImfDocumentFormat(PARAGRAPH_LOAD_FORMAT_NAME, "paragraph-normalized", ImDocumentRoot.NORMALIZATION_LEVEL_PARAGRAPHS);
	private DocumentFormat streamImfDocumentFormat = new ImfDocumentFormat(STREAM_LOAD_FORMAT_NAME, "fully normalized", ImDocumentRoot.NORMALIZATION_LEVEL_STREAMS);
	
	/** public zero-argument constructor to facilitate class loading */
	public ImfDocumentFormatProvider() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IMF Document Format";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFileExtensions()
	 */
	public String[] getFileExtensions() {
		String[] fes = {".imf"};
		return fes;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForFileExtension(java.lang.String)
	 */
	public DocumentFormat getFormatForFileExtension(String fileExtension) {
		return ("imf".equalsIgnoreCase(fileExtension) ? this.paragraphImfDocumentFormat : null);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFormatNames()
	 */
	public String[] getLoadFormatNames() {
		String[] lfns = {
			RAW_LOAD_FORMAT_NAME,
			WORD_LOAD_FORMAT_NAME,
			PARAGRAPH_LOAD_FORMAT_NAME,
			STREAM_LOAD_FORMAT_NAME
		};
		return lfns;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFormatNames()
	 */
	public String[] getSaveFormatNames() {
		return new String[0];
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForName(java.lang.String)
	 */
	public DocumentFormat getFormatForName(String formatName) {
		if (RAW_LOAD_FORMAT_NAME.equals(formatName))
			return this.rawImfDocumentFormat;
		else if (WORD_LOAD_FORMAT_NAME.equals(formatName))
			return this.wordImfDocumentFormat;
		else if (PARAGRAPH_LOAD_FORMAT_NAME.equals(formatName))
			return this.paragraphImfDocumentFormat;
		else if (STREAM_LOAD_FORMAT_NAME.equals(formatName))
			return this.streamImfDocumentFormat;
		else return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getLoadFileFilters()
	 */
	public DocumentFormat[] getLoadFileFilters() {
		DocumentFormat[] lffs = {
				this.rawImfDocumentFormat,
				this.wordImfDocumentFormat,
				this.paragraphImfDocumentFormat,
				this.streamImfDocumentFormat
			};
		return lffs;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getSaveFileFilters()
	 */
	public DocumentFormat[] getSaveFileFilters() {
		return new DocumentFormat[0];
	}
	
	private class ImfDocumentFormat extends DocumentFormat {
		private String name;
		private String description;
		private int normalizationLevel;
		
		ImfDocumentFormat(String name, String description, int normalizationLevel) {
			this.name = name;
			this.description = description;
			this.normalizationLevel = normalizationLevel;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
		 */
		public String getName() {
			return this.name;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return ImfDocumentFormatProvider.class.getName();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#loadDocument(java.io.InputStream)
		 */
		public MutableAnnotation loadDocument(final InputStream source) throws IOException {
			
			//	prepare loading in separate thread
			final MutableAnnotation[] doc = {null};
			final IOException[] loadException = {null};
			final ResourceSplashScreen loadScreen = new ResourceSplashScreen(DialogPanel.getTopWindow(), "Loading IMF Document", "", true, false);
			
			//	set up loading thread
			System.out.println("Creating load thread");
			Thread loadThread = new Thread("LoaderThread") {
				public void run() {
					try {
						loadScreen.setStep("Loading IMF Archive");
						while (!loadScreen.isVisible()) try {
							Thread.sleep(50);
						} catch (InterruptedException ie) {}
						
						//	load IMF document
						ImDocument imDoc = ImfIO.loadDocument(source, loadScreen, -1);
						imDoc.setAttribute(ImDocument.TOKENIZER_ATTRIBUTE, ((parent == null) ? Gamta.INNER_PUNCTUATION_TOKENIZER : parent.getTokenizer()));
						
						//	assemble config flags
						int configFlags = normalizationLevel;
						if ((normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_RAW) || (normalizationLevel == ImDocumentRoot.NORMALIZATION_LEVEL_WORDS))
							configFlags |= ImDocumentRoot.SHOW_TOKENS_AS_WORD_ANNOTATIONS;
						
						//	wrap document
						ImDocumentRoot wDoc = new ImDocumentRoot(imDoc, configFlags);
						doc[0] = Gamta.copyDocument(wDoc);
						
						//	clean up
						imDoc.dispose();
					}
					catch (IOException ioe) {
						loadException[0] = ioe;
					}
					finally {
						loadScreen.dispose();
					}
				}
			};
			loadThread.start();
			
			//	wait for loading thread and forward result
			loadScreen.setVisible(true);
			if (loadException[0] == null)
				return doc[0];
			else throw loadException[0];
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#loadDocument(java.io.Reader)
		 */
		public MutableAnnotation loadDocument(Reader source) throws IOException {
			return null; // we're loading the stream directly
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.gamta.QueriableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(QueriableAnnotation data, Writer out) throws IOException {
			return false; // we're not saving
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.goldenGate.DocumentEditor, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, Writer out) throws IOException {
			return false; // we're not saving
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#saveDocument(de.uka.ipd.idaho.goldenGate.DocumentEditor, de.uka.ipd.idaho.gamta.QueriableAnnotation, java.io.Writer)
		 */
		public boolean saveDocument(DocumentEditor data, QueriableAnnotation doc, Writer out) throws IOException {
			return false; // we're not saving
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#isExportFormat()
		 */
		public boolean isExportFormat() {
			return false; // we're not even saving
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#getDefaultSaveFileExtension()
		 */
		public String getDefaultSaveFileExtension() {
			return null; // we're not saving
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#equals(de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat)
		 */
		public boolean equals(DocumentFormat format) {
			return ((format instanceof ImfDocumentFormat) && ((ImfDocumentFormat) format).name.equals(this.name));
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat#accept(java.lang.String)
		 */
		public boolean accept(String fileName) {
			return ((fileName != null) && fileName.toLowerCase().endsWith(".imf"));
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		public String getDescription() {
			return ("IMF Documents (" + this.description + ")");
		}
	}
}