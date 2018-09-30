package com.ruscello;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;

public class CamelCaseFilter extends TokenFilter {

    private final CharTermAttribute _termAtt;

    protected CamelCaseFilter(TokenStream input) {
        super(input);
        this._termAtt = addAttribute(CharTermAttribute.class);
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!input.incrementToken())
            return false;
        CharTermAttribute a = this.getAttribute(CharTermAttribute.class);
        String spliettedString = splitCamelCase(a.toString());
        _termAtt.setEmpty();
        _termAtt.append(spliettedString);
        return true;

    }


    static String splitCamelCase(String s) {
        return s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );
    }
}