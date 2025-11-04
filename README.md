# clojure-mcp-light

> **⚠️ Experimental**: This project is in early stages. We're experimenting with what works to bring better Clojure support to Claude Code.

Automatic delimiter fixing for Clojure files in Claude Code using hooks and parinfer, plus a standalone nREPL evaluation tool.

## Overview

Clojure-mcp-light provides two main tools:

1. **Automatic delimiter fixing hooks** - Detects and fixes delimiter errors (mismatched brackets, parentheses, braces) when working with Clojure files in Claude Code. The hook system intercepts file operations and transparently fixes delimiter issues before they cause problems.

2. **clojure-nrepl-eval.bb** - A standalone Babashka script for evaluating Clojure code via nREPL with automatic delimiter repair, timeout handling, and formatted output.

## Features

- **Automatic delimiter detection** using edamame parser
- **Auto-fixing with parinfer-rust** for intelligent delimiter repair
- **Write operations**: Detects and fixes delimiter errors before writing files
- **Edit operations**: Creates backup before edits, auto-fixes after, or restores from backup if unfixable
- **Real-time feedback**: Communicates fixes and issues back to Claude Code via hook responses

## Requirements

- [Babashka](https://github.com/babashka/babashka) - Fast-starting Clojure scripting environment
- [parinfer-rust](https://github.com/eraserhd/parinfer-rust) - Delimiter inference and fixing
- [Claude Code](https://docs.claude.com/en/docs/claude-code) - The Claude CLI tool

## Setup

1. Clone this repository:
   ```bash
   git clone <repo-url> clojure-mcp-light
   cd clojure-mcp-light
   ```

2. Install dependencies:

   **Babashka** (macOS):
   ```bash
   brew install babashka/brew/babashka
   ```

   **parinfer-rust**:

   https://github.com/eraserhd/parinfer-rust

   > I had to compile parinfer rust for Apple Silicon and install it manually

   The `parinfer-rust` binary must be on Claude Code's PATH. To check Claude Code's PATH, you can ask Claude to run:
   ```bash
   echo $PATH
   ```

   Install the binary to a directory in the PATH (e.g., `/usr/local/bin`):
   ```bash
   # Example: create symbolic link in /usr/local/bin
   sudo ln -s /path/to/downloaded/parinfer-rust /usr/local/bin/parinfer-rust
   ```

3. Make the scripts executable:
   ```bash
   chmod +x clj-paren-repair-hook.bb
   chmod +x clojure-nrepl-eval.bb
   ```

   Optionally, add the `clojure-nrepl-eval.bb` script to your PATH so it can be used from anywhere:
   ```bash
   # Example: create symbolic link in /usr/local/bin
   sudo ln -s $(pwd)/clojure-nrepl-eval.bb /usr/local/bin/clojure-nrepl-eval
   ```

   Or ensure the directory containing the script is on Claude Code's PATH. To check Claude Code's PATH:
   ```bash
   echo $PATH
   ```

4. Configure Claude Code hooks by adding to `.claude/settings.local.json`:
   ```json
   {
     "hooks": {
       "PreToolUse": [
         {
           "matcher": "Write|Edit",
           "hooks": [
             {
               "type": "command",
               "command": "/absolute/path/to/clj-paren-repair-hook.bb"
             }
           ]
         }
       ],
       "PostToolUse": [
         {
           "matcher": "Edit",
           "hooks": [
             {
               "type": "command",
               "command": "/absolute/path/to/clj-paren-repair-hook.bb"
             }
           ]
         }
       ]
     }
   }
   ```

   See [settings_example/settings.local.json](settings_example/settings.local.json) for a complete example.

## Using with ClojureMCP

This project works well with [ClojureMCP](https://github.com/bhauman/clojure-mcp) for REPL integration. You can configure ClojureMCP to only enable the eval tool, avoiding conflicts with the delimiter fixing hooks.

Create a `.clojure-mcp/config.edn` file in your project:

```clojure
{:enabled-tools [:clojure_eval]}
```

This ensures that ClojureMCP only provides the REPL evaluation functionality while the hooks handle delimiter fixing.

See [settings_example/clojure-mcp-config.edn](settings_example/clojure-mcp-config.edn) for an example configuration.

## clojure-nrepl-eval.bb - Standalone nREPL Evaluator

This project includes a standalone Babashka script for evaluating Clojure code via nREPL with automatic delimiter repair.

### Features

- **Direct nREPL communication** using bencode protocol
- **Automatic delimiter repair** before evaluation using parinfer-rust
- **Timeout and interrupt handling** for long-running evaluations
- **Formatted output** with dividers between results
- **Flexible configuration** via command-line flags, environment variables, or `.nrepl-port` file

### Usage

```bash
# Evaluate code (port auto-detected from .nrepl-port file or NREPL_PORT env)
./clojure-nrepl-eval.bb "(+ 1 2 3)"

# Specify port explicitly
./clojure-nrepl-eval.bb --port 7888 "(println \"Hello\")"

# Use short flags
./clojure-nrepl-eval.bb -p 7889 "(* 5 6)"

# Set timeout (in milliseconds)
./clojure-nrepl-eval.bb --timeout 5000 "(Thread/sleep 10000)"

# Show help
./clojure-nrepl-eval.bb --help
```

### Automatic Delimiter Repair

The script automatically fixes missing or mismatched delimiters before evaluation:

```bash
# This will be auto-fixed from "(+ 1 2 3" to "(+ 1 2 3)"
./clojure-nrepl-eval.bb "(+ 1 2 3"
# => 6
```

### Options

- `-p, --port PORT` - nREPL port (default: from .nrepl-port or NREPL_PORT env)
- `-H, --host HOST` - nREPL host (default: 127.0.0.1 or NREPL_HOST env)
- `-t, --timeout MILLISECONDS` - Timeout in milliseconds (default: 120000)
- `-h, --help` - Show help message

### Environment Variables

- `NREPL_PORT` - Default nREPL port
- `NREPL_HOST` - Default nREPL host

### Port Configuration

The script needs to connect to an nREPL server. There are three ways to configure the port, checked in this order:

1. **Command-line flag** (highest priority)
   ```bash
   ./clojure-nrepl-eval.bb --port 7888 "(+ 1 2)"
   ```

2. **Environment variable**
   ```bash
   export NREPL_PORT=7888
   ./clojure-nrepl-eval.bb "(+ 1 2)"
   ```

3. **`.nrepl-port` file** (lowest priority)

   Most Clojure REPLs automatically create a `.nrepl-port` file in your project directory when they start. The script will automatically read this file:
   ```bash
   # Start your REPL (creates .nrepl-port automatically)
   clj -M:repl/nrepl

   # In another terminal, the script auto-detects the port
   ./clojure-nrepl-eval.bb "(+ 1 2)"
   ```

   You can also create this file manually:
   ```bash
   echo "7888" > .nrepl-port
   ```

**Starting an nREPL Server**

If you don't have an nREPL server running, you need to start one first. Here are common ways:

```bash
# Using Clojure CLI with nREPL dependency
clj -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.0.0"}}}' -M -m nrepl.cmdline

# Using Leiningen
lein repl :headless

# Using Babashka
bb nrepl-server 7888
```

Each of these will start an nREPL server and typically create a `.nrepl-port` file that the script can use.

**Error Handling**

If the script cannot find a port through any of the three methods, it will exit with an error:
```
Error: No nREPL port found
Provide port via --port, NREPL_PORT env var, or .nrepl-port file
```

Make sure you have an nREPL server running and the port is configured using one of the methods above.

## How It Works

The system uses Claude Code's hook mechanism to intercept file operations:

- **PreToolUse hooks** run before Write/Edit operations, allowing inspection and modification of content
- **PostToolUse hooks** run after Edit operations, enabling post-processing and restoration if needed
- **Detection → Fix → Feedback** flow ensures Claude is informed about what happened

**Write operations**: If delimiter errors are detected, the content is fixed via parinfer before writing. If unfixable, the write is blocked.

**Edit operations**: A backup is created before the edit. After the edit, if delimiter errors exist, they're fixed automatically. If unfixable, the file is restored from backup and Claude is notified.

## Example

Before (with delimiter error):
```clojure
(defn broken [x]
  (let [result (* x 2]
    result))
```

After (automatically fixed):
```clojure
(defn broken [x]
  (let [result (* x 2)]
    result))
```

The missing `)` is automatically added by parinfer, and Claude receives feedback about the fix.

## Contributing

Since this is experimental, contributions and ideas are welcome! Feel free to:

- Open issues with suggestions or bug reports
- Submit PRs with improvements
- Share your experiments and what works (or doesn't work)

## License

Eclipse Public License - v 2.0 (EPL-2.0)

See [LICENSE.md](LICENSE.md) for the full license text.
