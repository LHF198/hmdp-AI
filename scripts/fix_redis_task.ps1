# ============================================================
# fix_redis_task.ps1 — 修复 hmdp-Redis 计划任务（需管理员）
# 问题: MSYS2 fork 会转换命令行路径参数（C:\... 或 redis.conf 均被加前缀）
# 方案: cmd /c cd 到 redis 目录后以相对路径启动，MSYS CWD=/dev/redis 映射正确
# ============================================================
$ErrorActionPreference = 'Stop'
$log = Join-Path $PSScriptRoot '..\logs\fix_redis_task.log'
Start-Transcript -Path $log -Force | Out-Null

$taskName = 'hmdp-Redis'
$cmd = 'C:\Windows\System32\cmd.exe'
$args = '/c cd /d C:\Users\26821\dev\redis && redis-server.exe redis.conf'

$action = New-ScheduledTaskAction -Execute $cmd -Argument $args
$trigger = New-ScheduledTaskTrigger -AtStartup
$principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Principal $principal -Settings $settings -Force | Out-Null
Write-Host '[OK] 任务已重新注册'

# 清掉可能残留的 redis 进程后启动任务
Get-Process redis-server -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1
Start-ScheduledTask -TaskName $taskName
Start-Sleep -Seconds 5

Write-Host '--- 任务状态 ---'
Get-ScheduledTask -TaskName $taskName | Select-Object TaskName, State | Format-Table -AutoSize
Get-ScheduledTaskInfo -TaskName $taskName | Select-Object LastRunTime, LastTaskResult | Format-List
Write-Host '--- redis 进程 ---'
Get-Process redis-server -ErrorAction SilentlyContinue | Select-Object Id, StartTime | Format-Table -AutoSize
Write-Host '--- 端口 ---'
$l = Get-NetTCPConnection -State Listen -LocalPort 6379 -ErrorAction SilentlyContinue
if ($l) { Write-Host "[OK] 6379 监听中 PID $($l.OwningProcess)" } else { Write-Host '[WARN] 6379 未监听' }
Write-Host '--- redis.log 尾部 ---'
if (Test-Path 'C:\Users\26821\dev\redis\redis.log') { Get-Content 'C:\Users\26821\dev\redis\redis.log' -Tail 6 } else { Write-Host '(无 redis.log)' }
Write-Host '--- MySQL84 ---'
Get-Service MySQL84 | Format-Table Name, Status, StartType -AutoSize

Stop-Transcript | Out-Null
Write-Host 'DONE'
