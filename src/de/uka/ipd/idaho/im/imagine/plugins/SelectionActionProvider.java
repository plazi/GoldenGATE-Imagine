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

import java.awt.Point;

import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel.SelectionAction;

/**
 * Provider of actions working on Image Markup documents, to dynamically extend
 * the image markup editor.
 * 
 * @author sautter
 */
public interface SelectionActionProvider extends GoldenGateImaginePlugin {
	
	/**
	 * Retrieve the available actions for a word selection. The argument editor
	 * panel is to provide the current configuration of the editing interface.
	 * @param start the word where the selection started
	 * @param end the point word the selection ended
	 * @param idmp the document editor panel to display the actions in
	 * @return an array holding the actions
	 */
	public abstract SelectionAction[] getActions(ImWord start, ImWord end, ImDocumentMarkupPanel idmp);
	
	//	TODO consider adding spanning and overlapping annotations as arguments ... no use, or higher effort, computing separately them in every instance of this class
	
	/**
	 * Retrieve the available actions for a box selection. The argument editor
	 * panel is to provide the current configuration of the editing interface.
	 * @param start the point where the selection started, one corner of the
	 *            box
	 * @param end the point where the selection ended, the opposite corner of
	 *            the box
	 * @param page the document page the selection belongs to
	 * @param idmp the document editor panel to display the actions in
	 * @return true if the document was changed by the method, false otherwise
	 */
	public abstract SelectionAction[] getActions(Point start, Point end, ImPage page, ImDocumentMarkupPanel idmp);
}