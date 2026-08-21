$nginxExe = 'C:\Users\26821\dev\nginx-1.30.1\nginx.exe'
$nginxConf = 'C:\Users\26821\Downloads\hmdp-main\frontend\conf\nginx.conf'
$nginxWorkDir = 'C:\Users\26821\Downloads\hmdp-main\frontend'
$nginxTaskName = 'hmdp-Nginx'
$existing = Get-ScheduledTask -TaskName $nginxTaskName -ErrorAction SilentlyContinue
if (-not $existing) {
    $action = New-ScheduledTaskAction -Execute $nginxExe -Argument "-c `"$nginxConf`"" -WorkingDirectory $nginxWorkDir
    $trigger = New-ScheduledTaskTrigger -AtStartup
    $principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
    $settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
    Register-ScheduledTask -TaskName $nginxTaskName -Action $action -Trigger $trigger -Principal $principal -Settings $settings -Force | Out-Null
    Write-Host '[nginx] Scheduled task registered'
} else {
    Write-Host '[nginx] Scheduled task already exists'
}
Get-ScheduledTask -TaskName $nginxTaskName | Select-Object TaskName, State | Format-Table -AutoSize
