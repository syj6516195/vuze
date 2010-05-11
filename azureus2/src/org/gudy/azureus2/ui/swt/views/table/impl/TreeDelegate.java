/**
 * Created on May 3, 2010
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
 
package org.gudy.azureus2.ui.swt.views.table.impl;

import java.util.Arrays;

import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.ui.swt.views.table.TableColumnOrTreeColumn;
import org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT;

/**
 * @author TuxPaper
 * @created May 3, 2010
 *
 */
public class TreeDelegate implements TableOrTreeSWT
{
	Tree tree;

	public TreeDelegate(Composite parent, int style) {
		tree = new Tree(parent, style);
	}

	public TreeDelegate(Tree t) {
		tree = t;
	}

	public Composite getComposite() {
		return tree;
	}

	public Rectangle computeTrim(int x, int y, int width, int height) {
		return tree.computeTrim(x, y, width, height);
	}

	public void addControlListener(ControlListener listener) {
		tree.addControlListener(listener);
	}

	public void changed(Control[] changed) {
		tree.changed(changed);
	}

	public void addDragDetectListener(DragDetectListener listener) {
		tree.addDragDetectListener(listener);
	}

	public Rectangle getClientArea() {
		return tree.getClientArea();
	}

	public void addListener(int eventType, Listener listener) {
		tree.addListener(eventType, listener);
	}

	public void addFocusListener(FocusListener listener) {
		tree.addFocusListener(listener);
	}

	public ScrollBar getHorizontalBar() {
		return tree.getHorizontalBar();
	}

	public void addDisposeListener(DisposeListener listener) {
		tree.addDisposeListener(listener);
	}

	public ScrollBar getVerticalBar() {
		return tree.getVerticalBar();
	}

	public void addHelpListener(HelpListener listener) {
		tree.addHelpListener(listener);
	}

	public void addKeyListener(KeyListener listener) {
		tree.addKeyListener(listener);
	}

	public void addMenuDetectListener(MenuDetectListener listener) {
		tree.addMenuDetectListener(listener);
	}

	public void addMouseListener(MouseListener listener) {
		tree.addMouseListener(listener);
	}

	public void addSelectionListener(SelectionListener listener) {
		tree.addSelectionListener(listener);
	}

	public void addMouseTrackListener(MouseTrackListener listener) {
		tree.addMouseTrackListener(listener);
	}

	public void addMouseMoveListener(MouseMoveListener listener) {
		tree.addMouseMoveListener(listener);
	}

	public void addTreeListener(TreeListener listener) {
		tree.addTreeListener(listener);
	}

	public void addMouseWheelListener(MouseWheelListener listener) {
		tree.addMouseWheelListener(listener);
	}

	public int getBackgroundMode() {
		return tree.getBackgroundMode();
	}

	public void addPaintListener(PaintListener listener) {
		tree.addPaintListener(listener);
	}

	public Control[] getChildren() {
		return tree.getChildren();
	}

	public void addTraverseListener(TraverseListener listener) {
		tree.addTraverseListener(listener);
	}

	public void dispose() {
		tree.dispose();
	}

	public Layout getLayout() {
		return tree.getLayout();
	}

	public Control[] getTabList() {
		return tree.getTabList();
	}

	public boolean getLayoutDeferred() {
		return tree.getLayoutDeferred();
	}

	public Point computeSize(int wHint, int hHint) {
		return tree.computeSize(wHint, hHint);
	}

	public boolean isLayoutDeferred() {
		return tree.isLayoutDeferred();
	}

	public Object getData() {
		return tree.getData();
	}

	public void layout() {
		tree.layout();
	}

	public Object getData(String key) {
		return tree.getData(key);
	}

	public void layout(boolean changed) {
		tree.layout(changed);
	}

	public Display getDisplay() {
		return tree.getDisplay();
	}

	public Listener[] getListeners(int eventType) {
		return tree.getListeners(eventType);
	}

	public void layout(boolean changed, boolean all) {
		tree.layout(changed, all);
	}

