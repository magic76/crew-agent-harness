# Crew Agent Harness

Shared provider-neutral agent runtime for Crew apps.

This repository hosts reusable orchestration contracts used by Crew Helper, Crew Teacher, Crew Story, and Crew Mate. Product-specific prompts, state, permissions, approval policy, and tool implementations stay in each product.

## Runtime boundary

`user input -> ModelSession -> ModelEvent -> AgentHarness -> ToolRegistry -> ToolResult -> ModelSession`

## Modules

- `agent-core`: pure Java agent loop, events, model/session contracts, tool contracts, and tests.
- provider adapters such as Gemini Live will be added separately after the core API is stable.

Real-time voice can remain on Gemini Live while reasoning/self-improvement use separate model roles. The harness does not force one model for every role.

## JitPack

Release consumers can use the published `agent-core` module through JitPack.

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.magic76.crew-agent-harness:agent-core:v0.1.1'
}
```

The module keeps `com.magic76.crew:agent-core:0.1.0-SNAPSHOT` for local Maven publishing and switches to JitPack's repository/tag coordinates only inside a JitPack build.
