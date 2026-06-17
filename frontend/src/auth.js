import Keycloak from 'keycloak-js'

// Kết nối tới Keycloak (server :8081, realm coford, client public coford-web)
export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8081',
  realm: 'coford',
  clientId: 'coford-web',
})

export function hasRole(role) {
  return keycloak.tokenParsed?.realm_access?.roles?.includes(role) ?? false
}

export function currentUser() {
  const t = keycloak.tokenParsed
  if (!t) return { name: '', username: '', role: '' }
  return {
    name: t.name || t.preferred_username || '',
    username: t.preferred_username || '',
    role: hasRole('MANAGER') ? 'Quản lý' : 'Nhân viên',
  }
}

export function logout() {
  keycloak.logout({ redirectUri: window.location.origin })
}
