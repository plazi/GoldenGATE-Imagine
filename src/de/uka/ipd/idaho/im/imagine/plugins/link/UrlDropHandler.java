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
package de.uka.ipd.idaho.im.imagine.plugins.link;

import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.im.ImAnnotation;
import de.uka.ipd.idaho.im.ImPage;
import de.uka.ipd.idaho.im.ImWord;
import de.uka.ipd.idaho.im.imagine.plugins.AbstractGoldenGateImaginePlugin;
import de.uka.ipd.idaho.im.imagine.plugins.ImageDocumentDropHandler;
import de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel;

/**
 * This plugin receives URL drop events on Image Markup documents and adds the
 * dropped URL to annotations as a link.
 * 
 * @author sautter
 */
public class UrlDropHandler extends AbstractGoldenGateImaginePlugin implements ImageDocumentDropHandler {
	
	private TreeMap dropAcceptorsByType = new TreeMap();
	
	/** public zero-argument constructor for class loading */
	public UrlDropHandler() {}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "IM Drop Link Handler";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		try {
			
			//	load config
			Settings set = Settings.loadSettings(this.dataProvider.getInputStream("config.cnfg"));
			
			//	get annotation types
			String[] annotTypes = set.getSubsetPrefixes();
			
			//	load drop acceptors for each annotation type
			for (int t = 0; t < annotTypes.length; t++) {
				Settings atSet = set.getSubset(annotTypes[t]);
				String[] priorities = atSet.getSubsetPrefixes();
				if (priorities.length == 0)
					continue;
				TreeSet atDas = new TreeSet(new Comparator() {
					public int compare(Object obj1, Object obj2) {
						DropAcceptor da1 = ((DropAcceptor) obj1);
						DropAcceptor da2 = ((DropAcceptor) obj2);
						return (da1.priority - da2.priority);
					}
				});
				
				//	load individual drop acceptors
				for (int p = 0; p < priorities.length; p++) {
					Settings daSet = atSet.getSubset(priorities[p]);
					String attributeName = daSet.getSetting("attributeName");
					if (attributeName == null)
						continue;
					String mimeType = daSet.getSetting("mimeType");
					if (mimeType == null)
						continue;
					String filterPattern = daSet.getSetting("filterPattern");
					atDas.add(new DropAcceptor(Integer.parseInt(priorities[p]), annotTypes[t], mimeType, attributeName, filterPattern));
				}
				if (atDas.size() != 0)
					this.dropAcceptorsByType.put(annotTypes[t], atDas);
			}
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.im.imagine.plugins.ImageDocumentDropHandler#handleDrop(de.uka.ipd.idaho.im.util.ImDocumentMarkupPanel, de.uka.ipd.idaho.im.ImPage, int, int, java.awt.dnd.DropTargetDropEvent)
	 */
	public boolean handleDrop(ImDocumentMarkupPanel idmp, ImPage page, int pageX, int pageY, DropTargetDropEvent dtde) {
		
		//	get word at drop point
		ImWord imw = page.getWordAt(pageX, pageY);
		if (imw == null)
			return false;
		
		//	get annotations at drop point
		ImAnnotation[] annots = idmp.document.getAnnotationsSpanning(imw);
		if (annots.length == 0)
			return false;
		
		//	get transport and data flavors
		Transferable transfer = dtde.getTransferable();
		DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
		
		//	try annotations at drop point inmost out
		for (int a = (annots.length-1); a >= 0; a--) {
			
			//	we're not dropping on invisible annotations
			if (!idmp.areAnnotationsPainted(annots[a].getType()))
				continue;
			
			//	get drop acceptors for annotation
			TreeSet atDas = ((TreeSet) this.dropAcceptorsByType.get(annots[a].getType()));
			if (atDas == null)
				continue;
			
			//	got through drop acceptors
			for (Iterator dait = atDas.iterator(); dait.hasNext();) {
				DropAcceptor da = ((DropAcceptor) dait.next());
				for (int f = 0; f < dataFlavors.length; f++) try {
					
					//	test MIME type
					if (!da.mimeType.equalsIgnoreCase(dataFlavors[f].getMimeType()) && !dataFlavors[f].getMimeType().startsWith(da.mimeType + "; class="))
						continue;
						
					//	get data
					Object dropDataObj = transfer.getTransferData(dataFlavors[f]);
					if (dropDataObj == null)
						continue;
					String dropData = dropDataObj.toString().trim();
					
					//	test data
					if ((da.filterPattern != null) && !dropData.toString().matches(da.filterPattern))
						continue;
					
					//	get existing attribute value
					Object oldValue = annots[a].getAttribute(da.attributeName);
					
					//	no change
					if (dropData.equals(oldValue)) {
						DialogFactory.alert((da.annotType + "." + da.attributeName + " unchanged, value is\n  " + dropData), "Attribute Unchanged", JOptionPane.INFORMATION_MESSAGE);
						return true;
					}
					
					//	set attribute
					else if (oldValue == null) {
						annots[a].setAttribute(da.attributeName, dropData);
						DialogFactory.alert((da.annotType + "." + da.attributeName + " set to\n  " + dropData), "Attribute Set", JOptionPane.INFORMATION_MESSAGE);
						return true;
					}
					
					//	ask if attribute should be changes
					else {
						if (DialogFactory.confirm((da.annotType + "." + da.attributeName + " already exists, value is\n  " + oldValue.toString() + "\nReplace with new value\n  " + dropData), "Confirm Change Attribute", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION)
							annots[a].setAttribute(da.attributeName, dropData);
						return true;
					}
				}
				catch (Exception e) {
					e.printStackTrace(System.out);
				}
			}
		}
		
		//	nothing we could do about this drop ...
		return false;
	}
	
	private class DropAcceptor {
		final int priority;
		final String annotType;
		final String mimeType;
		final String attributeName;
		final String filterPattern;
		DropAcceptor(int priority, String annotType, String mimeType, String attributeName, String filterPattern) {
			this.priority = priority;
			this.annotType = annotType;
			this.mimeType = mimeType;
			this.attributeName = attributeName;
			this.filterPattern = filterPattern;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		JPanel p = new JPanel();
		DropTarget dropTarget = new DropTarget(p, new DropTargetAdapter() {
			public void drop(DropTargetDropEvent dtde) {
				System.out.println("\n======== drop event ========");
				dtde.acceptDrop(dtde.getDropAction());
				Transferable transfer = dtde.getTransferable();
				DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
				for (int d = 0; d < dataFlavors.length; d++) {
					System.out.println("  ====== " + dataFlavors[d].toString() + " ======");
					System.out.println(dataFlavors[d].getRepresentationClass());
					try {
						Object transferData = transfer.getTransferData(dataFlavors[d]);
						transferData = transfer.getTransferData(dataFlavors[d]);
						System.out.println(transferData.getClass().getName());
						System.out.println(transferData.toString());
					}
					catch (Exception e) {
						e.printStackTrace(System.out);
					}
				}
			}
		});
		dropTarget.setActive(true);
		
		JDialog d = DialogFactory.produceDialog("Drop Test", true);
		d.getContentPane().setLayout(new BorderLayout());
		d.getContentPane().add(p, BorderLayout.CENTER);
		d.setSize(300, 300);
		d.setLocationRelativeTo(null);
		d.setVisible(true);
	}
}