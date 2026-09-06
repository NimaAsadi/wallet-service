package ir.ebb.wallet.app.web;

import org.apache.pekko.http.javadsl.model.HttpRequest;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.Route;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.testkit.JUnitRouteTest;
import org.apache.pekko.http.javadsl.testkit.TestRoute;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseControllerTest extends JUnitRouteTest {

    /**
     * {@link JUnitRouteTest} wires its {@code ActorSystem} through a JUnit&nbsp;4 rule
     * ({@code ExternalResource}), which the Jupiter engine never invokes — so the
     * resource lifecycle is driven explicitly here.
     */
    @BeforeEach
    void startActorSystem() {
        systemResource().before();
    }

    @AfterEach
    void stopActorSystem() {
        systemResource().after();
    }

    /** Minimal concrete controller — proves the abstract contract is implementable. */
    private static final class PingController extends BaseController {
        @Override
        public Route getRoute() {
            return path("ping", () -> complete(StatusCodes.OK, "pong"));
        }
    }

    @Test
    void concreteSubclassServesItsRoute() {
        TestRoute route = testRoute(new PingController().getRoute());
        route.run(HttpRequest.GET("/ping")).assertStatusCode(StatusCodes.OK);
    }

    @Test
    void baseClassIsAbstractAllDirectives() {
        assertThat(java.lang.reflect.Modifier.ABSTRACT)
                .isEqualTo(BaseController.class.getModifiers() & java.lang.reflect.Modifier.ABSTRACT);
        assertThat(AllDirectives.class)
                .isAssignableFrom(BaseController.class);
    }
}
