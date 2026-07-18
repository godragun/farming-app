# Fixes common "emulator offline" / 300s timeout issues on Windows before you run from Android Studio or Gradle.
# Usage (PowerShell):  cd "...\Farming-App\scripts"; .\fix-emulator.ps1
# Optional: start AVD after cleanup:  .\fix-emulator.ps1 -StartAvd "Pixel_6_Pro_API_36"

param(
    [string] $StartAvd = ""
)

$ErrorActionPreference = "Stop"
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"
$emu = Join-Path $sdk "emulator\emulator.exe"

if (-not (Test-Path $adb)) {
    Write-Error "adb not found at $adb — set ANDROID_HOME or install Android SDK Platform-Tools."
}

Write-Host "Stopping ADB server..."
& $adb kill-server | Out-Null
Start-Sleep -Seconds 2

Write-Host "Starting ADB server..."
& $adb start-server | Out-Null

Write-Host "Devices (before cleanup):"
& $adb devices -l

# Drop stale offline emulator sockets so the next boot can register as "device"
Write-Host "Trying adb reconnect offline..."
& $adb reconnect offline 2>$null
Start-Sleep -Seconds 2

Write-Host "Devices (after reconnect):"
& $adb devices -l

if ($StartAvd) {
    if (-not (Test-Path $emu)) {
        Write-Error "emulator.exe not found at $emu"
    }
    Write-Host "Starting AVD '$StartAvd' with software GLES (reduces GPU hang -> offline)..."
    Write-Host "Close this window to stop the emulator."
    # -no-snapshot-load: cold boot (slower but clears bad snapshot state)
    # -gpu swiftshader_indirect: software rendering — most reliable when emulator stays "offline"
    & $emu -avd $StartAvd -no-snapshot-load -gpu swiftshader_indirect
} else {
    Write-Host ""
    Write-Host "Next steps if Android Studio still times out:"
    Write-Host "  1) Device Manager -> your AVD -> dropdown -> Cold Boot Now (or Wipe Data)."
    Write-Host "  2) Edit AVD -> Graphics = Software (same as swiftshader_indirect above)."
    Write-Host "  3) Prefer API 33–34 images if API 36 is unstable on your PC."
    Write-Host "  4) Then run: .\fix-emulator.ps1 -StartAvd YOUR_AVD_NAME"
}
