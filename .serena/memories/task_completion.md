# Task Completion Checklist

After completing a section:
1. Update memory/progress.md — mark section complete, advance pointer
2. Update memory/patterns.md — add new code patterns if established
3. Update memory/decisions.md — add new decisions if made
4. Update CLAUDE.md "Current State" — advance phase/section pointer
5. Update docs/phase*-tasks.md — mark tasks with completion status
6. Update CHANGELOG.md — append section completion summary
7. Run code review subagent (see memory/review-agent.md)
8. Commit with conventional commit format: type(scope): description

## Important Notes
- Memory files exist in TWO locations: `~/.claude/projects/.../memory/` AND `memory/` in project root
- Keep both copies in sync
- One conversation = one section of tasks
- Never leave a section half-done

## Key Docs Reference
| File | Content |
|------|---------|
| CLAUDE.md | Autopilot file, auto-loaded each conversation |
| docs/PRD.md | Product requirements, data model, features |
| docs/Architecture.md | High-level architecture, layers, threading |
| docs/system-design.md | Detailed system designs, flows, ER diagram |
| docs/ui-design-guide.md | Colors, typography, spacing, components |
| docs/phase1-tasks.md | Phase 1 task breakdown (75 tasks) |
| docs/phase2-tasks.md through phase6-tasks.md | Phases 2-6 |
