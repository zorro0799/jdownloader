package jd.gui.swing.jdgui.settings.panels.premium.Columns;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPasswordField;
import javax.swing.JTable;

import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.gui.swing.jdgui.settings.panels.premium.HostAccounts;
import jd.plugins.Account;
import jd.utils.JDUtilities;

class JDPasswordField extends JPasswordField implements ClipboardOwner {

    private static final long serialVersionUID = -7981118302661369727L;

    public JDPasswordField() {
        super();
    }

    @Override
    public void cut() {
        if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL) {
            StringSelection stringSelection = new StringSelection(String.valueOf(this.getSelectedText()));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);

            String text = String.valueOf(this.getPassword());
            int position = this.getSelectionStart();
            String s1 = text.substring(0, position);
            String s2 = text.substring(this.getSelectionEnd(), text.length());
            this.setText(s1 + s2);

            this.setSelectionStart(position);
            this.setSelectionEnd(position);
        }
    }

    @Override
    public void copy() {
        if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL) {
            StringSelection stringSelection = new StringSelection(String.valueOf(this.getSelectedText()));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);
        }
    }

    public void lostOwnership(Clipboard arg0, Transferable arg1) {
    }

}

public class PassColumn extends JDTableColumn implements ActionListener {

    private jd.gui.swing.jdgui.settings.panels.premium.Columns.JDPasswordField passw;

    public PassColumn(String name, JDTableModel table) {
        super(name, table);
        passw = new JDPasswordField();
    }

    /**
     * 
     */
    private static final long serialVersionUID = -5291590062503352550L;
    private Component co;
    private Component coedit;
    private static Dimension dim = new Dimension(200, 30);

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        passw.removeActionListener(this);
        passw.setText(((Account) value).getPass());
        passw.addActionListener(this);
        coedit = passw;
        return co;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        hasFocus = false;
        column = this.getJDTableModel().toModel(column);
        if (value instanceof Account) {
            Account ac = (Account) value;
            co = getDefaultTableCellRendererComponent(table, "*****", isSelected, hasFocus, row, column);
            co.setEnabled(ac.isEnabled());
        } else {
            HostAccounts ha = (HostAccounts) value;
            co = getDefaultTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            co.setEnabled(ha.isEnabled());
            co.setBackground(table.getBackground().darker());
        }
        co.setSize(dim);
        return co;
    }

    @Override
    public boolean isEditable(Object ob) {
        if (ob != null && ob instanceof Account) return true;
        return false;
    }

    @Override
    public void setValue(Object value, Object o) {
        String pw = (String) value;
        if (o instanceof Account) ((Account) o).setPass(pw);
    }

    @Override
    public Object getCellEditorValue() {
        if (coedit == null || !(coedit instanceof JDPasswordField)) return null;
        return new String(((JDPasswordField) coedit).getPassword());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        passw.removeActionListener(this);
        this.fireEditingStopped();
    }

}
