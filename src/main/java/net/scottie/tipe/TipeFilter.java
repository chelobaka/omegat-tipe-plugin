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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.omegat.core.Core;

import org.omegat.core.CoreEvents;
import org.omegat.core.events.IApplicationEventListener;
import org.omegat.filters2.AbstractFilter;
import org.omegat.filters2.FilterContext;
import org.omegat.filters2.Instance;
import org.omegat.util.LinebreakPreservingReader;

/**
 * tipe³ web authoring format filter for OmegaT.
 *
 * @author Lev Abashkin
 */
public class TipeFilter extends AbstractFilter {

    private static final Pattern ATOMIC_PATTERN =
            Pattern.compile("^\\s+|\\{\\{IMG.+?}}\\s*|\\n\\s*");

    private static final Pattern HREF_PATTERN = Pattern.compile("href=\"(.+?)\"");

    // Text formatting HTML tags
    private static final String[] FORMATTING_TAG_NAMES = {
        "strong",
        "em",
        "a",
        "strike",
        "sub",
        "sup",
        "span"
    };

    // Block level HTML tags
    private static final String[] BLOCK_TAG_NAMES = {
        "div",
        "iframe",
        "ul",
        "ol",
        "li",
        "p",
        "blockquote",
        "table",
        "tbody",
        "tr",
        "td",
        "th"
    };

    private static final String[][] SPECIAL_HTML_CHARACTERS = {
        {"&amp;", "&"},
        {"&nbsp;", " "},
        {"&quot;", "\""}
    };

    /**
     * Block type enum.
     */
    private enum BlockType {
        ATOMIC,  // Indivisible block without payload
        TAG,     // Formatting HTML tag
        PAYLOAD  // Text
    }

    /**
     * Tag types enum.
     */
    private enum TagType {
        OPENING,
        CLOSING,
        ANY      // This one for making pattern to match both types
    }

    /**
     * Helper class for block objects.
     */
    class Block implements Comparable<Block> {
        private final int start;
        private final int end;
        private final BlockType type;

        Block(final BlockType type, final int start, final int end) {
            this.type = type;
            this.start = start;
            this.end = end;
        }

        int getStart() {
            return start;
        }

        int getEnd() {
            return end;
        }

        BlockType getType() {
            return type;
        }

        @Override
        public int compareTo(@NotNull final Block that) {
            return Integer.compare(this.start, that.start);
        }
    }

    /**
     * Helper class for HTML tag objects.
     */
    class HTMLTag extends Block {
        private final String name;
        private final TagType tagType;
        private String metaBody;
        // Tag comment would be sent inside entry comment to OmegaT.
        // The purpose of this is passing HREF links to the editor.
        private String comment;
        private HTMLTag pair;

        String getMetaBody() {
            return metaBody;
        }

        String getName() {
            return name;
        }

        HTMLTag getPair() {
            return pair;
        }

        boolean hasPair() {
            return pair != null;
        }

        boolean hasMetaBody() {
            return metaBody != null;
        }

        boolean isOpening() {
            return tagType == TagType.OPENING;
        }

        void setMetaBody(final String metaBody) {
            this.metaBody = metaBody;
        }

        void setPair(final HTMLTag pair) {
            this.pair = pair;
        }

        String getComment() {
            return comment;
        }

        void setComment(final String comment) {
            this.comment = comment;
        }

        HTMLTag(final String name, final int start, final int end, final TagType tagType) {
            super(BlockType.TAG, start, end);
            this.name = name;
            this.tagType = tagType;
            metaBody = null;
            pair = null;
            comment = null;
        }
    }

    /* Private fields */

    private final Pattern blockTagPattern, oFTagPattern, cFTagPattern;
    private String doc; // current document

    // Final document blocks
    private List<Block> documentBlocks;

    private BufferedWriter fileWriter;

    // Structures for tag manipulations
    private Map<String, String> metaToHtmlMap;
    private Map<String, String> htmlToMetaMap;
    private Map<Character, Integer> metaCounters;

    /* End of private fields */

    // Register marker
    static {
         Core.registerMarker(new HTMLTagMarker());
    }

    /**
     * Constructor.
     */
    public TipeFilter() {

        // Build tag patterns
        oFTagPattern = buildTagPattern(FORMATTING_TAG_NAMES, TagType.OPENING);
        cFTagPattern = buildTagPattern(FORMATTING_TAG_NAMES, TagType.CLOSING);
        blockTagPattern = buildTagPattern(BLOCK_TAG_NAMES, TagType.ANY);

        // Create block container
        documentBlocks = new ArrayList<>();

        // Create structures for tag conversions
        metaToHtmlMap = new HashMap<>();
        htmlToMetaMap = new HashMap<>();
        metaCounters = new HashMap<>();
    }

