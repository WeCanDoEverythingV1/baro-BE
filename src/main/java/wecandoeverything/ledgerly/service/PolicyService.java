package wecandoeverything.ledgerly.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import wecandoeverything.ledgerly.domain.*;
import wecandoeverything.ledgerly.dto.PolicyExtractionResult;
import wecandoeverything.ledgerly.dto.PolicyRuleDto;
import wecandoeverything.ledgerly.dto.PolicyRulesetDto;
import wecandoeverything.ledgerly.exception.*;
import wecandoeverything.ledgerly.repository.ApprovalRequestRepository;
import wecandoeverything.ledgerly.repository.PolicyRuleRepository;
import wecandoeverything.ledgerly.repository.PolicyRulesetRepository;

import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {

    private static final Long COMPANY_ID = 1L; // constant until real multi-tenancy exists

    private final PolicyRulesetRepository rulesetRepository;
    private final PolicyRuleRepository ruleRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final PolicyExtractionService policyExtractionService;

    @Transactional
    public PolicyRulesetDto create(MultipartFile file, boolean force) {
        if (!"application/pdf".equals(file.getContentType())) {
            throw new UnsupportedFileTypeException(file.getContentType());
        }

        String hash = sha256(file);
        if (!force) {
            Optional<PolicyRuleset> existing = rulesetRepository.findBySourceFileHash(hash);
            if (existing.isPresent()) {
                return toDto(existing.get(), ruleRepository.findByRulesetId(existing.get().getId()));
            }
        }

        PolicyExtractionResult extraction = policyExtractionService.extract(file);

        int nextVersion = rulesetRepository.findAllByOrderByVersionDesc().stream()
                .findFirst().map(r -> r.getVersion() + 1).orElse(1);

        PolicyRuleset ruleset = rulesetRepository.save(PolicyRuleset.builder()
                .companyId(COMPANY_ID)
                .version(nextVersion)
                .status(PolicyRulesetStatus.DRAFT)
                .sourceFileName(file.getOriginalFilename())
                .sourceFileHash(hash)
                .pageCount(extraction.pageCount())
                .unmappedClauses(extraction.unmappedClauses())
                .build());

        List<PolicyRule> rules = extraction.rules().stream()
                .map(r -> r.toBuilder().ruleset(ruleset).build())
                .toList();
        ruleRepository.saveAll(rules);

        return toDto(ruleset, rules);
    }

    public List<PolicyRulesetDto> getAll() {
        return rulesetRepository.findAllByOrderByVersionDesc().stream()
                .map(r -> toDto(r, ruleRepository.findByRulesetId(r.getId())))
                .toList();
    }

    public Optional<PolicyRulesetDto> getActive() {
        return rulesetRepository.findByStatus(PolicyRulesetStatus.ACTIVE)
                .map(r -> toDto(r, ruleRepository.findByRulesetId(r.getId())));
    }

    @Transactional
    public PolicyRulesetDto updateRules(String id, List<PolicyRuleDto> ruleDtos) {
        PolicyRuleset ruleset = rulesetRepository.findById(id)
                .orElseThrow(() -> new PolicyRulesetNotFoundException(id));

        if (ruleset.getStatus() == PolicyRulesetStatus.ARCHIVED) {
            throw new PolicyRulesetArchivedException(id);
        }

        ruleRepository.deleteByRulesetId(id); // full replace — matches PUT semantics
        List<PolicyRule> newRules = ruleDtos.stream()
                .map(dto -> toEntity(dto, ruleset))
                .toList();
        ruleRepository.saveAll(newRules);

        return toDto(ruleset, newRules);
    }

    @Transactional
    public PolicyRulesetDto activate(String id) {
        PolicyRuleset ruleset = rulesetRepository.findById(id)
                .orElseThrow(() -> new PolicyRulesetNotFoundException(id));

        List<PolicyRule> rules = ruleRepository.findByRulesetId(id);
        if (rules.isEmpty()) {
            throw new PolicyRulesetNoRulesException(id);
        }

        rulesetRepository.findByStatus(PolicyRulesetStatus.ACTIVE).ifPresent(current -> {
            current.setStatus(PolicyRulesetStatus.ARCHIVED);
            rulesetRepository.save(current);
        });

        ruleset.setStatus(PolicyRulesetStatus.ACTIVE);
        ruleset.setActivatedAt(java.time.LocalDateTime.now());
        rulesetRepository.save(ruleset);

        return toDto(ruleset, rules);
    }

    @Transactional
    public void delete(String id) {
        PolicyRuleset ruleset = rulesetRepository.findById(id)
                .orElseThrow(() -> new PolicyRulesetNotFoundException(id));

        if (ruleset.getStatus() == PolicyRulesetStatus.ACTIVE) {
            throw new PolicyRulesetActiveException(id);
        }

        long referencingCount = approvalRequestRepository.countByRulesetVersion(ruleset.getVersion());
        if (referencingCount > 0) {
            throw new PolicyRulesetInUseException(id, referencingCount);
        }

        ruleRepository.deleteByRulesetId(id);
        rulesetRepository.delete(ruleset);
    }

    private PolicyRule toEntity(PolicyRuleDto dto, PolicyRuleset ruleset) {
        return PolicyRule.builder()
                .ruleset(ruleset)
                .expenseCategory(dto.getExpenseCategory())
                .scope(dto.getScope())
                .limitAmount(dto.getLimitAmount())
                .conditions(dto.getConditions())
                .requiredEvidence(dto.getRequiredEvidence())
                .prohibitions(dto.getProhibitions())
                .severity(dto.getSeverity())
                .note(dto.getNote())
                .clauseArticle(dto.getClauseArticle())
                .clauseText(dto.getClauseText())
                .clausePage(dto.getClausePage())
                .confidence(dto.getConfidence())
                .build();
    }

    private PolicyRulesetDto toDto(PolicyRuleset ruleset, List<PolicyRule> rules) {
        return PolicyRulesetDto.builder()
                .id(ruleset.getId())
                .companyId(ruleset.getCompanyId())
                .version(ruleset.getVersion())
                .status(ruleset.getStatus())
                .sourceFileName(ruleset.getSourceFileName())
                .sourceFileHash(ruleset.getSourceFileHash())
                .pageCount(ruleset.getPageCount())
                .unmappedClauses(ruleset.getUnmappedClauses())
                .createdAt(ruleset.getCreatedAt())
                .activatedAt(ruleset.getActivatedAt())
                .rules(rules.stream().map(this::toRuleDto).toList())
                .build();
    }

    private PolicyRuleDto toRuleDto(PolicyRule r) {
        return new PolicyRuleDto(r.getId(), r.getExpenseCategory(), r.getScope(), r.getLimitAmount(),
                r.getConditions(), r.getRequiredEvidence(), r.getProhibitions(), r.getSeverity(),
                r.getNote(), r.getClauseArticle(), r.getClauseText(), r.getClausePage(), r.getConfidence());
    }

    private String sha256(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash uploaded file", e);
        }
    }
}