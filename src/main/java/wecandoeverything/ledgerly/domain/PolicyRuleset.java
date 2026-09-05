package wecandoeverything.ledgerly.domain;

import jakarta.persistence.*;
import lombok.*;
import wecandoeverything.ledgerly.domain.converter.StringListConverter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "policy_ruleset", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "version"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PolicyRuleset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "company_id", nullable = false)
    private Long companyId; // constant 1L until real multi-tenancy exists

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyRulesetStatus status;

    @Column(name = "source_file_name")
    private String sourceFileName;

    @Column(name = "source_file_hash", length = 64)
    private String sourceFileHash;

    @Column(name = "page_count")
    private Integer pageCount;

    @Convert(converter = StringListConverter.class)
    @Column(name = "unmapped_clauses", columnDefinition = "TEXT")
    private List<String> unmappedClauses;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = PolicyRulesetStatus.DRAFT;
    }
}