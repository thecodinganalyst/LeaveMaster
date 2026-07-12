# Balanced Efficiency Rules

## Context Optimization (Save Credits)
- Skip reading test folders (`src/test/java`) entirely unless the issue specifically asks to write or fix tests.
- Do not read front-end, documentation, or static asset directories (e.g., resources/static, markdown files).
- Avoid explaining your logic. Provide only the necessary file modifications and a brief 1-sentence summary of the fix.

## Accuracy Guarantees (Maintain Quality)
- ALWAYS inspect `pom.xml` (or `build.gradle`) first to understand existing dependencies before introducing new code.
- Always check the package-level `repository` and `service` folders to see if an abstraction already exists before writing custom SQL or business logic.
- Ensure all new endpoints strictly adhere to the existing global exception handling design patterns present in the codebase.
