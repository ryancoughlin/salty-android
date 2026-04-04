# Workflow Enforcement

## Before Writing Any Kotlin Code
1. Read the iOS implementation first — no exceptions
2. Note the exact type names, property names, method signatures
3. Query Context7 for any library API you're about to use

## After Editing Any Kotlin File
1. Verify the edit matches iOS behavior
2. Check that no stubs or TODOs were introduced
3. If the file is a ViewModel, verify StateFlow properties match iOS @Published properties
4. If the file is a Service, verify API endpoints and serialization match iOS

## Before Committing
1. Run `/audit-ios-parity` on the changed files
2. Run `/verify-build` to confirm compilation
3. Use `/commit` to stage and commit with proper scoping

## When Stuck
1. Check `.claude/rules/lessons.md` for known patterns
2. Query Context7 for current library documentation
3. Read the iOS implementation again — the answer is usually there
