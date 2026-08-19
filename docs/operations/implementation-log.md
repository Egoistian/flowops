# FlowOps Implementation Log

## 2026-08-19 22:35 KST — Build bootstrap

- Java runtime: OpenJDK 17.0.18.
- Spring Boot line: 3.5.16.
- Gradle distribution: 8.14.3 binary distribution.
- Distribution URL: `https://services.gradle.org/distributions/gradle-8.14.3-bin.zip`.
- Published SHA-256: `bd71102213493060956ec229d946beee57158dbd89d0e62b91bca0fa2c5f3531`.
- Downloaded SHA-256: `bd71102213493060956ec229d946beee57158dbd89d0e62b91bca0fa2c5f3531`.
- Integrity result: exact match.
- Spring Initializr metadata was checked first. Its live generator exposed only Spring Boot 4.0/4.1, so it was not used to generate or mix Boot 4 source into this Boot 3.5 project.
- Wrapper source: generated locally with the verified official Gradle 8.14.3 distribution.
