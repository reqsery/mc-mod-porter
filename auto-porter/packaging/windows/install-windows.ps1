<#
.SYNOPSIS
    Installs MC Mod Porter for the current user and adds Start Menu and Desktop shortcuts.

.DESCRIPTION
    Copies the app to %LOCALAPPDATA%\Programs\MC-Mod-Porter, then creates "MC Mod Porter" shortcuts in the
    Start Menu (so it shows up in Windows search) and, unless -NoDesktopShortcut is given, on the Desktop.
    Works from the unzipped release download (the script sits next to MC-Mod-Porter.exe) and from the source
    tree after `gradlew packageWindowsApp` (build\jpackage\MC-Mod-Porter).
    No administrator rights are needed. Run uninstall-windows.ps1 to remove it again.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\install-windows.ps1
#>
param(
    [string]$Source = $(if (Test-Path (Join-Path $PSScriptRoot "MC-Mod-Porter.exe")) { $PSScriptRoot }
                        else { Join-Path $PSScriptRoot "..\..\build\jpackage\MC-Mod-Porter" }),
    [string]$InstallDir = (Join-Path $env:LOCALAPPDATA "Programs\MC-Mod-Porter"),
    [switch]$NoDesktopShortcut
)
$ErrorActionPreference = "Stop"

$Source = (Resolve-Path $Source -ErrorAction SilentlyContinue)
if (-not $Source -or -not (Test-Path (Join-Path $Source "MC-Mod-Porter.exe"))) {
    Write-Error "MC-Mod-Porter.exe not found. Unzip the release download and run this script from that folder, or build it first:  cd auto-porter; .\gradlew.bat packageWindowsApp"
}
$InstallDir = [IO.Path]::GetFullPath($InstallDir)
if ($Source.TrimEnd('\') -ieq $InstallDir.TrimEnd('\')) {
    Write-Error "This copy is already installed in $InstallDir. Run the script from the unzipped download instead."
}

if (Test-Path $InstallDir) {
    Get-Process -Name "MC-Mod-Porter*" -ErrorAction SilentlyContinue | Where-Object { $_.Path -like "$InstallDir*" } | Stop-Process -Force
    Remove-Item $InstallDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
Copy-Item -Path (Join-Path $Source "*") -Destination $InstallDir -Recurse -Force

$exe = Join-Path $InstallDir "MC-Mod-Porter.exe"
$shell = New-Object -ComObject WScript.Shell
function New-PorterShortcut([string]$Path) {
    $shortcut = $shell.CreateShortcut($Path)
    $shortcut.TargetPath = $exe
    $shortcut.WorkingDirectory = $InstallDir
    $shortcut.IconLocation = "$exe,0"
    $shortcut.Description = "Port Minecraft mod source projects between versions"
    $shortcut.Save()
}

$startMenu = Join-Path ([Environment]::GetFolderPath("Programs")) "MC Mod Porter.lnk"
New-PorterShortcut $startMenu
Write-Host "Start Menu shortcut: $startMenu"
if (-not $NoDesktopShortcut) {
    $desktop = Join-Path ([Environment]::GetFolderPath("Desktop")) "MC Mod Porter.lnk"
    New-PorterShortcut $desktop
    Write-Host "Desktop shortcut:    $desktop"
}
Write-Host "Installed to $InstallDir"
Write-Host "Command line: `"$(Join-Path $InstallDir 'MC-Mod-Porter-CLI.exe')`" --help"
