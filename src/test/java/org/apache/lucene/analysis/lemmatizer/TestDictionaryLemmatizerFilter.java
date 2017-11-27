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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.util.CharArraySet;

public class TestDictionaryLemmatizerFilter extends BaseTokenStreamTestCase {

  private Analyzer analyzer = new Analyzer() {
    @Override
    protected TokenStreamComponents createComponents(final String fieldName) {
      Tokenizer source = new MockTokenizer(MockTokenizer.WHITESPACE, false);
      return new TokenStreamComponents(source, new DictionaryLemmatizerFilter(source,
          getMockedWordlist(), null));
    }
  };

  public void testKeyword() throws IOException {
    final CharArraySet exclusionSet = new CharArraySet(asSet("katze"), false);
    Analyzer a = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(final String fieldName) {
        Tokenizer source = new MockTokenizer(MockTokenizer.WHITESPACE, false);
        TokenStream sink = new SetKeywordMarkerFilter(source, exclusionSet);
        return new TokenStreamComponents(source, new DictionaryLemmatizerFilter(sink,
            getMockedWordlist(), null));
      }
    };
    checkOneTerm(a, "katze", "katze");
  }

  /** blast some random strings through the analyzer */
  public void testRandomStrings() throws Exception {
    checkRandomData(random(), analyzer, 1000 * RANDOM_MULTIPLIER);
  }

  public void testEmptyTerm() throws IOException {
    Analyzer a = new Analyzer() {
      @Override
      protected TokenStreamComponents createComponents(final String fieldName) {
        Tokenizer tokenizer = new KeywordTokenizer();
        return new TokenStreamComponents(tokenizer, new DictionaryLemmatizerFilter(tokenizer,
            getMockedWordlist(), null));
      }
    };
    checkOneTerm(a, "", "");
  }

  private Map<String, String[]> getMockedWordlist() {
    final Map<String, String[]> wordList = new HashMap<String, String[]>();
    wordList.put("bücher", new String[] { "buch" });
    wordList.put("fragen", new String[] { "frage, fragen" });
    wordList.put("katzen", new String[] { "katze" });
    return wordList;
  }

}
