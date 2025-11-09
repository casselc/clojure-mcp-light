# Changelog

All notable changes to this project will be documented in this file.

## [0.0.3-alpha] - 2025-11-09

This version represents a major improvement in robustness and developer experience.

### Summary

* **Fixed hook response protocol** - Hooks now return `nil` for normal operations instead of explicit `permissionDecision: allow` responses. The previous approach was bypassing Claude Code's normal permission dialogs, causing the UI to not properly prompt users for confirmation.

* **Robust nREPL session persistence** - Session management now properly handles multiple concurrent Claude Code sessions running in the same directory using session-scoped temporary files with fallback strategies (env var → PPID-based → global).

* **Automatic cleanup via SessionEnd hook** - Session persistence requires temporary file storage that must be cleaned up. The new SessionEnd hook automatically removes session directories when Claude Code sessions terminate, preventing accumulation of stale temporary files.

* **cljfmt support** - The `--cljfmt` CLI option enables automatic code formatting. Claude frequently indents code incorrectly by one space, and cljfmt quickly fixes these issues. Well-formatted code is essential for parinfer's indent mode to work correctly, making this option highly recommended.

* **Debugging support** - The `--log-level` and `--log-file` CLI options provide configurable logging. Without proper logging, developing and troubleshooting clojure-mcp-light is extremely difficult.

* **Statistics tracking** - The `--stats` flag enables global tracking of delimiter events. The `scripts/stats-summary.bb` tool provides comprehensive analysis of fix rates, error patterns, and code quality metrics.

### Added
- **Statistics tracking system** - Track delimiter events to analyze LLM code quality
  - `--stats` CLI flag enables event logging to `~/.clojure-mcp-light/stats.log`
  - Event types: `:delimiter-error`, `:delimiter-fixed`, `:delimiter-fix-failed`, `:delimiter-ok`
  - Stats include timestamps, hook events, and file paths
  - `scripts/stats-summary.bb` - Comprehensive analysis tool for stats logs
  - Low-level parse error tracking and false positive filtering
  - Cljfmt efficiency tracking (already-formatted vs needed-formatting vs check-errors)

- **Unified tmp namespace** - Session-scoped temporary file management
  - Centralized temporary file paths with automatic cleanup
  - Editor session detection with fallback strategies (env var → PPID-based → global)
  - Deterministic paths based on user, hostname, session ID, and project SHA
  - Per-project isolation prevents conflicts across multiple projects
  - Functions: `session-root`, `editor-scope-id`, `cleanup-session!`, `get-possible-session-ids`

- **SessionEnd cleanup hook** - Automatic temp file cleanup
  - Removes session directories when Claude Code sessions terminate
  - Attempts cleanup for all possible session IDs (env-based and PPID-based)
  - Recursive deletion with detailed logging (attempted, deleted, errors, skipped)
  - Never blocks SessionEnd events, even on errors

- **Enhanced CLI options**
  - `--log-level LEVEL` - Explicit log level control (trace, debug, info, warn, error, fatal, report)
  - `--log-file PATH` - Custom log file path (default: `./.clojure-mcp-light-hooks.log`)
  - `--cljfmt` - Enable automatic code formatting with cljfmt after write/edit operations

- **Comprehensive testing documentation** in CLAUDE.md
  - Manual hook testing instructions
  - Claude Code integration testing guide
  - Troubleshooting section for common issues

### Changed
- **Logging system** - Replaced custom logging with Timbre
  - Structured logging with timestamps, namespaces, and line numbers
  - Configurable appenders and log levels
  - Conditional ns-filter for targeted logging
  - Disabled by default to avoid breaking hook protocol

- **Hook system improvements**
  - Refactored hook response format to minimize unnecessary output
  - Updated hook tests to match new response format
  - Extracted PPID session ID logic into dedicated function
  - Flattened tmp directory structure to single session-project level

- **CLI handling** - Refactored into dedicated `handle-cli-args` function
  - Cleaner separation of concerns
  - Better error handling and help messages
  - Uses `tools.cli` for argument parsing

- **File organization** - Migrated to unified tmp namespace
  - `hook.clj` now uses tmp namespace for backups
  - `nrepl_eval.clj` now uses tmp namespace for per-target sessions
  - Consistent session-scoped file management across all components

