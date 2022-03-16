package edu.hws.eck.umb.palette;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.hws.eck.umb.util.I18n;

/**
 * A dialog box that allows the user to specify a color and its position
 * in the palette.
 */
class ColorEdit extends JDialog {
	
	private float[] originalColorComponents;
	private double originalPosition;
	private Palette palette;
	private int index;
	private int createdIndex = -1;  // set when a new color is created
	
	private JButton okButton, closeButton, applyButton, revertButton;
	private JTextField[] input;
	private JTextField positionInput;
	private JPanel colorPatch;
	
	public static void showDialog(Component parent, Palette palette, int colorIndex) {
		while (parent != null && !(parent instanceof Dialog))
			parent = parent.getParent();
		ColorEdit dialog = new ColorEdit((Dialog)parent, palette, colorIndex);
		dialog.setVisible(true);
	}
	
	private ColorEdit(Dialog parent, Palette palette, int index) {
		super(parent, index == -1 ? I18n.tr("colorEditDialog.titleForAddingNewColor") : I18n.tr("colorEditDialog.title"), true);
		this.palette = palette;
		this.index = index;
		if (index == -1)
			originalColorComponents = new float[] {0.5F, 0.5F, 0.5F};
		else {
			originalColorComponents = palette.getDivisionPointColorComponents(index); // value is cloned
			originalPosition = palette.getDivisionPoint(index);
		}
		JPanel content = new JPanel();
		setContentPane(content);
		content.setBackground(Color.DARK_GRAY);
		content.setLayout(new BorderLayout(3,3));
		content.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY,3));
		
		okButton = new JButton(I18n.tr("buttonName.OK"));
		closeButton = new JButton(I18n.tr("buttonName.Close"));
		applyButton = new JButton(I18n.tr("buttonName.Apply"));
		revertButton = new JButton(I18n.tr("buttonName.Revert"));
		final Listener listener = new Listener();
		okButton.addActionListener(listener);
		closeButton.addActionListener(listener);
		applyButton.addActionListener(listener);
		revertButton.addActionListener(listener);
		JPanel buttonBar = new JPanel();
		buttonBar.setLayout(new GridLayout(2,2,1,1));
		buttonBar.setBackground(Color.LIGHT_GRAY);
		buttonBar.add(revertButton);
		buttonBar.add(applyButton);
		buttonBar.add(closeButton);
		buttonBar.add(okButton);
		content.add(buttonBar, BorderLayout.SOUTH);
		
		JPanel inputPanel = new JPanel();
		JPanel left = new JPanel();
		JPanel right = new JPanel();
		inputPanel.setLayout(new BorderLayout(5,5));
		inputPanel.setBackground(Color.LIGHT_GRAY);
		inputPanel.add(left, BorderLayout.WEST);
		inputPanel.add(right,BorderLayout.CENTER);
		left.setBackground(Color.LIGHT_GRAY);
		right.setBackground(Color.LIGHT_GRAY);
		left.setLayout(new GridLayout(3,1,3,3));
		right.setLayout(new GridLayout(3,1,3,3));
		input = new JTextField[3];
		if (palette.getColorType() == Palette.COLOR_TYPE_HSB) {
			left.add( new JLabel(I18n.tr("colorComponent.Hue") + " = ", JLabel.RIGHT) );
			left.add( new JLabel(I18n.tr("colorComponent.Saturation") + " = ", JLabel.RIGHT) );
			left.add( new JLabel(I18n.tr("colorComponent.Brightness") + " = ", JLabel.RIGHT) );
		}
		else {
			left.add( new JLabel(I18n.tr("colorComponent.Red") + " = ", JLabel.RIGHT) );
			left.add( new JLabel(I18n.tr("colorComponent.Green") + " = ", JLabel.RIGHT) );
			left.add( new JLabel(I18n.tr("colorComponent.Blue") + " = ", JLabel.RIGHT) );
		}
		for (int i = 0; i < 3; i++) {
			input[i] = new JTextField(""+originalColorComponents[i],9);
			input[i].setMargin(new Insets(0,4,0,4));
			right.add(input[i]);
		}
		inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		content.add(inputPanel, BorderLayout.WEST);
		
		if (index == -1 || index > 0 && index < palette.getDivisionPointCount() - 1) {
			JPanel top = new JPanel();
			top.setLayout(new BorderLayout(5,5));
			top.setBackground(Color.LIGHT_GRAY);
			top.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
			double leftLimit = index == -1 ? 0 : palette.getDivisionPoint(index - 1);
			double rightLimit = index == -1 ? 1 : palette.getDivisionPoint(index + 1);
			top.add(new JLabel(I18n.tr("colorEditDialog.positionOfColorBar", String.format("%1.4f",leftLimit), 
					String.format("%1.4f",rightLimit)) + " = "), BorderLayout.WEST);
			if (index == -1)
				positionInput = new JTextField(9);
			else
				positionInput = new JTextField(String.format("%1.4f", palette.getDivisionPoint(index)), 9);
			positionInput.setMargin(new Insets(4,4,4,4));
			top.add(positionInput, BorderLayout.CENTER);
			content.add(top, BorderLayout.NORTH);
		}
		
		colorPatch = new JPanel();
		colorPatch.setPreferredSize(new Dimension(100,100));
		if (index == -1) {
			if (palette.getColorType() == Palette.COLOR_TYPE_HSB)
				colorPatch.setBackground(Color.getHSBColor(0.5F, 0.5F, 0.5F));
			else
				colorPatch.setBackground(new Color(0.5F,0.5F,0.5F));
		}
		else
			colorPatch.setBackground(palette.getDivisionPointColor(index));
		colorPatch.setToolTipText(I18n.tr("colorEditDialog.tooltip.clickToEditColor"));
		colorPatch.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				Color c = colorPatch.getBackground();
				c = JColorChooser.showDialog(ColorEdit.this, I18n.tr("colorEditDialog.colorChooserTitl"), c);
				if (c != null) {
					float[] comps;
					if (ColorEdit.this.palette.getColorType() == Palette.COLOR_TYPE_HSB)
						comps = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
					else
						comps = c.getRGBColorComponents(null);
					if (ColorEdit.this.index != -1)
						ColorEdit.this.palette.setDivisionPointColorComponents(ColorEdit.this.index, comps[0], comps[1], comps[2]);
					else
						colorPatch.setBackground(c);
					for (int i = 0; i < 3; i++)
						input[i].setText("" + comps[i]);
				}
			}
		});
		palette.addChangeListener(listener);
		content.add(colorPatch,BorderLayout.CENTER);

		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		content.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke("ESCAPE"), "cancel");
		content.getActionMap().put("cancel",new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				closeButton.doClick();
			}
		});
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				doCancel();
			}
			public void windowClosed(WindowEvent evt) {
				ColorEdit.this.palette.removeChangeListener(listener);
			}
		});
		getRootPane().setDefaultButton(okButton);
		pack();
		setResizable(false);
		if (parent != null) {
			Point pt = parent.getLocation();
			setLocation(pt.x + 25, pt.y + 100);
		}
	}
	
	private class Listener implements ActionListener, ChangeListener {
		public void actionPerformed(ActionEvent evt) {
			Object src = evt.getSource();
			if (src == okButton)
				doOK();
			else if (src == closeButton)
				doCancel();
			else if (src == revertButton)
				doRevert();
			else if (src == applyButton)
				doApply();
		}
		public void stateChanged(ChangeEvent e) {
			if (index != -1)
				colorPatch.setBackground(palette.getDivisionPointColor(index));
		}
	}
	
	private void doOK() {
		if (doApply())
			dispose();
	}
	
	private void doCancel() {
		dispose();
	}
	
	private void doRevert() {
		if (createdIndex != -1) {
			index = -1;
			palette.join(createdIndex);
			createdIndex = -1;
			positionInput.setText("");
			for (JTextField f : input)
				f.setText("0.5");
			Color c;
			if (palette.getColorType() == Palette.COLOR_TYPE_HSB)
				c = Color.getHSBColor(0.5F, 0.5F, 0.5F);
			else
				c = new Color(0.5F, 0.5F, 0.5F);
			colorPatch.setBackground(c);
		}
		else if (index != -1){
			palette.setDivisionPointColorComponents(index, 
					originalColorComponents[0], originalColorComponents[1], originalColorComponents[2]);
		    if (index > 0 && index < palette.getDivisionPointCount() - 1)
		    	palette.setDivisionPoint(index,originalPosition);
		    for (int i = 0; i < 3; i++)
		    	input[i].setText(String.format("%1.4f", originalColorComponents[i]));
		}
	}
	
	private boolean doApply() {
		float[] c = new float[3];
		for (int i = 0; i < 3; i++) {
			String s = input[i].getText().trim();
			try {
				c[i] = Float.parseFloat(s);
			}
			catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, I18n.tr("colorEditDialog.badFloatValue", s));
				input[i].requestFocus();
				input[i].selectAll();
				return false;
			}
		}
		if (positionInput != null) {
			try {
				double divisionPoint = Double.parseDouble(positionInput.getText().trim());
				double leftLimit = index == -1 ? 0 : palette.getDivisionPoint(index-1);
				double rightLimit = index == -1 ? 1 : palette.getDivisionPoint(index+1);
				if (divisionPoint <= leftLimit || divisionPoint >= rightLimit) {
					JOptionPane.showMessageDialog(this, I18n.tr("colorEditDialog.positionOutOfRange",
							String.format("%1.4f",leftLimit), String.format("%1.4f",rightLimit)));
					return false;
				}
				if (index == -1) {
					createdIndex = palette.split(divisionPoint);
					if (createdIndex == -1)
						JOptionPane.showMessageDialog(this, I18n.tr("colorEditDialog.positionAlreadyExistsInPalette", ""+divisionPoint));
					else
						index = createdIndex;
				}
				else
					palette.setDivisionPoint(index, divisionPoint);
			}
			catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(this, 
						I18n.tr("colorEditDialog.badFloatValue", positionInput.getText().trim()));
				return false;
			}
		}
		palette.setDivisionPointColorComponents(index, c[0], c[1], c[2]);
		return true;
	}

}
