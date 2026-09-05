package wecandoeverything.ledgerly.exception;

public class PolicyRulesetNoRulesException extends RuntimeException {
    public PolicyRulesetNoRulesException(String id) {
        super("Cannot activate ruleset " + id + " — it has no rules");
    }
}