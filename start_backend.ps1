$jar = 'c:\Users\26821\Downloads\hmdp-main\hmdp-main\target\hm-dianping-0.0.1-SNAPSHOT.jar'
$out = 'c:\Users\26821\Downloads\hmdp-main\backend.log'
$err = 'c:\Users\26821\Downloads\hmdp-main\backend_err.log'
$java = 'C:\Users\26821\dev\jdk-17.0.20+8\bin\java.exe'

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

$p = Start-Process -FilePath $java -ArgumentList @('-jar', $jar) -WorkingDirectory 'c:\Users\26821\Downloads\hmdp-main\hmdp-main\target' -RedirectStandardOutput $out -RedirectStandardError $err -WindowStyle Hidden -PassThru
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
