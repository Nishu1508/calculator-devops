<#
  Points the GitHub webhook at the current public tunnel URL of Jenkins.

  The free Cloudflare quick tunnel (service "tunnel" in jenkins/docker-compose.yml) gets a new
  https://<random>.trycloudflare.com address every time it starts, so run this after starting it:

    cd jenkins
    docker compose --profile webhook up -d
    powershell -ExecutionPolicy Bypass -File .\update-webhook.ps1

  Requires the GitHub CLI (gh) to be logged in with admin rights on the repository.
#>
param(
    [string] $Repo = 'Nishu1508/calculator-devops'
)
$ErrorActionPreference = 'Stop'

# 1. Find the tunnel URL in the cloudflared logs (wait up to ~60 s for it to appear).
$url = $null
for ($i = 0; $i -lt 30 -and -not $url; $i++) {
    # via cmd so cloudflared's stderr log lines are not treated as PowerShell errors
    $logs = cmd /c "docker logs jenkins-tunnel 2>&1" | Out-String
    $m = [regex]::Matches($logs, 'https://[a-z0-9-]+\.trycloudflare\.com')
    if ($m.Count -gt 0) { $url = $m[$m.Count - 1].Value } else { Start-Sleep 2 }
}
if (-not $url) { throw 'Tunnel URL not found. Is the "tunnel" service running? (docker compose --profile webhook up -d)' }
$hookUrl = "$url/github-webhook/"
Write-Host "Jenkins public webhook URL: $hookUrl" -ForegroundColor Cyan

# 2. Create the webhook, or update the existing Jenkins one.
$hooks = @(gh api "repos/$Repo/hooks" | ConvertFrom-Json)
if ($LASTEXITCODE -ne 0) { throw 'gh api failed - is the GitHub CLI logged in? (gh auth status)' }
$existing = $hooks | Where-Object { $_.config.url -match '/github-webhook/$' } | Select-Object -First 1

$body = @{
    name   = 'web'
    active = $true
    events = @('push')
    config = @{ url = $hookUrl; content_type = 'json'; insecure_ssl = '0' }
} | ConvertTo-Json -Depth 5

$tmp = New-TemporaryFile
Set-Content -Path $tmp -Value $body -Encoding ascii
try {
    if ($existing) {
        gh api --method PATCH "repos/$Repo/hooks/$($existing.id)" --input $tmp | Out-Null
        Write-Host "Updated webhook #$($existing.id)" -ForegroundColor Green
    } else {
        gh api --method POST "repos/$Repo/hooks" --input $tmp | Out-Null
        Write-Host 'Created webhook' -ForegroundColor Green
    }
} finally {
    Remove-Item $tmp -ErrorAction SilentlyContinue
}
