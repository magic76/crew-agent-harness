package com.magic76.crew.agent;

/** Creates provider-specific ModelSession instances without exposing provider details to agent-core. */
public interface ModelSessionProvider {
    String id();
    ModelSession createSession();
}
