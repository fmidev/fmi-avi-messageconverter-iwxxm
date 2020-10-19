package fi.fmi.avi.converter.iwxxm.bulletin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;

import fi.fmi.avi.converter.ConversionException;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.iwxxm.XMLSchemaInfo;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletin;

public class BulletinIWXXMStringSerializer<U extends AviationWeatherMessage, S extends MeteorologicalBulletin<U>>
        extends AbstractBulletinIWXXMSerializer<String, U, S> {
    private static final Pattern XML_DECLARATION_PATTERN = Pattern.compile("^\\s*<\\?xml\\s[^>]*\\?>(?:[ \t]*(?:\r\n|\r|\n))?");
    private static final int INDENT_LENGTH = 2;

    @Override
    protected String aggregateAsBulletin(final Document collection, final List<Document> messages, final ConversionHints hints) throws ConversionException {
        final String collectionString = renderDOMToString(collection, hints);
        final int documentsInsertionIndex = collectionString.indexOf(">", collectionString.indexOf("<" + collection.getDocumentElement().getTagName())) + 1;
        final int baseIndentation = baseIndentation(collectionString, documentsInsertionIndex);
        final String metInfoElementFQN = documentElementPrefix(collection) + "meteorologicalInformation";
        final StringBuilder builder = new StringBuilder(collectionString.substring(0, documentsInsertionIndex));
        for (final Document message : messages) {
            try (final BufferedReader reader = new BufferedReader(new StringReader(removeXmlDeclaration(renderDOMToString(message, hints))))) {
                appendNewLineWithIndent(builder, baseIndentation);
                builder.append("<").append(metInfoElementFQN).append(">");
                String line = reader.readLine();
                while (line != null) {
                    appendNewLineWithIndent(builder, baseIndentation + INDENT_LENGTH);
                    builder.append(line);
                    line = reader.readLine();
                }
                appendNewLineWithIndent(builder, baseIndentation);
                builder.append("</").append(metInfoElementFQN).append(">");
            } catch (final IOException e) {
                throw new ConversionException("Error reading input messages", e);
            }
        }
        return builder.append(collectionString.substring(documentsInsertionIndex)).toString();
    }

    private String documentElementPrefix(final Document document) {
        final String prefix = document.getDocumentElement().getPrefix();
        return prefix == null ? "" : prefix + ":";
    }

    private String removeXmlDeclaration(final String xml) {
        if (xml == null) {
            return null;
        }
        final Matcher matcher = XML_DECLARATION_PATTERN.matcher(xml);
        return matcher.find() ? xml.substring(matcher.end()) : xml;
    }

    private int baseIndentation(final String collectionString, final int offset) {
        int baseIndentation = 0;
        while ((offset - baseIndentation) > 0 && collectionString.charAt(offset - baseIndentation) != '\n') {
            baseIndentation++;
        }
        baseIndentation += INDENT_LENGTH;
        return baseIndentation;
    }

    private void appendNewLineWithIndent(final StringBuilder builder, final int intendation) {
        builder.append('\n');
        for (int i = 0; i < intendation; i++) {
            builder.append(' ');
        }
    }

    @Override
    protected IssueList validate(final String output, final XMLSchemaInfo schemaInfo, final ConversionHints hints) throws ConversionException {
        return BulletinIWXXMStringSerializer.validateStringAgainstSchemaAndSchematron(output, schemaInfo, hints);
    }
}
