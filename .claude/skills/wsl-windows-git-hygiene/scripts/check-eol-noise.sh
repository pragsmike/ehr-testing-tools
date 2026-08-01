#!/usr/bin/env bash
set -euo pipefail

repo="${1:-.}"

if ! git -C "$repo" rev-parse --show-toplevel >/dev/null 2>&1; then
  echo "Not a git repository: $repo" >&2
  exit 1
fi

top="$(git -C "$repo" rev-parse --show-toplevel)"
cd "$top"

echo "== Repo =="
echo "$top"
echo

echo "== Relevant Git Config =="
git config --show-origin --get-regexp '^(core\.(autocrlf|eol|safecrlf|filemode)|merge\.renormalize)$' || true
echo

echo "== .gitattributes (first 200 lines) =="
if [[ -f .gitattributes ]]; then
  sed -n '1,200p' .gitattributes
else
  echo "(missing)"
fi
echo

echo "== Diff Summary =="
echo "-- raw --"
git diff --stat || true
echo
echo "-- ignore CR at EOL --"
git diff --ignore-cr-at-eol --stat || true
echo

declare -a eol_only=()
declare -a real_or_mixed=()

while IFS= read -r -d '' path; do
  if [[ -z "$(git diff --ignore-cr-at-eol -- "$path")" ]]; then
    eol_only+=("$path")
  else
    real_or_mixed+=("$path")
  fi
done < <(git diff --name-only -z)

echo "== Modified File Classification =="
echo "EOL-only candidates: ${#eol_only[@]}"
for path in "${eol_only[@]}"; do
  printf '  %s\n' "$path"
done
echo
echo "Real-or-mixed changes: ${#real_or_mixed[@]}"
for path in "${real_or_mixed[@]}"; do
  printf '  %s\n' "$path"
done
echo

if (( ${#eol_only[@]} > 0 )); then
  echo "== git ls-files --eol for EOL-only candidates =="
  git ls-files --eol -- "${eol_only[@]}" || true
  echo
fi

echo "== Untracked Files =="
git ls-files --others --exclude-standard || true
echo

echo "== Suggested Next Step =="
if (( ${#eol_only[@]} > 0 )) && (( ${#real_or_mixed[@]} == 0 )); then
  echo "Mostly line-ending noise. Keep .gitattributes strict, consider local excludes for untracked junk, and make a dedicated normalization commit."
elif (( ${#eol_only[@]} > 0 )) && (( ${#real_or_mixed[@]} > 0 )); then
  echo "Mixed state. Split normalization from semantic edits and commit them separately."
elif (( ${#real_or_mixed[@]} > 0 )); then
  echo "These changes still contain semantic diffs under --ignore-cr-at-eol. Review them on their merits."
else
  echo "No modified tracked files detected, or no EOL noise found."
fi
