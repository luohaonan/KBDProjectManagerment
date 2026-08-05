<#
.SYNOPSIS
  Build Docker images, optionally export local MySQL data, and create a server deployment package.

.NOTES
  This script intentionally uses ASCII text only to stay compatible with Windows PowerShell 5.1.
#>

[CmdletBinding()]
param(
  [string]$Registry = "",
  [string]$ImagePrefix = "kbd-pms",
  [string]$ImageTag = "latest",
  [switch]$Push,
  [switch]$ExportDatabase,
  [string]$DbHost = "127.0.0.1",
  [int]$DbPort = 3306,
  [string]$DbName = "kbd_pm_system",
  [string]$DbUser = "root",
  [switch]$EncryptPackage,
  [switch]$SkipImageArchive
)

$ErrorActionPreference = "Stop"

function Require-Command($Name) {
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Required command not found: $Name. Please install it or add it to PATH."
  }
}

function ConvertTo-PlainText([System.Security.SecureString]$SecureString) {
  $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureString)
  try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
  finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ProjectRoot

Require-Command docker

if ([string]::IsNullOrWhiteSpace($Registry)) {
  $ResolvedPrefix = $ImagePrefix
}
else {
  $ResolvedPrefix = "$Registry/$ImagePrefix"
}

$BackendImage = "${ResolvedPrefix}/backend:${ImageTag}"
$FrontendImage = "${ResolvedPrefix}/frontend:${ImageTag}"

Write-Host "==> Building backend image: $BackendImage" -ForegroundColor Cyan
docker build -t $BackendImage ./backend

Write-Host "==> Building frontend image: $FrontendImage" -ForegroundColor Cyan
docker build -t $FrontendImage ./frontend

if ($Push) {
  Write-Host "==> Pushing images" -ForegroundColor Cyan
  docker push $BackendImage
  docker push $FrontendImage
}

$DeployRoot = Join-Path $ProjectRoot "deploy"
$DbInitDir = Join-Path $DeployRoot "db\init"
New-Item -ItemType Directory -Force -Path $DbInitDir | Out-Null

if ($ExportDatabase) {
  Require-Command mysqldump
  $SecurePassword = Read-Host "Enter local MySQL password for user $DbUser" -AsSecureString
  $PlainPassword = ConvertTo-PlainText $SecurePassword
  $DumpFile = Join-Path $DbInitDir "001-current-database.sql"

  Write-Host "==> Exporting database $DbName to $DumpFile" -ForegroundColor Cyan
  $env:MYSQL_PWD = $PlainPassword
  try {
    mysqldump --host=$DbHost --port=$DbPort --user=$DbUser --single-transaction --routines --triggers --events --default-character-set=utf8mb4 --databases $DbName --result-file=$DumpFile
  }
  finally {
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
  }
}
else {
  Write-Host "==> -ExportDatabase not specified. Skipping database export." -ForegroundColor Yellow
}

$PackageDir = Join-Path $ProjectRoot "deploy-package"
if (Test-Path $PackageDir) { Remove-Item $PackageDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $PackageDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $PackageDir "deploy\db\init") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $PackageDir "images") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $PackageDir "db") | Out-Null

Copy-Item docker-compose.yml $PackageDir
Copy-Item .env.server.example $PackageDir
Copy-Item DOCKER_DEPLOYMENT_GUIDE.md $PackageDir -ErrorAction SilentlyContinue

$MailUrlMigration = Join-Path $ProjectRoot "db\20260731_mail_access_urls.sql"
if (Test-Path $MailUrlMigration) {
  Copy-Item $MailUrlMigration (Join-Path $PackageDir "db") -Force
}
else {
  Write-Host "==> Optional migration not found, skipping: $MailUrlMigration" -ForegroundColor Yellow
}

if (Test-Path $DbInitDir) {
  Copy-Item (Join-Path $DbInitDir "*") (Join-Path $PackageDir "deploy\db\init") -Recurse -Force -ErrorAction SilentlyContinue
}

if (-not $SkipImageArchive) {
  $ImagesArchive = Join-Path $PackageDir "images\kbd-pms-images.tar"
  Write-Host "==> Saving backend/frontend images to: $ImagesArchive" -ForegroundColor Cyan
  docker save -o $ImagesArchive $BackendImage $FrontendImage
}
else {
  Write-Host "==> -SkipImageArchive specified. Server must pull images from registry." -ForegroundColor Yellow
}

@"
DOCKER_IMAGE_PREFIX=$ResolvedPrefix
IMAGE_TAG=$ImageTag
"@ | Set-Content -Path (Join-Path $PackageDir "IMAGE_INFO.env") -Encoding UTF8

$ZipPath = Join-Path $ProjectRoot "kbd-pms-deploy-package.zip"
if (Test-Path $ZipPath) { Remove-Item $ZipPath -Force }

if ($EncryptPackage) {
  $SevenZip = Get-Command 7z -ErrorAction SilentlyContinue
  if ($SevenZip) {
    $SecureZipPassword = Read-Host "Enter deployment package password" -AsSecureString
    $ZipPassword = ConvertTo-PlainText $SecureZipPassword
    $EncryptedZipPath = Join-Path $ProjectRoot "kbd-pms-deploy-package-encrypted.zip"
    if (Test-Path $EncryptedZipPath) { Remove-Item $EncryptedZipPath -Force }
    & $SevenZip.Source a -tzip $EncryptedZipPath (Join-Path $PackageDir "*") "-p$ZipPassword" -mem=AES256 | Out-Host
    Write-Host "==> Encrypted package created: $EncryptedZipPath" -ForegroundColor Green
  }
  else {
    Write-Host "7z not found. Creating normal zip. Install 7-Zip to use -EncryptPackage." -ForegroundColor Yellow
    Compress-Archive -Path (Join-Path $PackageDir "*") -DestinationPath $ZipPath -Force
    Write-Host "==> Package created: $ZipPath" -ForegroundColor Green
  }
}
else {
  Compress-Archive -Path (Join-Path $PackageDir "*") -DestinationPath $ZipPath -Force
  Write-Host "==> Package created: $ZipPath" -ForegroundColor Green
}

Write-Host "==> Done. Follow DOCKER_DEPLOYMENT_GUIDE.md to deploy on server." -ForegroundColor Green