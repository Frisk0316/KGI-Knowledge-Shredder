package com.kgi.shredder.service.document;

import java.io.ByteArrayInputStream;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

@Service
public class TextExtractionService {
    private final AutoDetectParser parser = new AutoDetectParser();

    public ParsedDocument extract(byte[] fileBytes, String originalFilename) {
        try {
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, originalFilename);
            BodyContentHandler handler = new BodyContentHandler(-1);
            parser.parse(new ByteArrayInputStream(fileBytes), handler, metadata);
            return new ParsedDocument(clean(handler.toString()), "apache-tika");
        } catch (TikaException | SAXException | java.io.IOException ex) {
            throw new IllegalArgumentException("Unable to parse uploaded document.", ex);
        }
    }

    private String clean(String text) {
        return text == null ? "" : text.replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    public record ParsedDocument(String rawText, String parserName) {
    }
}
