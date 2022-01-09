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

curl -k -g -X POST -H "Content-Type: application/json" -d '{"query":"query{descriptions {  description }}"}' https://hornsup:8080/graphql

curl -k -g -X POST -H "Content-Type: application/json" -d '{"query":"query{accounts { accountId accountNameOwner }}"}' https://hornsup:8080/graphql

curl -k -g -X POST -H "Content-Type: application/json" -d '{"query":"query{transactions(accountNameOwner: \"chase_kari\") { transactionId accountNameOwner transactionDate description  activeStatus}}"}' https://hornsup:8080/graphql

curl -k -g -X POST -H "Content-Type: application/json" -d '{"query":"query{description(descriptionName: \"testing\") { descriptionId description activeStatus}}"}' https://hornsup:8080/graphql

curl -k -g -X POST -H "Content-Type: application/json" -d '{"query":"mutation{createDescription(description: \"testing}\"}) { descriptionId description activeStatus}}"}' https://hornsup:8080/graphql

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

Another thing here, in your vcl log you can see that Varnish is doing Hit-For-Pass, that is, Varnish is caching the fact that your request is not cached. If you want to truly test, you'll need to purge the content beforehands. One last thing : your backend server (apache) responds with a Cache-Control : max-age =0 header which prevent caching, you should change that if you want to cache content.
