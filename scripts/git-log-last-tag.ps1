# PowerShell script to print git log from last tag to HEAD in Markdown list
# Example format (same as used by the script):
# git log "$(git tag | Select-Object -Last 1)..HEAD" --pretty=format:"- [%h](https://github.com/Create-Nuclear-Team/CreateNuclearForge/commit/%H) | %s`n  Author: %an  %(trailers:key=Co-authored-by,valueonly=true)" --no-merges
param(
    [string]$OutputFile
)

Set-StrictMode -Version Latest

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Error "git is not installed or not in PATH"
    exit 1
}

Push-Location -Path (Resolve-Path -LiteralPath "./") | Out-Null
# Ensure variable exists for Set-StrictMode and later usage
$range = 'HEAD'
try {
    # Get the last tag (if any)
    $lastTag = git tag | Select-Object -Last 1

    $range = "$lastTag..HEAD"

    $format = '- [%h](https://github.com/Create-Nuclear-Team/CreateNuclearNeoForge/commit/%H) | %s  Author: %an  %(trailers:key=Co-authored-by,valueonly=true)'

    $args = @($range, "--pretty=format:$format")

    $result = & git log @args 2>&1

    if ($LASTEXITCODE -ne 0) {
        Write-Error "git log failed:`n$result"
        exit $LASTEXITCODE
    }

    if ($OutputFile) {
        $result | Out-File -FilePath $OutputFile -Encoding utf8
        Write-Host "Wrote output to $OutputFile"
    }
    else {
        Write-Output $result
    }
}
finally {
    Pop-Location | Out-Null
}
