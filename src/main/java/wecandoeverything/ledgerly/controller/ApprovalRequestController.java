package wecandoeverything.ledgerly.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wecandoeverything.ledgerly.domain.ApprovalStatus;
import wecandoeverything.ledgerly.dto.ApprovalRequestCreateDto;
import wecandoeverything.ledgerly.dto.ApprovalRequestResponseDto;
import wecandoeverything.ledgerly.service.ApprovalRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/approval-requests")
@RequiredArgsConstructor
@Tag(name = "Approval Requests", description = "Submit and manage expense approval requests")
public class ApprovalRequestController {

    private final ApprovalRequestService service;

    @Operation(summary = "Submit a new expense for approval")
    @PostMapping
    public ResponseEntity<ApprovalRequestResponseDto> create(
            @Valid @RequestBody ApprovalRequestCreateDto request) {
        ApprovalRequestResponseDto created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Get all pending approval requests")
    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalRequestResponseDto>> getPending() {
        return ResponseEntity.ok(service.getPending());
    }

    @Operation(summary = "Approve a request")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApprovalRequestResponseDto> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @Operation(summary = "Reject a request")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApprovalRequestResponseDto> reject(@PathVariable Long id) {
        return ResponseEntity.ok(service.reject(id));
    }

    @Operation(summary = "List approval requests, optionally filtered by status")
    @GetMapping
    public ResponseEntity<List<ApprovalRequestResponseDto>> getAll(
            @RequestParam(value = "status", required = false) ApprovalStatus status) {
        return ResponseEntity.ok(service.getByStatus(status));
    }
}