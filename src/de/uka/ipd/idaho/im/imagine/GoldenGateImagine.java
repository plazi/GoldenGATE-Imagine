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
package de.uka.ipd.idaho.im.imagine;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.util.imaging.PageImage;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageInputStream;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageStore;
import de.uka.ipd.idaho.gamta.util.imaging.PageImageStore.AbstractPageImageStore;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.GoldenGATE;
import de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilter;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilterManager;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentEditorExtension;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentSaver;
import de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin;
import de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePluginDataProvider;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceManager;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.im.ImDocument;
import de.uka.ipd.idaho.im.ImSupplement;
import de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImagineDocumentListener;
import de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin;
import de.uka.ipd.idaho.im.imagine.plugins.ImageDocumentDropHandler;
import de.uka.ipd.idaho.im.imagine.plugins.ImageDocumentExporter;
import de.uka.ipd.idaho.im.imagine.plugins.ImageEditToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ImageMarkupToolProvider;
import de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider;
import de.uka.ipd.idaho.im.imagine.plugins.SelectionActionProvider;
import de.uka.ipd.idaho.im.ocr.OcrEngine;
import de.uka.ipd.idaho.im.pdf.PdfExtractor;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public class GoldenGateImagine implements GoldenGateConstants {
	
	private static final SimpleDateFormat yearTimestamper = new SimpleDateFormat("yyyy");
	private static final String ABOUT_TEXT = 
		"GoldenGATE Imagine " + VERSION_STRING + "\n" +
		"The easy way to mark up Documents\n" +
		"Version Date: " + VERSION_DATE + "\n" +
		"\n" +
		"\u00A9 by Guido Sautter 2006-" + yearTimestamper.format(new Date()) + "\n" +
		"IPD Boehm\n" +
		"Karlsruhe Institute of Technology (KIT)";
	
	private static final int maxInMemoryImageSupplementBytes = (50 * 1024 * 1024); // 50 MB
	
	private GoldenGateConfiguration configuration;
	private GoldenGATE goldenGate;
	
	private PageImageStore pageImageStore;
	private PdfExtractor pdfExtractor;
	
	private GoldenGateImagine(GoldenGateConfiguration configuration, GoldenGATE gg, File path) {
		this.configuration = configuration;
		this.goldenGate = gg;
		
		//	get settings
		Settings set = configuration.getSettings();
		
		//	create page image store
		String pageImageFolderName = set.getSetting("pageImageFolder", "PageImages");
		final File pageImageFolder;
		if (pageImageFolderName.startsWith("/") || (pageImageFolderName.indexOf(':') != -1))
			pageImageFolder = new File(pageImageFolderName);
		else pageImageFolder = new File(path, pageImageFolderName);
		if (!pageImageFolder.exists())
			pageImageFolder.mkdirs();
		this.pageImageStore = new AbstractPageImageStore() {
			public boolean isPageImageAvailable(String name) {
				if (!name.endsWith(IMAGE_FORMAT))
					name += ("." + IMAGE_FORMAT);
				File pif = new File(pageImageFolder, name);
				return pif.exists();
			}
			public PageImageInputStream getPageImageAsStream(String name) throws IOException {
				if (!name.endsWith(IMAGE_FORMAT))
					name += ("." + IMAGE_FORMAT);
				File pif = new File(pageImageFolder, name);
				if (pif.exists())
					return new PageImageInputStream(new BufferedInputStream(new FileInputStream(pif)), this);
				else return null;
			}
			public boolean storePageImage(String name, PageImage pageImage) throws IOException {
				if (!name.endsWith(IMAGE_FORMAT))
					name += ("." + IMAGE_FORMAT);
				try {
					File pif = new File(pageImageFolder, name);
					if (pif.exists()) {
						String pifName = pif.getAbsolutePath();
						pif.renameTo(new File(pifName + "." + System.currentTimeMillis() + ".old"));
						pif = new File(pifName);
					}
					OutputStream imageOut = new BufferedOutputStream(new FileOutputStream(pif));
					pageImage.write(imageOut);
					imageOut.close();
					return true;
				}
				catch (IOException ioe) {
					ioe.printStackTrace(System.out);
					return false;
				}
			}
			public int getPriority() {
				return 0; // we're a general page image store, yield to more specific ones
			}
		};
		PageImage.addPageImageSource(this.pageImageStore);
		
		//	create PDF reader caching supplements on disc
		String supplementImageFolderName = set.getSetting("supplementImageFolder", "SupplementImages");
		final File supplementImageFolder;
		if (supplementImageFolderName.startsWith("/") || (supplementImageFolderName.indexOf(':') != -1))
			supplementImageFolder = new File(supplementImageFolderName);
		else supplementImageFolder = new File(path, supplementImageFolderName);
		if (!supplementImageFolder.exists())
			supplementImageFolder.mkdirs();
		this.pdfExtractor = new PdfExtractor(path, this.pageImageStore, true) {
			protected ImDocument createDocument(String docId) {
				return new ImDocument(docId) {
					private long inMemorySupplementBytes = 0;
					public ImSupplement addSupplement(ImSupplement ims) {
						
						//	store known type supplements on disc if there are too many or too large
						if ((ims instanceof ImSupplement.Figure) || (ims instanceof ImSupplement.Scan) || (ims instanceof ImSupplement.Source)) try {
							
							//	threshold already exceeded, disc cache right away
							if (this.inMemorySupplementBytes > maxInMemoryImageSupplementBytes)
								ims = this.createDiscSupplement(ims, null);
							
							//	still below threshold, check source
							else {
								InputStream sis = ims.getInputStream();
								
								//	this one resides in memory, count it
								if (sis instanceof ByteArrayInputStream)
									this.inMemorySupplementBytes += sis.available();
								
								//	threshold just exceeded
								if (this.inMemorySupplementBytes > maxInMemoryImageSupplementBytes) {
									
									//	disc cache all existing image supplements
									ImSupplement[] imss = this.getSupplements();
									for (int s = 0; s < imss.length; s++) {
										if ((imss[s] instanceof ImSupplement.Figure) || (imss[s] instanceof ImSupplement.Scan))
											super.addSupplement(this.createDiscSupplement(imss[s], null));
									}
									
									//	disc cache argument supplement
									ims = this.createDiscSupplement(ims, sis);
								}
							}
						}
						catch (IOException ioe) {
							System.out.println("Error caching supplement '" + ims.getId() + "': " + ioe.getMessage());
							ioe.printStackTrace(System.out);
						}
						
						//	store (possibly modified) supplement
						return super.addSupplement(ims);
					}
					
					private ImSupplement createDiscSupplement(ImSupplement ims, InputStream sis) throws IOException {
						
						//	get input stream if not already done
						if (sis == null)
							sis = ims.getInputStream();
						
						//	this one's not in memory, close input stream and we're done
						if (!(sis instanceof ByteArrayInputStream)) {
							sis.close();
							return ims;
						}
						
						//	get file name and extension
						String sDataName = ims.getId().replaceAll("[^a-zA-Z0-9]", "_");
						String sDataType = ims.getMimeType();
						if (sDataType.indexOf('/') != -1)
							sDataType = sDataType.substring(sDataType.indexOf('/') + "/".length());
						
						//	create file
						final File sFile = new File(supplementImageFolder, (this.docId + "." + sDataName + "." + sDataType));
						
						//	store supplement in file (if not done in previous run)
						if (!sFile.exists()) {
							sFile.createNewFile();
							OutputStream sos = new BufferedOutputStream(new FileOutputStream(sFile));
							byte[] sBuffer = new byte[1024];
							for (int r; (r = sis.read(sBuffer, 0, sBuffer.length)) != -1;)
								sos.write(sBuffer, 0, r);
							sos.flush();
							sos.close();
						}
						
						//	replace supplement with disc based one
						if (ims instanceof ImSupplement.Figure)
							return new ImSupplement.Figure(this, ims.getMimeType(), ((ImSupplement.Figure) ims).getPageId(), ((ImSupplement.Figure) ims).getDpi(), ((ImSupplement.Figure) ims).getBounds()) {
								public InputStream getInputStream() throws IOException {
									return new BufferedInputStream(new FileInputStream(sFile));
								}
							};
						else if (ims instanceof ImSupplement.Scan)
							return new ImSupplement.Scan(this, ims.getMimeType(), ((ImSupplement.Scan) ims).getPageId(), ((ImSupplement.Scan) ims).getDpi()) {
								public InputStream getInputStream() throws IOException {
									return new BufferedInputStream(new FileInputStream(sFile));
								}
							};
						else if (ims instanceof ImSupplement.Source)
							return new ImSupplement.Source(this, ims.getMimeType()) {
								public InputStream getInputStream() throws IOException {
									return new BufferedInputStream(new FileInputStream(sFile));
								}
							};
						else return ims; // never gonna happen, but Java don't know
					}
				};
			}
		};
		
		//	get and index applicable plugins (only now, as instance proper is fully initialized)
		GoldenGatePlugin[] ggps = this.goldenGate.getPlugins();
		for (int p = 0; p < ggps.length; p++) {
			if (ggps[p] instanceof GoldenGateImaginePlugin) {
				((GoldenGateImaginePlugin) ggps[p]).setImagineParent(this);
				((GoldenGateImaginePlugin) ggps[p]).initImagine();
			}
			if (ggps[p] instanceof ImageEditToolProvider)
				this.registerImageEditToolProvider((ImageEditToolProvider) ggps[p]);
			if (ggps[p] instanceof ImageMarkupToolProvider)
				this.registerImageMarkupToolProvider((ImageMarkupToolProvider) ggps[p]);
			if (ggps[p] instanceof SelectionActionProvider)
				this.registerSelectionActionProvider((SelectionActionProvider) ggps[p]);
			if (ggps[p] instanceof ImageDocumentDropHandler)
				this.registerDropHandler((ImageDocumentDropHandler) ggps[p]);
			if (ggps[p] instanceof ImageDocumentExporter)
				this.registerDocumentExporter((ImageDocumentExporter) ggps[p]);
			if (ggps[p] instanceof ReactionProvider)
				this.registerReactionProvider((ReactionProvider) ggps[p]);
			if (ggps[p] instanceof GoldenGateImagineDocumentListener)
				this.registerDocumentListener((GoldenGateImagineDocumentListener) ggps[p]);
		}
	}
	
	/**
	 * Retrieve the PDF Extractor embedded in this GoldenGATE Imagine instance.
	 * @return the PDF Extractor
	 */
	public PdfExtractor getPdfExtractor() {
		return this.pdfExtractor;
	}
	
	/**
	 * Retrieve the OCR Engine embedded in this GoldenGATE Imagine instance.
	 * @return the OCR Engine
	 */
	public OcrEngine getOcrEngine() {
		return this.pdfExtractor.getOcrEngine();
	}
	
	/**
	 * @return the GoldenGATE icon as provided by the current configuration
	 */
	public Image getGoldenGateIcon() {
		return this.goldenGate.getGoldenGateIcon();
	}
	
	/**
	 * @return the name of the configuration wrapped in this GoldenGATE Imagine instance
	 */
	public String getConfigurationName() {
		return this.goldenGate.getConfigurationName();
	}
	
	/**
	 * @return the path of the configuration wrapped in this GoldenGATE Imagine instance,
	 *         relative to the root path of the surrounding GoldenGATE
	 *         installation
	 */
	public String getConfigurationPath() {
		return this.goldenGate.getConfigurationPath();
	}
	
	/**
	 * @return a data provider pointing to the help path of the configuration
	 *         wrapped in this GoldenGATE instance
	 */
	public GoldenGatePluginDataProvider getHelpDataProvider() {
		return this.configuration.getHelpDataProvider();
	}
	
	//	register and lookup method for drop handlers
	private HashMap dropHandlersByClassName = new LinkedHashMap();
	
	private void registerDropHandler(ImageDocumentDropHandler iddh) {
		if (iddh != null)
			this.dropHandlersByClassName.put(iddh.getClass().getName(), iddh);
	}
	
	/**
	 * Find a document drop handler by its class name.
	 * @param pluginClassName the class name of the desired drop handler
	 * @return the drop handler with the specified class name
	 */
	public ImageDocumentDropHandler getDropHandler(String pluginClassName) {
		return ((ImageDocumentDropHandler) this.dropHandlersByClassName.get(pluginClassName));
	}
	
	/**
	 * Get all drop handlers that are currently available.
	 * @return an array holding all drop handlers registered
	 */
	public ImageDocumentDropHandler[] getDropHandlers() {
		ArrayList iddhs = new ArrayList(this.dropHandlersByClassName.values());
		return ((ImageDocumentDropHandler[]) iddhs.toArray(new ImageDocumentDropHandler[iddhs.size()]));
	}
	
	//	register and lookup method for document exporters
	private HashMap documentExportersByClassName = new LinkedHashMap();
	
	private void registerDocumentExporter(ImageDocumentExporter ide) {
		if (ide != null)
			this.documentExportersByClassName.put(ide.getClass().getName(), ide);
	}
	
	/**
	 * Find a document exporter by its class name.
	 * @param pluginClassName the class name of the desired exporter
	 * @return the exporter with the specified class name
	 */
	public ImageDocumentExporter getDocumentExporter(String pluginClassName) {
		return ((ImageDocumentExporter) this.documentExportersByClassName.get(pluginClassName));
	}
	
	/**
	 * Get all document exporters that are currently available.
	 * @return an array holding all document exporters registered
	 */
	public ImageDocumentExporter[] getDocumentExporters() {
		ArrayList ides = new ArrayList(this.documentExportersByClassName.values());
		return ((ImageDocumentExporter[]) ides.toArray(new ImageDocumentExporter[ides.size()]));
	}
	
	//	register and lookup method for image edit tool providers
	private HashMap imageEditToolProvidersByClassName = new LinkedHashMap();
	
	private void registerImageEditToolProvider(ImageEditToolProvider ietp) {
		if (ietp  != null)
			this.imageEditToolProvidersByClassName.put(ietp.getClass().getName(), ietp);
	}
	
	/**
	 * Find an image edit tool provider by its class name.
	 * @param pluginClassName the class name of the desired image edit tool provider
	 * @return the image edit tool provider with the specified class name
	 */
	public ImageEditToolProvider getImageEditToolProvider(String pluginClassName) {
		return ((ImageEditToolProvider) this.imageEditToolProvidersByClassName.get(pluginClassName));
	}
	
	/**
	 * Get all image edit tool providers that are currently available.
	 * @return an array holding all image edit tool providers registered
	 */
	public ImageEditToolProvider[] getImageEditToolProviders() {
		ArrayList ietps = new ArrayList(this.imageEditToolProvidersByClassName.values());
		return ((ImageEditToolProvider[]) ietps.toArray(new ImageEditToolProvider[ietps.size()]));
	}
	
	//	register and lookup method for image edit tool providers
	private HashMap imageMarkupToolProvidersByClassName = new LinkedHashMap();
	
	private void registerImageMarkupToolProvider(ImageMarkupToolProvider imtp) {
		if (imtp != null)
			this.imageMarkupToolProvidersByClassName.put(imtp.getClass().getName(), imtp);
	}
	
	/**
	 * Get all image markup tool providers that are currently available.
	 * @return an array holding all image markup tool providers registered
	 */
	public ImageMarkupToolProvider[] getImageMarkupToolProviders() {
		ArrayList imtps = new ArrayList(this.imageMarkupToolProvidersByClassName.values());
		return ((ImageMarkupToolProvider[]) imtps.toArray(new ImageMarkupToolProvider[imtps.size()]));
	}
	
	/**
	 * Find an image markup tool provider by its class name.
	 * @param pluginClassName the class name of the desired image markup tool provider
	 * @return the image markup tool provider with the specified class name
	 */
	public ImageMarkupToolProvider getImageMarkupToolProvider(String pluginClassName) {
		return ((ImageMarkupToolProvider) this.imageMarkupToolProvidersByClassName.get(pluginClassName));
	}
	
	//	register and lookup method for selection action providers
	private HashMap selectionActionProvidersByClassName = new LinkedHashMap();
	
	private void registerSelectionActionProvider(SelectionActionProvider sap) {
		if (sap != null)
			this.selectionActionProvidersByClassName.put(sap.getClass().getName(), sap);
	}
	
	/**
	 * Retrieve an image markup tool by its name. The name may be fully
	 * qualified, i.e., include the providerClassName, but need not. In the
	 * latter case, all image markup tool providers will be asked for an image
	 * markup tool with the specified name, and the first one found will be
	 * returned.
	 * @param name the name of the image markup tool
	 * @return the image markup tool with the specified name, or null, if there
	 *         is no such image markup tool
	 */
	public ImageMarkupTool getImageMarkupToolForName(String name) {
		int nameSplit = ((name == null) ? -1 : name.indexOf('@'));
		if ((nameSplit == -1) || ((nameSplit + 1) == name.length()))
			return this.getImageMarkupToolForName(name, null);
		else return this.getImageMarkupToolForName(name.substring(0, nameSplit), name.substring(nameSplit + 1));
	}
	
	/**
	 * Retrieve an image markup tool by its name. The providerClassName may be
	 * null. In this latter case, all image markup tool providers will be asked
	 * for an image markup tool with the specified name, and the first one found
	 * will be returned.
	 * @param name the name of the image markup tool
	 * @param providerClassName the class name of the desired image markup tool
	 *            provider to ask for the image markup tool
	 * @return the image markup tool with the specified name, or null, if there
	 *         is no such image markup tool
	 */
	public ImageMarkupTool getImageMarkupToolForName(String name, String providerClassName) {
		if (providerClassName == null) {
			ImageMarkupToolProvider[] imtps = this.getImageMarkupToolProviders();
			for (int p = 0; p < imtps.length; p++) {
				ImageMarkupTool imt = imtps[p].getImageMarkupTool(name);
				if (imt != null) return imt;
			}
			return null;
		}
		else {
			ImageMarkupToolProvider imtp = this.getImageMarkupToolProvider(providerClassName);
			return ((imtp == null) ? null : imtp.getImageMarkupTool(name));
		}
	}
	
	/**
	 * Find a GoldenGatePlugin by its class name.
	 * @param pluginClassName the class name of the desired GoldenGatePlugin
	 * @return the GoldenGatePlugin with the specified class name
	 */
	public SelectionActionProvider getSelectionActionProvider(String pluginClassName) {
		return ((SelectionActionProvider) this.selectionActionProvidersByClassName.get(pluginClassName));
	}
	
	/**
	 * Get all GoldenGatePlugins that are currently available.
	 * @return an array holding all GoldenGatePlugins registered
	 */
	public SelectionActionProvider[] getSelectionActionProviders() {
		ArrayList saps = new ArrayList(this.selectionActionProvidersByClassName.values());
		return ((SelectionActionProvider[]) saps.toArray(new SelectionActionProvider[saps.size()]));
	}
	
	//	register and lookup method for reaction providers
	private HashMap reactionProvidersByClassName = new LinkedHashMap();
	
	private void registerReactionProvider(ReactionProvider rp) {
		if (rp != null)
			this.reactionProvidersByClassName.put(rp.getClass().getName(), rp);
	}
	
	/**
	 * Find a ReactionProvider by its class name.
	 * @param pluginClassName the class name of the desired reaction provider
	 * @return the reaction provider with the specified class name
	 */
	public ReactionProvider getReactionProvider(String pluginClassName) {
		return ((ReactionProvider) this.reactionProvidersByClassName.get(pluginClassName));
	}
	
	/**
	 * Get all ReactionProvider that are currently available.
	 * @return an array holding all reaction providers registered
	 */
	public ReactionProvider[] getReactionProviders() {
		ArrayList rps = new ArrayList(this.reactionProvidersByClassName.values());
		return ((ReactionProvider[]) rps.toArray(new ReactionProvider[rps.size()]));
	}
	
	//	register for document listeners
	private ArrayList documentListeners = new ArrayList();
	
	private void registerDocumentListener(GoldenGateImagineDocumentListener ggidl) {
		if (ggidl != null)
			this.documentListeners.add(ggidl);
	}
	
	/**
	 * Get all document listeners that are currently available. This getter is
	 * mainly intended for applications that prefer to implement their own
	 * notification mechanisms instead of using the ones provided by this class.
	 * @return an array holding all document listeners registered
	 */
	public GoldenGateImagineDocumentListener[] getDocumentListeners() {
		return ((GoldenGateImagineDocumentListener[]) this.documentListeners.toArray(new GoldenGateImagineDocumentListener[this.documentListeners.size()]));
	}
	
	/**
	 * Notify all registered document listeners that an Image Markup document
	 * has been opened in an application built around this GoldenGATE Imagine
	 * core. This method should be called by client code right after an Image
	 * Markup document has been loaded.
	 * @param doc the document that was opened
	 */
	public void notifyDocumentOpened(ImDocument doc) {
		for (int l = 0; l < this.documentListeners.size(); l++)
			((GoldenGateImagineDocumentListener) this.documentListeners.get(l)).documentOpened(doc);
	}
	
	/**
	 * Notify all registered document listeners that an Image Markup document
	 * is about to be saved to persistent storage in an application built
	 * around this GoldenGATE Imagine core. This method should only be called
	 * if the Image Markup document is stored as such, rather than exported in
	 * another format.
	 * @param doc the document that is about to be saved
	 */
	public void notifyDocumentSaving(ImDocument doc) {
		for (int l = 0; l < this.documentListeners.size(); l++)
			((GoldenGateImagineDocumentListener) this.documentListeners.get(l)).documentSaving(doc);
	}
	
	/**
	 * Notify all registered document listeners that an Image Markup document
	 * has been saved to persistent storage in an application built around this
	 * GoldenGATE Imagine core. This method should only be called if the Image
	 * Markup document has been stored as such, rather than exported in another
	 * format, and only if the saving process has completed successfully.
	 * @param doc the document that has been saved
	 */
	public void notifyDocumentSaved(ImDocument doc) {
		for (int l = 0; l < this.documentListeners.size(); l++)
			((GoldenGateImagineDocumentListener) this.documentListeners.get(l)).documentSaved(doc);
	}
	
	/**
	 * Notify all registered document listeners that an Image Markup document
	 * has been saved to persistent storage in an application built around this
	 * GoldenGATE Imagine core. This method should be called by client code
	 * right after the document is disposed.
	 * @param docId the ID of the document that was closed
	 */
	public void notifyDocumentClosed(String docId) {
		for (int l = 0; l < this.documentListeners.size(); l++)
			((GoldenGateImagineDocumentListener) this.documentListeners.get(l)).documentClosed(docId);
	}
	
	/**
	 * Find a GoldenGatePlugin by its class name.
	 * @param pluginClassName the class name of the desired GoldenGatePlugin
	 * @return the GoldenGatePlugin with the specified class name
	 */
	public GoldenGatePlugin getPlugin(String pluginClassName) {
		return this.goldenGate.getPlugin(pluginClassName);
	}
	
	/**
	 * Get all GoldenGatePlugins that are currently available.
	 * @return an array holding all GoldenGatePlugins registered
	 */
	public GoldenGatePlugin[] getPlugins() {
		return this.goldenGate.getPlugins();
	}
	
	/**
	 * Find a DocumentEditorExtension by its class name.
	 * @param extensionClassName the class name of the desired
	 *            DocumentEditorExtension
	 * @return the DocumentEditorExtension with the specified class name
	 */
	public DocumentEditorExtension getDocumentEditorExtension(String extensionClassName) {
		return this.goldenGate.getDocumentEditorExtension(extensionClassName);
	}
	
	/**
	 * Get all DocumentEditorExtensions that are currently available.
	 * @return an array holding all DocumentEditorExtensions registered
	 */
	public DocumentEditorExtension[] getDocumentEditorExtensions() {
		return this.goldenGate.getDocumentEditorExtensions();
	}
	
	/**
	 * Find a ResourceManager by its class name (especially useful to find the
	 * provider of a Resource).
	 * @param providerClassName the class name of the desired ResourceProvider
	 * @return the ResourceManager with the specified class name
	 */
	public ResourceManager getResourceProvider(String providerClassName) {
		return this.goldenGate.getResourceProvider(providerClassName);
	}
	
	/**
	 * Get all ResourceManagers that are currently available.
	 * @return an array holding all ResourceManager registered
	 */
	public ResourceManager[] getResourceProviders() {
		return this.goldenGate.getResourceProviders();
	}
	
	/**
	 * Find a DocumentProcessorManager by its class name (especially useful to
	 * find the provider of a DocumentProcessor).
	 * @param providerClassName the class name of the desired
	 *            DocumentProcessorProvider
	 * @return the DocumentProcessorManager with the specified class name
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#getProviderClassName()
	 */
	public DocumentProcessorManager getDocumentProcessorProvider(String providerClassName) {
		return this.goldenGate.getDocumentProcessorProvider(providerClassName);
	}
	
	/**
	 * Get all DocumentProcessorManagers currently available.
	 * @return an array holding all DocumentProcessorManagers registered
	 */
	public DocumentProcessorManager[] getDocumentProcessorProviders() {
		return this.goldenGate.getDocumentProcessorProviders();
	}
	
	/**
	 * Retrieve a document processor by its name. The name may be fully
	 * qualified, i.e., include the providerClassName, but need not. In the
	 * latter case, all document processor managers will be asked for a document
	 * processor with the specified name, and the first one found will be
	 * returned.
	 * @param name the name of the document processor
	 * @return the document processor with the specified name, or null, if there
	 *         is no such document processor
	 */
	public DocumentProcessor getDocumentProcessorForName(String name) {
		return this.goldenGate.getDocumentProcessorForName(name);
	}
	
	/**
	 * Retrieve a document processor by its name. The providerClassName may be
	 * null. In this latter case, all document processor managers will be asked
	 * for a document processor with the specified name, and the first one found
	 * will be returned.
	 * @param name the name of the document processor
	 * @param providerClassName the class name of the desired document processor
	 *            manager to ask for the document processor
	 * @return the document processor with the specified name, or null, if there
	 *         is no such document processor
	 */
	public DocumentProcessor getDocumentProcessorForName(String name, String providerClassName) {
		return this.goldenGate.getDocumentProcessorForName(name, providerClassName);
	}
	
	/**
	 * Find a AnnotationSourceManager by its class name (especially useful to
	 * find the provider of a AnnotationSource).
	 * @param providerClassName the class name of the desired
	 *            AnnotationSourceProvider
	 * @return the AnnotationSourceManager with the specified class name
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource#getProviderClassName()
	 */
	public AnnotationSourceManager getAnnotationSourceProvider(String providerClassName) {
		return this.goldenGate.getAnnotationSourceProvider(providerClassName);
	}
	
	/**
	 * Get all AnnotationSourceManagers currently available.
	 * @return an array holding all AnnotationSourceManagers registered
	 */
	public AnnotationSourceManager[] getAnnotationSourceProviders() {
		return this.goldenGate.getAnnotationSourceProviders();
	}
	
	/**
	 * Retrieve an annotation source by its name. The name may be fully
	 * qualified, i.e., include the providerClassName, but need not. In the
	 * latter case, all annotation source managers will be asked for a
	 * annotation source with the specified name, and the first one found will
	 * be returned.
	 * @param name the name of the annotation source
	 * @return the annotation source with the specified name, or null, if there
	 *         is no such annotation source
	 */
	public AnnotationSource getAnnotationSourceForName(String name) {
		return this.goldenGate.getAnnotationSourceForName(name);
	}
	
	/**
	 * Retrieve a annotation source by its name. The providerClassName may be
	 * null. In this latter case, all annotation source managers will be asked
	 * for a annotation source with the specified name, and the first one found
	 * will be returned.
	 * @param name the name of the annotation source
	 * @param providerClassName the class name of the desired annotation source
	 *            manager to ask for the annotation source
	 * @return the annotation source with the specified name, or null, if there
	 *         is no such annotation source
	 */
	public AnnotationSource getAnnotationSourceForName(String name, String providerClassName) {
		return this.goldenGate.getAnnotationSourceForName(name, providerClassName);
	}
	
	/**
	 * Find a AnnotationFilterManager by its class name (especially useful to
	 * find the provider of a AnnotationFilters).
	 * @param providerClassName the class name of the desired
	 *            AnnotationFilterManager
	 * @return the AnnotationFilterManager with the specified class name
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AnnotationSource#getProviderClassName()
	 */
	public AnnotationFilterManager getAnnotationFilterProvider(String providerClassName) {
		return this.goldenGate.getAnnotationFilterProvider(providerClassName);
	}
	
	/**
	 * Get all AnnotationFilterManagers currently available.
	 * @return an array holding all AnnotationFilterManagers registered
	 */
	public AnnotationFilterManager[] getAnnotationFilterProviders() {
		return this.goldenGate.getAnnotationFilterProviders();
	}
	
	/**
	 * Retrieve an annotation filter by its name. The name may be fully
	 * qualified, i.e., include the providerClassName, but need not. In the
	 * latter case, all annotation filter managers will be asked for a
	 * annotation filter with the specified name, and the first one found will
	 * be returned.
	 * @param name the name of the annotation filter
	 * @return the annotation filter with the specified name, or null, if there
	 *         is no such annotation filter
	 */
	public AnnotationFilter getAnnotationFilterForName(String name) {
		return this.goldenGate.getAnnotationFilterForName(name);
	}
	
	/**
	 * Retrieve a annotation filter by its name. The providerClassName may be
	 * null. In this latter case, all annotation filter managers will be asked
	 * for a annotation filter with the specified name, and the first one found
	 * will be returned.
	 * @param name the name of the annotation filter
	 * @param providerClassName the class name of the desired annotation source
	 *            manager to ask for the annotation filter
	 * @return the annotation filter with the specified name, or null, if there
	 *         is no such annotation filter
	 */
	public AnnotationFilter getAnnotationFilterForName(String name, String providerClassName) {
		return this.goldenGate.getAnnotationFilterForName(name, providerClassName);
	}
	
	/**
	 * Find a DocumentSaver by its class name (especially useful to find a
	 * DocumentSaver).
	 * @param saverClassName the class name of the desired DocumentSaver
	 * @return the DocumentSaver with the specified class name
	 */
	public DocumentSaver getDocumentSaver(String saverClassName) {
		return this.goldenGate.getDocumentSaver(saverClassName);
	}
	
	/**
	 * Get all DocumentSaver currently available.
	 * @return an array holding all DocumentSavers registered
	 */
	public DocumentSaver[] getDocumentSavers() {
		return this.goldenGate.getDocumentSavers();
	}
	
	/**
	 * Find a DocumentFormatProvider by its class name (especially useful to
	 * find a DocumentFormatProvider).
	 * @param formatterClassName the class name of the desired
	 *            DocumentFormatProvider
	 * @return the DocumentFormatProvider with the specified class name
	 */
	public DocumentFormatProvider getDocumentFormatProvider(String formatterClassName) {
		return this.goldenGate.getDocumentFormatProvider(formatterClassName);
	}
	
	/**
	 * Get all DocumentFormatProvider currently available.
	 * @return an array holding all DocumentFormatProviders registered
	 */
	public DocumentFormatProvider[] getDocumentFormatProviders() {
		return this.goldenGate.getDocumentFormatProviders();
	}
	
	/**
	 * Obtain the format for some specific file extension (search all available
	 * DocumentFormatProviders).
	 * @param fileExtension the file extension to obtain the DocumentFormat for
	 * @return the DocumentFormat for the specified file extension, or null, if
	 *         this DocumentFormatProvider does not provide a format for the
	 *         specified file extension
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForFileExtension(java.lang.String)
	 */
	public DocumentFormat getDocumentFormatForFileExtension(String fileExtension) {
		return this.goldenGate.getDocumentFormatForFileExtension(fileExtension);
	}
	
	/**
	 * Obtain a DocumentFormat by its name. The name may be fully
	 * qualified, i.e., include the providerClassName, but need not. In the
	 * latter case, all document format providers will be asked for a
	 * document format with the specified name, and the first one found will
	 * be returned.
	 * @param formatName the name of the desired DocumentFormat
	 * @return the DocumentFormat with the specified name, or null, if there is
	 *         no such DocumentFormat
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider#getFormatForName(java.lang.String)
	 */
	public DocumentFormat getDocumentFormatForName(String formatName) {
		return this.goldenGate.getDocumentFormatForName(formatName);
	}
	
	/**
	 * Obtain a DocumentFormat by its name. The providerClassName may be
	 * null. In this latter case, all document format providers will be asked for a
	 * document format with the specified name, and the first one found will
	 * be returned.
	 * @param formatName the name of the desired DocumentFormat
	 * @param providerClassName the class name of the desired document format provider to ask for the document format
	 * @return the document format with the specified name, or null, if there
	 *         is no such document format
	 */
	public DocumentFormat getDocumentFormatForName(String formatName, String providerClassName) {
		return this.goldenGate.getDocumentFormatForName(formatName, providerClassName);
	}
	
	/**
	 * Shut down the GoldenGATE Imagine instance.
	 */
	public void exit() {
		this.goldenGate.exitShutdown();
		this.pdfExtractor.shutdown();
		PageImage.removePageImageStore(this.pageImageStore);
	}
	
	/**
	 * Display an 'About' info box.
	 */
	public void showAbout() {
		StringVector aboutExtensions = new StringVector();
		GoldenGatePlugin[] plugins = this.getPlugins();
		for (int p = 0; p < plugins.length; p++)
			if (plugins[p] instanceof GoldenGateImaginePlugin) {
				String aboutPlugin = plugins[p].getAboutBoxExtension();
				if (aboutPlugin == null)
					continue;
				aboutPlugin = aboutPlugin.trim();
				if (aboutPlugin.length() == 0)
					continue;
				aboutExtensions.addElement("\n-------- " + plugins[p].getPluginName() + " --------");
				aboutExtensions.addElement(aboutPlugin);
			}
		aboutExtensions.addElement("\n-------- PDF and Page Image Handling --------");
		aboutExtensions.addElement("IcePDF is open source software by Icesoft Technologies Inc.\n" +
			"The Tesseract OCR engine is open source software by Google Inc.\n" +
			"   formerly by Hewlett-Packard Company\n" +
			"ImageMagick is open source software by ImageMagick Studio LLC");
		JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), (ABOUT_TEXT + (aboutExtensions.isEmpty() ? "" : ("\n" + aboutExtensions.concatStrings("\n")))), "About GoldenGATE Imagine", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(this.getGoldenGateIcon()));
	}
	
	/**
	 * Display the 'README.txt' of the current configuration.
	 */
	public void showReadme() {
		StringVector readme;
		try {
			InputStream ris = this.configuration.getInputStream(README_FILE_NAME);
			readme = StringVector.loadList(ris);
			ris.close();
		}
		catch (IOException ioe) {
			readme = new StringVector();
			readme.addElement("An error occurred loading the readme file: " + ioe.getMessage());
			StackTraceElement[] stes = ioe.getStackTrace();
			for (int s = 0; s < stes.length; s++)
				readme.addElement(stes[s].toString());
		}
		
		final DialogPanel readmeDialog = new DialogPanel(("GoldenGATE Imagine - " + this.getConfigurationName() + " - " + README_FILE_NAME), true);
		
		final JTextArea readmeDisplay = new JTextArea();
		readmeDisplay.setEditable(false);
		readmeDisplay.setLineWrap(true);
		readmeDisplay.setWrapStyleWord(true);
		readmeDisplay.setFont(new Font(DocumentEditor.getDefaultTextFontName(), Font.PLAIN, DocumentEditor.getDefaultTextFontSize()));
		readmeDisplay.setText(readme.concatStrings("\n"));
		
		final JScrollPane readmeDisplayBox = new JScrollPane(readmeDisplay);
		
		JButton okButton = new JButton("OK");
		okButton.setBorder(BorderFactory.createRaisedBevelBorder());
		okButton.setPreferredSize(new Dimension(70, 21));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				readmeDialog.dispose();
			}
		});
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(okButton);
		
		readmeDialog.add(readmeDisplayBox, BorderLayout.CENTER);
		readmeDialog.add(buttonPanel, BorderLayout.SOUTH);
		
		readmeDialog.setSize(800, 600);
		readmeDialog.setLocationRelativeTo(readmeDialog.getOwner());
		readmeDialog.setResizable(true);
		
		readmeDialog.getDialog().addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent we) {
				readmeDisplayBox.getVerticalScrollBar().setValue(0);
			}
		});
		
		readmeDialog.setVisible(true);
	}
	
	/**
	 * Create an instance of the GoldenGATE Imagine core with a specific
	 * configuration.
	 * @param configuration the GoldenGateConfiguration to use
	 * @param path the base path of the GoldenGATE Imagine installation
	 * @param showStatus use a splash screen for monitoring startup status? If
	 *            set to false, or if the JVM is headless, the status
	 *            information will go to System.out instead.
	 * @return a new GoldenGATE Imagine instance to work with the specified
	 *         configuration
	 */
	public static synchronized GoldenGateImagine openGoldenGATE(GoldenGateConfiguration configuration, File path, boolean showStatus) throws IOException {
		return new GoldenGateImagine(configuration, GoldenGATE.openGoldenGATE(configuration, false, showStatus), path);
	}
	
	/**
	 * Encode a color in its hexadecimal RGB representation.
	 * @param color the color to encode
	 * @return the hexadecimal RGB representation of the argument color
	 */
	public static String getHex(Color color) {
		return ("" +
				getHex(color.getRed()) + 
				getHex(color.getGreen()) +
				getHex(color.getBlue()) +
				"");
	}
	
	private static final String getHex(int i) {
		int high = (i >>> 4) & 15;
		int low = i & 15;
		String hex = "";
		if (high < 10) hex += ("" + high);
		else hex += ("" + ((char) ('A' + (high - 10))));
		if (low < 10) hex += ("" + low);
		else hex += ("" +  ((char) ('A' + (low - 10))));
		return hex;
	}
	
	/**
	 * Decode a color from its hexadecimal RGB representation.
	 * @param rgb the hexadecimal RGB representation to decode
	 * @return the color corresponding to the argument hexadecimal RGB
	 *         representation
	 */
	public static Color getColor(String rgb) {
		if (rgb.length() == 3) return readHexRGB(rgb.substring(0, 1), rgb.substring(1, 2), rgb.substring(2, 3));
		else if (rgb.length() == 6) return readHexRGB(rgb.substring(0, 2), rgb.substring(2, 4), rgb.substring(4, 6));
		else return null;
	}
	
	private static final Color readHexRGB(String red, String green, String blue) {
		return new Color(
				translateString(red),
				translateString(green),
				translateString(blue)
			);
	}
	
	private static final int translateString(String s) {
		if (s.length() == 0)
			return 0;
		
		int v = 0;
		v += translateChar(s.charAt(0));
		v <<= 4;
		v += translateChar(s.charAt((s.length() > 1) ? 1 : 0));
		return v;
	}
	
	private static final int translateChar(char c) {
		if (('0' <= c) && (c <= '9')) return (((int) c) - '0');
		else if (('a' <= c) && (c <= 'f')) return (((int) c) - 'a' + 10);
		else if (('A' <= c) && (c <= 'F')) return (((int) c) - 'A' + 10);
		else return 0;
	}
}