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

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.BaseTokenStreamFactoryTestCase;
import org.apache.lucene.analysis.util.ClasspathResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoader;

public class TestDictionaryLemmatizerFilterFactory extends BaseTokenStreamFactoryTestCase {

	public void testLemmatizerWithNotRegularLemmas() throws Exception {
		Reader reader = new StringReader("bücher eldre");
		TokenStream stream = whitespaceMockTokenizer(reader);
		Map<String, String> args = new HashMap<String, String>();
		args.put("dictionaries", "dictionary.txt");
		args.put("lemmaPos", "0");
		args.put("wordPos", "1");  
		DictionaryLemmatizerFilterFactory f = new DictionaryLemmatizerFilterFactory(args);
		ResourceLoader l = new ClasspathResourceLoader();
		f.inform(l);
		stream = f.create(stream);
		assertTrue(stream instanceof DictionaryLemmatizerFilter);
		assertTokenStreamContents(stream, new String[] { "buch", "gammel" });
	}

	public void testLemmatizerWithNotRegularLemmasInDirectMemory() throws Exception {
		Reader reader = new StringReader("bücher eldre");
		TokenStream stream = whitespaceMockTokenizer(reader);
		Map<String, String> args = new HashMap<String, String>();
		args.put("dictionaries", "dictionary.txt");
		args.put("lemmaPos", "0");
		args.put("wordPos", "1");
		args.put("directMemory", "true");
		DictionaryLemmatizerFilterFactory f = new DictionaryLemmatizerFilterFactory(args);
		ResourceLoader l = new ClasspathResourceLoader();
		f.inform(l);
		stream = f.create(stream);
		assertTrue(stream instanceof DictionaryLemmatizerFilter);
		assertTokenStreamContents(stream, new String[] { "buch", "gammel" });
	}

  public void testLemmatizerWithNotRegularLemmasHuge() throws Exception {
    Reader reader = new StringReader("abaisseriez agnellera");
    TokenStream stream = whitespaceMockTokenizer(reader);
    Map<String, String> args = new HashMap<String, String>();
    args.put("dictionaries", "french-verb.txt");
    args.put("lemmaPos", "0");
    args.put("wordPos", "1");
    DictionaryLemmatizerFilterFactory f = new DictionaryLemmatizerFilterFactory(args);
    ResourceLoader l = new ClasspathResourceLoader();
    f.inform(l);
    stream = f.create(stream);
    assertTrue(stream instanceof DictionaryLemmatizerFilter);
    assertTokenStreamContents(stream, new String[] { "abaisser", "agneler" });
  }

  public void testLemmatizerWithNotRegularLemmasInDirectMemoryHuge() throws Exception {
    Reader reader = new StringReader("abaisseriez agnellera");
    TokenStream stream = whitespaceMockTokenizer(reader);
    Map<String, String> args = new HashMap<String, String>();
    args.put("dictionaries", "french-verb.txt");
    args.put("lemmaPos", "0");
    args.put("wordPos", "1");
    args.put("directMemory", "true");
    DictionaryLemmatizerFilterFactory f = new DictionaryLemmatizerFilterFactory(args);
    ResourceLoader l = new ClasspathResourceLoader();
    f.inform(l);
    stream = f.create(stream);
    assertTrue(stream instanceof DictionaryLemmatizerFilter);
    assertTokenStreamContents(stream, new String[] { "abaisser", "agneler" });
  }

  public void testLemmatizerWithNotRegularLemmasFallBackSnowballStemmer() throws Exception {
    Reader reader = new StringReader("bücher eldre tables");
    TokenStream stream = whitespaceMockTokenizer(reader);
    Map<String, String> args = new HashMap<String, String>();
    args.put("dictionaries", "dictionary.txt");
    args.put("lemmaPos", "0");
    args.put("wordPos", "1");
    args.put("fallBackStemmer", "SnowballStemmer|language=English");
    DictionaryLemmatizerFilterFactory f = new DictionaryLemmatizerFilterFactory(args);
    ResourceLoader l = new ClasspathResourceLoader();
    f.inform(l);
    stream = f.create(stream);
    assertTrue(stream instanceof DictionaryLemmatizerFilter);
    assertTokenStreamContents(stream, new String[] { "buch", "gammel", "tabl" });
  }

