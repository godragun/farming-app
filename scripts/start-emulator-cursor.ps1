# Used by Cursor/VS Code task "Android: Start emulator (separate window)".
param(
    [Parameter(Mandatory = $false)]
    [string] $Avd = "Pixel_6_Pro_API_36"
)

$ErrorActionPreference = "Stop"
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$exe = Join-Path $sdk "emulator\emulator.exe"

if (-not (Test-Path $exe)) {
    Write-Error "emulator.exe not found at $exe. Install Android Emulator in SDK Manager, or set ANDROID_HOME."
}

Write-Host "Launching AVD '$Avd' in a new window (software GLES)..."
Start-Process -FilePath $exe -ArgumentList @("-avd", $Avd, "-gpu", "swiftshader_indirect")
