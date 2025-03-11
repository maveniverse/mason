package eu.maveniverse.maven.mason.hocon;

public class HoconParseException extends RuntimeException {
    public HoconParseException(String message) {
        super(message);
    }

    public HoconParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
