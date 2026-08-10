# B.S. Awareness Branding

This folder is the production-oriented branding package derived from the selected alpha mockup.

## Primary identity

The core mark combines three ideas:

1. **Navigation** — the white directional arrow.
2. **Awareness** — the green horizon/arc.
3. **Travel** — the road centerline.

The minimal navy icon is the preferred application icon because it remains legible at launcher, favicon, and Android Auto sizes.

The scenic mountain icon is intentionally retained as an **optional alternate icon** and is recommended for repository/social artwork, splash art, and decorative uses.

## Asset roles

- `master/` — canonical mark geometry.
- `app/` — launcher/alternate app icon treatments.
- `android/` — Android adaptive icon components and Android Auto treatment.
- `web/` — favicons.
- `wordmarks/` — horizontal, stacked, and title lockups.
- `markers/` — BSA-native POI marker family.
- `social/` — GitHub/social-preview artwork.
- `reference/` — selected mockup and palette notes.
- `.old/` — archive location for superseded alpha/mockup assets.

## Typography

Wordmark SVGs use editable system-font text (`Segoe UI`, then `Arial`, then `sans-serif`) so no font files need to be redistributed.

If the project later adopts a dedicated open font, the text can be converted to paths for fully deterministic rendering.

## Iconography

BSA's application chrome should continue to use **Lucide** icons where practical. This branding package only defines BSA-specific GIS/brand symbols that Lucide does not provide.

## Alpha branding policy

Branding is expected to evolve during alpha development. Superseded assets should be moved to `.old/` rather than silently overwritten or deleted when their history remains useful.
