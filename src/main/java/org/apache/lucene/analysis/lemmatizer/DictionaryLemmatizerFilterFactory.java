package org.apache.lucene.analysis.lemmatizer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.lucene.analysis.TokenStream;
import static org.apache.lucene.analysis.no.NorwegianLightStemmer.*;
import org.apache.lucene.analysis.de.GermanMinimalStemmer;
import org.apache.lucene.analysis.en.EnglishMinimalStemmer;
import org.apache.lucene.analysis.no.NorwegianMinimalStemmer;
import org.apache.lucene.analysis.sv.SwedishLightStemmer;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link DictionaryLemmatizerFilter}. Example configuration:
 * &lt;filter class=&quot;solr.DictionaryLemmatizerFilterFactory&quot;
 * dictionaries=&quot;dictionary1.dic,dictionary2.dic&quot;
 * charset=&quot;iso-8859-1&quot;
 * storePosTag=&quot;false&quot;
 * reduceTo=&quot;noun,verb&quot;
 * wordClasses=&quot;noun,verb,adjective&quot;
 * lemmaPos=&quot;1&quot;
 * wordPos=&quot;2&quot;
 * wordClassPos=&quot;3&quot;
 * minLength=&quot;5&quot;
 * stemFallbackLang=&quot;de&quot;
 * Parameters dictionary, lemmaPos and wordPos are mandatory. /&gt;</pre>
 */
