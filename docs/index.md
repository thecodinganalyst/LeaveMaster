# LeaveMaestro Documentation

Welcome to the shared documentation site for LeaveMaestro. Choose the section that matches what you want to do.

<div class="grid cards" markdown>

-   :material-account-group:{ .lg .middle } **Using LeaveMaestro**

    ---

    User guides for employees, managers, HR, and tenant administrators. Learn how to sign in, manage leave, review requests, and use the features available to your role.

    [:octicons-arrow-right-24: Open the User Guide](user-guide/index.md)

-   :material-code-braces:{ .lg .middle } **Building & Operating LeaveMaestro**

    ---

    Technical documentation for developers, operators, and contributors covering architecture, APIs, security, development, deployment, operations, and Ask LeaveMaestro.

    [:octicons-arrow-right-24: Open Technical Documentation](technical/index.md)

</div>

## About LeaveMaestro

LeaveMaestro is a multi-tenant employee leave management application with a Spring Boot backend, a React/Refine frontend, policy-driven leave entitlements, configurable RBAC, deployment automation, and an embedded AI assistant.

The source code and both documentation audiences are maintained together in the [LeaveMaster GitHub repository](https://github.com/thecodinganalyst/LeaveMaster). The site continues to use one GitHub Pages deployment and one shared search experience.

## Technical architecture at a glance

```mermaid
flowchart LR
    Browser[Browser]
    Firebase[Firebase Hosting\nReact / Vite SPA]
    CloudRun[Google Cloud Run\nSpring Boot API]
    Postgres[(Supabase PostgreSQL)]
    GCS[(GCS attachments)]
    OpenAI[OpenAI API]

    Browser --> Firebase
    Firebase -->|Application and API routes| CloudRun
    CloudRun --> Postgres
    CloudRun --> GCS
    CloudRun -. optional assistant .-> OpenAI
```

For implementation details, continue to the [Technical Documentation](technical/index.md).
