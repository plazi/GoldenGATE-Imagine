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
package de.uka.ipd.idaho.im.imagine.plugins.configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager;
import de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.Configuration;
import de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.DataItem;
import de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.SpecialDataHandler;
import de.uka.ipd.idaho.goldenGate.configuration.FileConfiguration;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationFilterManager;
import de.uka.ipd.idaho.goldenGate.plugins.AnnotationSourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentEditorExtension;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormatProvider;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentSaver;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentViewer;
import de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceManager;
import de.uka.ipd.idaho.im.imagine.plugins.GoldenGateImaginePlugin;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Configuration manager for GoldenGATE Imagine. This configuration exporter
 * allows explicitly selecting only those plugins that can be used in
 * GoldenGATE Imagine. Transitive resolution of dependencies is not affected
 * by this restriction, however.
 * 
 * @author sautter
 */
public class GoldenGateImagineConfigurationManager extends AbstractConfigurationManager {
	
	/** public zero-argument constructor to facilitate class loading */
	public GoldenGateImagineConfigurationManager() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getPluginName()
	 */
	public String getPluginName() {
		return "IM Configuration Manager";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Imagine Configurations";
	}
	
	//	TODO add editor panel for GgImagine.cnfg
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager#getSelectablePluginClassNames()
	 */
	protected StringVector getSelectablePluginClassNames() {
		StringVector selectablePluginClassNames = new StringVector();
		GoldenGatePlugin[] plugins = this.parent.getPlugins();
		for (int p = 0; p < plugins.length; p++) {
			if (plugins[p] instanceof GoldenGateImaginePlugin) {
				selectablePluginClassNames.addElementIgnoreDuplicates(plugins[p].getClass().getName());
				continue;
			}
			if (plugins[p] instanceof DocumentSaver) {
				selectablePluginClassNames.addElementIgnoreDuplicates(plugins[p].getClass().getName());
				continue;
			}
			if (plugins[p] instanceof DocumentFormatProvider) {
				DocumentFormat[] sdfs = ((DocumentFormatProvider) plugins[p]).getSaveFileFilters();
				if ((sdfs != null) && (sdfs.length != 0))
					selectablePluginClassNames.addElementIgnoreDuplicates(plugins[p].getClass().getName());
				continue;
			}
			if (plugins[p] instanceof DocumentViewer)
				continue;
			if (plugins[p] instanceof DocumentEditorExtension)
				continue;
			if (plugins[p] instanceof DocumentProcessorManager)
				continue; // TODO consider using document processors, with normalization level 'paragraphs' or 'streams'
			if (plugins[p] instanceof AnnotationSourceManager)
				continue;
			if (plugins[p] instanceof AnnotationFilterManager)
				continue;
//			no use filtering these three, they are always selectable, in fact fixed to being selected
//			and configurations add the default implementations if they are missing
//			filtering out any custom functions, custom shortcuts, and tokenizers below
//			if (plugins[p] instanceof CustomFunction.Manager)
//				continue;
//			if (plugins[p] instanceof CustomShortcut.Manager)
//				continue;
//			if (plugins[p] instanceof TokenizerManager)
//				continue;
			if (plugins[p] instanceof ResourceManager) {
				selectablePluginClassNames.addElementIgnoreDuplicates(plugins[p].getClass().getName());
				continue;
			}
			if (plugins[p] instanceof GoldenGatePlugin) {
				selectablePluginClassNames.addElementIgnoreDuplicates(plugins[p].getClass().getName());
				continue;
			}
		}
		return selectablePluginClassNames;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager#adjustSelection(java.lang.String, de.uka.ipd.idaho.stringUtils.StringVector)
	 */
	protected void adjustSelection(String exportName, StringVector selected) {
		for (int s = 0; s < selected.size(); s++) {
			String selectedName = selected.get(s);
			if (selectedName.indexOf(".customFunction") != -1)
				selected.remove(s--);
			else if (selectedName.indexOf(".customShortcut") != -1)
				selected.remove(s--);
			else if (selectedName.indexOf(".tokenizer") != -1)
				selected.remove(s--);
		}
		super.adjustSelection(exportName, selected);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager#adjustConfiguration(java.lang.String, de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.Configuration)
	 */
	protected void adjustConfiguration(String exportName, Configuration config) {
		config.addDataItem(new DataItem("GgImagine.cnfg", config.configTimestamp));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager#getSpecialDataHandler(java.lang.String, de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.SpecialDataHandler)
	 */
	protected SpecialDataHandler getSpecialDataHandler(String exportName, final SpecialDataHandler sdh) {
		return new SpecialDataHandler() {
			public InputStream getInputStream(String dataName) throws IOException {
				if ("GgImagine.cnfg".equals(dataName))
					return dataProvider.getInputStream("GgImagine.cnfg");
				else return sdh.getInputStream(dataName);
			}
		};
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager#doExport(java.lang.String, de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.SpecialDataHandler, de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.Configuration, java.io.File, de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager.ExportStatusDialog)
	 */
	protected boolean doExport(String exportName, SpecialDataHandler specialData, Configuration config, File rootPath, ExportStatusDialog statusDialog) throws IOException {
		File ggRoot = this.getRootPath();
		if (ggRoot == null) return false;
		
		//	determine target folder
		File exportFolder = new File(new File(ggRoot, GoldenGateConstants.CONFIG_FOLDER_NAME), exportName);
		
		//	do export
		return FileConfiguration.createFileConfiguration(exportName, specialData, config, rootPath, exportFolder, statusDialog);
	}
}