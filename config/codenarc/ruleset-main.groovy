ruleset {
    description 'Stricter CodeNarc rules for main sources'

    // Core and style
    ruleset('rulesets/basic.xml')
    ruleset('rulesets/braces.xml')
    ruleset('rulesets/convention.xml')
    ruleset('rulesets/exceptions.xml')
    ruleset('rulesets/imports.xml')
    ruleset('rulesets/formatting.xml') {
        Indentation(enabled: true)
        SpaceAroundMapEntryColon(enabled: true)
        SpaceAroundOperator(enabled: true)
        LineLength(length: 140)
        TrailingWhitespace(enabled: true)
    }

    // Naming tightened for main
    ruleset('rulesets/naming.xml') {
        ClassName(enabled: true)
        MethodName(enabled: true)
        VariableName(enabled: true)
        ParameterName(enabled: true)
        FieldName(enabled: true)
    }

    // Size/complexity stricter for main
    ruleset('rulesets/size.xml') {
        CyclomaticComplexity(maxMethodComplexity: 15)
        MethodSize(maxLines: 150)
        NestedBlockDepth(maxNestedBlockDepth: 4)
        ParameterCount(maxParameters: 6)
    }

    // Code quality
    ruleset('rulesets/dry.xml')
    ruleset('rulesets/groovyism.xml')
    ruleset('rulesets/unnecessary.xml')
    ruleset('rulesets/duplicates.xml')
    ruleset('rulesets/logging.xml')
    ruleset('rulesets/security.xml')

    // We don't include JUnit-specific rules here; production code should be framework-agnostic
}

