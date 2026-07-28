package ir.ebb.wallet.serialization;

/**
 * Raised by the wallet {@link FastJsonSerializer} for any failure to (de)serialize a wallet
 * protocol message: an unknown manifest, an unregistered class, a corrupted/invalid JSON
 * payload, or a failed manifest migration. A {@code RuntimeException} so it can propagate out
 * of Pekko's serialization extension without forcing checked-exception handling at every call
 * site (Pekko treats a thrown serializer as a serialization failure).
 */
public final class SerializationException extends RuntimeException {

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
