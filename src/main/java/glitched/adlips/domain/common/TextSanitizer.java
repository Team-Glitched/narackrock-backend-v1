package glitched.adlips.domain.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Strips invisible fillers, bidi controls, and control characters while normalizing Unicode
 * spaces and line breaks without corrupting Korean Hangul or multi-code-point emojis.
 */
public final class TextSanitizer {
    private TextSanitizer() {
    }

    public static String sanitizeSingleLine(String input) {
        return sanitize(input, false);
    }

    public static String sanitizeMultiline(String input) {
        return sanitize(input, true);
    }

    public static boolean hasVisibleContent(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        for (int i = 0; i < input.length(); ) {
            int cp = input.codePointAt(i);
            i += Character.charCount(cp);
            if (!isBlankOrNonPrinting(cp) && !isCombiningMark(cp)) {
                return true;
            }
        }
        return false;
    }

    private static String sanitize(String input, boolean multiline) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // First pass: strip forbidden invisible/control chars, normalize Unicode spaces & line breaks.
        List<Integer> stage1 = new ArrayList<>(input.length());
        for (int i = 0; i < input.length(); ) {
            int cp = input.codePointAt(i);
            int charCount = Character.charCount(cp);

            if (cp == '\r') {
                if (i + 1 < input.length() && input.charAt(i + 1) == '\n') {
                    i += 2;
                } else {
                    i += 1;
                }
                stage1.add(multiline ? (int) '\n' : (int) ' ');
                continue;
            }
            i += charCount;

            if (cp == '\n' || cp == 0x2028 || cp == 0x2029) {
                stage1.add(multiline ? (int) '\n' : (int) ' ');
                continue;
            }

            if (isForbiddenInvisibleOrControl(cp)) {
                continue;
            }

            if (isNormalizedSpace(cp)) {
                stage1.add((int) ' ');
                continue;
            }

            stage1.add(cp);
        }

        // Second pass: strip orphaned/repeated ZWJ, orphaned variation selectors, tag chars outside flags,
        // and orphaned combining marks at blank boundaries.
        List<Integer> stage2 = new ArrayList<>(stage1.size());
        boolean inFlagTag = false;
        for (int idx = 0; idx < stage1.size(); idx++) {
            int cp = stage1.get(idx);

            if (cp == 0x1F3F4) {
                inFlagTag = true;
                stage2.add(cp);
                continue;
            }
            if (isTagChar(cp)) {
                if (inFlagTag) {
                    stage2.add(cp);
                    if (cp == 0xE007F) {
                        inFlagTag = false;
                    }
                }
                continue;
            }
            inFlagTag = false;

            if (isVariationSelector(cp)) {
                int prevBase = previousNonCombining(stage2);
                if (prevBase >= 0 && !isWhitespaceAscii(prevBase) && prevBase != 0x200D) {
                    stage2.add(cp);
                }
                continue;
            }

            if (isCombiningMark(cp)) {
                int prevBase = previousNonCombining(stage2);
                if (prevBase >= 0 && !isWhitespaceAscii(prevBase) && prevBase != 0x200D) {
                    stage2.add(cp);
                }
                continue;
            }

            if (cp == 0x200D) {
                int prevAnchor = previousEmojiAnchor(stage2);
                int nextAnchor = nextEmojiAnchor(stage1, idx + 1);
                if (prevAnchor >= 0 && nextAnchor >= 0 && (stage2.isEmpty() || stage2.getLast() != 0x200D)) {
                    stage2.add(cp);
                }
                continue;
            }

            stage2.add(cp);
        }

        // Trim leading and trailing ASCII whitespace / newlines.
        int start = 0;
        while (start < stage2.size() && isWhitespaceAscii(stage2.get(start))) {
            start++;
        }
        int end = stage2.size();
        while (end > start && isWhitespaceAscii(stage2.get(end - 1))) {
            end--;
        }
        if (start >= end) {
            return "";
        }

