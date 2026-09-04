package wecandoeverything.ledgerly.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wecandoeverything.ledgerly.dto.EmailDraftDto;
import wecandoeverything.ledgerly.dto.EmailDraftRequestDto;
import wecandoeverything.ledgerly.dto.SubscriptionAnalysisDto;
import wecandoeverything.ledgerly.dto.SubscriptionAnalysisRequest;
import wecandoeverything.ledgerly.service.EmailDraftService;
import wecandoeverything.ledgerly.service.SubscriptionAnalysisService;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Manage and analyze subscriptions")
public class SubscriptionController {

    private final SubscriptionAnalysisService analysisService;
    private final EmailDraftService emailDraftService;

    @Operation(summary = "Analyze subscriptions")
    @PostMapping("/analyze")
    public ResponseEntity<List<SubscriptionAnalysisDto>> analyze(
            @Valid @RequestBody SubscriptionAnalysisRequest request) {
        return ResponseEntity.ok(analysisService.analyze(request.getSubscriptions()));
    }

    @Operation(summary = "Write a draft of email")
    @PostMapping("/draft-email")
    public ResponseEntity<EmailDraftDto> draftEmail(
            @Valid @RequestBody EmailDraftRequestDto request) {
        return ResponseEntity.ok(emailDraftService.draft(request.getSubscription(), request.getAction(), request.getLanguage()));
    }
}