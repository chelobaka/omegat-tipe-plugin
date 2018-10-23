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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;


/**
 * Common utility stuff.
 */
final class Util {

    static final String SOURCE_FILENAME_MASK  = "*.tip";

    // Resource bundle
    static final ResourceBundle RESOURCE_BUNDLE;
    static {
        ResourceBundle.Control utf8Control = new UTF8Control();
        RESOURCE_BUNDLE = ResourceBundle.getBundle("TipeStrings", Locale.getDefault(), utf8Control);
    }

    // Filter name for OmegaT
    static final String FILTER_NAME = RESOURCE_BUNDLE.getString("FILTER_NAME");

    // Tag names with corresponding resource bundle strings
    static final Map<String, String> TAG_MAP = new LinkedHashMap<>();
    static {
        TAG_MAP.put("strong", "FORMAT_STRONG");
        TAG_MAP.put("em", "FORMAT_EMPHASIS");
        TAG_MAP.put("sup", "FORMAT_SUPERSCRIPT");
        TAG_MAP.put("sub", "FORMAT_SUBSCRIPT");
    };

    /**
     * Check if current file is supported by filter.
     * @return check result
     */
    static boolean isTipeFile() {
        String filePath = Core.getEditor().getCurrentFile();
        if (filePath == null) {
            return false;
        }
        for (IProject.FileInfo fi : Core.getProject().getProjectFiles()) {
            if (fi.filePath.equals(filePath)
                    && fi.filterFileFormatName.equals(Util.FILTER_NAME)) {
                return true;
            }
        }
        return false;
    }

    private Util() {
        // Disable instance creation.
    }
}
