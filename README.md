# Crew Agent Harness

Shared provider-neutral agent runtime for Crew apps.

This repository hosts reusable orchestration contracts used by Crew Helper, Crew Teacher, Crew Story, Crew Mate, and other Crew agents. Product-specific prompts, state, permissions, approval policy, and tool implementations stay in each product.

## Runtime boundary

`user input -> ModelSession -> ModelEvent -> AgentHarness -> ToolRegistry -> ToolResult -> ModelSession`

## Modules

- `agent-core`: pure Java agent loop, events, model/session contracts, tool contracts, model-role routing, and tests.
- provider adapters such as Gemini Live stay provider-specific and can live outside the core.

Real-time voice can remain on Gemini Live while reasoning, generation, and self-improvement use separate model roles. The harness does not force one model for every role.

## Model roles

`agent-core` defines four shared responsibilities:

```text
REALTIME_CONVERSATION
REASONING
GENERATION
SELF_IMPROVEMENT
```

`ModelRoleRouter` maps each role to a primary `ModelSessionProvider` plus optional fallbacks. It only selects and creates sessions. Product prompts, retry policy, state, and fallback after an already-started session fails remain product/runtime concerns.

Example:

```java
ModelRoleRouter router = new ModelRoleRouter()
        .registerPrimary(ModelRole.REALTIME_CONVERSATION, geminiLiveProvider)
        .registerPrimary(ModelRole.REASONING, reasoningProvider)
        .registerPrimary(ModelRole.GENERATION, storyProvider)
        .registerPrimary(ModelRole.SELF_IMPROVEMENT, improvementProvider)
        .addFallback(ModelRole.SELF_IMPROVEMENT, improvementFallbackProvider);

ModelRoleRouter.Selection selection = router.open(ModelRole.REASONING);
ModelSession session = selection.session();
```

## JitPack

Release consumers use the repository-level JitPack artifact:

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.magic76:crew-agent-harness:v0.1.3'
}
```

For local Maven publishing, the module coordinate is `com.magic76.crew:agent-core:0.1.3-SNAPSHOT`.
