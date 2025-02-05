/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.common;

import java.lang.Character.UnicodeScript;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Detects the language of a given text string by 
 * looking for typical words and word endings and used characters for each language.
 * German, english, french, arabic, chinese, japanese, greek and hebrew are currently supported.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRLanguageDetector {
    private static Logger LOGGER = LogManager.getLogger(MCRLanguageDetector.class);

    private static Properties words = new Properties();

    private static Properties endings = new Properties();

    private static Map<UnicodeScript, String> code2languageCodes = new HashMap<>();

    static {
        code2languageCodes.put(UnicodeScript.ARABIC, "ar");
        code2languageCodes.put(UnicodeScript.GREEK, "el");
        code2languageCodes.put(UnicodeScript.HAN, "zh");
        code2languageCodes.put(UnicodeScript.HEBREW, "he");
        code2languageCodes.put(UnicodeScript.HIRAGANA, "ja");
        code2languageCodes.put(UnicodeScript.KATAKANA, "ja");

        words.put("de", "als am auch auf aus bei bis das dem den der deren derer des dessen"
            + " die dies diese dieser dieses ein eine einer eines einem für"
            + " hat im ist mit sich sie über und vom von vor wie zu zum zur");
        words.put("en",
            "a and are as at do for from has have how its like new of on or the their through to with you your");
        words.put("fr", "la le les un une des, à aux de pour par sur comme aussi jusqu'à"
            + " jusqu'aux quel quels quelles laquelle lequel lesquelles"
            + " lesquelles auxquels auxquelles avec sans ont sont duquel desquels desquelles quand");

        endings.put("en", "ar ble cal ce ced ed ent ic ies ing ive ness our ous ons ral th ure y");
        endings.put("de", "ag chen gen ger iche icht ig ige isch ische ischen kar ker"
            + " keit ler mus nen ner rie rer ter ten trie tz ung yse");
        endings.put("fr", "é, és, ée, ées, euse, euses, ème, euil, asme, isme, aux");
    }

    private static int buildScore(String text, String lang, String wordList, String endings) {
        text = text.toLowerCase(Locale.ROOT).trim();
        text = text.replace(',', ' ').replace('-', ' ').replace('/', ' ');
        text = " " + text + " ";

        int score = 0;

        StringTokenizer st = new StringTokenizer(wordList, " ");
        while (st.hasMoreTokens()) {
            String word = st.nextToken();
            int pos = 0;
            while ((pos = text.indexOf(" " + word + " ", pos)) >= 0) {
                score += 2;
                pos = Math.min(pos + word.length() + 1, text.length());
            }
        }

        st = new StringTokenizer(endings, " ");
        while (st.hasMoreTokens()) {
            String ending = st.nextToken();

            if (text.contains(ending + " ")) {
                score += 1;
            }
            int pos = 0;
            while ((pos = text.indexOf(ending + " ", pos)) >= 0) {
                score += 1;
                pos = Math.min(pos + ending.length() + 1, text.length());
            }
        }

        LOGGER.debug("Score {} = {}", lang, score);
        return score;
    }

    public static String detectLanguageByCharacter(String text) {
        if (text == null || text.isEmpty()) {
            LOGGER.warn("The text for language detection is null or empty");
            return null;
        }
        LOGGER.debug("Detecting language of [{}]", text);

        Map<UnicodeScript, AtomicInteger> scores = new HashMap<>();
        buildScores(text, scores);
        UnicodeScript code = getCodeWithMaxScore(scores);

        return code2languageCodes.getOrDefault(code, null);
    }

    private static void buildScores(String text, Map<UnicodeScript, AtomicInteger> scores) {
        try {
            char[] chararray = text.toCharArray();
            for (int i = 0; i < text.length(); i++) {
                UnicodeScript code = UnicodeScript.of(Character.codePointAt(chararray, i));
                increaseScoreFor(scores, code);
            }
        } catch (Exception ignored) {
        }
    }

    private static void increaseScoreFor(Map<UnicodeScript, AtomicInteger> scores, UnicodeScript code) {
        scores.computeIfAbsent(code, k -> new AtomicInteger()).incrementAndGet();
    }

    private static UnicodeScript getCodeWithMaxScore(Map<UnicodeScript, AtomicInteger> scores) {
        UnicodeScript maxCode = null;
        int maxScore = 0;
        for (UnicodeScript code : scores.keySet()) {
            int score = scores.get(code).get();
            if (score > maxScore) {
                maxScore = score;
                maxCode = code;
            }
        }
        return maxCode;
    }

    /**
     * Detects the language of a given text string.
     * 
     * @param text the text string
     * @return the language code: de, en, fr, ar ,el, zh, he, jp or null
     */
    public static String detectLanguage(String text) {
        LOGGER.debug("Detecting language of [{}]", text);

        String bestLanguage = detectLanguageByCharacter(text);

        if (bestLanguage == null) {
            int bestScore = 0;
            Enumeration<Object> languages = words.keys();
            while (languages.hasMoreElements()) {
                String language = (String) languages.nextElement();
                String wordList = words.getProperty(language);
                String endingList = endings.getProperty(language);

                int score = buildScore(text, language, wordList, endingList);
                if (score > bestScore) {
                    bestLanguage = language;
                    bestScore = score;
                }
            }
        }

        LOGGER.debug("Detected language = {}", bestLanguage);
        return bestLanguage;
    }
}
