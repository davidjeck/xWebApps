package edu.hws.eck.umb.palette;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.*;

import edu.hws.eck.umb.util.I18n;

/**
 * A panel that displays a panel and lets the user edit it.  The palette
 * is shown as a band of colors, plus a "color patch" for every division 
 * point in the palette.  The user can drag the color patches, can edit
 * a color patch by double-clicking it, and can add a division point by
 * double-clicking the color band.  Several "Actions" are defined, which
 * are used in the PaletteEditDialog to make it possible, for example, 
 * to delete division points.
 */
class PaletteEditPanel extends JPanel {
	
	public final static String SELECTED_INDEX_PROPERTY = "PaletteEditPanel_Selected_Index";
	
	private final Palette palette;
	private ChangeListener changeListener;

	private final int BORDER = 12;
	private ArrayList<Rectangle> colorRects = new ArrayList<Rectangle>();
	
	private int selectedIndex = -1;
	
	public PaletteEditPanel() {
		this(null);
	}
	
	public PaletteEditPanel(Palette palette) {
		setPreferredSize(new Dimension(512 + 2*BORDER, 90+2*BORDER));
		if (palette == null)
			palette = new Palette(Palette.COLOR_TYPE_HSB);
		this.palette = palette;
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
		final Mouser mouseHandler = new Mouser();
		addMouseListener(mouseHandler);
		changeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				if (mouseHandler.dragging == -1)
					setSelection(-1);
				else
					repaint();
			}
		};
		palette.addChangeListener(changeListener);
	}
	
	public void finalize() {
		palette.removeChangeListener(changeListener);
	}
	
	void closing() {
		palette.removeChangeListener(changeListener);
		changeListener = null;
	}
	
	public final Action actionDeleteSelected = new AbstractAction(I18n.tr("paletteEditDialog.buttonName.DeleteSelected")) {
		public void actionPerformed(ActionEvent evt) {
			if (selectedIndex > 0 && selectedIndex < palette.getDivisionPointCount() - 1)
				palette.join(selectedIndex);
		}
	};
	
	public final Action actionEditSelected = new AbstractAction(I18n.tr("paletteEditDialog.buttonName.EditSelected")) {
		public void actionPerformed(ActionEvent evt) {
			if (selectedIndex >= 0)
				ColorEdit.showDialog(PaletteEditPanel.this, palette, selectedIndex);
		}
	};
	
	public final Action actionAddColor = new AbstractAction(I18n.tr("paletteEditDialog.buttonName.AddColorToPalette")) {
		public void actionPerformed(ActionEvent evt) {
			ColorEdit.showDialog(PaletteEditPanel.this, palette, -1);
		}
	};
	
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		int width = getWidth() - 2*BORDER;
		int height = getHeight() - 2*BORDER;
		int itemHeight = height/3;
		g.setColor(Color.DARK_GRAY);
		g.fillRect(BORDER-2,BORDER-2,width+5,itemHeight+4);
		for (int i = 0; i < width; i++) {
			double position =  (double)i / (width-1);
			g.setColor(palette.getColor(position));
			g.drawLine(BORDER+i,BORDER,BORDER+i,BORDER+itemHeight-1);
		}
		int colorCt = palette.getDivisionPointCount();
		int rectWidth = 15;
		if ((rectWidth+1)*colorCt > getWidth() - 4)
			rectWidth = (getWidth() - 4) / colorCt;
		if (rectWidth < 3)
			rectWidth = 3;
		colorRects.clear();
		int minDist = Integer.MAX_VALUE;
		for (int i = 0; i < colorCt; i++) {
			Rectangle r = new Rectangle();
			r.x = (int)(BORDER + palette.getDivisionPoint(i)*width) - rectWidth/2;
			r.y = BORDER + 2*itemHeight;
			r.width = rectWidth;
			r.height = itemHeight;
			colorRects.add(r);
			if (i > 0 && r.x - colorRects.get(colorRects.size()-2).x < minDist)
				minDist = r.x - colorRects.get(colorRects.size()-2).x;
		}
		if (minDist < rectWidth) {
			if (colorCt*(rectWidth+1) >= width + rectWidth) {
				double dx = (double)width / (colorCt-1);
				for (int i = 0; i < colorRects.size(); i++)
					colorRects.get(i).x = (int)(BORDER - rectWidth/2 + dx*i);
			}
			else {
				int[] x = new int[colorRects.size()];
				for (int i = 0; i < x.length; i++)
					x[i] = colorRects.get(i).x;
				while (true) {
					minDist = Integer.MAX_VALUE;
					int minDistIndex = 1;
					for (int i = 1; i < x.length; i++)
						if (x[i] - x[i-1] < minDist) {
							minDist = x[i] - x[i-1];
							minDistIndex = i;
						}
					if (minDist > rectWidth)
						break;
					int overlap = (x[minDistIndex - 1] + rectWidth + 1) - x[minDistIndex];
					int moveLeft = overlap/2;
					int moveRight = (overlap+1)/2;
					int leftSpace = (x[minDistIndex-1] - BORDER + rectWidth/2) - (minDistIndex-1)*(rectWidth+1);
					int rightSpace = (BORDER + width + rectWidth - rectWidth/2 - x[minDistIndex]) 
					                         - (colorCt-minDistIndex)*(rectWidth+1) + 1;
					if (moveLeft > leftSpace) {
						moveRight += moveLeft-leftSpace;
						moveLeft = leftSpace;
					}
					else if (moveRight > rightSpace) {
						moveLeft += moveRight - rightSpace;
						moveRight = rightSpace;
					}
					int i = minDistIndex - 1;
					while (moveLeft > 0) {
						int space = x[i] - (x[i-1] + rectWidth+1);
						x[i] = x[i] - moveLeft;
						if (space > 0)
							moveLeft -= space;
						i--;
					}
					i = minDistIndex;
					while (moveRight > 0) {
						int space = x[i+1] - (x[i] + rectWidth+1);
						x[i] += moveRight;
						if (space > 0)
							moveRight -= space;
						i++;
					}
				}
				for (int i = 0; i < x.length; i++)
					colorRects.get(i).x = x[i];
			}
		}
		for (int i = 0; i < colorCt; i++) {
			Rectangle r = colorRects.get(i);
			int x1 = (int)( BORDER + palette.getDivisionPoint(i)*width );
			int x2 = r.x + r.width/2;
			g.setColor(Color.DARK_GRAY);
			g.drawLine(x1,BORDER+itemHeight,x2,BORDER+itemHeight*2);
			g.fillRect(r.x,r.y,r.width,r.height);
			g.setColor(palette.getDivisionPointColor(i));
			g.fillRect(r.x+1,r.y+1,r.width-2,r.height-2);
		}
		if (selectedIndex >= 0) {
			double pos = palette.getDivisionPoint(selectedIndex);
			Rectangle r = colorRects.get(selectedIndex);
			int x = BORDER + (int)(pos * width);
			g.setColor(Color.BLACK);
			g.drawRect(x-2,BORDER-1,2,itemHeight+1);
			g.drawRect(r.x,r.y,r.width,r.height);
			g.setColor(Color.WHITE);
			g.drawRect(x-3,BORDER-2,4,itemHeight+3);
			g.drawRect(r.x-1,r.y-1,r.width+2,r.height+2);
			g.setColor(Color.BLACK);
			g.drawRect(x-4,BORDER-3,6,itemHeight+5);
			g.drawRect(r.x-2,r.y-2,r.width+4,r.height+4);
		}
	}
	
	private void setSelection(int index) {
		if (index != selectedIndex) {
			int oldVal = selectedIndex;
			selectedIndex = index;
			firePropertyChange(SELECTED_INDEX_PROPERTY, oldVal, selectedIndex);
		}
		repaint();
	}
	
	private void directColorEdit(int index) {
		Color c = palette.getDivisionPointColor(index);
		c = JColorChooser.showDialog(this, I18n.tr("colorEditDialog.colorChooserTitl"), c);
		if (c != null) {
			float[] color;
			if (palette.getColorType() == Palette.COLOR_TYPE_HSB)
				color =  Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
			else
				color = c.getRGBColorComponents(null);
			palette.setDivisionPointColorComponents(index, color[0], color[1], color[2]); 
		}
	}
	
	private class Mouser extends MouseAdapter implements MouseMotionListener {
		int dragging = -1;
		int dragPosition;
		int dragOffset;
		public void mousePressed(MouseEvent evt) {
			dragging = -1;
			int x = evt.getX();
			int y = evt.getY();
			dragOffset = 0;
			int width = getWidth() - 2*BORDER;
			int itemHeight = (getHeight() - 2*BORDER)/3;
			if (y > BORDER && y < BORDER+itemHeight) {
				int ct = palette.getDivisionPointCount();
				int clicked = -1;
				int minDist = Integer.MAX_VALUE;
				if (selectedIndex >= 0 && Math.abs(x - (BORDER + (int)(width*palette.getDivisionPoint(selectedIndex)))) < 4) {
					int xi = BORDER + (int)(width*palette.getDivisionPoint(selectedIndex));
					clicked = selectedIndex;
					dragPosition = xi;
					minDist = Math.abs(xi - x);
				}
				else {
					for (int i = 0; i < ct; i++) {
						int xi = BORDER + (int)(width*palette.getDivisionPoint(i));
						if (Math.abs(xi - x) < minDist) {
							clicked = i;
							dragPosition = xi;
							minDist = Math.abs(xi - x);
						}
					}
				}
				if (minDist < 4) {
					if (evt.getClickCount() == 1) {
						setSelection(clicked);
						if (clicked > 0 && clicked < palette.getDivisionPointCount() - 1) {
							PaletteEditPanel.this.addMouseMotionListener(this);
							dragging = clicked;
						}
						return;
					}
					if (evt.getClickCount() == 2) {
						if (!evt.isShiftDown() && !evt.isMetaDown())
							directColorEdit(clicked);
						else
							ColorEdit.showDialog(PaletteEditPanel.this, palette, clicked);
						return;
					}
				}
				double xd = (double)(evt.getX()-BORDER) / (getWidth() - 2*BORDER);
				if (evt.getClickCount() == 2 && xd > 0 && xd < 1) {
					setSelection(palette.split(xd));
					return;
				}
				setSelection(-1);
				return;
			}
			for (int i = 0; i < colorRects.size(); i++) {
				if (colorRects.get(i).contains(x, y)) {
					if (evt.getClickCount() == 2) {
						if (!evt.isShiftDown() && !evt.isMetaDown())
							directColorEdit(i);
						else
							ColorEdit.showDialog(PaletteEditPanel.this, palette, i);
					}
					else{
						setSelection(i);
						if (i > 0 && i < palette.getDivisionPointCount() - 1) {
							dragging = i;
							dragPosition = BORDER + (int)(width*palette.getDivisionPoint(i));
							dragOffset = x - dragPosition;
							PaletteEditPanel.this.addMouseMotionListener(this);
						}
					}
					return;
				}
			}
			setSelection(-1);
			return;
		}
		public void mouseReleased(MouseEvent evt) {
			if (dragging >= 0) {
				PaletteEditPanel.this.removeMouseMotionListener(this);
				dragging = -1;
			}
		}
		public void mouseDragged(MouseEvent evt) {
			if (dragging == -1)
				return;
			int width = getWidth() - 2*BORDER;
			int minX, maxX;
			if (dragging == 0)
				minX = BORDER + 1;
			else
				minX = BORDER + (int)(palette.getDivisionPoint(dragging-1)*width+0.499) + 1;
			if (dragging == palette.getDivisionPointCount() - 1)
				maxX = BORDER + width - 1;
			else
				maxX = BORDER + (int)(palette.getDivisionPoint(dragging+1)*width) - 1;
			int x = evt.getX() - dragOffset;
			if (x < minX)
				x = minX;
			else if (x > maxX)
				x = maxX;
			if (x != dragPosition) {
				double xd = (double)(x-BORDER) / (getWidth() - 2*BORDER);
				palette.setDivisionPoint(dragging, xd);
			}
		}
		public void mouseMoved(MouseEvent evt) {
		}
	}

}
