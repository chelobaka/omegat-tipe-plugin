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

import org.omegat.core.data.SourceTextEntry;
import org.omegat.gui.editor.mark.IMarker;
import org.omegat.gui.editor.mark.Mark;
import org.omegat.util.gui.Styles;

import javax.swing.text.AttributeSet;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Marker of HTML tags.
 */
class HTMLTagMarker implements IMarker {

    private static final Color TAG_FONT_COLOR = new Color(36, 163, 11);

    private static final AttributeSet ATTRIBUTES = Styles
            .createAttributeSet(TAG_FONT_COLOR, null, null, null);

    // Build pattern for extra HTML elements to be highlighted
    private static final Pattern HTML_TAG_PATTERN;
    static {
        String patternString = "</?(" + String.join("|", Util.TAG_MAP.keySet()) + ")>";
        HTML_TAG_PATTERN = Pattern.compile(patternString);
    }

    public List<Mark> getMarksForEntry(final SourceTextEntry ste, final String sourceText,
                                       final String translationText, final boolean isActive)
            throws Exception {

        if (translationText == null || !Util.isTipeFile()) {
            return null;
        }

        Matcher matcher = HTML_TAG_PATTERN.matcher(translationText);
        if (!matcher.find()) {
            return null;
        }

        List<Mark> result = new ArrayList<>();

        do {
            Mark mark = new Mark(Mark.ENTRY_PART.TRANSLATION, matcher.start(), matcher.end());
            mark.painter = null;
            mark.attributes = ATTRIBUTES;
            String rbName = Util.TAG_MAP.get(matcher.group(1));
            mark.toolTipText = Util.RESOURCE_BUNDLE.getString(rbName);
            result.add(mark);
        } while (matcher.find());

        return result;
    }
}
