package ir.ebb.wallet.app.web;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import ir.ebb.wallet.app.bridge.grpc.WalletGrpcServiceImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * Plain grpc-java server on :9090 (replaces net.devh's spring-boot starter).
 * Wraps the {@code WalletService} with the bridge auth interceptor.
 */
@Slf4j
public class GrpcServer {

    private final int port;
    private final WalletGrpcServiceImpl walletService;
    private final BridgeGrpcAuthInterceptor authInterceptor;
    private Server server;

    public GrpcServer(int port, WalletGrpcServiceImpl walletService, BridgeGrpcAuthInterceptor authInterceptor) {
        this.port = port;
        this.walletService = walletService;
        this.authInterceptor = authInterceptor;
    }

    public void start() throws Exception {
        server = ServerBuilder.forPort(port)
                .addService(ServerInterceptors.intercept(walletService, authInterceptor))
                .build()
                .start();
        log.info("gRPC server started on port {}", port);
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }
}
