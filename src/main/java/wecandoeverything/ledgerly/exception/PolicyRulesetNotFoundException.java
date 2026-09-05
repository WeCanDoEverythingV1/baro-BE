package wecandoeverything.ledgerly.exception;

public class PolicyRulesetNotFoundException extends RuntimeException {
    public PolicyRulesetNotFoundException(String id) {
        super("Policy ruleset not found with id: " + id);
    }
}