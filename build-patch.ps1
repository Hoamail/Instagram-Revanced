Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

<#
.SYNOPSIS
  Rebuild the view-once media saver for Instagram 455.0.0.51.72 from the stock APK.

.DESCRIPTION
  Chain:
    1. extract the dex that owns the view-once callback (X/0lJh) into a stub APK
    2. decode it with APKEditor -> smali
    3. inject.py     : append the Saver hook to X/0lJh->Fs7(LX/00n3;)V
    4. build the extension (javac + d8) -> classes33.dex
    5. APKEditor     : smali -> dex
    6. repack.py     : swap classes21.dex, add classes33.dex into the signed-ready APK
    7. zipalign + apksigner

.PARAMETER StockApk
  Stock base.apk of the target version.

.PARAMETER Out
  Output APK (signed). Default: work\ig-hx-signed.apk

.PARAMETER SkipInstall
  Do not push to the device.
#>
param(
    [Parameter(Mandatory = $true)][string]$StockApk,
    [string]$Out = "work\ig-hx-signed.apk",
    [string]$Keystore = "hx.keystore",
    [string]$KsPass = "hxpass123",
    [string]$KeyAlias = "hx",
    [switch]$SkipInstall
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$sdk    = "tools\sdk"
$androidJar = "$sdk\platforms\android-36\android.jar"
$bt     = "$sdk\build-tools\36.0.0"
$apkeditor = "tools\dl\APKEditor.jar"
$dexName = "classes21.dex"      # dex that carries X/0lJh on 455.0.0.51.72

function Step($msg) { Write-Host "==> $msg" -ForegroundColor Cyan }

if (-not (Test-Path $androidJar)) { throw "missing $androidJar" }
if (-not (Test-Path $Keystore))  { throw "missing keystore $Keystore" }

# 1. stub APK with only the manifest, resources and the target dex -----------
Step "stub APK from $StockApk"
New-Item -ItemType Directory -Force work | Out-Null
python build-stub.py $StockApk work\one.apk $dexName
if ($LASTEXITCODE -ne 0) { throw "build-stub failed" }

# 2. decode ----------------------------------------------------------------
Step "decode smali"
if (Test-Path work\one-d) { Remove-Item work\one-d -Recurse -Force }
java -Xmx6g -jar $apkeditor d -f -no-cache -comment-level off -i work\one.apk -o work\one-d
if ($LASTEXITCODE -ne 0) { throw "decode failed" }

# 3. inject ----------------------------------------------------------------
Step "inject hook"
python inject.py work\one-d --dex $dexName
if ($LASTEXITCODE -ne 0) { throw "inject failed (anchor changed?)" }

# 4. extension dex ---------------------------------------------------------
Step "build extension"
Remove-Item work\ext-classes, work\ext-dex -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force work\ext-classes, work\ext-dex | Out-Null
$src = "$root\ext\app\hx\viewonce\Saver.java"
javac -nowarn -source 11 -target 11 -classpath $androidJar -d work\ext-classes $src
if ($LASTEXITCODE -ne 0) { throw "javac failed" }
& "$bt\d8.bat" --min-api 28 --lib $androidJar --output work\ext-dex `
    (Get-ChildItem work\ext-classes -Recurse -File -Filter *.class | ForEach-Object { $_.FullName })
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

# 5. rebuild dex -----------------------------------------------------------
Step "rebuild dex"
if (Test-Path work\one-patched.apk) { Remove-Item work\one-patched.apk -Force }
java -Xmx6g -jar $apkeditor b -i work\one-d -o work\one-patched.apk
if ($LASTEXITCODE -ne 0) { throw "rebuild failed" }
python -c "import zipfile,sys; z=zipfile.ZipFile(r'work\one-patched.apk'); d=z.read('$dexName'); open(r'work\one-patched.dex','wb').write(d); sys.exit(0 if b'app/hx/viewonce/Saver' in d else 1)"
if ($LASTEXITCODE -ne 0) { throw "hook missing from rebuilt dex" }

# 6. repack ----------------------------------------------------------------
Step "repack"
Remove-Item work\ig-hx-unsigned.apk, work\ig-hx-aligned.apk -Force -ErrorAction SilentlyContinue
python repack.py $StockApk work\one-patched.dex work\ext-dex\classes.dex work\ig-hx-unsigned.apk
if ($LASTEXITCODE -ne 0) { throw "repack failed" }

# 7. align + sign ----------------------------------------------------------
Step "align + sign"
& "$bt\zipalign.exe" -p -f 4 work\ig-hx-unsigned.apk work\ig-hx-aligned.apk
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }
if (Test-Path $Out) { Remove-Item $Out -Force }
& "$bt\apksigner.bat" sign --ks $Keystore --ks-pass "pass:$KsPass" --key-pass "pass:$KsPass" --out $Out work\ig-hx-aligned.apk
if ($LASTEXITCODE -ne 0) { throw "sign failed" }

$sizeMb = [int]((Get-Item $Out).Length / 1MB)
Write-Host "built $Out ($sizeMb MB)" -ForegroundColor Green

if (-not $SkipInstall) {
    Step "install (keeps app data, same signing key)"
    adb install -r $Out
}
