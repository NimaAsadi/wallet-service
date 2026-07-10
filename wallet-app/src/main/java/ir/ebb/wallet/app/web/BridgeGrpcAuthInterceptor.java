package ir.ebb.wallet.app.web;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import ir.ebb.base.security.UserPrincipal;

/**
 * Replaces net.devh's {@code @GrpcGlobalServerInterceptor}. Enforces a bridge
 * Keycloak Bearer token on every gRPC call using the same {@link JwtVerifier}
 * as the REST bridge audience.
 */
public class BridgeGrpcAuthInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final JwtVerifier bridgeVerifier;

    public BridgeGrpcAuthInterceptor(JwtVerifier bridgeVerifier) {
        this.bridgeVerifier = bridgeVerifier;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String token = headers.get(AUTHORIZATION);
        UserPrincipal principal = bridgeVerifier.verify(token);
        if (principal == null) {
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid Bearer token"), headers);
            return new ServerCall.Listener<>() {};
        }
        return next.startCall(call, headers);
    }
}
