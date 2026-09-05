package wecandoeverything.ledgerly.exception;

public class PolicyRulesetActiveException extends RuntimeException {
    public PolicyRulesetActiveException(String id) {
        super("Cannot delete ruleset " + id + " — it is currently ACTIVE. Activate a different ruleset first.");
    }
}