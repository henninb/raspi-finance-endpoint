# raspi-finance-endpoint

## update gradle wrapper version
./gradlew wrapper --gradle-version 6.0
./gradlew wrapper --gradle-version 6.0 --distribution-type all

## gradle command to find dependencies
./gradlew :dependencies > dependencies.txt
./gradlew :dependencies --configuration compile > dependencies_compile.txt

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

Check the application health

```shell
$ curl localhost:8080/actuator/health
```

Check the application health - prometheus

- open a browser - http://localhost:9090/

Find Unit test results

```shell
$ ./gradlew clean test
```

Find Integration test results

```shell
$ ./gradlew clean integrationTest
```

Find Integration functional test results

```shell
$ ./run.sh local
$ ./gradlew clean funtionalTest
```

Find Performance test results - 500 record (~3:45)

```shell
$ ./gradlew clean performanceTest
```
