$ErrorActionPreference = "Stop"
$SDK  = "D:\android\Sdk"
$BT   = "$SDK\build-tools\35.0.0"
$AJAR = "$SDK\platforms\android-35\android.jar"
$PROJ = "D:\fastboot\echomenu"
Set-Location $PROJ

Remove-Item -Recurse -Force obj,out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force obj,out | Out-Null

Write-Host "[1/6] aapt2 compile + link (resources + manifest -> base.apk)"
& "$BT\aapt2.exe" compile --dir res -o out\res-compiled.zip
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }
& "$BT\aapt2.exe" link --manifest AndroidManifest.xml -I $AJAR `
    --min-sdk-version 21 --target-sdk-version 25 `
    -o out\base.apk out\res-compiled.zip
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Host "[2/6] javac"
& javac --release 11 -classpath $AJAR -d obj src\com\rev\godmode\GodMode.java
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host "[3/6] d8 -> classes.dex"
$classes = Get-ChildItem "obj\com\rev\godmode\*.class" | ForEach-Object { $_.FullName }
& java -cp "$BT\lib\d8.jar" com.android.tools.r8.D8 --min-api 21 --lib $AJAR --output out @classes
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Write-Host "[4/6] add classes.dex into apk"
Push-Location out
& jar uf base.apk classes.dex
Pop-Location

Write-Host "[5/6] zipalign"
& "$BT\zipalign.exe" -f 4 out\base.apk out\aligned.apk
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

Write-Host "[6/6] sign"
$KS = "$PROJ\debug.keystore"
if (-not (Test-Path $KS)) {
    & keytool -genkeypair -keystore $KS -storepass android -keypass android `
        -alias dbg -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Rev Debug"
}
& "$BT\apksigner.bat" sign --ks $KS --ks-pass pass:android --key-pass pass:android `
    --out out\GodMode.apk out\aligned.apk
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

Write-Host "BUILD OK -> $PROJ\out\GodMode.apk"
Get-Item out\GodMode.apk | Select-Object Name,Length
