# Makefile
# NOTE: each command line below begins with a literal TAB character.

MVN ?= mvn
MVN_FLAGS ?= -q -DskipTests

.PHONY: help hooks format check verify ci status clean

help:
	@echo ""
	@echo "Targets:"
	@echo "  make hooks    - Install/enable repo-local git hooks"
	@echo "  make format   - Spotless apply (format + license headers)"
	@echo "  make check    - Spotless check + Checkstyle"
	@echo "  make verify   - mvn verify"
	@echo "  make ci       - CI checks (Spotless + Checkstyle, no tests)"
	@echo "  make status   - Show git hooksPath"
	@echo "  make clean    - mvn clean"
	@echo ""

hooks:
	@bash scripts/install-git-hooks.sh
	@echo "✅ Git hooks ready."

format:
	@echo "✨ Formatting (Spotless apply + headers)…"
	@$(MVN) $(MVN_FLAGS) spotless:apply

check:
	@echo "🔍 Checking format + headers + checkstyle…"
	@$(MVN) $(MVN_FLAGS) spotless:check checkstyle:check

verify:
	@echo "🧪 Running mvn verify…"
	@$(MVN) -B verify

ci:
	@echo "🏗️ CI checks (no tests)…"
	@$(MVN) -B -DskipTests spotless:check checkstyle:check

status:
	@echo -n "hooksPath: "
	@git config --get core.hooksPath || echo "(not set)"

clean:
	@echo "🧹 Cleaning…"
	@$(MVN) -q clean
