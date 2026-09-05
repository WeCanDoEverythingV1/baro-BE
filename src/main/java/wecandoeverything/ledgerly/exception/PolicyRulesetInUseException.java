package wecandoeverything.ledgerly.exception;

public class PolicyRulesetInUseException extends RuntimeException {
    public PolicyRulesetInUseException(String id, long referencingCount) {
        super("Cannot delete ruleset " + id + " — referenced by " + referencingCount + " approval request(s)");
    }
}