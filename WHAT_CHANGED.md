# What changed in this redesign

## Bugs fixed (these were breaking existing features, not just cosmetic)

1. **The widget's gradient background was being wiped out.** The Android code set
   a flat solid color on top of the pretty gradient drawable every time the widget
   refreshed, which is why it never looked like your reference image. Fixed —
   the widget now paints a real two-color gradient card that matches your chosen
   in-app theme exactly.

2. **"Show Progress Bar" and "Pulse Animation" in Widget Settings did nothing.**
   The switches saved to storage but the app never actually read them when
   building the widget. Fixed — both now genuinely control the widget.

3. **The ring wasn't a real circle.** It was a horizontal progress bar rotated
   90°, layered on top of a drawable that was *also* rotated — a double-rotation
   bug — and it would stretch into an oval on the widget's actual (wide, short)
   shape. It's now drawn as a perfect circle at any widget size.

4. **The "custom photo" widget background option did nothing.** You could pick
   a photo in Settings, but it was never sent to the widget. It now works, with
   a dark overlay automatically applied so text stays readable.

## What's new

- **Two new themes:** *Amoled Noir* (true black, battery-friendly, neon teal/violet)
  and *Golden Hour* (amber → coral). They show up automatically in Settings →
  App Theme, alongside your existing five.
- **A real High Contrast mode.** It now pushes text, borders, and dividers to
  true black/white throughout the whole app — not just a light tint change —
  and it also applies to both home-screen widgets.
- **A genuine circular progress ring** on the widget, drawn natively so it looks
  crisp and correctly circular no matter how you resize the widget.
- **A soft glow "pulse"** on the widget ring for events under 24 hours away,
  and a real, smoothly-animating pulse on the countdown cards inside the app.

## One honest limitation

Android home-screen widgets are static snapshots — they physically can't run a
continuous animation without a background service that would drain your
battery, which this app deliberately avoids. So the widget's pulse is a
"breathing glow" that varies each time the widget refreshes (opening the app,
editing an event, or its periodic background sync), while the countdown cards
*inside* the app pulse smoothly and continuously, since that's real Flutter
animation. This is a platform limit, not something any code change can get
around without hurting battery life.

## Files touched

- `lib/theme/app_themes.dart` — new themes, real high-contrast, widget palette helper
- `lib/services/widget_service.dart` — sends full gradient + flags, respects toggles
- `lib/event_card.dart` + new `lib/WIDGET/pulsing_progress_ring.dart` — real animated ring
- `lib/screens/settings_screen.dart`, `lib/screens/widget_settings_screen.dart` — keep both widgets in sync on every relevant change
- `android/.../WidgetArtRenderer.kt` (new) — draws the gradient card + circular ring
- `android/.../EventCountdownWidgetProvider.kt`, `MainActivity.kt` — wired to the new renderer, with a safe fallback if anything ever fails
- `android/.../res/layout/event_widget_layout.xml`, `pomodoro_widget_layout.xml` — new layout structure

Nothing else was touched — database, notifications, backups, Pomodoro logic,
and all other screens are untouched and behave exactly as before.
