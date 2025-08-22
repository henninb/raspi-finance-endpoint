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
$ curl localhost:8443/actuator/health
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
$ curl -k 'https://localhost:8443/account/select/active'
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
http://hornsup:8443/graphiql
npx graphql-codegen

curl -k -g -X POST -H "Content-Type: application/json" -d '{"query":"query{descriptions {  description }}"}' https://hornsup:8443/graphql

curl -k -g -X POST -H "Content-Type: application/json" -d '{"query":"query{accounts { accountId accountNameOwner }}"}' https://hornsup:8443/graphql

curl -k -g -X POST -H "Content-Type: application/json" -d '{"query":"query{transactions(accountNameOwner: \"chase_kari\") { transactionId accountNameOwner transactionDate description  activeStatus}}"}' https://hornsup:8443/graphql

curl -k -g -X POST -H "Content-Type: application/json" -d '{"query":"query{description(descriptionName: \"testing\") { descriptionId description activeStatus}}"}' https://hornsup:8443/graphql

curl -k -g -X POST -H "Content-Type: application/json" -d '{"query":"mutation{createDescription(description: \"testing}\"}) { descriptionId description activeStatus}}"}' https://hornsup:8443/graphql

# https://stackoverflow.com/questions/55113542/how-to-have-graphql-enum-resolve-strings

mutation { createDescription(description: "car") {descriptionId} }
mutation { createPayment(payment: {accountNameOwner: "test", activeStatus: true, amount: 0.0}) {paymentId} }

# api key
Just add the @EnableApiKeyAuthentication annotation to you Spring Boot Application class
and provide web.authentication.apikey property to enable static API key authentication.
This will add a Spring HandlerInterceptor that will check the X-Api-Key request header for the configured static API key.
If no or not the correct key is provided the request will fail and send 401 as return code.
```
curl -v --header "API_KEY: abcdefg"
curl https://hornsup/tokens/$ACCESS_KEY -H "X-Auth-Token: $SECRET_KEY"
```

# log4j dependency details
```
./gradlew :dep | grep log4j
```

# varnish log
```
varnishlog
```

# vcl caching issue
```
  : your backend server (apache) responds with a Cache-Control : max-age =0 header which prevent caching,
  you should change that if you want to cache content.
```

## log4j
```
because you are using version 2.4.4 of springboot or greater.
spring boot use StaticLoggerBinder to get log factory.
StaticLoggerBinder has been deleted in version 1.3.x of logback-classic.
Here are two ways to solve this problem:
 1.use versions of slf4j-api(2.x.x) and logback-classic(1.3.x) without spring boot.
 2.use spring boot's default logback dependencies.
```

## hibernate 6.x
```https://stackoverflow.com/questions/72761919/class-springhibernatejpapersistenceprovider-does-not-implement-the-requested-int```
// https://mvnrepository.com/artifact/jakarta.persistence/jakarta.persistence-api
implementation group: 'jakarta.persistence', name: 'jakarta.persistence-api', version: '3.1.0'

## testing
```
./gradlew integrationTest --tests "*.DatabaseResilienceIntSpec" -Dspring.profiles.active=int
./gradlew integrationTest --tests "finance.processors.ProcessorIntegrationSpec"
```



  curl -X POST http://localhost:8080/graphql \
    -H "Content-Type: application/json" \
    -d '{
      "query": "{ transfers { transactionDate sourceAccount destinationAccount amount } }"
    }'

  Alternative with formatted JSON for readability:

  curl -X POST http://localhost:8080/graphql \
    -H "Content-Type: application/json" \
    -d '{
      "query": "query GetTransfers { transfers { transactionDate sourceAccount destinationAccount amount } }"
    }'
