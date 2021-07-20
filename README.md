# raspi-finance-endpoint

## update gradle wrapper version
./gradlew wrapper --gradle-version 6.0
./gradlew wrapper --gradle-version 6.0 --distribution-type all

## gradle command to find dependencies
./gradlew :dependencies > dependencies.txt
./gradlew :dependencies --configuration compile > dependencies_compile.txt

## flyway with gradle
```
./gradlew flywayMigrate --info
gradle -Dflyway.configFiles=path/to/myAlternativeConfig.conf flywayMigrate
```

## clojure and lambda
```
    // clojure example
    fun safeDivide(numerator: Int, denominator: Int) =
            if (denominator == 0) 0.0 else numerator.toDouble() / denominator

    val f: (Int, Int) -> Double = ::safeDivide

    val quotient = f(3, 4)

    //lambda example
    val safeDivide: (Int, Int) -> Double = { numerator, denominator ->
        if (denominator == 0) 0.0 else numerator.toDouble() / denominator
    }
```

## Check the application health

```shell
$ curl localhost:8080/actuator/health
```

## Check the application health - prometheus

- open a browser - http://localhost:9090/

## Find Unit test results

```shell
$ ./gradlew clean test
```

## Find Integration test results

```shell
$ ./gradlew clean integrationTest
```

## Find Integration functional test results

```shell
$ ./gradlew clean funtionalTest
```

## Find Performance test results - 500 record (~3:45)

```shell
$ ./gradlew clean performanceTest
```

## Select all active accounts

```shell
$ curl -k 'https://localhost:8080/account/select/active'
```

## gradle wrapper update
```
./gradlew wrapper --gradle-version=6.7 --distribution-type=bin
./gradlew wrapper --gradle-version=6.7
```

## docker compose
```
3.8 19.03.0+
3.7 18.06.0+
```

## dependency checker
```
./gradlew dependencyUpdates -Drevision=release
```

## retrofit example
```
https://github.com/jeyrschabu/retrofit-samples
```

## latest openjdk11 version
```
https://wiki.openjdk.java.net/display/JDKUpdates/JDK11u
```

## external ip address
dig +short myip.opendns.com @resolver1.opendns.com

## docker pull arm32v7/openjdk

## git preserve
```
git update-index --assume-unchanged src/main/kotlin/finance/configurations/OracleConfig.kt
git update-index --assume-unchanged env.secrets
git update-index --no-assume-unchanged src/main/kotlin/finance/configurations/OracleConfig.kt
```

## grafana setup
https://riamf.github.io/posts/dockerized_grafana_setup/

## logback translator
http://logback.qos.ch/translator/asGroovy.html

## graphql
http://hornsup:8080/graphiql
npx graphql-codegen

curl -g -X POST -H "Content-Type: application/json" -d '{"query":"query{descriptions { descriptionId description }}"}' http://hornsup:8080/graphql

curl -k -g -X POST -H "Content-Type: application/json" -d '{"query":"query{accounts { accountId accountNameOwner }}"}' https://hornsup:8080/graphql

curl -k -g -X POST -H "Content-Type: application/json" -d '{"query":"query{transactions(accountNameOwner: \"chase_kari\") { transactionId accountNameOwner transactionDate description  activeStatus}}"}' https://hornsup:8080/graphql

curl -k -g -X POST -H "Content-Type: application/json" -d '{"query":"query{description(descriptionName: \"testing\") { descriptionId description activeStatus}}"}' https://hornsup:8080/graphql

curl -k -g -X POST -H "Content-Type: application/json" -d '{"query":"mutation{createDescription(description: {description: \"testing}\"}) { descriptionId description activeStatus}}"}' https://hornsup:8080/graphql


# https://stackoverflow.com/questions/55113542/how-to-have-graphql-enum-resolve-strings
curl -k \
  -X POST \
  -H "Content-Type: application/json" \
  --data '{ "query": "mutation Add { createDescription(description: {description: \"car\"}){descriptionId}}" }' https://hornsup:8080/graphql
