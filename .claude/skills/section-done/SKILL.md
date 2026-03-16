---
name: section-done
description: Run after completing a section. Launches code review, updates all tracking docs, and commits.
disable-model-invocation: true
argument-hint: [phase] [section] [section-name]
---

# Section Done

Complete the end-of-section protocol for **Phase $0, Section $1: $2**.

## Step 1: Launch Code Review Agent

1. Read `memory/review-agent.md` for the prompt template.
2. Fill in the template: PHASE=$0, SECTION=$1, SECTION_NAME=$2.
3. Identify all source files changed/created in this section (use `git diff` and `git status`).
4. Launch a **background** code review agent with the filled template and file list.

## Step 2: Update Tracking Docs (while review runs)

Update ALL of the following files:

### memory/progress.md
- Mark Section $1 as COMPLETE.
- Update the "Last completed" pointer to Phase $0, Section $1.
- Set "Next" to the next section (increment section number, or next phase if last section).

### docs/phase$0-tasks.md
- Change all task statuses in Section $1 from ⬜ to ✅.
- Update the section Status to COMPLETE.
- Add any relevant notes.

### CHANGELOG.md
- Add a new entry for Phase $0, Section $1: $2.
- List what was Added, Changed, or Fixed in this section.

### memory/patterns.md
- Add any new code patterns established during this section.

### memory/decisions.md
- Log any architecture decisions made during this section.

### CLAUDE.md
- Update the "Current State" line in Project Overview to reflect the new state.

### memory/MEMORY.md
- Update the index if any new memory files were created.

## Step 3: Wait for Review Agent

- Check the review agent results.
- If FAIL: address critical/major findings before committing.
- If PASS WITH NOTES: note minor findings but proceed.
- If PASS: proceed to commit.

## Step 4: Commit

Create a conventional commit with scope:

```
docs(phase$0): complete section $1 - $2
```

Stage only the tracking/doc files updated in Step 2. Do NOT stage source code files (those should already be committed).

Include `Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>` in the commit message.

## Step 5: Report

Summarize to the user:
- Review verdict and any findings
- Files updated
- Commit hash
- What the next section is
