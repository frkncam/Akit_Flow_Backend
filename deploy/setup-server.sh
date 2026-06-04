#!/usr/bin/env bash
# deploy/setup-server.sh — taze bir Ubuntu 22.04 sunucuyu Mühür icin hazirlar.
#
# Katman 05'te ELLE yaptigimiz Adim 4-10'un idempotent (tekrar calistirilabilir) hali.
# Amac: "yeni sunucuda tek komutla tekrarlanabilir kurulum" — DevOps'un temel fikri.
#
# Calistirma (sunucuda, root olarak ILK kurulumda):
#   ssh root@<ip> 'bash -s' < deploy/setup-server.sh
# veya sunucuya kopyalayip: sudo bash setup-server.sh
#
# NOT: SSH anahtari sunucu olusturulurken (Hetzner) root'a eklenmis olmali.
set -euo pipefail

DEPLOY_USER="deploy"
APP_DIR="/opt/akitflow"

echo "==> 1/7 Sistem guncelleme"
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get upgrade -y

echo "==> 2/7 deploy kullanicisi (key-only login + NOPASSWD sudo)"
if ! id "${DEPLOY_USER}" &>/dev/null; then
  useradd -m -s /bin/bash "${DEPLOY_USER}"
fi
usermod -aG sudo "${DEPLOY_USER}"
echo "${DEPLOY_USER} ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/90-${DEPLOY_USER}
chmod 440 /etc/sudoers.d/90-${DEPLOY_USER}
visudo -c -f /etc/sudoers.d/90-${DEPLOY_USER}
# root'un SSH anahtarini deploy'a tasi (key ile girebilsin)
if [ -f /root/.ssh/authorized_keys ]; then
  mkdir -p /home/${DEPLOY_USER}/.ssh
  cp /root/.ssh/authorized_keys /home/${DEPLOY_USER}/.ssh/authorized_keys
  chown -R ${DEPLOY_USER}:${DEPLOY_USER} /home/${DEPLOY_USER}/.ssh
  chmod 700 /home/${DEPLOY_USER}/.ssh
  chmod 600 /home/${DEPLOY_USER}/.ssh/authorized_keys
fi

echo "==> 3/7 SSH sertlestirme (root login + parola kapali)"
cat > /etc/ssh/sshd_config.d/99-hardening.conf <<'EOF'
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
EOF
sshd -t
systemctl restart ssh

echo "==> 4/7 Firewall (ufw): sadece 22/80/443"
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

echo "==> 5/7 fail2ban"
apt-get install -y fail2ban
systemctl enable --now fail2ban

echo "==> 6/7 Docker + compose plugin"
if ! command -v docker &>/dev/null; then
  curl -fsSL https://get.docker.com | sh
fi
usermod -aG docker "${DEPLOY_USER}"
systemctl enable --now docker

echo "==> 7/7 Proje dizini + 2 GB swap"
mkdir -p "${APP_DIR}"
chown ${DEPLOY_USER}:${DEPLOY_USER} "${APP_DIR}"
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo
echo "==> TAMAM. Sonraki adimlar (elle/CD):"
echo "   1) ${APP_DIR}'e kopyala: docker-compose.prod.yml, init-db/, keys/ (RSA), .env.prod"
echo "   2) docker login ghcr.io -u <kullanici> --password-stdin   (read:packages PAT)"
echo "   3) cd ${APP_DIR} && docker compose -f docker-compose.prod.yml --env-file .env.prod up -d"
