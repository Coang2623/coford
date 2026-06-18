# Deploy Coford lên VM Ubuntu (Docker Compose, truy cập qua IP)

Toàn bộ stack (backend + frontend + hạ tầng) chạy bằng Docker Compose trên 1 VM, truy cập qua IP LAN (HTTP).

## 1. Cài Docker trên VM (1 lần)
```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER      # để chạy docker không cần sudo
# đăng xuất/đăng nhập lại SSH cho nhóm docker có hiệu lực
```

## 2. Lấy mã nguồn lên VM
```bash
git clone <repo-url> coford && cd coford
# hoặc copy từ máy local: scp -r ./coford user@VM_IP:~/
```

## 3. Cấu hình IP của VM
```bash
ip addr            # xem IP LAN của VM (vd 192.168.1.100)
cd infra
cp .env.example .env
nano .env          # sửa HOST_IP=<IP_VM>, đổi mật khẩu nếu muốn
```
`HOST_IP` rất quan trọng: Keycloak `issuer` + redirect + URL frontend đều bám theo IP này.

## 4. Mở port firewall (nếu bật ufw)
```bash
sudo ufw allow 80,8081,3000,9090,8090,8080/tcp
```

## 5. Build & chạy
```bash
# vẫn ở thư mục infra/
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
```
Lần đầu build backend (Maven) hơi lâu; cần VM tối thiểu ~2GB RAM (khuyến nghị 4GB).

## 6. Truy cập
| Dịch vụ | URL | Đăng nhập |
|---|---|---|
| App (frontend) | https://HOST_IP (tự chấp nhận cảnh báo cert self-signed) | thungan/123456 (STAFF), quanly/123456 (MANAGER) |
| Keycloak admin | https://HOST_IP/kc/admin | admin/admin |
| Grafana | http://HOST_IP:3000 | admin/admin |
| Prometheus | http://HOST_IP:9090 | — |
| Adminer (DB) | http://HOST_IP:8090 | server=postgres, user/pass=coford |
| Backend (debug) | http://HOST_IP:8080/actuator/health | — |

## Lệnh thường dùng
```bash
docker compose -f docker-compose.prod.yml down            # dừng
docker compose -f docker-compose.prod.yml up -d --build    # cập nhật sau khi sửa code
docker compose -f docker-compose.prod.yml logs -f frontend # xem log
docker compose -f docker-compose.prod.yml down -v          # xóa luôn dữ liệu DB (cẩn thận)
```

## Lưu ý
- Sửa code **frontend** rồi muốn cập nhật → phải `--build` lại (Vite nhúng `VITE_KEYCLOAK_URL` lúc build).
- Đổi `HOST_IP` → phải build lại frontend và recreate backend (issuer thay đổi).
- Realm Keycloak chỉ import lần đầu (khi realm chưa tồn tại). Đổi realm sau đó cần `down -v` hoặc import thủ công trong admin.
- redirectUris đặt `"*"` cho tiện học tập/LAN — môi trường thật nên giới hạn đúng origin.
- Postgres/Redis/Kafka **không** mở ra ngoài, chỉ dùng nội bộ mạng Docker.
```
