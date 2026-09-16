package com.magic76.crew.agent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class ModelRoleRouterTest {
    @Test
    public void routesEachRoleIndependently() {
        FakeSession realtime = new FakeSession();
        FakeSession reasoning = new FakeSession();
        ModelRoleRouter router = new ModelRoleRouter()
                .registerPrimary(ModelRole.REALTIME_CONVERSATION,
                        provider("gemini-live", realtime))
                .registerPrimary(ModelRole.REASONING,
                        provider("reasoner", reasoning));

        ModelRoleRouter.Selection realtimeSelection =
                router.open(ModelRole.REALTIME_CONVERSATION);
        ModelRoleRouter.Selection reasoningSelection =
                router.open(ModelRole.REASONING);

        assertEquals("gemini-live", realtimeSelection.providerId());
        assertEquals(ModelRole.REALTIME_CONVERSATION, realtimeSelection.role());
        assertSame(realtime, realtimeSelection.session());
        assertEquals("reasoner", reasoningSelection.providerId());
        assertSame(reasoning, reasoningSelection.session());
    }

    @Test
    public void fallsBackWhenPrimaryCannotCreateSession() {
        final FakeSession fallback = new FakeSession();
        ModelRoleRouter router = new ModelRoleRouter()
                .registerPrimary(ModelRole.SELF_IMPROVEMENT,
                        new ModelSessionProvider() {
                            @Override public String id() { return "primary"; }
                            @Override public ModelSession createSession() {
                                throw new IllegalStateException("unavailable");
                            }
                        })
                .addFallback(ModelRole.SELF_IMPROVEMENT,
                        provider("fallback", fallback));

        ModelRoleRouter.Selection selection = router.open(ModelRole.SELF_IMPROVEMENT);

        assertEquals("fallback", selection.providerId());
        assertSame(fallback, selection.session());
    }

    @Test
    public void fallsBackWhenPrimaryReturnsNull() {
        final FakeSession fallback = new FakeSession();
        ModelRoleRouter router = new ModelRoleRouter()
                .registerPrimary(ModelRole.GENERATION,
                        new ModelSessionProvider() {
                            @Override public String id() { return "empty"; }
                            @Override public ModelSession createSession() { return null; }
                        })
                .addFallback(ModelRole.GENERATION, provider("story", fallback));

        assertSame(fallback, router.open(ModelRole.GENERATION).session());
    }

    @Test(expected = IllegalStateException.class)
    public void missingRoleFailsClearly() {
        new ModelRoleRouter().open(ModelRole.REASONING);
    }

    @Test
    public void hasRouteReflectsConfiguration() {
        ModelRoleRouter router = new ModelRoleRouter();
        assertTrue(!router.hasRoute(ModelRole.REASONING));
        router.registerPrimary(ModelRole.REASONING, provider("reasoner", new FakeSession()));
        assertTrue(router.hasRoute(ModelRole.REASONING));
    }

    private static ModelSessionProvider provider(final String id, final ModelSession session) {
        return new ModelSessionProvider() {
            @Override public String id() { return id; }
            @Override public ModelSession createSession() { return session; }
        };
    }

    private static final class FakeSession implements ModelSession {
        @Override public void start(SessionConfig config, Listener listener) {}
        @Override public void sendUserText(String text) {}
        @Override public void sendUserAudio(byte[] pcmOrEncodedAudio) {}
        @Override public void sendToolResult(ToolResult result) {}
        @Override public void interrupt() {}
        @Override public void close() {}
    }
}
