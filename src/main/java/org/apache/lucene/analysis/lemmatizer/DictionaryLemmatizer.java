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

import java.util.Map;

/**
 * Lemmatizer which looks up lemmas from a dictionary
 */
public class DictionaryLemmatizer {

  private final Map<String, String[]> wordlist;

  /**
   * Creates a new DictionaryLemmatizer
   * 
   * @param wordlist a Hashmap containing all the words with their lemmas
   */
  public DictionaryLemmatizer(final Map<String, String[]> wordlist) {
    this.wordlist = wordlist;
  }

  /**
   * Find the lemma(s) of the provided word.
   * 
   * @param word Word to find the lemma(s)
   * @return a list of lemmas for the word
   */
  public String[] lemmatize(final String word) {
    return wordlist.get(word);
  }

}
