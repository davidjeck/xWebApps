package edu.hws.eck.umb;

import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;

import javax.swing.*;

import edu.hws.eck.umb.util.I18n;

/**
 * Defines a modal dialog where the user can set the x/y limits for the image
 * in a MandelbrotDisplay.  The dialog box contains four input boxes, for xmin, xmax,
 * ymin, and ymax.
 */
public class SetLimitsDialog extends JDialog {
	
	private BigDecimal[] valueInput;
	private boolean canceled = true;
	private JTextField[] inputs;
	private JButton okButton, cancelButton;

	/**
	 * Show the dialog box and wait for the user's input.
	 * @param parent The frame, if any, containing this component is the parent
	 * for the dialog box.
	 * @param initialLimitsAsStrings if non-null, provides the initial content of the input boxes.
	 * @return the user's inputs, or null if the user cancels.
	 */
	public static BigDecimal[] showDialog(Component parent, String[] initialLimitsAsStrings) {
		while (parent != null && !(parent instanceof Frame))
			parent = parent.getParent();
		SetLimitsDialog dialog = new SetLimitsDialog((Frame)parent,initialLimitsAsStrings);
		dialog.setVisible(true);
		if (dialog.canceled)
			return null;
		else
			return dialog.valueInput;
	}
	
	private SetLimitsDialog(Frame parent, String[] initialLimitsAsStrings) {
		super(parent,I18n.tr("setLimitsDialog.title"),true);
		inputs = new JTextField[4];
		for (int i = 0; i < 4; i++) {
			inputs[i] = new JTextField(20);
			if (initialLimitsAsStrings != null)
				inputs[i].setText(initialLimitsAsStrings[i].toString());
		}
		JPanel content = new JPanel();
		content.setLayout(new BorderLayout(8,8));
		content.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
		setContentPane(content);
		content.add(new JLabel(I18n.tr("setLimitsDialog.instructions")), BorderLayout.NORTH);
		JPanel middle = new JPanel();
		middle.setLayout(new BorderLayout());
		JPanel midLeft = new JPanel();
		midLeft.setLayout(new GridLayout(4,1,3,3));
		JPanel midRight = new JPanel();
		midRight.setLayout(new GridLayout(4,1,3,3));
		middle.add(midLeft,BorderLayout.WEST);
		middle.add(midRight,BorderLayout.CENTER);
		for (int i = 0; i < 4; i++)
			midRight.add(inputs[i]);
		midLeft.add(new JLabel(I18n.tr("term.MinimumX") + " = "));
		midLeft.add(new JLabel(I18n.tr("term.MaximumX") + " = "));
		midLeft.add(new JLabel(I18n.tr("term.MinimumY") + " = "));
		midLeft.add(new JLabel(I18n.tr("term.MaximumY") + " = "));
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
				BigDecimal[] input = new BigDecimal[4];
				for (int i = 0; i < 4; i++) {
					try {
						input[i] = new BigDecimal(inputs[i].getText().trim());
					}
					catch (NumberFormatException e) {
						String s;
						if (i == 0)
							s = I18n.tr("term.MinimumX");
						else if (i == 1)
							s = I18n.tr("term.MaximumX");
						else if (i == 2)
							s = I18n.tr("term.MinimumY");
						else
							s = I18n.tr("term.MaximumY");
						JOptionPane.showMessageDialog(SetLimitsDialog.this, I18n.tr("setLimitsDialog.error.NotANumber",s));
						inputs[i].selectAll();
						inputs[i].requestFocus();
						return;
					}
				}
				if (input[0].compareTo(input[1]) >= 0) {
					JOptionPane.showMessageDialog(SetLimitsDialog.this, I18n.tr("setLimitsDialog.error.MinMaxOutOfOrder",
							I18n.tr("term.MinimumX"), I18n.tr("term.MaximumX")));
					inputs[1].selectAll();
					inputs[1].requestFocus();
					return;
				}
				if (input[2].compareTo(input[3]) >= 0) {
					JOptionPane.showMessageDialog(SetLimitsDialog.this, I18n.tr("setLimitsDialog.error.MinMaxOutOfOrder",
							I18n.tr("term.MinimumY"), I18n.tr("term.MaximumY")));
					inputs[3].selectAll();
					inputs[3].requestFocus();
					return;
				}
				valueInput = input;
				canceled = false;
				dispose();
			}
		}
	}

}