    /**
     * Build match pattern for a list of tags.
     * @param tagList : List of tags
     * @param type : Tag type (opening, closing, any)
     * @return Pattern object
     **/
    private Pattern buildTagPattern(final String[] tagList, final TagType type) {
        String re, prefix, suffix;
        String separator = "|";

        switch (type) {
            case OPENING:
                prefix = "<(";
                suffix = ")(?:[^>])*>";
                break;
            case CLOSING:
                prefix = "</(";
                suffix = ")>";
                break;
            case ANY:
                prefix = "</*(?:";
                suffix = ")(?:[^>])*> *";
                break;
            default: // Should not happen
                prefix = "";
                suffix = "";
        }

        re = Stream.of(tagList).collect(Collectors.joining(separator, prefix, suffix));
        return Pattern.compile(re);
    }

    private List<HTMLTag> findDocumentHTMLTags(final Pattern pattern, final TagType tagType) {
        List<HTMLTag> result = new ArrayList<>();
        Matcher matcher = pattern.matcher(doc);
        while (matcher.find()) {
            HTMLTag tag = new HTMLTag(
                    matcher.group(1),
                    matcher.start(),
                    matcher.end(),
                    tagType);
            result.add(tag);
        }
        return result;
    }

    /**
     * Generate meta body for tag and its pair if it exists.
     * Orphan tags receive unique meta bodies.
     * @param tag
     */
    private void generateMetaBody(final HTMLTag tag) {

        // Do not overwrite meta body values
        if (tag.hasMetaBody()) {
            return;
        }

        String tagBody = doc.substring(tag.getStart(), tag.getEnd());
        String metaBody = htmlToMetaMap.get(tagBody);

        if (metaBody == null || !tag.hasPair()) {
            Character metaNameChar = tag.getName().charAt(0);
            Integer metaCounter = metaCounters.get(metaNameChar);

            if (metaCounter == null) {
                metaCounter = 1;
            } else {
                metaCounter += 1;
            }

            metaCounters.put(metaNameChar, metaCounter);
            String tagFormat;
            if (tag.isOpening()) {
                tagFormat = "<%s%d>";
            } else {
                tagFormat = "</%s%d>";
            }
            metaBody = String.format(tagFormat, metaNameChar, metaCounter);
        }
        tag.setMetaBody(metaBody);

        // Extract href attribute for anchor tags and store it as comment
        if (tag.getName().equals("a")) {
            Matcher matcher = HREF_PATTERN.matcher(tagBody);
            if (matcher.find()) {
                tag.setComment(matcher.group(1));
            }
        }

        metaToHtmlMap.put(metaBody, tagBody);
        htmlToMetaMap.put(tagBody, metaBody);

        // Pair tag can be only closing because of walking direction.
        if (tag.hasPair()) {
            HTMLTag pair = tag.getPair();
            String pairBody = doc.substring(pair.getStart(), pair.getEnd());
            String pairMetaBody = "</" + metaBody.substring(1);
            pair.setMetaBody(pairMetaBody);
            metaToHtmlMap.put(pairMetaBody, pairBody);
        }
    }

