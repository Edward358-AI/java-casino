#!/bin/bash
# verify_grid.sh — reproduce the 9-archetype × 3-mode BB/100 grid.
#
# Usage:
#   ./verify_grid.sh                  # 1 run per cell, 50k pairs (~6 min total)
#   ./verify_grid.sh 5                # 5 runs per cell, averaged (~30 min)
#   ./verify_grid.sh 50 25000         # 50 runs × 25k pairs averaged (faster, more samples)
#
# Prereq: javac PokerBot.java PokerSimulator.java Mode6Verify.java
# (will auto-compile if Mode6Verify.class is missing)
#
# Output: prints a Markdown-style table to stdout. Each cell shows the mean
# BB/100 from God's perspective (positive = God wins, negative = Arc-bot wins).

set -e
cd "$(dirname "$0")"

RUNS=${1:-1}
PAIRS=${2:-50000}

# Auto-compile if needed
if [ ! -f Mode6Verify.class ] || [ Mode6Verify.java -nt Mode6Verify.class ]; then
  echo "Compiling..." >&2
  javac PokerBot.java PokerSimulator.java Mode6Verify.java
fi

ARC_NAMES=("NIT" "STATION" "MANIAC" "TAG" "WHALE" "FISH" "BULLY" "SHORT_STACKER" "LAG")
MODES=("pure" "neural" "nightmare")

run_cell() {
  local arc=$1 mode=$2
  local sum=0 count=0
  for r in $(seq 1 $RUNS); do
    local v=$(java Mode6Verify $arc $PAIRS $mode 2>&1 \
              | grep 'Bot A \[God\] BB/100' | head -1 \
              | sed -E 's/.*-> ([+-]?[0-9]+\.[0-9]+).*/\1/')
    sum=$(awk "BEGIN {print $sum + $v}")
    count=$((count + 1))
  done
  awk "BEGIN {printf \"%.2f\", $sum / $count}"
}

echo "Running 9 archetypes × 3 modes × $RUNS runs of $PAIRS pairs..."
echo "(this takes ~$(awk "BEGIN {printf \"%.0f\", 9 * 3 * $RUNS * $PAIRS / 50000 * 13 / 60}") minutes)"
echo ""

# Header
printf "| %-13s |" "Arc"
for mode in "${MODES[@]}"; do printf " %12s |" "$mode"; done
echo ""
printf "|%-15s|" "---"
for mode in "${MODES[@]}"; do printf "%14s|" "---:"; done
echo ""

# Rows
for arc in 0 1 2 3 4 5 6 7 8; do
  printf "| %-13s |" "${ARC_NAMES[$arc]}"
  for mode in "${MODES[@]}"; do
    avg=$(run_cell $arc $mode)
    printf " %12s |" "$avg"
  done
  echo ""
done
