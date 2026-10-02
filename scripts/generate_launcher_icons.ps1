param(
    [string]$SourceImage = "app/src/main/res/drawable/app_logo.png"
)

Add-Type -AssemblyName System.Drawing

$root = Resolve-Path "$PSScriptRoot\.."
$sourcePath = Join-Path $root $SourceImage

if (-not (Test-Path $sourcePath)) {
    Write-Error "Source image not found at $sourcePath"
    exit 1
}

$densities = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

$srcBitmap = [System.Drawing.Bitmap]::FromFile($sourcePath)

# Color of the background in the logo
$bgColor = [System.Drawing.Color]::FromArgb(255, 6, 25, 40)

foreach ($entry in $densities.GetEnumerator()) {
    $dirName = $entry.Key
    $size = $entry.Value
    $targetDir = Join-Path "$root\app\src\main\res" $dirName
    if (-not (Test-Path $targetDir)) {
        New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
    }

    # Generate square ic_launcher.png
    $destSquare = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($destSquare)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.Clear($bgColor)

    # Calculate scaling to fit the logo nicely with padding
    $padding = [int]($size * 0.08)
    $drawWidth = $size - (2 * $padding)
    $aspect = $srcBitmap.Height / $srcBitmap.Width
    $drawHeight = [int]($drawWidth * $aspect)
    $yOffset = [int](($size - $drawHeight) / 2)

    $g.DrawImage($srcBitmap, $padding, $yOffset, $drawWidth, $drawHeight)
    $g.Dispose()

    $destSquare.Save((Join-Path $targetDir "ic_launcher.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $destSquare.Dispose()

    # Generate round ic_launcher_round.png
    $destRound = New-Object System.Drawing.Bitmap $size, $size
    $gRound = [System.Drawing.Graphics]::FromImage($destRound)
    $gRound.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $gRound.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $gRound.Clear([System.Drawing.Color]::Transparent)

    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddEllipse(0, 0, $size, $size)
    $gRound.SetClip($path)

    $gRound.Clear($bgColor)
    $gRound.DrawImage($srcBitmap, $padding, $yOffset, $drawWidth, $drawHeight)
    $gRound.Dispose()
    $path.Dispose()

    $destRound.Save((Join-Path $targetDir "ic_launcher_round.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $destRound.Dispose()

    Write-Output "Generated $dirName icons (${size}x${size})"
}

$srcBitmap.Dispose()
Write-Output "All launcher icons generated successfully!"
