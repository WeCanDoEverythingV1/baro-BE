package wecandoeverything.ledgerly.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import wecandoeverything.ledgerly.dto.ReceiptScanResultDto;
import wecandoeverything.ledgerly.service.ReceiptScanService;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipt Scan", description = "Scan a receipt")
public class ReceiptScanController {

    private final ReceiptScanService receiptScanService;

    @Operation(summary = "Scan a receipt file")
    @PostMapping(value = "/scan", consumes = "multipart/form-data")
    public ResponseEntity<ReceiptScanResultDto> scan(
            @RequestParam("file") MultipartFile file,
            @RequestParam("employeeName") String employeeName) {
        return ResponseEntity.ok(receiptScanService.scan(file, employeeName));
    }
}