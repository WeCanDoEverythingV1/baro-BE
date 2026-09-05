package wecandoeverything.ledgerly.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;
import wecandoeverything.ledgerly.dto.PolicyEvaluationInputDto;
import wecandoeverything.ledgerly.dto.PolicyEvaluationResultDto;
import wecandoeverything.ledgerly.dto.PolicyRulesetDto;
import wecandoeverything.ledgerly.dto.UpdateRulesRequestDto;
import wecandoeverything.ledgerly.exception.NoActivePolicyException;
import wecandoeverything.ledgerly.service.PolicyEvaluationService;
import wecandoeverything.ledgerly.service.PolicyService;

import java.util.List;

@Tag(name = "Policies", description = "Expense policy ruleset upload, review, and activation")
@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;
    private final PolicyEvaluationService policyEvaluationService;
    private final ObjectMapper mapper;

    @Operation(summary = "Upload a policy PDF and extract a draft ruleset")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<PolicyRulesetDto> create(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(policyService.create(file));
    }

    @Operation(summary = "List all rulesets, newest version first")
    @GetMapping
    public ResponseEntity<List<PolicyRulesetDto>> getAll() {
        return ResponseEntity.ok(policyService.getAll());
    }

    @Operation(summary = "Get the currently active ruleset, or null if none")
    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getActive() throws Exception {
        var dto = policyService.getActive();
        String body = dto.isPresent() ? mapper.writeValueAsString(dto.get()) : "null";
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Save reviewer edits to a ruleset's rules")
    @PutMapping("/{id}/rules")
    public ResponseEntity<PolicyRulesetDto> updateRules(
            @PathVariable String id, @Valid @RequestBody UpdateRulesRequestDto request) {
        return ResponseEntity.ok(policyService.updateRules(id, request.getRules()));
    }

    @Operation(summary = "Activate a ruleset, archiving the previous active one")
    @PostMapping("/{id}/activate")
    public ResponseEntity<PolicyRulesetDto> activate(@PathVariable String id) {
        return ResponseEntity.ok(policyService.activate(id));
    }

    @Operation(summary = "Delete a ruleset not referenced by any approval request")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        policyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Test-evaluate an expense against the active ruleset")
    @PostMapping("/evaluate")
    public ResponseEntity<PolicyEvaluationResultDto> evaluate(@Valid @RequestBody PolicyEvaluationInputDto input) {
        return policyEvaluationService.evaluate(input)
                .map(ResponseEntity::ok)
                .orElseThrow(NoActivePolicyException::new);
    }
}