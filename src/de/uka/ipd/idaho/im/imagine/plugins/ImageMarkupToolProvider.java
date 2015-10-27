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
package de.uka.ipd.idaho.im.imagine.plugins;

import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.ImageMarkupTool;

/**
 * A plugin to GoldenGATE Imagine that provides tools for working on Image
 * Markup documents as a whole, integrated in the user interface via the 'Edit'
 * or 'Tools' menu.
 * 
 * @author sautter
 */
public interface ImageMarkupToolProvider extends GoldenGateImaginePlugin {

	/**
	 * Retrieve the names of menu items to integrate functionality from this
	 * plugin in the 'Edit' menu of a GoldenGATE Imagine user interface. The
	 * labels and tooltips for names returned by this method will be retrieved
	 * from the <code>getLabel()</code> and <code>getTooltip()</code> methods,
	 * and the objects providing the actual functionality will be retrieved
	 * from the <code>getImageMarkupTool()</code> method.
	 * @return an array holding the names for entries in the 'Edit' menu
	 */
	public abstract String[] getEditMenuItemNames();
	
	/**
	 * Retrieve the names of menu items to integrate functionality from this
	 * plugin in the 'Tools' menu of a GoldenGATE Imagine user interface. The
	 * labels and tooltips for names returned by this method will be retrieved
	 * from the <code>getLabel()</code> and <code>getTooltip()</code> methods,
	 * and the objects providing the actual functionality will be retrieved
	 * from the <code>getImageMarkupTool()</code> method.
	 * @return an array holding the names for entries in the 'Tools' menu
	 */
	public abstract String[] getToolsMenuItemNames();
	
	/**
	 * Retrieve a markup tool with a given name.
	 * @param name the name of the markup tool
	 * @return the markup tool with the argument name
	 */
	public abstract ImageMarkupTool getImageMarkupTool(String name);
}