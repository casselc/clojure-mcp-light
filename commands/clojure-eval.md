---
description: Info on how to evaluate Clojure code via nREPL using clojure-nrepl-eval.bb
---

When you need to evaluate Clojure code you can use the
`clojure-nrepl-eval.bb` script to evalute code against an nREPL server.
This means the state of the REPL session will persist between
evaluations.

You can require or load a file in one evaluattion of the script and
when you call the script again the namespace will still be available.

## Example uses

You can evaluate clojure code to check if a file you just edited still compiles and loads.

Whenever you require a namespace always use the `:reload` key.

## How to Use

The following evaluates Clojure code via an nREPL connection.

```bash
./clojure-nrepl-eval.bb "<clojure-code>"
```

## Options

- `-p, --port PORT` - Specify nREPL port (if not using .nrepl-port or NREPL_PORT env)
- `-H, --host HOST` - Specify nREPL host (default: 127.0.0.1)
- `-t, --timeout MILLISECONDS` - Set timeout in milliseconds (default: 120000)
- `-h, --help` - Show help message

## Examples

**Basic evaluation (auto-detects port):**
```bash
./clojure-nrepl-eval.bb "(+ 1 2 3)"
```

**With specific port:**
```bash
./clojure-nrepl-eval.bb --port 7888 "(println \"Hello\")"
```

**With timeout:**
```bash
./clojure-nrepl-eval.bb --timeout 5000 "(Thread/sleep 10000)"
```

**Multiple expressions:**
```bash
./clojure-nrepl-eval.bb "(def x 10) (* x 2) (+ x 5)"
```

## Features

- **Auto-detects port** from .nrepl-port file or NREPL_PORT environment variable
- **Automatic delimiter repair** - fixes missing/mismatched parens before evaluation
- **Timeout handling** - interrupts long-running evaluations
