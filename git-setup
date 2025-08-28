#!/bin/sh

# Git repository setup script for raspi-finance-endpoint
# Run this script after cloning the repository to configure local settings

echo "Setting up local git configuration for raspi-finance-endpoint..."

# User identification
git config --local user.email henninb@gmail.com
git config --local user.name 'Brian Henning'

# Branch tracking
git branch --set-upstream-to=origin/main main

# Security and workflow configurations
git config --local core.autocrlf false          # Prevent line ending conversion issues on mixed environments
git config --local core.safecrlf true          # Warn about mixed line endings
git config --local push.default simple         # Only push current branch to matching branch
git config --local pull.rebase false           # Use merge instead of rebase for pulls
git config --local init.defaultBranch main     # Set default branch name for new repos

# Editor configuration (uncomment and modify if needed)
# git config --local core.editor "vim"         # Set default editor
# git config --local core.editor "code --wait" # For VS Code users

# Secrets filter (currently disabled - uncomment if .gitattributes is configured)
#git config --local filter.secrets.clean "sed 's/_PASSWORD=.*/_PASSWORD=********/'"
#git config --local filter.secrets.smudge cat

# Performance optimizations for Spring Boot API repositories
git config --local core.preloadindex true      # Preload index for faster operations
git config --local core.fscache true          # Cache file system information (Windows/some Linux)

# Java/Gradle specific configurations
git config --local core.ignorecase false       # Case sensitive file names (important for Java packages)

echo "✅ Local git configuration completed successfully"
echo "📋 Current configuration:"
git config --local --list | grep -E "(user\.|branch\.main\.|push\.|pull\.)"

exit 0