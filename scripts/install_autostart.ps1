# ============================================================
# install_autostart.ps1 — Redis / MySQL / nginx 开机自启（需管理员）
#   Redis  : 计划任务 hmdp-Redis（ONSTART / SYSTEM / 失败自动重启）
#            说明: 本机 Redis fork 不支持 --service-install，用计划任务等效实现
#   MySQL  : 原生 Windows 服务 MySQL84（Automatic）
#   nginx  : 计划任务 hmdp-Nginx（ONSTART / SYSTEM / 失败自动重启）
# ============================================================
$ErrorActionPreference = 'Stop'
$log = Join-Path $PSScriptRoot '..\logs\install_autostart.log'
Start-Transcript -Path $log -Force | Out-Null

$redisDir = 'C:\Users\26821\dev\redis'
$redisExe = Join-Path $redisDir 'redis-server.exe'
$redisConf = Join-Path $redisDir 'redis.conf'

Write-Host '== [1/5] Redis 配置修正 =='
$confText = Get-Content $redisConf -Raw
if (-not (Test-Path "$redisConf.bak")) { Copy-Item $redisConf "$redisConf.bak" -Force }
$changed = $false
if ($confText -match '(?m)^\s*dir\s+\./') {
    $confText = [regex]::Replace($confText, '(?m)^\s*dir\s+\./.*$', 'dir C:/Users/26821/dev/redis')
    $changed = $true
}
if ($confText -match '(?m)^\s*logfile\s+""') {
    $confText = [regex]::Replace($confText, '(?m)^\s*logfile\s+""\s*$', 'logfile "C:/Users/26821/dev/redis/redis.log"')
    $changed = $true
}
if ($changed) {
    [System.IO.File]::WriteAllText($redisConf, $confText, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host '[Redis] conf 已修正（dir 绝对路径 + logfile 落盘）'
} else { Write-Host '[Redis] conf 无需修改' }

Write-Host '== [2/5] Redis 开机自启（计划任务 ONSTART, SYSTEM） =='
$taskName = 'hmdp-Redis'
$existing = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if (-not $existing) {
    $action = New-ScheduledTaskAction -Execute $redisExe -Argument $redisConf
    $trigger = New-ScheduledTaskTrigger -AtStartup
    $principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
    $settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
    Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Principal $principal -Settings $settings -Force | Out-Null
    Write-Host '[Redis] 计划任务已注册'
} else { Write-Host '[Redis] 计划任务已存在' }

Write-Host '== [3/5] MySQL 服务注册 =='
$mysqld = 'C:\Users\26821\dev\mysql\mysql-8.4.9-winx64\bin\mysqld.exe'
$mysqlSvc = Get-Service -Name 'MySQL84' -ErrorAction SilentlyContinue
if (-not $mysqlSvc) {
    & $mysqld --install MySQL84 2>&1 | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "mysqld --install 失败: $LASTEXITCODE" }
    Start-Sleep -Seconds 1
    $mysqlSvc = Get-Service -Name 'MySQL84' -ErrorAction SilentlyContinue
}
if (-not $mysqlSvc) { throw 'MySQL 服务注册失败' }
Write-Host "[MySQL] 服务已注册: $($mysqlSvc.Name), StartType=$($mysqlSvc.StartType)"

Write-Host '== [5/7] nginx 开机自启（计划任务 ONSTART, SYSTEM） =='
$nginxExe = 'C:\Users\26821\dev\nginx-1.30.1\nginx.exe'
$nginxConf = 'C:\Users\26821\Downloads\hmdp-main\frontend\conf\nginx.conf'
$nginxWorkDir = 'C:\Users\26821\Downloads\hmdp-main\frontend'
$nginxTaskName = 'hmdp-Nginx'
$nginxExisting = Get-ScheduledTask -TaskName $nginxTaskName -ErrorAction SilentlyContinue
if (-not $nginxExisting) {
    $nginxAction = New-ScheduledTaskAction -Execute $nginxExe -Argument "-c `"$nginxConf`"" -WorkingDirectory $nginxWorkDir
    $nginxTrigger = New-ScheduledTaskTrigger -AtStartup
    $nginxPrincipal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
    $nginxSettings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
    Register-ScheduledTask -TaskName $nginxTaskName -Action $nginxAction -Trigger $nginxTrigger -Principal $nginxPrincipal -Settings $nginxSettings -Force | Out-Null
    Write-Host '[nginx] 计划任务已注册'
} else { Write-Host '[nginx] 计划任务已存在' }

Write-Host '== [6/7] 立即启动验证 =='
Get-NetTCPConnection -State Listen -LocalPort 6379,3306 -ErrorAction SilentlyContinue | ForEach-Object {
    Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
}
# 停止已运行的 nginx（避免端口冲突）
Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue | ForEach-Object {
    Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
}
Start-ScheduledTask -TaskName $taskName
Start-Service -Name 'MySQL84' -ErrorAction SilentlyContinue
# nginx 计划任务以 SYSTEM 身份运行，此处用当前用户身份直接启动以便验证
if (Test-Path $nginxExe) {
    Start-Process -FilePath $nginxExe -ArgumentList "-c", "`"$nginxConf`"" -WorkingDirectory $nginxWorkDir -WindowStyle Hidden
}
Start-Sleep -Seconds 8

Write-Host '== [7/7] 状态与端口验证 =='
foreach ($port in 6379, 3306, 8080) {
    $l = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
    if ($l) { Write-Host "[OK] 端口 $port 已监听 (PID $($l.OwningProcess))" }
    else { Write-Host "[WARN] 端口 $port 未监听" }
}
Get-Service -Name 'MySQL84' | Format-Table Name, Status, StartType -AutoSize
Get-ScheduledTask -TaskName $taskName, $nginxTaskName | Select-Object TaskName, State | Format-Table -AutoSize

Stop-Transcript | Out-Null
Write-Host 'DONE'
