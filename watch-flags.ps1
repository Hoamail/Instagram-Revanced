# Sample the Instagram modal window flags until stopped; log every distinct state.
$out = "work\watch-flags.txt"
"# start $(Get-Date -Format HH:mm:ss)" | Out-File $out -Encoding utf8
$last = ""
$end = (Get-Date).AddMinutes(20)
while ((Get-Date) -lt $end) {
    $all = adb shell dumpsys window windows 2>&1
    $hit = $all | Select-String -Pattern "Window #\d+ Window\{.*com.instagram.android/com.instagram.modal.TransparentModalActivity" | Select-Object -First 1
    if ($hit) {
        $i = $hit.LineNumber
        $fl = ($all[($i - 1)..($i + 12)] | Select-String -Pattern "^\s*fl=" | Select-Object -First 1)
        $line = if ($fl) { $fl.Line.Trim() } else { "(no fl= line)" }
        if ($line -ne $last) {
            "$(Get-Date -Format HH:mm:ss)  $line" | Out-File $out -Append -Encoding utf8
            $last = $line
        }
    } elseif ($last -ne "(window absent)") {
        "$(Get-Date -Format HH:mm:ss)  (window absent)" | Out-File $out -Append -Encoding utf8
        $last = "(window absent)"
    }
    Start-Sleep -Seconds 2
}