### Removed
- **-c short flag** for `--cljfmt` option (prevented conflicts with potential future flags)

[0.0.3-alpha]: https://github.com/bhauman/clojure-mcp-light/releases/tag/v0.0.3-alpha

## [0.0.2-alpha] - 2025-11-08

### Added
- **Enhanced ClojureScript support** - Learning to use edamame to detect delimiter errors across the widest possible set of Clojure/ClojureScript files
  - Added `:features #{:clj :cljs :cljr :default}` to enable platform-specific reader features
  - Explicit readers for common ClojureScript/EDN tagged literals:
    - `#js` - JavaScript object literals
    - `#jsx` - JSX literals
    - `#queue` - Queue data structures
    - `#date` - Date literals
  - Changed `:auto-resolve` to use `name` function for better compatibility

- **scripts/test-parse-all.bb** - Testing utility for delimiter detection
  - Recursively finds and parses all Clojure files in a directory
  - Reports unknown tags with suggestions for adding readers
  - Helps validate edamame configuration across real codebases
  - Stops on first error with detailed reporting

- **Dynamic var for error handling** - `*signal-on-bad-parse*` (defaults to `true`)
  - Triggers parinfer on unknown tag errors as a safety net
  - Allows users to opt out via binding if needed
  - More defensive approach: better to attempt repair than skip

- **Expanded test coverage**
  - 30 tests (up from 27) with 165 assertions (up from 129)
  - New test suites for ClojureScript features:
    - `clojurescript-tagged-literals-test` - All supported tagged literals
    - `clojurescript-features-test` - Namespaced keywords and `::keys` destructuring
    - `mixed-clj-cljs-features-test` - Cross-platform code with reader conditionals
  - Tests validate both delimiter detection and proper parsing

### Changed
- Updated `bb.edn` to use cognitect test-runner instead of manual test loading
  - Cleaner test execution
  - Better output formatting
  - Standard Clojure tooling approach

### Removed
- **Legacy standalone .bb scripts** - Removed `clj-paren-repair-hook.bb` and `clojure-nrepl-eval.bb`
  - Now use `bb -m clojure-mcp-light.hook` and `bb -m clojure-mcp-light.nrepl-eval` instead
  - bbin installation uses namespace entrypoints from `bb.edn`
  - Eliminates 597 lines of duplicate code
  - Simpler maintenance with single source of truth

[0.0.2-alpha]: https://github.com/bhauman/clojure-mcp-light/releases/tag/v0.0.2-alpha

## [0.0.1-alpha] - 2025-11-08

### Added
- **clj-paren-repair-claude-hook** - Claude Code hook for automatic Clojure delimiter fixing
  - Detects delimiter errors using edamame parser
  - Auto-fixes with parinfer-rust
  - PreToolUse hooks for Write/Edit/Bash operations
  - PostToolUse hooks for Edit operations with backup/restore
  - Cross-platform backup path handling
  - Session-specific backup isolation

- **clj-nrepl-eval** - nREPL evaluation tool
  - Direct bencode protocol implementation for nREPL communication
  - Automatic delimiter repair before evaluation
  - Timeout and interrupt handling for long-running evaluations
  - Persistent session support with Claude Code session-id based tmp-file with `./.nrepl-session` file as fallback
  - `--reset-session` flag for session management
  - Port detection: CLI flag > NREPL_PORT env > .nrepl-port file
  - Formatted output with dividers

- **Slash commands** for Claude Code
  - `/start-nrepl` - Start nREPL server in background
  - `/clojure-eval` - Information about REPL evaluation

- **Installation support**
  - bbin package manager integration
  - Proper namespace structure (clojure-mcp-light.*)

- **Documentation**
  - Comprehensive README.md
  - CLAUDE.md project documentation for Claude Code
  - Example settings and configuration files
  - EPL-2.0 license

### Changed
- Logging disabled by default for hook operations
- Error handling: applies parinfer on all errors for maximum robustness

[0.0.1-alpha]: https://github.com/bhauman/clojure-mcp-light/releases/tag/v0.0.1-alpha
