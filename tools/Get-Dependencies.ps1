param([string]$Destination)
$ErrorActionPreference='Stop'
$repoRoot=Split-Path $PSScriptRoot
if(-not $Destination){$Destination=Join-Path $repoRoot '.cache/dependencies'}
New-Item -ItemType Directory -Force $Destination | Out-Null
foreach($dependency in (Get-Content (Join-Path $PSScriptRoot 'dependencies.lock.json') -Raw | ConvertFrom-Json)) {
    $target=Join-Path $Destination $dependency.file
    if(-not (Test-Path -LiteralPath $target)) {Invoke-WebRequest -Uri $dependency.url -OutFile $target}
    if((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant() -ne $dependency.sha256){throw "Checksum mismatch: $target"}
    Write-Host "Verified $($dependency.file)"
}