    /**
     * Tokenize document to atomic and formatting blocks.
     * Create meta names for formatting tags.
     */
    private void tokenizeDocument() {

        // Add non-HTML atomic blocks to block list
        Matcher matcher = ATOMIC_PATTERN.matcher(doc);
        while (matcher.find()) {
            documentBlocks.add(new Block(BlockType.ATOMIC, matcher.start(), matcher.end()));
        }

        // Add HTML atomic blocks to block list
        matcher = blockTagPattern.matcher(doc);
        while (matcher.find()) {
            documentBlocks.add(new Block(BlockType.ATOMIC, matcher.start(), matcher.end()));
        }

        // Search for formatting HTML tags
        List<HTMLTag> allTags = findDocumentHTMLTags(oFTagPattern, TagType.OPENING);
        allTags.addAll(findDocumentHTMLTags(cFTagPattern, TagType.CLOSING));
        Collections.sort(allTags);

        // Search for tag pairs
        int tagDepth;
        // Iterate to penultimate tag
        for (int i = 0; i < allTags.size() - 1; ++i) {

            HTMLTag currentTag = allTags.get(i);
            // Skip if tag has a pair or it's a closing tag
            if (currentTag.hasPair() || !currentTag.isOpening()) {
                continue;
            }

            tagDepth = 1;
            // Search pair in following tags
            for (int j = i + 1; j < allTags.size(); ++j) {
                HTMLTag pairCandidate = allTags.get(j);
                // Skip tags with different name
                if (!pairCandidate.getName().equals(currentTag.getName())) {
                    continue;
                }

                // Calculate current depth
                if (pairCandidate.isOpening()) {
                    tagDepth++;
                    continue;
                } else {
                    tagDepth--;
                }

                if (tagDepth == 0) {
                    currentTag.setPair(pairCandidate);
                    break;
                }
            }
        }

        // Generate meta bodies for all tags
        allTags.forEach(tag -> generateMetaBody(tag));

        // Add tags to document blocks
        documentBlocks.addAll(allTags);

        // Add dummy block at the end of the document to simplify logic
        documentBlocks.add(new Block(BlockType.ATOMIC, doc.length(), doc.length()));

        // Sort blocks
        Collections.sort(documentBlocks);

        // Create cache for found blocks
        List<Block> blockCache = new ArrayList<>();

        // Now search for payload blocks before/between other blocks
        int lastBlockEnd = 0;

        for (Block block : documentBlocks) {
            if (block.start > lastBlockEnd) {
                blockCache.add(new Block(BlockType.PAYLOAD, lastBlockEnd, block.start));
            }
            lastBlockEnd = block.end;
        }

        // Add found payload blocks to document blocks
        documentBlocks.addAll(blockCache);
        Collections.sort(documentBlocks);
    }

    /**
     * Translate group of blocks.
     * @param blocks : List of blocks where at least one is payload
     * @return Translated string
     */
    private String translateBlocks(final List<Block> blocks) {

        // Store initial bounds
        int groupStart = blocks.get(0).getStart();
        int groupEnd = blocks.get(blocks.size() - 1).getEnd();

        // Strip margin tags
        while (blocks.size() > 2) {
            Block leftBlock = blocks.get(0);
            Block rightBlock = blocks.get(blocks.size() - 1);
            if (leftBlock.getType() != BlockType.TAG || rightBlock.getType() != BlockType.TAG) {
                break;
            }
            if (((HTMLTag) leftBlock).getPair() != rightBlock) {
                break;
            }
            // Do not strip <a> tags since they contain href used in comment
            if (((HTMLTag) leftBlock).getName().equals("a")) {
                break;
            }

            blocks.remove(0);
            blocks.remove(blocks.size() - 1);
        }

        int scopeStart = blocks.get(0).getStart();
        int scopeEnd = blocks.get(blocks.size() - 1).getEnd();

        // Build result string
        StringBuilder resultBuilder = new StringBuilder();

        // Write left stripped tags
        if (groupStart < scopeStart) {
            resultBuilder.append(doc.substring(groupStart, scopeStart));
        }

        Set<String> scopeMetaBodies = new HashSet<>();
        StringBuilder commentBuilder = new StringBuilder();
        Map<String, String> anchorTagHrefs = new LinkedHashMap<>();

        // Build string for translation
        StringBuilder translationBuilder = new StringBuilder();
        for (Block block : blocks) {
            switch (block.getType()) {
                case TAG:
                    HTMLTag tag = (HTMLTag) block;
                    String metaBody = tag.getMetaBody();
                    translationBuilder.append(metaBody);
                    scopeMetaBodies.add(metaBody);
                    if (tag.getComment() != null) {
                        // Append tag name and its href to comment
                        commentBuilder.append(tag.metaBody);
                        commentBuilder.append(": ");
                        commentBuilder.append(tag.getComment());
                        commentBuilder.append("\n");
                        // Store tag name / href pair for translation
                        anchorTagHrefs.put(tag.metaBody, tag.getComment());
                    }
                    break;
                case PAYLOAD:
                    translationBuilder.append(doc.substring(block.getStart(), block.getEnd()));
                    break;
                default:
                    // Should not happen
            }
        }

        String translation = translationBuilder.toString();

        // Substitute special HTML characters with real ones
        for (String[] subst : SPECIAL_HTML_CHARACTERS) {
            translation = translation.replaceAll(subst[0], subst[1]);
        }

        // Check if we have any comments
        String comment;
        if (commentBuilder.length() > 0) {
            comment = commentBuilder.toString();
        } else {
            comment = null;
        }

        // Fetch actual translation
        translation = processEntry(translation, comment);

        // Put back special HTML characters
        for (String[] subst : SPECIAL_HTML_CHARACTERS) {
            translation = translation.replaceAll(subst[1], subst[0]);
        }

        // Put HTML tags back
        for (String meta : scopeMetaBodies) {
            translation = translation.replaceAll(meta, metaToHtmlMap.get(meta));
        }

        // Translate anchor hrefs and replace them in translated string
        for (Map.Entry<String, String> hrefEntry : anchorTagHrefs.entrySet()) {
            String originalHref = hrefEntry.getValue();
            String metaTag = hrefEntry.getKey();
            String translatedHref = processEntry(originalHref,
                    String.format("%s %s",
                            Util.RESOURCE_BUNDLE.getString("HYPERLINK_FOR"), metaTag));
            translation = translation.replaceAll(wrapWithHref(originalHref),
                    wrapWithHref(translatedHref));
        }

        // Append translation to result
        resultBuilder.append(translation);

        // Write right stripped tags
        if (groupEnd > scopeEnd) {
            resultBuilder.append(doc.substring(scopeEnd, groupEnd));
        }

        return resultBuilder.toString();
    }

