# ============================================================
# hmdp 项目环境配置脚本
# 1. Maven 用户级 settings.xml（阿里云/腾讯云/华为云 国内镜像）
# 2. 启动 nginx 前端服务（8080）
# ============================================================
$ErrorActionPreference = 'Continue'

# ---------- 1. Maven 国内镜像 ----------
$m2Dir = Join-Path $env:USERPROFILE '.m2'
New-Item -ItemType Directory -Force -Path $m2Dir | Out-Null
$settingsPath = Join-Path $m2Dir 'settings.xml'
$xml = @'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <!-- CN mirrors: Aliyun primary, Tencent/Huawei backup -->
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>*</mirrorOf>
      <name>aliyun public</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
    <mirror>
      <id>tencentmaven</id>
      <mirrorOf>central</mirrorOf>
      <name>tencent public</name>
      <url>https://mirrors.cloud.tencent.com/nexus/repository/maven-public/</url>
    </mirror>
    <mirror>
      <id>huaweimaven</id>
      <mirrorOf>central</mirrorOf>
      <name>huawei public</name>
      <url>https://repo.huaweicloud.com/repository/maven/</url>
    </mirror>
  </mirrors>
</settings>
'@
# UTF-8 without BOM (required by Maven XML parser)
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($settingsPath, $xml, $utf8NoBom)
Write-Host "[OK] Maven settings.xml -> $settingsPath"

# ---------- 2. Redis（6379，redis-windows 7.2.15） ----------
$redisDir = 'C:\Users\26821\dev\redis'
$redisListen = Get-NetTCPConnection -State Listen -LocalPort 6379 -ErrorAction SilentlyContinue
if ($redisListen) {
    Write-Host "[SKIP] Redis already listening on 6379"
} elseif (Test-Path (Join-Path $redisDir 'redis-server.exe')) {
    Start-Process -FilePath (Join-Path $redisDir 'redis-server.exe') -WorkingDirectory $redisDir -WindowStyle Hidden
    Start-Sleep -Seconds 2
    $l2 = Get-NetTCPConnection -State Listen -LocalPort 6379 -ErrorAction SilentlyContinue
    if ($l2) { Write-Host "[OK] Redis started on 6379" } else { Write-Host "[WARN] Redis failed to start" }
} else {
    Write-Host "[WARN] redis-server.exe not found at $redisDir"
}

# ---------- 3. 启动 nginx（8080 前端） ----------
# 相对项目根路径（脚本位于 scripts/ 下，项目根为其上一级）
$nginxDir = Join-Path $PSScriptRoot '..\frontend'
$nginxExe = Join-Path $nginxDir 'nginx.exe'
if (-not (Test-Path $nginxExe)) {
    Write-Host "[FAIL] nginx.exe not found at $nginxExe"
} else {
    $listen = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue
    if ($listen) {
        Write-Host "[SKIP] Port 8080 already in use (nginx running?)"
    } else {
        Start-Process -FilePath $nginxExe -ArgumentList '-p', "$nginxDir\" -WorkingDirectory $nginxDir -WindowStyle Hidden
        Start-Sleep -Seconds 2
        $listen2 = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue
        if ($listen2) {
            Write-Host "[OK] nginx started on http://localhost:8080"
        } else {
            Write-Host "[WARN] nginx did not start, check $nginxDir\logs\error.log"
            $log = Join-Path $nginxDir 'logs\error.log'
            if (Test-Path $log) { Get-Content $log -Tail 20 }
        }
    }
}

Write-Host "Done."
