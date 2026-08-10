@echo off
setlocal
cd /d "%~dp0"
start "" powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "%~dp0BSA-Lazy-Dev-Wizard.ps1"
