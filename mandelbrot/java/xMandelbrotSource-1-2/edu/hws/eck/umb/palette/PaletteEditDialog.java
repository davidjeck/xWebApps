package edu.hws.eck.umb.palette;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.hws.eck.umb.MandelbrotDisplay;
import edu.hws.eck.umb.util.I18n;

/**
 * Show a dialog for editing the palette that is being used in a
 * MandelbrotDisplay.  The dialog shows a PaletteEditPanel that is
 * used to edit the palette itself, a HistogramPanel that is used
 * to set the PaletteMapping, and a small MandelbrotDisplay that
 * shows a preview of the palette changes.
 */
public class PaletteEditDialog extends JDialog {
	
	private MandelbrotDisplay owner;
	private MandelbrotDisplay preview;
	private HistogramPanel histogramPanel;
	
	private Palette originalPalette;
	private Palette paletteInEditor;
	private PaletteMapping originalPaletteMapping;
	private PaletteMapping paletteMappingInEditor;
	
	private PaletteEditPanel paletteEditor;
	
	private JButton closeButton, revertButton, applyButton;
	private JButton helpButton, addButton, deleteSelectedButton, editSelectedButton;
	
	private JCheckBox paletteMatchesMaxIterations;
	private JTextField paletteLengthInput;
	private JTextField paletteOffsetInput;
	
	private JMenuItem open, save;
	
	private JMenuItem undoTransform;
	private MandelbrotDisplay.PaletteInfo paletteForUndoTransform;
	
