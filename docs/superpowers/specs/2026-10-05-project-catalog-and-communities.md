# Project catalogue and communities design

## Goal

Make the first production app session useful by showing TNHC projects, public
communities, and genuine project updates instead of empty placeholder screens.

## Source and curation rules

- Use the 32 code-bearing Nexus entries from the company ecosystem register: 6
  Live, 1 Beta, and 25 In development. The register was measured on
  2026-08-19; retain that date as the status source date where current status
  has not been rechecked.
- Add DevTrack 1.0 as a member-owned released project from Zajfan's
  2026-10-02 release post.
- Exclude the 82 Nexus Scaffold/Stub entries and personal test, trail,
  research, and upstream dependency repositories. In particular, the local
  StrudelLang checkout is an upstream Codeberg project, not a TNHC product.
- Every project record links to its evidence source. Do not invent downloads,
  release dates, contributors, or project activity.
- Create a small set of public topic communities around the real TNHC projects.
  Seed one authentic DevTrack release update with a link to the original post;
  do not create synthetic users or engagement.

## Product behavior

- Projects remain server-owned Supabase records. The member app lists them,
  filters by category/status, shows detail, and offers website/source links.
- The Community destination lists public topics. Signed-in active members can
  join or leave public topics, read visible posts, and publish to topics they
  joined. Server RLS and author-attribution triggers remain authoritative.
- Founder project editing must preserve source URLs and the explicit
  `in_development` stage.
- Empty, loading, and network-error states stay distinct. A public topic with
  no posts explains how to start a discussion.

## Boundaries

This slice does not add direct messaging, opportunities, marketplace listings,
video calls, imported member identities, or fake demo activity. Those are
separate product slices. Production data is limited to the approved 33 project
records, initial public topic records, and the one sourced DevTrack release
post.

## Validation

- Database tests cover project URL validation, status values, seeded catalogue
  counts, topic visibility, and the sourced post's real Founder author.
- Android unit and device tests cover project links/status display, topic
  listing, joining/leaving, reading and creating posts, and loading/error/empty
  recovery.
- The release APK contains only the public HTTPS backend URL and publishable
  key; no service-role or mail-provider credential.
