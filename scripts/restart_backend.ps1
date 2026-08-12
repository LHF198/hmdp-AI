# Restart backend: stop old process on 8081 -> start new jar in a new window
$ErrorActionPreference = "SilentlyContinue"

# 1. Stop old backend occupying 8081 (skip IDE language servers)
$conn = Get-NetTCPConnection -LocalPort 8081 -State Listen
foreach ($c in $conn) {
    $p = Get-CimInstance Win32_Process -Filter "ProcessId=$($c.OwningProcess)"
    if ($p.CommandLine -like "*hm-dianping*") {
        Stop-Process -Id $c.OwningProcess -Force
        Write-Host "stopped old backend pid=$($c.OwningProcess)"
    }
}
Start-Sleep -Seconds 2

# 2. Start new jar in a minimized window (detached from current process tree)
# 相对项目根路径（脚本位于 scripts/ 下，项目根为其上一级）
$jar = Join-Path $PSScriptRoot "..\target\hm-dianping-0.0.1-SNAPSHOT.jar"
$proc = Start-Process -FilePath "java" -ArgumentList "-jar", $jar -WorkingDirectory (Join-Path $PSScriptRoot '..') -WindowStyle Minimized -PassThru
Write-Host "backend started pid=$($proc.Id)"

# 3. Wait for port 8081 up to 60s
for ($i = 0; $i -lt 12; $i++) {
    Start-Sleep -Seconds 5
    $listen = Get-NetTCPConnection -LocalPort 8081 -State Listen
    if ($listen) {
        Write-Host "8081 is UP"
        exit 0
    }
}
Write-Host "8081 still not listening after 60s, check manually"
exit 1