	public int getStyle() {
		return tree.getStyle();
	}

	public boolean dragDetect(Event event) {
		return tree.dragDetect(event);
	}

	public void layout(Control[] changed) {
		tree.layout(changed);
	}

	public boolean isListening(int eventType) {
		return tree.isListening(eventType);
	}

	public boolean dragDetect(MouseEvent event) {
		return tree.dragDetect(event);
	}

	public void layout(Control[] changed, int flags) {
		tree.layout(changed, flags);
	}

	public void notifyListeners(int eventType, Event event) {
		tree.notifyListeners(eventType, event);
	}

	public void removeListener(int eventType, Listener listener) {
		tree.removeListener(eventType, listener);
	}

	public void removeDisposeListener(DisposeListener listener) {
		tree.removeDisposeListener(listener);
	}

	public void reskin(int flags) {
		tree.reskin(flags);
	}

	public void setBackgroundMode(int mode) {
		tree.setBackgroundMode(mode);
	}

	public boolean setFocus() {
		return tree.setFocus();
	}

	public void setLayout(Layout layout) {
		tree.setLayout(layout);
	}

	public int getBorderWidth() {
		return tree.getBorderWidth();
	}

	public void setLayoutDeferred(boolean defer) {
		tree.setLayoutDeferred(defer);
	}

	public Rectangle getBounds() {
		return tree.getBounds();
	}

	public void setTabList(Control[] tabList) {
		tree.setTabList(tabList);
	}

	public void setData(Object data) {
		tree.setData(data);
	}

	public Cursor getCursor() {
		return tree.getCursor();
	}

	public void setData(String key, Object value) {
		tree.setData(key, value);
	}

	public boolean getDragDetect() {
		return tree.getDragDetect();
	}

	public boolean getEnabled() {
		return tree.getEnabled();
	}

	public Font getFont() {
		return tree.getFont();
	}

	public Color getForeground() {
		return tree.getForeground();
	}

	public Object getLayoutData() {
		return tree.getLayoutData();
	}

	public Point getLocation() {
		return tree.getLocation();
	}

	public Menu getMenu() {
		return tree.getMenu();
	}

	public Monitor getMonitor() {
		return tree.getMonitor();
	}

	public Composite getParent() {
		return tree.getParent();
	}

	public Region getRegion() {
		return tree.getRegion();
	}

	public Shell getShell() {
		return tree.getShell();
	}

	public Point getSize() {
		return tree.getSize();
	}

	public String getToolTipText() {
		return tree.getToolTipText();
	}

	public boolean getVisible() {
		return tree.getVisible();
	}

	public String toString() {
		return tree.toString();
	}

	public boolean isEnabled() {
		return tree.isEnabled();
	}

	public boolean isFocusControl() {
		return tree.isFocusControl();
	}

	public boolean isReparentable() {
		return tree.isReparentable();
	}

	public boolean isVisible() {
		return tree.isVisible();
	}

	public void moveAbove(Control control) {
		tree.moveAbove(control);
	}

	public void moveBelow(Control control) {
		tree.moveBelow(control);
	}

	public void pack() {
		tree.pack();
	}

	public void pack(boolean changed) {
		tree.pack(changed);
	}

	public void clear(int index, boolean all) {
		tree.clear(index, all);
	}

	public boolean print(GC gc) {
		return tree.print(gc);
	}

	public void clearAll(boolean all) {
		tree.clearAll(all);
	}

	public Point computeSize(int wHint, int hHint, boolean changed) {
		return tree.computeSize(wHint, hHint, changed);
	}

	public void redraw() {
		tree.redraw();
	}

	public void redraw(int x, int y, int width, int height, boolean all) {
		tree.redraw(x, y, width, height, all);
	}

	public void removeControlListener(ControlListener listener) {
		tree.removeControlListener(listener);
	}

	public void removeDragDetectListener(DragDetectListener listener) {
		tree.removeDragDetectListener(listener);
	}

