package com.magic76.crew.agent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Provider-neutral routing from a model responsibility to an ordered provider chain.
 *
 * The router only selects/creates sessions. Product prompts, retry policy and runtime
 * fallback after a started session fails remain outside this class.
 */
public final class ModelRoleRouter {
    public static final class Selection {
        private final ModelRole role;
        private final String providerId;
        private final ModelSession session;

        Selection(ModelRole role, String providerId, ModelSession session) {
            this.role = role;
            this.providerId = providerId == null ? "" : providerId;
            this.session = session;
        }

        public ModelRole role() { return role; }
        public String providerId() { return providerId; }
        public ModelSession session() { return session; }
    }

    private final Map<ModelRole, List<ModelSessionProvider>> routes =
            new EnumMap<ModelRole, List<ModelSessionProvider>>(ModelRole.class);

    public synchronized ModelRoleRouter registerPrimary(
            ModelRole role,
            ModelSessionProvider provider) {
        requireRole(role);
        requireProvider(provider);
        ArrayList<ModelSessionProvider> chain = new ArrayList<ModelSessionProvider>();
        chain.add(provider);
        List<ModelSessionProvider> previous = routes.get(role);
        if (previous != null && previous.size() > 1) {
            chain.addAll(previous.subList(1, previous.size()));
        }
        routes.put(role, chain);
        return this;
    }

    public synchronized ModelRoleRouter addFallback(
            ModelRole role,
            ModelSessionProvider provider) {
        requireRole(role);
        requireProvider(provider);
        List<ModelSessionProvider> chain = routes.get(role);
        if (chain == null) {
            chain = new ArrayList<ModelSessionProvider>();
            routes.put(role, chain);
        }
        chain.add(provider);
        return this;
    }

    public synchronized boolean hasRoute(ModelRole role) {
        List<ModelSessionProvider> chain = routes.get(role);
        return chain != null && !chain.isEmpty();
    }

    /**
     * Creates a session from the first provider that succeeds. Creation failures are
     * allowed to fall through to the next provider; failures after session start are
     * intentionally left to product/runtime policy.
     */
    public Selection open(ModelRole role) {
        requireRole(role);
        final List<ModelSessionProvider> chain;
        synchronized (this) {
            List<ModelSessionProvider> configured = routes.get(role);
            if (configured == null || configured.isEmpty()) {
                throw new IllegalStateException("No model route configured for role " + role);
            }
            chain = new ArrayList<ModelSessionProvider>(configured);
        }

        RuntimeException lastFailure = null;
        for (ModelSessionProvider provider : chain) {
            try {
                ModelSession session = provider.createSession();
                if (session != null) {
                    return new Selection(role, safeProviderId(provider), session);
                }
            } catch (RuntimeException failure) {
                lastFailure = failure;
            }
        }

        IllegalStateException exhausted = new IllegalStateException(
                "No model provider could create a session for role " + role);
        if (lastFailure != null) exhausted.initCause(lastFailure);
        throw exhausted;
    }

    private static void requireRole(ModelRole role) {
        if (role == null) throw new IllegalArgumentException("role == null");
    }

    private static void requireProvider(ModelSessionProvider provider) {
        if (provider == null) throw new IllegalArgumentException("provider == null");
    }

    private static String safeProviderId(ModelSessionProvider provider) {
        try {
            String id = provider.id();
            return id == null ? "" : id.trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }
}
