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
		//stream = tokenFilterFactory("DictionaryLemmatizer", "dictionaries", "dictionary.txt",
		//    "lemmaPos", "0", "wordPos", "1").create(stream);
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

	public void testLemmatizerWithMultipleLemmas() throws Exception {
		Reader reader = new StringReader("sykler");
		TokenStream stream = whitespaceMockTokenizer(reader);
		//stream = tokenFilterFactory("DictionaryLemmatizer", "dictionaries", "dictionary.txt",
		//    "lemmaPos", "0", "wordPos", "1").create(stream);
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
		//stream = tokenFilterFactory("DictionaryLemmatizer", "dictionaries", "dictionary.txt",
		//    "lemmaPos", "0", "wordPos", "1", "wordClassPos", "2", "storePosTag", "true", "wordClasses",
		//    "noun,verb").create(stream);
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
		//stream = tokenFilterFactory("DictionaryLemmatizer", "dictionaries", "dictionary.txt",
		//    "lemmaPos", "0", "wordPos", "1", "wordClassPos", "2", "reduceTo", "noun", "wordClasses",
		//    "noun,verb").create(stream);
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
			//tokenFilterFactory("DictionaryLemmatizer", "dictionaries", "dictionary.txt", "lemmaPos", "0",
			//    "wordPos", "1", "bogusArg", "bogusValue");
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