#!/usr/bin/env bash
# =============================================================================
# Mühür - Tum Modulleri Build Scripti (Bash)
# =============================================================================
# 1. Tum modullerde mvn clean
# 2. common modulunde mvn clean install
# 3. Diger tum modullerde mvn clean package
# =============================================================================

set -euo pipefail

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
else
    failure_msg "Maven bulunamadi! PATH'de mvn oldugundan emin olun."
    exit 1
fi

# -----------------------------------------------------------------------------
# Phase 1: Tum modullerde clean
# -----------------------------------------------------------------------------
step_header "Phase 1: Tum modullerde mvn clean"

for module in "${ALL_MODULES[@]}"; do
    info_msg "Cleaning: $module"

    if [ ! -f "$module/pom.xml" ]; then
        failure_msg "$module/pom.xml bulunamadi, atlaniyor..."
        FAILED_MODULES+=("$module")
        continue
    fi

    pushd "$module" > /dev/null
    if mvn clean; then
        success_msg "Clean tamam: $module"
    else
        failure_msg "Clean basarisiz: $module"
        FAILED_MODULES+=("$module")
    fi
    popd > /dev/null
done

if [ ${#FAILED_MODULES[@]} -gt 0 ]; then
    echo -e "\n${YELLOW}Bazi modullerde clean basarisiz oldu, devam ediliyor...${NC}"
fi

# -----------------------------------------------------------------------------
# Phase 2: common modulunu install et
# -----------------------------------------------------------------------------
step_header "Phase 2: common modulunde mvn clean install"

if [ ! -f "$COMMON_MODULE/pom.xml" ]; then
    failure_msg "$COMMON_MODULE/pom.xml bulunamadi!"
    exit 1
fi

pushd "$COMMON_MODULE" > /dev/null
if mvn clean install; then
    success_msg "common install basarili"
    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
else
    failure_msg "common install basarisiz! Build durduruldu."
    popd > /dev/null
    exit 1
fi
popd > /dev/null

# -----------------------------------------------------------------------------
# Phase 3: Diger tum modulleri package et
# -----------------------------------------------------------------------------
step_header "Phase 3: Servis modullerinde mvn clean package"

for module in "${SERVICE_MODULES[@]}"; do
    info_msg "Building: $module"

    if [ ! -f "$module/pom.xml" ]; then
        failure_msg "$module/pom.xml bulunamadi, atlaniyor..."
        FAILED_MODULES+=("$module")
        continue
    fi

    pushd "$module" > /dev/null
    if mvn clean package; then
        success_msg "Package basarili: $module"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        failure_msg "Package basarisiz: $module"
        FAILED_MODULES+=("$module")
    fi
    popd > /dev/null
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
fi

exit 0
