/**
 * Created on May 12, 2010
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package org.eclipse.swt.widgets;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.Constants;

/**
 * @author TuxPaper
 * @created May 12, 2010
 *
 */
public class Tree2
	extends Tree
{
	static final Map<Integer, Integer> mapStyleToWidgetStyle = new HashMap<Integer, Integer>(
			0);

	public Tree2(Composite parent, int style) {
		super(parent, style);
	}

	protected void checkSubclass() {
		// skip check
	};

	int widgetStyle() {
		/* I was going to go with this code, but OSX doesn't ahve widgetStyle,
		    so compliling breaks on super call
		int oldStyle = super.widgetStyle();
		try {
			Class<?> claOS = Class.forName("org.eclipse.swt.internal.win32.OS");
			// & ~(OS.TVS_LINESATROOT | OS.TVS_HASBUTTONS)
			if (claOS != null) {
				int TVS_LINESATROOT = ((Number)claOS.getField("TVS_LINESATROOT").get(null)).intValue();
				int TVS_HASBUTTONS = ((Number)claOS.getField("TVS_HASBUTTONS").get(null)).intValue();
				oldStyle &= ~(TVS_HASBUTTONS | TVS_LINESATROOT);
			}
		} catch (Throwable e) {
		}
		return oldStyle;
		*/

		if (!Constants.isWindows) {
			return 0;
		}

		try {
			Integer widgetStyle = mapStyleToWidgetStyle.get(style);
			if (widgetStyle != null) {
				return widgetStyle.intValue();
			}

			Tree tree = new Tree(parent, style);
			Method method = Tree.class.getDeclaredMethod("widgetStyle", null);
			method.setAccessible(true);
			int oldStyle = ((Number) method.invoke(tree, null)).intValue();
			tree.dispose();

			Class<?> claOS = Class.forName("org.eclipse.swt.internal.win32.OS");
			// & ~(OS.TVS_LINESATROOT | OS.TVS_HASBUTTONS)
			if (claOS != null) {
				int TVS_LINESATROOT = ((Number) claOS.getField("TVS_LINESATROOT").get(
						null)).intValue();
				int TVS_HASBUTTONS = ((Number) claOS.getField("TVS_HASBUTTONS").get(
						null)).intValue();
				oldStyle &= ~(TVS_HASBUTTONS | TVS_LINESATROOT);
			}
			mapStyleToWidgetStyle.put(style, oldStyle);
			return oldStyle;
		} catch (Throwable e) {
			e.printStackTrace();
		}

		return 0;
	}
}
