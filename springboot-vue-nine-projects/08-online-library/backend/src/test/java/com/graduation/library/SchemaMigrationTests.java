package com.graduation.library;
import org.junit.jupiter.api.Test;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.ActiveProfiles;
@SpringBootTest @ActiveProfiles("ci") class SchemaMigrationTests { @Test void flywaySchemaMatchesJpaModel(){} }
