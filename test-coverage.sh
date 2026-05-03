#!/bin/sh

./gradlew test jacocoTestReport
echo xdg-open build/reports/jacoco/test/html/index.html
xdg-open build/reports/jacoco/test/html/index.html

exit 0
