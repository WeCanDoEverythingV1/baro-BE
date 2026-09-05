package wecandoeverything.ledgerly.exception;

public class UnsupportedFileTypeException extends RuntimeException {
    public UnsupportedFileTypeException(String contentType) {
        super("Unsupported file type: " + contentType + ". Only PDF is accepted.");
    }
}