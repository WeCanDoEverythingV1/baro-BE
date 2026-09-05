package wecandoeverything.ledgerly.exception;

public class NoActivePolicyException extends RuntimeException {
    public NoActivePolicyException() { super("No active policy ruleset exists"); }
}