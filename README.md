# Henshin Application

This repository contains utilities for working with [Eclipse Henshin](https://www.eclipse.org/henshin/) transformation rules. It includes tools for translating rules to Cypher, performing conflict and dependency analysis and interacting with OpenAI's ChatGPT to reason about the generated queries.

## Project Layout

- **HenshinApplication/src** – Java sources for the main functionality.
- **HenshinApplication/src/Henshin** – logic for rule translation, conflict detection and dependency detection.
- **HenshinApplication/src/API_ChatGPT** – helper classes for communicating with ChatGPT. Conversation logs are written to `src/API_ChatGPT/GPT_Results/`.
- **HenshinApplication/logs** – runtime log files. Execution times are stored under `logs/time/` and analysis results under `logs/results/`.
- **bank/** – example Henshin models used by the analyses.
- Provided JARs (`org.eclipse.emf.*.jar`) contain the required Eclipse Henshin and EMF libraries.

## Prerequisites

- **JDK 8 or higher** – required to compile and run the Java sources.
- **Python 3** – needed only when using the ChatGPT integration.
  - Install the `openai` package: `pip install openai`.

## Building

Compile the sources from the repository root:

```bash
javac -d HenshinApplication/bin \
  -cp "org.eclipse.emf.common-2.27.0.jar:org.eclipse.emf.ecore-2.29.0.jar:org.eclipse.emf.ecore.xmi-2.17.0.jar:org.eclipse.emf.henshin.model_1.8.0.202302121604.jar:org.eclipse.emf.henshin.interpreter_1.8.0.202302121604.jar:org.eclipse.emf.henshin.multicda.cda_1.8.0.202206300647.jar" \
  HenshinApplication/src/Henshin/*.java HenshinApplication/src/API_ChatGPT/*.java
```

## Running

The main classes can be executed using the compiled binaries and jars on the classpath. The Henshin examples in the `bank/` folder are used automatically.

Run the Cypher generation and ChatGPT interaction:

```bash
java -cp "HenshinApplication/bin:org.eclipse.emf.common-2.27.0.jar:org.eclipse.emf.ecore-2.29.0.jar:org.eclipse.emf.ecore.xmi-2.17.0.jar:org.eclipse.emf.henshin.model_1.8.0.202302121604.jar:org.eclipse.emf.henshin.interpreter_1.8.0.202302121604.jar:org.eclipse.emf.henshin.multicda.cda_1.8.0.202206300647.jar" Henshin.MainClass
```

Run conflict detection:

```bash
java -cp "HenshinApplication/bin:org.eclipse.emf.common-2.27.0.jar:org.eclipse.emf.ecore-2.29.0.jar:org.eclipse.emf.ecore.xmi-2.17.0.jar:org.eclipse.emf.henshin.model_1.8.0.202302121604.jar:org.eclipse.emf.henshin.interpreter_1.8.0.202302121604.jar:org.eclipse.emf.henshin.multicda.cda_1.8.0.202206300647.jar" Henshin.HenshinConflictDetection
```

Run dependency detection:

```bash
java -cp "HenshinApplication/bin:org.eclipse.emf.common-2.27.0.jar:org.eclipse.emf.ecore-2.29.0.jar:org.eclipse.emf.ecore.xmi-2.17.0.jar:org.eclipse.emf.henshin.model_1.8.0.202302121604.jar:org.eclipse.emf.henshin.interpreter_1.8.0.202302121604.jar:org.eclipse.emf.henshin.multicda.cda_1.8.0.202206300647.jar" Henshin.HenshinDependencyDetection
```

Each run creates log files under `HenshinApplication/logs/` with a timestamped filename. Results produced by ChatGPT are stored in `src/API_ChatGPT/GPT_Results/`.