	public static JDialog createDialog(MandelbrotDisplay display) {
		Component parent = display;
		while (parent != null && !(parent instanceof Frame))
			parent = parent.getParent();
		PaletteEditDialog dialog = new PaletteEditDialog((Frame)parent, display);
		return dialog;
	}
	
	
	private PaletteEditDialog(Frame parent, MandelbrotDisplay display) {
		super(parent,I18n.tr("paletteEditDialog.title"),false);
		paletteInEditor = display.getCopyOfPalette();
		originalPalette = paletteInEditor.clone();
		paletteMappingInEditor = new PaletteMapping(display.getPaletteLength(),display.getPaletteOffset());
		originalPaletteMapping = new PaletteMapping(display.getPaletteLength(),display.getPaletteOffset());
		owner = display;
		paletteEditor = new PaletteEditPanel(paletteInEditor);
		closeButton = new JButton(I18n.tr("buttonName.Close"));
		revertButton = new JButton(I18n.tr("buttonName.Revert"));
		applyButton = new JButton(I18n.tr("buttonName.Apply"));
		helpButton = new JButton(I18n.tr("buttonName.Help"));
		deleteSelectedButton = new JButton(paletteEditor.actionDeleteSelected);
		editSelectedButton = new JButton(paletteEditor.actionEditSelected);
		addButton = new JButton(paletteEditor.actionAddColor);
		ActionListener lis = new ButtonHandler();
		closeButton.addActionListener(lis);
		revertButton.addActionListener(lis);
		applyButton.addActionListener(lis);
		helpButton.addActionListener(lis);
		deleteSelectedButton.setEnabled(false);
		editSelectedButton.setEnabled(false);
		revertButton.setEnabled(false);
		applyButton.setEnabled(false);
		paletteMatchesMaxIterations = new JCheckBox(I18n.tr("paletteEditDialog.buttonName.lockPaletteLengthToMaxIterations"));
		paletteMatchesMaxIterations.setSelected(paletteMappingInEditor.getLength() == 0);
		paletteMatchesMaxIterations.setBackground(Color.LIGHT_GRAY);
		paletteMatchesMaxIterations.addActionListener(lis);
		paletteLengthInput = new JTextField(""+paletteMappingInEditor.getLength(), 5);
		paletteLengthInput.setEditable(paletteMappingInEditor.getLength() != 0);
		paletteOffsetInput = new JTextField(""+paletteMappingInEditor.getOffset(),5);
		paletteLengthInput.addActionListener(lis);
		paletteOffsetInput.addActionListener(lis);
		
		JPanel content = new JPanel();
		content.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
		content.setLayout(new BorderLayout());
		setContentPane(content);
		
		JPanel top = new JPanel();
		top.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		top.setBackground(Color.LIGHT_GRAY);
		top.setLayout(new BorderLayout(3,3));
		top.add(paletteEditor, BorderLayout.CENTER);
		JPanel bar1 = new JPanel();
		bar1.setBackground(Color.LIGHT_GRAY);
		bar1.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));
		bar1.add(addButton);
		bar1.add(deleteSelectedButton);
		bar1.add(editSelectedButton);
		top.add(bar1, BorderLayout.SOUTH);
		content.add(top, BorderLayout.CENTER);
		
		JPanel bar2 = new JPanel();
		bar2.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));
		bar2.setBackground(Color.LIGHT_GRAY);
		bar2.add(helpButton);
		bar2.add(applyButton);
		bar2.add(revertButton);
		bar2.add(closeButton);
		
		JPanel bar3 = new JPanel();
		bar3.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));
		bar3.setBackground(Color.LIGHT_GRAY);
		bar3.add(new JLabel(I18n.tr("paletteEditDialog.textInputLabel.PaletteOffset") + " = "));
		bar3.add(paletteOffsetInput);
		bar3.add(Box.createHorizontalStrut(15));
		bar3.add(new JLabel(I18n.tr("paletteEditDialog.textInputLabel.PaletteLength") + " = "));
		bar3.add(paletteLengthInput);
		bar3.add(Box.createHorizontalStrut(5));
		bar3.add(paletteMatchesMaxIterations);

		JPanel bottom = new JPanel();
		bottom.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		bottom.setBackground(Color.LIGHT_GRAY);
		bottom.setLayout(new BorderLayout(3,3));
		histogramPanel = new HistogramPanel(paletteInEditor, paletteMappingInEditor);
		bottom.add(histogramPanel, BorderLayout.WEST);
		preview = new MandelbrotDisplay(false,false);
		preview.setPreferredSize(new Dimension(180,180));
		preview.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
		preview.setMaxIterations(owner.getMaxIterations());
		preview.setLimits(owner.getLimitsRequested());
		preview.setPaletteOffset(owner.getPaletteOffset());
		preview.setPaletteLength(owner.getPaletteLength());
		preview.setPalette(paletteInEditor);
		preview.setMandelbrotColor(owner.getMandelbrotColor());
		bottom.add(preview, BorderLayout.CENTER);
		bottom.add(bar2, BorderLayout.SOUTH);
		bottom.add(bar3, BorderLayout.NORTH);
		content.add(bottom,BorderLayout.SOUTH);
		
		setJMenuBar(makeMenuBar(lis));
		
		paletteEditor.addPropertyChangeListener(PaletteEditPanel.SELECTED_INDEX_PROPERTY, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				int selected = (Integer)evt.getNewValue();
				deleteSelectedButton.setEnabled(selected > 0 && selected < paletteInEditor.getDivisionPointCount() - 1);
				editSelectedButton.setEnabled(selected >= 0);
			}
		});
		final PropertyChangeListener propListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				String prop = evt.getPropertyName();
				if (prop.equals(MandelbrotDisplay.PROPERTY_STATUS)) {
					MandelbrotDisplay.StatusInfo info = (MandelbrotDisplay.StatusInfo)evt.getNewValue();
					if (info.status != MandelbrotDisplay.STATUS_RUNNING_SECOND_PASS)
						histogramPanel.setHistogram(owner.createIterationCountHistogram());
				}
				else if (prop.equals(MandelbrotDisplay.PROPERTY_PALETTE)) {
					Palette palette = ((MandelbrotDisplay.PaletteInfo)evt.getNewValue()).palette;
					PaletteMapping paletteMapping = ((MandelbrotDisplay.PaletteInfo)evt.getNewValue()).paletteMapping;
					if ( ! (palette.equals(paletteEditor) && paletteMapping.equals(paletteMappingInEditor)) ) {
						originalPalette = palette;
						originalPaletteMapping = paletteMapping;
						paletteInEditor.copyFrom(palette);
						paletteMappingInEditor.setLength(paletteMapping.getLength());
						paletteMappingInEditor.setOffset(paletteMapping.getOffset());
						applyButton.setEnabled(false);
						revertButton.setEnabled(false);
					}
				}
				else if (prop.equals(MandelbrotDisplay.PROPERTY_LIMITS))
					preview.setLimits((BigDecimal[])evt.getNewValue());
				else if (prop.equals(MandelbrotDisplay.PROPERTY_HIGH_PRECISION))
					preview.setHighPrecisionEnabled((Boolean)evt.getNewValue());
				else if (prop.equals(MandelbrotDisplay.PROPERTY_MANDLELBROT_COLOR))
					preview.setMandelbrotColor((Color)evt.getNewValue());
				else if (prop.equals(MandelbrotDisplay.PROPERTY_MAX_ITERATIONS))
					preview.setMaxIterations((Integer)evt.getNewValue());
			}
		};
		owner.addPropertyChangeListener(propListener);
		histogramPanel.setHistogram(owner.createIterationCountHistogram());
		final ChangeListener cl = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				revertButton.setEnabled(true);
				applyButton.setEnabled(true);
				undoTransform.setEnabled(false);
				paletteForUndoTransform = null;
				if (e.getSource() instanceof Palette)
					preview.setPalette(paletteInEditor);
				else if (e.getSource() instanceof PaletteMapping) {
					preview.setPaletteLength(paletteMappingInEditor.getLength());
					preview.setPaletteOffset(paletteMappingInEditor.getOffset());
					paletteLengthInput.setText(""+paletteMappingInEditor.getLength());
					paletteOffsetInput.setText(""+paletteMappingInEditor.getOffset());
					paletteMatchesMaxIterations.setSelected(paletteMappingInEditor.getLength() == 0);
					paletteLengthInput.setEditable(paletteMappingInEditor.getLength() != 0);
				}
			}
		};
		paletteInEditor.addChangeListener(cl);
		paletteMappingInEditor.addChangeListener(cl);
		addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				preview.closing();
				paletteEditor.closing();
				paletteInEditor.removeChangeListener(cl);
				owner.removePropertyChangeListener(propListener);
			}
		});
		DocumentListener dl = new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
			}
			public void insertUpdate(DocumentEvent e) {
				applyButton.setEnabled(true);
			}
			public void removeUpdate(DocumentEvent e) {
				applyButton.setEnabled(true);
			}
		};
		paletteLengthInput.getDocument().addDocumentListener(dl);
		paletteOffsetInput.getDocument().addDocumentListener(dl);
		content.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke("ESCAPE"), "cancel");
		content.getActionMap().put("cancel",new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				closeButton.doClick();
			}
		});
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		pack();
		setResizable(false);
		if (parent != null) {
			Rectangle parentRect = parent.getBounds();
			Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
			int x = parentRect.x + parentRect.width + 8;
			if (x + getWidth() > screensize.width)
				x = screensize.width - getWidth() - 5;
			int y = parentRect.y + parentRect.height - getHeight() - 5;
			if (y + getHeight() > screensize.height)
				y = screensize.height - getHeight() - 5;
			setLocation(x, y);
		}
	}
	
	
	private class ButtonHandler implements ActionListener {
		public void actionPerformed(ActionEvent evt) {
			Object src = evt.getSource();
			String cmd = evt.getActionCommand();
			if (src == closeButton) {
				dispose();
			}
			else if (src == revertButton) {
				paletteInEditor.copyFrom(originalPalette);
				paletteMappingInEditor.setLength(originalPaletteMapping.getLength());
				paletteMappingInEditor.setOffset(originalPaletteMapping.getOffset());
				paletteMatchesMaxIterations.setSelected(originalPaletteMapping.getLength() == 0);
				paletteLengthInput.setText(""+originalPaletteMapping.getLength());
				paletteOffsetInput.setText(""+originalPaletteMapping.getOffset());
				paletteLengthInput.setEditable(originalPaletteMapping.getLength() != 0);
				revertButton.setEnabled(false);
				applyButton.setEnabled(false);
			}
			else if (src == applyButton || src instanceof JTextField) {
				int length, offset;
				try {
					length = Integer.parseInt(paletteLengthInput.getText().trim());
					if (length < 0)
						throw new NumberFormatException();
				}
				catch (NumberFormatException e) {
					JOptionPane.showMessageDialog(PaletteEditDialog.this, I18n.tr("paletteEditDialog.error.BadPaletteLength"));
					paletteLengthInput.selectAll();
					paletteLengthInput.requestFocus();
					return;
				}
				try {
					offset = Integer.parseInt(paletteOffsetInput.getText().trim());
					if (offset < 0)
						throw new NumberFormatException();
				}
				catch (NumberFormatException e) {
					JOptionPane.showMessageDialog(PaletteEditDialog.this, I18n.tr("paletteEditDialog.error.BadPaletteOffset"));
					paletteOffsetInput.selectAll();
					paletteOffsetInput.requestFocus();
					return;
				}
				paletteMappingInEditor.setLength(length);
				paletteMappingInEditor.setOffset(offset);
				if (src == applyButton)
					owner.setPaletteInfo(paletteInEditor,paletteMappingInEditor);
			}
			else if (src == paletteMatchesMaxIterations) {
				if (paletteMatchesMaxIterations.isSelected()) {
					paletteLengthInput.setText("0");
					paletteLengthInput.setEditable(false);
					paletteMappingInEditor.setLength(0);
				}
				else {
					paletteLengthInput.setText("" + owner.getMaxIterations());
					paletteLengthInput.setEditable(true);
					paletteMappingInEditor.setLength(owner.getMaxIterations());
				}
			}
			else if (src == helpButton) {
				JOptionPane.showMessageDialog(PaletteEditDialog.this, new JLabel(I18n.tr("paletteEditDialog.helpText")));
			}
			else if (src == open) {
				Palette p = PaletteIO.doOpen(PaletteEditDialog.this);
				if (p != null)
					paletteInEditor.copyFrom(p);
			}
			else if (src == save)
				PaletteIO.doSave(PaletteEditDialog.this, paletteInEditor);
			else if (src == undoTransform) {
				if (paletteForUndoTransform != null) {
					MandelbrotDisplay.PaletteInfo newInfo = paletteForUndoTransform; // paletteForUndoTransform will be reset to null by next line
					paletteInEditor.copyFrom(newInfo.palette);
					paletteMappingInEditor.setLength(newInfo.paletteMapping.getLength());
					paletteMappingInEditor.setOffset(newInfo.paletteMapping.getOffset());
				}
			}
			else if (cmd.startsWith("Transform/"))
				doTransform(cmd);
			else
				paletteInEditor.copyFrom(Palette.makeDefaultPalette(cmd));
		}
	}
	
	private void doTransform(String command) {
		ArrayList<Double> points = new ArrayList<Double>();
		ArrayList<float[]> colors = new ArrayList<float[]>();
		int colorType = paletteInEditor.getColorType();
		int offset = paletteMappingInEditor.getOffset();
		int length = paletteMappingInEditor.getLength();
		int ct = paletteInEditor.getDivisionPointCount();
		if (command.equals("Transform/Flip")) {
			for (int i = ct-1; i >= 0; i--) {
				points.add(1.0 - paletteInEditor.getDivisionPoint(i));
				colors.add(paletteInEditor.getDivisionPointColorComponents(i));
			}
		}
		else if (command.equals("Transform/Extend")) {
			for (int i = 0; i < ct; i++) {
				points.add(paletteInEditor.getDivisionPoint(i) / 2);
				colors.add(paletteInEditor.getDivisionPointColorComponents(i));
			}
			points.add(1.0);
			colors.add(paletteInEditor.getDivisionPointColorComponents(ct-1));
			if (length > 0)
				length *= 2;
		}
		else if (command.equals("Transform/ExtendDuplicate")) {
			for (int i = 0; i < ct; i++) {
				points.add(paletteInEditor.getDivisionPoint(i) / 2);
				colors.add(paletteInEditor.getDivisionPointColorComponents(i));
			}
			float[] c1 = paletteInEditor.getDivisionPointColorComponents(ct-1);
			float[] c2 = paletteInEditor.getDivisionPointColorComponents(0);
			if ( ! (c1[0] == c2[0] && c1[1] == c2[1] && c1[2] == c2[2]) ) {
				points.add(0.5001);
				colors.add(paletteInEditor.getDivisionPointColorComponents(0));
			}
			for (int i = 1; i < ct; i++) {
				points.add(0.5 + paletteInEditor.getDivisionPoint(i) / 2);
				colors.add(paletteInEditor.getDivisionPointColorComponents(i));
			}
			if (length > 0)
				length *= 2;
		}
		else if (command.equals("Transform/ExtendMirror")) {
			for (int i = 0; i < ct; i++) {
				points.add(paletteInEditor.getDivisionPoint(i) / 2);
				colors.add(paletteInEditor.getDivisionPointColorComponents(i));
			}
			for (int i = ct-2; i >= 0; i--) {
				points.add(1.0 - paletteInEditor.getDivisionPoint(i) / 2);
				colors.add(paletteInEditor.getDivisionPointColorComponents(i));
			}
			if (length > 0)
				length *= 2;
		}
		else { // (command.equals("Transform/Random"))
			int ptCt = 3 + (int)(Math.random()*5);
			double[] pts = new double[ptCt];
			pts[0] = 0;
			for (int i = 1; i < ptCt-1; i++) {
				double r;
				boolean dupp;
				do {
					r = (int)(10000*Math.random()) / 10000.0; 
					dupp = false;
					for (int j = 0; j < i; j++)
						if (Math.abs(r - pts[j]) < 0.05)
							dupp = true;
				} while (dupp);
				pts[i] = r;
			}
			pts[ptCt-1] = 1;
			Arrays.sort(pts);
			for (int i = 0; i < ptCt; i++) {
				points.add(pts[i]);
				colors.add(new float[] { (float)Math.random(), (float)Math.random(), (float)Math.random() });
			}
			colorType = (Math.random() < 0.5)? Palette.COLOR_TYPE_HSB : Palette.COLOR_TYPE_RGB;
		}
		MandelbrotDisplay.PaletteInfo oldInfo = 
			new MandelbrotDisplay.PaletteInfo(paletteInEditor.clone(),paletteMappingInEditor.clone());
		Palette p = new Palette(colorType,true,points,colors);
		paletteInEditor.copyFrom(p);
		paletteMappingInEditor.setLength(length);
		paletteMappingInEditor.setOffset(offset);
		undoTransform.setEnabled(true);
		paletteForUndoTransform = oldInfo;
	}
	
	private JMenuBar makeMenuBar(ActionListener listener) {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu(I18n.tr("menuName.File"));
		open = new JMenuItem(I18n.tr("menuCommand.FileMenu.Open"));
		save = new JMenuItem(I18n.tr("menuCommand.FileMenu.Save"));
		menu.add(open);
		menu.add(save);
		open.addActionListener(listener);
		save.addActionListener(listener);
		menuBar.add(menu);
		JMenu def = new JMenu(I18n.tr("paletteEditDialog.menuName.LoadDefault"));
		String[] defaults = new String[] { "Spectrum", "PaleSpectrum", "DarkSpectrum", "Grayscale",
				"CyclicGrayscale","CyclicRedCyan", "EarthSky", "HotCold", "Fire" };
		for ( String d : defaults ) {
			JMenuItem item = new JMenuItem(I18n.tr("paletteEditDialog.menuCommand.LoadDefault." + d));
			item.setActionCommand(d);
			item.addActionListener(listener);
			def.add(item);
		}
		menuBar.add(def);
		JMenu trans = new JMenu(I18n.tr("paletteEditDialog.menuName.Transform"));
		undoTransform = new JMenuItem(I18n.tr("paletteEditDialog.menuCommand.UndoTransform"));
		undoTransform.addActionListener(listener);
		undoTransform.setEnabled(false);
		String[] transforms = new String[] { "Flip", "Extend", "ExtendDuplicate", 
				"ExtendMirror", "Random" };
		for (String t : transforms) {
			JMenuItem item = new JMenuItem(I18n.tr("paletteEditDialog.menuCommand.Transform." + t));
			item.setActionCommand("Transform/" + t);
			item.addActionListener(listener);
			trans.add(item);
		}
		trans.addSeparator();
		trans.add(undoTransform);
		menuBar.add(trans);
		return menuBar;
	}

}
