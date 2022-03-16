package edu.hws.eck.umb;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.hws.eck.umb.comp.MandelbrotNetworkTaskServer;
import edu.hws.eck.umb.comp.TaskManager;
import edu.hws.eck.umb.comp.TaskManager.NetworkWorkerInfo;
import edu.hws.eck.umb.util.I18n;
import edu.hws.eck.umb.util.Util;

/**
 * Defines a dialog where the user can configure multiprocessing in the
 * TaskManager associated with a given MandelbrotDisplay.  The user can set
 * the number of processes to be used in the TaskManager and can enable
 * network processing and add network workers to the TaskManager.  The list
 * of network workers is saved in the user's preferences, so they will be
 * there the next time the program is run (but networking is not automatically
 * turned back on -- the user must do that by hand).  The only public method
 * in this class shows the dialog.
 */
public class MultiprocessingConfigDialog extends JDialog {

	private MandelbrotDisplay display;
	private TaskManager taskManager;
	private JCheckBox defaultProcessCountCheckBox, enableNetworkCheckBox;
	private JTextField processCountInput;
	private JButton okButton, applyButton, cancelButton;
	private JButton addButton, deleteButton;
	private JList networkWorkerList;
	private NetworkWorkerListModel networkWorkerModel;
	private Timer updatetimer;

	/**
	 * Show a modal dialog where the user can configure multiprocessing
	 * for the given MandelbrotDisplay.
	 */
	public static void showDialog(MandelbrotDisplay display) {
		Component c = display.getParent();
		while (c != null && !(c instanceof Frame))
			c = c.getParent();
		MultiprocessingConfigDialog dialog = new MultiprocessingConfigDialog((Frame)c,display);
		dialog.setVisible(true);
	}

