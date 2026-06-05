#!/usr/bin/env bash
# =============================================================================
# Mühür - Tum Modulleri Build Scripti (Bash, PARALEL + TESTSIZ)
# =============================================================================
# 1. common modulunde mvn clean install -DskipTests  (BARRIER: digerleri buna bagimli)
# 2. Diger tum modullerde mvn clean package -DskipTests  -> PARALEL
#
# Ortam degiskeni:
#   MAX_PARALLEL : ayni anda kosacak modul sayisi (varsayilan: CPU cekirdek sayisi)
#                  Ornek: MAX_PARALLEL=4 bash build-all.sh
# =============================================================================

# Not: paralel job + 'wait' ile -e erken cikmaya sebep oldugu icin -e kullanilmiyor;
# hatalar her modulun status dosyasindan acikca toplaniyor.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Modul listesi
COMMON_MODULE="common"
SERVICE_MODULES=(
    "eureka-server"
    "api-gateway"
    "ai-service"
    "auth"
    "notification"
    "audit"
    "contract"
    "signature"
    "template"
    "workflow"
)

ALL_MODULES=("$COMMON_MODULE" "${SERVICE_MODULES[@]}")
MVN_FLAGS=(-DskipTests)
MAX_PARALLEL="${MAX_PARALLEL:-$(nproc 2>/dev/null || echo 4)}"
LOG_DIR="$(mktemp -d)"

FAILED_MODULES=()
SUCCESS_COUNT=0
TOTAL_MODULES=${#ALL_MODULES[@]}

# Renk kodlari
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# -----------------------------------------------------------------------------
# Yardimci fonksiyonlar
# -----------------------------------------------------------------------------
step_header() {
    echo ""
    echo -e "${CYAN}============================================================${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}============================================================${NC}"
}

success_msg() {
    echo -e "  ${GREEN}[OK]${NC} $1"
}

failure_msg() {
    echo -e "  ${RED}[FAILED]${NC} $1"
}

info_msg() {
    echo -e "  ${YELLOW}[..]${NC} $1"
}

# -----------------------------------------------------------------------------
# Maven kontrolu
# -----------------------------------------------------------------------------
step_header "Maven Kontrolu"

if command -v mvn &>/dev/null; then
    MVN_VERSION=$(mvn --version 2>&1 | head -1)
    success_msg "Maven bulundu: $MVN_VERSION"
    info_msg "Paralellik (MAX_PARALLEL): $MAX_PARALLEL  |  Testler: ATLANIYOR (-DskipTests)"
else
    failure_msg "Maven bulunamadi! PATH'de mvn oldugundan emin olun."
    exit 1
fi

# -----------------------------------------------------------------------------
# Phase 1: common modulunu install et (BARRIER — servisler buna bagimli)
# -----------------------------------------------------------------------------
step_header "Phase 1: common -> mvn clean install -DskipTests"

if [ ! -f "$COMMON_MODULE/pom.xml" ]; then
    failure_msg "$COMMON_MODULE/pom.xml bulunamadi!"
    exit 1
fi

pushd "$COMMON_MODULE" > /dev/null
if mvn clean install "${MVN_FLAGS[@]}"; then
    success_msg "common install basarili"
    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
else
    failure_msg "common install basarisiz! Build durduruldu."
    popd > /dev/null
    rm -rf "$LOG_DIR"
    exit 1
fi
popd > /dev/null

# -----------------------------------------------------------------------------
# Phase 2: Servis modullerini PARALEL package et
# -----------------------------------------------------------------------------
step_header "Phase 2: Servis modulleri -> mvn clean package -DskipTests (PARALEL: $MAX_PARALLEL)"

# Bir modulu arka planda build eder; ciktiyi log dosyasina, exit kodunu status dosyasina yazar.
launch_build() {
    local module="$1"
    (
        cd "$module" && mvn clean package "${MVN_FLAGS[@]}"
        echo $? > "$LOG_DIR/$module.status"
    ) > "$LOG_DIR/$module.log" 2>&1 &
}

for module in "${SERVICE_MODULES[@]}"; do
    if [ ! -f "$module/pom.xml" ]; then
        failure_msg "$module/pom.xml bulunamadi, atlaniyor..."
        FAILED_MODULES+=("$module")
        continue
    fi

    # Throttle: ayni anda en fazla MAX_PARALLEL job kossun.
    while [ "$(jobs -rp | wc -l)" -ge "$MAX_PARALLEL" ]; do
        wait -n 2>/dev/null || true
    done

    info_msg "Basladi: $module"
    launch_build "$module"
done

# Kalan tum joblarin bitmesini bekle.
wait 2>/dev/null || true

# Sonuclari topla (status dosyalarindan — wait sirasindan bagimsiz, guvenli).
for module in "${SERVICE_MODULES[@]}"; do
    [ -f "$LOG_DIR/$module.status" ] || continue   # pom.xml'i olmayanlar zaten atlandi
    status="$(cat "$LOG_DIR/$module.status")"
    if [ "$status" = "0" ]; then
        success_msg "Package basarili: $module"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        failure_msg "Package basarisiz: $module"
        FAILED_MODULES+=("$module")
        echo -e "  ${YELLOW}--- $module log (son 25 satir) ---${NC}"
        tail -25 "$LOG_DIR/$module.log" | sed 's/^/    /'
    fi
done

# -----------------------------------------------------------------------------
# Ozet
# -----------------------------------------------------------------------------
step_header "Build Ozeti"

echo ""
echo -e "  Toplam modul  : $TOTAL_MODULES"
echo -e "  Basarili      : ${GREEN}$SUCCESS_COUNT${NC}"

if [ ${#FAILED_MODULES[@]} -gt 0 ]; then
    echo -e "  Basarisiz     : ${RED}${#FAILED_MODULES[@]}${NC}"
    echo -e "  Hata alanlar  : ${RED}${FAILED_MODULES[*]}${NC}"
    echo -e "  Loglar        : ${YELLOW}$LOG_DIR${NC} (incelemek icin saklandi)"
    echo ""
    exit 1
else
    echo -e "  Basarisiz     : ${GREEN}0${NC}"
    echo ""
    echo -e "  ${GREEN}Tum moduller basariyla build edildi!${NC}"

    # JAR dosyalarini listele
    echo -e "\n  Olusan JAR dosyalari:"
    for module in "${ALL_MODULES[@]}"; do
        jar_file=$(find "$module/target" -maxdepth 1 -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" ! -name "*original*" 2>/dev/null | head -1)
        if [ -n "$jar_file" ]; then
            jar_name=$(basename "$jar_file")
            if [[ "$(uname -s)" == "Darwin" ]]; then
                size_mb=$(stat -f%z "$jar_file" 2>/dev/null | awk '{printf "%.2f", $1/1048576}')
            else
                size_mb=$(stat -c%s "$jar_file" 2>/dev/null | awk '{printf "%.2f", $1/1048576}')
            fi
            echo -e "    ${CYAN}$module${NC} -> $jar_name (${size_mb} MB)"
        fi
    done
    echo ""
    rm -rf "$LOG_DIR"   # basarili build'de gecici loglari temizle
fi

exit 0
