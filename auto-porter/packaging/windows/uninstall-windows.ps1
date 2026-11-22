<#
.SYNOPSIS
    Removes MC Mod Porter installed by install-windows.ps1 (program folder and shortcuts).
#>
param([string]$InstallDir = (Join-Path $env:LOCALAPPDATA "Programs\MC-Mod-Porter"))
$ErrorActionPreference = "Stop"

Get-Process -Name "MC-Mod-Porter*" -ErrorAction SilentlyContinue | Where-Object { $_.Path -like "$InstallDir*" } | Stop-Process -Force
foreach ($link in @(
        (Join-Path ([Environment]::GetFolderPath("Programs")) "MC Mod Porter.lnk"),
        (Join-Path ([Environment]::GetFolderPath("Desktop")) "MC Mod Porter.lnk"))) {
    if (Test-Path $link) { Remove-Item $link -Force; Write-Host "Removed $link" }
}
if (Test-Path $InstallDir) { Remove-Item $InstallDir -Recurse -Force; Write-Host "Removed $InstallDir" }
Write-Host "MC Mod Porter was uninstalled. Downloaded mappings stay in $env:USERPROFILE\.mc-mod-porter (delete it if you like)."