  public void testLemmatizerWithNotRegularLemmasFallBackStemmer() throws Exception {
    Reader reader = new StringReader("bücher eldre tables");
    TokenStream stream = whitespaceMockTokenizer(reader);
    Map<String, String> args = new HashMap<String, String>();
    args.put("dictionaries", "dictionary.txt");
    args.put("lemmaPos", "0");
    args.put("wordPos", "1");
    args.put("fallBackStemmer", "EnglishMinimalStemmer");
    DictionaryLemmatizerFilterFactory f = new DictionaryLemmatizerFilterFactory(args);
    ResourceLoader l = new ClasspathResourceLoader();
    f.inform(l);
    stream = f.create(stream);
    assertTrue(stream instanceof DictionaryLemmatizerFilter);
    assertTokenStreamContents(stream, new String[] { "buch", "gammel", "table" });
  }

	public void testLemmatizerWithMultipleLemmas() throws Exception {
		Reader reader = new StringReader("sykler");
		TokenStream stream = whitespaceMockTokenizer(reader);
		Map<String, String> args = new HashMap<String, String>();
		args.put("dictionaries", "dictionary.txt");
		args.put("lemmaPos", "0");
		args.put("wordPos", "1");  
		DictionaryLemmatizerFilterFactory f = new DictionaryLemmatizerFilterFactory(args);
		ResourceLoader l = new ClasspathResourceLoader();
		f.inform(l);
		stream = f.create(stream);
		assertTokenStreamContents(stream, new String[] { "sykle", "sykkel" }, new int[] { 1, 0 });
	}

	public void testLemmatizerUsingPOSTags() throws Exception {
		Reader reader = new StringReader("sykler");
		TokenStream stream = whitespaceMockTokenizer(reader);
		Map<String, String> args = new HashMap<String, String>();
		args.put("dictionaries", "dictionary.txt");
		args.put("lemmaPos", "0");
		args.put("wordPos", "1");  
		args.put("wordClassPos", "2");  
		args.put("storePosTag", "true");  
		args.put("wordClasses", "noun,verb");  
		DictionaryLemmatizerFilterFactory f = new DictionaryLemmatizerFilterFactory(args);
		ResourceLoader l = new ClasspathResourceLoader();
		f.inform(l);
		stream = f.create(stream);
		assertTokenStreamContents(stream, new String[] { "sykkel$0", "sykle$1" }, new int[] { 1, 0 });
	}

	public void testLemmatizerUsingReduction() throws Exception {
		Reader reader = new StringReader("sykler");
		TokenStream stream = whitespaceMockTokenizer(reader);
		Map<String, String> args = new HashMap<String, String>();
		args.put("dictionaries", "dictionary.txt");
		args.put("lemmaPos", "0");
		args.put("wordPos", "1");  
		args.put("wordClassPos", "2");  
		args.put("reduceTo", "noun");  
		args.put("wordClasses", "noun,verb");  
		DictionaryLemmatizerFilterFactory f = new DictionaryLemmatizerFilterFactory(args);
		ResourceLoader l = new ClasspathResourceLoader();
		f.inform(l);
		stream = f.create(stream);
		assertTokenStreamContents(stream, new String[] { "sykkel" });
	}

	/** Test that bogus arguments result in exception */
	public void testBogusArguments() throws Exception {
		try {
			Reader reader = new StringReader("sykler");
			TokenStream stream = whitespaceMockTokenizer(reader);
			Map<String, String> args = new HashMap<String, String>();
			args.put("dictionaries", "dictionary.txt");
			args.put("lemmaPos", "0");
			args.put("wordPos", "1");  
			args.put("bogusArg", "bogusValue");  
			DictionaryLemmatizerFilterFactory f = new DictionaryLemmatizerFilterFactory(args);
			stream = f.create(stream);
			fail();
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("Unknown parameters"));
		}
	}
}