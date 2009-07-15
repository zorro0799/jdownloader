//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JSeparator;

import jd.OptionalPluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigGroup;
import jd.config.MenuItem;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.JDUnderlinedText;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class AddonTaskPane extends TaskPanel implements ActionListener {

    private static final long serialVersionUID = -7720749076951577192L;
    private HashMap<Object, OptionalPluginWrapper> buttonMap;
    private HashMap<String, JCheckBox> chks;
    private JButton config;
    private ArrayList<CollapseButton> entries;

    public AddonTaskPane(String string, ImageIcon ii) {
        super(string, ii, "addons");

        initGUI();
    }

    public void initGUI() {
        int width = this.getWidth();
        int height = this.getHeight();
        this.removeAll();
        this.buttonMap = new HashMap<Object, OptionalPluginWrapper>();
        this.entries = new ArrayList<CollapseButton>();
        this.chks = new HashMap<String, JCheckBox>();
        for (OptionalPluginWrapper wrapper : OptionalPluginWrapper.getOptionalWrapper()) {
            if (!wrapper.isEnabled() || wrapper.getPlugin() == null) continue;
            ArrayList<MenuItem> menuItems = wrapper.getPlugin().createMenuitems();
            if (menuItems != null) {
                if (menuItems.size() > 1 || wrapper.getPlugin().getConfig().getEntries().size() > 0) {
                    CollapseButton bt = new CollapseButton(wrapper.getPlugin().getHost(), JDTheme.II(wrapper.getPlugin().getIconKey(), 16, 16));
                    add(bt);

                    for (MenuItem entry : menuItems) {
                        JComponent comp = createMenu(entry, null, wrapper);
                        comp.setOpaque(false);

                        bt.getContentPane().add(comp, "gapleft 20");
                    }
                    bt.getButton().addActionListener(this);
                    buttonMap.put(bt.getButton(), wrapper);
                    entries.add(bt);
                } else if (menuItems.size() == 1) {
                    JComponent btn = createMenu(menuItems.get(0), JDTheme.II(wrapper.getPlugin().getIconKey(), 16, 16), wrapper);
                    btn.setOpaque(false);
                    if (menuItems.get(0).getType() == MenuItem.TOGGLE) {
                        add(btn);
                    } else {
                        add(btn);
                    }
                }
            } else if (wrapper.isEnabled()) {
                menuItems = new ArrayList<MenuItem>();
                MenuItem mi;
                menuItems.add(mi = new MenuItem(wrapper.getPlugin().getHost(), 0));
                mi.setActionListener(this);
                buttonMap.put(mi, wrapper);
                JComponent btn = createMenu(menuItems.get(0), JDTheme.II(wrapper.getPlugin().getIconKey(), 16, 16), wrapper);
                btn.setOpaque(false);
                if (menuItems.get(0).getType() == MenuItem.TOGGLE) {
                    add(btn);
                } else {
                    add(btn);
                }
            }
        }
        add(new JSeparator());

        config = createButton(JDL.L("gui.tasks.addons.edit", "Addonmanager"), JDTheme.II("gui.images.config.addons", 16, 16));

        add(config);
        this.revalidate();
        this.repaint(0, 0, width, height);
    }

    public void actionPerformed(ActionEvent e) {
        OptionalPluginWrapper wrapper = buttonMap.get(e.getSource());
        boolean isAction = false;
        if (wrapper != null) {
            ConfigContainer cfg = wrapper.getPlugin().getConfig();
            if (cfg != null) {
                if (cfg.getContainerNum() == 0) {
                    int i = 0;
                    ConfigGroup group = new ConfigGroup(wrapper.getPlugin().getHost(), JDTheme.II(wrapper.getPlugin().getIconKey(), 32, 32));
                    while (cfg.getEntryAt(i) != null) {
                        cfg.getEntryAt(i).setGroup(group);

                        i++;
                    }
                }

                updateToggle(wrapper);

                isAction = true;
                SimpleGUI.CURRENTGUI.getContentPane().display(new ConfigEntriesPanel(cfg));
            }
        }
        if (e.getSource() == config) {
            SimpleGUI.CURRENTGUI.getContentPane().display(SimpleGUI.CURRENTGUI.getCfgTskPane().getPanel(ConfigTaskPane.ACTION_ADDONS));
            isAction = true;
        }
        if (isAction) {
            for (CollapseButton entry : entries) {
                if (entry.getButton() != e.getSource()) {
                    entry.setCollapsed(true);
                }
            }
        }
    }

    private JComponent createMenu(final MenuItem entry, final ImageIcon ii, final OptionalPluginWrapper wrapper) {
        JButton bt;
        switch (entry.getType()) {
        case MenuItem.CONTAINER:
            CollapseButton col = new CollapseButton(entry.getTitle(), ii);
            for (int i = 0; i < entry.getSize(); i++) {
                switch (entry.get(i).getType()) {
                case MenuItem.CONTAINER:
                case MenuItem.NORMAL:
                case MenuItem.SEPARATOR:
                    col.getContentPane().add(createMenu(entry.get(i), ii, wrapper), "gapleft 10");
                    break;

                case MenuItem.TOGGLE:
                    col.getContentPane().add(createMenu(entry.get(i), ii, wrapper), "gapleft 10");
                    break;
                }
            }

            col.getButton().addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    entry.getActionListener().actionPerformed(new ActionEvent(entry, entry.getActionID(), arg0.getActionCommand()));
                }
            });
            return col;
        case MenuItem.NORMAL:
            bt = createButton(entry.getTitle(), ii);
            bt.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    entry.getActionListener().actionPerformed(new ActionEvent(entry, entry.getActionID(), arg0.getActionCommand()));
                }
            });

            return bt;
        case MenuItem.SEPARATOR:
            return new JSeparator();

        case MenuItem.TOGGLE:
            JCheckBox ch = new JCheckBox(entry.getTitle(), entry.isSelected());
            ch.setContentAreaFilled(false);

            ch.setFocusPainted(false);
            ch.setBorderPainted(false);
            ch.addMouseListener(new JDUnderlinedText(ch));
            ch.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    entry.getActionListener().actionPerformed(new ActionEvent(entry, entry.getActionID(), arg0.getActionCommand()));
                }
            });
            chks.put(wrapper.getPlugin().getHost() + "-" + entry.getTitle(), ch);
            return ch;
        }
        return null;
    }
/**
 * overrides onHide to collapse all entries on hide
 */
    public void onHide() {
        super.onHide();
        for (CollapseButton entry : entries) {

            entry.setCollapsed(true);

        }
    }

    public void updateToggle() {
        for (OptionalPluginWrapper wrapper : OptionalPluginWrapper.getOptionalWrapper()) {
            if (!wrapper.isEnabled() || wrapper.getPlugin() == null) continue;
            updateToggle(wrapper);
        }
    }

    private void updateToggle(OptionalPluginWrapper wrapper) {
        ArrayList<MenuItem> menuItems = wrapper.getPlugin().createMenuitems();
        if (menuItems != null) {
            for (MenuItem entry : wrapper.getPlugin().createMenuitems()) {
                if (entry.getType() == MenuItem.TOGGLE) {
                    chks.get(wrapper.getPlugin().getHost() + "-" + entry.getTitle()).setSelected(entry.isSelected());
                }
            }
        }
    }
}