package org.opencds.cqf.fhir.cr.measure.constant;

public class HtmlConstants {
    private HtmlConstants() {}

    public static final String HTML_DIV_CONTENT = "<div xmlns=\"http://www.w3.org/1999/xhtml\">%s</div>";
    public static final String HTML_PARAGRAPH_CONTENT = "<p>%s</p>";
    public static final String HTML_DIV_PARAGRAPH_CONTENT = HTML_DIV_CONTENT.formatted(HTML_PARAGRAPH_CONTENT);
}
