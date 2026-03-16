# Memly UI Design Guide

## Brand Identity

- **App Name:** Memly
- **Tagline:** "Capture. Relive. Remember."
- **Core Values:**
  - Nostalgic but modern
  - Simple, clean, minimalistic
  - Emotional, personal, reflective
- **Tone:** Friendly and warm, soft nostalgia, trustworthy

---

## Color Palette

### Light Theme (Primary)

| Role              | Color       | Hex       | Usage                                        |
| ----------------- | ----------- | --------- | -------------------------------------------- |
| Primary           | Soft Coral  | `#FF6B6B` | Buttons, FAB, primary accents, active bottom nav |
| Primary Container | Light Coral | `#FFE0E0` | Chip backgrounds, selected states            |
| Secondary         | Soft Teal   | `#4BC0C8` | Links, highlights, secondary actions         |
| Secondary Container | Light Teal | `#E0F7FA` | Tag chip backgrounds                        |
| Background        | Off-White   | `#FAFAFA` | Screen backgrounds                           |
| Surface           | Warm Beige  | `#FFF3E6` | Cards, bottom sheets, dialogs                |
| Surface Variant   | Light Beige | `#F5EDE3` | Alternate card backgrounds                   |
| On Background     | Charcoal    | `#333333` | Primary text                                 |
| On Surface        | Dark Gray   | `#444444` | Card text                                    |
| On Surface Variant | Medium Gray | `#777777` | Secondary text, captions                    |
| Outline           | Light Gray  | `#E0E0E0` | Dividers, borders                            |
| Error             | Soft Red    | `#E53935` | Error states                                 |

### Dark Theme

| Role              | Color            | Hex       | Usage                                             |
| ----------------- | ---------------- | --------- | ------------------------------------------------- |
| Primary           | Soft Coral       | `#FF8A8A` | Buttons, FAB (slightly lighter coral for dark bg)  |
| Primary Container | Dark Coral       | `#5C2A2A` | Chip backgrounds                                  |
| Secondary         | Soft Teal        | `#80DEEA` | Links, highlights                                 |
| Secondary Container | Dark Teal      | `#1A3A3D` | Tag chip backgrounds                              |
| Background        | Deep Charcoal    | `#121212` | Screen backgrounds                                |
| Surface           | Dark Surface     | `#1E1E1E` | Cards, bottom sheets                               |
| Surface Variant   | Elevated Surface | `#2A2A2A` | Alternate cards                                    |
| On Background     | Off-White        | `#F0F0F0` | Primary text                                       |
| On Surface        | Light Gray       | `#E0E0E0` | Card text                                          |
| On Surface Variant | Medium Gray     | `#999999` | Secondary text                                     |
| Outline           | Dark Gray        | `#3A3A3A` | Dividers                                           |

### Mood Colors (Shared Across Both Themes)

| Mood        | Color        | Hex       | Label          |
| ----------- | ------------ | --------- | -------------- |
| Happy       | Warm Yellow  | `#FFD93D` | Bright, sunny  |
| Nostalgic   | Soft Amber   | `#FFB347` | Warm, golden   |
| Adventurous | Teal         | `#4BC0C8` | Exploration    |
| Calm        | Soft Blue    | `#74B9FF` | Peaceful       |
| Excited     | Coral Orange | `#FF6348` | Energetic      |
| Grateful    | Soft Green   | `#7ED6A8` | Growth         |
| Romantic    | Rose Pink    | `#FF85A1` | Warm pink      |
| Reflective  | Lavender     | `#A29BFE` | Contemplative  |
| Sad         | Muted Blue   | `#778CA3` | Subdued        |
| Funny       | Bright Lime  | `#BADC58` | Playful        |

---

## Typography

### Font Families

- **Headings:** Poppins Bold -- used for screen titles and memory titles.
- **Subheadings:** Poppins SemiBold -- used for section headers and card titles.
- **Body:** Inter Regular -- used for descriptions, notes, and captions.
- **Buttons / Labels:** Inter Medium -- used for action text, chips, and labels.

### Type Scale

| Level    | Size  | Font            | Usage                              |
| -------- | ----- | --------------- | ---------------------------------- |
| Display  | 32sp  | Poppins Bold    | Hero text, onboarding titles       |
| Headline | 24sp  | Poppins Bold    | Screen titles                      |
| Title    | 20sp  | Poppins SemiBold | Section headers, card titles      |
| Body     | 16sp  | Inter Regular   | Descriptions, notes, body text     |
| Label    | 14sp  | Inter Medium    | Buttons, chips, form labels        |
| Caption  | 12sp  | Inter Regular   | Timestamps, metadata, helper text  |

### Line Height

All body and caption text should use a line height multiplier between **1.4** and **1.6** for comfortable readability.

---

## Spacing and Layout

### Screen-Level Spacing

| Property        | Value | Notes                                 |
| --------------- | ----- | ------------------------------------- |
| Screen padding  | 16dp  | Horizontal padding on all screens     |
| Card padding    | 16dp  | Internal padding within cards         |
| Card gap        | 12dp  | Vertical spacing between cards        |
| Section gap     | 24dp  | Vertical spacing between sections     |

### Corner Radius Scale

| Size   | Radius | Usage                                 |
| ------ | ------ | ------------------------------------- |
| Small  | 8dp    | Chips, small buttons, text fields     |
| Medium | 12dp   | Standard cards, containers            |
| Large  | 16dp   | Memory cards, image containers        |
| XL     | 20dp   | Dialogs, bottom sheets               |

### Touch Targets

