package ir.ebb.wallet.app.bridge.config;

import io.grpc.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Validates Bearer tokens on gRPC calls using the BRIDGE Keycloak realm.
 * The token must be passed as the "Authorization" metadata key.
 */
@Slf4j
@GrpcGlobalServerInterceptor
@RequiredArgsConstructor
public class BridgeGrpcAuthInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> AUTH_KEY =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final JwtDecoder bridgeJwtDecoder;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String authHeader = headers.get(AUTH_KEY);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            call.close(Status.UNAUTHENTICATED.withDescription("Missing Bearer token"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        String token = authHeader.substring(7);
        try {
            bridgeJwtDecoder.decode(token);
        } catch (JwtException e) {
            log.warn("gRPC auth failed: {}", e.getMessage());
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid token: " + e.getMessage()), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        return next.startCall(call, headers);
    }
}
