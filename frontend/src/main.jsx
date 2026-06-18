import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'

import '@fontsource/inter/400.css'
import '@fontsource/inter/500.css'
import '@fontsource/inter/600.css'
import '@fontsource/inter/700.css'

import './index.css'
import App from './App.jsx'
import { keycloak } from './auth'

function render() {
  createRoot(document.getElementById('root')).render(
    <StrictMode>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </StrictMode>,
  )
}

// Bắt buộc đăng nhập Keycloak trước khi vào app.
// PKCE S256 cần crypto.subtle — chỉ có trong "secure context" (HTTPS hoặc localhost).
// Khi deploy LAN qua HTTP+IP, crypto.subtle không tồn tại nên bỏ PKCE để init() không vỡ.
const initOptions = { onLoad: 'login-required', checkLoginIframe: false }
if (window.isSecureContext && window.crypto?.subtle) {
  initOptions.pkceMethod = 'S256'
}
keycloak
  .init(initOptions)
  .then((authenticated) => {
    if (!authenticated) {
      keycloak.login()
      return
    }
    // tự gia hạn token
    setInterval(() => {
      keycloak.updateToken(60).catch(() => keycloak.login())
    }, 30000)
    render()
  })
  .catch((e) => {
    document.getElementById('root').innerHTML =
      '<div style="padding:40px;font-family:system-ui">Không kết nối được Keycloak (:8081). Kiểm tra dịch vụ rồi tải lại.</div>'
    console.error(e)
  })
