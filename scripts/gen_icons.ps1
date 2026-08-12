Add-Type -AssemblyName System.Drawing
# 相对项目根路径（脚本位于 scripts/ 下，项目根为其上一级）
$dir = Join-Path $PSScriptRoot '..\frontend\html\hmdp\imgs\types'
$ink = [System.Drawing.Color]::FromArgb(255, 40, 40, 40)

function New-IconBitmap {
    $bmp = New-Object System.Drawing.Bitmap(200, 200, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)
    return @($bmp, $g)
}

function Add-RoundedRect([System.Drawing.Drawing2D.GraphicsPath]$path, $x, $y, $w, $h, $r) {
    $d = $r * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
}

# ========== hpg.png 轰趴馆：房子 + 音符 ==========
$res = New-IconBitmap; $bmp = $res[0]; $g = $res[1]
$brush = New-Object System.Drawing.SolidBrush($ink)

# 房子（屋顶+主体），门窗用 Alternate 挖洞
$house = New-Object System.Drawing.Drawing2D.GraphicsPath
$house.FillMode = [System.Drawing.Drawing2D.FillMode]::Alternate
$roof = [System.Drawing.PointF[]]@(
    (New-Object System.Drawing.PointF(10, 130)),
    (New-Object System.Drawing.PointF(85, 68)),
    (New-Object System.Drawing.PointF(160, 130))
)
$house.AddPolygon($roof)
$house.AddRectangle((New-Object System.Drawing.RectangleF(30, 130, 110, 62)))
$house.AddRectangle((New-Object System.Drawing.RectangleF(52, 156, 26, 32)))   # 门
$house.AddRectangle((New-Object System.Drawing.RectangleF(98, 146, 24, 22)))   # 窗
$g.FillPath($brush, $house)

# 双音符（右上）
$note = New-Object System.Drawing.Drawing2D.GraphicsPath
$note.AddEllipse((New-Object System.Drawing.RectangleF(128, 52, 22, 16)))      # 符头1
$note.AddEllipse((New-Object System.Drawing.RectangleF(162, 42, 22, 16)))      # 符头2
$note.AddRectangle((New-Object System.Drawing.RectangleF(145, 18, 5, 42)))     # 符干1
$note.AddRectangle((New-Object System.Drawing.RectangleF(179, 8, 5, 42)))      # 符干2
$beam = [System.Drawing.PointF[]]@(                                            # 符杠
    (New-Object System.Drawing.PointF(145, 18)),
    (New-Object System.Drawing.PointF(184, 8)),
    (New-Object System.Drawing.PointF(184, 18)),
    (New-Object System.Drawing.PointF(145, 28))
)
$note.AddPolygon($beam)
$g.FillPath($brush, $note)

$bmp.Save("$dir\hpg.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()
Write-Host "hpg.png generated"

# ========== mjmj.png 美睫・美甲：眼睛+睫毛 + 指甲油瓶 ==========
$res = New-IconBitmap; $bmp = $res[0]; $g = $res[1]
$brush = New-Object System.Drawing.SolidBrush($ink)

# 眼睛：杏仁形外轮廓，虹膜挖洞，瞳孔再填充（Alternate 三层嵌套）
$eye = New-Object System.Drawing.Drawing2D.GraphicsPath
$eye.FillMode = [System.Drawing.Drawing2D.FillMode]::Alternate
$eye.StartFigure()
$eye.AddBezier(12, 112, 48, 72, 100, 72, 138, 112)      # 上眼睑
$eye.AddBezier(138, 112, 100, 140, 48, 140, 12, 112)    # 下眼睑
$eye.CloseFigure()
$eye.AddEllipse(51, 82, 48, 48)                         # 虹膜（洞）
$eye.AddEllipse(58, 89, 34, 34)                         # 瞳孔（再填充）
$g.FillPath($brush, $eye)

# 睫毛：5 根放射状粗线
$pen = New-Object System.Drawing.Pen($ink, 7)
$pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$g.DrawLine($pen, 35, 92, 22, 70)
$g.DrawLine($pen, 55, 81, 46, 57)
$g.DrawLine($pen, 75, 78, 75, 52)
$g.DrawLine($pen, 95, 81, 104, 57)
$g.DrawLine($pen, 115, 92, 128, 70)

# 指甲油瓶：瓶盖 + 瓶身（圆角矩形）
$cap = New-Object System.Drawing.Drawing2D.GraphicsPath
Add-RoundedRect $cap 161 62 26 36 6
$g.FillPath($brush, $cap)
$body = New-Object System.Drawing.Drawing2D.GraphicsPath
Add-RoundedRect $body 152 104 44 86 10
$g.FillPath($brush, $body)

$bmp.Save("$dir\mjmj.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()
Write-Host "mjmj.png generated"