	public void removeFocusListener(FocusListener listener) {
		tree.removeFocusListener(listener);
	}

	public void removeHelpListener(HelpListener listener) {
		tree.removeHelpListener(listener);
	}

	public void removeKeyListener(KeyListener listener) {
		tree.removeKeyListener(listener);
	}

	public void removeMenuDetectListener(MenuDetectListener listener) {
		tree.removeMenuDetectListener(listener);
	}

	public void removeMouseTrackListener(MouseTrackListener listener) {
		tree.removeMouseTrackListener(listener);
	}

	public void removeMouseListener(MouseListener listener) {
		tree.removeMouseListener(listener);
	}

	public void removeMouseMoveListener(MouseMoveListener listener) {
		tree.removeMouseMoveListener(listener);
	}

	public void removeMouseWheelListener(MouseWheelListener listener) {
		tree.removeMouseWheelListener(listener);
	}

	public void removePaintListener(PaintListener listener) {
		tree.removePaintListener(listener);
	}

	public void removeTraverseListener(TraverseListener listener) {
		tree.removeTraverseListener(listener);
	}

	public void deselect(TableItemOrTreeItem item) {
		tree.deselect((TreeItem) item.getItem());
	}

	public void setBackground(Color color) {
		tree.setBackground(color);
	}

	public void deselectAll() {
		tree.deselectAll();
	}

	public boolean equals(Object obj) {
		return tree.equals(obj);
	}

	public boolean forceFocus() {
		return tree.forceFocus();
	}

	public Accessible getAccessible() {
		return tree.getAccessible();
	}

	public Color getBackground() {
		return tree.getBackground();
	}

	public Image getBackgroundImage() {
		return tree.getBackgroundImage();
	}

	public void setBackgroundImage(Image image) {
		tree.setBackgroundImage(image);
	}

	public void setBounds(int x, int y, int width, int height) {
		tree.setBounds(x, y, width, height);
	}

	public void setBounds(Rectangle rect) {
		tree.setBounds(rect);
	}

	public void setCapture(boolean capture) {
		tree.setCapture(capture);
	}

	public void setCursor(Cursor cursor) {
		tree.setCursor(cursor);
	}

	public void setDragDetect(boolean dragDetect) {
		tree.setDragDetect(dragDetect);
	}

	public void setEnabled(boolean enabled) {
		tree.setEnabled(enabled);
	}

	public void setForeground(Color color) {
		tree.setForeground(color);
	}

	public void setLayoutData(Object layoutData) {
		tree.setLayoutData(layoutData);
	}

	public void setLocation(int x, int y) {
		tree.setLocation(x, y);
	}

	public void setLocation(Point location) {
		tree.setLocation(location);
	}

	public void setMenu(Menu menu) {
		tree.setMenu(menu);
	}

	public int getGridLineWidth() {
		return tree.getGridLineWidth();
	}

	public int getHeaderHeight() {
		return tree.getHeaderHeight();
	}

	public boolean getHeaderVisible() {
		return tree.getHeaderVisible();
	}

	public void setRegion(Region region) {
		tree.setRegion(region);
	}

	public void setSize(int width, int height) {
		tree.setSize(width, height);
	}

	public TableColumnOrTreeColumn getColumn(int index) {
		return wrapOrNull(tree.getColumn(index));
	}

	public void setSize(Point size) {
		tree.setSize(size);
	}

	public int getColumnCount() {
		return tree.getColumnCount();
	}

	public void setToolTipText(String string) {
		tree.setToolTipText(string);
	}

	public int[] getColumnOrder() {
		return tree.getColumnOrder();
	}

	public void setVisible(boolean visible) {
		tree.setVisible(visible);
	}

	public TableColumnOrTreeColumn[] getColumns() {
		return wrapOrNull(tree.getColumns());
	}

	public TableItemOrTreeItem getItem(int index) {
		if (index < 0) {
			return null;
		}
		return wrapOrNull(tree.getItem(index));
	}

