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

import java.awt.dnd.DropTargetDropEvent;

import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;

/**
 * Handler of the drop part of a drag &amp; drop operation occurring on an
 * Image Markup document.
 * 
 * @author sautter
 */
public interface ImageDocumentDropHandler extends GoldenGateImaginePlugin {
	
	/**
	 * Handle a drop event on an Image Markup document. The argument drop
	 * coordinates are relative to the argument page, and in the resolution
	 * of the page. Client code is responsible for translating whichever drop
	 * target location indicated in the drop event to this coordinate space. If
	 * an implementation handles the drop, it has to return <code>true</code>
	 * to indicate so, and client code should not consult any further drop
	 * handlers soon as one indicates having handled the drop.
	 * @param idmp the document markup panel the drop event occurred on
	 * @param page the page the drop occurred on
	 * @param pageX the X coordinate of the drop point in the argument page
	 * @param pageY the Y coordinate of the drop point in the argument page
	 * @param dtde the drop event to obtain further data from
	 * @return true to indicate the drop was handled, false otherwise
	 */
	public abstract boolean handleDrop(ImDocumentMarkupPanel idmp, ImPage page, int pageX, int pageY, DropTargetDropEvent dtde);
}