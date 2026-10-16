# GPClaimBuffer

Enforce a minimum buffer distance between GriefPrevention claims.

Implements the feature requested in
[GriefPrevention discussion #2660](https://github.com/GriefPrevention/GriefPrevention/discussions/2660)
("As a server Admin I want to be able to have default buffer space between claims"),
first raised in
[#1754](https://github.com/GriefPrevention/GriefPrevention/discussions/1754)
and also in
[#768](https://github.com/GriefPrevention/GriefPrevention/discussions/768)
(duplicate of deleted issue #505, listed in the
[FAQ](https://github.com/GriefPrevention/GriefPrevention/discussions/752)
as "Can you enforce a minimum distance between claims?").

No published GPAddons addon implements this (checked October 2026).
The closest existing implementations are forks:
[GriefPrevention3D](https://github.com/castledking/GriefPrevention3D)
(`Claims.MinimumDistance` + neighbor trust) and LemonGaming's
proximity check. This addon covers the core request as a small,
config-driven add-on for the legacy GriefPrevention branch (16.18.x).

## Behavior

* Listens to `ClaimCreatedEvent` and `ClaimResizeEvent`.
* Only top-level claims are checked and only top-level claims count as
  obstacles. Subdivisions are exempt.
* Distance is edge-to-edge in X/Z (block gap between borders).
  Adjacent claims have a gap of 0.
* Creating an administrative claim always bypasses the check.
  Existing admin claims still count as obstacles unless
  `ignore-admin-claims: true`.
* Claims by the same owner are exempt by default (`same-owner-exempt: true`),
  so a player can still expand next to their own claims.
* `gpclaimbuffer.bypass` (op by default) bypasses the check.
* On denial the creator is told whose claim blocked them and the
  conflicting claim is visualized as a `CONFLICT_ZONE`.
* Claim lookup is chunk-scoped around the buffered box, not a full scan.

## Configuration (`config.yml`)

```yaml
minimum-distance: 10
same-owner-exempt: true
ignore-admin-claims: false
```

`minimum-distance: 0` disables the check.

## Permissions

* `gpclaimbuffer.bypass` (op by default) — bypass the buffer check
  when creating or resizing claims.

## Commands

None. All behavior is config-driven (see above).

## Build

Targets legacy GriefPrevention `16.18.7`
(`me.ryanhamshire` package, Java 21):

```sh
mvn package
```

## Install

1. Drop `GPClaimBuffer.jar` next to `GriefPrevention.jar`.
2. Restart, set `minimum-distance` in
   `plugins/GPClaimBuffer/config.yml`, restart or reload.
