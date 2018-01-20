/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool 
          with fuzzy matching, translation memory, keyword search, 
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2008 Alex Buloichik
               2010 Volker Berlin
               Home page: http://www.omegat.org/
               Support center: http://groups.yahoo.com/group/OmegaT/

 This file is part of OmegaT.

 OmegaT is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 OmegaT is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.omegat.filters;

import java.util.List;

import org.junit.Test;
import net.scottie.tipe.TipeFilter;

import static org.junit.Assert.*;

public class TipeFilterTest extends TestFilterBase {

    @Test
    public void testTextFilterParsing() throws Exception {
        List<String> entries = parse(new TipeFilter(), "/filters/tipe/test.tip");
        int i = 0;
        assertEquals("Title text", entries.get(i++));
        assertEquals("Strong text and <e1>strong italic text</e1>", entries.get(i++));
        assertEquals("Block quote level 1", entries.get(i++));
        assertEquals("Block quote level 2", entries.get(i++));
        assertEquals("<a1>Table text 1</a1>", entries.get(i++));
        assertEquals("http://url1.net", entries.get(i++));
        assertEquals("Table text 2", entries.get(i++));
        assertEquals("list item 1", entries.get(i++));
        assertEquals("<a2>Link</a2> (description)", entries.get(i++));
        assertEquals("https://url2.org", entries.get(i++));
        assertEquals("<e2><e2>Bad tags <s3>here</s4></e2>", entries.get(i++));
    }

    @Test
    public void testTranslate() throws Exception {
        translateText(new TipeFilter(), "/filters/tipe/test.tip");
    }
}
