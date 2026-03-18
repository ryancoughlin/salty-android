---
description: Stage and commit changes for a specific scope
---

Review the git diff and stage ONLY files related to: $ARGUMENTS

Rules:
1. Stage only files directly related to the described scope - nothing else
2. If a file has mixed changes (related + unrelated), do NOT stage it - ask me first
3. Write a simple, lowercase commit message (no emoji, no "Generated with Claude Code", no Co-Authored-By)
4. Format: `<type>: <description>` where type is fix/feat/refactor/docs/chore
5. Show me what you're about to commit before running git commit
6. Do not stage files that are unrelated to the scope even if they're modified

Steps:
1. Run `git diff --stat` and `git status` to see all changes
2. Identify which files match the scope
3. Show me the files you plan to stage and your proposed commit message
4. Wait for my approval before committing
