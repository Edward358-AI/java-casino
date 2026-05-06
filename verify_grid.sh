#!/bin/bash
# verify_grid.sh — reproduce the 9-archetype × 3-mode BB/100 grid.
#
# Usage:
#   ./verify_grid.sh                  # 1 run per cell, 50k pairs (~6 min total)
#   ./verify_grid.sh 5                # 5 runs per cell, averaged (~30 min)
#   ./verify_grid.sh 50 25000         # 50 runs × 25k pairs averaged (~3hr)
#
# Multi-run behavior (RUNS >= 2): each cell shows "mean ± SE" computed across
# the N runs. Higher N → tighter SE → more confidence in the mean.
#
# Output:
#   stdout: live progress + final table
#   file:   verify_results_<timestamp>.txt (full table + per-run raw data)
#
# Prereq: javac PokerBot.java PokerSimulator.java Mode6Verify.java
# (will auto-compile if Mode6Verify.class is missing or stale)

set -e
cd "$(dirname "$0")"

RUNS=${1:-1}
PAIRS=${2:-50000}

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTFILE="verify_results_${TIMESTAMP}.txt"
RAWFILE="verify_raw_${TIMESTAMP}.tsv"

# Auto-compile if needed
if [ ! -f Mode6Verify.class ] || [ Mode6Verify.java -nt Mode6Verify.class ]; then
  echo "Compiling..." >&2
  javac PokerBot.java PokerSimulator.java Mode6Verify.java
fi

ARC_NAMES=("NIT" "STATION" "MANIAC" "TAG" "WHALE" "FISH" "BULLY" "SHORT_STACKER" "LAG")
MODES=("pure" "neural" "nightmare")

# tee output to both stdout and file
exec > >(tee -a "$OUTFILE")

echo "# Mode 6 Verification Grid"
echo "# Date:  $(date)"
echo "# Config: ${RUNS} run(s) × ${PAIRS} pairs per cell"
echo "# Total: $((9 * 3 * RUNS)) runs / $((9 * 3 * RUNS * PAIRS)) pairs simulated"
echo "# Estimated wall-clock: ~$(awk "BEGIN {printf \"%.0f\", 9 * 3 * $RUNS * $PAIRS / 50000 * 13 / 60}") minutes"
echo "# Raw per-run data: $RAWFILE"
echo ""

# Header for raw file
echo -e "arc\tmode\trun\tbb100" > "$RAWFILE"

run_cell() {
  local arc=$1 mode=$2
  local sum=0 sumsq=0
  local arc_name="${ARC_NAMES[$arc]}"
  for r in $(seq 1 $RUNS); do
    local v=$(java Mode6Verify $arc $PAIRS $mode 2>&1 \
              | grep 'Bot A \[God\] BB/100' | head -1 \
              | sed -E 's/.*-> ([+-]?[0-9]+\.[0-9]+).*/\1/')
    echo -e "${arc_name}\t${mode}\t${r}\t${v}" >> "$RAWFILE"
    sum=$(awk "BEGIN {print $sum + $v}")
    sumsq=$(awk "BEGIN {print $sumsq + $v * $v}")
  done
  # Compute mean and SE
  if [ "$RUNS" -ge 2 ]; then
    awk -v sum="$sum" -v sumsq="$sumsq" -v n="$RUNS" 'BEGIN {
      mean = sum / n
      var = (sumsq / n) - (mean * mean)
      se = sqrt(var > 0 ? var / n : 0)
      printf "%+.2f ± %.2f", mean, se
    }'
  else
    awk -v sum="$sum" -v n="$RUNS" 'BEGIN {printf "%+.2f", sum / n}'
  fi
}

# Print Markdown table
if [ "$RUNS" -ge 2 ]; then
  printf "| %-13s | %-15s | %-15s | %-15s |\n" "Arc" "Pure" "Neural" "Unprotected"
  printf "|%-15s|%-17s|%-17s|%-17s|\n" "---" "---:" "---:" "---:"
else
  printf "| %-13s | %-10s | %-10s | %-12s |\n" "Arc" "Pure" "Neural" "Unprotected"
  printf "|%-15s|%-12s|%-12s|%-14s|\n" "---" "---:" "---:" "---:"
fi

# Track totals for averages row
declare -A col_sum
declare -A col_count

for arc in 0 1 2 3 4 5 6 7 8; do
  if [ "$RUNS" -ge 2 ]; then
    printf "| %-13s |" "${ARC_NAMES[$arc]}"
  else
    printf "| %-13s |" "${ARC_NAMES[$arc]}"
  fi
  for mode in "${MODES[@]}"; do
    val=$(run_cell $arc $mode)
    if [ "$RUNS" -ge 2 ]; then
      printf " %15s |" "$val"
    else
      printf " %10s |" "$val"
    fi
    # Extract numeric part for averaging
    bare=$(echo "$val" | sed -E 's/([+-]?[0-9]+\.[0-9]+).*/\1/')
    col_sum[$mode]=$(awk "BEGIN {print ${col_sum[$mode]:-0} + $bare}")
    col_count[$mode]=$((${col_count[$mode]:-0} + 1))
  done
  echo ""
done

# Average row across archetypes
if [ "$RUNS" -ge 2 ]; then
  printf "| **%-11s** |" "Avg"
else
  printf "| **%-11s** |" "Avg"
fi
for mode in "${MODES[@]}"; do
  avg=$(awk "BEGIN {printf \"%+.2f\", ${col_sum[$mode]} / ${col_count[$mode]}}")
  if [ "$RUNS" -ge 2 ]; then
    printf " **%13s** |" "$avg"
  else
    printf " **%8s** |" "$avg"
  fi
done
echo ""

echo ""
echo "# Done. Saved to: $OUTFILE"
echo "# Raw data:       $RAWFILE"
