---
name: clojure-eval
description: Evaluate Clojure code via nREPL using clj-nrepl-eval. Use this when you need to test code, check if edited files compile, verify function behavior, or interact with a running REPL session.
---

# Clojure REPL Evaluation

## When to Use This Skill

Use this skill when you need to:
- **Verify that edited Clojure files compile and load correctly**
- Test function behavior interactively
- Check the current state of the REPL
- Debug code by evaluating expressions
- Require or load namespaces for testing
- Validate that code changes work before committing

## How It Works

The `clj-nrepl-eval` command evaluates Clojure code against an nREPL server. **Session state persists between evaluations**, so you can require a namespace in one evaluation and use it in subsequent calls. Each host:port combination maintains its own session file.

## Instructions

### 1. Display Actively Connected nREPL Sessions

Before evaluating, you can check if you already have an active session on a port:

```bash
clj-nrepl-eval --connected-ports
```

This shows active nREPL sessions and their ports. Connections only
appear in this list after you've evaluated code on that port at least once.

**Important:** This only shows connections you have made, not all nREPL
servers running on the system.

### 2. Evaluate Clojure Code

> Evaluation automatically connects to the given port

Use the `-p` flag to specify the port and pass your Clojure code:

**As a command-line argument:**
```bash
clj-nrepl-eval --port 7888 "(+ 1 2 3)"
```

**Via stdin pipe:**
```bash
echo "(+ 1 2 3)" | clj-nrepl-eval -p 7888
```

**Via heredoc (for multiline code or code needing complex symbol escaping):**
```bash
clj-nrepl-eval -p 7888 <<'EOF'
(def x 10)
(+ x 20)
EOF
```

### 3. Common Patterns

**Require a namespace (always use :reload to pick up changes):**
```bash
clj-nrepl-eval -p 7888 "(require '[my.namespace :as ns] :reload)"
```

**Test a function after requiring:**
```bash
clj-nrepl-eval -p 7888 "(ns/my-function arg1 arg2)"
```

**Check if a file compiles:**
```bash
clj-nrepl-eval -p 7888 "(require 'my.namespace :reload)"
```

**Multiple expressions:**
```bash
clj-nrepl-eval -p 7888 "(def x 10) (* x 2) (+ x 5)"
```

**With custom timeout (in milliseconds):**
```bash
clj-nrepl-eval -p 7888 --timeout 5000 "(long-running-fn)"
```

**Reset the session (clears all state):**
```bash
clj-nrepl-eval -p 7888 --reset-session
clj-nrepl-eval -p 7888 --reset-session "(def x 1)"
```

## Available Options

- `-p, --port PORT` - nREPL port (required)
- `-H, --host HOST` - nREPL host (default: 127.0.0.1)
- `-t, --timeout MILLISECONDS` - Timeout (default: 120000 = 2 minutes)
- `-r, --reset-session` - Reset the persistent nREPL session
- `-c, --connected-ports` - List all active nREPL connections
- `-h, --help` - Show help message

## Important Notes

- **Sessions persist:** State (vars, namespaces, loaded libraries) persists across invocations until the nREPL server restarts or `--reset-session` is used
- **Automatic delimiter repair:** The tool automatically repairs missing or mismatched parentheses
- **Always use :reload:** When requiring namespaces, use `:reload` to pick up recent changes
- **Default timeout:** 2 minutes (120000ms) - increase for long-running operations
- **Input precedence:** Command-line arguments take precedence over stdin

## Typical Workflow

1. Discover connections: `clj-nrepl-eval --connected-ports`
2. Require namespace: `clj-nrepl-eval -p 7888 "(require '[my.ns :as ns] :reload)"`
3. Test function: `clj-nrepl-eval -p 7888 "(ns/my-fn ...)"`
4. Iterate: Make changes, re-require with `:reload`, test again
