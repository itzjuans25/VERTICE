package com.ecommerce.catalogo;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.ecommerce.catalogo", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {
    @ArchTest
    static final ArchRule nucleoIndependiente = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "javax.persistence..",
                    "org.hibernate..", "com.rabbitmq..", "..infraestructura..");
}