public class DictionaryLemmatizerFilterFactory extends TokenFilterFactory implements
        ResourceLoaderAware {

    private final Map<String, Set<String>> wordlist = new HashMap<String, Set<String>>();
    private final Map<String, String[]> finallist = new HashMap<String, String[]>();

    private Reader reader = null;
    private BufferedReader br = null;

    private static final String PARAM_WORD_CLASSES = "wordClasses";
    private static final String PARAM_CHARSET = "charset";
    private static final String PARAM_MIN_LENGTH = "minLength";
    private static final String PARAM_LEMMA_POS = "lemmaPos";
    private static final String PARAM_WORD_POS = "wordPos";
    private static final String PARAM_WORD_CLASS_POS = "wordClassPos";
    private static final String PARAM_REDUCE_TO = "reduceTo";
    private static final String PARAM_STORE_POS_TAG = "storePosTag";
    private static final String PARAM_DICTIONARIES = "dictionaries";
    private static final String PARAM_STEM_FALLBACK_LANG = "stemFallbackLang";

    private int minLength;
    private String dictionaries;
    private int lemmaPos;
    private int wordPos;
    private int wordClassPos;
    private String[] wordClasses;
    private String charset;
    private String[] reduceTo;
    private boolean storePosTag;
    private String stemFallbackLang;

    private HashMap<String, Object> fallbackStemmers = new HashMap<>();

    private transient static Logger log = LoggerFactory
            .getLogger(DictionaryLemmatizerFilterFactory.class);

    {
        fallbackStemmers.put("no", new NorwegianMinimalStemmer(BOKMAAL | NYNORSK));
        fallbackStemmers.put("nb", new NorwegianMinimalStemmer(BOKMAAL));
        fallbackStemmers.put("nn", new NorwegianMinimalStemmer(NYNORSK));
        fallbackStemmers.put("se", new SwedishLightStemmer());
        fallbackStemmers.put("de", new GermanMinimalStemmer());
        fallbackStemmers.put("en", new EnglishMinimalStemmer());
    }


    /** Creates a new DictionaryLemmatizerFilterFactory */
    public DictionaryLemmatizerFilterFactory(final Map<String, String> args) {
        super(args);

        String wordClassList = get(args, PARAM_WORD_CLASSES);
        wordClasses = (wordClassList != null) ? wordClassList.split(",") : null;
        charset = get(args, PARAM_CHARSET, "UTF-8");
        minLength = getInt(args, PARAM_MIN_LENGTH, 3);
        dictionaries = require(args, PARAM_DICTIONARIES);
        lemmaPos = getInt(args, PARAM_LEMMA_POS, -1);
        wordPos = getInt(args, PARAM_WORD_POS, -1);
        wordClassPos = getInt(args, PARAM_WORD_CLASS_POS, -1);
        final String reduceToList = get(args, PARAM_REDUCE_TO);
        reduceTo = (reduceToList != null) ? reduceToList.split(",") : null;
        storePosTag = getBoolean(args, PARAM_STORE_POS_TAG, false);
        stemFallbackLang = get(args, PARAM_STEM_FALLBACK_LANG);

        if (lemmaPos < 0) {
            throw new IllegalArgumentException("Parameter " + PARAM_LEMMA_POS + " not properly set");
        }
        if (wordPos < 0) {
            throw new IllegalArgumentException("Parameter " + PARAM_WORD_POS + " not properly set");
        }

        if (stemFallbackLang != null && fallbackStemmers.get(stemFallbackLang) == null) {
            throw new IllegalArgumentException("Unsupported language in parameter " + PARAM_STEM_FALLBACK_LANG + ": "
                    + stemFallbackLang);
        }

        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }

    }


    private void handleStream(final List<InputStream> inputStreams) throws IOException {
        for (InputStream inputStream : inputStreams) {
            if (inputStream instanceof ZipInputStream) {
                ZipEntry entry;
                while ((entry = ((ZipInputStream) inputStream).getNextEntry()) != null) {
                    final String entryName = entry.getName();
                    final File file = new File(entryName);
                    final String directory = file.getParent();
                    if (directory == null) {
                        if (file.isDirectory()) {
                            break;
                        }
                    }
                    addDictionary(inputStream);
                }
            } else {
                addDictionary(inputStream);
            }
        }
        log.info(wordlist.size() + " entries has been added.");
        addEntries();
        wordlist.clear();
    }


    /*
     * Adds entries in a dictionary to a map where the key is the word and the
     * value is a comma-separated list of lemmas for that word. A word can have
     * several lemmas: German: (wir/viele) fragen => (zu) fragen (verb), (eine)
     * Frage (noun) Norwegian: (vi/mange) sykler => (å) sykle (verb), (en) sykkel
     * (noun)
     */
    private void addDictionary(final InputStream inputStream) throws IOException {
        reader = new InputStreamReader(inputStream, charset);
        br = new BufferedReader(reader);
        String line = null;
        while ((line = br.readLine()) != null) {

            final String[] parts = line.split("\t");

            // Skip comments etc. in file:
            if (parts.length < 2 || line.trim().startsWith("#") || line.trim().startsWith("*")) {
                continue;
            }

            final String word = parts[wordPos];

            // Skip words which do not meet the threshold:
            if (word.length() <= minLength) {
                continue;
            }

            // Skip splitting words and those with a hyphen - they
            // interfere with tokenizers.
            if (word.contains(" ") || word.contains("-")) {
                continue;
            }

            // Only include words which belong to the defined word classes
            String lemma = null;
            if (wordClasses != null) {
                for (int i = 0; i < wordClasses.length; i++) {
                    final String wordClass = wordClasses[i];
                    if (parts[wordClassPos].contains(wordClass)) {
                        lemma = parts[lemmaPos] + "$" + i; // add
                        // POS-tag for the given word class
                        break;
                    }
                }
            } else {
                lemma = parts[lemmaPos];
            }
            if (lemma == null) {
                continue;
            }

            Set<String> entry = wordlist.get(word);
            if (entry == null) {
                entry = new HashSet<String>();
            }
            entry.add(lemma);
            wordlist.put(word, entry);
        }
    }


    private void addEntries() {
        // Clean and compress list further
        for (Iterator<Map.Entry<String, Set<String>>> entries = wordlist.entrySet().iterator(); entries
                .hasNext();) {
            final Map.Entry<String, Set<String>> entry = entries.next();
            final Set<String> lemmas = entry.getValue();
            final String word = entry.getKey();

            // If reduce is defined, make sure that at least one lemma from a defined
            // word class is added:
            if (reduceTo != null) {
                if (lemmas.size() > 1) {
                    // If several lemmas for the same word class are found, use the
                    // shortest:
                    String lemmaToUse = null;
                    for (String wordClass : reduceTo) {
                        final int posTag = Arrays.asList(wordClasses).indexOf(wordClass);
                        for (String lemma : lemmas) {
                            if (lemma.contains("$" + posTag) && !lemma.equals(word + "$" + posTag)) {
                                lemmaToUse = (lemmaToUse != null) ? (lemmaToUse.length() > lemma.length()) ? lemma
                                        : lemmaToUse : lemma;
                            }
                        }
                        if (lemmaToUse != null) {
                            break;
                        }
                    }
                    if (lemmaToUse == null) { // Did not find any matched word classes,
                                              // just use the shortest lemma:
                        for (String lemma : lemmas) {
                            lemmaToUse = (lemmaToUse != null) ? (lemmaToUse.length() > lemma.length()) ? lemma
                                    : lemmaToUse : lemma;
                        }
                    }
                    if (lemmaToUse != null) {
                        final String newLemma = (storePosTag) ? lemmaToUse : lemmaToUse.replaceAll("\\$\\d+",
                                "");
                        final String[] newLemmas = { newLemma };
                        finallist.put(word, newLemmas);
                        continue;
                    } else {
                        continue;
                    }
                } else {
                    storeLemmas(lemmas, word);
                }
            } else {
                storeLemmas(lemmas, word);
            }
        }
        log.info(finallist.size() + " entries after compression");
    }


    private void storeLemmas(final Set<String> lemmas, final String word) {
        if (storePosTag) {
            final int size = (reduceTo != null) ? 1 : lemmas.size();
            final String[] newLemmas = lemmas.toArray(new String[size]);
            finallist.put(word, newLemmas);
        } else {
            // Exclude entries where the lemma equals the word as long as we
            // only have one lemma:
            if (lemmas.size() == 1) {
                final String newLemma = lemmas.iterator().next().replaceAll("\\$\\d+", "");
                if (!newLemma.equals(word)) {
                    final String[] newLemmas = { newLemma };
                    finallist.put(word, newLemmas);
                }
            } else {
                final Set<String> lemmaList = new HashSet<String>();
                for (String lemma : lemmas) {
                    final String newLemma = lemma.replaceAll("\\$\\d+", "");
                    if (!newLemma.equals(word)) {
                        lemmaList.add(newLemma);
                    }
                }
                if (lemmaList.size() > 0) {
                    final int size = (reduceTo != null) ? 1 : lemmaList.size();
                    final String[] newLemmas = lemmaList.toArray(new String[size]);
                    finallist.put(word, newLemmas);
                }
            }
        }
    }


    @Override
    public TokenStream create(final TokenStream input) {
        return new DictionaryLemmatizerFilter(input, finallist, fallbackStemmers.get(stemFallbackLang));
    }


    @Override
    public void inform(final ResourceLoader resourceLoader) throws IOException {
        final String[] files = dictionaries.split(",");
        final List<InputStream> dictionaries = new ArrayList<InputStream>();
        try {
            for (String file : files) {
                InputStream inputStream = resourceLoader.openResource(file);
                if (file.endsWith(".gz")) {
                    inputStream = new GZIPInputStream(inputStream);
                } else if (file.endsWith(".zip")) {
                    inputStream = new ZipInputStream(inputStream);
                }
                dictionaries.add(inputStream);
            }
            handleStream(dictionaries);
        } catch (Exception e) {
            throw new IOException("Unable to load dictionary", e);
        } finally {
            IOUtils.closeWhileHandlingException(dictionaries);
            IOUtils.closeWhileHandlingException(reader);
            IOUtils.closeWhileHandlingException(br);
        }
    }

}
