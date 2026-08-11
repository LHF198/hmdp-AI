# ============================================================
# 黑马点评前端构建脚本（L1：Vite 多入口打包 -> html/dist）
# 用法：powershell -ExecutionPolicy Bypass -File build.ps1
# 说明：Node.js 为便携版（C:\Users\26821\dev\nodejs），未加入系统 PATH，
#       本脚本临时注入 PATH 后调用 npm run build
# ============================================================
$ErrorActionPreference = 'Stop'

$nodeDir = 'C:\Users\26821\dev\nodejs'
if (-not (Test-Path (Join-Path $nodeDir 'node.exe'))) {
    Write-Host "[FAIL] 未找到 Node.js：$nodeDir"
    exit 1
}

$env:Path = "$nodeDir;$env:Path"
Push-Location $PSScriptRoot
try {
    npm run build
    if ($LASTEXITCODE -eq 0) {
        Write-Host '[OK] 构建完成 -> html/dist（nginx root 已指向该目录）'
    } else {
        Write-Host '[FAIL] 构建失败，请检查上方输出'
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}
