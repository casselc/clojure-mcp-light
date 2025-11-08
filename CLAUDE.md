# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

clojure-mcp-light is NOT an MCP server. It provides minimal CLI tooling for Clojure development in Claude Code through two main tools:

1. **clj-paren-repair-claude-hook** - Hook for automatic delimiter fixing in Clojure files
2. **clj-nrepl-eval** - nREPL evaluation tool with automatic delimiter repair

The project leverages Claude Code's native hook system to transparently fix delimiter errors (mismatched brackets, parentheses, braces) without custom MCP tooling.

## Development Commands

### Running Tests
```bash
bb test
```
This runs all tests in the test/ directory using Babashka's built-in test runner.

### Installing Locally with bbin
```bash
# Install both commands
bbin install .
bbin install . --as clj-nrepl-eval --main-opts '["-m" "clojure-mcp-light.nrepl-eval"]'
```

### Testing the Hook Manually
```bash
echo '{"hook_event_name":"PreToolUse","tool_name":"Write","tool_input":{"file_path":"test.clj","content":"(def x 1)"}}' | clj-paren-repair-claude-hook
```

### Testing nREPL Evaluation
```bash
# Start an nREPL server first (creates .nrepl-port automatically)
clj -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.0.0"}}}' -M -m nrepl.cmdline

# In another terminal, evaluate code
clj-nrepl-eval "(+ 1 2 3)"
```

## Architecture

### Core Modules

**delimiter_repair.clj** - Core delimiter error detection and repair
- Uses edamame parser to detect delimiter errors specifically (not general syntax errors)
- Calls parinfer-rust CLI for intelligent delimiter repair
- Key functions: `delimiter-error?`, `fix-delimiters`

**hook.clj** - Claude Code hook implementation
- Intercepts Write and Edit tool operations via PreToolUse/PostToolUse hooks
- For Write: detects and fixes delimiter errors before writing, blocks write if unfixable
- For Edit: creates backup before edit, auto-fixes after edit, restores from backup if unfixable
- Uses multimethod dispatch on `[hook_event_name tool_name]` pairs
- Communicates fixes back to Claude via hook response format

**nrepl_eval.clj** - nREPL client with timeout and session management
- Direct bencode protocol implementation for nREPL communication
- Automatic delimiter repair before evaluation
- Timeout/interrupt handling for long-running evaluations
- **Persistent sessions**: Reuses session ID from `.nrepl-session` file across invocations
- Session management: `--reset-session` flag to start fresh session
- Port detection: CLI flag > NREPL_PORT env > .nrepl-port file

### Hook System Flow

**Write operations (PreToolUse):**
1. Detect delimiter errors in content
2. If errors found, attempt fix with parinfer
3. If fixable, update content and allow write with reason message
4. If unfixable, deny write with error message

**Edit operations (PreToolUse + PostToolUse):**
1. PreToolUse: Create backup of file in temp directory with session ID
2. Allow edit to proceed
3. PostToolUse: Check edited file for delimiter errors
4. If errors found, attempt fix
5. If fixable, apply fix and delete backup
6. If unfixable, restore from backup and block with error message
7. If no errors, delete backup silently

### Backup File Path Generation

The `backup-path` function creates deterministic backup paths:
- Uses `java.nio.file.Paths` for cross-platform path handling
- Strips root component (works on both Unix `/foo/bar` and Windows `C:\foo\bar`)
- Format: `{java.io.tmpdir}/claude-hook-backup-{session-id}/{relative-path}`
- Session ID ensures backups don't collide across Claude Code sessions

### Session Persistence

The nREPL evaluation tool implements persistent sessions:
- Session ID stored in `.nrepl-session` file
- Reused across invocations until nREPL server restarts or `--reset-session` used
- Allows stateful development: vars, namespaces, and loaded libraries persist
- Functions: `slurp-nrepl-session`, `spit-nrepl-session`, `delete-nrepl-session`

## Key Implementation Details

### Delimiter Detection vs General Errors
The `delimiter-error?` function is deliberately narrow - it only detects delimiter errors, not general syntax errors. This is by design to avoid false positives and unnecessary parinfer runs.

### File Type Detection
The hook only processes files ending in: `.clj`, `.cljs`, `.cljc`, `.bb`, `.edn`

### Logging
Hooks support optional logging via dynamic vars `*enable-logging*` and `*log-file*`. Disabled by default to avoid breaking hook protocol.

### nREPL Timeout Strategy
The `eval-expr-with-timeout` function polls every 250ms to honor timeout deadlines while reading responses. When timeout hits, it sends an nREPL `:interrupt` op to gracefully stop evaluation.

## Common Patterns

### When requiring namespaces via clj-nrepl-eval
Always use the `:reload` flag to ensure fresh code:
```bash
clj-nrepl-eval "(require 'my.namespace :reload)"
```

### Testing delimiter repair in isolation
```clojure
(require '[clojure-mcp-light.delimiter-repair :refer [delimiter-error? fix-delimiters]])
(delimiter-error? "(+ 1 2")  ; => true
(fix-delimiters "(+ 1 2")    ; => "(+ 1 2)"
```

### Hook response format
Hooks must return JSON matching Claude Code's hook protocol:
- PreToolUse: `{:hookSpecificOutput {:hookEventName "PreToolUse" :permissionDecision "allow"}}`
- PostToolUse: `{:hookSpecificOutput {:hookEventName "PostToolUse" :additionalContext "..."}}`
- Block: `{:decision "block" :reason "..." :hookSpecificOutput {...}}`

## Dependencies

External tools required:
- **parinfer-rust** - Must be on PATH for delimiter repair
- **babashka** - For running as a script
- **bbin** - For installation

Clojure dependencies (bb.edn):
- borkdude/edamame - Parser for delimiter detection
- cheshire/cheshire - JSON encoding/decoding for hook protocol
- org.clojure/tools.cli - CLI argument parsing
- nrepl/bencode - nREPL protocol implementation

## Testing Philosophy

Tests live in `test/clojure_mcp_light/` mirroring source structure. Run with `bb test` which uses Babashka's test runner.
