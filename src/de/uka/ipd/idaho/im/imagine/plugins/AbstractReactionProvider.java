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
 * Convenience abstract super class for reactive plugins, inheriting the life
 * cycle methods from abstract GoldenGATE plugin. This class further includes
 * default implementations of all the notification methods, sub classes need
 * to overwrite only those they require for providing meaningful functionality.
 * 
 * @author sautter
 */
public class AbstractReactionProvider extends AbstractGoldenGateImaginePlugin implements ReactionProvider {
	
	/** we need a zero-argument constructor for class loading */
	protected AbstractReactionProvider() {}
	
	/**
	 * This default implementation does nothing; sub classes are welcome to
	 * overwrite it as needed.
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#typeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void typeChanged(ImObject object, String oldType, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/**
	 * This default implementation does nothing; sub classes are welcome to
	 * overwrite it as needed.
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#attributeChanged(de.uka.ipd.idaho.im.ImObject, java.lang.String, java.lang.Object, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void attributeChanged(ImObject object, String attributeName, Object oldValue, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/**
	 * This default implementation does nothing; sub classes are welcome to
	 * overwrite it as needed.
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#regionAdded(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionAdded(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/**
	 * This default implementation does nothing; sub classes are welcome to
	 * overwrite it as needed.
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#regionRemoved(de.uka.ipd.idaho.im.ImRegion, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void regionRemoved(ImRegion region, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/**
	 * This default implementation does nothing; sub classes are welcome to
	 * overwrite it as needed.
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationAdded(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationAdded(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
	
	/**
	 * This default implementation does nothing; sub classes are welcome to
	 * overwrite it as needed.
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ReactionProvider#annotationRemoved(de.uka.ipd.idaho.im.ImAnnotation, de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, boolean)
	 */
	public void annotationRemoved(ImAnnotation annotation, ImDocumentMarkupPanel idmp, boolean allowPrompt) {}
}