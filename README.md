# raspi-finance-endpoint

## update gradle wrapper version
./gradlew wrapper --gradle-version 6.0
./gradlew wrapper --gradle-version 6.0 --distribution-type all

## gradle command to find dependencies
./gradlew :dependencies > dependencies.txt
./gradlew :dependencies --configuration compile > dependencies_compile.txt
