package wecandoeverything.ledgerly.controller;

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
public class ReceiptScanController {

    private final ReceiptScanService receiptScanService;

    @PostMapping(value = "/scan", consumes = "multipart/form-data")
    public ResponseEntity<ReceiptScanResultDto> scan(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(receiptScanService.scan(file));
    }
}