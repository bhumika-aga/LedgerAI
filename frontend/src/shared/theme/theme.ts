import type { Theme } from "@mui/material";
import { createTheme } from "@mui/material";

/**
 * Design-system composition point (DESIGN_SYSTEM.md, UI_GUIDELINES.md).
 *
 * The application consumes the design system through this Material UI theme. It is intentionally the
 * library default: the concrete design tokens (color, spacing, typography, radii) are the single
 * source of visual truth defined in the design documents and are applied here when the design system
 * is implemented. No tokens, palette, or typography are invented in this scaffold.
 */
export const theme: Theme = createTheme();
