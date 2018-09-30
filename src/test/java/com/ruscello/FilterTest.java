package com.ruscello;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FilterTest {

    @Test
    public void stopFilterTest() throws IOException {
        StringReader stringReader = new StringReader("Quick brown fox jumped over the lazy dog");
        Set<String> stopWords = new HashSet(Arrays.asList("is", "the", "Time"));
        CharArraySet sw = new CharArraySet(stopWords, true);

        Analyzer analyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new StandardTokenizer();
                return new TokenStreamComponents(source, new StopFilter(source, sw));
            }

        };

        TokenStream ts = analyzer.tokenStream(null, stringReader);
        ts.reset();
        CharTermAttribute tatt = ts.getAttribute(CharTermAttribute.class);
        while (ts.incrementToken()) {
            String token = tatt.toString();
            System.out.println(token);
        }
    }
}
