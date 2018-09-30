package com.ruscello;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ngram.EdgeNGramFilterFactory;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.reverse.ReverseStringFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static java.util.Collections.unmodifiableMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NGramTest {

    @Test
    public void ngramTest() throws IOException {
        StringReader stringReader = new StringReader("seanCarroll");
        NGramTokenizer tokenizer = new NGramTokenizer(1, 4);
        tokenizer.setReader(stringReader);
        tokenizer.reset();
        CharTermAttribute termAtt = tokenizer.getAttribute(CharTermAttribute.class);
        // TODO: need to remove duplicates
        final List<String> expected = Arrays.asList(
                "s", "se", "sea", "sean", "e", "ea", "ean", "eanC", "a", "an", "anC", "anCa",
                "n", "nC", "nCa", "nCar", "C", "Ca", "Car", "Carr", "a", "ar", "arr", "arro",
                "r", "rr", "rro", "rrol", "r", "ro", "rol", "roll", "o", "ol", "oll", "l", "ll", "l"
        );
        int tokenCount = 0;
        while (tokenizer.incrementToken()) {
            String token = termAtt.toString();
            System.out.println(token);
            assertTrue(expected.contains(token));
            tokenCount++;
        }
        assertEquals(expected.size(), tokenCount);
    }

    @Test
    public void edgeNGramDefaultTest() throws IOException {
        StringReader stringReader = new StringReader("sean-carroll");
        EdgeNGramTokenizer tokenizer = new EdgeNGramTokenizer(1, 4);

        final List<String> expected = Arrays.asList("s", "se", "sea", "sean");

        tokenizer.setReader(stringReader);
        tokenizer.reset();
        CharTermAttribute termAtt = tokenizer.getAttribute(CharTermAttribute.class);
        int tokenCount = 0;
        while (tokenizer.incrementToken()) {
            String token = termAtt.toString();
            System.out.println(token);
            tokenCount++;
        }
        assertEquals(expected.size(), tokenCount);
    }


    @Test
    public void edgeNGramCustomTest() throws IOException {
        StringReader stringReader = new StringReader("sean-carroll");

        CharMatcher matcher = parseTokenChars(Arrays.asList("letter", "digit"));
        EdgeNGramTokenizer tokenizer = new EdgeNGramTokenizer(1, 4) {
            @Override
            protected boolean isTokenChar(int chr) {
                return matcher.isTokenChar(chr);
            }
        };
        tokenizer.setReader(stringReader);
        tokenizer.reset();
        CharTermAttribute termAtt = tokenizer.getAttribute(CharTermAttribute.class);

        final List<String> expected = Arrays.asList("s", "se", "sea", "sean", "c", "ca", "car", "carr");
        int tokenCount = 0;
        while (tokenizer.incrementToken()) {
            String token = termAtt.toString();
            System.out.println(token);
            assertTrue(expected.contains(token));
            tokenCount++;
        }
        assertEquals(expected.size(), tokenCount);
    }

    // TODO: better understand the difference between tokenizers and filters
    @Test
    public void reverseEdgeNGramTest() throws IOException {
        StringReader stringReader = new StringReader("sean-carroll");
        EdgeNGramFilterFactory reverseEdgeNGram = new EdgeNGramFilterFactory(new HashMap<>()) {
            @Override
            public TokenFilter create(TokenStream input) {
                TokenStream result = input;
                // applying ReverseStringFilter up-front and after the token filter as the same effect
                result = new ReverseStringFilter(result);
                result = new EdgeNGramTokenFilter(result, 1, 4);
                // applying ReverseStringFilter up-front and after the token filter has the same effect
                return new ReverseStringFilter(result);
            }
        };

        Analyzer analyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new StandardTokenizer();
                TokenStream filter = reverseEdgeNGram.create(source);

                return new TokenStreamComponents(source, filter);
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

    @Test
    public void reverseEdgeNGramTokenizerTest() {
        // https://stackoverflow.com/questions/42581623/front-and-back-edgengrams-in-solr
    }


    @Test
    public void forwardAndReverseEdgeNGramTest() throws IOException {
        StringReader stringReader = new StringReader("sean-carroll");
        Map<String, String> settings = new HashMap<>(2);
        settings.put("minGramSize", "1");
        settings.put("maxGramSize", "4");
        EdgeNGramFilterFactory f = new EdgeNGramFilterFactory(settings) {

            @Override
            public TokenFilter create(TokenStream input) {
                try {
                    // TODO: Fix. This produces the wrong result
                    // Not sure that filters are appropriate way to handle this we need to process the same TokenStream
                    // multiple times.
                    TokenStream result = input;
                    result = new EdgeNGramTokenFilter(result, 1, 4);
                    // applying ReverseStringFilter up-front and after the token filter as the same effect
                    result = new ReverseStringFilter(result);
                    result = new EdgeNGramTokenFilter(result, 1, 4);
                    // applying ReverseStringFilter up-front and after the token filter has the same effect
                    return new ReverseStringFilter(result);
                } catch(Exception ex) {

                }
                return null;
            }
        };

        Analyzer analyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new StandardTokenizer(); //WordDelimiterGraphFilter
                TokenStream filter = f.create(source);
                return new TokenStreamComponents(source, filter);
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

    // WordDelimiterFilterFactory
    // LowerCaseFilterFactory
    // EdgeNGramFilterFactory

    // what are BACK grams used for?
    // Back grams would work for leading wildcards. They might be useful for things where the head is at the end (tail-first?), like domain names.
    // Not super-useful, but it is a small part of the code in the tokenizer.

    // new ShingleFilter( tokenStream, 2 );
    // https://stackoverflow.com/questions/17275510/how-to-combine-two-tokenizers-in-lucene-japaneseanalyzer-and-standardanalyzer

    /*
    @Test
    public void reverseEdgeNGramTest() throws IOException {
        StringReader stringReader = new StringReader("sean-carroll");
        ReverseStringFilter.reverse("sean-carroll");
//        ReverseStringFilter reverseStringFilter = new ReverseStringFilter();
//        EdgeNGramTokenFilter

        // https://github.com/elastic/elasticsearch/blob/428e70758ac6895ac995f4315412f4d3729aea9b/modules/analysis-common/src/main/java/org/elasticsearch/analysis/common/EdgeNGramTokenFilterFactory.java
        // https://github.com/elastic/elasticsearch/blob/99f88f15c5febbca2d13b5b5fda27b844153bf1a/server/src/test/java/org/elasticsearch/search/suggest/phrase/NoisyChannelSpellCheckerTests.java

        EdgeNGramFilterFactory f = new EdgeNGramFilterFactory(new HashMap<>()) {
            @Override
            public TokenFilter create(TokenStream input) {
                TokenStream result = input;
                // applying ReverseStringFilter up-front and after the token filter as the same effect
                result = new ReverseStringFilter(result);
                result = new EdgeNGramTokenFilter(result, 1, 4);
                // applying ReverseStringFilter up-front and after the token filter has the same effect
                return new ReverseStringFilter(result);
            }
        };



//        - keyword tokenizer
//        - reverse token filter
//        - edge ngram


        // TokenStream result = null;
        // applying ReverseStringFilter up-front and after the token filter as the same effect
        // result = new ReverseStringFilter(result);
        // result = new EdgeNGramTokenFilter(result, 1, 4);
        // applying ReverseStringFilter up-front and after the token filter has the same effect
        // result = new ReverseStringFilter(result);

        Analyzer analyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
//                Tokenizer tokenizer = new PlusSignTokenizer(reader);
//                TokenStream filter = new EmptyStringTokenFilter(tokenizer);
//                filter = new LowerCaseFilter(filter);
//                return new TokenStreamComponents(tokenizer, filter);

//                Tokenizer source = new StandardTokenizer();
//
//                TokenStream filter = new WordDelimiterGraphFilter(source, 8, null);
//                return new TokenStreamComponents(source, filter);

                Tokenizer source = new StandardTokenizer();
                TokenStream filter = f.create(source);
                return new TokenStreamComponents(source, filter);
            }

        };


        TokenStream ts = analyzer.tokenStream(null, stringReader);
        ts.reset();
        CharTermAttribute tatt = ts.getAttribute(CharTermAttribute.class);
        while (ts.incrementToken()) {
            String token = tatt.toString();
            System.out.println(token);
        }

        // TokenStream stream  = analyzer.tokenStream(field, new StringReader(keywords));
        // TokenStream stream = analyzer.tokenStream(null, new StringReader(text));

//        CharMatcher matcher = parseTokenChars(Arrays.asList("letter", "digit"));
//        EdgeNGramTokenizer tokenizer = new EdgeNGramTokenizer(1, 4) {
//            @Override
//            protected boolean isTokenChar(int chr) {
//                return matcher.isTokenChar(chr);
//            }
//
//        };
//        tokenizer.setReader(stringReader);
//        tokenizer.reset();
//        CharTermAttribute termAtt = tokenizer.getAttribute(CharTermAttribute.class);
//
//        final List<String> expected = Arrays.asList("s", "se", "sea", "sean", "c", "ca", "car", "carr");
//        int tokenCount = 0;
//        while (tokenizer.incrementToken()) {
//            String token = termAtt.toString();
//            System.out.println(token);
//            assertTrue(expected.contains(token));
//            tokenCount++;
//        }
//        assertEquals(expected.size(), tokenCount);
    }
     */



    static final Map<String, CharMatcher> MATCHERS;

    static {
        Map<String, CharMatcher> matchers = new HashMap<>();
        matchers.put("letter", CharMatcher.Basic.LETTER);
        matchers.put("digit", CharMatcher.Basic.DIGIT);
        matchers.put("whitespace", CharMatcher.Basic.WHITESPACE);
        matchers.put("punctuation", CharMatcher.Basic.PUNCTUATION);
        matchers.put("symbol", CharMatcher.Basic.SYMBOL);
        // Populate with unicode categories from java.lang.Character
        for (Field field : Character.class.getFields()) {
            if (!field.getName().startsWith("DIRECTIONALITY")
                    && Modifier.isPublic(field.getModifiers())
                    && Modifier.isStatic(field.getModifiers())
                    && field.getType() == byte.class) {
                try {
                    matchers.put(field.getName().toLowerCase(Locale.ROOT), CharMatcher.ByUnicodeCategory.of(field.getByte(null)));
                } catch (Exception e) {
                    // just ignore
                    continue;
                }
            }
        }
        MATCHERS = unmodifiableMap(matchers);
    }

    static CharMatcher parseTokenChars(List<String> characterClasses) {
        if (characterClasses == null || characterClasses.isEmpty()) {
            return null;
        }
        CharMatcher.Builder builder = new CharMatcher.Builder();
        for (String characterClass : characterClasses) {
            characterClass = characterClass.toLowerCase(Locale.ROOT).trim();
            CharMatcher matcher = MATCHERS.get(characterClass);
            if (matcher == null) {
                throw new IllegalArgumentException("Unknown token type: '" + characterClass + "', must be one of " + MATCHERS.keySet());
            }
            builder.or(matcher);
        }
        return builder.build();
    }



//    @Test
//    public void t() {
//        StringReader stringReader = new StringReader("seanCarroll");
//
//        Map<String,String> args = new HashMap<>();
//        args.put("generateWordParts", "1");
//        args.put("generateNumberParts", "1");
//        args.put("catenateWords", "1");
//        args.put("catenateNumbers", "1");
//        args.put("catenateAll", "0");
//        args.put("splitOnCaseChange", "1");
//        WordDelimiterGraphFilterFactory fact = new WordDelimiterGraphFilterFactory(args);
//
//        TokenStream ts = fact.create(new LowerCaseTokenizer());
//
//
//    }

}