    /**
     * Wrap URL with href attribute to avoid translation corruption due to bad URLs.
     * @param url
     * @return
     */
    private static String wrapWithHref(final String url) {
        return String.format("href=\"%s\"", url);
    }

    /**
     * After the document was tokenized we can translate block groups with payload
     * or directly write blocks without payload to target file.
     * @throws IOException
     */
    private void translateDocument() throws IOException {
        boolean payloadInCache = false;
        List<Block> cache = new ArrayList<>();

        for (Block block : documentBlocks) {
            switch (block.getType()) {
                case ATOMIC: // The last one is dummy atomic
                    if (cache.size() > 0) {
                        if (payloadInCache) {
                            fileWriter.write(translateBlocks(cache));
                        } else {
                            int cacheStart = cache.get(0).getStart();
                            int cacheEnd = cache.get(cache.size() - 1).getEnd();
                            fileWriter.write(doc.substring(cacheStart, cacheEnd));
                        }
                        payloadInCache = false;
                        cache.clear();
                    }
                    fileWriter.write(doc.substring(block.start, block.end));
                    break;
                case PAYLOAD:
                    payloadInCache = true; // No break here
                case TAG:
                    cache.add(block);
                default:
            }
        }
    }

    private static IApplicationEventListener generateIApplicationEventListener() {
        return new IApplicationEventListener() {

            private static final int MENU_PRIORITY = 100;

            @Override
            public void onApplicationStartup() {
                Core.getEditor().registerPopupMenuConstructors(MENU_PRIORITY,
                        new PopupMenuConstructor());
            }

            @Override
            public void onApplicationShutdown() {
            }
        };
    }

    /**
     * Plugin loader.
     */
    public static void loadPlugins() {
        Core.registerFilterClass(TipeFilter.class);
        CoreEvents.registerApplicationEventListener(generateIApplicationEventListener());
    }

    /**
     * Plugin unloader.
     */
    public static void unloadPlugins() { }

    @Override
    public String getFileFormatName() {
        return Util.FILTER_NAME;
    }

    @Override
    public boolean isSourceEncodingVariable() {
        return true;
    }

    @Override
    public boolean isTargetEncodingVariable() {
        return true;
    }

    @Override
    public Instance[] getDefaultInstances() {
        return new Instance[] {
                new Instance(Util.SOURCE_FILENAME_MASK, "*", "*"),
        };
    }

    @Override
    protected boolean requirePrevNextFields() {
        return false;
    }

    @Override
    protected boolean isFileSupported(final BufferedReader reader) {
        return true;
    }

    /**
     * All stuff starts here.
     */
    @Override
    public void processFile(final BufferedReader reader, final BufferedWriter outfile,
            final FilterContext fc) throws IOException {

        // Read file
        StringBuilder builder = new StringBuilder();
        LinebreakPreservingReader lbpr = new LinebreakPreservingReader(reader);
        String line, br;

        while ((line = lbpr.readLine()) != null) {
            br = lbpr.getLinebreak();
            builder.append(line);
            builder.append(br);
        }

        fileWriter = outfile;
        doc = builder.toString();

        // Clean up document level structures
        documentBlocks.clear();
        metaToHtmlMap.clear();
        htmlToMetaMap.clear();
        metaCounters.clear();

        // Find blocks, create meta tags
        tokenizeDocument();
        // Translate actual text
        translateDocument();
    }
}
