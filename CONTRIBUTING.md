# Contributing

Thanks for helping improve this project! This guide explains the local dev setup, the formatting/licensing workflow, testing, and how to propose changes.

---

## TL;DR (Fast path)

1. **Install prerequisites:** JDK 17+, Maven 3.8+, Git.
2. **Clone** the repo and create a branch: `git checkout -b my-fix`.
3. **Build locally:** run `mvn clean install` from the repo root. Spotless will **apply formatting and license headers** during the build.
4. **Before pushing:** run `mvn -DskipTests spotless:check checkstyle:check`.
5. **Open a PR** with a clear title and description.

---

## Project Conventions

### Java, Maven, and modules

* This is a **multi-module Maven** project.
* **Java 17** is the baseline.
* Build with `mvn -B verify` (CI runs with `-DskipTests` selectively when needed).

### Formatting and License Headers

We use **Spotless** (Maven) to:

* Format Java with **google-java-format**.
* Insert/normalize **license headers** (the header content is defined **inline** in the parent POM’s `<licenseHeader>` configuration).

> **Do not** hand-format files or hand-edit headers; let Spotless do it. Running `mvn clean install` locally will mutate sources to match the standard.

### Checkstyle

We keep **Checkstyle** to enforce additional rules. CI fails if formatting or style checks fail.

---

## Developer Setup

### Prerequisites

* **JDK 17+**
* **Maven 3.8+**
* **Git**
* Optional IDE plugins:

  * IntelliJ: *Google Java Format* plugin (for local editing experience). Spotless remains the source of truth.

### Local build & formatting

* Run `mvn clean install` from the repo root. During the build, Spotless **applies** formatting and license headers.
* Before pushing, run `mvn -DskipTests spotless:check checkstyle:check` to ensure CI will pass.

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

  * [ ] `mvn -DskipTests spotless:check checkstyle:check` passes locally
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

* The license header is defined **inline** in the parent POM under Spotless’ `<licenseHeader>`.
* To update it, edit the parent POM and run `mvn spotless:apply` to propagate changes.

---

## CI

* CI runs `spotless:check` and `checkstyle:check` to ensure consistent formatting and style.
* Builds fail if formatting or headers drift. Run `mvn spotless:apply` locally to fix.

---

## Troubleshooting

* **Spotless changed files during build?** That’s expected locally. Review `git status`, then stage and commit the updates.
* **CI red on formatting?** Run `mvn spotless:apply` locally, commit, and push.
* **Running only a submodule?** Prefer `mvn -pl <module> -am …` from the repo root so parent config (Spotless/Checkstyle) is applied consistently.

---

## Questions

Open a discussion or issue on the repository. Thanks for contributing! 🎉
