# Contributing

Thanks for helping improve this project! This guide explains the local dev setup, the formatting/licensing workflow, testing, and how to propose changes.

---

## TL;DR (Fast path)

1. **Install prerequisites:** JDK 17+, Maven 3.8+, Git, Make, Bash.
2. **Clone** the repo and create a branch: `git checkout -b my-fix`.
3. **Install the repo’s pre-commit hook:** `make hooks` (one-time).
4. **Commit normally.** The pre-commit hook will auto-format Java files and **add license headers**, restage the changes, and let the commit proceed.
5. **Before pushing:** `make check` to run Spotless + Checkstyle locally.
6. **Open a PR** with a clear title and description.

---

## Project Conventions

### Java, Maven, and modules

* This is a **multi-module Maven** project.
* **Java 17** is the baseline.
* Build with `mvn -B verify` (CI runs with `-DskipTests` selectively when needed).

### Formatting and License Headers

We use **Spotless** (Maven) to:

* Format Java with **google-java-format**.
* Insert/normalize **license headers** from `config/license-header.txt`.

> **Do not** hand-format files or hand-edit headers; let Spotless do it. The pre-commit hook runs `spotless:apply` for staged Java files.

Spotless is configured in the **parent POM** and resolves the header file using:

```
${maven.multiModuleProjectDirectory}/config/license-header.txt
```

so children modules pick up the root header correctly.

### Checkstyle

We keep **Checkstyle** to enforce additional rules. CI fails if formatting or style checks fail.

---

## Developer Setup

### Prerequisites

* **JDK 17+**
* **Maven 3.8+**
* **Git**, **Make**, **Bash**
* Optional IDE plugins:

    * IntelliJ: *Google Java Format* plugin (for local editing experience). Spotless remains the source of truth.

### Install the Git Hooks (one-time)

```bash
make hooks
```

This calls `scripts/install-git-hooks.sh` which sets `core.hooksPath` to `.githooks` and marks hooks executable.

### Pre-commit Hook Behavior

* Runs only on **staged** `*.java` files.
* Temporarily stashes unstaged edits (to avoid committing them), runs Spotless, **re-stages** formatted files, then restores the stash.
* If it reformats something, your commit still proceeds with the updated index.

If something goes wrong:

* Run `make format` to apply formatting to the whole repo.
* Stage changes and commit again.

---

## Make Targets

Common tasks are wrapped in a Makefile:

```text
make hooks   # Install/enable repo-local git hooks
make format  # Spotless apply (format + license headers)
make check   # Spotless check + Checkstyle check
make verify  # mvn verify (full build)
make ci      # CI checks (Spotless + Checkstyle, no tests)
make clean   # mvn clean
```

> If `make` complains about a “missing separator”, ensure each command line under a target starts with a **TAB**.

---

## Testing

* **Unit tests:** `mvn -q test` or `mvn verify`.
* **Integration tests (if defined):** Use the dedicated Maven profile exposed by the module, e.g. `-Pintegration-tests`.
* Prefer fast, deterministic tests. Avoid time-sensitive sleeps; use time abstractions or awaitility-style utilities.

### Test Guidelines

* Use clear, behavior-driven names: `methodName_shouldDoX_whenY`.
* Keep one logical assertion per test (or one behavior per test).
* Mock only external dependencies; don’t over-mock domain logic.
* Make tests independent; no ordering assumptions.

---

## Commit & PR Guidelines

### Branching

* Use short, descriptive names: `fix/formatter`, `feat/http-call`, `docs/contributing`.

### Commit messages

* Keep messages clear and imperative: `Fix header resolution for child modules`.
* Conventional Commits are welcome (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`). Example: `feat: add A2A call task support`.

### Pull Requests

* Describe the **problem**, the **solution**, and any **trade-offs**.
* Include before/after snippets or screenshots when relevant.
* Link related issues.
* Check the box:

    * [ ] `make check` passes locally
    * [ ] Unit/integration tests updated
    * [ ] Public API changes documented/Javadoc updated
    * [ ] No unrelated formatting churn (Spotless should keep this minimal)

---

## Code Style & Design Notes

* **Immutability first:** prefer `final` fields and defensive copies.
* **Null-safety:** use `Objects.requireNonNull` at boundaries; consider `Optional` for truly optional returns (don’t use it for fields/params).
* **Exceptions:** throw specific exceptions; include actionable messages; don’t swallow errors.
* **Logging:** use SLF4J; no `System.out.println`; keep logs structured and at appropriate levels.
* **APIs:** document with Javadoc; avoid breaking changes to public APIs unless necessary and called out in release notes.
* **Generics & type-safety:** prefer precise types over `Object`; isolate unchecked casts.
* **Performance:** avoid premature optimization; measure first; add micro-benchmarks (JMH) when optimizing hot paths.

---

## License Headers

* Header template lives at: `config/license-header.txt` (root).
* Spotless inserts or normalizes it automatically.
* If the copyright line needs updates, edit the template and run `make format`.

---

## CI

* CI runs `spotless:check` and `checkstyle:check` to ensure consistent formatting and style.
* Builds fail if formatting or headers drift. Run `make format` locally to fix.

---

## Troubleshooting

* **Hook didn’t run?** Did you run `make hooks` after cloning? Check `git config --get core.hooksPath` prints `.githooks`.
* **Header file not found?** Ensure Spotless uses the root path via `${maven.multiModuleProjectDirectory}/config/license-header.txt`.
* **Formatting conflicts with unstaged edits?** The hook stashes them. If a conflict occurs, complete the merge in your working copy, then `git add` and commit again.

---

## Questions

Open a discussion or issue on the repository. Thanks for contributing! 🎉
