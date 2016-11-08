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
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;

/**
 * A {@link TokenFilter} that applies {@link DictionaryLemmatizer} to lemmatize
 * words.
 * <p>
 * To prevent terms from being lemmatized, use an instance of
 * {@link SetKeywordMarkerFilter} or a custom {@link TokenFilter} that sets the
 * {@link KeywordAttribute} before this {@link TokenStream}.
 * </p>
 */
public final class DictionaryLemmatizerFilter extends TokenFilter {
  private final DictionaryLemmatizer lemmatizer;
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);
  private final PositionIncrementAttribute positionAttr = addAttribute(PositionIncrementAttribute.class);
  private final Queue<String> terms = new LinkedList<String>();

  private AttributeSource.State current = null;

  /**
   * Creates a DictionaryLemmatizerFilter outputting possible lemmas.
   * 
   * @param input TokenStream whose tokens will be lemmatized
   * @param wordlist a Hashmap containing all the words with their lemmas
   */
  public DictionaryLemmatizerFilter(final TokenStream input, final Map<String, String[]> wordlist) {
    super(input);
    lemmatizer = new DictionaryLemmatizer(wordlist);
  }

  @Override
  public boolean incrementToken() throws IOException {
    if (!terms.isEmpty()) {
      if (createToken(terms.poll(), current)) {
        return true;
      }
    }
    if (input.incrementToken()) {
      if (!keywordAttr.isKeyword()) {
        char[] buffer = termAtt.buffer();
        final String tokenTerm = new String(buffer, 0, termAtt.length());

        final String[] values = lemmatizer.lemmatize(tokenTerm);
        if (values != null) {
          // Replace first token with the lemma:
          termAtt.setEmpty().append(values[0]);
          if (values.length > 1) {
            // Queue remaining lemmas for later processing
            for (int i = 1; i < values.length; i++) {
              terms.add(values[i]);
            }
          }
        }
        current = captureState();
      }
      return true;
    } else {
      return false;
    }
  }

  protected boolean createToken(final String synonym, final AttributeSource.State current) {
    restoreState(current);
    termAtt.setEmpty().append(synonym);
    positionAttr.setPositionIncrement(0);
    return true;
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    current = null;
  }

}