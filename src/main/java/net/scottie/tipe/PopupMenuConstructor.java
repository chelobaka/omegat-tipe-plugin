/**************************************************************************
 Tipe³ file filter for OmegaT

 Copyright (C) 2018 Lev Abashkin

 This file is NOT a part of OmegaT.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package net.scottie.tipe;

import org.omegat.core.Core;
import org.omegat.gui.editor.IPopupMenuConstructor;
import org.omegat.gui.editor.SegmentBuilder;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Editor popup menu constructor.
 */
public class PopupMenuConstructor implements IPopupMenuConstructor {

    private static final Pattern SHORTCUT_PAIR = Pattern.compile("(<([a-z]\\d+)>).+?(</\\2>)");
    private static final Pattern SHORTCUT_SINGLE = Pattern.compile("<([a-z]\\d+)/>");

    /**
     * Wrap around selected text (if any) into a given tag.
     * @param tagName tag name
     * @return formatted string
     */
    private static String createExtraTag(final String tagName) {
        String selectedText = Core.getEditor().getSelectedText();
        if (selectedText == null) {
            selectedText = "";
        }
        return String.format("<%s>%s</%s>", tagName, selectedText, tagName);
    }

    /**
     * Add items to popup menu.
     * @param menu menu
     * @param comp text component
     * @param mousepos mouse position
     * @param isInActiveEntry is in active entry?
     * @param isInActiveTranslation is in active translation?
     * @param sb segment builder
     */
    public void addItems(final JPopupMenu menu,
                          final JTextComponent comp,
                          final int mousepos,
                          final boolean isInActiveEntry,
                          final boolean isInActiveTranslation,
                          final SegmentBuilder sb) {

        if (!Util.isTipeFile()) {
            return;
        }

        String selection = Core.getEditor().getSelectedText();
        if (selection == null) {
            selection = "";
        }

        JMenu pluginSubMenu = new JMenu();
        pluginSubMenu.setText(Util.RESOURCE_BUNDLE.getString("POPUP_MENU_NAME"));

        /* Found shortcuts */
        boolean shortcutsFound = false;
        String src = Core.getEditor().getCurrentEntry().getSrcText();
        Set<String> foundShortcuts = new HashSet<>();
        Matcher matcher = SHORTCUT_PAIR.matcher(src);
        while (matcher.find()) {

            if (foundShortcuts.contains(matcher.group(2))) {
                continue;
            } else {
                foundShortcuts.add(matcher.group(2));
            }

            JMenuItem item = new JMenuItem();
            item.setText(matcher.group(1) + "…" + matcher.group(3));
            String insertion = matcher.group(1) + selection + matcher.group(3);
            item.addActionListener(e -> Core.getEditor().insertText(insertion));
            pluginSubMenu.add(item);
            shortcutsFound = true;
        }

        matcher = SHORTCUT_SINGLE.matcher(src);
        while (matcher.find()) {
            String shortcut = matcher.group(0);
            JMenuItem item = new JMenuItem();
            item.setText(shortcut);
            item.addActionListener(e -> Core.getEditor().insertText(shortcut));
            pluginSubMenu.add(item);
            shortcutsFound = true;
        }
        if (shortcutsFound) {
            pluginSubMenu.addSeparator();
        }

        for (Map.Entry<String, String> entry : Util.TAG_MAP.entrySet()) {
            JMenuItem item = new JMenuItem();
            item.setText(Util.RESOURCE_BUNDLE.getString(entry.getValue()));
            String insertion = createExtraTag(entry.getKey());
            item.addActionListener(e -> Core.getEditor().insertText(insertion));
            pluginSubMenu.add(item);
        }

        menu.addSeparator();
        menu.add(pluginSubMenu);
        menu.addSeparator();
    }
}
