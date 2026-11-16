param(
    [string]$Stock = "restore-point\20261116-154535\apk\base.apk",
    [string]$Out = "work\ig-hx-signed.apk",
    [switch]$NoInstall
)

# Assemble the final APK from every patched dex + the Saver extension, then install.
# Expects work\<dex>.dex.patched for each dex below (produced by patch_dex.py) and
# work\ext-dex\classes.dex. Signs with the same keystore, so it installs as an update
# and the logged-in session survives.

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
$bt = "tools\sdk\build-tools\36.0.0"

$dexes = @("classes4", "classes5", "classes13", "classes16", "classes21", "classes27", "classes28", "classes29")

$argsList = @()
foreach ($d in $dexes) {
    $p = "work\$d.dex.patched"
    if (-not (Test-Path $p)) { throw "missing $p (run patch_dex.py for $d.dex)" }
    $argsList += @("--replace", "$d.dex=$p")
}
if (-not (Test-Path "work\ext-dex\classes.dex")) { throw "missing work\ext-dex\classes.dex" }
$argsList += @("--add", "classes33.dex=work\ext-dex\classes.dex")

Remove-Item work\ig-hx-unsigned.apk, work\ig-hx-aligned.apk, $Out -Force -ErrorAction SilentlyContinue

python repack2.py $Stock work\ig-hx-unsigned.apk @argsList
if ($LASTEXITCODE -ne 0) { throw "repack failed" }

& "$bt\zipalign.exe" -p -f 4 work\ig-hx-unsigned.apk work\ig-hx-aligned.apk
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

& "$bt\apksigner.bat" sign --ks hx.keystore --ks-pass pass:hxpass123 --key-pass pass:hxpass123 --out $Out work\ig-hx-aligned.apk
if ($LASTEXITCODE -ne 0) { throw "sign failed" }

Write-Host ("built {0} ({1} MB)" -f $Out, [int]((Get-Item $Out).Length / 1MB)) -ForegroundColor Green
if (-not $NoInstall) { adb install -r $Out }
