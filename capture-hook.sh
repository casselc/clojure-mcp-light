#!/bin/bash
# Capture hook for debugging Edit and Write tool calls
# This script captures the JSON input from PreToolUse hooks and logs it

# Create logs directory if it doesn't exist
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="${SCRIPT_DIR}/hook-logs"
mkdir -p "$LOG_DIR"

# Generate timestamp-based log file
TIMESTAMP=$(date +"%Y%m%d_%H%M%S_%N")
LOG_FILE="${LOG_DIR}/hook-${TIMESTAMP}.json"

# Read JSON from stdin and save it
cat > "$LOG_FILE"

# Print the log file location to stderr (won't interfere with JSON output)
echo "Hook data captured to: $LOG_FILE" >&2

# Exit 0 to allow the operation to proceed
exit 0
