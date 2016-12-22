# solr-lemmatizer

## configuration - shema.xml

```
    <fieldType name="text_lem" class="solr.TextField" positionIncrementGap="100" multiValued="true">
      <analyzer type="index">
        <tokenizer class="solr.StandardTokenizerFactory"/>
        <filter class="solr.StopFilterFactory" ignoreCase="true" words="stopwords.txt" />
        <filter class="org.apache.lucene.analysis.lemmatizer.DictionaryLemmatizerFilterFactory" 
        	dictionaries="dictionary.txt"
        	lemmaPos="0"
			wordPos="1" 
        />
        <filter class="solr.LowerCaseFilterFactory"/>
      </analyzer>
      <analyzer type="query">
        <tokenizer class="solr.StandardTokenizerFactory"/>
        <filter class="solr.StopFilterFactory" ignoreCase="true" words="stopwords.txt" />
        <filter class="org.apache.lucene.analysis.lemmatizer.DictionaryLemmatizerFilterFactory" 
        	dictionaries="dictionary.txt"
        	lemmaPos="0"
			wordPos="1" 
        />
        <filter class="solr.LowerCaseFilterFactory"/>
      </analyzer>
    </fieldType>
```

Parameter | Sample | Default | Description
------------|-------------|-------------|-------------
dictionaries|dic1.txt.gz,dic2.txt||
lemmaPos|1||Where to find the lemmas
wordPos|2||Where to find the words
wordClassPos|3||(optional) Where to find the word classes. 
charset|iso-8859-1|UTF-8|(optional) charset of the dic file
wordClasses|subst,verb,adj||which word class to add (note: bad parameter name, will be changed)
reduceTo|subst,verb||words with several stems get reduced to one in this order. Optionally
minLength|||(optional) word minimum length in dictionnary. Smaller words will be ignored.
storePosTag|false|false|(optional) if 'true' wordClassPos should be >0 and wordClasses shouldn't be empty


## Dictionary - dictionary.txt

```
#Lemma	word	word class

sykkel	sykler	noun
sykkel	sykkelen	noun
sykle	sykler	verb
sykle	syklet	verb

frage	fragen	noun
frage	frage noun
fragen	fragte	verb
fragen	fragt	verb

buch	bücher	noun
gammel	eldre	noun
```
