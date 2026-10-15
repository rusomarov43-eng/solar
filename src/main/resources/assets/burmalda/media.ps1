param([string]$dir, [int]$ppid)
$ErrorActionPreference = 'SilentlyContinue'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
Add-Type -AssemblyName System.Runtime.WindowsRuntime
Add-Type -AssemblyName System.Drawing

$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
})[0]

function Await($op, $type) {
    $task = $asTaskGeneric.MakeGenericMethod($type).Invoke($null, @($op))
    $task.Wait(-1) | Out-Null
    return $task.Result
}

$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]
$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType=WindowsRuntime]
$null = [Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType=WindowsRuntime]

$mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
if ($null -eq $mgr) { [Console]::Out.WriteLine('N'); [Console]::Out.Flush(); exit }

$lastKey = ''
$lastCover = ''
$serial = 0
$tick = 0
$ytOnly = $true
$best = $null
$cmdFile = Join-Path $dir 'cmd.txt'
$ytFile = Join-Path $dir 'yt.txt'

while ($true) {
    $tick++

    if (($tick % 8) -eq 0) {
        if (-not (Get-Process -Id $ppid -ErrorAction SilentlyContinue)) { exit }
    }

    # ---- commands from the game
    if (Test-Path $cmdFile) {
        $cmd = ((Get-Content $cmdFile -Raw) + '').Trim()
        Remove-Item $cmdFile -Force
        if ($null -ne $best -and $cmd -ne '') {
            $sess = $best.Session
            if ($cmd -eq 'toggle') {
                $null = Await ($sess.TryTogglePlayPauseAsync()) ([bool])
            } elseif ($cmd -eq 'rew') {
                $np = $best.PosMs - 10000; if ($np -lt 0) { $np = 0 }
                $null = Await ($sess.TryChangePlaybackPositionAsync([int64]($np * 10000))) ([bool])
            } elseif ($cmd -eq 'fwd') {
                $np = $best.PosMs + 10000
                $null = Await ($sess.TryChangePlaybackPositionAsync([int64]($np * 10000))) ([bool])
            } elseif ($cmd.StartsWith('seek:')) {
                $ms = [int64]$cmd.Substring(5)
                $null = Await ($sess.TryChangePlaybackPositionAsync([int64]($ms * 10000))) ([bool])
            }
            $tick = 1
        }
    }

    # ---- refresh state (every 500 ms)
    if (($tick % 2) -eq 0) {
        if (Test-Path $ytFile) { $ytOnly = (((Get-Content $ytFile -Raw) + '').Trim() -eq '1') }

        $titles = @()
        if ($ytOnly) {
            $titles = @([System.Diagnostics.Process]::GetProcesses() |
                Where-Object { $_.MainWindowTitle -like '*YouTube*' } |
                ForEach-Object { $_.MainWindowTitle })
        }

        $best = $null
        foreach ($s in $mgr.GetSessions()) {
            $app = [string]$s.SourceAppUserModelId
            if ($app -notmatch 'chrome|msedge|edge|firefox|brave|opera|vivaldi|yandex|browser|zen|arc') { continue }
            $props = Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
            if ($null -eq $props -or [string]::IsNullOrEmpty($props.Title)) { continue }
            $t = [string]$props.Title
            if ($ytOnly) {
                $hit = $false
                foreach ($w in $titles) { if ($w.Contains($t)) { $hit = $true; break } }
                if (-not $hit) { continue }
            }
            $status = [int]$s.GetPlaybackInfo().PlaybackStatus
            if ($status -ne 4 -and $status -ne 5) { continue }
            $cand = [pscustomobject]@{ Session = $s; Props = $props; Status = $status; PosMs = 0 }
            if ($null -eq $best -or ($status -eq 4 -and $best.Status -ne 4)) { $best = $cand }
        }

        if ($null -eq $best) {
            [Console]::Out.WriteLine('N'); [Console]::Out.Flush()
        } else {
            $p = $best.Props
            $title = ([string]$p.Title) -replace "[\t\r\n]", ' '
            $artist = ([string]$p.Artist) -replace "[\t\r\n]", ' '
            $tl = $best.Session.GetTimelineProperties()
            $pos = [int64]$tl.Position.TotalMilliseconds
            $dur = [int64]($tl.EndTime.TotalMilliseconds - $tl.StartTime.TotalMilliseconds)
            $best.PosMs = $pos

            $key = $title + '|' + $artist
            if ($key -ne $lastKey) {
                $lastKey = $key
                $serial++
                $lastCover = ''
                try {
                    if ($null -ne $p.Thumbnail) {
                        $st = Await ($p.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
                        $net = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($st)
                        $img = [System.Drawing.Image]::FromStream($net)
                        $path = Join-Path $dir ("cover_" + $serial + ".png")
                        $img.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
                        $img.Dispose(); $net.Dispose()
                        $lastCover = $path
                        Get-ChildItem $dir -Filter 'cover_*.png' | Where-Object { $_.Name -ne ("cover_" + $serial + ".png") } | Remove-Item -Force
                    }
                } catch { $lastCover = '' }
            }

            $play = 0; if ($best.Status -eq 4) { $play = 1 }
            $line = "S`t$play`t$title`t$artist`t$pos`t$dur`t$serial`t$lastCover"
            [Console]::Out.WriteLine($line); [Console]::Out.Flush()
        }
    }

    Start-Sleep -Milliseconds 250
}
