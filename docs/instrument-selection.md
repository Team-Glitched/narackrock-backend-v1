# Instrument families and recorded roles

Track creation now requires a known sampled instrument or recorded role. `InstrumentFamily` defines musical families: piano/electric piano share a family; nylon/acoustic guitar share a family; clean/overdrive/distortion electric guitar share another; bass tones share a family. Acoustic guitar and electric guitar remain separate. Lead/backing vocal/rap share the vocal family. The frontend's grouped picker uses the same mapping.

Multiple players may choose the same instrument or family, including identical guitar tones and lead vocals for a duet. Families organize the picker; they do not reserve a part for one player. DRAFT, PENDING and APPROVED assignments do not block teammates. The existing one-active-layer-per-user rule still applies. Legacy uppercase/Korean aliases map to the same families. Unknown new assignments and unclassified audio creation are rejected; existing data is not automatically relabelled.

## Change API

`PATCH /api/v1/projects/{projectId}/tracks/{trackId}/instrument`

```json
{"instrument":"electric-guitar"}
```

Only the track's authenticated owner can change an editable DRAFT/APPROVED track in an unfinished project. A teammate's instrument never blocks creation or an instrument change. Creation and updates retain the project write lock to coordinate with clip, draft and metadata changes. The obsolete `INSTRUMENT_FAMILY_TAKEN` response has been removed.

The API changes instrument metadata without rewriting note data or audio contents. It checks saved clips and the owner's active private drafts before switching to drums (MIDI 35–81) or to an audio-only role. MIDI notes cannot be assigned a vocal/other audio-only role. MIDI creation, editing and draft validation reject such roles instead of rendering them as piano. Audio clip creation requires a classification. Approved changes invalidate rendered track media, update project version and queue a published mix refresh.

No schema migration is needed. The frontend and backend companion branches are `fix/allow-shared-instruments`, both based on their latest remote dev. Deploy both changes: an older frontend still disables shared choices, while an older backend still rejects them.

## Verification — 2026-11-05

- TDD: six shared-family creation cases and seven shared-family instrument-change/note-preservation cases failed under the old restriction, then passed after removing it.
- Java 21 `./gradlew test`: 1,136 tests total, zero failures/errors, seven Redis integration tests skipped by their environment conditions (1,129 passed).
- Guitar tests cover the identical tone and other tones in the same family; vocal tests cover identical lead vocals, backing vocals and rap. Existing tests continue to cover the user's layer limit, ownership, pending/completed edit restrictions, rendered-media invalidation and MIDI pitch/role validation.
- Changing to a teammate's guitar retains saved notes and the active private draft without rewriting draft content.
- Frontend: 207 tests, ESLint and production build passed. An isolated browser harness loaded the actual instrument dialog with two guitar layers and two vocal layers. The already-used guitar and lead vocal were selectable and explicit confirmation completed. Team usage remains informational. The temporary harness files were deleted before committing; this was a component UI check, not a fresh end-to-end HTTP integration run.

No production database, runtime authentication data or temporary test pages are included in this change.
