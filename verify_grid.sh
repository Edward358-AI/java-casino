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
# Portability: works with bash 3.2+ (macOS) and BSD awk.

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

# Use plain arrays (no associative arrays — bash 3 compatible)
ARC_NAMES="NIT STATION MANIAC TAG WHALE FISH BULLY SHORT_STACKER LAG"
MODES="pure neural nightmare"

# tee output to both stdout and file
exec > >(tee -a "$OUTFILE")

echo "# Mode 6 Verification Grid"
echo "# Date:  $(date)"
echo "# Config: ${RUNS} run(s) × ${PAIRS} pairs per cell"
TOTAL_RUNS=$((9 * 3 * RUNS))
TOTAL_PAIRS=$((TOTAL_RUNS * PAIRS))
echo "# Total: ${TOTAL_RUNS} runs / ${TOTAL_PAIRS} pairs simulated"
EST_MIN=$(awk -v r="$RUNS" -v p="$PAIRS" 'BEGIN {printf "%.0f", 9 * 3 * r * p / 50000 * 13 / 60}')
echo "# Estimated wall-clock: ~${EST_MIN} minutes"
echo "# Raw per-run data: $RAWFILE"
echo ""

# Header for raw file
printf "arc\tmode\trun\tbb100\n" > "$RAWFILE"

run_cell() {
  local arc=$1 mode=$2
  local sum=0 sumsq=0
  local arc_name=$3
  local r v
  for r in $(seq 1 $RUNS); do
    v=$(java Mode6Verify $arc $PAIRS $mode 2>&1 \
              | grep 'Bot A \[God\] BB/100' | head -1 \
              | sed -E 's/.*-> ([+-]?[0-9]+\.[0-9]+).*/\1/')
    printf "%s\t%s\t%s\t%s\n" "$arc_name" "$mode" "$r" "$v" >> "$RAWFILE"
    sum=$(awk -v s="$sum" -v x="$v" 'BEGIN {print s + x}')
    sumsq=$(awk -v s="$sumsq" -v x="$v" 'BEGIN {print s + x * x}')
  done
  if [ "$RUNS" -ge 2 ]; then
    awk -v sum="$sum" -v sumsq="$sumsq" -v n="$RUNS" 'BEGIN {
      mean = sum / n
      var = (sumsq / n) - (mean * mean)
      se = sqrt(var > 0 ? var / n : 0)
      printf "%+.2f +/- %.2f", mean, se
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

# Use indexed parallel arrays for column-sum tracking (bash 3.2 compatible).
# Index: 0=pure, 1=neural, 2=nightmare.
col_sum_0=0; col_sum_1=0; col_sum_2=0
col_count_0=0; col_count_1=0; col_count_2=0

arc_idx=0
for arc_name in $ARC_NAMES; do
  printf "| %-13s |" "$arc_name"
  mode_idx=0
  for mode in $MODES; do
    val=$(run_cell $arc_idx $mode "$arc_name")
    if [ "$RUNS" -ge 2 ]; then
      printf " %15s |" "$val"
    else
      printf " %10s |" "$val"
    fi
    bare=$(echo "$val" | sed -E 's/([+-]?[0-9]+\.[0-9]+).*/\1/')
    case $mode_idx in
      0) col_sum_0=$(awk -v s="$col_sum_0" -v x="$bare" 'BEGIN {print s + x}'); col_count_0=$((col_count_0 + 1));;
      1) col_sum_1=$(awk -v s="$col_sum_1" -v x="$bare" 'BEGIN {print s + x}'); col_count_1=$((col_count_1 + 1));;
      2) col_sum_2=$(awk -v s="$col_sum_2" -v x="$bare" 'BEGIN {print s + x}'); col_count_2=$((col_count_2 + 1));;
    esac
    mode_idx=$((mode_idx + 1))
  done
  echo ""
  arc_idx=$((arc_idx + 1))
done

# Average row across archetypes
printf "| **%-11s** |" "Avg"
mode_idx=0
for mode in $MODES; do
  case $mode_idx in
    0) avg=$(awk -v s="$col_sum_0" -v n="$col_count_0" 'BEGIN {printf "%+.2f", s / n}');;
    1) avg=$(awk -v s="$col_sum_1" -v n="$col_count_1" 'BEGIN {printf "%+.2f", s / n}');;
    2) avg=$(awk -v s="$col_sum_2" -v n="$col_count_2" 'BEGIN {printf "%+.2f", s / n}');;
  esac
  if [ "$RUNS" -ge 2 ]; then
    printf " **%13s** |" "$avg"
  else
    printf " **%8s** |" "$avg"
  fi
  mode_idx=$((mode_idx + 1))
done
echo ""

echo ""
echo "# Done. Saved to: $OUTFILE"
echo "# Raw data:       $RAWFILE"
