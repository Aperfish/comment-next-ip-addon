param(
    [Parameter(Mandatory=$true)][string]$OriginalJar,
    [string]$LibDirectory = '.cache/dependencies'
)
$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
$repoRoot=[System.IO.Path]::GetFullPath($PSScriptRoot)
$upstreamJar=(Resolve-Path -LiteralPath $OriginalJar).Path
$dependencyPath=(Resolve-Path -LiteralPath $LibDirectory).Path
$workPath=[System.IO.Path]::GetFullPath((Join-Path $repoRoot 'work'))
if($upstreamJar.StartsWith($workPath+[System.IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Put the input JAR outside the generated work directory.'}
foreach($dependency in (Get-Content tools/dependencies.lock.json -Raw | ConvertFrom-Json)) {
    $file=Join-Path $dependencyPath $dependency.file
    if(-not (Test-Path -LiteralPath $file)){throw "Missing dependency: $file. Run tools/Get-Dependencies.ps1 first."}
    if((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant() -ne $dependency.sha256){throw "Dependency checksum mismatch: $file"}
}
if($workPath -ne [System.IO.Path]::Combine($repoRoot,'work')){throw 'Unsafe work directory'}
if(Test-Path -LiteralPath $workPath){Remove-Item -LiteralPath $workPath -Recurse -Force}
New-Item -ItemType Directory -Force work/addon/src,work/addon/stubs,work/addon/temp,work/v1013/original,dist | Out-Null
Copy-Item src/main/java/* work/addon/src/ -Recurse
Copy-Item compile-fixtures/* work/addon/stubs/ -Recurse
Copy-Item tools/PackageAddon.java work/addon/
Push-Location work/v1013/original
try {jar xf $upstreamJar;if($LASTEXITCODE -ne 0){throw 'Upstream extraction failed'}}finally{Pop-Location}
$descriptor=Get-Content work/v1013/original/plugin.yaml -Raw
if($descriptor -notmatch 'name:\s*"?PluginCommentNext"?' -or $descriptor -notmatch 'version:\s*"?1\.0\.13"?\s') {throw 'Input must be original PluginCommentNext 1.0.13'}
node tools/prepare.cjs
if($LASTEXITCODE -ne 0){throw 'Preparation failed'}
javac --release 21 -encoding UTF-8 -cp "$dependencyPath/*" -d work/addon/stubclasses '@work/addon/stubs.args'
if($LASTEXITCODE -ne 0){throw 'Compile fixtures failed'}
javac --release 21 -encoding UTF-8 -cp "work/addon/stubclasses;work/v1013/original;$dependencyPath/*" -d work/addon/compiled '@work/addon/src.args'
if($LASTEXITCODE -ne 0){throw 'Compile addon failed'}
javac -encoding UTF-8 -cp "$dependencyPath/*" -d work/addon/tools work/addon/PackageAddon.java
if($LASTEXITCODE -ne 0){throw 'Compile packaging tools failed'}
java -cp "work/addon/tools;$dependencyPath/*" PackageAddon
if($LASTEXITCODE -ne 0){throw 'Relocation checks failed'}
New-Item -ItemType Directory -Force work/addon/classes/META-INF/licenses | Out-Null
Copy-Item LICENSE work/addon/classes/META-INF/licenses/GPL-3.0.txt
Copy-Item NOTICE.md work/addon/classes/META-INF/NOTICE.md
Copy-Item licenses/* work/addon/classes/META-INF/licenses/
$artifact=Join-Path $repoRoot 'dist/comment-next-ip-addon-1.0.13-4.jar'
java '-Djava.io.tmpdir=work/addon/temp' sun.tools.jar.Main --create --file $artifact --manifest work/addon/manifest.mf -C work/addon/classes .
if($LASTEXITCODE -ne 0){throw 'JAR packaging failed'}
$testSources=Get-ChildItem tests/*.java | ForEach-Object FullName
javac --release 21 -encoding UTF-8 -cp "$artifact;work/addon/stubclasses;$dependencyPath/*" -d work/addon/tests $testSources
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
$formatClasspath="work/addon/tests;$artifact;$dependencyPath/*"
$linkClasspath="work/addon/tests;$artifact;work/v1013/original;work/addon/stubclasses;$dependencyPath/*"
foreach($test in @('io.github.commentipaddon.internal.widget.iplocation.DistrictTest','AddonFormatTest')) {
    java -Xverify:all -cp $formatClasspath $test
    if($LASTEXITCODE -ne 0){throw "Test failed: $test"}
}
foreach($test in @('InjectionRegressionTest','AdapterLinkTest')) {
    java -Xverify:all -cp $linkClasspath $test
    if($LASTEXITCODE -ne 0){throw "Test failed: $test"}
}
Get-FileHash -LiteralPath $artifact -Algorithm SHA256
Write-Host 'Offline checks passed. This does not establish live Halo/API compatibility.'
