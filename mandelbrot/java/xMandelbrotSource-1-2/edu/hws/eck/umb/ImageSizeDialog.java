package edu.hws.eck.umb;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import edu.hws.eck.umb.util.I18n;

/**
 * Defines a dialog that allows the user to input an image
 * size as a value of type Dimension, with text input fields for
 * the height and width of the image.
 */
public class ImageSizeDialog extends JDialog {
	
	private Dimension valueInput;
	private boolean canceled = true;
	private JTextField widthInput, heightInput;
	private JButton okButton, cancelButton;

	/**
	 * Show the dialog box and wait for user's input.
	 * @param parent The frame containing this component, if any, is the parent of the dialog box.
	 * @param initialValue if non-null, supplies the initial content of the input boxes.
	 * @return the width and height entered by the user, in a value of type Dimension, or
	 * null if the user cancels the dialog box.
	 */
	public static Dimension showDialog(Component parent, Dimension initialValue) {
		while (parent != null && !(parent instanceof Frame))
			parent = parent.getParent();
		ImageSizeDialog dialog = new ImageSizeDialog((Frame)parent,initialValue);
		dialog.setVisible(true);
		if (dialog.canceled)
			return null;
		else
			return dialog.valueInput;
	}
	
	private ImageSizeDialog(Frame parent, Dimension initialValue) {
		super(parent,I18n.tr("imageSizeDialog.title"),true);
		widthInput = new JTextField(5);
		heightInput = new JTextField(5);
		if (initialValue != null) {
			widthInput.setText("" + initialValue.width);
			heightInput.setText("" + initialValue.height);
		}
		JPanel content = new JPanel();
		content.setLayout(new BorderLayout(8,8));
		content.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
		setContentPane(content);
		content.add(new JLabel(I18n.tr("imageSizeDialog.instructions")), BorderLayout.NORTH);
		JPanel middle = new JPanel();
		middle.setLayout(new GridLayout(2,2,5,5));
		middle.add(new JLabel(I18n.tr("term.width") + " = ", JLabel.RIGHT));
		middle.add(widthInput);
		middle.add(new JLabel(I18n.tr("term.height") + " = ", JLabel.RIGHT));
		middle.add(heightInput);
		content.add(middle,BorderLayout.CENTER);
		JPanel bottom = new JPanel();
		bottom.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		okButton = new JButton(I18n.tr("buttonName.OK"));
		cancelButton = new JButton(I18n.tr("buttonName.Cancel"));
		ActionListener listener = new ButtonHandler();
		okButton.addActionListener(listener);
		cancelButton.addActionListener(listener);
		bottom.add(cancelButton);
		bottom.add(okButton);
		content.add(bottom, BorderLayout.SOUTH);
		getRootPane().setDefaultButton(okButton);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setResizable(false);
		if (parent != null)
			setLocation(parent.getX() + 40, parent.getY() + 80);
		content.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke("ESCAPE"), "cancel");
		content.getActionMap().put("cancel",new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				cancelButton.doClick();
			}
		});
	}
	
	private class ButtonHandler implements ActionListener {
		public void actionPerformed(ActionEvent evt) {
			Object src = evt.getSource();
			if (src == cancelButton) {
				dispose();
			}
			else if (src == okButton) {
				int w, h;
				try {
					w = Integer.parseInt(widthInput.getText().trim());
					if (w <= 0)
						throw new NumberFormatException();
				}
				catch (NumberFormatException e) {
					JOptionPane.showMessageDialog(ImageSizeDialog.this, I18n.tr("imageSizeDialog.error.WidthMustBePositive"));
					widthInput.selectAll();
					widthInput.requestFocus();
					return;
				}
				try {
					h = Integer.parseInt(heightInput.getText().trim());
					if (h <= 0)
						throw new NumberFormatException();
				}
				catch (NumberFormatException e) {
					JOptionPane.showMessageDialog(ImageSizeDialog.this, I18n.tr("imageSizeDialog.error.HeightMustBePositive"));
					heightInput.selectAll();
					heightInput.requestFocus();
					return;
				}
				valueInput = new Dimension(w,h);
				canceled = false;
				dispose();
			}
		}
	}

}
