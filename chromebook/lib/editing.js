// Stubs (tests first): the list changes for editing services.
export function siteOf() { return 'stub'; }
export function permissionPattern() { return 'stub'; }
export function serviceFromAddress() { return { name: 'stub', url: 'https://stub/', site: 'stub', colour: '#000' }; }
export function addService(list) { return { list, service: list[0] }; }
export function moveService(list) { return list; }
export function setHidden(list) { return list; }
export function removeService(list) { return list; }
