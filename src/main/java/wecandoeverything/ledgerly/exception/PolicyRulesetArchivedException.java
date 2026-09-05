package wecandoeverything.ledgerly.exception;

public class PolicyRulesetArchivedException extends RuntimeException {
    public PolicyRulesetArchivedException(String id) {
        super("Cannot modify ruleset " + id + " — it is archived");
    }
}