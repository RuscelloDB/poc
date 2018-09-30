package com.ruscello;

// EdgeNGramTokenizerFactory
public class EdgeNGram { //extends AbstractTokenizerFactory {

//    private final int minGram;
//
//    private final int maxGram;
//
//    private final CharMatcher matcher;
//
//
//    public EdgeNGramTokenizerFactory(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
//        super(indexSettings, name, settings);
//        this.minGram = settings.getAsInt("min_gram", NGramTokenizer.DEFAULT_MIN_NGRAM_SIZE);
//        this.maxGram = settings.getAsInt("max_gram", NGramTokenizer.DEFAULT_MAX_NGRAM_SIZE);
//        this.matcher = parseTokenChars(settings.getAsList("token_chars"));
//    }
//
//    @Override
//    public Tokenizer create() {
//        if (matcher == null) {
//            return new EdgeNGramTokenizer(minGram, maxGram);
//        } else {
//            return new EdgeNGramTokenizer(minGram, maxGram) {
//                @Override
//                protected boolean isTokenChar(int chr) {
//                    return matcher.isTokenChar(chr);
//                }
//            };
//        }
//    }
}
