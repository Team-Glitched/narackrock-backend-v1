# Studio track visibility

`GET /api/v1/projects/{projectId}/tracks` accepts a `view` query parameter and an optional `contributionId`.

| Request | Visible content |
| --- | --- |
| No parameters, or `view=STUDIO` | Published projects: approved tracks and approved clips only. Private projects: approved content plus the viewer's own working layer. |
| `view=APPROVED` | Approved tracks and approved clips only, including when the viewer owns a pending layer. |
| `view=WORKING` | Approved content plus the current editor's own non-rejected working track and clips. Other people's pending/draft/rejected content is excluded. |
| `contributionId={id}` | Approved content plus the exact track/clip targets in that contribution. No other requests or private working copies are included. |

Contribution lookup is constrained to the requested project. Only the project owner and the request's author can open a contribution's track view; another editor receives 403, and an unknown/project-mismatched request receives 404. Nonpositive contribution IDs receive 400. Clip-only requests on an existing approved track include that requested clip, rather than every pending clip on the track.

Public and contribution views are inspection views: their permission response disables editing/contributing/exporting in that view. Personal editing uses `WORKING`; private projects retain their existing editable workflow under `STUDIO`. A contribution's approval makes the approved targets visible in the ordinary public studio. No database migration or bulk mutation of existing tracks is required.

The frontend must send the selected view, keep separate cache keys for ordinary/working/contribution views, omit private draft overlays from inspection views, and preserve the scope in piano-roll and return links. The companion frontend branch is `fix/studio-approved-visibility`. Deploy backend and frontend together, with the backend first.

Validation: unit coverage for approved/working/request views, exact clip selection, denied request access, private editing and post-approval visibility; real HTTP coverage for default public visibility and imported draft isolation. A local Spring/H2 and Vite run used four disposable users with two approved layers and separate piano/bass requests to verify those views, piano-roll return links, same-project cache transitions and approval. No production data was changed.
