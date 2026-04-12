/**
 * Generate a placeholder image URL using a simple SVG data URI.
 * Replace these with real images later.
 */
export function placeholder(
	width: number,
	height: number,
	label?: string,
): string {
	const text = label ?? `${width}×${height}`;
	const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}">
    <rect width="100%" height="100%" fill="#1e1e1c"/>
    <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle"
      font-family="Inter, sans-serif" font-size="14" fill="#8a8680">${text}</text>
  </svg>`;
	return `data:image/svg+xml,${encodeURIComponent(svg)}`;
}
