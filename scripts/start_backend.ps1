# 相对项目根路径（脚本位于 scripts/ 下，项目根为其上一级）
$jar = Join-Path $PSScriptRoot '..\target\hm-dianping-0.0.1-SNAPSHOT.jar'
$out = Join-Path $PSScriptRoot '..\logs\backend.log'
$err = Join-Path $PSScriptRoot '..\logs\backend_err.log'
$java = 'C:\Users\26821\dev\jdk-17.0.20+8\bin\java.exe'

# ============================================================
# AI_API_KEY check: the backend reads the key from the env var
# (never hardcoded in application.yaml). Warn early if missing.
# ============================================================
if (-not $env:AI_API_KEY) {
    Write-Host "[WARN] AI_API_KEY env var not set in this session - AI assistant will be unavailable"
    Write-Host "       Run: setx AI_API_KEY \"sk-...\"  then reopen the terminal/IDE"
}

# ============================================================
# Auto cleanup: force-free port 8081 before startup (kill stale
# process), so duplicate-instance conflicts never happen again.
# No manual cleanup tool is needed.
# ============================================================
$conns = Get-NetTCPConnection -State Listen -LocalPort 8081 -ErrorAction SilentlyContinue
if ($conns) {
    $conns | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object {
        Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
        Write-Host ("[CLEAN] killed old process on 8081, pid=" + $_)
    }
    # Wait until the port is actually released (max 10 seconds)
    for ($i = 0; $i -lt 10; $i++) {
        Start-Sleep -Seconds 1
        if (-not (Get-NetTCPConnection -State Listen -LocalPort 8081 -ErrorAction SilentlyContinue)) {
            break
        }
    }
} else {
    Write-Host "[CLEAN] port 8081 is free, no cleanup needed"
}

# 工作目录必须为项目根（与 restart_backend.ps1 一致）：
# app.upload-dir 默认相对路径（frontend/html/hmdp/imgs/）按工作目录解析，
# 若以 target/ 为工作目录会解析成 target/frontend/html/hmdp/imgs/，
# 与 nginx /imgs/ alias（frontend/html/hmdp/imgs/）不一致导致上传图片 404
$p = Start-Process -FilePath $java -ArgumentList @('-jar', $jar) -WorkingDirectory (Join-Path $PSScriptRoot '..') -RedirectStandardOutput $out -RedirectStandardError $err -WindowStyle Hidden -PassThru
Write-Host ("[INFO] started pid=" + $p.Id)
Start-Sleep -Seconds 25
$c = Get-NetTCPConnection -State Listen -LocalPort 8081 -ErrorAction SilentlyContinue
if ($c) {
    Write-Host ("[OK] 8081 listening pid=" + $c.OwningProcess)
} else {
    Write-Host "[WARN] 8081 not listening yet"
    Write-Host "=== err log tail ==="
    Get-Content $err -Tail 25 -ErrorAction SilentlyContinue
}