        StringBuilder result = new StringBuilder(end - start);
        for (int i = start; i < end; i++) {
            result.appendCodePoint(stage2.get(i));
        }
        String sanitized = result.toString();
        return hasVisibleContent(sanitized) ? sanitized : "";
    }

    private static boolean isForbiddenInvisibleOrControl(int cp) {
        // Hangul invisible fillers (U+115F, U+1160, U+3164, U+FFA0) & Braille blank (U+2800)
        if (cp == 0x115F || cp == 0x1160 || cp == 0x3164 || cp == 0xFFA0 || cp == 0x2800) {
            return true;
        }
        // Zero-width & invisible formatting characters (preserve U+200D & VS for emoji sequences)
        if (cp == 0x00AD || cp == 0x034F || cp == 0x061C
                || cp == 0x17B4 || cp == 0x17B5 || cp == 0x180E
                || cp == 0x200B || cp == 0x200C || cp == 0x200E || cp == 0x200F
                || (cp >= 0x2060 && cp <= 0x2064)
                || (cp >= 0x206A && cp <= 0x206F)
                || cp == 0xFEFF
                || (cp >= 0xFFF9 && cp <= 0xFFFC)
                || cp == 0x1D159
                || (cp >= 0x1D173 && cp <= 0x1D17A)
                || (cp >= 0xE0000 && cp <= 0xE001F)) {
            return true;
        }
        // Bidi overrides, embeddings, and isolates
        if ((cp >= 0x202A && cp <= 0x202E) || (cp >= 0x2066 && cp <= 0x2069)) {
            return true;
        }
        // C0 controls (except \t, \n, \r), DEL, and C1 controls
        return (cp >= 0x0000 && cp <= 0x0008)
                || cp == 0x000B || cp == 0x000C
                || (cp >= 0x000E && cp <= 0x001F)
                || (cp >= 0x007F && cp <= 0x009F);
    }

    private static boolean isNormalizedSpace(int cp) {
        return cp == ' ' || cp == '\t'
                || cp == 0x00A0 || cp == 0x1680
                || (cp >= 0x2000 && cp <= 0x200A)
                || cp == 0x202F || cp == 0x205F || cp == 0x3000
                || Character.isWhitespace(cp);
    }

    private static boolean isWhitespaceAscii(int cp) {
        return cp == ' ' || cp == '\n';
    }

    private static boolean isVariationSelector(int cp) {
        return (cp >= 0xFE00 && cp <= 0xFE0F) || (cp >= 0xE0100 && cp <= 0xE01EF);
    }

    private static boolean isTagChar(int cp) {
        return cp >= 0xE0020 && cp <= 0xE007F;
    }

    private static boolean isCombiningMark(int cp) {
        int type = Character.getType(cp);
        return type == Character.NON_SPACING_MARK
                || type == Character.ENCLOSING_MARK
                || type == Character.COMBINING_SPACING_MARK;
    }

    private static boolean isBlankOrNonPrinting(int cp) {
        return isWhitespaceAscii(cp)
                || cp == 0x200D
                || isVariationSelector(cp)
                || isTagChar(cp)
                || isForbiddenInvisibleOrControl(cp)
                || isNormalizedSpace(cp);
    }

    private static boolean isEmojiAnchor(int cp) {
        if (cp < 0x80) {
            return false;
        }
        return Character.isEmoji(cp) || Character.getType(cp) == Character.OTHER_SYMBOL;
    }

    private static int previousNonCombining(List<Integer> emitted) {
        for (int i = emitted.size() - 1; i >= 0; i--) {
            int cp = emitted.get(i);
            if (!isVariationSelector(cp) && !isCombiningMark(cp)) {
                return cp;
            }
        }
        return -1;
    }

    private static int previousEmojiAnchor(List<Integer> emitted) {
        for (int i = emitted.size() - 1; i >= 0; i--) {
            int cp = emitted.get(i);
            if (isVariationSelector(cp) || isTagChar(cp) || cp == 0x20E3) {
                continue;
            }
            return isEmojiAnchor(cp) ? cp : -1;
        }
        return -1;
    }

    private static int nextEmojiAnchor(List<Integer> cps, int fromIdx) {
        for (int i = fromIdx; i < cps.size(); i++) {
            int cp = cps.get(i);
            if (cp == 0x200D || isVariationSelector(cp)) {
                continue;
            }
            return isEmojiAnchor(cp) ? cp : -1;
        }
        return -1;
    }
}
