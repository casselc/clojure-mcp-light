# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
