#!/bin/bash
#!/usr/bin/env bash

PRO_VERSION="0.6.0"
TOOLS_DIR="./tools/"
TOOLS_DIR_PRETTY="./tools"

# Function to list available tools
list_tools() {
    echo "Available tools:"
    echo "  - init (builtin tool command)"
    echo "  - init-dir (builtin tool command - initializes tools dir and creates sample entries)"
    echo "  - bundle (builtin tool command)"
    for tool in "$TOOLS_DIR"./*; do
        if [[ -x "$tool" && -f "$tool" ]]; then
            echo "  - $(basename "$tool")"
        fi
    done
}

# No arguments: show help and list tools
if [ $# -eq 0 ]; then
    echo "protools shell command: use to execute build tools or list them."
    echo "Usage:"
    echo "  ./pro                  # Show this help and list available tools"
    echo "  ./pro <tool>           # Execute a tool with default config"
    echo "  ./pro <tool> --help    # Execute a tool with help"
    echo "  ./pro bundle           # Copy the pro executable into the current repo"
    echo "Author: DanLP6"
    echo "Version: "$PRO_VERSION
    echo ""
    list_tools
    exit 0
fi

if [ "$1" == init ]; then
   for tool in "$TOOLS_DIR"./*; do
      chmod +x $tool
   done
fi

if [ "$1" == init-dir ]; then
   mkdir $TOOLS_DIR
   cd $TOOLS_DIR
   touch build
   touch compile
   touch build-release
   cd ..
   echo "added temlates"
   for tool in "$TOOLS_DIR"./*; do
      chmod +x $tool
   done
fi

if [ "$1" == bundle ]; then
    PRO_SRC="$(readlink -f "${BASH_SOURCE[0]}")"
    if [ -e ./pro ]; then
        # Copies without a PRO_VERSION line predate versioning and count as outdated
        BUNDLED_VERSION="$(grep -m1 '^PRO_VERSION=' ./pro | cut -d'"' -f2)"
        BUNDLED_VERSION="${BUNDLED_VERSION:-0}"
        NEWEST="$(printf '%s\n%s\n' "$BUNDLED_VERSION" "$PRO_VERSION" | sort -V | tail -n1)"
        if [ "$BUNDLED_VERSION" == "$NEWEST" ]; then
            echo "pro in $(pwd) is up to date ($BUNDLED_VERSION), skipping."
            exit 0
        fi
        echo "Updating pro in $(pwd) from $BUNDLED_VERSION to $PRO_VERSION"
    fi
    cp "$PRO_SRC" ./pro
    chmod +x ./pro
    echo "Bundled pro into $(pwd)/pro"
    exit 0
fi

# Extract tool name and shift to pass remaining args
TOOL_NAME="$1"
TOOL_ARGS="$2"
shift 1
TOOL_PATH="$TOOLS_DIR/$TOOL_NAME"

# Check if tool exists and is executable
if [[ -x "$TOOL_PATH" && -f "$TOOL_PATH" ]]; then
    "$TOOL_PATH" "$TOOL_ARGS" "$@"
else
    if [ "$TOOL_NAME" == init ]; then
        echo ""
        list_tools
        exit 0
    elif [ "$TOOL_NAME" == init-dir ]; then
        echo ""
        list_tools
        exit 0
    else
        echo "Error: Tool '$TOOL_NAME' not found in $TOOLS_DIR_PRETTY or is not executable."
        echo
        list_tools
        exit 1
    fi
fi

