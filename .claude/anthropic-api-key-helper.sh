#!/bin/sh
# Outputs the Anthropic API key for this project's Claude Code terminal.
# Key itself is never stored in this repo — it lives in the macOS login keychain.
security find-generic-password -s "blacksmith-anthropic-api-key" -a "$USER" -w
