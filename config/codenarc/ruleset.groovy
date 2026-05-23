ruleset {
    description 'Baseline CodeNarc rules for Groovy test sources'

    // Core correctness
    ruleset('rulesets/basic.xml') {
        DeadCode(enabled: false)
        // Tests intentionally compare object to itself to verify equals() reflexivity
        ComparisonWithSelf(enabled: false)
    }

    ruleset('rulesets/braces.xml') {
        // Single-line if statements are common in test setup/teardown; disable for now
        IfStatementBraces(enabled: false)
    }

    // Convention: disable rules that fight Groovy/Spock idioms
    ruleset('rulesets/convention.xml') {
        CompileStatic(enabled: false)
        // Inverted if/else and ternary refactoring are style preferences
        InvertedIfElse(enabled: false)
        CouldBeElvis(enabled: false)
        TernaryCouldBeElvis(enabled: false)
        // Parameter reassignment is sometimes cleaner in builder/loop logic
        ParameterReassignment(enabled: false)
        // def is idiomatic Groovy — especially in Spock setup/cleanup methods
        NoDef(enabled: false)
        VariableTypeRequired(enabled: false)
        MethodReturnTypeRequired(enabled: false)
        FieldTypeRequired(enabled: false)
        MethodParameterTypeRequired(enabled: false)
        // Ordering rules make no sense for Spock test classes
        PublicMethodsBeforeNonPublicMethods(enabled: false)
        StaticMethodsBeforeInstanceMethods(enabled: false)
        StaticFieldsBeforeInstanceFields(enabled: false)
        // Trailing comma is a style choice
        TrailingComma(enabled: false)
        // Allow Date and Double in tests
        NoJavaUtilDate(enabled: false)
        NoDouble(enabled: false)
        // Implicit returns and closure params are Groovy idioms
        ImplicitReturnStatement(enabled: false)
        ImplicitClosureParameter(enabled: false)
    }

    // Exceptions: tests sometimes need broad catch patterns
    ruleset('rulesets/exceptions.xml') {
        ThrowRuntimeException(enabled: false)
        CatchException(enabled: false)
        // Tests sometimes catch Throwable in setup/cleanup for isolation
        CatchThrowable(enabled: false)
        // Tests sometimes throw generic Exception in mock stubs
        ThrowException(enabled: false)
        // Tests sometimes return null from catch to allow assertion on null
        ReturnNullFromCatchBlock(enabled: false)
        // Tests sometimes catch RuntimeException to verify behavior
        CatchRuntimeException(enabled: false)
    }

    ruleset('rulesets/imports.xml') {
        UnusedImport(enabled: true)
        NoWildcardImports(enabled: false)
        // Project convention: static imports come AFTER regular imports
        MisorderedStaticImports(comesBefore: false)
    }

    ruleset('rulesets/formatting.xml') {
        // Standard Groovy map literal style is `key: value`, not `key : value`
        SpaceAroundMapEntryColon(enabled: false)
        // Indentation enforcement is too strict for Spock's DSL-style blocks
        Indentation(enabled: false)
        // Spock data tables and long mock chains need more room
        LineLength(length: 400)
        // Blank-line-in-block rules are too pedantic for test classes
        ClassEndsWithBlankLine(enabled: false)
        ClassStartsWithBlankLine(enabled: false)
        BlockEndsWithBlankLine(enabled: false)
        BlockStartsWithBlankLine(enabled: false)
        MissingBlankLineBeforeAnnotatedField(enabled: false)
        // Empty blocks like >> {} or new Foo() {} don't need inner spaces
        SpaceAfterOpeningBrace(ignoreEmptyBlock: true)
        SpaceBeforeClosingBrace(ignoreEmptyBlock: true)
    }

    ruleset('rulesets/naming.xml') {
        // Spock test names use string literals — MethodName doesn't apply
        MethodName(enabled: false)
        FactoryMethodName(enabled: false)
        ConfusingMethodName(enabled: false)
        // Allow underscores and digits in test variable names (e.g. d2023_01_05)
        VariableName(regex: /[a-z][a-zA-Z0-9_]*/)
    }

    ruleset('rulesets/size.xml') {
        CyclomaticComplexity(maxMethodComplexity: 40)
        MethodSize(maxLines: 200)
        MethodCount(enabled: false)
        // Test classes naturally grow large with many test cases; disable
        ClassSize(enabled: false)
        // ABC metric is noisy for test helpers with many setup branches
        AbcMetric(enabled: false)
        // Test helpers and data builders legitimately take many parameters
        ParameterCount(maxParameters: 8)
    }

    // Test data naturally repeats literals; dry rules are too noisy in tests
    ruleset('rulesets/dry.xml') {
        DuplicateStringLiteral(enabled: false)
        DuplicateNumberLiteral(enabled: false)
        DuplicateMapLiteral(enabled: false)
        DuplicateListLiteral(enabled: false)
    }

    ruleset('rulesets/groovyism.xml') {
        // closures as last params are idiomatic Groovy
        ClosureAsLastMethodParameter(enabled: false)
        // Explicit .equals() calls are used to test equals() contracts in isolation
        ExplicitCallToEqualsMethod(enabled: false)
        // These Groovy idiom replacements are minor; skip for tests
        ExplicitCallToMultiplyMethod(enabled: false)
        ExplicitCallToCompareToMethod(enabled: false)
        // GString expressions within strings are minor style issues
        GStringExpressionWithinString(enabled: false)
    }

    ruleset('rulesets/unnecessary.xml') {
        // Explicit return keyword is a valid style choice
        UnnecessaryReturnKeyword(enabled: false)
        // Fully-qualified class names in code are sometimes clearer; disable for tests
        UnnecessaryPackageReference(enabled: false)
        // Parens around closures can improve readability; style preference
        UnnecessaryParenthesesForMethodCallWithClosure(enabled: false)
        // .collect{it} calls can be intentional patterns; disable for tests
        UnnecessaryCollectCall(enabled: false)
        // else after return is sometimes clearer; style preference
        UnnecessaryElseStatement(enabled: false)
        // In finance code, new BigDecimal("0.00") is intentional for scale control
        UnnecessaryBigDecimalInstantiation(enabled: false)
        // Double quotes are fine; consistent quoting is a style preference
        UnnecessaryGString(enabled: false)
        // .class can improve readability in dynamic contexts
        UnnecessaryDotClass(enabled: false)
        // Variable references can improve readability
        UnnecessaryObjectReferences(enabled: false)
        // Kotlin-Groovy interop sometimes requires explicit getter/setter calls
        UnnecessaryGetter(enabled: false)
        UnnecessarySetter(enabled: false)
        UnnecessaryBooleanExpression(enabled: false)
        // .toString() in tests can be intentional for type coercion clarity
        UnnecessaryToString(enabled: false)
    }

    // Spock uses its own assertion model; disable JUnit-specific rules
    ruleset('rulesets/junit.xml') {
        JUnitPublicNonTestMethod(enabled: false)
        JUnitPublicProperty(enabled: false)
        JUnitSetUpCallsSuper(enabled: false)
        JUnitTearDownCallsSuper(enabled: false)
        JUnitTestMethodWithoutAssert(enabled: false)
    }

    ruleset('rulesets/logging.xml')

    ruleset('rulesets/security.xml') {
        // new Random() is fine in test data generation
        InsecureRandom(enabled: false)
    }
}
