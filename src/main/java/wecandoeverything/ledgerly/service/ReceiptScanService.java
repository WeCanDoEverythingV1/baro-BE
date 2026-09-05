package wecandoeverything.ledgerly.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;
import wecandoeverything.ledgerly.domain.ApprovalRequest;
import wecandoeverything.ledgerly.domain.ExpenseCategory;
import wecandoeverything.ledgerly.dto.ReceiptScanResultDto;
import wecandoeverything.ledgerly.exception.ReceiptUnreadableException;
import wecandoeverything.ledgerly.repository.ApprovalRequestRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReceiptScanService {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "application/pdf");

    private static final List<String> CATEGORY_NAMES =
            Arrays.stream(ExpenseCategory.values()).map(Enum::name).toList();

    private final GeminiClient geminiClient;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final RiskAnalysisService riskAnalysisService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReceiptScanResultDto scan(MultipartFile file) {
        validate(file);
        String base64 = encode(file);

        String prompt = """
                You are reading a business expense receipt. Extract the following
                fields. If a field is genuinely not visible, return an empty
                string for text fields or 0 for amount — do not guess or invent
                values.

                - merchant: the business name
                - date: the transaction date, formatted exactly as YYYY-MM-DD
                - amount: the total amount paid, as a plain number
                - itemName: a short 3-6 word description of what was purchased
                - category: classify the purchase into exactly one of the
                  allowed categories, based on what was purchased
                - purpose: infer a short, plausible business reason for this
                  expense based on the merchant and items alone (e.g. a coffee
                  shop receipt might suggest "고객 미팅" or "팀 업무 논의").
                  Be specific where the receipt gives any hint, but keep it
                  brief — one short phrase. This is a draft suggestion the
                  employee will review and can edit, not a claim of fact.
                
                Write in korean.
                """;

        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "merchant", Map.of("type", "STRING"),
                        "date", Map.of("type", "STRING"),
                        "amount", Map.of("type", "NUMBER"),
                        "itemName", Map.of("type", "STRING"),
                        "category", Map.of("type", "STRING", "enum", CATEGORY_NAMES),
                        "purpose", Map.of("type", "STRING")
                ),
                "required", List.of("merchant", "date", "amount", "itemName", "category", "purpose")
        );

        String json = geminiClient.generate(prompt, schema, file.getContentType(), base64);
        ReceiptScanResultDto result = toDto(json);
        return applyDuplicateCheck(result);
    }

    private ReceiptScanResultDto applyDuplicateCheck(ReceiptScanResultDto result) {
        List<ApprovalRequest> matches = approvalRequestRepository
                .findByMerchantIgnoreCaseAndAmount(result.getMerchant(), result.getAmount());

        boolean duplicateFound = matches.stream().anyMatch(existing ->
                result.getDate() != null &&
                        Math.abs(java.time.temporal.ChronoUnit.DAYS.between(existing.getDate(), result.getDate())) <= 3
        );

        if (!duplicateFound) return result;

        return result.toBuilder()
                .possibleDuplicate(true)
                .duplicateNote("A similar request for " + result.getMerchant() +
                        " ($" + result.getAmount() + ") was already submitted within the last few days.")
                .build();
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + contentType + ". Use JPG, PNG, or PDF.");
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
                    .category((String) parsed.get("category"))
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
            return null;
        }
    }
}