	private MultiprocessingConfigDialog(Frame parent, MandelbrotDisplay display) {
		super(parent,I18n.tr("multiprocessingConfigDialog.title"), false);
		this.display = display;
		this.taskManager = display.getTaskManager();
		JPanel content = new JPanel();
		content.setLayout(new BorderLayout(3,3));
		content.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		setContentPane(content);
		JPanel top = new JPanel();
		top.setLayout(new BorderLayout(5,5));
		top.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(I18n.tr("multiprocessingConfigDialog.paneTitle.LocalConfig")),
				BorderFactory.createEmptyBorder(3,5,3,3)) );
		JPanel middle = new JPanel();
		middle.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		JPanel bottom = new JPanel();
		bottom.setLayout(new BorderLayout(5,5));
		bottom.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(I18n.tr("multiprocessingConfigDialog.paneTitle.NetworkConfig")),
				BorderFactory.createEmptyBorder(3,5,3,3)) );
		content.add(top,BorderLayout.NORTH);
		content.add(middle,BorderLayout.CENTER);
		content.add(bottom,BorderLayout.SOUTH);
		ActionListener listener = new ButtonHandler();
		okButton = new JButton(I18n.tr("buttonName.OK"));
		okButton.addActionListener(listener);
		cancelButton = new JButton(I18n.tr("buttonName.Cancel"));
		cancelButton.addActionListener(listener);
		middle.add(cancelButton);
		middle.add(okButton);
		addButton = new JButton(I18n.tr("multiprocessingConfigDialog.buttonName.AddNetworkHost"));
		addButton.addActionListener(listener);
		deleteButton = new JButton(I18n.tr("multiprocessingConfigDialog.buttonName.DeleteSelected"));
		deleteButton.addActionListener(listener);
		applyButton = new JButton(I18n.tr("multiprocessingConfigDialog.buttonName.ApplyNetworkConfig"));
		applyButton.addActionListener(listener);

		int availableProcessors = Runtime.getRuntime().availableProcessors();
		int processCount = taskManager.getThreadPoolSize();
		boolean isDefault = availableProcessors == processCount;
		defaultProcessCountCheckBox = new JCheckBox(I18n.tr("multiprocessingConfigDialog.buttonName.UseDefaultProcessCount",""+availableProcessors));
		defaultProcessCountCheckBox.addActionListener(listener);
		defaultProcessCountCheckBox.setSelected(isDefault);
		processCountInput = new JTextField(""+processCount,3);
		processCountInput.setEditable(!isDefault);
		JPanel pcIn = new JPanel();
		pcIn.setLayout(new BorderLayout(4,4));
		pcIn.add(processCountInput,BorderLayout.EAST);
		pcIn.add(new JLabel(I18n.tr("multiprocessingConfigDialog.buttonName.NumberOfProcesses") + ":", JLabel.RIGHT), BorderLayout.CENTER);
		top.add(new JLabel(I18n.tr("multiprocessingConfigDialog.buttonName.DefaultProcessCountInfo")), BorderLayout.NORTH);
		top.add(defaultProcessCountCheckBox, BorderLayout.CENTER);
		top.add(pcIn,BorderLayout.SOUTH);

		enableNetworkCheckBox = new JCheckBox(I18n.tr("multiprocessingConfigDialog.buttonName.EnableNetworking"));
		enableNetworkCheckBox.setSelected(taskManager.getNetworkingEnabled());
		enableNetworkCheckBox.addActionListener(listener);
		boolean networkEnabled = taskManager.getNetworkingEnabled();
		enableNetworkCheckBox.setSelected(networkEnabled);
		TaskManager.NetworkWorkerInfo[] workerInfo;
		if (networkEnabled)
			workerInfo = taskManager.getAllNetworkWorkerInfo();
		else
			workerInfo = getNetworkPrefs();
		networkWorkerModel = new NetworkWorkerListModel(workerInfo);
		networkWorkerList = new JList(networkWorkerModel);
		networkWorkerList.setVisibleRowCount(7);
		networkWorkerList.setEnabled(networkEnabled);
		JScrollPane scroller = new JScrollPane(networkWorkerList);
		scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		bottom.add(scroller, BorderLayout.CENTER);
		JPanel p1 = new JPanel();
		p1.setLayout(new BorderLayout(5,5));
		p1.add(new JLabel(I18n.tr("multiprocessingConfigDialog.buttonName.NetworkConfigInfo")),BorderLayout.NORTH);
		JPanel p2 = new JPanel();
		p2.setLayout(new BorderLayout(5,5));
		p2.add(enableNetworkCheckBox,BorderLayout.WEST);
		p2.add(new JLabel(), BorderLayout.CENTER);
		p2.add(applyButton, BorderLayout.EAST);
		p1.add(p2,BorderLayout.CENTER);
		bottom.add(p1,BorderLayout.NORTH);
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		buttons.add(addButton);
		buttons.add(deleteButton);
		addButton.setEnabled(networkEnabled);
		deleteButton.setEnabled(false);
		applyButton.setEnabled(false);
		bottom.add(buttons,BorderLayout.SOUTH);
		networkWorkerList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				deleteButton.setEnabled(enableNetworkCheckBox.isSelected() && networkWorkerList.getSelectedIndices().length > 0);
			}
		});
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				if (! applyButton.isEnabled()) 
					dispose();
				else {
					int ans = JOptionPane.showConfirmDialog(MultiprocessingConfigDialog.this,
							"You have unsaved network configuration changes.\nDo you want to save them?", 
							"Save Network Config?", JOptionPane.YES_NO_CANCEL_OPTION);
					if (ans == JOptionPane.YES_OPTION)
						doApply();
					if (ans != JOptionPane.CANCEL_OPTION)
						dispose();
				}
			}
		});
		pack();
		setResizable(false);
		if (parent != null)
			setLocation(parent.getX() + 20, parent.getY() + 50);
		content.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke("ESCAPE"), "cancel");
		content.getActionMap().put("cancel",new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				cancelButton.doClick();
			}
		});
		if (networkEnabled)
			startUpdateTimer(5000);
	}

	private class NetworkWorkerListModel extends AbstractListModel {
		ArrayList<TaskManager.NetworkWorkerInfo> items = new ArrayList<TaskManager.NetworkWorkerInfo>();
		NetworkWorkerListModel(TaskManager.NetworkWorkerInfo[] data) {
			if (data != null)
				for (TaskManager.NetworkWorkerInfo item : data)
					items.add(item);
		}
		public void add(TaskManager.NetworkWorkerInfo item) {
			items.add(item);
			fireIntervalAdded(this, items.size()-1, items.size()-1);
		}
		public void remove(int index) {
			items.remove(index);
			fireIntervalRemoved(this, index, index);
		}
		public void inactivateAll() {
			if (items.size() == 0)
				return;
			for (int i = 0; i < items.size(); i++) {
				TaskManager.NetworkWorkerInfo info = items.get(i);
				items.set(i, new TaskManager.NetworkWorkerInfo(info.host,info.port,TaskManager.NET_STATUS_INACTIVE,0,-1));
			}
			fireContentsChanged(this, 0, items.size()-1);
		}
		public Object getElementAt(int index) {
			return makeListItem(items.get(index));
		}
		public int getSize() {
			return items.size();
		}
		public void setData(ArrayList<NetworkWorkerInfo> newItems) {
			int n = items.size();
			if (n > 0) {
				items.clear();
				fireIntervalRemoved(this, 0, n-1);
			}
			items = newItems;
			if (items.size() > 0)
				fireIntervalAdded(this, 0, items.size()-1);
		}
	}

	private class ButtonHandler implements ActionListener {
		public void actionPerformed(ActionEvent evt) {
			Object src = evt.getSource();
			if (src == defaultProcessCountCheckBox) {
				boolean useDefault = defaultProcessCountCheckBox.isSelected();
				if (useDefault) {
					int availableProcessors = Runtime.getRuntime().availableProcessors();
					processCountInput.setEditable(false);
					processCountInput.setText("" + availableProcessors);
				}
				else {
					processCountInput.setEditable(true);
					processCountInput.selectAll();
					processCountInput.requestFocus();
				}
			}
			else if (src == enableNetworkCheckBox) {
				boolean enable = enableNetworkCheckBox.isSelected();
				if (enable) {
					networkWorkerList.setEnabled(true);
					addButton.setEnabled(true);
					applyButton.setEnabled(!taskManager.getNetworkingEnabled() && networkWorkerModel.getSize() > 0);
				}
				else {
					networkWorkerList.setEnabled(false);
					networkWorkerList.clearSelection();
					applyButton.setEnabled(taskManager.getNetworkingEnabled());
					addButton.setEnabled(false);
					deleteButton.setEnabled(false);
				}
			}
			else if (src == deleteButton) {
				int[] sel = networkWorkerList.getSelectedIndices();
				for (int i = sel.length-1; i >= 0; i--)
					networkWorkerModel.remove(sel[i]);
				if (sel.length > 0)
					applyButton.setEnabled(true);
			}
			else if (src == applyButton) {
				doApply();
			}
			else if (src == addButton) {
				TaskManager.NetworkWorkerInfo info = showAddDialog();
				if (info != null) {
					networkWorkerModel.add(info);
					applyButton.setEnabled(true);
				}
			}
			else if (src == cancelButton)
				dispose();
			else if (src == okButton) {
				int processCount;
				if (defaultProcessCountCheckBox.isSelected())
					processCount = Runtime.getRuntime().availableProcessors();
				else {
					try {
						processCount = Integer.parseInt(processCountInput.getText().trim());
						if (processCount <= 0)
							throw new NumberFormatException();
					}
					catch (NumberFormatException e) {
						JOptionPane.showMessageDialog(MultiprocessingConfigDialog.this, 
								I18n.tr("multiprocessingConfigDialog.error.BadNumberOfProcesses"));
						processCountInput.selectAll();
						processCountInput.requestFocus();
						return;
					}
				}
				taskManager.setThreadPoolSize(processCount);
				doApply();
				dispose();
			}
		}
	}

	private void doApply() {
		if (enableNetworkCheckBox.isSelected()) {
			taskManager.setNetworkingEnabled(true);
			TaskManager.NetworkWorkerInfo[] currentItems = taskManager.getAllNetworkWorkerInfo();
			ArrayList<TaskManager.NetworkWorkerInfo> desiredItems = networkWorkerModel.items;
			for (TaskManager.NetworkWorkerInfo item : currentItems) {
				boolean found = false;
				for (TaskManager.NetworkWorkerInfo dItem : desiredItems)
					if (dItem.workerID == item.workerID) {
						found = true;
						break;
					}
				if (!found)
					taskManager.removeNetworkWorker(item.workerID);
			}
			ArrayList<TaskManager.NetworkWorkerInfo> newItems = new ArrayList<TaskManager.NetworkWorkerInfo>();
			for (TaskManager.NetworkWorkerInfo item: desiredItems) {
				if (item.workerID == -1) {
					int id = taskManager.addNetworkWorker(item.host, item.port);
					newItems.add(taskManager.getNetworkWorkerInfo(id));
				}
				else {	
					TaskManager.NetworkWorkerInfo oldInfo = taskManager.getNetworkWorkerInfo(item.workerID);
					if (oldInfo == null) {
						int id = taskManager.addNetworkWorker(item.host, item.port);
						newItems.add(taskManager.getNetworkWorkerInfo(id));
					}
					else if (oldInfo.status != TaskManager.NET_STATUS_CONNECTED) {
						taskManager.removeNetworkWorker(item.workerID);
						int id = taskManager.addNetworkWorker(item.host, item.port);
						newItems.add(taskManager.getNetworkWorkerInfo(id));
					}
					else {
						newItems.add(oldInfo);
					}
				}
			}
			networkWorkerModel.setData(newItems);
			startUpdateTimer(500);
		}
		else {
			if (updatetimer != null) {
				updatetimer.stop();
				updatetimer = null;
			}
			taskManager.setNetworkingEnabled(false);
			networkWorkerModel.inactivateAll();
		}
		applyButton.setEnabled(false);
		saveNetworkPrefs(networkWorkerModel.items);
	}

	private void startUpdateTimer(int initialDelay) {
		if (updatetimer != null)
			updatetimer.stop();
		updatetimer = new Timer(5000, new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				if ( ! taskManager.getNetworkingEnabled() )
					return;
				ArrayList<TaskManager.NetworkWorkerInfo> items = networkWorkerModel.items;
				ArrayList<TaskManager.NetworkWorkerInfo> newItems = new ArrayList<TaskManager.NetworkWorkerInfo>();
				boolean changed = false;
				for (TaskManager.NetworkWorkerInfo item : items) {
					if (item.workerID == -1)
						newItems.add(item);
					else {
						TaskManager.NetworkWorkerInfo current = taskManager.getNetworkWorkerInfo(item.workerID);
						if (current == null)
							changed = true;
						else {
							newItems.add(current);
							if (current.status != item.status || current.tasksDone != item.tasksDone)
								changed = true;
						}
					}
				}
				if (changed)
					networkWorkerModel.setData(newItems);
			}
		});
		updatetimer.setInitialDelay(initialDelay);
		updatetimer.start();
	}

	private void saveNetworkPrefs(ArrayList<TaskManager.NetworkWorkerInfo> items) {
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < items.size(); i++) {
			TaskManager.NetworkWorkerInfo item = items.get(i);
			b.append(item.host);
			b.append(':');
			b.append(item.port);
			if (i < items.size() - 1)
				b.append(';');
		}
		Util.setPref("MultiprocessingConfigDialog.networkWorkerList", b.toString());
	}

	private TaskManager.NetworkWorkerInfo[] getNetworkPrefs() {
		String netpref = Util.getPref("MultiprocessingConfigDialog.networkWorkerList");
		if (netpref == null)
			return null;
		ArrayList<TaskManager.NetworkWorkerInfo> list = new ArrayList<TaskManager.NetworkWorkerInfo>();
		try {
			String[] hosts = netpref.split(";");
			for (String itemString : hosts) {
				String[] info = itemString.split(":");
				String host = info[0].trim();
				int port = Integer.parseInt(info[1]);
				if (host.length() == 0 || port <= 0)
					return null;
				list.add(new TaskManager.NetworkWorkerInfo(host,port,TaskManager.NET_STATUS_INACTIVE,0,-1));
			}
		}
		catch (Exception e) {
			return null;
		}
		if (list.size() == 0)
			return null;
		else {
			TaskManager.NetworkWorkerInfo[] workers = new TaskManager.NetworkWorkerInfo[list.size()];
			for (int i = 0; i < workers.length; i++)
				workers[i] = list.get(i);
			return workers;
		}
	}

	private String makeListItem(TaskManager.NetworkWorkerInfo info) {
		StringBuffer b = new StringBuffer();
		b.append("<html>");
		if (info.status == TaskManager.NET_STATUS_INACTIVE)
			b.append("<font color='#AAAAAA'>");
		b.append(info.host);
		b.append(':');
		b.append(info.port);
		b.append(' ');
		if (info.status == TaskManager.NET_STATUS_INACTIVE) {
			b.append(I18n.tr("multiprocessingConfigDialog.netStatus.INACTIVE"));
			b.append("</font>");
		}
		else if (info.status < 0) {
			b.append("<font color=red>");
			if (info.status == TaskManager.NET_STATUS_ERROR_CANT_CONNECT)
				b.append(I18n.tr("multiprocessingConfigDialog.netStatus.CANTCONNECT"));
			else
				b.append(I18n.tr("multiprocessingConfigDialog.netStatus.ERROR"));
			b.append("</font>");
		}
		else if (info.status == TaskManager.NET_STATUS_CONNECTED) {
			b.append("<font color='#00BB00'>");
			b.append(I18n.tr("multiprocessingConfigDialog.netStatus.CONNECTED"));
			b.append("</font>");
		}
		else if (info.status == TaskManager.NET_STATUS_CONNECTING)
			b.append(I18n.tr("multiprocessingConfigDialog.netStatus.CONNECTING"));
		else if (info.status == TaskManager.NET_STATUS_CLOSING)
			b.append(I18n.tr("multiprocessingConfigDialog.netStatus.CLOSING"));
		else if (info.status == TaskManager.NET_STATUS_CLOSED)
			b.append(I18n.tr("multiprocessingConfigDialog.netStatus.CLOSED"));
		if (info.tasksDone > 0) {
			b.append(' ');
			b.append(I18n.tr("multiprocessingConfigDialog.netStatus.tasksDone",""+info.tasksDone));
		}
		b.append("</html>");
		return b.toString();
	}

	TaskManager.NetworkWorkerInfo showAddDialog() {
		JPanel panel = new JPanel();
		int defaultPort = MandelbrotNetworkTaskServer.DEFAULT_PORT;
		JTextField hostInput = new JTextField(30);
		JTextField portInput = new JTextField(""+defaultPort,5);
		panel.setLayout(new BorderLayout(5,5));
		panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		panel.add(new JLabel(I18n.tr("multiprocessingConfigdialog.addNetworkHostDialog.info",""+defaultPort)), BorderLayout.NORTH);
		JPanel p1 = new JPanel();
		p1.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		p1.add(Box.createHorizontalStrut(10));
		p1.add(new JLabel(I18n.tr("multiprocessingConfigdialog.addNetworkHostDialog.HostName")+ ": "));
		p1.add(hostInput);
		panel.add(p1,BorderLayout.CENTER);
		JPanel p2 = new JPanel();
		p2.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		p2.add(Box.createHorizontalStrut(10));
		p2.add(new JLabel(I18n.tr("multiprocessingConfigdialog.addNetworkHostDialog.PortNumber")+ ": "));
		p2.add(portInput);
		panel.add(p2,BorderLayout.SOUTH);
		int ok = JOptionPane.showConfirmDialog(display,panel,
				I18n.tr("multiprocessingConfigdialog.addNetworkHostDialog.title"),JOptionPane.OK_CANCEL_OPTION);
		if (ok != JOptionPane.OK_OPTION)
			return null;
		String host = hostInput.getText().trim();
		if (host.length() == 0) {
			JOptionPane.showMessageDialog(display, I18n.tr("multiprocessingConfigdialog.addNetworkHostDialog.error.EmptyHost"));
			return null;
		}
		if (host.indexOf(';') >= 0) {
			JOptionPane.showMessageDialog(display, I18n.tr("multiprocessingConfigdialog.addNetworkHostDialog.error.NoSemicolonInHost"));
			return null;
		}
		if (host.indexOf(':') >= 0) {
			JOptionPane.showMessageDialog(display, I18n.tr("multiprocessingConfigdialog.addNetworkHostDialog.error.NoColonInHost"));
			return null;
		}
		int port;
		try {
			port = Integer.parseInt(portInput.getText().trim());
			if (port <= 0)
				throw new NumberFormatException();
		}
		catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(display, I18n.tr("multiprocessingConfigdialog.addNetworkHostDialog.error.BadPortNumber"));
			return null;
		}
		return new TaskManager.NetworkWorkerInfo(host,port,TaskManager.NET_STATUS_INACTIVE,0,-1);
	}

}
