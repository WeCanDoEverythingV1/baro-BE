package wecandoeverything.ledgerly.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wecandoeverything.ledgerly.dto.ApprovalRequestCreateDto;
import wecandoeverything.ledgerly.dto.ApprovalRequestResponseDto;
import wecandoeverything.ledgerly.service.ApprovalRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/approval-requests")
@RequiredArgsConstructor
public class ApprovalRequestController {

    private final ApprovalRequestService service;

    @PostMapping
    public ResponseEntity<ApprovalRequestResponseDto> create(
            @Valid @RequestBody ApprovalRequestCreateDto dto) {
        ApprovalRequestResponseDto created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ApprovalRequestResponseDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalRequestResponseDto>> getPending() {
        return ResponseEntity.ok(service.getPending());
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApprovalRequestResponseDto> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApprovalRequestResponseDto> reject(@PathVariable Long id) {
        return ResponseEntity.ok(service.reject(id));
    }
}