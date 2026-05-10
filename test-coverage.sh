#!/usr/bin/env bash

set -euo pipefail

REPORT_HTML="build/reports/jacoco/test/html/index.html"
REPORT_CSV="build/reports/jacoco/test/csv/jacocoTestReport.csv"

echo "=== Running unit tests ==="
./gradlew test 2>&1

echo ""
echo "=== Running integration tests ==="
SPRING_PROFILES_ACTIVE=int ./gradlew integrationTest 2>&1

echo ""
echo "=== Running functional tests ==="
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest 2>&1

echo ""
echo "=== Generating combined JaCoCo coverage report ==="
./gradlew jacocoTestReport 2>&1

echo ""
echo "=== Coverage Summary ==="
if [ -f "$REPORT_CSV" ]; then
    awk -F',' '
    NR == 1 { next }
    {
        missed_inst += $4; covered_inst += $5
        missed_branch += $6; covered_branch += $7
        missed_line += $8; covered_line += $9
    }
    END {
        total_inst  = missed_inst  + covered_inst
        total_branch = missed_branch + covered_branch
        total_line  = missed_line  + covered_line
        pct_inst   = total_inst   > 0 ? (covered_inst   / total_inst   * 100) : 0
        pct_branch = total_branch > 0 ? (covered_branch / total_branch * 100) : 0
        pct_line   = total_line   > 0 ? (covered_line   / total_line   * 100) : 0
        printf "Instructions: %d/%d (%.1f%%)\n", covered_inst,   total_inst,   pct_inst
        printf "Branches:     %d/%d (%.1f%%)\n", covered_branch, total_branch, pct_branch
        printf "Lines:        %d/%d (%.1f%%)\n", covered_line,   total_line,   pct_line
    }' "$REPORT_CSV"
else
    echo "CSV report not found at $REPORT_CSV"
fi

echo ""
echo "HTML report: $REPORT_HTML"
if command -v xdg-open &>/dev/null && [ -n "${DISPLAY:-}" ]; then
    xdg-open "$REPORT_HTML"
fi
echo xdg-open build/reports/jacoco/test/html/index.html

exit 0