	public Point toControl(int x, int y) {
		return tree.toControl(x, y);
	}

	public Point toControl(Point point) {
		return tree.toControl(point);
	}

	public TableItemOrTreeItem getItem(Point point) {
		return wrapOrNull(tree.getItem(point));
	}

	public Point toDisplay(int x, int y) {
		return tree.toDisplay(x, y);
	}

	public Point toDisplay(Point point) {
		return tree.toDisplay(point);
	}

	public int getItemCount() {
		return tree.getItemCount();
	}

	public int getItemHeight() {
		return tree.getItemHeight();
	}

	public TableItemOrTreeItem[] getItems() {
		return wrapOrNull(tree.getItems());
	}

	public boolean getLinesVisible() {
		return tree.getLinesVisible();
	}

	public TableItemOrTreeItem getParentItem() {
		return wrapOrNull(tree.getParentItem());
	}

	public boolean traverse(int traversal) {
		return tree.traverse(traversal);
	}

	public TableItemOrTreeItem[] getSelection() {
		return wrapOrNull(tree.getSelection());
	}

	public boolean traverse(int traversal, Event event) {
		return tree.traverse(traversal, event);
	}

	public boolean traverse(int traversal, KeyEvent event) {
		return tree.traverse(traversal, event);
	}

	public int getSelectionCount() {
		return tree.getSelectionCount();
	}

	public TableColumnOrTreeColumn getSortColumn() {
		return wrapOrNull(tree.getSortColumn());
	}

	public int getSortDirection() {
		return tree.getSortDirection();
	}

	public TableItemOrTreeItem getTopItem() {
		return wrapOrNull(tree.getTopItem());
	}

	public int hashCode() {
		return tree.hashCode();
	}

	public boolean isDisposed() {
		return tree.isDisposed();
	}

	public int internal_new_GC(GCData data) {
		return tree.internal_new_GC(data);
	}

	public void internal_dispose_GC(int hDC, GCData data) {
		tree.internal_dispose_GC(hDC, data);
	}

	public void update() {
		tree.update();
	}

	public int indexOf(TableColumnOrTreeColumn column) {
		return tree.indexOf((TreeColumn) column.getColumn());
	}

	public int indexOf(TableItemOrTreeItem item) {
		TreeItem ti = (TreeItem) item.getItem();
		if (ti.isDisposed()) {
			return -1;
		}
		return tree.indexOf(ti);
	}

	public boolean setParent(Composite parent) {
		return tree.setParent(parent);
	}

	public void removeAll() {
		tree.removeAll();
	}

	public void removeSelectionListener(SelectionListener listener) {
		tree.removeSelectionListener(listener);
	}

	public void removeTreeListener(TreeListener listener) {
		tree.removeTreeListener(listener);
	}

	public void setInsertMark(TableItemOrTreeItem item, boolean before) {
		tree.setInsertMark((TreeItem) item.getItem(), before);
	}

	public void setItemCount(int count) {
		tree.setItemCount(count);
	}

	public void setLinesVisible(boolean show) {
		tree.setLinesVisible(show);
	}

	public void select(TableItemOrTreeItem item) {
		tree.select((TreeItem) item.getItem());
	}

	public void selectAll() {
		tree.selectAll();
	}

	public void setColumnOrder(int[] order) {
		tree.setColumnOrder(order);
	}

	public void setFont(Font font) {
		tree.setFont(font);
	}

	public void setHeaderVisible(boolean show) {
		tree.setHeaderVisible(show);
	}

	public void setRedraw(boolean redraw) {
		tree.setRedraw(redraw);
	}

	public void setSelection(TableItemOrTreeItem item) {
		tree.setSelection((TreeItem) item.getItem());
	}

	public void setSelection(TableItemOrTreeItem[] items) {
		tree.setSelection(toTreeItemArray(items));
	}

	public void setSortColumn(TableColumnOrTreeColumn column) {
		tree.setSortColumn(column == null ? null : (TreeColumn) column.getColumn());
	}

