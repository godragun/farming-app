# Install debug APK and bring the app to the foreground (launcher = DashboardActivity).
# Requires: adb sees a device as "device" (not offline). Start emulator first (Task or Android Studio).
param(
    [string] $Avd = "Pixel_6_Pro_API_36"
)

$ErrorActionPreference = "Stop"
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"
$pkg = "com.project.farmingapp"
$activity = "$pkg/.view.dashboard.DashboardActivity"

if (-not (Test-Path $adb)) { Write-Error "adb not found at $adb" }

$dev = (& $adb devices | Select-String "^\S+\s+device\s*$")
if (-not $dev) {
    Write-Host "No device in 'device' state. Current list:"
    & $adb devices -l
    Write-Host ""
    Write-Host "Fix: close any hung emulator window, then run:"
    Write-Host "  .\scripts\fix-emulator.ps1"
    Write-Host "  .\scripts\start-emulator-cursor.ps1 -Avd `"$Avd`""
    Write-Host "Wait until the home screen appears, then run this script again."
    exit 1
}

Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    Write-Host "Installing debug build..."
    & .\gradlew.bat installDebug --no-daemon
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host "Launching $activity ..."
    & $adb shell am start -n $activity
} finally {
    Pop-Location
}
