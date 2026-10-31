# NOVA interface contract

## Direction

NOVA is a local Android assistant. Its interface centers on requests, files, actions, and runtime readiness. Use a minimal One UI–inspired light interface: a soft grey canvas, rounded white surfaces, restrained blue actions, and native system typography. Use quiet borders and one clear primary action rather than repeated technical diagrams or simulated hardware readouts.

This is a refinement of the existing design system. Taste Skill informs restraint and hierarchy; Avoid AI Design informs product-specific structure and clear copy; UI UX Pro Max informs Android accessibility, native controls, and safe areas. Web-specific type, animation, and grid instructions are adapted to Compose rather than copied verbatim.

## Tokens

- Canvas: #F4F5F7; cards: #FFFFFF.
- Primary action: #245BC4, with white text.
- Selection container: #E8EFFF, with #245BC4 content.
- Primary text: #202124; secondary text: #626873.
- Quiet card border: #E1E5EC; dividers: #DCE1E9.
- Error: #BA1A1A. Accompany status colors with explicit text.
- System sans-serif supports platform font scaling; no downloaded font dependency.
- Headings: 28sp/36sp and 24sp/32sp; body: 16sp/24sp; labels: 12â€“14sp.
- Spacing: 8dp small, 16dp card inset, 20dp page gutter/section gap, 32dp major separation.
- 24dp rounded cards with quiet 1dp borders, pill buttons, and no decorative shadows or ambient animation. Blue is reserved for actions and selection; red is reserved for errors.

## Shared behavior

Use one bottom navigation with five destinations: Home, Files, Actions, Ask, Settings. The top header shows brand, current destination, commands shortcut, and profile control. Native NavigationBarItem conveys selected state; decorative icons beside labels have no redundant descriptions.

Reserve bottom navigation space through layout measurement, not a guessed overlay height. Respect status/navigation bars and the keyboard. Constrain main content to 840dp on large displays. Home uses one lazy scrolling column, with full-width controls that support long labels and font scaling. Screen changes do not crossfade multiple expensive trees.

Buttons have button semantics, ripple feedback, and at least 48dp height. Toggle rows expose switch state and make the complete row tappable. Shared switches retain native semantics and minimum interaction size. Disabled command submission matches processing/recording state. Preserve typed command text across configuration changes.

Home displays runtime readiness, indexing progress, session uptime, module preferences, and actual recent activity. Do not display generated NPU statistics, assumed model names, or calculated GB figures as measurements. Telemetry now uses persisted database counts and measured vector-search/command timings. Unsupported hardware metrics are explicitly unavailable; model status reflects file availability.

## Verification

Compile the app and package a debug APK. Review activity text contrast, touch targets, selection semantics, disabled states, and expandable result callbacks. Phone rendering, landscape, largest font scale, TalkBack, keyboard behavior, and system animation settings need a connected Android device or emulator. This pass keeps the existing light theme; app-wide dark mode is not implemented.

## Reference sources

- https://github.com/Leonxlnx/taste-skill
- https://github.com/funboy322/avoid-ai-design
- https://github.com/nextlevelbuilder/ui-ux-pro-max-skill
