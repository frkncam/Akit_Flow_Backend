# =============================================================================
# AkitFlow - Tum Modulleri Build Scripti
# =============================================================================
# 1. Tum modullerde mvn clean
# 2. common modulunde mvn clean install
# 3. Diger tum modullerde mvn clean package
# =============================================================================

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

# Modul listesi (build sirasina gore)
$CommonModule = "common"
$ServiceModules = @(
    "eureka-server",
    "api-gateway",
    "auth",
    "notification",
    "audit",
    "contract",
    "signature",
    "template",
    "workflow"
)

$AllModules = @($CommonModule) + $ServiceModules
$FailedModules = @()
$SuccessCount = 0
$TotalModules = $AllModules.Count

# -----------------------------------------------------------------------------
# Renkli cikti yardimcilari
# -----------------------------------------------------------------------------
function Write-Step {
    param([string]$Message)
    Write-Host "`n============================================================" -ForegroundColor Cyan
    Write-Host "  $Message" -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "  [OK] $Message" -ForegroundColor Green
}

function Write-Failure {
    param([string]$Message)
    Write-Host "  [FAILED] $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "  [..] $Message" -ForegroundColor Yellow
}

# -----------------------------------------------------------------------------
# Maven kontrolu
# -----------------------------------------------------------------------------
Write-Step "Maven Kontrolu"

try {
    $mvnVersion = mvn --version 2>&1 | Select-Object -First 1
    Write-Success "Maven bulundu: $mvnVersion"
}
catch {
    Write-Failure "Maven bulunamadi! PATH'de mvn oldugundan emin olun."
    exit 1
}

# -----------------------------------------------------------------------------
# Phase 1: Tum modullerde clean
# -----------------------------------------------------------------------------
Write-Step "Phase 1: Tum modullerde mvn clean"

foreach ($module in $AllModules) {
    Write-Info "Cleaning: $module"

    if (-not (Test-Path "$module\pom.xml")) {
        Write-Failure "$module/pom.xml bulunamadi, atlaniyor..."
        $FailedModules += $module
        continue
    }

    Push-Location $module
    try {
        $output = mvn clean 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Clean tamam: $module"
        }
        else {
            Write-Failure "Clean basarisiz: $module"
            $FailedModules += $module
        }
    }
    catch {
        Write-Failure "Clean hatasi: $module -> $_"
        $FailedModules += $module
    }
    finally {
        Pop-Location
    }
}

if ($FailedModules.Count -gt 0) {
    Write-Host "`nBazi modullerde clean basarisiz oldu, devam ediliyor..." -ForegroundColor Yellow
}
$CleanFailed = $FailedModules.Count

# -----------------------------------------------------------------------------
# Phase 2: common modulunu install et
# -----------------------------------------------------------------------------
Write-Step "Phase 2: common modulunde mvn clean install"

if (-not (Test-Path "$CommonModule\pom.xml")) {
    Write-Failure "$CommonModule/pom.xml bulunamadi!"
    exit 1
}

Push-Location $CommonModule
try {
    $output = mvn clean install 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Success "common install basarili"
        $SuccessCount++
    }
    else {
        Write-Failure "common install basarisiz!"
        Write-Host "`n--- Maven ciktisi (son 30 satir) ---" -ForegroundColor Red
        $output | Select-Object -Last 30 | ForEach-Object { Write-Host $_ -ForegroundColor Red }
        Pop-Location
        exit 1
    }
}
catch {
    Write-Failure "common install hatasi: $_"
    Pop-Location
    exit 1
}
finally {
    Pop-Location
}

# -----------------------------------------------------------------------------
# Phase 3: Diger tum modulleri package et
# -----------------------------------------------------------------------------
Write-Step "Phase 3: Servis modullerinde mvn clean package"

foreach ($module in $ServiceModules) {
    Write-Info "Building: $module"

    if (-not (Test-Path "$module\pom.xml")) {
        Write-Failure "$module/pom.xml bulunamadi, atlaniyor..."
        $FailedModules += $module
        continue
    }

    Push-Location $module
    try {
        $output = mvn clean package 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Package basarili: $module"
            $SuccessCount++
        }
        else {
            Write-Failure "Package basarisiz: $module"
            Write-Host "`n--- Maven ciktisi (son 20 satir) ---" -ForegroundColor Red
            $output | Select-Object -Last 20 | ForEach-Object { Write-Host $_ -ForegroundColor Red }
            $FailedModules += $module
        }
    }
    catch {
        Write-Failure "Package hatasi: $module -> $_"
        $FailedModules += $module
    }
    finally {
        Pop-Location
    }
}

# -----------------------------------------------------------------------------
# Ozet
# -----------------------------------------------------------------------------
Write-Step "Build Ozeti"

Write-Host ""
Write-Host "  Toplam modul  : $TotalModules" -ForegroundColor White
Write-Host "  Basarili      : $SuccessCount" -ForegroundColor Green

if ($FailedModules.Count -gt 0) {
    Write-Host "  Basarisiz     : $($FailedModules.Count)" -ForegroundColor Red
    Write-Host "  Hata alanlar  : $($FailedModules -join ', ')" -ForegroundColor Red
    Write-Host ""
    exit 1
}
else {
    Write-Host "  Basarisiz     : 0" -ForegroundColor Green
    Write-Host ""
    Write-Host "  Tum moduller basariyla build edildi!" -ForegroundColor Green

    # JAR dosyalarini listele
    Write-Host "`n  Olusan JAR dosyalari:" -ForegroundColor Cyan
    foreach ($module in $AllModules) {
        $jarPath = Get-ChildItem -Path "$module\target\*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch 'sources|javadoc|original' } | Select-Object -First 1
        if ($jarPath) {
            $sizeMB = [math]::Round($jarPath.Length / 1MB, 2)
            Write-Host "    $module -> $($jarPath.Name) ($sizeMB MB)" -ForegroundColor White
        }
    }
    Write-Host ""
}

exit 0
