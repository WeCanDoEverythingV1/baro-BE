package wecandoeverything.ledgerly.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder(toBuilder = true)
public class ReceiptScanResultDto {
    private String merchant;
    private LocalDate date;
    private BigDecimal amount;
    private String itemName;
    private String purpose;
    private String category;
    private boolean possibleDuplicate;
    private String duplicateNote; // null when no duplicate found
}