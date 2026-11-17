// Stubs (tests first): finding and fetching a service's own icon.
export function findIconCandidates() { return [{ url: 'https://stub/icon.png', size: 1 }]; }
export function chooseIcon(candidates) { return candidates[candidates.length - 1]; }
export async function fetchServiceIcon() { return 'data:stub'; }
