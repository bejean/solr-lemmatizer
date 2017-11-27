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
import org.apache.lucene.analysis.ar.ArabicStemmer;
import org.apache.lucene.analysis.bg.BulgarianStemmer;
import org.apache.lucene.analysis.ckb.SoraniStemmer;
import org.apache.lucene.analysis.cz.CzechStemmer;
import org.apache.lucene.analysis.de.GermanLightStemmer;
import org.apache.lucene.analysis.de.GermanMinimalStemmer;
import org.apache.lucene.analysis.el.GreekStemmer;
import org.apache.lucene.analysis.en.EnglishMinimalStemmer;
import org.apache.lucene.analysis.es.SpanishLightStemmer;
import org.apache.lucene.analysis.fi.FinnishLightStemmer;
import org.apache.lucene.analysis.fr.FrenchLightStemmer;
import org.apache.lucene.analysis.fr.FrenchMinimalStemmer;
import org.apache.lucene.analysis.gl.GalicianMinimalStemmer;
import org.apache.lucene.analysis.gl.GalicianStemmer;
import org.apache.lucene.analysis.hi.HindiStemmer;
import org.apache.lucene.analysis.hu.HungarianLightStemmer;
import org.apache.lucene.analysis.id.IndonesianStemmer;
import org.apache.lucene.analysis.it.ItalianLightStemmer;
import org.apache.lucene.analysis.lv.LatvianStemmer;
import org.apache.lucene.analysis.no.NorwegianLightStemmer;
import org.apache.lucene.analysis.no.NorwegianMinimalStemmer;
import org.apache.lucene.analysis.pt.PortugueseLightStemmer;
import org.apache.lucene.analysis.pt.PortugueseMinimalStemmer;
import org.apache.lucene.analysis.pt.PortugueseStemmer;
import org.apache.lucene.analysis.ru.RussianLightStemmer;
import org.apache.lucene.analysis.sv.SwedishLightStemmer;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;
import org.tartarus.snowball.SnowballProgram;

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
  private final Object fallbackStemmer;
  private String stemmerName;
  private AttributeSource.State current = null;

  /**
   * Creates a DictionaryLemmatizerFilter outputting possible lemmas.
   * 
   * @param input TokenStream whose tokens will be lemmatized
   * @param wordlist a Hashmap containing all the words with their lemmas
   */
  public DictionaryLemmatizerFilter(final TokenStream input, final Map<String, String[]> wordlist, Object fallbackStemmer, String stemmerName) {
    super(input);
    lemmatizer = new DictionaryLemmatizer(wordlist);
    this.fallbackStemmer = fallbackStemmer;
    this.stemmerName = stemmerName;
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
        } else if (fallbackStemmer != null) {
          if (fallbackStemmer instanceof SnowballProgram) {
            char termBuffer[] = termAtt.buffer();
            final int length = termAtt.length();

            SnowballProgram snowball = (SnowballProgram) fallbackStemmer;
            snowball.setCurrent(buffer, termAtt.length());
            snowball.stem();

            final char finalTerm[] = snowball.getCurrentBuffer();
            final int newLength = snowball.getCurrentBufferLength();
            if (finalTerm != termBuffer)
              termAtt.copyBuffer(finalTerm, 0, newLength);
            else
              termAtt.setLength(newLength);

          } else {
            int len = 0;
            //String newTerm = null;
            if (fallbackStemmer instanceof ArabicStemmer) {
              len = ((ArabicStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof BulgarianStemmer) {
              len = ((BulgarianStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof CzechStemmer) {
              len = ((CzechStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof EnglishMinimalStemmer) {
              len = ((EnglishMinimalStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof FinnishLightStemmer) {
              len = ((FinnishLightStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof FrenchLightStemmer) {
              len = ((FrenchLightStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof FrenchMinimalStemmer) {
              len = ((FrenchMinimalStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof GalicianMinimalStemmer) {
              len = ((GalicianMinimalStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof GalicianStemmer) {
              len = ((GalicianStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof GermanLightStemmer) {
              len = ((GermanLightStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof GermanMinimalStemmer) {
              len = ((GermanMinimalStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof GreekStemmer) {
              len = ((GreekStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof HindiStemmer) {
              len = ((HindiStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof HungarianLightStemmer) {
              len = ((HungarianLightStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof IndonesianStemmer) {
              if ("IndonesianLightStemmer".equals(stemmerName))
                len = ((IndonesianStemmer) fallbackStemmer).stem(buffer, termAtt.length(), true);
              else
                len = ((IndonesianStemmer) fallbackStemmer).stem(buffer, termAtt.length(), false);
            } else if (fallbackStemmer instanceof ItalianLightStemmer) {
              len = ((ItalianLightStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof LatvianStemmer) {
              len = ((LatvianStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof NorwegianLightStemmer) {
              len = ((NorwegianLightStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof NorwegianMinimalStemmer) {
              len = ((NorwegianMinimalStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof PortugueseLightStemmer) {
              len = ((PortugueseLightStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof PortugueseMinimalStemmer) {
              len = ((PortugueseMinimalStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof PortugueseStemmer) {
              len = ((PortugueseStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof RussianLightStemmer) {
              len = ((RussianLightStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof SoraniStemmer) {
              len = ((SoraniStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof SpanishLightStemmer) {
              len = ((SpanishLightStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            } else if (fallbackStemmer instanceof SwedishLightStemmer) {
              len = ((SwedishLightStemmer) fallbackStemmer).stem(buffer, termAtt.length());
            }
            if (len > 0) {
              termAtt.setEmpty().append(tokenTerm.substring(0, len));
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

  protected boolean createToken(final String token, final AttributeSource.State current) {
    restoreState(current);
    termAtt.setEmpty().append(token);
    positionAttr.setPositionIncrement(0);
    return true;
  }

  @Override
  public void reset() throws IOException {
    super.reset();
    current = null;
  }

}