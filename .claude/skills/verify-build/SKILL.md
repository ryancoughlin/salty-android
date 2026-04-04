---
name: verify-build
description: Run the Android build to verify code compiles. Use after making significant changes or before committing.
allowed-tools: Bash
---

# Build Verification

Run the Android debug build and report the result.

```bash
cd /home/user/salty-android && ./gradlew assembleDebug 2>&1 | tail -30
```

## On Success
Report: "Build passed." with the build time.

## On Failure
1. Show the **first** compilation error (not the full log)
2. Identify the file and line number
3. Suggest a fix based on the error message

Do not attempt to fix the error — just report it clearly.
