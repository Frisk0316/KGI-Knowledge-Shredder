package com.kgi.shredder.service.document;

import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class SafePreviewService {
    private static final Pattern EMAIL = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern TW_ID = Pattern.compile("\\b[A-Z][12]\\d{8}\\b");
    private static final Pattern PHONE = Pattern.compile("\\b(?:\\+886[-\\s]?)?(?:0?9\\d{2}[-\\s]?\\d{3}[-\\s]?\\d{3}|0\\d{1,2}[-\\s]?\\d{3,4}[-\\s]?\\d{3,4})\\b");
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");

    public String preview(String rawText) {
        String redacted = EMAIL.matcher(rawText).replaceAll("[REDACTED_EMAIL]");
        redacted = TW_ID.matcher(redacted).replaceAll("[REDACTED_TW_ID]");
        redacted = PHONE.matcher(redacted).replaceAll("[REDACTED_PHONE]");
        redacted = CARD.matcher(redacted).replaceAll("[REDACTED_NUMBER]");
        if (redacted.length() <= 1200) {
            return redacted;
        }
        return redacted.substring(0, 1200).stripTrailing() + "\n\n...[Preview truncated]...";
    }
}
