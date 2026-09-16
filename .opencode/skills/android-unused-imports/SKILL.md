---

name: android-unused-imports
description: Keep Kotlin and Java imports clean across an Android project. Remove unused imports, detect import-related cleanup opportunities, and verify that no unused imports remain after changes.
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

# Android Unused Imports

Maintain a clean import section across the entire Android project.

## Goal

Ensure that Kotlin and Java source files contain no unused imports.

This applies to:

* Kotlin `.kt` files
* Kotlin script files `.kts` when they contain source imports
* Java `.java` files

Search the entire Android project, not only the files currently being modified, when explicitly asked to clean unused imports globally.

## Rules

### 1. Remove unused imports

For every modified Kotlin or Java source file:

* Remove imports that are not referenced.
* Do not leave commented-out imports.
* Do not replace explicit imports with wildcard imports as a cleanup technique.
* Preserve imports that are required indirectly by language/framework syntax.
* Do not remove imports merely because their usage is visually non-obvious.

Examples of imports that may look unused but must be preserved when required:

* Kotlin extension functions
* Compose APIs
* annotations
* generated APIs
* Java/Kotlin interop
* operator functions
* type aliases
* DSL-specific imports

When uncertain, verify by compiling or running the appropriate static analysis rather than guessing.

### 2. Prefer IDE/compiler-supported cleanup

For Kotlin, prefer the project's existing formatter/import optimizer when available.

Possible mechanisms include:

```bash
./gradlew ktlintCheck
./gradlew ktlintFormat
```

or the project's configured Detekt/Ktlint/Android lint tasks.

Do NOT introduce a new formatting or lint dependency solely for this skill.

First inspect the project to determine which tooling is already configured.

### 3. Android Studio-compatible behavior

The desired result should be equivalent to:

```text
Optimize Imports
```

in Android Studio.

Do not perform unrelated formatting changes.

An import-cleanup operation should not modify:

* code formatting unrelated to imports
* naming
* ordering of declarations
* whitespace outside import sections
* Gradle configuration
* dependencies

unless required by the project's existing formatter.

### 4. Scope

When modifying a file:

```text
Changed Kotlin/Java file
        ↓
Check imports
        ↓
Remove unused imports
        ↓
Run relevant verification
```

When explicitly asked:

> clean all unused imports

then:

```text
Entire Android project
        ↓
Find Kotlin + Java sources
        ↓
Optimize imports
        ↓
Verify
        ↓
Report remaining problems
```

Do not limit the operation to the current module unless the user explicitly requests a module.

### 5. Generated code

Do not modify generated sources.

Ignore directories such as:

```text
build/
.gradle/
generated/
build/generated/
```

and other project-specific generated directories.

If a generated directory is actually tracked source code, inspect the project's configuration before modifying it.

### 6. Tests

Apply the same import-cleanup rules to:

```text
src/test/
src/androidTest/
```

Unused imports in tests should also be removed.

### 7. Verification

After cleanup, verify using the project's existing tooling.

Prefer, in order:

1. Existing Ktlint/Detekt configuration
2. Android Lint
3. Gradle compilation
4. IDE/compiler diagnostics if available

Examples:

```bash
./gradlew ktlintCheck
./gradlew detekt
./gradlew lint
```

Only run tasks that actually exist in the project.

If no dedicated import/lint task exists, compile the affected modules:

```bash
./gradlew <module>:compileDebugKotlin
```

or the appropriate project task.

### 8. Avoid false positives

Never remove an import solely based on textual search.

Kotlin imports can be required by:

* extension functions
* overloaded operators
* annotations
* delegated properties
* Compose DSLs
* infix functions
* companion/static-like APIs
* type aliases
* generic type references
* Java interop
* generated symbols

The compiler or configured static-analysis tool is authoritative.

### 9. Final verification requirement

Before considering the task complete:

* Search for remaining unused-import diagnostics.
* Run the relevant configured verification task.
* Confirm that cleanup did not introduce compilation errors.
* If verification cannot be run, explicitly report that fact.

Do not claim that all unused imports were removed unless the available verification supports that conclusion.

## Workflow

### For a normal code change

1. Inspect the changed Kotlin/Java files.
2. Identify unused imports.
3. Remove them.
4. Preserve required imports.
5. Run the narrowest relevant verification.
6. Report any unrelated pre-existing failures separately.

### For a project-wide cleanup

1. Inspect project structure.
2. Identify configured lint/format/import tools.
3. Identify Kotlin and Java source roots.
4. Exclude generated/build directories.
5. Clean unused imports.
6. Run project-wide verification.
7. Fix only import-related issues discovered by this task.
8. Report:

    * files changed
    * verification performed
    * remaining import-related issues
    * unrelated failures, if any

## Important constraint

This skill is specifically for import hygiene.

Do not turn an unused-import cleanup into a general refactoring task.

Avoid changing architecture, APIs, formatting, dependencies, or behavior unless necessary to resolve an import-related problem.
