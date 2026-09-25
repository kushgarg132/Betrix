// Ellipse radii in % of the scene box. Portrait (phones) is a tall oval, landscape a wide one.
const RADII = { portrait: { rx: 40, ry: 42 }, landscape: { rx: 44, ry: 38 } };

/**
 * Screen position of every seat, indexed by actual player index, rotated so the hero sits
 * bottom-center and the rest follow clockwise (the order the action moves).
 */
export function seatLayout(seatCount, heroIndex, orientation = 'portrait') {
  const { rx, ry } = RADII[orientation] || RADII.portrait;
  const anchor = heroIndex >= 0 ? heroIndex : 0;
  return Array.from({ length: seatCount }, (_, i) => {
    const step = (i - anchor + seatCount) % seatCount;
    // Start at the bottom (90° in screen coordinates, y down) and go clockwise on screen.
    const angle = Math.PI / 2 + (step * 2 * Math.PI) / seatCount;
    // `angle` is kept alongside the position so a seat can point something (the bet chip) back
    // toward the table center without re-deriving its place on the ellipse.
    return { left: 50 + rx * Math.cos(angle), top: 50 + ry * Math.sin(angle), angle };
  });
}
