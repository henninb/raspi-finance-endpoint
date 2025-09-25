ruleset {
    description 'Baseline CodeNarc rules for Groovy (tests-heavy project)'

    // Core rule sets
    ruleset('rulesets/basic.xml') {
        DeadCode(enabled: false)
    }
    ruleset('rulesets/braces.xml')
    ruleset('rulesets/convention.xml') {
        // Spock/Groovy tests need dynamic behavior; avoid forcing @CompileStatic
        CompileStatic(enabled: false)
        // Allow legacy java.util.Date use in tests
        NoJavaUtilDate(enabled: false)
        // Allow Double usage in tests for simplicity
        NoDouble(enabled: false)
    }
    ruleset('rulesets/exceptions.xml') {
        // Tests sometimes throw RuntimeException intentionally
        ThrowRuntimeException(enabled: false)
    }
    ruleset('rulesets/imports.xml') {
        // Tighten: enable unused import detection
        UnusedImport(enabled: true)
    }
    ruleset('rulesets/formatting.xml') {
        // Incremental tightening: keep reasonable formatting without being ultra strict
        SpaceAroundMapEntryColon(enabled: true)
        SpaceAroundOperator(enabled: true)
        Indentation(enabled: true)
        LineLength(length: 400) // allow longer lines for Spock data tables
    }
    ruleset('rulesets/naming.xml') {
        ClassName(enabled: true)
        // Keep permissive for tests; we can tighten later with per-source-set configs
        MethodName(enabled: false)
        VariableName(enabled: true)
        // Spock method names often include words like 'create...'; ignore this in tests
        FactoryMethodName(enabled: false)
        ConfusingMethodName(enabled: false)
    }
    ruleset('rulesets/size.xml') {
        CyclomaticComplexity(maxMethodComplexity: 20)
        MethodSize(maxLines: 200)
        MethodCount(enabled: false)
    }
    // New: detect duplicates & common Groovy idioms
    ruleset('rulesets/dry.xml')
    ruleset('rulesets/groovyism.xml')
    ruleset('rulesets/unnecessary.xml')

    // Disable JUnit-centric rules; we use Spock
    ruleset('rulesets/junit.xml') {
        JUnitPublicNonTestMethod(enabled: false)
        JUnitPublicProperty(enabled: false)
        JUnitSetUpCallsSuper(enabled: false)
        JUnitTearDownCallsSuper(enabled: false)
        JUnitTestMethodWithoutAssert(enabled: false)
    }

    // Note: Project uses Spock; we keep some naming relaxed globally for now
}