All interactive elements must have a minimum touch target size of **48dp x 48dp**, following Material Design accessibility guidelines.

---

## Components

### Memory Card

The memory card is the primary content unit across the app.

- **Corner radius:** 16dp (Large)
- **Elevation:** 2dp in light mode; elevated surface color (`#2A2A2A`) in dark mode
- **Image area:** Thumbnail image displayed prominently when media exists. Aspect ratio should be 16:9 or square, depending on layout context.
- **Content layout:**
  - Title (Poppins SemiBold, Title size)
  - Date (Inter Regular, Caption size, on-surface-variant color)
  - Mood indicator: small colored dot or chip using the corresponding mood color
  - Place label (Inter Regular, Caption size), shown when location data is available
  - Tag chips: display a maximum of 3 visible chips; overflow is shown as a "+N more" label
- **Interaction:** Tapping the card navigates to the memory detail screen.

### Mood Selector

Used during memory creation and editing to assign an emotional tone.

- Layout: `FlowRow` of `FilterChip` components
- Each chip uses its mood color as a background tint with the mood label as text
- Selection mode: single selection only
- Selected state: stronger color fill with higher opacity
- Unselected state: outlined or lightly tinted

### Tag Chips

Used to categorize and filter memories.

- Background: secondary container color (`#E0F7FA` light / `#1A3A3D` dark)
- Text: Inter Medium, Label size
- Editable mode: includes a dismiss icon (X) for removal
- Display mode: compact sizing for use within memory cards
- Corner radius: Small (8dp)

### FAB (Floating Action Button)

The primary action trigger for creating a new memory.

- Color: Primary coral (`#FF6B6B` light / `#FF8A8A` dark)
- Icon: "+" (add) icon in white
- Position: bottom-end of the screen
- Visibility: shown on main screens (Timeline, Map, Search); hidden on full-screen routes such as capture and detail views

### Bottom Navigation

Provides top-level navigation between the three main sections.

- **Tabs:** Timeline, Map, Search
- **Active tab:** Primary coral icon with label text
- **Inactive tab:** On-surface-variant colored icon with label text
- **Label display:** Labels can be hidden on inactive tabs for a cleaner appearance, configurable by user preference
- **Elevation:** Surface-level background; no visible shadow

### Empty States

Shown when a screen has no content to display.

- **Layout:** Centered vertically and horizontally
- **Illustration area:** Reserved space for a future illustration or icon placeholder
- **Headline:** Concise message such as "No memories yet" or "No results found" (Poppins SemiBold, Title size)
- **Body text:** Brief guidance explaining what the user can do next (Inter Regular, Body size, on-surface-variant color)
- **CTA button (optional):** A primary-styled button to guide the user toward an action, such as "Create your first memory"

### Dialogs

Used for confirmations, destructive actions, and focused input.

- Background: Surface color (`#FFF3E6` light / `#1E1E1E` dark)
- Scrim: Semi-transparent dark overlay behind the dialog
- Corner radius: XL (20dp)
- Structure:
  - Title (Poppins SemiBold, Title size)
  - Body text (Inter Regular, Body size)
  - Action buttons aligned to the bottom-end
- Destructive actions: Use the error color (`#E53935`) for destructive button labels

---

## Animations and Interactions

### Card Interactions

- **Tap feedback:** Subtle scale-down press effect (scale to approximately 0.97) with a quick ease-out return.
- **Timeline scrolling:** Smooth and gentle scrolling behavior with no abrupt snapping.

### Loading and Transitions

- **Pull-to-refresh:** Available on the timeline screen with a standard Material pull indicator.
- **Thumbnail loading:** Fade-in crossfade effect using Coil's built-in crossfade transition.
- **Screen transitions:** Shared element transitions where appropriate (e.g., card image to detail header image).

### Map Interactions

- **Pin appearance:** Pop-in animation when map pins become visible.
- **Pin tap:** Subtle bounce or scale effect on tap, followed by displaying a preview card.

### Component-Level Animations

- **Mood chip selection:** Smooth color transition (approximately 300ms) when toggling between selected and unselected states.
- **Photo gallery:** Horizontal swipe gesture to navigate between photos in the detail view.
- **FAB:** Gentle show/hide animation tied to scroll direction (hide on scroll down, show on scroll up).

---

## App Icon Concept

- **Shape:** A simple rounded photo frame or memory pin incorporating a heart motif
- **Colors:** Primary coral (`#FF6B6B`) combined with warm beige (`#FFF3E6`)
- **Format:** Adaptive icon compatible, with separate foreground and background layers following Android adaptive icon specifications

---

## Logo

- **Style:** Wordmark reading "Memly" set in Poppins Bold
- **Accent:** The letter "M" features a coral-colored accent to tie into the primary brand color
- **Scalability:** Designed to remain legible and recognizable when scaled down to app icon size
- **Usage:** Appears on the splash screen, onboarding flow, and about/settings screens

---

## Design Principles Summary

1. **Warmth first.** Every design decision should reinforce the feeling of revisiting cherished memories. Warm colors, soft edges, and gentle interactions contribute to this tone.
2. **Content is king.** The user's photos, notes, and emotions are the focus. The UI should frame content, not compete with it.
3. **Simplicity over complexity.** Minimize cognitive load. Use progressive disclosure to reveal details only when needed.
4. **Consistency.** Adhere to the spacing, color, and typography scales defined in this guide to maintain a cohesive experience across all screens.
5. **Accessibility.** Maintain sufficient contrast ratios, respect minimum touch target sizes, and support both light and dark themes.
