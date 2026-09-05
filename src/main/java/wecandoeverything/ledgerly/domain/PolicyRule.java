package wecandoeverything.ledgerly.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import wecandoeverything.ledgerly.domain.converter.RuleConditionsConverter;
import wecandoeverything.ledgerly.domain.converter.StringListConverter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "policy_rule")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PolicyRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruleset_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private PolicyRuleset ruleset;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_category", nullable = false)
    private ExpenseCategory expenseCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleScope scope;

    @Column(name = "limit_amount", precision = 15, scale = 2)
    private BigDecimal limitAmount; // null = no amount limit (prohibition/evidence-only rule)

    @Convert(converter = RuleConditionsConverter.class)
    @Column(columnDefinition = "TEXT")
    private RuleConditions conditions;

    @Convert(converter = StringListConverter.class)
    @Column(name = "required_evidence", columnDefinition = "TEXT")
    private List<String> requiredEvidence;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> prohibitions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleSeverity severity;

    @Column(length = 500)
    private String note;

    @Column(name = "clause_article", length = 64)
    private String clauseArticle;

    @Column(name = "clause_text", columnDefinition = "TEXT")
    private String clauseText;

    @Column(name = "clause_page")
    private Integer clausePage;

    private Double confidence;
}