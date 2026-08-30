package wecandoeverything.ledgerly.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;
import wecandoeverything.ledgerly.dto.ReceiptScanResultDto;
import wecandoeverything.ledgerly.exception.ReceiptUnreadableException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReceiptScanService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptScanService.class);

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "application/pdf");

    private final GeminiClient geminiClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReceiptScanResultDto scan(MultipartFile file) {
        validate(file);

        String base64 = encode(file);
        String prompt = """
                You are reading a business expense receipt. Extract the following
                fields from the image. If a field is genuinely not visible on the
                receipt, return an empty string for text fields or 0 for amount —
                do not guess or invent values.

                - merchant: the business name
                - date: the transaction date, formatted exactly as YYYY-MM-DD
                - amount: the total amount paid, as a plain number (no currency symbol)
                - itemName: a short 3-6 word description of what was purchased
                - purpose: leave this as an empty string — the employee will fill it in
                """;

        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "merchant", Map.of("type", "STRING"),
                        "date", Map.of("type", "STRING"),
                        "amount", Map.of("type", "NUMBER"),
                        "itemName", Map.of("type", "STRING"),
                        "purpose", Map.of("type", "STRING")
                ),
                "required", List.of("merchant", "date", "amount", "itemName", "purpose")
        );

        String json = geminiClient.generate(prompt, schema, file.getContentType(), base64);
        return toDto(json);
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String contentType = file.getContentType();
        log.info("Received file with content type: {}", contentType);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + file.getContentType() + ". Use JPG, PNG, or PDF.");
        }
    }

    private String encode(MultipartFile file) {
        try {
            return Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }
    }

    private ReceiptScanResultDto toDto(String json) {
        try {
            Map<String, Object> parsed = mapper.readValue(json, Map.class);

            String merchant = (String) parsed.get("merchant");
            if (merchant == null || merchant.isBlank()) {
                throw new ReceiptUnreadableException(
                        "Could not read a merchant name from this receipt. Try a clearer photo.");
            }

            return ReceiptScanResultDto.builder()
                    .merchant(merchant)
                    .date(parseDate((String) parsed.get("date")))
                    .amount(BigDecimal.valueOf(((Number) parsed.get("amount")).doubleValue()))
                    .itemName((String) parsed.get("itemName"))
                    .purpose((String) parsed.get("purpose"))
                    .build();
        } catch (ReceiptUnreadableException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse receipt scan result: " + json, e);
        }
    }

    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (Exception e) {
            return null; // let the employee fill it in manually rather than failing the whole scan
        }
    }
}