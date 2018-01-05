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
import org.omegat.core.data.IProject;
import org.omegat.gui.editor.IEditor;
import org.omegat.gui.editor.IPopupMenuConstructor;
import org.omegat.gui.editor.SegmentBuilder;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Editor popup menu constructor.
 */
public class TipePopupMenuConstructor implements IPopupMenuConstructor {
    private final ResourceBundle rb;
    private final IEditor editor = Core.getEditor();

    /**
     * Constructor.
     */
    public TipePopupMenuConstructor() {
        ResourceBundle.Control utf8Control = new UTF8Control();
        rb = ResourceBundle.getBundle("TipeStrings", Locale.getDefault(), utf8Control);
    }

    /**
     * Check if current file is supported by filter.
     * @return check result
     */
    private static boolean isSupportedFile() {
        String filePath = Core.getEditor().getCurrentFile();
        if (filePath == null) {
            return false;
        }
        for (IProject.FileInfo fi : Core.getProject().getProjectFiles()) {
            if (fi.filePath.equals(filePath)
                    && fi.filterFileFormatName.equals(TipeFilter.FILTER_NAME)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wrap around selected text (if any) into a given tag.
     * @param tagName
     * @return formatted string
     */
    private String createExtraTag(final String tagName) {
        String selectedText = editor.getSelectedText();
        if (selectedText == null) {
            selectedText = "";
        }

        return String.format("<%s>%s</%s>", tagName, selectedText, tagName);
    }

    /**
     * Add new items to popup menu.
     * @param menu parent menu
     * @param comp nevermind
     * @param mousepos nevermind
     * @param isInActiveEntry nevermind
     * @param isInActiveTranslation nevermind
     * @param sb nevermind
     */
    public void addItems(final JPopupMenu menu,
                         final JTextComponent comp,
                         final int mousepos,
                         final boolean isInActiveEntry,
                         final boolean isInActiveTranslation,
                         final SegmentBuilder sb) {

        if (!isSupportedFile()) {
            return;
        }

        JMenu pluginSubMenu = new JMenu();
        pluginSubMenu.setText(rb.getString("POPUP_MENU_NAME"));
        JMenuItem item = new JMenuItem();
        item.setText(rb.getString("POPUP_MENU_FORMAT_STRONG"));
        item.addActionListener(e -> Core.getEditor().insertText(createExtraTag("strong")));
        pluginSubMenu.add(item);
        item = new JMenuItem();
        item.setText(rb.getString("POPUP_MENU_FORMAT_EMPHASIS"));
        item.addActionListener(e -> Core.getEditor().insertText(createExtraTag("em")));
        pluginSubMenu.add(item);
        menu.addSeparator();
        menu.add(pluginSubMenu);
        menu.addSeparator();
    }
}
