# Instrument families and recorded roles

Track creation now requires a known sampled instrument or recorded role. `InstrumentFamily` defines musical families: piano/electric piano share a family; nylon/acoustic guitar share a family; clean/overdrive/distortion electric guitar share another; bass tones share a family. Acoustic guitar and electric guitar remain separate. Lead/backing vocal/rap share the vocal family. The frontend's grouped picker uses the same mapping.

`TrackCreateUseCase` rejects a family already reserved by a non-deleted, non-rejected track with HTTP 409 `INSTRUMENT_FAMILY_TAKEN`. Draft and pending assignments reserve their families. Legacy uppercase/Korean aliases map to the same families. Unknown new assignments and unclassified audio creation are rejected; existing data is not automatically relabelled.

## Change API

`PATCH /api/v1/projects/{projectId}/tracks/{trackId}/instrument`

```json
{"instrument":"electric-guitar"}
```

Only the track's authenticated owner can change an editable DRAFT/APPROVED track in an unfinished project. The current track is excluded from the reservation check, allowing tone changes within its own family. Creation and updates acquire the same project write lock, so competing family claims cannot both succeed.

The API changes instrument metadata without rewriting note data or audio contents. It checks saved clips and the owner's active private drafts before switching to drums (MIDI 35–81) or to an audio-only role. MIDI notes cannot be assigned a vocal/other audio-only role. MIDI creation, editing and draft validation reject such roles instead of rendering them as piano. Audio clip creation requires a classification. Approved changes invalidate rendered track media, update project version and queue a published mix refresh.

No schema migration is needed. The frontend companion is `refector/piano-roll-studio`; deploy both together because creation now requires an instrument/role.

## Verification

- TDD: duplicate-tone family creation test failed before implementation; instrument-change tests initially failed for the missing use case; missing-role audio test failed before adding its guard.
- Java 21 `./gradlew test`: 1,066 tests, zero failures/errors/skips.
- Real isolated local-test projects 6/7: preview did not create a track; confirmation created the chosen track; tone changes preserved three draft notes and undo history.
- Conflicting families returned 409 and another owner's track returned 403. Concurrent synth-lead/synth-pad reservations returned exactly one 200 and one 409.
- Actual browser WAV selection/trim/role confirmation stored a 3,000ms AUDIO clip as vocal. Changing to backing vocal retained the original clip; another player's rap selection returned 409.
- Physical microphone capture and live HTI conversion were not exercised. The selected HTI output sound has frontend unit coverage.

All runtime data is isolated under the existing local TEST H2 directory. No production database or credentials are included in this change.
