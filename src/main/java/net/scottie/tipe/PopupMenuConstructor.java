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
import java.util.Map;

/**
 * Editor popup menu constructor.
 */
public class PopupMenuConstructor implements IPopupMenuConstructor {

    /**
     * Wrap around selected text (if any) into a given tag.
     * @param tagName
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
     * Add new items to popup menu.
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

        JMenu pluginSubMenu = new JMenu();
        pluginSubMenu.setText(Util.RESOURCE_BUNDLE.getString("POPUP_MENU_NAME"));

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
