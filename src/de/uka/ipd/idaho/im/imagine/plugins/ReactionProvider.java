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

import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImObject;
import de.uka.ipd.idaho.im.ImRegion;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;

/**
 * Reactive plugins receive notification of user edits to a document and can
 * take further measures in response.
 * 
 * @author sautter
 */
public interface ReactionProvider extends GoldenGateImaginePlugin {
	
	/**
	 * Notify the reactive plugin that the type of an Image Markup object has
	 * changed.
	 * @param object the object whose type changed
	 * @param oldType the old type of the object
	 * @param idmp the document editor panel the change occurred in
	 * @param allowPrompt got permission to prompt user?
	 */
	public abstract void typeChanged(ImObject object, String oldType, ImDocumentMarkupPanel idmp, boolean allowPrompt);
	
	/**
	 * Notify the reactive plugin that an attribute has changed in an Image
	 * Markup object. The affected attribute can also be a functional pseudo
	 * attribute, like the predecessor or successor of Image Markup words.
	 * @param object the object whose attribute changed
	 * @param attributeName the name of the attribute
	 * @param oldValue the old value of the attribute, which was just
	 *        replaced
	 * @param idmp the document editor panel the change occurred in
	 * @param allowPrompt got permission to prompt user?
	 */
	public abstract void attributeChanged(ImObject object, String attributeName, Object oldValue, ImDocumentMarkupPanel idmp, boolean allowPrompt);
	
	/**
	 * Notify the reactive plugin that a region has been added. The runtime
	 * type of the argument region can also be an Image Markup word or page.
	 * @param region the region that was just added
	 * @param idmp the document editor panel the change occurred in
	 * @param allowPrompt got permission to prompt user?
	 */
	public abstract void regionAdded(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt);
	
	/**
	 * Notify the reactive plugin that a region has been removed. The runtime
	 * type of the argument region can also be an Image Markup word.
	 * @param region the region that was just removed
	 * @param idmp the document editor panel the change occurred in
	 * @param allowPrompt got permission to prompt user?
	 */
	public abstract void regionRemoved(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt);
	
	/**
	 * Notify the reactive plugin that an annotation has been added.
	 * @param annotation the annotation that was just added
	 * @param idmp the document editor panel the change occurred in
	 * @param allowPrompt got permission to prompt user?
	 */
	public abstract void annotationAdded(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt);
	
	/**
	 * Notify the reactive plugin that an annotation has been removed.
	 * @param region the annotation that was just removed
	 * @param idmp the document editor panel the change occurred in
	 * @param allowPrompt got permission to prompt user?
	 */
	public abstract void annotationRemoved(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt);
}