	public void setSortDirection(int direction) {
		tree.setSortDirection(direction);
	}

	public void setTopItem(TableItemOrTreeItem item) {
		tree.setTopItem((TreeItem) item.getItem());
	}

	public void showColumn(TableColumnOrTreeColumn column) {
		tree.showColumn((TreeColumn) column.getColumn());
	}

	public void showItem(TableItemOrTreeItem item) {
		tree.showItem((TreeItem) item.getItem());
	}

	public void showSelection() {
		tree.showSelection();
	}

	///////
	
	private TableItemOrTreeItem wrapOrNull(TreeItem item) {
		if (item == null) {
			return null;
		}
		return new TreeItemDelegate(item);
	}

	private TableItemOrTreeItem[] wrapOrNull(TreeItem[] items) {
		if (items == null) {
			return null;
		}
		TableItemOrTreeItem[] returnItems = new TableItemOrTreeItem[items.length];
		for (int i = 0; i < returnItems.length; i++) {
			returnItems[i] = new TreeItemDelegate(items[i]);
		}
		return returnItems;
	}
	
	private TableColumnOrTreeColumn wrapOrNull(TreeColumn item) {
		if (item == null) {
			return null;
		}
		return new TreeColumnDelegate(item);
	}

	private TableColumnOrTreeColumn[] wrapOrNull(TreeColumn[] items) {
		if (items == null) {
			return null;
		}
		TableColumnOrTreeColumn[] returnItems = new TableColumnOrTreeColumn[items.length];
		for (int i = 0; i < returnItems.length; i++) {
			returnItems[i] = new TreeColumnDelegate(items[i]);
		}
		return returnItems;
	}
	
	private TreeItem[] toTreeItemArray(TableItemOrTreeItem[] items) {
		if (items == null) {
			return null;
		}
		TreeItem[] returnItems = new TreeItem[items.length];
		for (int i = 0; i < returnItems.length; i++) {
			returnItems[i] = (TreeItem) items[i].getItem();
		}
		return returnItems;
	}
	
	public int getTopIndex() {
		TreeItem topItem = tree.getTopItem();
		if (topItem == null) {
			return -1;
		}
		return tree.indexOf(topItem);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT#getSelectionIndex()
	public int getSelectionIndex() {
		TreeItem[] selection = tree.getSelection();
		if (selection == null || selection.length == 0) {
			return -1;
		}
		return 0;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT#getSelectionIndices()
	public int[] getSelectionIndices() {
		TreeItem[] selection = tree.getSelection();
		if (selection == null || selection.length == 0) {
			return new int[0];
		}
		
		int[] vals = new int[selection.length];
		for (int i = 0; i < vals.length; i++) {
			vals[i] = tree.indexOf(selection[i]);
		}
		return vals;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT#setSelection(int[])
	public void setSelection(int[] newSelectedRowIndices) {
		TreeItem[] items = new TreeItem[newSelectedRowIndices.length];
		for (int i = 0; i < items.length; i++) {
			items[i] = tree.getItem(newSelectedRowIndices[i]);
		}
		tree.setSelection(items);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT#select(int[])
	public void select(int[] newSelectedRowIndices) {
		for (int i : newSelectedRowIndices) {
			if (i >= 0) {
				tree.select(tree.getItem(i));
			}
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableOrTreeSWT#isSelected(int)
	public boolean isSelected(int index) {
		int[] selectionIndices = getSelectionIndices();
		Arrays.sort(selectionIndices);
		return Arrays.binarySearch(selectionIndices, index) == 0;
	}

	public boolean equalsTableOrTree(TableOrTreeSWT tt) {
		return tree.equals(tt.getComposite());
	}

	public TableItemOrTreeItem createNewItem(int style) {
		return new TreeItemDelegate(this, style);
	}

  public TableColumnOrTreeColumn createNewColumn(int style) {
  	return  new TreeColumnDelegate(this, style);
  }
}

