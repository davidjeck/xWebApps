package edu.hws.eck.umb.palette;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A panel used in a PaletteEditDialog.  Shows a histogram of the number of times
 * each iteration count occurs in the MandelbrotDisplay whose palette is being edited.
 * Contains tabs that the user can drag to set the palette length and palette offset.
 */
class HistogramPanel extends JPanel  {
	
	private int[] histogram;
	private Palette palette;
	private PaletteMapping paletteMapping;
	
	private Polygon paletteLengthTab, paletteOffsetTab;
	
	private int paletteOffsetTabPosition, paletteLengthTabPosition;
	private double graphWidth;

	/**
	 * Create HistogramPanel showing a given palette.  The HistogramPanel
	 * registers as a ChangeListener with the Palette and the PaletteMapping,
	 * so that it can redraw itself if these are changed.  When the user
	 * drags the tabs in the HistogramPanel, the PaletteMapping is modified
	 * to reflect the change; other components that are registered as
	 * listeners to the PaletteMapping can then respond to the change. 
	 */
	public HistogramPanel(Palette palette, PaletteMapping paletteMapping) {
		setBackground(Color.WHITE);
		setPreferredSize(new Dimension(400,180));
		setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
		this.palette = palette;
		this.paletteMapping = paletteMapping;
		ChangeListener cl = new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				repaint();
			}
		};
		palette.addChangeListener(cl);
		paletteMapping.addChangeListener(cl);
		addMouseListener(new Mouser());
	}
	
	public void setHistogram(int[] histogram) {
		this.histogram = histogram;
		repaint();
	}
	
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(Color.BLUE);
		int axis_y = getHeight() - 70;
		g.drawLine(10,axis_y+1, getWidth()-10,axis_y+1);
		g.drawLine(10,axis_y+2, getWidth()-10,axis_y+2);
		g.drawLine(8,8,8,axis_y+2);
		g.drawLine(9,8,9,axis_y+2);
		if (histogram == null)
			return;
		graphWidth = getWidth()-20;
		double graphHeight =  axis_y-10;
		double max = 0;
		for (int i = 0; i < histogram.length; i++)
			if (histogram[i] > max)
				max = histogram[i];
		if (max == 0)
			return;
		g.setColor(Color.BLACK);
		for (int i = 0; i < histogram.length; i++) {
			if (histogram[i] > 0) {
				int y = (int)(graphHeight*histogram[i]/max);
				int x = (int)(graphWidth*i/histogram.length);
				g.drawLine(11+x,axis_y-y-1,11+x,axis_y-1);
			}
		}
		int colorCt = paletteMapping.getLength() == 0 ? histogram.length : paletteMapping.getLength();
		paletteLengthTabPosition = colorCt;
		int[] colors = palette.makeRGBs(colorCt, paletteMapping.getOffset());
		for (int i = 0; i < graphWidth; i++) {
			int pos = (int)(i/graphWidth * histogram.length);
			Color c = new Color(colors[pos % colorCt]);
			g.setColor(c);
			g.drawLine(11+i, axis_y+25, 11+i, axis_y+45);
		}
		g.setColor(Color.DARK_GRAY);
		g.drawRect(10,axis_y+24,getWidth()-19,21);
		if (paletteMapping.getLength() > histogram.length) {
			if (paletteMapping.getOffset() >= histogram.length)
				paletteOffsetTabPosition = (int)graphWidth + 5;
			else
				paletteOffsetTabPosition = (int)(graphWidth*paletteMapping.getOffset()/histogram.length);
		}
		else {
			int length;
			if (paletteMapping.getLength() == 0)
				length = histogram.length;
			else 
				length = paletteMapping.getLength() * (histogram.length/paletteMapping.getLength());
			paletteOffsetTabPosition = paletteMapping.getOffset() % length;
			paletteOffsetTabPosition = (int)(graphWidth*paletteOffsetTabPosition/histogram.length);
		}
		paletteOffsetTab = new Polygon();
		int x = paletteOffsetTabPosition + 11;
		int y = axis_y + 40;
		paletteOffsetTab.addPoint(x,y);
		paletteOffsetTab.addPoint(x+7,y+7);
		paletteOffsetTab.addPoint(x+7,y+25);
		paletteOffsetTab.addPoint(x-6,y+25);
		paletteOffsetTab.addPoint(x-6,y+7);
		g.setColor(new Color(50,200,50));
		g.fillPolygon(paletteOffsetTab);
		g.setColor(new Color(150,255,150));
		for (int i = x-4; i <= x+4; i+=4)
			g.drawLine(i,y+12,i,y+20);
		g.setColor(Color.DARK_GRAY);
		for (int i = x-3; i <= x+5; i+=4)
			g.drawLine(i,y+13,i,y+21);
		g.setColor(Color.BLACK);
		g.drawPolygon(paletteOffsetTab);
		if (paletteMapping.getLength() == 0 || paletteMapping.getLength() > histogram.length)
			paletteLengthTabPosition = (int)graphWidth+5;
		else {
			paletteLengthTabPosition = paletteMapping.getLength() % histogram.length;
			if (paletteLengthTabPosition == 0)
				paletteLengthTabPosition = histogram.length;
			paletteLengthTabPosition = (int)(graphWidth*paletteLengthTabPosition/histogram.length);
		}
		x = paletteLengthTabPosition + 11;
		y = axis_y + 30;
		paletteLengthTab = new Polygon();
		paletteLengthTab.addPoint(x,y);
		paletteLengthTab.addPoint(x+7,y-7);
		paletteLengthTab.addPoint(x+7,y-25);
		paletteLengthTab.addPoint(x-6,y-25);
		paletteLengthTab.addPoint(x-6,y-7);
		g.setColor(new Color(225,30,30));
		g.fillPolygon(paletteLengthTab);
		g.setColor(new Color(255,150,150));
		for (int i = x-4; i <= x+4; i+=4)
			g.drawLine(i,y-12,i,y-20);
		g.setColor(Color.DARK_GRAY);
		for (int i = x-3; i <= x+5; i+=4)
			g.drawLine(i,y-11,i,y-19);
		g.setColor(Color.BLACK);
		g.drawPolygon(paletteLengthTab);
	}
	
	private class Mouser extends MouseAdapter implements MouseMotionListener {
	    boolean dragging;
	    boolean draggingOffset;
	    int start;
	    int offset;
	    boolean lengthLocked;
	    public void mousePressed(MouseEvent evt) {
	    	if (dragging)
	    		return;
	    	if (paletteLengthTab.contains(evt.getX(), evt.getY())) {
	    		dragging = true;
	    		draggingOffset = false;
	    		lengthLocked = paletteMapping.getLength() == 0 || paletteMapping.getLength() > histogram.length;
	    		start = evt.getX();
	    		offset = start - (paletteLengthTabPosition + 11);
	    		addMouseMotionListener(this);
	    	}
	    	else if (paletteOffsetTab.contains(evt.getX(), evt.getY())) {
	    		dragging = true;
	    		draggingOffset = true;
	    		start = evt.getX();
	    		offset = start - (paletteOffsetTabPosition + 11);
	    		addMouseMotionListener(this);
	    	}
	    }
	    public void mouseReleased(MouseEvent evt) {
	    	if (dragging) {
	    		dragging = false;
	    		removeMouseMotionListener(this);
	    	}
	    }
	    public void mouseDragged(MouseEvent evt) {
	    	if (!dragging)
	    		return;
	    	if (lengthLocked && start - evt.getX() < 15)
	    		return;
	    	lengthLocked = false;
	    	if (draggingOffset) {
		    	int position = evt.getX() - offset - 11;
		    	if (position < 0 && position > -10)
		    		position = 0;
		    	int value = (int)(position/graphWidth * histogram.length);
	    		int length;
	    		if (paletteMapping.getLength() == 0)
	    			length = histogram.length;
	    		else if (paletteMapping.getLength() > histogram.length)
	    			length =paletteMapping.getLength();
	    		else
	    			length = paletteMapping.getLength() * (histogram.length/paletteMapping.getLength());
	    		value = value - (length * (int)Math.floor((double)value/length));
	    		paletteMapping.setOffset(value);
	    	}
	    	else {
		    	int position = evt.getX() - offset - 11;
		    	int value = (int)(position/graphWidth * histogram.length);
	    		if (evt.getX() > getWidth() + 50)
	    			value = 0;
	    		else if (value > histogram.length)
	    			value = histogram.length;
	    		else if (value < 1)
	    			value = 1;
	    		paletteMapping.setLength(value);
	    	}
	    }
		public void mouseMoved(MouseEvent e) {
		}
	}

}
