# Production validation contract

The disposable validation branch is accepted only when one commit SHA passes the complete `Production Suite Validation` workflow without weakening any gate.

The gate covers all nine independent Spring Boot + Vue projects:

- `mvn verify` including security regression tests;
- MySQL 8.4 production-profile startup with Flyway migrations and Hibernate schema validation;
- non-root backend container builds;
- health, authorization, production seed isolation, and request-correlation smoke checks;
- committed frontend lockfiles, reproducible `npm ci`, production dependency audit, and Vite production builds;
- source-policy invariants for authentication, authorization, pagination/DTO boundaries, CORS, secret handling, error semantics, container users, and migration indexes;
- Java and JavaScript/TypeScript CodeQL analysis.

The workflow itself uses immutable action commit SHAs, explicit Ubuntu runner generation, least-privilege GitHub token permissions, and checkout credential persistence disabled.

A green run is necessary but not by itself a claim that software is mathematically impossible to attack. Final acceptance additionally requires review of the same SHA for known high-risk design gaps before it is landed and the disposable branch is removed.
