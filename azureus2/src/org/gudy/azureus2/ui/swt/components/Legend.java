/*
 * Created on 13-Sep-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.ui.swt.Messages;

public class Legend {
	/** Creates the legend Composite
	 * 
	 * @return The created Legend Composite
	 */
	public static Composite createLegendComposite(Composite panel,
			Color[] colors, String[] keys) {
		Object layout = panel.getLayout();
		Object layoutData = null;
		if (layout instanceof GridLayout)
			layoutData = new GridData(GridData.FILL_HORIZONTAL);

		return createLegendComposite(panel, colors, keys, layoutData);
	}


	public static Composite createLegendComposite(Composite panel,
			Color[] colors, String[] keys, Object layoutData) {
		if (colors.length != keys.length)
			return null;

		Composite legend = new Composite(panel, SWT.NONE);
		if (layoutData != null)
			legend.setLayoutData(layoutData);

		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
		layout.wrap = true;
		layout.marginBottom = 0;
		layout.marginTop = 0;
		layout.marginLeft = 0;
		layout.marginRight = 0;
		layout.spacing = 0;
		legend.setLayout(layout);

		RowData data;
		for (int i = 0; i < colors.length; i++) {
			Composite colorSet = new Composite(legend, SWT.NONE);

			colorSet.setLayout(new RowLayout(SWT.HORIZONTAL));

			Label lblColor = new Label(colorSet, SWT.BORDER);
			lblColor.setBackground(colors[i]);
			data = new RowData();
			data.width = 20;
			data.height = 10;
			lblColor.setLayoutData(data);

			BufferedLabel lblDesc = new BufferedLabel(colorSet, SWT.NULL);
			Messages.setLanguageText(lblDesc, keys[i]);
		}

		return legend;
	}
}
