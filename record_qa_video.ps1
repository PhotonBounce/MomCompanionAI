# PowerShell script to automate video QA for Android tests
# 1. Starts adb screenrecord
# 2. Runs your test suite
# 3. Stops recording
# 4. Pulls video to PC

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$videoFile = "/sdcard/qa_test_$timestamp.mp4"
$localDir = "QA_Videos"
$localFile = "$localDir\qa_test_$timestamp.mp4"

if (!(Test-Path $localDir)) { New-Item -ItemType Directory -Path $localDir | Out-Null }

Write-Host "Starting screen recording..."
Start-Process adb -ArgumentList "shell screenrecord $videoFile" -WindowStyle Hidden
Start-Sleep -Seconds 2

Write-Host "Running test suite..."
./gradlew connectedAndroidTest

Write-Host "Stopping screen recording..."
adb shell pkill -l2 screenrecord
Start-Sleep -Seconds 2

Write-Host "Pulling video to PC..."
adb pull $videoFile $localFile
Write-Host "Video saved to $localFile"
