param(
  [Parameter(Mandatory = $false)]
  [string]$ServerHost = "3.111.187.30",

  [Parameter(Mandatory = $false)]
  [string]$User = "ubuntu",

  [Parameter(Mandatory = $false)]
  [string]$KeyPath = ".\pp.pem",

  [Parameter(Mandatory = $false)]
  [string]$RemoteDir = "/home/ubuntu/automation-app"
)

$ErrorActionPreference = "Stop"

function Run-Step($name, $script) {
  Write-Host "==> $name" -ForegroundColor Cyan
  $global:LASTEXITCODE = 0
  & $script
  if ($LASTEXITCODE -ne 0) {
    throw "Step failed: $name (exit code $LASTEXITCODE)"
  }
}

if (!(Test-Path $KeyPath)) {
  throw "PEM key not found: $KeyPath"
}

$tmpTar = Join-Path $env:TEMP ("automation-deploy-" + [guid]::NewGuid().ToString("N") + ".tar.gz")

Run-Step "Create source archive" {
  tar --exclude=.git --exclude=frontend/node_modules --exclude=backend/target --exclude=pgdata --exclude=frontend/dist -czf $tmpTar .
}

Run-Step "Check SSH connectivity" {
  ssh -o StrictHostKeyChecking=accept-new -i $KeyPath "$User@$ServerHost" "echo CONNECTED"
}

Run-Step "Upload archive" {
  scp -i $KeyPath $tmpTar "$User@${ServerHost}:/tmp/automation-deploy.tar.gz"
}

$remoteScript = @"
set -e
if command -v apt-get >/dev/null 2>&1; then
  sudo apt-get update -y
  sudo apt-get install -y docker.io docker-compose-plugin
elif command -v yum >/dev/null 2>&1; then
  sudo yum update -y
  sudo yum install -y docker
  sudo yum install -y docker-compose-plugin || true
fi
sudo systemctl enable docker || true
sudo systemctl start docker || true
sudo usermod -aG docker $USER || true
rm -rf $RemoteDir
mkdir -p $RemoteDir
tar -xzf /tmp/automation-deploy.tar.gz -C $RemoteDir
cd $RemoteDir

COMPOSE_CMD=""
if sudo docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD="docker-compose"
else
  sudo curl -L "https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-linux-x86_64" -o /usr/local/bin/docker-compose
  sudo chmod +x /usr/local/bin/docker-compose
  COMPOSE_CMD="docker-compose"
fi

if [ "$COMPOSE_CMD" = "docker compose" ]; then
  sudo docker compose up --build -d
  sudo docker compose ps
else
  sudo docker-compose up --build -d
  sudo docker-compose ps
fi
"@

Run-Step "Install runtime and deploy containers" {
  ssh -i $KeyPath "$User@$ServerHost" $remoteScript
}

Write-Host ""
Write-Host "Deployment complete." -ForegroundColor Green
Write-Host "Frontend: http://${ServerHost}:3000"
Write-Host "Backend:  http://${ServerHost}:8080/api/reports"
