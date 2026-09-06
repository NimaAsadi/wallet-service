package ir.ebb.wallet.app.web;

import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.Route;

/**
 * Base class for HTTP controllers. Subclasses own one route subtree and
 * expose it via {@link #getRoute()}; the composition root
 * ({@code WalletHttpServer}) concatenates them under their audience
 * path prefixes. Extending {@link AllDirectives} keeps all routing
 * directives available to subclass code.
 */
public abstract class BaseController extends AllDirectives {

    /** @return the route subtree served by this controller. */
    public abstract Route getRoute();
}
