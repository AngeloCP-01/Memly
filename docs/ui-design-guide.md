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

## UX Reference Patterns

Inspired by Ronas IT's meditation and travel app designs. These patterns inform how Memly displays content.

### Image-Dominant Cards
Memory cards are **photo-first**. The image fills the card and text floats over a gradient overlay at the bottom. This makes the timeline feel like a visual journal, not a data list.

### Gradient Overlay on Images
A **dark gradient** (black, 0% opacity at top to 60% opacity at bottom) sits over the bottom third of the card image. This ensures title, date, and mood text is always readable regardless of the photo behind it.

### Portrait Aspect Ratio
Memory cards use a **3:4 portrait aspect ratio** for the image area. This matches how most phone photos are taken and gives images room to breathe.

### Horizontal Carousels
Secondary content ("On This Day" memories, highlights, recent collections) appears in **horizontal scroll carousels** above or below the main feed. Cards in carousels are smaller (width ~200dp, 4:5 ratio).

### Generous Whitespace
Screens should feel calm and breathable. Use 16dp+ gaps between cards, 24dp between sections. Never pack content tightly — this is a personal memory app, not a news feed.

### Full-Bleed Detail Header
The memory detail screen opens with the **photo filling the top half** of the screen (no side padding), with metadata flowing below it. If multiple photos exist, they are in a swipeable horizontal pager.

### Stat Cards
Small rounded cards showing a single metric (memory count, current streak, top mood). Used on analytics/dashboard screens. Clean, one number + one label per card.

---

## Components

### Memory Card (Timeline)

The memory card is the primary content unit. It is **image-dominant with overlay text**.

- **Corner radius:** 16dp (Large)
- **Elevation:** 0dp (flat) — relies on rounded corners and image fill for visual separation, not shadow
- **Image area:** Fills the entire card. Aspect ratio **3:4 portrait**. Loaded via Coil AsyncImage with crossfade.
- **Gradient overlay:** Linear gradient from transparent at top to black at 60% opacity at bottom, covering the lower third of the image.
- **Content over gradient (bottom of card):**
  - Title (Poppins SemiBold, 16sp, white) — left-aligned, bottom
  - Date (Inter Regular, 12sp, white at 80% opacity) — below title
  - Mood chip: small pill with mood color background + white label text, positioned top-right of card
  - Place label (Inter Regular, 12sp, white at 80% opacity) — next to date, with a pin icon
- **Tag chips:** Shown below the image area (outside the overlay), max 3 visible + "+N" overflow
- **No-image fallback:** If no media, show a solid surface-variant background with mood color accent strip at top, title and notes displayed as plain text card
- **Interaction:** Tap navigates to detail. Subtle scale-down to 0.97 on press.

### Memory Card (Carousel Variant)

Used in horizontal scroll sections ("On This Day", highlights).

- **Width:** ~180dp fixed
- **Aspect ratio:** 4:5 portrait
- **Corner radius:** 12dp (Medium)
- **Same gradient overlay pattern** but simplified: only title + date over gradient
- **No tag chips** (too small)

### Memory Card (Search Result Variant)

Horizontal layout for search results and collection contents.

- **Layout:** Row — small square thumbnail (80dp x 80dp, 12dp radius) on the left, text on the right
- **Text:** Title, date, mood chip, place label
- **No gradient overlay** (image is small, text is beside it)

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

### Memory Detail Header

The detail screen opens with a full-bleed photo hero.

- **Image:** Full-width, no horizontal padding. Aspect ratio 3:4 or fills ~50% of screen height.
- **Multiple photos:** Horizontal pager (swipeable) with page indicator dots.
- **Back button:** Semi-transparent circular button overlaid on top-left of image.
- **Mood chip:** Overlaid on top-right of image (same style as timeline card).
- **Below image:** Title, date, place label, notes, tags — all in a scrollable column with 16dp horizontal padding.
- **Edit/Delete:** Toolbar icons or FAB for edit mode.

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
