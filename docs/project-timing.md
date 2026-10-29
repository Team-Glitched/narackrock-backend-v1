# Shared project BPM and time signature

`PATCH /api/v1/projects/{projectId}/timing` requires the authenticated original owner of a DRAFT or IN_PROGRESS project. Completed projects are read-only. Example body:

```json
{"bpm":90,"timeSignatureNumerator":6,"timeSignatureDenominator":8}
```

BPM accepts integers 30–300, numerator 1–12, and denominator 2, 4, 8 or 16. All three fields are required. The response contains the saved timing, recalculated `maxTick` and project version. Repeating the same setting is a no-op.

A tempo change retains MIDI ticks and recalculates cached clip milliseconds. Audio clips retain their original recording duration and source offset in milliseconds; their beat duration is recalculated and their musical start stays at the same tick. Rendered track media is invalidated. The update is rejected before mutation if an existing clip or any participant's active private draft would exceed the project duration. Private drafts remain private; the error exposes no draft contents. Timing updates take the same project write lock used by editor draft saves/commits.

Changed settings increment the minor project version. Published projects enqueue a refreshed mix through the existing export workflow. No database migration is required: projects already store both timing fields.

## Validation

- TDD: `ProjectTimingUpdateUseCaseTest` was written first and failed for the absent use case; implementation then passed all six cases.
- `./gradlew test`: 1,059 tests, zero failures/errors/skips (Java 21).
- Real local-test API/UI: original-owner update persisted; non-owner returned 403; invalid/missing values returned 400; existing MIDI/private draft/undo history survived timing changes and reload.
- Other participant's private draft prevented a tempo reduction beyond the one-minute limit; project timing remained unchanged.
- A published four-quarter-note mix regenerated at 90 BPM to approximately 2.666 seconds.

The frontend companion branch is `refector/piano-roll-studio`. Deploy the endpoint together with its UI. Local checks used an isolated H2 database, loopback port 8081 and the existing local-test identities, never a production database.
