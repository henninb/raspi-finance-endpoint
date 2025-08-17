#!/bin/sh
# trim-trailing-space.sh
# POSIX-compatible: remove trailing whitespace in files under the current dir.

# ---- Configuration (override via env vars) ----
# Space-separated list of directory names to skip entirely.
EXCLUDE_DIRS=${EXCLUDE_DIRS:-".git .hg .svn node_modules dist build .cache .venv .direnv target out vendor"}
# Space-separated list of filename extensions (without dots) to process.
# Keep this conservative to avoid binary files.
EXTENSIONS=${EXTENSIONS:-"sh bash zsh fish txt md markdown rst conf ini cfg env sample
json json5 yaml yml toml properties
c h cpp hpp cc hh m mm java kt kts groovy gradle
py rb pl php go rs hs
js mjs cjs jsx ts tsx css scss less html htm svelte vue
sql csv tsv dockerfile make mk mkd cmake nix"}
# If set to "1", only report what would change.
DRY_RUN=${DRY_RUN:-0}
# If set to "1", create a .bak alongside each modified file.
BACKUP=${BACKUP:-0}

# ---- Helpers ----
is_excluded_dir() {
  bn=$1
  for d in $EXCLUDE_DIRS; do
    [ "$bn" = "$d" ] && return 0
  done
  return 1
}

has_wanted_ext() {
  f=$1
  case "$f" in
    *.*)
      ext=${f##*.}
      ;;
    *)
      # no extension: skip by default
      return 1
      ;;
  esac
  for e in $EXTENSIONS; do
    [ "$ext" = "$e" ] && return 0
  done
  return 1
}

process_file() {
  f=$1
  # Work in a temp file (portable; avoid sed -i).
  tmp="${TMPDIR:-/tmp}/trimspace.$$.$RANDOM"
  # Some shells don’t set RANDOM; ensure uniqueness with pid/time.
  [ -z "$RANDOM" ] && tmp="${TMPDIR:-/tmp}/trimspace.$$.$(date +%s%N 2>/dev/null || date +%s)"

  # Strip trailing whitespace using POSIX BRE (\{1,\} instead of +)
  # Also strip trailing spaces on the final line (even if no newline).
  # We do two passes: normal lines, then possible last line without newline.
  if sed 's/[[:space:]]\{1,\}$//' "$f" >"$tmp" 2>/dev/null; then
    if cmp -s "$f" "$tmp"; then
      rm -f "$tmp"
      return 0
    fi

    if [ "$DRY_RUN" = "1" ]; then
      echo "DRY: would trim: $f"
      rm -f "$tmp"
      return 0
    fi

    if [ "$BACKUP" = "1" ]; then
      cp -- "$f" "$f.bak" 2>/dev/null || cp "$f" "$f.bak"
    fi

    # Replace the original
    mv -- "$tmp" "$f" 2>/dev/null || mv "$tmp" "$f"
    echo "trimmed: $f"
  else
    rm -f "$tmp"
    echo "warn: could not process: $f" >&2
  fi
}

walk() {
  dir=$1
  # Portable directory traversal without -print0; handle spaces via set -- and quoting.
  # We use 'ls -A' carefully; if you prefer, replace this block with 'find' below.
  # Safer: use POSIX 'find' with an exec.
  find "$dir" -mindepth 1 -maxdepth 1 -print | while IFS= read -r path; do
    base=$(basename -- "$path" 2>/dev/null || basename "$path")
    if [ -d "$path" ]; then
      if is_excluded_dir "$base"; then
        :
      else
        walk "$path"
      fi
    elif [ -f "$path" ]; then
      if has_wanted_ext "$path"; then
        process_file "$path"
      fi
    fi
  done
}

# Entry
walk "."

exit 0
